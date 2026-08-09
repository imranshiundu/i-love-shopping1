package com.iloveshopping.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Object details;

    public ApiException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApiException(HttpStatus status, String message, String errorCode, Object details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST");
    }

    public static ApiException badRequest(String message, Object details) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST", details);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message, "UNAUTHORIZED");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, "FORBIDDEN");
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, "NOT_FOUND");
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, "CONFLICT");
    }

    public static ApiException internal(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, "INTERNAL_ERROR");
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, "TOO_MANY_REQUESTS");
    }
}