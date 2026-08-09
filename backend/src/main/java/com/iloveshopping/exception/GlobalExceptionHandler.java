package com.iloveshopping.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.iloveshopping.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex, WebRequest request) {
        log.error("API Exception: ", ex);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.<Object>builder()
                        .success(false)
                        .error(error)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return handleApiException(ex, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return handleApiException(ex, request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceConflictException(ResourceConflictException ex, WebRequest request) {
        return handleApiException(ex, request);
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull org.springframework.http.HttpStatus status,
            @NonNull WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation error: {}", fieldErrors);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .details(fieldErrors)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv ->
                fieldErrors.put(cv.getPropertyPath().toString(), cv.getMessage())
        );

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .details(fieldErrors)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatus status,
            @NonNull WebRequest request) {

        String message = "Invalid request body";
        if (ex.getCause() instanceof JsonMappingException jme) {
            message = "Invalid value for field '" + jme.getPath().getLast().getFieldName() + "': " + jme.getMessage();
        } else if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }

        log.warn("Message not readable: {}", message);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {

        log.error("Data integrity violation: ", ex);

        String message = "Database constraint violated";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("duplicate key")) {
                message = "A record with this value already exists";
            } else if (ex.getMessage().contains("foreign key")) {
                message = "Referenced record does not exist";
            }
        }

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex, WebRequest request) {

        log.warn("Optimistic locking failure: ", ex);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("Record was modified by another user. Please try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllUncaughtException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception: ", ex);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.internalServerError().body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            @NonNull HttpRequestMethodNotSupportedException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatus status,
            @NonNull WebRequest request) {

        String message = "Method " + ex.getMethod() + " not supported for " + request.getDescription(false);
        log.warn(message);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            @NonNull HttpMediaTypeNotSupportedException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatus status,
            @NonNull WebRequest request) {

        String message = "Media type " + ex.getContentType() + " not supported";
        log.warn(message);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }

    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatus status,
            @NonNull WebRequest request) {

        String message = "Missing required parameter: " + ex.getParameterName();
        log.warn(message);

        ApiResponse.ErrorResponse error = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build());
    }
}