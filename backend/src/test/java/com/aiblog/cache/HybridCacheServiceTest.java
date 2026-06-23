package com.aiblog.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HybridCacheServiceTest {

    private CacheProperties properties;
    private HybridCacheService cacheService;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new CacheProperties();
        properties.setKeyPrefix("test-cache");
        properties.setRedisEnabled(false);
        properties.setLocalMaxSize(10);
        registry = new SimpleMeterRegistry();
        cacheService = new HybridCacheService(properties, new ObjectMapper(), registry);
    }

    @Test
    void returnsLocalCachedValueWithoutCallingLoaderAgain() {
        AtomicInteger loads = new AtomicInteger();

        String first = cacheService.getOrLoad("public:posts:list", String.class, Duration.ofSeconds(1), () -> {
            loads.incrementAndGet();
            return "posts";
        });
        String second = cacheService.getOrLoad("public:posts:list", String.class, Duration.ofSeconds(1), () -> {
            loads.incrementAndGet();
            return "new-posts";
        });

        assertThat(first).isEqualTo("posts");
        assertThat(second).isEqualTo("posts");
        assertThat(loads).hasValue(1);
        assertThat(cacheService.localSize()).isEqualTo(1);
        assertThat(registry.counter("aiblog.cache.events", "event", "loader_load").count()).isEqualTo(1);
        assertThat(registry.counter("aiblog.cache.events", "event", "local_hit").count()).isEqualTo(1);
        assertThat(registry.get("aiblog.cache.local.size").gauge().value()).isEqualTo(1);
    }

    @Test
    void concurrentMissesForSameKeyOnlyCallLoaderOnce() throws Exception {
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        List<Future<String>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return cacheService.getOrLoad("public:posts:list", String.class, Duration.ofSeconds(1), () -> {
                        loads.incrementAndGet();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return "posts";
                    });
                }));
            }

            assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<String> result : results) {
                assertThat(result.get(1, TimeUnit.SECONDS)).isEqualTo("posts");
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(loads).hasValue(1);
        assertThat(cacheService.localSize()).isEqualTo(1);
    }

    @Test
    void evictByPrefixRemovesMatchingLocalEntries() {
        cacheService.getOrLoad("public:posts:list", String.class, Duration.ofSeconds(1), () -> "posts");
        cacheService.getOrLoad("public:skills:list", String.class, Duration.ofSeconds(1), () -> "skills");

        cacheService.evictByPrefix("public:posts:");

        String posts = cacheService.getOrLoad("public:posts:list", String.class, Duration.ofSeconds(1), () -> "reloaded");
        String skills = cacheService.getOrLoad("public:skills:list", String.class, Duration.ofSeconds(1), () -> "new-skills");

        assertThat(posts).isEqualTo("reloaded");
        assertThat(skills).isEqualTo("skills");
        assertThat(registry.counter("aiblog.cache.events", "event", "evict_prefix").count()).isEqualTo(1);
    }

    @Test
    void disabledCacheAlwaysUsesLoader() {
        properties.setEnabled(false);
        AtomicInteger loads = new AtomicInteger();

        cacheService.getOrLoad("key", String.class, Duration.ofSeconds(1), () -> "value-" + loads.incrementAndGet());
        String second = cacheService.getOrLoad("key", String.class, Duration.ofSeconds(1), () -> "value-" + loads.incrementAndGet());

        assertThat(second).isEqualTo("value-2");
        assertThat(cacheService.localSize()).isZero();
        assertThat(registry.counter("aiblog.cache.events", "event", "disabled_load").count()).isEqualTo(2);
    }
}
