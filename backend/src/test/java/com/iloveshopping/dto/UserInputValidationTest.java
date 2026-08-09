package com.iloveshopping.dto.auth;

import com.iloveshopping.dto.user.ChangePasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ===== RegisterRequest =====

    @Test
    void shouldValidateValidRegisterRequest() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .captchaToken("captcha-token")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .captchaToken("captcha-token")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void shouldRejectShortPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("short")
                .captchaToken("captcha-token")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void shouldRejectMissingEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .password("password123")
                .captchaToken("captcha-token")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void shouldRejectMissingCaptchaToken() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("captchaToken")));
    }

    // ===== LoginRequest =====

    @Test
    void shouldValidateValidLoginRequest() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectLoginWithoutEmail() {
        LoginRequest request = LoginRequest.builder()
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectLoginWithoutPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ===== ForgotPasswordRequest =====

    @Test
    void shouldValidateValidForgotPasswordRequest() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("invalid")
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ===== ResetPasswordRequest =====

    @Test
    void shouldValidateValidResetPasswordRequest() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("reset-token-123")
                .password("newPassword123")
                .build();

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectShortResetPassword() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("reset-token-123")
                .password("short")
                .build();

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectMissingResetToken() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .password("newPassword123")
                .build();

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ===== Enable2FARequest =====

    @Test
    void shouldValidateValidEnable2FARequest() {
        Enable2FARequest request = Enable2FARequest.builder()
                .code("123456")
                .build();

        Set<ConstraintViolation<Enable2FARequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectShort2FACode() {
        Enable2FARequest request = Enable2FARequest.builder()
                .code("12345")
                .build();

        Set<ConstraintViolation<Enable2FARequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectLong2FACode() {
        Enable2FARequest request = Enable2FARequest.builder()
                .code("1234567")
                .build();

        Set<ConstraintViolation<Enable2FARequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ===== Disable2FARequest =====

    @Test
    void shouldValidateValidDisable2FARequest() {
        Disable2FARequest request = Disable2FARequest.builder()
                .password("currentPassword")
                .code("123456")
                .build();

        Set<ConstraintViolation<Disable2FARequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectMissingPasswordInDisable2FARequest() {
        Disable2FARequest request = Disable2FARequest.builder()
                .code("123456")
                .build();

        Set<ConstraintViolation<Disable2FARequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ===== ChangePasswordRequest =====

    @Test
    void shouldValidateValidChangePasswordRequest() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("currentPass")
                .newPassword("newPassword123")
                .build();

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectShortNewPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("currentPass")
                .newPassword("short")
                .build();

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}