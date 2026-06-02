# Tutorial Favorites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the existing resource favorites feature so logged-in users can favorite published tutorials and see them in the account center.

**Architecture:** Reuse the new `resource_favorite` table and `ResourceFavoriteButton` component by adding a `POST` ref type. Backend validation only allows favoriting published tutorials, while account-list mapping marks unpublished or missing tutorials as unavailable.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3, TypeScript, Vite.

---

## File Structure

- Modify `backend/src/main/java/com/aiblog/entity/ResourceFavorite.java`: add `POST` to the favorite ref enum.
- Modify `backend/src/main/java/com/aiblog/service/ResourceFavoriteService.java`: inject `PostRepository`, validate published posts, map tutorial favorites to `/tutorials/{slug}`.
- Modify `backend/src/test/java/com/aiblog/service/ResourceFavoriteServiceTest.java`: cover POST favorite creation, unpublished validation, and account-list mapping.
- Modify `frontend/src/api/types.ts`: add `POST` to `ResourceFavoriteRefType`.
- Modify `frontend/src/views/TutorialDetail.vue`: render `ResourceFavoriteButton` for tutorial detail pages.
- Modify `frontend/src/views/Account.vue`: label POST favorites as `教程`.
- Modify `README.md`: update resource favorite docs from Skill/MCP/API to tutorial + resource favorites.

---

### Task 1: Backend POST Favorite Support

**Files:**
- Modify: `backend/src/main/java/com/aiblog/entity/ResourceFavorite.java`
- Modify: `backend/src/main/java/com/aiblog/service/ResourceFavoriteService.java`
- Modify: `backend/src/test/java/com/aiblog/service/ResourceFavoriteServiceTest.java`

- [ ] **Step 1: Extend tests first**

Add these test cases to `ResourceFavoriteServiceTest`:

```java
@Test
void postFavoriteCreatesOneRecordForPublishedTutorial()

@Test
void postFavoriteRejectsUnpublishedTutorial()

@Test
void listFavoritesMapsPostSkillMcpAndApiItems()
```

The test setup must mock `PostRepository`, instantiate:

```java
service = new ResourceFavoriteService(favoriteRepo, postRepo, skillRepo, mcpRepo, apiRepo);
```

Run: `mvn -q -Dtest=ResourceFavoriteServiceTest test`

Expected: FAIL because `ResourceFavorite.RefType.POST` and the new constructor are not implemented yet.

- [ ] **Step 2: Implement POST backend behavior**

Make these changes:

```java
public enum RefType { POST, SKILL, MCP, API }
```

Inject `PostRepository` into `ResourceFavoriteService`.

Validation:

```java
case POST -> postRepo.findById(refId).filter(Post::isPublished).isPresent();
```

Mapping:

```java
case POST -> postRepo.findById(favorite.getRefId())
    .filter(Post::isPublished)
    .map(post -> fromPost(favorite, post))
    .orElseGet(() -> missing(favorite));
```

Post cards should use title, summary, category, tags, and `/tutorials/{slug}`.

- [ ] **Step 3: Run focused backend tests**

Run: `mvn -q -Dtest=ResourceFavoriteServiceTest test`

Expected: PASS.

---

### Task 2: Frontend Tutorial Detail Integration

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/views/TutorialDetail.vue`
- Modify: `frontend/src/views/Account.vue`

- [ ] **Step 1: Extend frontend favorite type**

Change:

```ts
export type ResourceFavoriteRefType = 'SKILL' | 'MCP' | 'API'
```

to:

```ts
export type ResourceFavoriteRefType = 'POST' | 'SKILL' | 'MCP' | 'API'
```

- [ ] **Step 2: Add tutorial favorite button**

In `TutorialDetail.vue`, import:

```ts
import ResourceFavoriteButton from '../components/ResourceFavoriteButton.vue'
```

Place this in the tutorial header when `post` is loaded:

```vue
<ResourceFavoriteButton ref-type="POST" :ref-id="post.id" />
```

Use a `.post-head-row` wrapper so the category/title area and favorite button align on desktop and stack on mobile.

- [ ] **Step 3: Update account labels**

In `Account.vue`, extend:

```ts
const resourceTypeLabels: Record<ResourceFavoriteItem['refType'], string> = {
  POST: '教程',
  SKILL: 'Skill',
  MCP: 'MCP',
  API: 'API'
}
```

- [ ] **Step 4: Run frontend build**

Run: `npm run build`

Expected: PASS.

---

### Task 3: Documentation And Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README wording**

Change the resource favorites row to describe:

```text
登录用户的教程、Skill、MCP、API 收藏
```

- [ ] **Step 2: Run full backend tests**

Run: `mvn -q test`

Expected: PASS.

- [ ] **Step 3: Run frontend production build**

Run: `npm run build`

Expected: PASS.

- [ ] **Step 4: Manual smoke path**

Without starting services automatically:
- Backend and frontend can be started manually when needed.
- Visit `/tutorials/{slug}`.
- Confirm the favorite button appears in the tutorial header.
- Anonymous click redirects to `/login?redirect=<tutorial path>`.
- Logged-in click toggles favorite state.
- Visit `/account`, open `资源收藏`, and confirm tutorial favorites display with the `教程` chip and route to `/tutorials/{slug}`.

---

## Self-Review

- Spec coverage: The plan extends the current resource favorite system to tutorials, updates UI and docs, and verifies backend/frontend.
- Placeholder scan: No task uses TBD or vague implementation language.
- Type consistency: Backend and frontend both add uppercase `POST`; account labels use the same union value.
