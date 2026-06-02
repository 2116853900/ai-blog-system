package com.aiblog.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean enabled = true;
    private boolean redisEnabled = false;
    private String keyPrefix = "ai-blog";
    private int localMaxSize = 1000;
    private long publicContentTtlMs = 30_000;
    private long searchTtlMs = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getLocalMaxSize() {
        return localMaxSize;
    }

    public void setLocalMaxSize(int localMaxSize) {
        this.localMaxSize = localMaxSize;
    }

    public long getPublicContentTtlMs() {
        return publicContentTtlMs;
    }

    public void setPublicContentTtlMs(long publicContentTtlMs) {
        this.publicContentTtlMs = publicContentTtlMs;
    }

    public long getSearchTtlMs() {
        return searchTtlMs;
    }

    public void setSearchTtlMs(long searchTtlMs) {
        this.searchTtlMs = searchTtlMs;
    }
}
