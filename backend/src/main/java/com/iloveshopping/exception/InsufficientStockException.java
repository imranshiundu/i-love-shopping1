package com.iloveshopping.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InsufficientStockException extends ApiException {

    private final String productName;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, int requested, int available) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Insufficient stock for %s. Requested: %d, Available: %d", productName, requested, available),
                "INSUFFICIENT_STOCK");
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String message) {
        super(HttpStatus.BAD_REQUEST, message, "INSUFFICIENT_STOCK");
        this.productName = null;
        this.requested = 0;
        this.available = 0;
    }
}