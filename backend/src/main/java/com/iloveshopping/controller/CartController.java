package com.iloveshopping.controller;

import com.iloveshopping.dto.cart.AddToCartRequest;
import com.iloveshopping.dto.cart.CartResponse;
import com.iloveshopping.dto.cart.UpdateCartItemRequest;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
public class CartController {

    private final CartService cartService;
    private static final String GUEST_COOKIE = "cartSessionId";

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @CookieValue(name = "cartSessionId", required = false) String sessionId) {
        CartResponse cart = cartService.getCurrentUserCart(sessionId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping
    @Operation(summary = "Create or get cart (for anonymous users)")
    public ResponseEntity<ApiResponse<CartResponse>> createCart(
            @CookieValue(name = "cartSessionId", required = false) String sessionId,
            HttpServletResponse response) {

        CartResponse cart = cartService.createOrGetCart(sessionId);
        issueGuestCookieIfNew(sessionId, cart, response);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @CookieValue(name = "cartSessionId", required = false) String sessionId,
            HttpServletResponse response) {

        CartResponse cart = cartService.addItem(sessionId, request);
        issueGuestCookieIfNew(sessionId, cart, response);
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

    private void issueGuestCookieIfNew(String incomingSessionId, CartResponse cart, HttpServletResponse response) {
        if (incomingSessionId != null && !incomingSessionId.isBlank()) {
            return;
        }
        if (cart == null || cart.getSessionId() == null || cart.getSessionId().isBlank()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(GUEST_COOKIE, cart.getSessionId())
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 3600)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}