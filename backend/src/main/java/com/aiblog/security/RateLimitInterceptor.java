package com.aiblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitProperties properties;
    private final InMemoryRateLimiter rateLimiter;
    private final RedisRateLimiter redisRateLimiter;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RateLimitInterceptor(RateLimitProperties properties,
                                InMemoryRateLimiter rateLimiter,
                                RedisRateLimiter redisRateLimiter,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.redisRateLimiter = redisRateLimiter;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        RuleSelection selection = selectRule(request);
        if (selection == null) {
            return true;
        }

        String bucketKey = selection.name() + ":" + requesterKey(request);
        if (tryConsume(bucketKey, selection.rule())) {
            recordRateLimitEvent(selection.name(), "allowed");
            return true;
        }

        recordRateLimitEvent(selection.name(), "rejected");
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds(selection.rule())));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", "请求过于频繁，请稍后再试"));
        return false;
    }

    private boolean tryConsume(String bucketKey, RateLimitProperties.Rule rule) {
        if ("redis".equalsIgnoreCase(properties.getStorage())) {
            try {
                return redisRateLimiter.tryConsume(bucketKey, rule);
            } catch (RuntimeException e) {
                recordRateLimitEvent(ruleNameFromBucket(bucketKey), "redis_fallback");
                log.debug("Redis rate limiter unavailable, falling back to local limiter: {}", e.getMessage());
            }
        }
        return rateLimiter.tryConsume(bucketKey, rule);
    }

    private RuleSelection selectRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = requestPath(request);

        if ("POST".equals(method) && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path))) {
            return new RuleSelection("auth", properties.getAuth());
        }
        if ("POST".equals(method) && ("/api/comments".equals(path)
                || "/api/submissions".equals(path)
                || "/api/reports".equals(path))) {
            return new RuleSelection("public-mutation", properties.getPublicMutation());
        }
        if (isMutation(method) && path.startsWith("/api/forum/")) {
            return new RuleSelection("forum-mutation", properties.getForumMutation());
        }
        if (("POST".equals(method) || "DELETE".equals(method))
                && (path.startsWith("/api/account/resource-favorites/")
                || path.startsWith("/api/account/resource-reviews/"))) {
            return new RuleSelection("account-mutation", properties.getAccountMutation());
        }
        return null;
    }

    private boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private long retryAfterSeconds(RateLimitProperties.Rule rule) {
        long refillPeriodMs = Math.max(1000, rule == null ? 60_000 : rule.getRefillPeriodMs());
        return Math.max(1, (refillPeriodMs + 999) / 1000);
    }

    private void recordRateLimitEvent(String ruleName, String outcome) {
        meterRegistry.counter("aiblog.rate_limit.requests", "rule", ruleName, "outcome", outcome).increment();
    }

    private String ruleNameFromBucket(String bucketKey) {
        int separator = bucketKey.indexOf(':');
        return separator > 0 ? bucketKey.substring(0, separator) : "unknown";
    }

    private String requesterKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private record RuleSelection(String name, RateLimitProperties.Rule rule) {
    }
}
