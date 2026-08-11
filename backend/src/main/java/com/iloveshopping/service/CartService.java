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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartResponse getCurrentUserCart() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getId());
            return cartOpt.map(CartResponse::from).orElse(null);
        }
        return null;
    }

    @Transactional
    public CartResponse createOrGetCart(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            Cart cart = Cart.builder().build();
            cart = cartRepository.save(cart);
            return CartResponse.from(cart);
        }

        Optional<Cart> cartOpt = cartRepository.findBySessionId(sessionId);
        if (cartOpt.isPresent()) {
            return CartResponse.from(cartOpt.get());
        }

        Cart cart = Cart.builder()
                .sessionId(sessionId)
                .build();
        cart = cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse addItem(String sessionId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(sessionId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product", "id", product.getId());
        }

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException(
                    product.getName(), request.getQuantity(), product.getStock()
            );
        }

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductIdAndVariantId(
                        cart.getId(), product.getId(), request.getVariantId()
                );

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new InsufficientStockException(
                        product.getName(), newQuantity, product.getStock()
                );
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variantId(request.getVariantId())
                    .quantity(request.getQuantity())
                    .priceSnapshot(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        cart = cartRepository.findById(cart.getId()).orElse(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(String itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        Product product = item.getProduct();
        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException(
                    product.getName(), request.getQuantity(), product.getStock()
            );
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart cart = cartRepository.findById(item.getCart().getId()).orElseThrow();
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(String itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        String cartId = item.getCart().getId();
        cartItemRepository.deleteCartItemById(itemId);

        Cart cart = cartRepository.findById(cartId).orElseThrow();
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clearCart() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getId());
            if (cartOpt.isPresent()) {
                Cart cart = cartOpt.get();
                cartItemRepository.deleteByCartId(cart.getId());
                cart.setItems(java.util.List.of());
                return CartResponse.from(cart);
            }
        }
        return null;
    }

    private Cart getOrCreateCart(String sessionId) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return cartRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        Cart cart = Cart.builder().user(currentUser).build();
                        return cartRepository.save(cart);
                    });
        }

        if (sessionId == null || sessionId.isBlank()) {
            return cartRepository.save(Cart.builder().build());
        }

        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart cart = Cart.builder().sessionId(sessionId).build();
                    return cartRepository.save(cart);
                });
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