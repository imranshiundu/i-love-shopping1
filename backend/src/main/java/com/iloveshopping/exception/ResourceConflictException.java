package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends ApiException {

    public ResourceConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(HttpStatus.CONFLICT,
                String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue),
                "RESOURCE_CONFLICT");
    }

    public ResourceConflictException(String message) {
        super(HttpStatus.CONFLICT, message, "RESOURCE_CONFLICT");
    }
}