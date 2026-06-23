# Redis Local Cache Concurrency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AI 信息站补充可选 Redis、本地 TTL 缓存、分布式限流和公开读路径缓存，继续降低高并发下数据库压力。

**Architecture:** 在现有 Controller-Service-Repository 分层上增加基础设施适配器：`cache` 包提供本地 L1 + Redis L2 的混合缓存，`security` 限流优先使用 Redis 并在失败时回退本地 token bucket。业务层只依赖缓存服务接口，不直接依赖 Redis，避免 Redis 不可用时影响主流程。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Spring Data Redis, Jackson, MySQL 8.

---

## 架构分析

可以加 Redis 和本地缓存，但不能把 Redis 设计成硬依赖。当前项目以读多写少的公开内容为主，适合把缓存放在公开列表、详情、全站搜索、论坛板块这类路径。用户态数据、通知、评论提交、收藏状态、后台审核列表不适合长 TTL 缓存，避免出现强时效数据不一致。

推荐缓存层次：
- L1 本地缓存：进程内 `ConcurrentHashMap`，短 TTL，最快，Redis 不可用也能工作。
- L2 Redis：跨实例共享，适合分布式限流和公开读缓存；Redis 异常时自动跳过。
- Database：权威数据源，所有写操作仍先进数据库，写后清理相关缓存。

需要补齐的高并发点：
- 写接口限流当前是单机内存，横向扩容后每个实例各算一份，需要 Redis 分布式计数。
- 公开内容列表和详情每次命中数据库，热门首页/详情会制造重复查询。
- 全站搜索使用多表 LIKE 查询，应加短 TTL 缓存减少重复搜索压力；更长期应迁移 FULLTEXT/搜索引擎。
- 管理端内容写操作没有统一清理公开缓存，会造成缓存与写操作不一致。

## 文件结构

- Modify: `backend/pom.xml`
  - 增加 `spring-boot-starter-data-redis`。
- Modify: `backend/src/main/resources/application.yml`
  - 增加 Redis、缓存 TTL、限流存储模式配置。
- Create: `backend/src/main/java/com/aiblog/cache/CacheProperties.java`
  - 缓存开关、TTL 和本地最大条目配置。
- Create: `backend/src/main/java/com/aiblog/cache/HybridCacheService.java`
  - 本地 L1 + Redis L2 缓存服务，提供 `getOrLoad`、`evict`、`evictByPrefix`。
- Create: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
  - 封装公开内容列表、详情、论坛板块、全站搜索缓存 key 和失效方法。
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitProperties.java`
  - 增加 `storage` 和 Redis key 前缀配置。
- Create: `backend/src/main/java/com/aiblog/security/RedisRateLimiter.java`
  - 基于 Redis `INCR` + `EXPIRE` 的固定窗口限流。
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
  - 限流优先 Redis，失败或禁用时回退本地。
- Modify: public controllers and admin controllers
  - 公开读接口走 `PublicContentCacheService`；管理端写操作后清理对应缓存。
- Tests:
  - Create cache service tests.
  - Update rate limit tests.
  - Add controller cache smoke tests where practical.

## Task 1: Redis 与缓存配置

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`

- [x] **Step 1: 增加 Redis starter**

`backend/pom.xml` 增加 `spring-boot-starter-data-redis`。

- [x] **Step 2: 增加配置**

`application.yml` 增加：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT_MS:2000}

app:
  cache:
    enabled: ${APP_CACHE_ENABLED:true}
    redis-enabled: ${APP_CACHE_REDIS_ENABLED:false}
    key-prefix: ${APP_CACHE_KEY_PREFIX:ai-blog}
    local-max-size: ${APP_CACHE_LOCAL_MAX_SIZE:1000}
    public-content-ttl-ms: ${APP_CACHE_PUBLIC_CONTENT_TTL_MS:30000}
    search-ttl-ms: ${APP_CACHE_SEARCH_TTL_MS:10000}
  rate-limit:
    storage: ${RATE_LIMIT_STORAGE:local}
    redis-key-prefix: ${RATE_LIMIT_REDIS_KEY_PREFIX:ai-blog:rate-limit}
```

Redis 默认关闭，部署时显式设置 `APP_CACHE_REDIS_ENABLED=true` 和 `RATE_LIMIT_STORAGE=redis`。

## Task 2: 混合缓存基础设施

**Files:**
- Create: `backend/src/main/java/com/aiblog/cache/CacheProperties.java`
- Create: `backend/src/main/java/com/aiblog/cache/HybridCacheService.java`
- Test: `backend/src/test/java/com/aiblog/cache/HybridCacheServiceTest.java`

- [x] **Step 1: 创建缓存配置类**

配置类包含 `enabled`、`redisEnabled`、`keyPrefix`、`localMaxSize`、`publicContentTtlMs`、`searchTtlMs`。

- [x] **Step 2: 创建混合缓存服务**

`HybridCacheService.getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader)` 流程：
- 先查本地缓存，命中直接返回。
- Redis 开启时查 Redis，命中后回填本地。
- 未命中时调用 loader，结果写本地和 Redis。
- Redis 异常只记录 debug 日志，不影响业务返回。

- [x] **Step 3: 支持前缀失效**

`evictByPrefix(prefix)` 先清理本地 key，再在 Redis 中使用 `SCAN` 找匹配 key 并删除，避免 `KEYS` 阻塞 Redis。

## Task 3: 公开内容缓存服务

**Files:**
- Create: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`

- [x] **Step 1: 封装 key 与 TTL**

提供 `getPublicContent()`、`getSearch()`、`evictPosts()`、`evictSkills()`、`evictMcps()`、`evictApiStations()`、`evictForumCategories()`、`evictSearch()`。

- [x] **Step 2: 防止实体引用被列表接口污染**

教程列表需要把 `bodyMarkdown` 置空，缓存 loader 必须返回已经裁剪过的列表，避免把完整正文缓存到列表响应里。

## Task 4: 公共读接口接入缓存

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/PostController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/SkillController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/McpController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ForumCategoryController.java`
- Modify: `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`

- [x] **Step 1: 公开列表和详情缓存**

缓存 key 包含接口名、参数和 id/slug。列表缓存 30 秒，搜索缓存 10 秒。

- [x] **Step 2: 搜索缓存**

`GlobalSearchService.search()` 对非空 query 使用短 TTL 缓存，减少重复 LIKE 查询压力。

## Task 5: 写后缓存失效

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminPostController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminSkillController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminMcpController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminApiStationController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminForumCategoryController.java`
- Modify: `backend/src/main/java/com/aiblog/service/StatusCheckService.java`

- [x] **Step 1: 管理端内容写操作后失效公开缓存**

新增、更新、删除、发布切换后清理对应公开内容缓存和搜索缓存。

- [x] **Step 2: API 状态检测后失效 API 站点缓存**

`StatusCheckService.checkAndSave()` 保存状态后清理 API 站点公开缓存，避免状态徽章长时间旧数据。

## Task 6: Redis 优先限流与本地回退

**Files:**
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitProperties.java`
- Create: `backend/src/main/java/com/aiblog/security/RedisRateLimiter.java`
- Modify: `backend/src/main/java/com/aiblog/security/RateLimitInterceptor.java`
- Test: `backend/src/test/java/com/aiblog/security/RateLimitInterceptorTest.java`

- [x] **Step 1: 限流配置增加存储模式**

`storage` 支持 `local` 和 `redis`，默认 `local`。

- [x] **Step 2: Redis 固定窗口限流**

`RedisRateLimiter.tryConsume()` 使用 `INCR` 计数，首次写入设置 `EXPIRE`，超过 capacity 返回 false。Redis 异常抛给拦截器，由拦截器回退本地限流。

- [x] **Step 3: 拦截器优先 Redis，失败回退本地**

`RATE_LIMIT_STORAGE=redis` 时先用 Redis；Redis 不可用时继续用本地 token bucket，避免请求全部失败。

## Task 7: 验证

**Files:**
- No file changes.

- [x] **Step 1: 后端相关测试**

Run: `mvn -q test -Dtest=HybridCacheServiceTest,RateLimitInterceptorTest,GlobalSearchServiceTest`
Expected: `BUILD SUCCESS`

- [x] **Step 2: 后端编译**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`

- [x] **Step 3: 后端全量测试**

Run: `mvn -q test`
Expected: `BUILD SUCCESS`

## 仍建议后续补齐

- 对全站搜索引入 MySQL FULLTEXT 或搜索引擎，缓存只能降低重复查询，不能解决任意关键词扫表。
- 对热点浏览数引入 Redis 计数缓冲和批量落库，避免超热帖子每次浏览都写数据库。
- 增加 Actuator/Micrometer 指标：缓存命中率、Redis 降级次数、限流拒绝次数、DB pool 使用率。
- 后台列表改分页，当前部分后台接口直接返回全部数据，数据量大后仍会产生内存和响应体压力。
