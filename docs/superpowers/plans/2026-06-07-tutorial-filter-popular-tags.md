# Tutorial Filter Popular Tags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move tutorial list filtering to the backend and add popular tutorial tag chips to the public tutorials page.

**Architecture:** Extend the existing public posts endpoint to accept `q`, `tag`, and `category` parameters using the shared `SearchSpecs` helper plus a `published = true` guard. Reuse the existing resource tag summary response shape for a `GET /api/posts/tags/popular` endpoint, and update the Vue tutorials page to use the same list-view interaction model as Skill/MCP pages.

**Tech Stack:** Spring Boot 3, Spring Data JPA Specifications, JUnit 5/Mockito/AssertJ, Vue 3, Vite, TypeScript.

---

### Project Analysis

Current state:

- `PostController.list()` returns every published tutorial without query parameters and strips `bodyMarkdown` before responding.
- `Tutorials.vue` downloads all tutorials, then filters by keyword/category in the browser.
- Skill/MCP/API pages already call backend list filters and now expose popular tag chips.
- `SearchSpecs` already supports keyword, tag, and category filtering for comma-separated `tags` fields.

Chosen feature:

- Add backend tutorial filtering and tutorial popular tags so the tutorial page has the same discovery and filtering behavior as the other resource pages.

### Task 1: Backend Tutorial Filtering

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/PostRepository.java`
- Modify: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/PostController.java`
- Test: `backend/src/test/java/com/aiblog/controller/PostControllerTest.java`

- [ ] **Step 1: Enable Specification queries**

Change `PostRepository` to extend `JpaSpecificationExecutor<Post>`.

- [ ] **Step 2: Add cached post list key**

Add `postsListKey(String q, String tag, String category)` and keep the existing `postsListKey()` delegating to the empty-parameter key.

- [ ] **Step 3: Update `PostController.list` signature**

Change `list()` to accept optional `q`, `tag`, and `category`; build a `Specification<Post>` that combines `Post::published` with `SearchSpecs.build(q, tag, category, List.of("title", "summary", "tags", "category"))`; sort by `createdAt DESC`; strip `bodyMarkdown` from every returned post.

- [ ] **Step 4: Add controller tests**

Test that list calls `findAll(Specification, Sort)` when any filter is provided, keeps only list summary payloads by clearing `bodyMarkdown`, and still returns a published list when no filters are supplied.

- [ ] **Step 5: Run backend test**

Run: `mvn -q "-Dtest=PostControllerTest" test`

Expected: all tests pass.

### Task 2: Tutorial Popular Tags Endpoint

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ResourceTagService.java`
- Modify: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/PostController.java`
- Test: `backend/src/test/java/com/aiblog/service/ResourceTagServiceTest.java`
- Test: `backend/src/test/java/com/aiblog/controller/PostControllerTest.java`

- [ ] **Step 1: Add post tag service method**

Inject `PostRepository` into `ResourceTagService` and add `postPopularTags(int limit)` using `postRepo.findByPublishedTrueOrderByCreatedAtDesc().stream().map(Post::getTags).toList()`.

- [ ] **Step 2: Add cache key**

Add `postsPopularTagsKey(int limit)` using the same `1..50` limit cap as other popular tag cache keys.

- [ ] **Step 3: Add endpoint**

Expose `GET /api/posts/tags/popular?limit=20` in `PostController`, returning cached `List<ResourceTagSummaryResponse>`.

- [ ] **Step 4: Add tests**

Extend service tests to assert post tags are counted only from repository data. Extend controller tests to assert the endpoint forwards the requested limit.

- [ ] **Step 5: Run backend focused tests**

Run: `mvn -q "-Dtest=PostControllerTest,ResourceTagServiceTest" test`

Expected: all tests pass.

### Task 3: Frontend Tutorial Page

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Tutorials.vue`

- [ ] **Step 1: Update public API methods**

Change `publicApi.posts()` to accept `{ q?: string; tag?: string; category?: string }`, and add `postPopularTags({ limit?: number })`.

- [ ] **Step 2: Use backend list filtering**

Replace the page-local `posts`, `filtered`, and manual category filtering state with `useListView<Post>(publicApi.posts)`.

- [ ] **Step 3: Render popular tags**

Load `publicApi.postPopularTags({ limit: 12 })` on mount and render clickable chips below category chips. Clicking a popular tag calls `toggleTag(tag.tag)`.

- [ ] **Step 4: Run frontend build**

Run: `npm run build`

Expected: TypeScript and Vite production build pass.

### Task 4: Final Verification

**Files:**
- Verify: all modified files

- [ ] **Step 1: Run focused backend tests**

Run: `mvn -q "-Dtest=PostControllerTest,ResourceTagServiceTest" test`

Expected: tests pass with no failures.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: build passes.

- [ ] **Step 3: Inspect git diff**

Run: `git diff --stat -- backend frontend docs/superpowers/plans/2026-06-07-tutorial-filter-popular-tags.md`

Expected: diff contains tutorial filtering, tutorial popular tags, and plan changes only, plus the earlier uncommitted resource tag work.
