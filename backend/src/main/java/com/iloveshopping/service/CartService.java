package com.iloveshopping.service;

import com.iloveshopping.dto.cart.AddToCartRequest;
import com.iloveshopping.dto.cart.CartResponse;
import com.iloveshopping.dto.cart.UpdateCartItemRequest;
import com.iloveshopping.entity.Cart;
import com.iloveshopping.entity.CartItem;
import com.iloveshopping.entity.Product;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.InsufficientStockException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.CartItemRepository;
import com.iloveshopping.repository.CartRepository;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(String sessionId) {
        Cart cart = findCart(sessionId);
        if (cart == null) return null;
        return toResponse(cart);
    }

    @Transactional
    public CartResponse getOrCreateCart(String sessionId) {
        Cart cart = findCart(sessionId);
        if (cart != null) return toResponse(cart);

        cart = new Cart();
        cart.setSessionId(sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString());
        cart = cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String sessionId, AddToCartRequest request) {
        Cart cart = getOrCreateCartEntity(sessionId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product not available: " + product.getId());
        }

        int qty = request.getQuantity() == null ? 1 : request.getQuantity();
        if (qty <= 0) qty = 1;

        if (product.getStock() < qty) {
            throw new InsufficientStockException(product.getName(), qty, product.getStock());
        }

        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductIdAndVariantId(cart.getId(), product.getId(), request.getVariantId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + qty;
            if (product.getStock() < newQty) {
                throw new InsufficientStockException(product.getName(), newQty, product.getStock());
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variantId(request.getVariantId())
                    .quantity(qty)
                    .priceSnapshot(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        int qty = request.getQuantity() == null ? 1 : request.getQuantity();
        if (qty <= 0) {
            cartItemRepository.delete(item);
            Cart cart = cartRepository.findById(item.getCart().getId()).orElseThrow();
            return toResponse(cart);
        }

        Product product = item.getProduct();
        if (product.getStock() < qty) {
            throw new InsufficientStockException(product.getName(), qty, product.getStock());
        }
        item.setQuantity(qty);
        cartItemRepository.save(item);

        return toResponse(item.getCart());
    }

    @Transactional
    public CartResponse removeItem(String itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        String cartId = item.getCart().getId();
        cartItemRepository.delete(item);
        Cart cart = cartRepository.findById(cartId).orElseThrow();
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(String sessionId) {
        Cart cart = findCart(sessionId);
        if (cart == null) return null;
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        return toResponse(cart);
    }

    @Transactional
    public CartResponse mergeGuestCart(String guestSessionId) {
        User user = currentUser();
        if (user == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        Cart userCart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user);
                    return cartRepository.save(c);
                });

        if (guestSessionId == null || guestSessionId.isBlank()) {
            return toResponse(userCart);
        }

        Optional<Cart> guestCartOpt = cartRepository.findBySessionId(guestSessionId);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getId().equals(userCart.getId())) {
            return toResponse(userCart);
        }

        Cart guestCart = guestCartOpt.get();
        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCart.getId());

        for (CartItem guestItem : guestItems) {
            Product product = guestItem.getProduct();
            int stock = Math.max(product.getStock(), 0);
            if (stock <= 0) continue;

            Optional<CartItem> existing = cartItemRepository
                    .findByCartIdAndProductIdAndVariantId(userCart.getId(), product.getId(), guestItem.getVariantId());

            int qty = Math.min(guestItem.getQuantity(), stock);
            if (existing.isPresent()) {
                CartItem target = existing.get();
                int newQty = Math.min(target.getQuantity() + qty, stock);
                target.setQuantity(newQty);
                cartItemRepository.save(target);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(product)
                        .variantId(guestItem.getVariantId())
                        .quantity(qty)
                        .priceSnapshot(product.getPrice())
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        cartItemRepository.deleteByCartId(guestCart.getId());
        cartRepository.delete(guestCart);

        return toResponse(userCart);
    }

    @Transactional
    public Cart restoreCartFromOrder(com.iloveshopping.entity.Order order) {
        Cart cart;
        if (order.getUser() != null) {
            cart = cartRepository.findByUserId(order.getUser().getId())
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setUser(order.getUser());
                        return cartRepository.save(c);
                    });
        } else if (order.getCartSessionId() != null && !order.getCartSessionId().isBlank()) {
            final String sid = order.getCartSessionId();
            cart = cartRepository.findBySessionId(sid)
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setSessionId(sid);
                        return cartRepository.save(c);
                    });
        } else {
            cart = new Cart();
            cart.setSessionId(UUID.randomUUID().toString());
            cart = cartRepository.save(cart);
        }

        cartItemRepository.deleteByCartId(cart.getId());

        for (com.iloveshopping.entity.OrderItem orderItem : order.getItems()) {
            Product product = orderItem.getProduct();
            int stock = Math.max(product.getStock(), 0);
            if (stock <= 0) continue;
            int qty = Math.min(orderItem.getQuantity(), stock);

            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variantId(orderItem.getVariantId())
                    .quantity(qty)
                    .priceSnapshot(product.getPrice())
                    .build();
            cartItemRepository.save(item);
        }

        return cart;
    }

    private Cart findCart(String sessionId) {
        User user = currentUser();
        if (user != null) {
            return cartRepository.findByUserId(user.getId()).orElse(null);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionId(sessionId).orElse(null);
        }
        return null;
    }

    private Cart getOrCreateCartEntity(String sessionId) {
        User user = currentUser();
        if (user != null) {
            return cartRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setUser(user);
                        return cartRepository.save(c);
                    });
        }
        if (sessionId == null || sessionId.isBlank()) {
            Cart c = new Cart();
            c.setSessionId(UUID.randomUUID().toString());
            return cartRepository.save(c);
        }
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setSessionId(sessionId);
                    return cartRepository.save(c);
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cart.getId() == null
                ? new ArrayList<>()
                : cartItemRepository.findByCartId(cart.getId());

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = items.stream()
                .map(it -> it.getPriceSnapshot().multiply(BigDecimal.valueOf(it.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .items(itemResponses)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .build();
    }

    private CartResponse.CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        String image = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            image = product.getImages().get(0).getUrl();
        }
        return CartResponse.CartItemResponse.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productSlug(product != null ? product.getSlug() : null)
                .productImage(image)
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .priceSnapshot(item.getPriceSnapshot())
                .lineTotal(item.getLineTotal())
                .maxStock(product != null ? product.getStock() : 0)
                .build();
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return userRepository.findById(u.getId()).orElse(null);
        }
        return null;
    }
}
