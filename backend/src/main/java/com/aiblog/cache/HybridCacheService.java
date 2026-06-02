package com.aiblog.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class HybridCacheService {

    private static final Logger log = LoggerFactory.getLogger(HybridCacheService.class);

    private final CacheProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Map<String, LocalEntry> localCache = new ConcurrentHashMap<>();

    @Autowired
    public HybridCacheService(CacheProperties properties,
                              ObjectMapper objectMapper,
                              ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                              MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    public HybridCacheService(CacheProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = null;
        this.meterRegistry = new SimpleMeterRegistry();
        registerGauges();
    }

    HybridCacheService(CacheProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = null;
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        return getOrLoadInternal(key, ttl, loader, value -> objectMapper.readValue(value, type));
    }

    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        return getOrLoadInternal(key, ttl, loader, value -> objectMapper.readValue(value, type));
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrLoadInternal(String key,
                                    Duration ttl,
                                    Supplier<T> loader,
                                    RedisValueReader<T> redisValueReader) {
        if (!properties.isEnabled()) {
            recordCacheEvent("disabled_load");
            return loader.get();
        }

        String fullKey = fullKey(key);
        long expiresAt = System.currentTimeMillis() + normalizedTtl(ttl).toMillis();
        LocalEntry local = localCache.get(fullKey);
        if (local != null && local.expiresAtMs() > System.currentTimeMillis()) {
            recordCacheEvent("local_hit");
            return (T) local.value();
        }

        T redisValue = readRedis(fullKey, redisValueReader);
        if (redisValue != null) {
            recordCacheEvent("redis_hit");
            putLocal(fullKey, redisValue, expiresAt);
            return redisValue;
        }

        recordCacheEvent("loader_load");
        T loaded = loader.get();
        if (loaded != null) {
            putLocal(fullKey, loaded, expiresAt);
            writeRedis(fullKey, loaded, normalizedTtl(ttl));
        }
        return loaded;
    }

    public void evict(String key) {
        recordCacheEvent("evict");
        String fullKey = fullKey(key);
        localCache.remove(fullKey);
        if (redisTemplate != null && properties.isRedisEnabled()) {
            try {
                redisTemplate.delete(fullKey);
            } catch (RuntimeException e) {
                recordCacheEvent("redis_evict_error");
                log.debug("Redis cache delete failed for {}: {}", fullKey, e.getMessage());
            }
        }
    }

    public void evictByPrefix(String prefix) {
        recordCacheEvent("evict_prefix");
        String fullPrefix = fullKey(prefix);
        localCache.keySet().removeIf(key -> key.startsWith(fullPrefix));
        if (redisTemplate == null || !properties.isRedisEnabled()) {
            return;
        }
        try {
            List<String> keys = scanRedisKeys(fullPrefix + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            recordCacheEvent("redis_evict_error");
            log.debug("Redis cache prefix delete failed for {}: {}", fullPrefix, e.getMessage());
        }
    }

    int localSize() {
        return localCache.size();
    }

    private <T> T readRedis(String fullKey, RedisValueReader<T> redisValueReader) {
        if (redisTemplate == null || !properties.isRedisEnabled()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(fullKey);
            return json == null ? null : redisValueReader.read(json);
        } catch (Exception e) {
            recordCacheEvent("redis_read_error");
            log.debug("Redis cache read failed for {}: {}", fullKey, e.getMessage());
            return null;
        }
    }

    private void writeRedis(String fullKey, Object value, Duration ttl) {
        if (redisTemplate == null || !properties.isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(fullKey, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            recordCacheEvent("redis_write_error");
            log.debug("Redis cache write failed for {}: {}", fullKey, e.getMessage());
        }
    }

    private void registerGauges() {
        Gauge.builder("aiblog.cache.local.size", localCache, Map::size)
                .description("Current local cache entry count")
                .register(meterRegistry);
    }

    private void recordCacheEvent(String event) {
        meterRegistry.counter("aiblog.cache.events", "event", event).increment();
    }

    private void putLocal(String fullKey, Object value, long expiresAt) {
        int maxSize = Math.max(1, properties.getLocalMaxSize());
        if (localCache.size() >= maxSize && !localCache.containsKey(fullKey)) {
            evictSomeLocalEntries(maxSize);
        }
        localCache.put(fullKey, new LocalEntry(value, expiresAt));
    }

    private void evictSomeLocalEntries(int maxSize) {
        long now = System.currentTimeMillis();
        localCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() <= now);
        if (localCache.size() < maxSize) {
            return;
        }
        int removeCount = Math.max(1, maxSize / 10);
        Iterator<String> iterator = localCache.keySet().iterator();
        while (iterator.hasNext() && removeCount-- > 0) {
            iterator.next();
            iterator.remove();
        }
    }

    private List<String> scanRedisKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<List<String>>) connection -> {
            List<String> keys = new ArrayList<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
    }

    private String fullKey(String key) {
        String prefix = properties.getKeyPrefix() == null || properties.getKeyPrefix().isBlank()
                ? "ai-blog"
                : properties.getKeyPrefix().trim();
        return prefix + ":" + key;
    }

    private Duration normalizedTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofSeconds(1);
        }
        return ttl;
    }

    private record LocalEntry(Object value, long expiresAtMs) {
    }

    @FunctionalInterface
    private interface RedisValueReader<T> {
        T read(String value) throws Exception;
    }
}
