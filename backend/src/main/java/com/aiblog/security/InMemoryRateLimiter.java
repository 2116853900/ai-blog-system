package com.aiblog.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Component
public class InMemoryRateLimiter {

    private final RateLimitProperties properties;
    private final LongSupplier currentTimeMillis;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong();

    @Autowired
    public InMemoryRateLimiter(RateLimitProperties properties) {
        this(properties, System::currentTimeMillis);
    }

    InMemoryRateLimiter(RateLimitProperties properties, LongSupplier currentTimeMillis) {
        this.properties = properties;
        this.currentTimeMillis = currentTimeMillis;
    }

    public boolean tryConsume(String bucketKey, RateLimitProperties.Rule rule) {
        if (!properties.isEnabled()) {
            return true;
        }

        RateLimitProperties.Rule effectiveRule = rule == null ? new RateLimitProperties.Rule() : rule;
        int capacity = Math.max(1, effectiveRule.getCapacity());
        int refillTokens = Math.max(1, effectiveRule.getRefillTokens());
        long refillPeriodMs = Math.max(1, effectiveRule.getRefillPeriodMs());
        String normalizedKey = normalizeKey(bucketKey);
        long now = currentTimeMillis.getAsLong();

        cleanupIfNeeded(now);

        Bucket bucket = buckets.computeIfAbsent(normalizedKey, ignored -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.capacity = capacity;
            if (bucket.tokens > capacity) {
                bucket.tokens = capacity;
            }
            refill(bucket, now, capacity, refillTokens, refillPeriodMs);
            bucket.lastAccessedAt = now;
            if (bucket.tokens <= 0) {
                return false;
            }
            bucket.tokens--;
            return true;
        }
    }

    int bucketCount() {
        return buckets.size();
    }

    private void refill(Bucket bucket, long now, int capacity, int refillTokens, long refillPeriodMs) {
        long elapsed = now - bucket.lastRefillAt;
        if (elapsed < refillPeriodMs) {
            return;
        }
        long periods = elapsed / refillPeriodMs;
        long tokensToAdd = periods * (long) refillTokens;
        bucket.tokens = (int) Math.min(capacity, bucket.tokens + tokensToAdd);
        bucket.lastRefillAt += periods * refillPeriodMs;
    }

    private void cleanupIfNeeded(long now) {
        long cleanupIntervalMs = Math.max(1, properties.getCleanupIntervalMs());
        long previousCleanup = lastCleanupAt.get();
        if (now - previousCleanup < cleanupIntervalMs) {
            return;
        }
        if (!lastCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }
        long expiresBefore = now - cleanupIntervalMs;
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            if (entry.getValue().lastAccessedAt < expiresBefore) {
                buckets.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private String normalizeKey(String bucketKey) {
        if (bucketKey == null || bucketKey.isBlank()) {
            return "unknown";
        }
        return bucketKey;
    }

    private static class Bucket {
        private int capacity;
        private int tokens;
        private long lastRefillAt;
        private volatile long lastAccessedAt;

        private Bucket(int capacity, long now) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillAt = now;
            this.lastAccessedAt = now;
        }
    }
}
