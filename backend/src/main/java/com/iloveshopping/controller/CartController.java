package com.iloveshopping.controller;

import com.iloveshopping.dto.cart.AddToCartRequest;
import com.iloveshopping.dto.cart.CartResponse;
import com.iloveshopping.dto.cart.UpdateCartItemRequest;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCurrentUserCart();
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping
    @Operation(summary = "Create or get cart (for anonymous users)")
    public ResponseEntity<ApiResponse<CartResponse>> createCart(
            @CookieValue(name = "cartSessionId", required = false) String sessionId) {

        CartResponse cart = cartService.createOrGetCart(sessionId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @CookieValue(name = "cartSessionId", required = false) String sessionId) {

        CartResponse cart = cartService.addItem(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse cart = cartService.updateItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String itemId) {

        CartResponse cart = cartService.removeItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        CartResponse cart = cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success(cart));
    }
}