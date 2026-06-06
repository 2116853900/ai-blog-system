# Forum Thread Sorting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user-selectable forum thread ordering so visitors can browse by latest activity, newest posts, or popularity.

**Architecture:** Keep sorting as a query parameter on the existing `/api/forum/threads` endpoint. The backend maps a small allowlist of sort keys to Spring `Sort` instances, while the Vue forum list stores the selected sort in the URL query and passes it through the existing API client.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

## Project Analysis

The project is an AI information community site with:

- Public resource sections for tutorials, Skills, MCPs, and API stations.
- Detail pages with comments, resource favorites, resource reviews, and linked forum discussions.
- A forum with categories, threads, replies, likes, favorites, reports, and account activity.
- Admin pages for content governance, submissions, users, reports, operation logs, and dashboard metrics.
- Backend built with Spring Boot 3, Spring Security/JWT, JPA, Redis support, and MySQL.
- Frontend built with Vue 3, Vite, TypeScript, Pinia, and CSS variables in `frontend/src/styles/main.css`.

The safest high-value feature addition is forum sorting. It improves discovery, reuses existing thread stats, and has a small blast radius.

## File Structure

- Modify `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
  - Add `sort` query parameter.
  - Add a private `resolveThreadSort(String sort)` allowlist helper.
  - Keep the default ordering as latest activity.

- Create `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
  - Unit-test controller-level sort mapping with mocked services.
  - Cover `latest`, `newest`, `popular`, and unknown fallback.

- Modify `frontend/src/api/index.ts`
  - Add a `sort?: 'latest' | 'newest' | 'popular'` parameter to `forumApi.threads`.

- Modify `frontend/src/views/Forum.vue`
  - Add a local `ForumSort` type and selected sort ref seeded from `route.query.sort`.
  - Include sort in API requests and URL query.
  - Add a compact segmented control next to the forum search.

## Task 1: Backend Sort Parameter

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Create: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`

- [ ] **Step 1: Write the controller test**

Create `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`:

```java
package com.aiblog.controller;

import com.aiblog.entity.ForumThread;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumThreadControllerTest {

    @Test
    void listDefaultsToLatestActivitySort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }

    @Test
    void listSupportsNewestSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, "newest", 1, 10);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }

    @Test
    void listSupportsPopularSortWithStableTieBreakers() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "replyCount")
                .and(Sort.by(Sort.Direction.DESC, "viewCount"))
                .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest expectedPage = PageRequest.of(0, 20, expectedSort);
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(3L, "mcp", expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "mcp", "popular", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "mcp", expectedPage);
    }

    @Test
    void listFallsBackToLatestSortForUnknownSortKey() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, "bad-input", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ForumThreadControllerTest test`

Expected: FAIL because `ForumThreadController.list` does not accept the `sort` argument yet.

- [ ] **Step 3: Implement controller sorting**

In `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`, update the list signature and add the helper:

```java
    @GetMapping
    public Page<ForumThread> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, resolveThreadSort(sort));
        if (categoryId != null || (q != null && !q.isBlank())) {
            return threadService.search(categoryId, q, pageable);
        }
        return threadService.listAll(pageable);
    }
```

Add before `resolveUserId`:

```java
    private Sort resolveThreadSort(String sort) {
        if ("newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("popular".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "replyCount")
                    .and(Sort.by(Sort.Direction.DESC, "viewCount"))
                    .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "lastReplyAt")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
```

- [ ] **Step 4: Run backend targeted test**

Run: `mvn -q -Dtest=ForumThreadControllerTest test`

Expected: PASS.

## Task 2: Frontend Sort Control

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add API parameter type**

Change `forumApi.threads` in `frontend/src/api/index.ts` from:

```ts
  threads: (params?: { categoryId?: number; q?: string; page?: number; size?: number }) =>
```

to:

```ts
  threads: (params?: { categoryId?: number; q?: string; sort?: 'latest' | 'newest' | 'popular'; page?: number; size?: number }) =>
```

- [ ] **Step 2: Add sort state and request plumbing**

In `frontend/src/views/Forum.vue`, add after imports:

```ts
type ForumSort = 'latest' | 'newest' | 'popular'

const sortOptions: Array<{ value: ForumSort; label: string }> = [
  { value: 'latest', label: '最近活跃' },
  { value: 'newest', label: '最新发布' },
  { value: 'popular', label: '热门' }
]
```

Add after `q`:

```ts
const sort = ref<ForumSort>(parseSort(route.query.sort))
```

Add helper:

```ts
function parseSort(value: unknown): ForumSort {
  return value === 'newest' || value === 'popular' ? value : 'latest'
}
```

In `loadThreads`, include:

```ts
      sort: sort.value,
```

In `syncQuery`, include:

```ts
  if (sort.value !== 'latest') query.sort = sort.value
```

Add:

```ts
async function selectSort(value: ForumSort) {
  if (sort.value === value) return
  sort.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}
```

- [ ] **Step 3: Add sort controls to the forum header**

Replace the single `SearchBar` in `frontend/src/views/Forum.vue` with:

```vue
          <div class="thread-tools">
            <div class="sort-tabs" aria-label="帖子排序">
              <button
                v-for="option in sortOptions"
                :key="option.value"
                type="button"
                :class="{ active: sort === option.value }"
                @click="selectSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <SearchBar v-model="q" placeholder="搜索标题、正文或标签" @search="searchThreads" />
          </div>
```

Add scoped CSS:

```css
.thread-tools { display: grid; gap: 10px; }
.sort-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.sort-tabs button {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-size: 13px;
  padding: 7px 10px;
  white-space: nowrap;
}
.sort-tabs button:hover,
.sort-tabs button.active {
  background: var(--primary-soft);
  color: var(--primary);
}
```

In the `@media (max-width: 640px)` block, add:

```css
  .sort-tabs { overflow-x: auto; }
```

- [ ] **Step 4: Run frontend build**

Run: `npm run build`

Expected: PASS.

## Task 3: Final Verification

**Files:**
- Verify only.

- [ ] **Step 1: Run backend test**

Run from `backend`: `mvn -q -Dtest=ForumThreadControllerTest test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

- [ ] **Step 3: Inspect changed files**

Run: `git diff -- backend/src/main/java/com/aiblog/controller/ForumThreadController.java backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java frontend/src/api/index.ts frontend/src/views/Forum.vue docs/superpowers/plans/2026-06-06-forum-thread-sorting.md`

Expected: Diff only contains the planned sorting feature.
