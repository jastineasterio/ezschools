package com.edumanage.config.rate_limit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(2, 60, 2);
        rateLimiter = new SlidingWindowRateLimiter(properties);
    }

    @Test
    void testAllowsWithinLimit() {
        assertTrue(rateLimiter.tryConsume("test-key"));
        assertTrue(rateLimiter.tryConsume("test-key"));
    }

    @Test
    void testBlocksWhenExceeded() {
        assertTrue(rateLimiter.tryConsume("test-key"));
        assertTrue(rateLimiter.tryConsume("test-key"));
        assertFalse(rateLimiter.tryConsume("test-key"));
    }

    @Test
    void testDifferentKeysIndependent() {
        assertTrue(rateLimiter.tryConsume("key-a"));
        assertTrue(rateLimiter.tryConsume("key-a"));
        assertTrue(rateLimiter.tryConsume("key-b"));
        assertTrue(rateLimiter.tryConsume("key-b"));
        assertFalse(rateLimiter.tryConsume("key-a"));
        assertFalse(rateLimiter.tryConsume("key-b"));
    }
}
