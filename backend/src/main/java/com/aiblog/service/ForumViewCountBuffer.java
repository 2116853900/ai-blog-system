package com.aiblog.service;

import com.aiblog.repository.ForumThreadRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ForumViewCountBuffer {

    private static final Logger log = LoggerFactory.getLogger(ForumViewCountBuffer.class);

    private final ForumThreadRepository threadRepo;
    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final boolean redisEnabled;
    private final String redisKey;
    private final int maxBatchSize;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<Long, LongAdder> localDeltas = new ConcurrentHashMap<>();

    @Autowired
    public ForumViewCountBuffer(ForumThreadRepository threadRepo,
                                ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                @Value("${app.forum.view-buffer.enabled:true}") boolean enabled,
                                @Value("${app.forum.view-buffer.redis-enabled:false}") boolean redisEnabled,
                                @Value("${app.forum.view-buffer.redis-key:ai-blog:forum:view-count-delta}") String redisKey,
                                @Value("${app.forum.view-buffer.max-batch-size:500}") int maxBatchSize,
                                MeterRegistry meterRegistry) {
        this.threadRepo = threadRepo;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.enabled = enabled;
        this.redisEnabled = redisEnabled;
        this.redisKey = redisKey == null || redisKey.isBlank() ? "ai-blog:forum:view-count-delta" : redisKey;
        this.maxBatchSize = Math.max(1, maxBatchSize);
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    ForumViewCountBuffer(ForumThreadRepository threadRepo,
                         StringRedisTemplate redisTemplate,
                         boolean enabled,
                         boolean redisEnabled,
                         String redisKey,
                         int maxBatchSize) {
        this(threadRepo, redisTemplate, enabled, redisEnabled, redisKey, maxBatchSize, new SimpleMeterRegistry());
    }

    ForumViewCountBuffer(ForumThreadRepository threadRepo,
                         StringRedisTemplate redisTemplate,
                         boolean enabled,
                         boolean redisEnabled,
                         String redisKey,
                         int maxBatchSize,
                         MeterRegistry meterRegistry) {
        this.threadRepo = threadRepo;
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.redisEnabled = redisEnabled;
        this.redisKey = redisKey == null || redisKey.isBlank() ? "ai-blog:forum:view-count-delta" : redisKey;
        this.maxBatchSize = Math.max(1, maxBatchSize);
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    public void recordView(Long threadId) {
        if (threadId == null) {
            return;
        }
        if (!enabled) {
            threadRepo.incrementViewCount(threadId);
            recordViewEvent("direct");
            return;
        }
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForHash().increment(redisKey, threadId.toString(), 1L);
                recordViewEvent("redis");
                return;
            } catch (RuntimeException e) {
                recordViewEvent("local_fallback");
                log.debug("Redis view count buffer unavailable, falling back to local buffer: {}", e.getMessage());
            }
        } else {
            recordViewEvent("local");
        }
        localDeltas.computeIfAbsent(threadId, ignored -> new LongAdder()).increment();
    }

    @Scheduled(fixedDelayString = "${app.forum.view-buffer.flush-interval-ms:5000}")
    public void scheduledFlush() {
        flush();
    }

    @Transactional
    public void flush() {
        if (!enabled) {
            return;
        }
        flushRedisDeltas();
        flushLocalDeltas();
    }

    @PreDestroy
    public void flushBeforeShutdown() {
        try {
            flush();
        } catch (RuntimeException e) {
            log.warn("刷新浏览数缓冲失败: {}", e.getMessage());
        }
    }

    int localBucketCount() {
        return localDeltas.size();
    }

    private void flushRedisDeltas() {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        try {
            List<Object> fields = scanRedisFields();
            int processed = 0;
            long flushedDelta = 0;
            for (Object field : fields) {
                if (processed >= maxBatchSize) {
                    break;
                }
                String threadIdText = String.valueOf(field);
                String deltaText = (String) redisTemplate.opsForHash().get(redisKey, threadIdText);
                long delta = parsePositiveLong(deltaText);
                if (delta <= 0) {
                    redisTemplate.opsForHash().delete(redisKey, threadIdText);
                    continue;
                }
                incrementViewCountBy(parseThreadId(threadIdText), delta);
                Long remaining = redisTemplate.opsForHash().increment(redisKey, threadIdText, -delta);
                if (remaining != null && remaining <= 0) {
                    redisTemplate.opsForHash().delete(redisKey, threadIdText);
                }
                flushedDelta += delta;
                processed++;
            }
            recordFlushEvent("redis", "success");
            if (flushedDelta > 0) {
                recordFlushedDelta("redis", flushedDelta);
            }
        } catch (RuntimeException e) {
            recordFlushEvent("redis", "error");
            log.debug("Redis view count flush skipped: {}", e.getMessage());
        }
    }

    private void flushLocalDeltas() {
        if (localDeltas.isEmpty()) {
            return;
        }
        Map<Long, Long> snapshot = new HashMap<>();
        int processed = 0;
        for (Map.Entry<Long, LongAdder> entry : localDeltas.entrySet()) {
            if (processed >= maxBatchSize) {
                break;
            }
            LongAdder adder = entry.getValue();
            long delta = adder.sumThenReset();
            if (delta > 0) {
                snapshot.put(entry.getKey(), delta);
                processed++;
            }
            if (adder.sum() == 0) {
                localDeltas.remove(entry.getKey(), adder);
            }
        }
        Map<Long, Long> pending = new HashMap<>(snapshot);
        long flushedDelta = 0;
        try {
            for (Map.Entry<Long, Long> entry : snapshot.entrySet()) {
                incrementViewCountBy(entry.getKey(), entry.getValue());
                pending.remove(entry.getKey());
                flushedDelta += entry.getValue();
            }
            recordFlushEvent("local", "success");
            if (flushedDelta > 0) {
                recordFlushedDelta("local", flushedDelta);
            }
        } catch (RuntimeException e) {
            pending.forEach((threadId, delta) -> localDeltas.computeIfAbsent(threadId, ignored -> new LongAdder()).add(delta));
            recordFlushEvent("local", "error");
            throw e;
        }
    }

    private void registerGauges() {
        Gauge.builder("aiblog.forum.view_buffer.local_buckets", localDeltas, Map::size)
                .description("Current local view buffer bucket count")
                .register(meterRegistry);
    }

    private void recordViewEvent(String backend) {
        meterRegistry.counter("aiblog.forum.view_buffer.recorded", "backend", backend).increment();
    }

    private void recordFlushEvent(String backend, String outcome) {
        meterRegistry.counter("aiblog.forum.view_buffer.flushes", "backend", backend, "outcome", outcome).increment();
    }

    private void recordFlushedDelta(String backend, long delta) {
        meterRegistry.counter("aiblog.forum.view_buffer.flushed_delta", "backend", backend).increment(delta);
    }

    private List<Object> scanRedisFields() {
        List<Object> fields = new ArrayList<>();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(redisKey, ScanOptions.scanOptions().count(maxBatchSize).build())) {
            while (cursor.hasNext() && fields.size() < maxBatchSize) {
                fields.add(cursor.next().getKey());
            }
        }
        return fields;
    }

    private void incrementViewCountBy(Long threadId, Long delta) {
        if (threadId != null && delta != null && delta > 0) {
            threadRepo.incrementViewCountBy(threadId, delta);
        }
    }

    private long parsePositiveLong(String value) {
        try {
            return value == null ? 0 : Math.max(0, Long.parseLong(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long parseThreadId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
