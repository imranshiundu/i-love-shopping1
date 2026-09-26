package com.iloveshopping.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token bucket rate limiter — unit tests for the bucket mechanics
 * (continuous refill, burst capacity, recovery time).
 */
class RateLimitFilterTest {

    @Test
    void newBucketStartsFullAndAllowsBurst() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(30, 10.0 / 60); // 10/min + burst 20
        for (int i = 0; i < 30; i++) {
            assertTrue(bucket.tryConsume(), "initial capacity of 30 must be consumable, failed at " + i);
        }
    }

    @Test
    void bucketRejectsWhenEmpty() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(3, 0.0001); // near-zero refill
        for (int i = 0; i < 3; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume(), "bucket must be empty after capacity is drained");
    }

    @Test
    void bucketRefillsOverTime() throws InterruptedException {
        // 1 token/second refill
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(1, 1.0);
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume(), "drained immediately after first token");
        Thread.sleep(1100);
        assertTrue(bucket.tryConsume(), "token must regenerate after the refill interval");
    }

    @Test
    void refillNeverExceedsCapacity() throws InterruptedException {
        // 100 tokens/sec refill into a capacity-2 bucket. Sleep 50ms — the
        // raw refill would add 5 tokens, so only the capacity cap stops the
        // bucket from holding more than 2.
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(2, 100.0);
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        Thread.sleep(50); // would overflow to 7 tokens without the cap
        assertTrue(bucket.tryConsume(), "capped refill restores capacity tokens");
        assertTrue(bucket.tryConsume(), "both capacity tokens are consumable");
        assertFalse(bucket.tryConsume(), "bucket must hold at most capacity tokens (2), not refilled overflow");
    }

    @Test
    void secondsUntilNextTokenIsPositiveWhenEmpty() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(1, 1.0);
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.secondsUntilNextToken() > 0, "must report positive wait time when empty");
    }
}
