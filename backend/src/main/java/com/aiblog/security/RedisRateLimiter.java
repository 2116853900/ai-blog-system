package com.aiblog.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimiter {

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisRateLimiter(RateLimitProperties properties,
                            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    RedisRateLimiter(RateLimitProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(String bucketKey, RateLimitProperties.Rule rule) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate is not available");
        }

        RateLimitProperties.Rule effectiveRule = rule == null ? new RateLimitProperties.Rule() : rule;
        int capacity = Math.max(1, effectiveRule.getCapacity());
        long windowMs = Math.max(1, effectiveRule.getRefillPeriodMs());
        String key = redisKey(bucketKey, windowMs);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMillis(windowMs));
        }
        return count != null && count <= capacity;
    }

    private String redisKey(String bucketKey, long windowMs) {
        String prefix = properties.getRedisKeyPrefix() == null || properties.getRedisKeyPrefix().isBlank()
                ? "ai-blog:rate-limit"
                : properties.getRedisKeyPrefix().trim();
        long window = System.currentTimeMillis() / windowMs;
        return prefix + ":" + normalize(bucketKey) + ":" + window;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9:._-]", "_");
    }
}
