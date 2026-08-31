package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ApiException {

    public AuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message, "AUTHENTICATION_FAILED");
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }

    public static AuthenticationException accountNotFound() {
        return new AuthenticationException("No account found with this email. Please create an account.");
    }

    public static AuthenticationException incorrectPassword() {
        return new AuthenticationException("Incorrect password. Please try again.");
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Token has expired");
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid token");
    }

    public static AuthenticationException twoFactorRequired() {
        return new AuthenticationException("Two-factor authentication required");
    }

    public static AuthenticationException invalidTwoFactorCode() {
        return new AuthenticationException("Invalid 2FA code");
    }

    public static AuthenticationException accountNotVerified() {
        return new AuthenticationException("Email not verified");
    }
}