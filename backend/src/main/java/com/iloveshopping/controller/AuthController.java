package com.iloveshopping.controller;

import com.iloveshopping.dto.auth.*;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.service.AuthService;
import com.iloveshopping.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestUtil.getClientIp(httpRequest);
        String userAgent = RequestUtil.getUserAgent(httpRequest);

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        String token = refreshToken != null ? refreshToken : "";
        RefreshRequest request = RefreshRequest.builder().refreshToken(token).build();
        AuthResponse response = authService.refresh(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices and sessions")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader("Authorization") String authHeader) {

        String userId = ""; // Extract from JWT
        authService.logoutAllSessions(userId);
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
    public ResponseEntity<ApiResponse<TwoFASetupResponse>> setup2FA() {
        String userId = ""; // Extract from JWT
        TwoFASetupResponse response = authService.setup2FA(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> enable2FA(
            @Valid @RequestBody Enable2FARequest request) {

        String userId = ""; // Extract from JWT
        authService.enable2FA(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @Valid @RequestBody Disable2FARequest request) {

        String userId = ""; // Extract from JWT
        authService.disable2FA(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA code for login")
    public ResponseEntity<ApiResponse<AuthResponse>> verify2FA(
            @Valid @RequestBody Verify2FARequest request) {

        return ResponseEntity.ok(ApiResponse.success(AuthResponse.builder().build()));
    }
}