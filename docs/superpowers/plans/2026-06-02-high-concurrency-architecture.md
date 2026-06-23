# High Concurrency Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AI 信息站补齐高并发基础能力，优先解决热点计数丢更新、写接口缺少限流、运行时并发配置不足的问题。

**Architecture:** 保留现有 Spring Boot Controller-Service-Repository 分层，先在仓储层增加原子更新端口，在服务层保持业务编排，在 Web 层增加限流拦截器。后续大规模 Clean/Hexagonal Architecture 重构按 bounded context 渐进推进，不在本轮直接打散已有模块。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring MVC, Spring Security, Spring Data JPA, MySQL 8, Vue 3 + Vite.

---

## 项目分析

现有功能已经覆盖内容浏览、Skill/MCP/API 资源、教程、论坛、评论、投稿、举报、通知、收藏、后台审核和后台统计。后端是典型 Controller-Service-Repository + JPA Entity 结构，适合先做低侵入高并发增强。

当前主要缺口：
- 热点计数通过 `findById -> setCount -> save` 修改，点赞、收藏、浏览、回复、板块帖子数在并发下可能丢增量。
- 点赞/收藏虽然有唯一约束，但服务层先查后写，高并发下仍可能触发重复插入竞争，或在异常处理不当时影响计数。
- 公开写接口和登录/注册没有统一限流，容易被刷评论、投稿、登录和论坛操作。
- 运行时未配置 Java 21 virtual threads、Hikari 连接池边界和 Tomcat 请求队列，默认配置不利于高并发压测。
- API 站点定时检测目前串行执行，站点多时会拉长调度周期；这是后续阶段任务。
- 管理后台 overview 每次执行多条 count 查询，适合后续加短 TTL 缓存。

## 高并发架构设计

推荐目标架构：
- Web Adapter：控制器只做鉴权上下文、请求参数、响应映射；跨接口的封禁校验、限流放在拦截器。
- Application Service：保持事务边界和用例编排；高频写只调用仓储原子方法，不直接读改写热点实体。
- Persistence Adapter：JPA Repository 暴露 `@Modifying` 原子更新、幂等插入/删除端口；MySQL 负责唯一约束和行级原子性。
- Runtime：Java 21 virtual threads 提升阻塞 MVC 请求承载能力，Hikari pool 控制 DB 并发上限，限流保护写入口。
- Future Bounded Contexts：`content`、`community`、`moderation`、`identity`、`ops`，逐步拆包，避免一次性大重构。

## 第一阶段文件结构

- Modify: `backend/src/main/resources/application.yml`
  - 增加 virtual threads、Tomcat 队列、Hikari pool、限流配置。
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
  - 增加浏览、点赞、收藏、回复计数原子更新方法。
- Modify: `backend/src/main/java/com/aiblog/repository/ForumCategoryRepository.java`
  - 增加板块帖子数原子增减方法。
- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostLikeRepository.java`
  - 增加 MySQL `insert ignore` 幂等插入和 JPQL 批量删除方法。
- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostFavoriteRepository.java`
  - 增加 MySQL `insert ignore` 幂等插入和 JPQL 批量删除方法。
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
  - 使用幂等插入/删除 + 原子计数替代读改写。
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
  - 使用原子浏览数和板块计数更新。
- Modify: `backend/src/main/java/com/aiblog/service/ForumReplyService.java`
  - 使用原子回复计数更新，保留当前楼层计算，楼层分配进入第二阶段。
- Create: `backend/src/main/java/com/aiblog/security/RateLimitProperties.java`
  - 承载限流配置。
- Create: `backend/src/main/java/com/aiblog/security/InMemoryRateLimiter.java`
  - 基于 `ConcurrentHashMap` 的线程安全 token bucket。
- Create: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
  - 对登录、注册、评论、投稿、举报、论坛写操作、资源收藏写操作限流。
- Modify: `backend/src/main/java/com/aiblog/config/WebMvcConfig.java`
  - 注册限流拦截器，顺序早于封禁校验。
- Modify: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`
  - 更新断言，验证原子计数方法被调用。
- Create: `backend/src/test/java/com/aiblog/security/InMemoryRateLimiterTest.java`
  - 覆盖 token bucket 消耗、补充和禁用行为。
- Create: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`
  - 覆盖登录限流、论坛写限流、读请求跳过。

## Task 1: 运行时高并发配置

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [x] **Step 1: 添加配置**

在 `server` 下增加 Tomcat 队列配置，在 `spring` 下增加 virtual threads 和 Hikari pool，在 `app` 下增加限流默认规则：

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: ${TOMCAT_MAX_THREADS:200}
      min-spare: ${TOMCAT_MIN_SPARE_THREADS:20}
    accept-count: ${TOMCAT_ACCEPT_COUNT:200}

spring:
  threads:
    virtual:
      enabled: ${VIRTUAL_THREADS_ENABLED:true}
  datasource:
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE:30}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT_MS:30000}
      max-lifetime: ${DB_CONNECTION_MAX_LIFETIME_MS:1800000}

app:
  rate-limit:
    enabled: ${RATE_LIMIT_ENABLED:true}
    cleanup-interval-ms: ${RATE_LIMIT_CLEANUP_INTERVAL_MS:60000}
    auth:
      capacity: ${RATE_LIMIT_AUTH_CAPACITY:5}
      refill-tokens: ${RATE_LIMIT_AUTH_REFILL_TOKENS:5}
      refill-period-ms: ${RATE_LIMIT_AUTH_REFILL_PERIOD_MS:60000}
    public-mutation:
      capacity: ${RATE_LIMIT_PUBLIC_MUTATION_CAPACITY:10}
      refill-tokens: ${RATE_LIMIT_PUBLIC_MUTATION_REFILL_TOKENS:10}
      refill-period-ms: ${RATE_LIMIT_PUBLIC_MUTATION_REFILL_PERIOD_MS:60000}
    forum-mutation:
      capacity: ${RATE_LIMIT_FORUM_MUTATION_CAPACITY:30}
      refill-tokens: ${RATE_LIMIT_FORUM_MUTATION_REFILL_TOKENS:30}
      refill-period-ms: ${RATE_LIMIT_FORUM_MUTATION_REFILL_PERIOD_MS:60000}
    account-mutation:
      capacity: ${RATE_LIMIT_ACCOUNT_MUTATION_CAPACITY:60}
      refill-tokens: ${RATE_LIMIT_ACCOUNT_MUTATION_REFILL_TOKENS:60}
      refill-period-ms: ${RATE_LIMIT_ACCOUNT_MUTATION_REFILL_PERIOD_MS:60000}
```

- [x] **Step 2: 验证配置语法**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`

## Task 2: 原子计数与幂等互动

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumCategoryRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostLikeRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostFavoriteRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumReplyService.java`
- Test: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`

- [x] **Step 1: Repository 增加原子方法**

`ForumThreadRepository` 增加 `@Modifying` 方法：

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.viewCount = t.viewCount + 1 where t.id = :id")
int incrementViewCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.likeCount = t.likeCount + 1 where t.id = :id")
int incrementLikeCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.likeCount = case when t.likeCount > 0 then t.likeCount - 1 else 0 end where t.id = :id")
int decrementLikeCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.favoriteCount = t.favoriteCount + 1 where t.id = :id")
int incrementFavoriteCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.favoriteCount = case when t.favoriteCount > 0 then t.favoriteCount - 1 else 0 end where t.id = :id")
int decrementFavoriteCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.replyCount = t.replyCount + 1, t.lastReplyUserId = :authorId, t.lastReplyAt = :lastReplyAt where t.id = :id")
int incrementReplyCount(@Param("id") Long id, @Param("authorId") Long authorId, @Param("lastReplyAt") Instant lastReplyAt);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumThread t set t.replyCount = case when t.replyCount > 0 then t.replyCount - 1 else 0 end where t.id = :id")
int decrementReplyCount(@Param("id") Long id);
```

`ForumCategoryRepository` 增加原子方法：

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumCategory c set c.threadCount = c.threadCount + 1 where c.id = :id")
int incrementThreadCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ForumCategory c set c.threadCount = case when c.threadCount > 0 then c.threadCount - 1 else 0 end where c.id = :id")
int decrementThreadCount(@Param("id") Long id);
```

点赞/收藏仓储增加幂等插入和批量删除：

```java
@Modifying
@Query(value = "insert ignore into forum_post_like (post_id, user_id, created_at) values (:postId, :userId, current_timestamp(6))", nativeQuery = true)
int insertIgnore(@Param("postId") Long postId, @Param("userId") Long userId);

@Modifying
@Query("delete from ForumPostLike l where l.postId = :postId and l.userId = :userId")
int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
```

`ForumPostFavoriteRepository` 使用同样结构，表名改为 `forum_post_favorite`，实体类名改为 `ForumPostFavorite`。

- [x] **Step 2: Service 使用原子方法**

`ForumInteractionService.like()` 逻辑：

```java
ensureInteractableThread(threadId);
if (likeRepo.insertIgnore(threadId, userId) > 0) {
    threadRepo.incrementLikeCount(threadId);
}
return getInteraction(threadId, userId);
```

`unlike()` / `favorite()` / `unfavorite()` 同理，删除影响行数大于 0 才递减计数。

`ForumThreadService.incrementViewCount()` 改成：

```java
threadRepo.incrementViewCount(id);
```

板块计数增减改用 `ForumCategoryRepository.incrementThreadCount()` 和 `decrementThreadCount()`。

`ForumReplyService` 回复计数增减改用 `ForumThreadRepository.incrementReplyCount()` 和 `decrementReplyCount()`。

- [x] **Step 3: 更新单元测试**

`ForumInteractionServiceTest` 不再断言实体字段被直接修改，改为验证原子仓储方法调用：

```java
verify(likeRepo, times(1)).insertIgnore(THREAD_ID, USER_ID);
verify(threadRepo, times(1)).incrementLikeCount(THREAD_ID);
verify(threadRepo, never()).save(thread);
```

- [x] **Step 4: 运行测试**

Run: `mvn -q test -Dtest=ForumInteractionServiceTest`
Expected: `BUILD SUCCESS`

## Task 3: 写接口限流

**Files:**
- Create: `backend/src/main/java/com/aiblog/security/RateLimitProperties.java`
- Create: `backend/src/main/java/com/aiblog/security/InMemoryRateLimiter.java`
- Create: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
- Modify: `backend/src/main/java/com/aiblog/config/WebMvcConfig.java`
- Test: `backend/src/test/java/com/aiblog/security/InMemoryRateLimiterTest.java`
- Test: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`

- [x] **Step 1: 创建配置类**

`RateLimitProperties` 包含 `enabled`、`cleanupIntervalMs` 和四个 `Rule`：`auth`、`publicMutation`、`forumMutation`、`accountMutation`。`Rule` 包含 `capacity`、`refillTokens`、`refillPeriodMs`，默认值分别为 `10`、`10`、`60000`。

- [x] **Step 2: 创建 token bucket**

`InMemoryRateLimiter` 使用 `ConcurrentHashMap<String, Bucket>` 存储桶，`tryConsume(String bucketKey, Rule rule)` 在同步桶对象内完成补充和消耗，返回是否允许。每隔 `cleanupIntervalMs` 清理长时间未访问桶，避免内存持续增长。

- [x] **Step 3: 创建拦截器**

`RateLimitInterceptor` 按请求路径选择规则：
- `POST /api/auth/login` 和 `POST /api/auth/register` 使用 `auth`。
- `POST /api/comments`、`POST /api/submissions`、`POST /api/reports` 使用 `publicMutation`。
- `POST|PUT|DELETE /api/forum/**` 使用 `forumMutation`。
- `POST|DELETE /api/account/resource-favorites/**` 使用 `accountMutation`。

限流 key 使用 `ruleName + ":" + authenticatedUsernameOrRemoteIp`，被拒绝时返回 HTTP 429 和 JSON：

```json
{"message":"请求过于频繁，请稍后再试"}
```

- [x] **Step 4: 注册拦截器**

`WebMvcConfig` 构造器注入 `RateLimitInterceptor`，在 `addInterceptors` 中先注册限流，再注册封禁校验。

- [x] **Step 5: 运行测试**

Run: `mvn -q test -Dtest=InMemoryRateLimiterTest,RateLimitInterceptorTest,ForumMutationGuardInterceptorTest`
Expected: `BUILD SUCCESS`

## Task 4: 第一阶段整体验证

**Files:**
- No file changes.

- [x] **Step 1: 后端全量测试**

Run: `mvn -q test`
Expected: `BUILD SUCCESS`

- [x] **Step 2: 后端编译**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`

- [x] **Step 3: 前端构建**

Run: `npm run build`
Working directory: `frontend`
Expected: TypeScript build and Vite build complete.

## 当前验收结论

代码已包含第一阶段大部分高并发能力：Spring virtual threads、Tomcat/Hikari 边界配置、论坛点赞/收藏幂等插入、论坛热点计数原子更新、论坛回复楼层悲观锁、写接口限流、API 站点有界并发检测、后台 overview 短 TTL 缓存。

仍需要补齐的缺口：
- 通用资源收藏仍使用 `exists -> save`，高并发重复点击会撞唯一键；取消收藏也需要直接删除，减少一次实体读取。
- 举报计数仍通过 `findById -> setReportCount + 1 -> save`，高并发举报同一帖子/回复时可能丢增量。
- 评论、论坛列表、收藏列表等热点查询缺少复合索引，数据量上来后容易把压力打到全表扫描和 filesort。
- 限流拒绝响应缺少 `Retry-After`，客户端无法明确退避窗口。

## 追加执行计划：第二阶段补强

### Task 5: 通用资源收藏幂等写

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ResourceFavoriteRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ResourceFavoriteService.java`
- Test: `backend/src/test/java/com/aiblog/service/ResourceFavoriteServiceTest.java`

- [x] **Step 1: Repository 增加 MySQL 幂等插入和直接删除**

`ResourceFavoriteRepository` 添加：`insertIgnore(Long userId, String refType, Long refId)` 和 `deleteByUserIdAndRefTypeAndRefId(Long userId, ResourceFavorite.RefType refType, Long refId)`。

- [x] **Step 2: Service 改为幂等写路径**

`favorite()` 调用 `favoriteRepo.insertIgnore(userId, refType.name(), refId)`；`unfavorite()` 调用直接删除方法，两个路径都继续返回 `getInteraction()` 的实时状态。

- [x] **Step 3: 更新并运行测试**

Run: `mvn -q test -Dtest=ResourceFavoriteServiceTest`
Expected: `BUILD SUCCESS`

### Task 6: 举报计数原子更新

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumReplyRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Test: `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`

- [x] **Step 1: Repository 增加原子举报计数**

在 `ForumThreadRepository` 和 `ForumReplyRepository` 添加 `incrementReportCount(Long id)`，使用 `@Modifying` JPQL `update ... set reportCount = reportCount + 1`。

- [x] **Step 2: Service 替换读改写**

`ContentReportService.incrementReportCount()` 对帖子和回复分别调用仓储原子更新，不再加载实体再 `save()`。

- [x] **Step 3: 更新并运行测试**

Run: `mvn -q test -Dtest=ContentReportServiceTest`
Expected: `BUILD SUCCESS`

### Task 7: 热点查询复合索引

**Files:**
- Modify: `backend/src/main/java/com/aiblog/entity/Comment.java`
- Modify: `backend/src/main/java/com/aiblog/entity/ForumThread.java`
- Modify: `backend/src/main/java/com/aiblog/entity/ForumReply.java`
- Modify: `backend/src/main/java/com/aiblog/entity/ResourceFavorite.java`

- [x] **Step 1: 补评论、论坛、收藏列表索引**

为评论可见列表和审核列表、论坛按状态/最后回复排序、作者页、收藏按用户时间排序补复合索引。

- [x] **Step 2: 编译验证实体映射**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`

### Task 8: 限流响应退避提示

**Files:**
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
- Test: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`

- [x] **Step 1: 429 响应增加 `Retry-After`**

拒绝请求时根据规则 refill 周期写入 `Retry-After` 秒数。

- [x] **Step 2: 更新并运行测试**

Run: `mvn -q test -Dtest=RateLimitInterceptorTest`
Expected: `BUILD SUCCESS`

## 后续建议

后续可拆成独立计划执行：
- 公共列表和详情增加 HTTP cache headers 或 Spring Cache，并在内容更新后主动失效。
- 增加 Actuator/Micrometer 指标，输出限流命中、DB pool、请求耗时、状态检测耗时。
- 全站搜索从 `LIKE '%q%'` 迁移到 MySQL FULLTEXT 或独立搜索引擎，避免大数据量下的扫表。
- 按 `content`、`community`、`moderation`、`identity`、`ops` 拆包，逐步引入 Ports/Adapters，而不是一次性重构全部实体。

## Self-Review

Spec coverage: 覆盖了项目分析、面向高并发的架构设计、缺失功能识别、第一阶段可执行改造和验证命令。

Placeholder scan: 没有 `TBD`、`TODO`、`implement later` 等占位内容；第二阶段明确标为后续独立计划，不属于本轮交付。

Type consistency: Repository 方法名、Service 调用名、限流配置字段在任务中保持一致。
