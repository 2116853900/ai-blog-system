# Resource Popular Tags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add popular tag discovery for public Skill, MCP, and API station list pages.

**Architecture:** Add one backend service that counts comma-separated resource tags case-insensitively and returns stable summaries. Expose cached public endpoints on each existing resource controller, then render those summaries as clickable filter chips in the existing Vue list pages.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito/AssertJ, Vue 3, Vite, TypeScript.

---

### Project Analysis

The project is an AI information site with:

- Backend: `backend/` Spring Boot 3 app using JPA repositories, Spring Security/JWT, MySQL, and unit tests under `backend/src/test/java`.
- Frontend: `frontend/` Vue 3 + Vite + TypeScript app using `frontend/src/api/index.ts` as the API boundary.
- Existing related pattern: forum threads already expose `GET /api/forum/threads/tags/popular` through `ForumThreadService.popularTags`.
- Gap: Skill, MCP, and API station pages support tag filtering only after users discover a tag on a card; there is no first-class popular tag entry point for those resource types.

### Task 1: Backend Tag Summary Service

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ResourceTagSummaryResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/ResourceTagService.java`
- Test: `backend/src/test/java/com/aiblog/service/ResourceTagServiceTest.java`

- [ ] **Step 1: Add response DTO**

```java
package com.aiblog.dto;

public record ResourceTagSummaryResponse(String tag, long count) {
}
```

- [ ] **Step 2: Add service**

Create `ResourceTagService` with constructor dependencies on `SkillRepository`, `McpRepository`, and `ApiStationRepository`. Implement `skillPopularTags(int limit)`, `mcpPopularTags(int limit)`, and `apiStationPopularTags(int limit)` by reading all resource tags, splitting by `,` or `，`, counting case-insensitively, preserving first-seen display casing, sorting by count descending then tag ascending, and capping limit to `1..50`.

- [ ] **Step 3: Add unit tests**

Add tests covering mixed-case counting, Chinese comma splitting, blank tag omission, deterministic tie sorting, and limit capping.

- [ ] **Step 4: Run service tests**

Run: `mvn -q -Dtest=ResourceTagServiceTest test`

Expected: all tests pass.

### Task 2: Public API Endpoints

**Files:**
- Modify: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/SkillController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/McpController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Test: `backend/src/test/java/com/aiblog/controller/ResourceTagControllerTest.java`

- [ ] **Step 1: Add cache keys**

Add methods:

```java
public String skillsPopularTagsKey(int limit) {
    return SKILLS_PREFIX + "tags:popular:" + Math.max(1, Math.min(limit, 50));
}

public String mcpsPopularTagsKey(int limit) {
    return MCPS_PREFIX + "tags:popular:" + Math.max(1, Math.min(limit, 50));
}

public String apiStationsPopularTagsKey(int limit) {
    return API_STATIONS_PREFIX + "tags:popular:" + Math.max(1, Math.min(limit, 50));
}
```

- [ ] **Step 2: Add controller endpoints**

Expose:

- `GET /api/skills/tags/popular?limit=20`
- `GET /api/mcps/tags/popular?limit=20`
- `GET /api/api-stations/tags/popular?limit=20`

Each endpoint should use `PublicContentCacheService.publicContent(...)` and `ResourceTagService`.

- [ ] **Step 3: Add controller forwarding tests**

Test that each controller forwards the requested `limit` and returns the service result.

- [ ] **Step 4: Run controller tests**

Run: `mvn -q -Dtest=ResourceTagControllerTest test`

Expected: all tests pass.

### Task 3: Frontend Popular Tag Chips

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Skills.vue`
- Modify: `frontend/src/views/Mcps.vue`
- Modify: `frontend/src/views/ApiStations.vue`

- [ ] **Step 1: Add type and API methods**

Add `ResourceTagSummary` to `types.ts`, then add API methods:

```ts
skillPopularTags: (params?: { limit?: number }) =>
  http.get<ResourceTagSummary[]>('/skills/tags/popular', { params }).then(r => r.data)
```

Repeat for MCP and API stations.

- [ ] **Step 2: Render popular tag chips**

On each resource list page, load the popular tags on mount and render a compact row below search controls. Clicking a chip should call existing `toggleTag(tag)`, reusing the existing backend tag filter.

- [ ] **Step 3: Run frontend build**

Run: `npm run build`

Expected: TypeScript and Vite production build pass.

### Task 4: Final Verification

**Files:**
- Verify: all modified files

- [ ] **Step 1: Run backend focused tests**

Run: `mvn -q -Dtest=ResourceTagServiceTest,ResourceTagControllerTest test`

Expected: all focused backend tests pass.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: frontend build passes.

- [ ] **Step 3: Inspect git diff**

Run: `git diff -- backend frontend docs/superpowers/plans/2026-06-07-resource-popular-tags.md`

Expected: diff only contains the popular tag feature and the implementation plan.
