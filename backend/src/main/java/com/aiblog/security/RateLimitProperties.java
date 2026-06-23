package com.aiblog.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String storage = "local";
    private String redisKeyPrefix = "ai-blog:rate-limit";
    private long cleanupIntervalMs = 60_000;
    private Rule auth = new Rule(5, 5, 60_000);
    private Rule publicMutation = new Rule(10, 10, 60_000);
    private Rule forumMutation = new Rule(30, 30, 60_000);
    private Rule accountMutation = new Rule(60, 60, 60_000);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public Rule getAuth() {
        return auth;
    }

    public void setAuth(Rule auth) {
        this.auth = auth;
    }

    public Rule getPublicMutation() {
        return publicMutation;
    }

    public void setPublicMutation(Rule publicMutation) {
        this.publicMutation = publicMutation;
    }

    public Rule getForumMutation() {
        return forumMutation;
    }

    public void setForumMutation(Rule forumMutation) {
        this.forumMutation = forumMutation;
    }

    public Rule getAccountMutation() {
        return accountMutation;
    }

    public void setAccountMutation(Rule accountMutation) {
        this.accountMutation = accountMutation;
    }

    public static class Rule {
        private int capacity = 10;
        private int refillTokens = 10;
        private long refillPeriodMs = 60_000;

        public Rule() {
        }

        public Rule(int capacity, int refillTokens, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodMs = refillPeriodMs;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriodMs() {
            return refillPeriodMs;
        }

        public void setRefillPeriodMs(long refillPeriodMs) {
            this.refillPeriodMs = refillPeriodMs;
        }
    }
}
