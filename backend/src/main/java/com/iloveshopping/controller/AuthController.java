package com.iloveshopping.controller;

import com.iloveshopping.dto.auth.*;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.entity.User;
import com.iloveshopping.service.AuthService;
import com.iloveshopping.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final AuthService authService;
    private final com.iloveshopping.util.CaptchaUtil captchaUtil;

    @PostMapping("/register")
    @Operation(summary = "Register new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse response = authService.register(request);
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ipAddress = RequestUtil.getClientIp(httpRequest);
        String userAgent = RequestUtil.getUserAgent(httpRequest);

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String token = refreshToken != null ? refreshToken : "";
        RefreshRequest request = RefreshRequest.builder().refreshToken(token).build();
        AuthResponse response = authService.refresh(request);
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        authService.logout(refreshToken);
        clearAuthCookies(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        if (authResponse.getAccessToken() != null) {
            Cookie accessCookie = new Cookie("accessToken", authResponse.getAccessToken());
            accessCookie.setPath("/api/v1");
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false); // set true in production
            accessCookie.setMaxAge((int) (authResponse.getExpiresIn()));
            response.addCookie(accessCookie);
        }
        if (authResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
            refreshCookie.setPath("/api/v1/auth");
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false); // set true in production
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);
        }
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", "");
        accessCookie.setPath("/api/v1");
        accessCookie.setHttpOnly(true);
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setPath("/api/v1/auth");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices and sessions")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {

        authService.logoutAllSessions(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email with token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {

        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/verify-captcha")
    @Operation(summary = "Get CAPTCHA site key for frontend")
    public ResponseEntity<ApiResponse<String>> getCaptchaSiteKey() {
        return ResponseEntity.ok(ApiResponse.success(captchaUtil.getSiteKey()));
    }

    @PostMapping("/2fa/setup")
    @Operation(summary = "Setup two-factor authentication")
    public ResponseEntity<ApiResponse<TwoFASetupResponse>> setup2FA(
            @AuthenticationPrincipal User user) {
        TwoFASetupResponse response = authService.setup2FA(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> enable2FA(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody Enable2FARequest request) {

        authService.enable2FA(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody Disable2FARequest request) {

        authService.disable2FA(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA code for login")
    public ResponseEntity<ApiResponse<AuthResponse>> verify2FA(
            @Valid @RequestBody Verify2FARequest request) {

        return ResponseEntity.ok(ApiResponse.success(AuthResponse.builder().build()));
    }
}