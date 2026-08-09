package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(HttpStatus.NOT_FOUND,
                String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
                "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message, "RESOURCE_NOT_FOUND");
    }
}