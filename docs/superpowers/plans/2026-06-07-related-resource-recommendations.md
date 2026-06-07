# Related Resource Recommendations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add public related-resource recommendations across tutorials, Skills, MCPs, and API stations, then surface them on each detail page.

**Architecture:** A backend service builds a normalized in-memory candidate list from existing repositories, scores candidates by shared tags, category match, title/description token overlap, and resource quality signals, and returns a compact DTO. A public controller exposes `GET /api/related-resources?refType=&refId=&limit=`. The Vue frontend adds a reusable detail-page component that calls the endpoint and renders compact recommendation rows.

**Tech Stack:** Spring Boot 3, Spring Data JPA repositories, JUnit 5 with Mockito, Vue 3 Composition API, TypeScript, Vite.

---

### Task 1: Backend DTO and Public Controller

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/RelatedResourceResponse.java`
- Create: `backend/src/main/java/com/aiblog/controller/RelatedResourceController.java`
- Modify: `backend/src/main/java/com/aiblog/config/SecurityConfig.java`

- [ ] **Step 1: Add response DTO**

Create `RelatedResourceResponse` as a Java record with fields: `type`, `id`, `title`, `description`, `url`, `category`, `tags`, `score`, `reason`.

- [ ] **Step 2: Add controller**

Create `RelatedResourceController` mapped to `/api/related-resources`. It accepts `refType`, `refId`, and optional `limit` defaulting to `6`, delegates to `RelatedResourceService`, and returns `404` when the source resource does not exist.

- [ ] **Step 3: Permit public GET access**

Add `/api/related-resources` and `/api/related-resources/**` to the existing public `GET` matcher in `SecurityConfig`.

### Task 2: Recommendation Service

**Files:**
- Create: `backend/src/main/java/com/aiblog/service/RelatedResourceService.java`
- Test: `backend/src/test/java/com/aiblog/service/RelatedResourceServiceTest.java`

- [ ] **Step 1: Implement source resolution**

Resolve `POST`, `SKILL`, `MCP`, and `API` source resources from existing repositories. `POST` sources must only be valid when published.

- [ ] **Step 2: Implement candidate normalization**

Normalize all resources into an internal candidate model with type, id, title, description, category, tags, url, quality score, and source flag. Skip unpublished posts and skip the source item itself.

- [ ] **Step 3: Implement scoring**

Score candidates with deterministic rules: `12` points per shared tag, `6` for same category, up to `8` for shared title/description tokens, `recommendLevel` for Skill/MCP, `3` for API status `UP`, `1` for `UNKNOWN`, and `0` for `DOWN`. Sort by score descending, then type, then title.

- [ ] **Step 4: Implement reasons**

Return a short Chinese reason such as `共同标签：RAG、MCP`, `同属分类：开发`, `关键词相近`, or `推荐级别较高`.

### Task 3: Backend Tests

**Files:**
- Create: `backend/src/test/java/com/aiblog/service/RelatedResourceServiceTest.java`

- [ ] **Step 1: Test source-not-found behavior**

Assert that a missing source returns `Optional.empty()`.

- [ ] **Step 2: Test ranking**

Build a POST source with tags `RAG,MCP` and candidates across Skill, MCP, API, and another Post. Assert shared-tag candidates rank ahead of weak matches and the current resource is excluded.

- [ ] **Step 3: Test limits**

Assert `limit` is clamped to a safe range and the returned list respects the requested size.

### Task 4: Frontend API and Component

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/components/RelatedResources.vue`

- [ ] **Step 1: Add TypeScript type**

Add `RelatedResource` with the same shape as the backend DTO.

- [ ] **Step 2: Add public API method**

Add `publicApi.relatedResources(refType, refId, { limit })`.

- [ ] **Step 3: Build reusable component**

Create `RelatedResources.vue` with props `refType`, `refId`, and optional `limit`. It loads recommendations, hides itself when no results exist, shows loading skeletons, and renders type badges, titles, descriptions, reasons, and tag snippets.

### Task 5: Detail Page Integration and Verification

**Files:**
- Modify: `frontend/src/views/TutorialDetail.vue`
- Modify: `frontend/src/views/SkillDetail.vue`
- Modify: `frontend/src/views/McpDetail.vue`
- Modify: `frontend/src/views/ApiStationDetail.vue`
- Modify: `README.md`

- [ ] **Step 1: Insert recommendation component**

Place recommendations after the primary content/tags and before linked discussions in all four detail pages.

- [ ] **Step 2: Update documentation**

Document `GET /api/related-resources?refType=&refId=&limit=` in README and mention detail-page related resources.

- [ ] **Step 3: Run backend tests**

Run `mvn -q test` from `backend`.

- [ ] **Step 4: Run frontend build**

Run `npm run build` from `frontend`.

---

Self-review:
- Spec coverage: the plan includes analysis, a larger end-to-end feature, implementation, documentation, and tests.
- Placeholder scan: no task depends on an undefined file or vague future work.
- Type consistency: backend and frontend both use `RelatedResource`/`RelatedResourceResponse` with the same fields.
