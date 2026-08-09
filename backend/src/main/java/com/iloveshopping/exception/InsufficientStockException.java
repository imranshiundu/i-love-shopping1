package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Insufficient stock for %s. Requested: %d, Available: %d", productName, requested, available),
                "INSUFFICIENT_STOCK");
    }

    public InsufficientStockException(String message) {
        super(HttpStatus.BAD_REQUEST, message, "INSUFFICIENT_STOCK");
    }
}