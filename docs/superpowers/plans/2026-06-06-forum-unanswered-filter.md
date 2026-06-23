# Forum Unanswered Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let forum visitors filter the thread list to only show threads with zero replies.

**Architecture:** Extend the existing `/api/forum/threads` list endpoint with an `unanswered=true` query parameter. The backend composes this with category, keyword, tag, and sort inside `ForumThreadService.search(...)`; the frontend exposes it as a compact checkbox filter beside the existing sort/search controls and stores it in the URL query.

**Tech Stack:** Spring Boot 3, Spring Data JPA Specifications, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

## Project Analysis

The forum list now supports sorting and tag filtering. A “只看未回复” filter helps users find unanswered questions and complements the existing `replyCount` field already returned by `ForumThread`. Because `ForumThreadService.search(...)` now uses a JPA `Specification`, adding `replyCount = 0` is a small, composable change.

The current worktree contains unrelated pending changes outside the forum list. This plan only touches the forum list controller/service/tests, frontend API typing, `Forum.vue`, and this plan document.

## File Structure

- Modify `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
  - Add optional `Boolean unanswered` query parameter.
  - Route to service search when `unanswered=true`.

- Modify `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
  - Keep existing search overloads.
  - Add `search(Long categoryId, String q, String tag, Boolean unanswered, Pageable pageable)`.
  - Apply `replyCount = 0` when `unanswered` is true.

- Modify `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
  - Update direct calls for the new method signature.
  - Add a forwarding test for `unanswered=true`.

- Modify `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`
  - Add a test proving unanswered searches use the specification path.

- Modify `frontend/src/api/index.ts`
  - Add `unanswered?: boolean` to `forumApi.threads` params.

- Modify `frontend/src/views/Forum.vue`
  - Add `unansweredOnly` state from `route.query.unanswered`.
  - Include it in API requests and URL sync.
  - Add a checkbox filter control next to sorting/search.

## Task 1: Backend Unanswered Filter

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
- Modify: `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`

- [ ] **Step 1: Update controller tests**

Update all `controller.list(...)` calls to include `unanswered` before `sort`:

```java
controller.list(categoryId, q, tag, unanswered, sort, page, size)
```

Add:

```java
    @Test
    void listForwardsUnansweredFilterWithSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(null, null, null, true, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, true, "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(null, null, null, true, expectedPage);
    }
```

- [ ] **Step 2: Update service tests**

Add:

```java
    @Test
    void searchWithUnansweredUsesSpecificationSearch() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = new ForumThreadService(
                threadRepo,
                mock(ForumCategoryRepository.class),
                mock(ForumUserRepository.class),
                mock(AdminOperationLogRepository.class),
                mock(ForumViewCountBuffer.class)
        );
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ForumThread> page = new PageImpl<>(List.of(), pageable, 0);
        when(threadRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ForumThread> result = service.search(null, null, null, true, pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }
```

- [ ] **Step 3: Run backend tests to verify failure**

Run from `backend`: `mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test`

Expected: FAIL because the controller and service overloads do not exist yet.

- [ ] **Step 4: Implement backend filtering**

In `ForumThreadController.list`, add:

```java
            @RequestParam(required = false) Boolean unanswered,
```

before `sort`, and change the branch to:

```java
        if (categoryId != null || hasText(q) || hasText(tag) || Boolean.TRUE.equals(unanswered)) {
            return threadService.search(categoryId, q, tag, unanswered, pageable);
        }
```

In `ForumThreadService`, change:

```java
    public Page<ForumThread> search(Long categoryId, String q, String tag, Pageable pageable) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
```

to:

```java
    public Page<ForumThread> search(Long categoryId, String q, String tag, Pageable pageable) {
        return search(categoryId, q, tag, null, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, String tag, Boolean unanswered, Pageable pageable) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
```

Inside the specification, after the tag predicate, add:

```java
            if (Boolean.TRUE.equals(unanswered)) {
                predicates.add(cb.equal(root.get("replyCount"), 0));
            }
```

- [ ] **Step 5: Run backend tests**

Run from `backend`: `mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test`

Expected: PASS.

## Task 2: Frontend Unanswered Filter

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add API parameter**

Change `forumApi.threads` params to:

```ts
{ categoryId?: number; q?: string; tag?: string; unanswered?: boolean; sort?: 'latest' | 'newest' | 'popular'; page?: number; size?: number }
```

- [ ] **Step 2: Add state and URL sync**

Add:

```ts
const unansweredOnly = ref(route.query.unanswered === 'true')
```

Include in `loadThreads`:

```ts
      unanswered: unansweredOnly.value || undefined,
```

Include in `syncQuery`:

```ts
  if (unansweredOnly.value) query.unanswered = 'true'
```

Add:

```ts
async function setUnansweredOnly(value: boolean) {
  unansweredOnly.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

function onUnansweredChange(event: Event) {
  void setUnansweredOnly((event.target as HTMLInputElement).checked)
}
```

- [ ] **Step 3: Add checkbox control**

Inside `.thread-tools`, between sort tabs and `SearchBar`, add:

```vue
            <label class="filter-toggle">
              <input type="checkbox" :checked="unansweredOnly" @change="onUnansweredChange" />
              只看未回复
            </label>
```

Add CSS:

```css
.filter-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-soft);
  font-size: 13px;
}
.filter-toggle input {
  accent-color: var(--primary);
}
```

- [ ] **Step 4: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

## Task 3: Final Verification

- [ ] **Step 1: Run backend targeted tests**

Run from `backend`: `mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.
