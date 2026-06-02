package com.aiblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private RateLimitProperties properties;
    private RateLimitInterceptor interceptor;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setAuth(new RateLimitProperties.Rule(1, 1, 60_000));
        properties.setForumMutation(new RateLimitProperties.Rule(1, 1, 60_000));
        properties.setPublicMutation(new RateLimitProperties.Rule(1, 1, 60_000));
        properties.setAccountMutation(new RateLimitProperties.Rule(1, 1, 60_000));
        registry = new SimpleMeterRegistry();
        AtomicLong now = new AtomicLong(0);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties, now::get);
        RedisRateLimiter redisLimiter = new RedisRateLimiter(properties, (StringRedisTemplate) null);
        interceptor = new RateLimitInterceptor(properties, limiter, redisLimiter, new ObjectMapper(), registry);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksRepeatedLoginAttemptsByIp() throws Exception {
        MockHttpServletRequest first = request("POST", "/api/auth/login");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        MockHttpServletRequest second = request("POST", "/api/auth/login");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(first, firstResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(second, secondResponse, new Object())).isFalse();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(secondResponse.getContentAsString()).contains("请求过于频繁");
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "auth", "outcome", "allowed").count()).isEqualTo(1);
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "auth", "outcome", "rejected").count()).isEqualTo(1);
    }

    @Test
    void blocksRepeatedForumMutationsByUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));
        MockHttpServletRequest first = request("POST", "/api/forum/threads");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletRequest second = request("POST", "/api/forum/threads");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(first, firstResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(second, secondResponse, new Object())).isFalse();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "forum-mutation", "outcome", "rejected").count()).isEqualTo(1);
    }

    @Test
    void ignoresForumReadRequests() throws Exception {
        MockHttpServletRequest first = request("GET", "/api/forum/threads");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletRequest second = request("GET", "/api/forum/threads");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(first, firstResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(second, secondResponse, new Object())).isTrue();
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void matchesPathsBehindContextPath() throws Exception {
        MockHttpServletRequest first = request("POST", "/app/api/comments");
        first.setContextPath("/app");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletRequest second = request("POST", "/app/api/comments");
        second.setContextPath("/app");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(first, firstResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(second, secondResponse, new Object())).isFalse();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "public-mutation", "outcome", "allowed").count()).isEqualTo(1);
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "public-mutation", "outcome", "rejected").count()).isEqualTo(1);
    }

    @Test
    void redisStorageFallsBackToLocalLimiterWhenRedisIsUnavailable() throws Exception {
        properties.setStorage("redis");
        MockHttpServletRequest first = request("POST", "/api/auth/login");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletRequest second = request("POST", "/api/auth/login");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(first, firstResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(second, secondResponse, new Object())).isFalse();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "auth", "outcome", "redis_fallback").count()).isEqualTo(2);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("203.0.113.8");
        return request;
    }

    private UsernamePasswordAuthenticationToken auth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
