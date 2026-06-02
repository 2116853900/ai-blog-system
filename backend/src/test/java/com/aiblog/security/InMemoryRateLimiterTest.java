package com.aiblog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private RateLimitProperties properties;
    private AtomicLong now;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setCleanupIntervalMs(60_000);
        now = new AtomicLong(0);
    }

    @Test
    void consumesCapacityAndBlocksUntilRefill() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties, now::get);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule(2, 1, 1_000);

        assertThat(limiter.tryConsume("auth:ip:127.0.0.1", rule)).isTrue();
        assertThat(limiter.tryConsume("auth:ip:127.0.0.1", rule)).isTrue();
        assertThat(limiter.tryConsume("auth:ip:127.0.0.1", rule)).isFalse();

        now.addAndGet(1_000);

        assertThat(limiter.tryConsume("auth:ip:127.0.0.1", rule)).isTrue();
        assertThat(limiter.tryConsume("auth:ip:127.0.0.1", rule)).isFalse();
    }

    @Test
    void disabledLimiterAlwaysAllowsRequests() {
        properties.setEnabled(false);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties, now::get);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule(1, 1, 60_000);

        assertThat(limiter.tryConsume("forum:user:alice", rule)).isTrue();
        assertThat(limiter.tryConsume("forum:user:alice", rule)).isTrue();
        assertThat(limiter.tryConsume("forum:user:alice", rule)).isTrue();
    }

    @Test
    void cleanupRemovesExpiredBuckets() {
        properties.setCleanupIntervalMs(10);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties, now::get);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule(1, 1, 60_000);

        assertThat(limiter.tryConsume("public:ip:1.1.1.1", rule)).isTrue();
        assertThat(limiter.bucketCount()).isEqualTo(1);

        now.set(21);

        assertThat(limiter.tryConsume("public:ip:2.2.2.2", rule)).isTrue();
        assertThat(limiter.bucketCount()).isEqualTo(1);
    }
}
