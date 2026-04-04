package com.bitsoft.originmcp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter using sliding window algorithm.
 * Tracks request counts per client (identified by client ID).
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    @Value("${mcp.security.rate-limit.default:60}")
    private int defaultLimit;

    @Value("${mcp.security.rate-limit.window-seconds:60}")
    private int windowSeconds;

    // Client ID -> SlidingWindowCounter
    private final ConcurrentHashMap<String, SlidingWindowCounter> limiters = new ConcurrentHashMap<>();

    /**
     * Check if a request is allowed for the given client.
     *
     * @param clientId The client identifier
     * @param limit The rate limit (requests per window)
     * @return true if request is allowed, false if rate limited
     */
    public boolean tryAcquire(String clientId, int limit) {
        if (limit <= 0) {
            return true; // Rate limiting disabled for this client
        }

        SlidingWindowCounter counter = limiters.computeIfAbsent(clientId,
            k -> new SlidingWindowCounter(windowSeconds * 1000L));
        return counter.tryAcquire(limit);
    }

    /**
     * Check if a request is allowed using the default limit.
     */
    public boolean tryAcquire(String clientId) {
        return tryAcquire(clientId, defaultLimit);
    }

    /**
     * Get remaining quota for a client.
     */
    public int getRemainingQuota(String clientId, int limit) {
        SlidingWindowCounter counter = limiters.get(clientId);
        if (counter == null) {
            return limit;
        }
        return counter.getRemaining(limit);
    }

    /**
     * Clear rate limit data for a client.
     */
    public void reset(String clientId) {
        limiters.remove(clientId);
    }

    /**
     * Clear all rate limit data.
     */
    public void resetAll() {
        limiters.clear();
    }

    /**
     * Sliding window counter implementation.
     */
    private static class SlidingWindowCounter {
        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart;

        SlidingWindowCounter(long windowMillis) {
            this.windowMillis = windowMillis;
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                // Reset window
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }

        synchronized int getRemaining(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMillis) {
                return limit;
            }
            return Math.max(0, limit - count.get());
        }
    }
}
