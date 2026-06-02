# Admin List Pagination Concurrency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台内容和审核类大列表改为分页加载，减少高并发/大数据量下的全表查询、内存占用和响应体大小。

**Architecture:** 保持现有 Controller-Repository 结构，管理端列表接口从 `List<T>` 升级为 Spring Data `Page<T>`。前端复用已有分页 UI 模式，按 `page/size` 请求；写操作后刷新当前页。论坛板块保留全量读取，因为它数量小且层级排序依赖完整集合。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, Vue 3, TypeScript, Axios.

---

## 分析

当前后台以下列表一次性返回全量数据：教程、Skill、MCP、API 站点、评论、投稿。数据量增长后，这些接口会造成数据库全量排序、后端内存膨胀和前端大响应体渲染。论坛帖子、回复、用户、举报已经是分页接口；论坛板块通常数量很小，且父子层级排序依赖全量集合，本轮不分页。

## 文件结构

- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminPostController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminSkillController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminMcpController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminApiStationController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminCommentController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminSubmissionController.java`
- Modify: `backend/src/main/java/com/aiblog/service/AdminCommentService.java`
- Modify: `backend/src/main/java/com/aiblog/repository/CommentRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/SubmissionRepository.java`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/admin/AdminPosts.vue`
- Modify: `frontend/src/views/admin/AdminSkills.vue`
- Modify: `frontend/src/views/admin/AdminMcps.vue`
- Modify: `frontend/src/views/admin/AdminApiStations.vue`
- Modify: `frontend/src/views/admin/AdminComments.vue`
- Modify: `frontend/src/views/admin/AdminSubmissions.vue`

## Task 1: 后端列表接口分页

**Files:** backend admin controllers and repositories.

- [x] **Step 1: 内容管理列表返回 Page**

`AdminPostController.list()`、`AdminSkillController.list()`、`AdminMcpController.list()`、`AdminApiStationController.list()` 增加 `page`、`size` 参数，返回 `Page<T>`。

- [x] **Step 2: 评论审核分页**

`AdminCommentService.list()` 返回 `Page<Comment>`，Repository 增加分页查询签名。

- [x] **Step 3: 投稿审核分页**

`AdminSubmissionController.list()` 返回 `Page<Submission>`，Repository 增加分页查询签名。

## Task 2: 前端 API 类型升级

**Files:** `frontend/src/api/index.ts`

- [x] **Step 1: 管理端列表 API 返回 Page**

`posts`、`skills`、`mcps`、`apiStations`、`comments`、`submissions` 接收 `{ page, size }` 参数并返回 `Page<T>`。

## Task 3: 管理页分页 UI

**Files:** admin Vue pages.

- [x] **Step 1: 接入分页数据**

每页增加 `pageData`、`page`、`size`，列表渲染使用 `pageData.content`。

- [x] **Step 2: 增加分页按钮**

复用现有后台论坛管理页风格：上一页 / 页码 / 总数 / 下一页。

- [x] **Step 3: 操作后刷新当前页**

新增、编辑、删除、审核、检测后调用 `load()` 保持当前分页上下文。

## Task 4: 验证

**Files:** no file changes.

- [x] **Step 1: 后端编译**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`

- [x] **Step 2: 后端全量测试**

Run: `mvn -q test`
Expected: `BUILD SUCCESS`

- [x] **Step 3: 前端构建**

Run: `npm run build`
Expected: `vue-tsc -b && vite build` succeeds.

## 后续建议

- 后台教程/Skill/MCP/API 站点增加关键词筛选，减少编辑定位成本。
- 搜索迁移 FULLTEXT 或搜索服务。
- 为后台分页响应增加默认排序字段的复合索引。
