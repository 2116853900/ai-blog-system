# Observability Metrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为缓存、限流、论坛浏览量缓冲和 API 站点检测补齐 Micrometer 指标，让高并发优化具备可观测数据。

**Architecture:** 保留现有 Controller-Service-Repository 分层，不改变业务语义；在缓存、安全和运行服务中注入 `MeterRegistry` 记录命中、拒绝、降级、flush、检测结果等基础指标。指标通过已启用的 Spring Actuator `/actuator/metrics` 和 `/actuator/prometheus` 暴露。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Spring Data Redis, Spring Actuator, Micrometer, MySQL 8.

---

## 项目分析

当前项目是 AI 信息站：前端 Vue 3 + Vite + TypeScript，后端 Spring Boot 3 + Spring Security + JWT + JPA，数据源 MySQL 8，可选 Redis。功能已经覆盖教程、Skill、MCP、公益 API 站点、全站搜索、收藏、评论、论坛、通知、举报和管理后台。

现有架构为典型 Controller-Service-Repository：
- Controller：公开接口、账号接口和后台接口，负责 HTTP 参数与响应。
- Service：业务编排、状态检测、搜索、论坛互动、后台统计。
- Repository：JPA 实体持久化，部分热点写已改为原子更新。
- Infrastructure：`cache` 包提供本地/Redis 混合缓存，`security` 包提供限流，`service.ForumViewCountBuffer` 提供浏览量缓冲。

已完成或正在推进的能力：热点计数原子更新、资源收藏幂等写、写接口限流、Redis/本地缓存、浏览量批量落库、后台列表分页、Actuator/Prometheus 基础依赖。

仍缺失的高价值能力：
- 缓存命中率、Redis 降级、缓存失效次数没有指标，无法判断缓存是否有效。
- 限流允许/拒绝/Redis 回退没有指标，无法定位被刷接口或 Redis 限流异常。
- 浏览量缓冲的记录数、flush 批次数、落库增量和本地积压没有指标，无法判断是否出现积压。
- API 站点检测没有成功/失败/耗时指标，只能看日志。
- 全站搜索长期仍建议迁移 MySQL FULLTEXT 或专用搜索引擎，但在没有指标前不宜盲目重构。

## 架构设计

本轮不做大规模 Clean/Hexagonal 重构，因为当前代码已有大量功能且横切基础设施更急迫。推荐渐进式架构：
- 保持业务服务依赖基础设施服务，不让 Controller 直接埋指标。
- 指标名称按领域前缀组织：`aiblog.cache.*`、`aiblog.rate_limit.*`、`aiblog.forum.view_buffer.*`、`aiblog.api_station.status_check.*`。
- Counter 记录离散事件，Timer 记录耗时，Gauge 记录当前本地缓存大小和浏览缓冲积压。
- 指标标签保持低基数，只使用 `backend`、`rule`、`outcome` 等枚举值，不把用户、IP、URL、帖子 ID 写入指标标签。

## 文件结构

- Modify: `backend/src/main/java/com/aiblog/cache/HybridCacheService.java`
  - 注入 `MeterRegistry`，记录本地命中、Redis 命中、加载、失效和 Redis 异常。
  - 暴露本地缓存大小 gauge。
- Modify: `backend/src/test/java/com/aiblog/cache/HybridCacheServiceTest.java`
  - 使用 `SimpleMeterRegistry` 断言缓存命中与加载指标。
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
  - 记录各规则允许、拒绝和 Redis 回退次数。
- Modify: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`
  - 使用 `SimpleMeterRegistry` 断言限流拒绝指标。
- Modify: `backend/src/main/java/com/aiblog/service/ForumViewCountBuffer.java`
  - 记录浏览事件、flush 批次、落库 delta、Redis flush 异常、本地积压 gauge。
- Modify: `backend/src/test/java/com/aiblog/service/ForumViewCountBufferTest.java`
  - 使用 `SimpleMeterRegistry` 断言浏览记录和 flush 指标。
- Modify: `backend/src/main/java/com/aiblog/service/StatusCheckService.java`
  - 为单站点检测增加 Timer，并按 `UP`/`DOWN` 记录结果 Counter。
- Modify: `backend/src/test/java/com/aiblog/service/StatusCheckServiceTest.java`
  - 使用 `SimpleMeterRegistry` 断言检测结果指标。

## Task 1: 缓存指标

**Files:**
- Modify: `backend/src/main/java/com/aiblog/cache/HybridCacheService.java`
- Modify: `backend/src/test/java/com/aiblog/cache/HybridCacheServiceTest.java`

- [x] **Step 1: 注入 MeterRegistry 并注册 gauge**

`HybridCacheService` 增加字段：

```java
private final MeterRegistry meterRegistry;
```

主构造器接收 `MeterRegistry meterRegistry`，并注册：

```java
Gauge.builder("aiblog.cache.local.size", localCache, Map::size)
        .description("Current local cache entry count")
        .register(meterRegistry);
```

测试构造器使用 `new SimpleMeterRegistry()`。

- [x] **Step 2: 记录缓存事件**

添加辅助方法：

```java
private void recordCacheEvent(String event) {
    meterRegistry.counter("aiblog.cache.events", "event", event).increment();
}
```

事件：`disabled_load`、`local_hit`、`redis_hit`、`loader_load`、`redis_read_error`、`redis_write_error`、`evict`、`evict_prefix`、`redis_evict_error`。

- [x] **Step 3: 更新测试**

`HybridCacheServiceTest` 改为持有 `SimpleMeterRegistry registry`，新增断言：

```java
assertThat(registry.counter("aiblog.cache.events", "event", "loader_load").count()).isEqualTo(1);
assertThat(registry.counter("aiblog.cache.events", "event", "local_hit").count()).isEqualTo(1);
assertThat(registry.get("aiblog.cache.local.size").gauge().value()).isEqualTo(1);
```

## Task 2: 限流指标

**Files:**
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
- Modify: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`

- [x] **Step 1: 注入 MeterRegistry**

构造器增加 `MeterRegistry meterRegistry` 字段，测试使用 `SimpleMeterRegistry`。

- [x] **Step 2: 记录限流事件**

添加：

```java
private void recordRateLimitEvent(String ruleName, String outcome) {
    meterRegistry.counter("aiblog.rate_limit.requests", "rule", ruleName, "outcome", outcome).increment();
}
```

允许请求记录 `allowed`，拒绝请求记录 `rejected`，Redis 限流异常回退记录 `redis_fallback`。

- [x] **Step 3: 更新测试**

在 `blocksRepeatedLoginAttemptsByIp` 断言：

```java
assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "auth", "outcome", "allowed").count()).isEqualTo(1);
assertThat(registry.counter("aiblog.rate_limit.requests", "rule", "auth", "outcome", "rejected").count()).isEqualTo(1);
```

## Task 3: 浏览量缓冲指标

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ForumViewCountBuffer.java`
- Modify: `backend/src/test/java/com/aiblog/service/ForumViewCountBufferTest.java`

- [x] **Step 1: 注入 MeterRegistry 并注册 gauge**

构造器增加 `MeterRegistry meterRegistry`，注册：

```java
Gauge.builder("aiblog.forum.view_buffer.local_buckets", localDeltas, Map::size)
        .description("Current local view buffer bucket count")
        .register(meterRegistry);
```

- [x] **Step 2: 记录记录与 flush 事件**

添加指标：
- `aiblog.forum.view_buffer.recorded`，标签 `backend=direct|redis|local|local_fallback`。
- `aiblog.forum.view_buffer.flushes`，标签 `backend=redis|local`、`outcome=success|error`。
- `aiblog.forum.view_buffer.flushed_delta`，标签 `backend=redis|local`。

- [x] **Step 3: 更新测试**

在本地缓冲测试断言：

```java
assertThat(registry.counter("aiblog.forum.view_buffer.recorded", "backend", "local").count()).isEqualTo(3);
assertThat(registry.counter("aiblog.forum.view_buffer.flushed_delta", "backend", "local").count()).isEqualTo(3);
```

## Task 4: API 状态检测指标

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/StatusCheckService.java`
- Modify: `backend/src/test/java/com/aiblog/service/StatusCheckServiceTest.java`

- [x] **Step 1: 注入 MeterRegistry**

主构造器与测试构造器增加 `MeterRegistry meterRegistry`，测试使用 `SimpleMeterRegistry`。

- [x] **Step 2: 记录检测结果与耗时**

在 `checkAndSave` 开始创建 `Timer.Sample`，结束后记录：

```java
meterRegistry.counter("aiblog.api_station.status_check.results", "status", status.name()).increment();
sample.stop(meterRegistry.timer("aiblog.api_station.status_check.duration", "status", status.name()));
```

- [x] **Step 3: 更新测试**

成功检测断言 `UP` counter 为 2；失败检测断言 `DOWN` counter 为 1。

## Task 5: 编译与测试

**Files:**
- No file changes.

- [x] **Step 1: 运行相关测试**

Run: `mvn -q test -Dtest=HybridCacheServiceTest,RateLimitInterceptorTest,ForumViewCountBufferTest,StatusCheckServiceTest`

Expected: `BUILD SUCCESS`。

- [x] **Step 2: 运行后端编译**

Run: `mvn -q -DskipTests compile`

Expected: `BUILD SUCCESS`。

## 后续建议

- 为全站搜索增加 MySQL FULLTEXT 或独立搜索引擎，并以 `aiblog.search.*` 指标评估命中率、耗时和降级。
- 为后台关键操作日志增加审计查询筛选和导出能力。
- 按 `content`、`community`、`moderation`、`identity`、`ops` 逐步拆包，避免一次性重构影响大量功能。

## Self-Review

Spec coverage: 覆盖了项目分析、架构设计、缺失功能识别、计划文档和本轮执行范围。

Placeholder scan: 没有 `TBD`、`TODO`、`implement later` 等占位内容。

Type consistency: 指标名、构造器依赖、测试使用的 `SimpleMeterRegistry` 在所有任务中保持一致。
