package com.iloveshopping.controller;

import com.iloveshopping.config.AppProperties;
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
    private final AppProperties appProperties;
    private static final String COOKIE = "cartSessionId";

    @GetMapping
    @Operation(summary = "Get the current cart (user or guest)")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @CookieValue(name = COOKIE, required = false) String sessionId,
            HttpServletResponse response) {
        CartResponse cart = cartService.getOrCreateCart(sessionId);
        setCookieIfNeeded(sessionId, cart, response);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request,
            @CookieValue(name = COOKIE, required = false) String sessionId,
            HttpServletResponse response) {

        CartResponse cart = cartService.addItem(sessionId, request);
        setCookieIfNeeded(sessionId, cart, response);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.updateItem(itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable String itemId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.removeItem(itemId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear the current cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @CookieValue(name = COOKIE, required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.clearCart(sessionId)));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart into user cart on login")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @CookieValue(name = COOKIE, required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.mergeGuestCart(sessionId)));
    }

    private void setCookieIfNeeded(String incoming, CartResponse cart, HttpServletResponse response) {
        if (incoming != null && !incoming.isBlank()) return;
        if (cart == null || cart.getSessionId() == null || cart.getSessionId().isBlank()) return;

        boolean isSecure = appProperties.getFrontendUrl() != null
                && appProperties.getFrontendUrl().startsWith("https");
        ResponseCookie cookie = ResponseCookie.from(COOKIE, cart.getSessionId())
                .httpOnly(true)
                .sameSite(isSecure ? "None" : "Lax")
                .secure(isSecure)
                .path("/")
                .maxAge(7 * 24 * 3600)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
