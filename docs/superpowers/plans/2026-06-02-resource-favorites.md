# Resource Favorites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let logged-in users favorite Skill, MCP, and API station resources from detail pages and revisit them from the account center.

**Architecture:** Add a small resource-favorite aggregate in the backend that records `userId + refType + refId`, validates targets through existing resource repositories, and exposes a public interaction summary plus authenticated account endpoints. On the frontend, add a reusable favorite button for resource detail pages and extend the account activity panel with a resource favorites tab.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3, TypeScript, Pinia, Vite.

---

## File Structure

- Create `backend/src/main/java/com/aiblog/entity/ResourceFavorite.java`: JPA entity for user resource favorites with `SKILL`, `MCP`, and `API` ref types.
- Create `backend/src/main/java/com/aiblog/repository/ResourceFavoriteRepository.java`: persistence methods for idempotent toggles, counts, and paged account lists.
- Create `backend/src/main/java/com/aiblog/dto/ResourceFavoriteInteractionResponse.java`: detail-page summary payload.
- Create `backend/src/main/java/com/aiblog/dto/ResourceFavoriteItemResponse.java`: account-list resource card payload.
- Create `backend/src/main/java/com/aiblog/service/ResourceFavoriteService.java`: validation, favorite/unfavorite, summary, and account-list mapping.
- Create `backend/src/main/java/com/aiblog/controller/ResourceFavoriteController.java`: public summary and authenticated account endpoints.
- Modify `backend/src/main/java/com/aiblog/config/SecurityConfig.java`: allow public GET summaries at `/api/resource-favorites/**`.
- Create `backend/src/test/java/com/aiblog/service/ResourceFavoriteServiceTest.java`: idempotency, validation, count, and mapping tests.
- Modify `frontend/src/api/types.ts`: add resource favorite types.
- Modify `frontend/src/api/index.ts`: add public summary and account toggle/list methods.
- Create `frontend/src/components/ResourceFavoriteButton.vue`: reusable favorite control with login redirect.
- Modify `frontend/src/views/SkillDetail.vue`: place favorite control beside the existing rating.
- Modify `frontend/src/views/McpDetail.vue`: place favorite control beside the existing rating.
- Modify `frontend/src/views/ApiStationDetail.vue`: place favorite control in the detail header.
- Modify `frontend/src/views/Account.vue`: add a `资源收藏` activity tab with paged resource cards.
- Modify `README.md`: document the new resource favorites endpoints and validation flow.

---

### Task 1: Backend Resource Favorite Model And Service

**Files:**
- Create: `backend/src/main/java/com/aiblog/entity/ResourceFavorite.java`
- Create: `backend/src/main/java/com/aiblog/repository/ResourceFavoriteRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/ResourceFavoriteInteractionResponse.java`
- Create: `backend/src/main/java/com/aiblog/dto/ResourceFavoriteItemResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/ResourceFavoriteService.java`
- Create: `backend/src/test/java/com/aiblog/service/ResourceFavoriteServiceTest.java`

- [ ] **Step 1: Add service tests first**

Create `ResourceFavoriteServiceTest` covering:

```java
@Test
void favoriteCreatesOneRecordAndReturnsUpdatedCount()

@Test
void favoriteIsIdempotent()

@Test
void unfavoriteDeletesExistingRecord()

@Test
void getInteractionRejectsMissingResource()

@Test
void listFavoritesMapsSkillMcpAndApiItems()
```

Run: `mvn -q -Dtest=ResourceFavoriteServiceTest test`

Expected: FAIL because `ResourceFavoriteService` and DTO/entity classes do not exist yet.

- [ ] **Step 2: Add entity, repository, DTOs, and service**

Implement:

```java
public class ResourceFavorite {
    public enum RefType { SKILL, MCP, API }
}
```

The service must:

```java
ResourceFavoriteInteractionResponse getInteraction(ResourceFavorite.RefType refType, Long refId, Long userId)
ResourceFavoriteInteractionResponse favorite(ResourceFavorite.RefType refType, Long refId, Long userId)
ResourceFavoriteInteractionResponse unfavorite(ResourceFavorite.RefType refType, Long refId, Long userId)
Page<ResourceFavoriteItemResponse> listFavorites(Long userId, Pageable pageable)
```

Behavior:
- Validate the resource exists before summary/toggle operations.
- Create at most one favorite per `userId + refType + refId`.
- Delete only the matching favorite on unfavorite.
- Return `favorited` and total `favoriteCount` after each operation.
- Map account cards to `/skills/{id}`, `/mcps/{id}`, or `/api-stations/{id}`.

- [ ] **Step 3: Run focused backend tests**

Run: `mvn -q -Dtest=ResourceFavoriteServiceTest test`

Expected: PASS.

---

### Task 2: Backend API Endpoints And Security

**Files:**
- Create: `backend/src/main/java/com/aiblog/controller/ResourceFavoriteController.java`
- Modify: `backend/src/main/java/com/aiblog/config/SecurityConfig.java`
- Modify: `README.md`

- [ ] **Step 1: Add controller endpoints**

Expose:

```text
GET    /api/resource-favorites/{refType}/{refId}
GET    /api/account/resource-favorites?page=&size=
POST   /api/account/resource-favorites/{refType}/{refId}
DELETE /api/account/resource-favorites/{refType}/{refId}
```

Behavior:
- Public GET summary returns `favorited=false` when no user is logged in.
- Account list/toggle requires a forum user account.
- Invalid or missing resources return a clear 400 response from the service exception.

- [ ] **Step 2: Update security config**

Permit:

```java
.requestMatchers(HttpMethod.GET, "/api/resource-favorites/**").permitAll()
```

Keep account endpoints protected by the existing `.anyRequest().authenticated()` rule.

- [ ] **Step 3: Document API changes**

Update README main interface table with:

```text
GET /api/resource-favorites/{type}/{id}
GET/POST/DELETE /api/account/resource-favorites/**
```

- [ ] **Step 4: Run backend package check**

Run: `mvn -q test`

Expected: PASS.

---

### Task 3: Frontend API Types And Favorite Button

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/components/ResourceFavoriteButton.vue`

- [ ] **Step 1: Add TypeScript types and API methods**

Add:

```ts
export type ResourceFavoriteRefType = 'SKILL' | 'MCP' | 'API'

export interface ResourceFavoriteInteraction {
  favorited: boolean
  favoriteCount: number
}

export interface ResourceFavoriteItem {
  id: number
  refType: ResourceFavoriteRefType
  refId: number
  title: string
  description?: string
  url: string
  category?: string
  tags?: string
  available: boolean
  createdAt: string
}
```

Add public and account API methods:

```ts
publicApi.resourceFavoriteInteraction(refType, refId)
accountApi.resourceFavorites(params)
accountApi.favoriteResource(refType, refId)
accountApi.unfavoriteResource(refType, refId)
```

- [ ] **Step 2: Add reusable button**

`ResourceFavoriteButton.vue` must:
- Load public summary on mount and when props change.
- Redirect anonymous users to `/login?redirect=<current detail page>`.
- Toggle favorite through account API when logged in.
- Display `收藏`, `已收藏`, or `登录后收藏`, plus the count.
- Use existing `.btn`, `.btn-primary`, and theme variables.

- [ ] **Step 3: Run frontend type check**

Run: `npm run build`

Expected: PASS.

---

### Task 4: Detail Pages And Account Center

**Files:**
- Modify: `frontend/src/views/SkillDetail.vue`
- Modify: `frontend/src/views/McpDetail.vue`
- Modify: `frontend/src/views/ApiStationDetail.vue`
- Modify: `frontend/src/views/Account.vue`

- [ ] **Step 1: Add favorite control to resource detail pages**

Place:

```vue
<ResourceFavoriteButton ref-type="SKILL" :ref-id="skill.id" />
<ResourceFavoriteButton ref-type="MCP" :ref-id="mcp.id" />
<ResourceFavoriteButton ref-type="API" :ref-id="station.id" />
```

Use a `.head-actions` wrapper so ratings/status and favorite controls stay aligned and wrap on mobile.

- [ ] **Step 2: Add resource favorites tab**

Update `Account.vue`:
- Add `resources` to `ActivityTab`.
- Add `myResourceFavorites = ref<Page<ResourceFavoriteItem> | null>(null)`.
- Load `accountApi.resourceFavorites(params)` for the new tab.
- Render resource cards with type chip, title, description, and `收藏于` metadata.
- Keep existing forum thread/reply tabs unchanged.

- [ ] **Step 3: Run frontend build**

Run: `npm run build`

Expected: PASS.

---

### Task 5: Full Verification

**Files:**
- No new files unless verification reveals a necessary fix.

- [ ] **Step 1: Run backend tests**

Run: `mvn -q test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: PASS.

- [ ] **Step 3: Manual smoke path**

With backend and frontend running:
- Visit `/skills/1`, `/mcps/1`, and `/api-stations/1`.
- Confirm the favorite button renders on each detail page.
- Anonymous click goes to `/login` with a redirect query.
- Logged-in click toggles between `收藏` and `已收藏`.
- Visit `/account`, open `资源收藏`, and confirm the saved resources link back to their detail pages.

---

## Self-Review

- Spec coverage: The plan analyzes a concrete missing feature and implements it across persistence, API, detail-page UX, account UX, docs, and verification.
- Placeholder scan: No steps depend on TBD behavior; all commands and endpoints are explicit.
- Type consistency: Backend uses `ResourceFavorite.RefType`; frontend uses matching uppercase string union values `SKILL`, `MCP`, and `API`.
