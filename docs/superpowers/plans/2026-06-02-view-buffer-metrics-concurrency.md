# View Buffer Metrics Concurrency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为论坛热帖浏览数补充缓冲批量落库，并暴露基础运行指标，继续降低高并发下 MySQL 热行写压力。

**Architecture:** 保留现有 Controller-Service-Repository 分层，新增 `metrics`/service 基础设施组件处理浏览增量聚合。帖子详情只记录浏览事件，定时任务批量调用仓储原子增量更新；Redis 可选作为跨实例聚合后端，本地内存作为默认兜底。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Spring Data Redis, Spring Actuator, Micrometer, MySQL 8.

---

## 分析

当前帖子详情每次请求都会直接调用 `ForumThreadRepository.incrementViewCount(id)`，这已经是原子更新，但在热帖场景仍会把每次浏览都变成一次 MySQL 行写。高并发下同一帖子行会成为写热点，影响列表/详情查询和其他互动写操作。

推荐改造：
- 详情请求只记录浏览增量，不直接写数据库。
- 默认使用本地 `ConcurrentHashMap<Long, LongAdder>` 聚合增量。
- Redis 开启时使用 Redis hash 聚合，适合多实例部署。
- 定时任务每隔数秒批量 flush，调用 `update ForumThread set viewCount = viewCount + :delta where id = :id`。
- 应用停止时尽力 flush 本地增量。
- 通过 Actuator/Micrometer 暴露 DB pool、缓存/浏览缓冲、限流等指标。

## 文件结构

- Modify: `backend/pom.xml`
  - 增加 `spring-boot-starter-actuator`。
- Modify: `backend/src/main/resources/application.yml`
  - 增加浏览缓冲配置和 actuator 暴露配置。
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
  - 增加 `incrementViewCountBy(id, delta)`。
- Create: `backend/src/main/java/com/aiblog/service/ForumViewCountBuffer.java`
  - 记录浏览事件、定时 flush、关闭时 flush。
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
  - `incrementViewCount()` 改为记录缓冲。
- Test: `backend/src/test/java/com/aiblog/service/ForumViewCountBufferTest.java`
  - 覆盖本地缓冲、flush、禁用时直接落库、Redis 异常回退。

## Task 1: 配置与依赖

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`

- [x] **Step 1: 增加 Actuator**

添加 `spring-boot-starter-actuator`。

- [x] **Step 2: 增加浏览缓冲配置**

```yaml
app:
  forum:
    view-buffer:
      enabled: ${FORUM_VIEW_BUFFER_ENABLED:true}
      redis-enabled: ${FORUM_VIEW_BUFFER_REDIS_ENABLED:false}
      redis-key: ${FORUM_VIEW_BUFFER_REDIS_KEY:ai-blog:forum:view-count-delta}
      flush-interval-ms: ${FORUM_VIEW_BUFFER_FLUSH_INTERVAL_MS:5000}
      max-batch-size: ${FORUM_VIEW_BUFFER_MAX_BATCH_SIZE:500}
```

- [x] **Step 3: 暴露基础 actuator**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

## Task 2: 浏览数批量原子更新

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
- Create: `backend/src/main/java/com/aiblog/service/ForumViewCountBuffer.java`

- [x] **Step 1: 增加批量增量仓储方法**

`ForumThreadRepository` 添加：

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.viewCount = t.viewCount + :delta where t.id = :id")
int incrementViewCountBy(@Param("id") Long id, @Param("delta") long delta);
```

- [x] **Step 2: 创建缓冲服务**

`ForumViewCountBuffer.recordView(id)`：
- 缓冲禁用时直接调用 `incrementViewCount(id)`。
- Redis 开启时 `HINCRBY redisKey id 1`。
- Redis 失败或未开启时本地 `LongAdder.increment()`。

`flush()`：
- 先 flush Redis hash，再 flush 本地 map。
- 每个帖子调用 `incrementViewCountBy(id, delta)`。
- flush 后清理已落库增量。

## Task 3: 接入帖子详情浏览

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`

- [x] **Step 1: 注入缓冲服务**

构造器增加 `ForumViewCountBuffer`。

- [x] **Step 2: 修改浏览递增**

`incrementViewCount(id)` 改为 `viewCountBuffer.recordView(id)`。

## Task 4: 测试与验证

**Files:**
- Create: `backend/src/test/java/com/aiblog/service/ForumViewCountBufferTest.java`

- [x] **Step 1: 测试本地缓冲 flush**

记录同一帖子多次浏览，flush 后只调用一次 `incrementViewCountBy(id, delta)`。

- [x] **Step 2: 测试禁用缓冲直接落库**

禁用时 `recordView()` 直接调用 `incrementViewCount(id)`。

- [x] **Step 3: 编译与测试**

Run: `mvn -q test -Dtest=ForumViewCountBufferTest`
Expected: `BUILD SUCCESS`

Run: `mvn -q test`
Expected: `BUILD SUCCESS`

## 后续建议

- 引入登录用户/IP 维度的短 TTL 去重，避免同一客户端频繁刷新刷高浏览量。
- 后台列表分页化，减少大表一次性加载。
- 对搜索迁移 FULLTEXT/专用搜索引擎。
