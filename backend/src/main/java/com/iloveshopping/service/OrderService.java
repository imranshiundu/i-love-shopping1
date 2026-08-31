package com.iloveshopping.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iloveshopping.config.AppProperties;
import com.iloveshopping.dto.order.CheckoutRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.entity.Cart;
import com.iloveshopping.entity.CartItem;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.OrderItem;
import com.iloveshopping.entity.Product;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.InsufficientStockException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.CartItemRepository;
import com.iloveshopping.repository.CartRepository;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final OrderMessagePublisher orderMessagePublisher;
    private final CartService cartService;
    private final com.iloveshopping.validation.AddressValidator addressValidator;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, String sessionId) {
        User user = currentUser();

        Cart cart = user != null
                ? cartRepository.findByUserId(user.getId()).orElse(null)
                : (sessionId != null && !sessionId.isBlank()
                    ? cartRepository.findBySessionId(sessionId).orElse(null) : null);

        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        String email = user != null ? user.getEmail() : request.getGuestEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required for guest checkout");
        }
        if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 255) {
            throw new IllegalArgumentException("Invalid email address");
        }

        // Verify shipping/billing addresses are plausible (reject gibberish)
        addressValidator.validate(request.getShippingAddress(), "Shipping");
        addressValidator.validate(request.getBillingAddress(), "Billing");

        // Calculate from current cart state (server is source of truth)
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate stock + build items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(product.getName(), item.getQuantity(), product.getStock());
            }
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(appProperties.getTaxRate()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = calculateShipping(subtotal);
        BigDecimal total = subtotal.add(tax).add(shipping);

        String orderNumber = generateOrderNumber();

        String shippingJson = toJson(request.getShippingAddress());
        String billingJson = toJson(request.getBillingAddress());

        Order order = Order.builder()
                .number(orderNumber)
                .user(user)
                .guestEmail(email)
                .cartSessionId(user == null ? cart.getSessionId() : null)
                .status(Order.OrderStatus.PENDING)
                .subtotal(subtotal)
                .tax(tax)
                .shipping(shipping)
                .total(total)
                .currency(appProperties.getDefaultCurrency())
                .shippingAddress(shippingJson)
                .billingAddress(billingJson)
                .notes(request.getNotes())
                .build();

        for (CartItem ci : items) {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(ci.getProduct())
                    .variantId(ci.getVariantId())
                    .name(ci.getProduct().getName())
                    .price(ci.getPriceSnapshot())
                    .quantity(ci.getQuantity())
                    .total(ci.getLineTotal())
                    .build();
            order.getItems().add(oi);
        }

        // Validate stock availability before saving order
        for (CartItem ci : items) {
            Product p = ci.getProduct();
            if (p.getStock() < ci.getQuantity()) {
                throw new InsufficientStockException(p.getName(), ci.getQuantity(), p.getStock());
            }
        }

        final Order saved = orderRepository.save(order);

        // Atomic stock decrement with compensation on failure
        List<CartItem> decremented = new ArrayList<>();
        try {
            for (CartItem ci : items) {
                int rows = productRepository.decrementStock(ci.getProduct().getId(), ci.getQuantity());
                if (rows == 0) {
                    // Restore any stock we already decremented
                    for (CartItem restored : decremented) {
                        productRepository.incrementStock(restored.getProduct().getId(), restored.getQuantity());
                    }
                    Product p = ci.getProduct();
                    throw new InsufficientStockException(p.getName(), ci.getQuantity(), p.getStock());
                }
                decremented.add(ci);
            }
        } catch (InsufficientStockException e) {
            throw e;
        } catch (Exception e) {
            // Restore stock on unexpected failure
            for (CartItem restored : decremented) {
                productRepository.incrementStock(restored.getProduct().getId(), restored.getQuantity());
            }
            throw e;
        }

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());

        // Publish event after commit
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try { orderMessagePublisher.publishOrderCreated(saved); }
                            catch (Exception e) { log.error("publishOrderCreated failed: {}", e.getMessage()); }
                        }
                    });
        }

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(int page, int size, String statusFilter) {
        User user = currentUser();
        if (user == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        Order.OrderStatus filter = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try { filter = Order.OrderStatus.valueOf(statusFilter.toUpperCase()); }
            catch (IllegalArgumentException e) { /* ignore invalid */ }
        }
        var pageReq = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return orderRepository.findByUserIdFiltered(
                user.getId(),
                filter != null, filter,
                false, null,
                false, null,
                pageReq)
                .getContent().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, String sessionId) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        User user = currentUser();
        boolean isOwner = false;

        if (user != null && order.getUser() != null && order.getUser().getId().equals(user.getId())) {
            isOwner = true;
        }
        if (!isOwner && order.getUser() == null && sessionId != null
                && sessionId.equals(order.getCartSessionId())) {
            isOwner = true;
        }
        if (!isOwner && user != null && user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> r == User.Role.ADMIN)) {
            isOwner = true;
        }
        if (!isOwner) {
            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber, String sessionId) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        User user = currentUser();
        boolean isOwner = false;
        if (user != null && order.getUser() != null && order.getUser().getId().equals(user.getId())) {
            isOwner = true;
        }
        if (!isOwner && order.getUser() == null && sessionId != null
                && sessionId.equals(order.getCartSessionId())) {
            isOwner = true;
        }
        if (!isOwner && user != null && user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> r == User.Role.ADMIN)) {
            isOwner = true;
        }
        if (!isOwner) {
            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled at status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Restore the cart
        try {
            cartService.restoreCartFromOrder(order);
        } catch (Exception e) {
            log.error("Failed to restore cart for {}: {}", order.getNumber(), e.getMessage());
        }

        try { orderMessagePublisher.publishOrderCancelled(order); }
        catch (Exception e) { log.error("publishOrderCancelled failed: {}", e.getMessage()); }

        return OrderResponse.from(order);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300_000)
    @Transactional
    public void expireStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Order> stale = orderRepository.findStalePendingOrders(cutoff);
        for (Order order : stale) {
            log.warn("Auto-expiring unpaid order {} (created {})", order.getNumber(), order.getCreatedAt());
            for (OrderItem item : order.getItems()) {
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
            order.setStatus(Order.OrderStatus.EXPIRED);
            orderRepository.save(order);
        }
        if (!stale.isEmpty()) {
            log.info("Auto-expired {} unpaid orders", stale.size());
        }
    }

    private String generateOrderNumber() {
        String prefix = appProperties.getOrderNumberPrefix();
        String ts = String.format("%010d", System.currentTimeMillis() / 1000);
        // 8 hex chars = 4 billion combinations, much safer than 4
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + ts + "-" + rand;
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(appProperties.getFreeShippingThreshold())) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(10); // KES 10 flat shipping
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize: " + o, e);
        }
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return userRepository.findById(u.getId()).orElse(null);
        }
        return null;
    }
}
