package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends ApiException {

    public PaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, message, "PAYMENT_ERROR");
    }

    public static PaymentException paymentFailed(String reason) {
        return new PaymentException("Payment failed: " + reason);
    }

    public static PaymentException paymentTimeout() {
        return new PaymentException("Payment timed out");
    }

    public static PaymentException invalidCallback() {
        return new PaymentException("Invalid payment callback");
    }

    public static PaymentException providerUnavailable() {
        return new PaymentException("Payment provider unavailable");
    }
}