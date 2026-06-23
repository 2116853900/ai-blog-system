package com.aiblog.service;

import com.aiblog.repository.ForumThreadRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumViewCountBufferTest {

    @Mock
    private ForumThreadRepository threadRepo;

    private ForumViewCountBuffer buffer;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        buffer = new ForumViewCountBuffer(threadRepo, (StringRedisTemplate) null, true, false, "test:view-count", 100, registry);
    }

    @Test
    void localBufferAggregatesViewsAndFlushesOneDelta() {
        buffer.recordView(10L);
        buffer.recordView(10L);
        buffer.recordView(10L);

        assertThat(buffer.localBucketCount()).isEqualTo(1);
        assertThat(registry.counter("aiblog.forum.view_buffer.recorded", "backend", "local").count()).isEqualTo(3);
        assertThat(registry.get("aiblog.forum.view_buffer.local_buckets").gauge().value()).isEqualTo(1);

        buffer.flush();

        verify(threadRepo).incrementViewCountBy(10L, 3L);
        verify(threadRepo, never()).incrementViewCount(10L);
        assertThat(buffer.localBucketCount()).isZero();
        assertThat(registry.counter("aiblog.forum.view_buffer.flushes", "backend", "local", "outcome", "success").count()).isEqualTo(1);
        assertThat(registry.counter("aiblog.forum.view_buffer.flushed_delta", "backend", "local").count()).isEqualTo(3);
        assertThat(registry.get("aiblog.forum.view_buffer.local_buckets").gauge().value()).isZero();
    }

    @Test
    void disabledBufferWritesThroughImmediately() {
        ForumViewCountBuffer disabled = new ForumViewCountBuffer(threadRepo, (StringRedisTemplate) null, false, false, "test:view-count", 100, registry);

        disabled.recordView(10L);

        verify(threadRepo).incrementViewCount(10L);
        verify(threadRepo, never()).incrementViewCountBy(10L, 1L);
        assertThat(disabled.localBucketCount()).isZero();
        assertThat(registry.counter("aiblog.forum.view_buffer.recorded", "backend", "direct").count()).isEqualTo(1);
    }

    @Test
    void nullThreadIdIsIgnored() {
        buffer.recordView(null);
        buffer.flush();

        verify(threadRepo, never()).incrementViewCountBy(null, 1L);
        assertThat(buffer.localBucketCount()).isZero();
    }

    @Test
    void failedLocalFlushRestoresDeltaForNextAttempt() {
        buffer.recordView(10L);
        when(threadRepo.incrementViewCountBy(10L, 1L)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> buffer.flush()).isInstanceOf(RuntimeException.class);

        assertThat(buffer.localBucketCount()).isEqualTo(1);
        assertThat(registry.counter("aiblog.forum.view_buffer.flushes", "backend", "local", "outcome", "error").count()).isEqualTo(1);
    }
}
