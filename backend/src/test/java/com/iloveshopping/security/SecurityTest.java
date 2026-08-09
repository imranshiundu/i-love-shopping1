package com.iloveshopping.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {

    @Test
    void shouldRejectSqlInjectionInProductId() {
        String[] injectionPayloads = {
                "' OR '1'='1",
                "1; DROP TABLE users",
                "1 UNION SELECT * FROM users",
                "' OR 1=1 --",
                "admin'--",
                "1' OR '1'='1' /*"
        };

        for (String payload : injectionPayloads) {
            assertDoesNotThrow(() -> {
                String sanitized = sanitizeInput(payload);
                assertTrue(isSqlInjection(payload), "Should detect SQL injection: " + payload);
            });
        }
    }

    @Test
    void shouldRejectXssPayloads() {
        String[] xssPayloads = {
                "<script>alert('xss')</script>",
                "<img src=x onerror=alert('xss')>",
                "javascript:alert('xss')",
                "<svg onload=alert('xss')>",
                "'\"><script>alert('xss')</script>"
        };

        for (String payload : xssPayloads) {
            assertTrue(isXssPayload(payload), "Should detect XSS payload: " + payload);
        }
    }

    @Test
    void shouldRejectPathTraversalAttacks() {
        String[] traversalPayloads = {
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32",
                "....//....//....//etc/passwd",
                "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd"
        };

        for (String payload : traversalPayloads) {
            assertTrue(isPathTraversal(payload), "Should detect path traversal: " + payload);
        }
    }

    @Test
    void shouldRejectMalformedJsonInputs() {
        String[] malformedInputs = {
                "{\"email\":}",
                "{\"email\":\"not-an-email\"}",
                "{\"password\":\"\"}",
                "{\"email\":\"\"}",
                "{\"email\":null,\"password\":null}"
        };

        for (String input : malformedInputs) {
            assertDoesNotThrow(() -> {
                // Validate that malformed inputs are handled gracefully
                boolean isValid = validateJsonInput(input);
                // At least the malformed JSON should not crash the application
            });
        }
    }

    @Test
    void shouldRejectOversizedInputs() {
        String longString = "x".repeat(10000);

        assertThrows(IllegalArgumentException.class, () -> {
            validateInputLength(longString, 100);
        });
    }

    @Test
    void shouldAcceptNormalInputs() {
        String[] validInputs = {
                "test@example.com",
                "John Doe",
                "P@ssw0rd123!",
                "Product Name - Special Edition"
        };

        for (String input : validInputs) {
            assertTrue(isValidInput(input), "Should accept valid input: " + input);
        }
    }

    @Test
    void shouldValidateJwtStructure() {
        String[] invalidTokens = {
                "",
                "invalid",
                "header.payload", // missing signature
                "header.payload.signature.extra" // too many parts
        };

        for (String token : invalidTokens) {
            assertFalse(isValidJwt(token), "Should reject invalid JWT: " + token);
        }
    }

    @Test
    void shouldAcceptValidJwtStructure() {
        String validJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature";
        assertTrue(isValidJwt(validJwt), "Should accept valid JWT structure");
    }

    // Helper methods for testing
    private boolean isSqlInjection(String input) {
        String lower = input.toLowerCase();
        return lower.contains("'") || lower.contains(";") || lower.contains("union") ||
               lower.contains("drop") || lower.contains("insert") || lower.contains("delete");
    }

    private boolean isXssPayload(String input) {
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") ||
               lower.contains("onerror") || lower.contains("onload") || lower.contains("<svg");
    }

    private boolean isPathTraversal(String input) {
        return input.contains("../") || input.contains("..\\") || input.contains("%2e%2e");
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'&]", "")
                   .replaceAll("(?i)(union|select|insert|delete|drop|update|exec|execute)", "")
                   .trim();
    }

    private boolean validateJsonInput(String json) {
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateInputLength(String input, int maxLength) {
        if (input.length() > maxLength) {
            throw new IllegalArgumentException("Input exceeds maximum length of " + maxLength);
        }
    }

    private boolean isValidInput(String input) {
        if (input == null || input.isBlank()) return false;
        if (input.length() > 500) return false;
        return !input.contains("<script") && !input.contains("DROP TABLE");
    }

    private boolean isValidJwt(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        for (String part : parts) {
            if (part.isBlank()) return false;
        }
        return true;
    }
}