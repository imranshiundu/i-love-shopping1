package com.iloveshopping.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends ApiException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR", fieldErrors);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}