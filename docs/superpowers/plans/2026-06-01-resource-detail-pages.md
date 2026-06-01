# Resource Detail Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add shareable public detail pages for Skills, MCP servers, and API stations so global search results can open exact resources.

**Architecture:** Extend existing public controllers with `GET /api/skills/{id}`, `GET /api/mcps/{id}`, and `GET /api/api-stations/{id}`. Add frontend API methods and three route-backed detail pages that reuse existing display components, then update global search URLs to point to those routes.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21, Vue 3, Vue Router, TypeScript, Vite.

---

## File Structure

- Modify: `backend/src/main/java/com/aiblog/controller/SkillController.java`
  - Add public detail endpoint by ID.
- Modify: `backend/src/main/java/com/aiblog/controller/McpController.java`
  - Add public detail endpoint by ID.
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
  - Add public detail endpoint by ID.
- Modify: `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`
  - Link Skill/MCP/API search results to exact detail routes.
- Modify: `backend/src/test/java/com/aiblog/service/GlobalSearchServiceTest.java`
  - Assert new exact URLs.
- Create: `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java`
  - Unit test present/missing detail endpoint behavior.
- Modify: `frontend/src/api/index.ts`
  - Add `publicApi.skill`, `publicApi.mcp`, and `publicApi.apiStation`.
- Create: `frontend/src/views/SkillDetail.vue`
  - Skill detail page with tags, rating, link, and comments.
- Create: `frontend/src/views/McpDetail.vue`
  - MCP detail page with install command, repository link, and comments.
- Create: `frontend/src/views/ApiStationDetail.vue`
  - API station detail page with status, base URL, supported models, tags, and comments.
- Modify: `frontend/src/router/index.ts`
  - Add `/skills/:id`, `/mcps/:id`, and `/api-stations/:id` routes.
- Modify: `README.md`
  - Document the new public detail endpoints.

---

### Task 1: Backend Detail Endpoints and Search URLs

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/SkillController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/McpController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Modify: `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`
- Modify: `backend/src/test/java/com/aiblog/service/GlobalSearchServiceTest.java`
- Create: `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java` with tests that instantiate public controllers with mocked repositories and assert `200 OK` for existing rows and `404 Not Found` for missing rows.

- [ ] **Step 2: Run failing tests**

Run: `mvn -q -Dtest=PublicResourceDetailControllerTest test` from `backend`.

Expected: FAIL because public detail methods do not exist yet.

- [ ] **Step 3: Add controller detail methods**

Add `ResponseEntity<T>` detail methods to `SkillController`, `McpController`, and `ApiStationController`:

```java
@GetMapping("/{id}")
public ResponseEntity<Skill> detail(@PathVariable Long id) {
    return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
}
```

Use the matching entity type in each controller.

- [ ] **Step 4: Update search URL generation**

In `GlobalSearchService`, change:

```java
"/skills"
"/mcps"
"/api-stations"
```

to:

```java
"/skills/" + skill.getId()
"/mcps/" + mcp.getId()
"/api-stations/" + apiStation.getId()
```

- [ ] **Step 5: Update global search test expected URLs**

In `GlobalSearchServiceTest`, expect `/skills/2`, `/mcps/3`, and `/api-stations/4`.

- [ ] **Step 6: Run backend tests**

Run:

```bash
mvn -q -Dtest=PublicResourceDetailControllerTest,GlobalSearchServiceTest test
```

Expected: PASS.

---

### Task 2: Frontend Detail Pages

**Files:**
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/SkillDetail.vue`
- Create: `frontend/src/views/McpDetail.vue`
- Create: `frontend/src/views/ApiStationDetail.vue`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Add frontend API methods**

Add these methods to `publicApi`:

```ts
skill: (id: number) => http.get<Skill>(`/skills/${id}`).then(r => r.data),
mcp: (id: number) => http.get<Mcp>(`/mcps/${id}`).then(r => r.data),
apiStation: (id: number) => http.get<ApiStation>(`/api-stations/${id}`).then(r => r.data),
```

- [ ] **Step 2: Create `SkillDetail.vue`**

Create a route page that loads `publicApi.skill(Number(route.params.id))`, shows loading skeleton, 404 state, category, rating, description, tags, external link, and `CommentSection ref-type="SKILL"`.

- [ ] **Step 3: Create `McpDetail.vue`**

Create a route page that loads `publicApi.mcp(Number(route.params.id))`, shows install command with `CopyButton`, repository link, tags, rating, and `CommentSection ref-type="MCP"`.

- [ ] **Step 4: Create `ApiStationDetail.vue`**

Create a route page that loads `publicApi.apiStation(Number(route.params.id))`, shows `StatusBadge`, base URL with `CopyButton`, supported models, tags, and `CommentSection ref-type="API"`.

- [ ] **Step 5: Register routes**

Add routes after existing list routes:

```ts
{ path: '/skills/:id', name: 'skill-detail', component: () => import('../views/SkillDetail.vue') },
{ path: '/mcps/:id', name: 'mcp-detail', component: () => import('../views/McpDetail.vue') },
{ path: '/api-stations/:id', name: 'api-station-detail', component: () => import('../views/ApiStationDetail.vue') },
```

- [ ] **Step 6: Build frontend**

Run: `npm run build` from `frontend`.

Expected: PASS.

---

### Task 3: Documentation and Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document detail endpoints**

Update the public API table so Skill, MCP, and API station rows mention list and detail endpoints.

- [ ] **Step 2: Compile backend**

Run: `mvn -q -DskipTests compile` from `backend`.

Expected: PASS.

- [ ] **Step 3: Build frontend**

Run: `npm run build` from `frontend`.

Expected: PASS.

- [ ] **Step 4: Final diff review**

Run: `git diff -- README.md backend/src/main/java/com/aiblog backend/src/test/java/com/aiblog frontend/src docs/superpowers/plans/2026-06-01-resource-detail-pages.md`.

Expected: Diff contains resource detail pages and the previous global-search work; unrelated existing dirty changes are not reverted.

---

## Self-Review

Spec coverage:
- New public exact resource pages are covered by backend detail endpoints and frontend detail routes.
- Global search now benefits from exact resource URLs.
- Verification covers focused backend tests, backend compile, and frontend build.

Placeholder scan:
- No `TBD`, `TODO`, or unspecified implementation steps remain.

Type consistency:
- Public API methods are `skill`, `mcp`, and `apiStation`.
- Routes are `/skills/:id`, `/mcps/:id`, and `/api-stations/:id`.
- Search URLs use the same route shapes.
