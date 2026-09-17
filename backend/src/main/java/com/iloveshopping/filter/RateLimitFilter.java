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

/**
 * Token bucket rate limiting per client IP.
 *
 * <p>Each bucket starts full (capacity = burst allowance) and drains one
 * token per request. Tokens refill continuously at a rate of
 * {@code requestsPerMinute / 60.0} tokens per second, so steady traffic at
 * or below the sustained rate passes while bursts above the bucket capacity
 * are rejected with 429.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${security.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${security.rate-limit.api-requests-per-minute:100}")
    private int apiRequestsPerMinute;

    /** Burst allowance above the sustained rate, in requests. */
    @Value("${security.rate-limit.burst-capacity:20}")
    private int burstCapacity;

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

        boolean auth = isAuthEndpoint(requestUri);
        int perMinute = auth ? authRequestsPerMinute : apiRequestsPerMinute;
        String key = clientIp + ":" + (auth ? "auth" : "api");

        double refillPerSecond = perMinute / 60.0;
        double capacity = perMinute + burstCapacity;

        TokenBucket bucket = buckets.compute(key, (k, existing) ->
                existing == null ? new TokenBucket(capacity, refillPerSecond) : existing);

        if (!bucket.tryConsume()) {
            long retryAfterSeconds = (long) Math.ceil(bucket.secondsUntilNextToken());
            log.warn("Rate limit exceeded for IP: {} on path: {} ({} req/min, burst {})",
                    clientIp, requestUri, perMinute, (long) capacity);
            writeRateLimitExceeded(response, perMinute, Math.max(1, retryAfterSeconds));
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

    private void writeRateLimitExceeded(HttpServletResponse response, int limit, long retryAfterSeconds) throws IOException {
        ApiResponse.ErrorResponse errorResponse = ApiResponse.ErrorResponse.builder()
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Too many requests. Limit is " + limit + " requests per minute. Retry in " + retryAfterSeconds + "s.")
                .build();

        ApiResponse<Object> apiResponse = ApiResponse.<Object>builder()
                .success(false)
                .error(errorResponse)
                .timestamp(Instant.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }

    /**
     * Continuous-refill token bucket, safe for concurrent use.
     */
    static class TokenBucket {
        private double tokens;
        private final double capacity;
        private final double refillPerSecond;
        private long lastRefillNanos;

        TokenBucket(double capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity; // start full so legitimate first bursts pass
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized double secondsUntilNextToken() {
            refill();
            return Math.max(0, (1.0 - tokens) / refillPerSecond);
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
                lastRefillNanos = now;
            }
        }
    }
}
