package com.iloveshopping.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iloveshopping.dto.common.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${security.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${security.rate-limit.api-requests-per-minute:100}")
    private int apiRequestsPerMinute;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        int limit = isAuthEndpoint(requestUri) ? authRequestsPerMinute : apiRequestsPerMinute;
        String key = clientIp + ":" + (isAuthEndpoint(requestUri) ? "auth" : "api");

        RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RequestCounter();
            }
            return existing;
        });

        if (counter.incrementAndGet() > limit) {
            log.warn("Rate limit exceeded for IP: {} on path: {} (limit: {})", clientIp, requestUri, limit);
            writeRateLimitExceeded(response, limit);
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.contains("/auth/") || uri.contains("/login") || uri.contains("/register");
    }

    private void writeRateLimitExceeded(HttpServletResponse response, int limit) throws IOException {
        ApiResponse.ErrorResponse errorResponse = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Too many requests. Limit is " + limit + " requests per minute.")
                .build();

        ApiResponse<Object> apiResponse = ApiResponse.<Object>builder()
                .success(false)
                .error(errorResponse)
                .timestamp(Instant.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }

    private static class RequestCounter {
        private long count = 0;
        private final long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60_000; // 1 minute

        long incrementAndGet() {
            return ++count;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_MS;
        }
    }
}
