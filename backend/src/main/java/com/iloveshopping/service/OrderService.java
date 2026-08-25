package com.iloveshopping.service;

import com.iloveshopping.config.AppProperties;
import com.iloveshopping.dto.order.CheckoutRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.entity.*;
import com.iloveshopping.exception.InsufficientStockException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.messaging.OrderMessagePublisher;
import com.iloveshopping.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@lombok.extern.slf4j.Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final OrderMessagePublisher orderMessagePublisher;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, String guestSessionId) {
        User currentUser = getCurrentUser();

        Cart cart = null;
        if (currentUser != null) {
            cart = cartRepository.findByUserId(currentUser.getId()).orElse(null);
        } else if (guestSessionId != null && !guestSessionId.isBlank()) {
            cart = cartRepository.findBySessionId(guestSessionId).orElse(null);
        }
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate stock for all items
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), item.getQuantity(), product.getStock()
                );
            }
        }

        // Calculate totals
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(appProperties.getTaxRate()));
        BigDecimal shipping = calculateShipping(subtotal);
        BigDecimal total = subtotal.add(tax).add(shipping);

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Build order
        String shippingAddrJson;
        String billingAddrJson;
        try {
            shippingAddrJson = com.iloveshopping.service.DataEncryptionService.encryptForJson(
                    objectMapper.writeValueAsString(request.getShippingAddress()));
            billingAddrJson = com.iloveshopping.service.DataEncryptionService.encryptForJson(
                    objectMapper.writeValueAsString(request.getBillingAddress()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize addresses", e);
        }

        Order order = Order.builder()
                .number(orderNumber)
                .user(currentUser)
                .status(Order.OrderStatus.PENDING)
                .subtotal(subtotal)
                .tax(tax)
                .shipping(shipping)
                .total(total)
                .currency(appProperties.getDefaultCurrency())
                .shippingAddress(shippingAddrJson)
                .billingAddress(billingAddrJson)
                .notes(request.getNotes())
                .build();

        // Create order items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variantId(cartItem.getVariantId())
                    .name(cartItem.getProduct().getName())
                    .price(cartItem.getPriceSnapshot())
                    .quantity(cartItem.getQuantity())
                    .total(cartItem.getLineTotal())
                    .build();
            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        // Decrement stock
        for (CartItem cartItem : cart.getItems()) {
            productRepository.decrementStock(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());

        Order savedOrder = order;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                orderMessagePublisher.publishOrderCreated(savedOrder);
                            } catch (Exception e) {
                                log.error("Failed to publish ORDER_CREATED event for order {}: {}", savedOrder.getNumber(), e.getMessage());
                            }
                        }
                    });
        }

        return OrderResponse.from(order);
    }

    public List<OrderResponse> getUserOrders(int page, int size) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("User", "authentication", "not found");
        }

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        return orders.stream()
                .skip((long) page * size)
                .limit(size)
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));

        User currentUser = getCurrentUser();
        if (currentUser == null || !order.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Order", "number", orderNumber);
        }

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled at this status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        return OrderResponse.from(order);
    }

    private String generateOrderNumber() {
        String prefix = appProperties.getOrderNumberPrefix();
        String timestamp = String.format("%010d", System.currentTimeMillis() / 1000);
        String random = UUID.randomUUID().toString().substring(0, 4);
        return prefix + "-" + timestamp + "-" + random.toUpperCase();
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(appProperties.getFreeShippingThreshold())) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(200); // Default shipping fee in KES
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        }
        return null;
    }
}