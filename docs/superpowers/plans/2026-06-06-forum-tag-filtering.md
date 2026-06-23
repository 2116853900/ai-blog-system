# Forum Tag Filtering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let forum visitors filter the thread list by clicking a thread tag or opening `/forum?tag=...`.

**Architecture:** Extend the existing forum thread list endpoint with a `tag` query parameter. Keep the current search and category behavior, but route list filtering through `ForumThreadService.search(categoryId, q, tag, pageable)` so `categoryId`, keyword search, tag filtering, and the existing sort parameter compose cleanly.

**Tech Stack:** Spring Boot 3, Spring Data JPA Specifications, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

## Project Analysis

The forum list already renders each thread's comma-separated `tags` field as visual chips, but those chips are passive. The backend stores forum tags in `ForumThread.tags` as a comma-separated string and already supports keyword search across title, content, and tags. Adding a dedicated tag filter is a small feature with clear value because it turns existing metadata into navigation.

The current worktree includes unrelated pending changes for resource reviews, operation logs, security, README, and several detail pages. This plan only touches forum list files plus the existing forum controller test created for the previous sorting feature.

## File Structure

- Modify `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
  - Add optional `tag` request parameter.
  - Route any list request with `categoryId`, `q`, or `tag` to `threadService.search(categoryId, q, tag, pageable)`.

- Modify `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
  - Keep existing `search(Long categoryId, String q, Pageable pageable)` for compatibility.
  - Add `search(Long categoryId, String q, String tag, Pageable pageable)` backed by `JpaSpecificationExecutor`.
  - Match `tag` as a comma-delimited token after removing spaces, so `mcp` matches `AI,mcp` and not `mcp-server` unless that is a separate tag.

- Modify `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
  - Update existing direct calls to the new controller method signature.
  - Add a test proving `tag` is forwarded with sort and category.

- Create `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`
  - Unit-test that blank keyword and tag-only filtering uses `findAll(Specification, Pageable)` instead of the old repository query.
  - Unit-test that the existing two-argument search delegates to the new search overload.

- Modify `frontend/src/api/index.ts`
  - Add `tag?: string` to `forumApi.threads` params.

- Modify `frontend/src/views/Forum.vue`
  - Add `selectedTag` state from `route.query.tag`.
  - Include `tag` in API requests and URL sync.
  - Add a removable active tag filter indicator.
  - Change tag chips in thread cards from inert spans to buttons that filter by that tag.

## Task 1: Backend Tag Filtering

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
- Create: `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`

- [ ] **Step 1: Update controller tests for tag forwarding**

In `ForumThreadControllerTest`, change existing calls from:

```java
controller.list(categoryId, q, sort, page, size)
```

to:

```java
controller.list(categoryId, q, null, sort, page, size)
```

Add:

```java
    @Test
    void listForwardsTagFilterWithSortAndCategory() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(3L, "", "mcp", expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "", "mcp", "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "", "mcp", expectedPage);
    }
```

- [ ] **Step 2: Add service tests**

Create `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`:

```java
package com.aiblog.service;

import com.aiblog.entity.ForumThread;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumCategoryRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumThreadServiceSearchTest {

    @Test
    void searchWithTagUsesSpecificationSearch() {
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

        Page<ForumThread> result = service.search(null, "", "mcp", pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void legacySearchDelegatesToSpecificationSearchWithoutTag() {
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

        Page<ForumThread> result = service.search(2L, "prompt", pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }
}
```

- [ ] **Step 3: Run backend tests to verify failure**

Run from `backend`: `mvn -q -Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest test`

Expected: FAIL because the controller and service overloads do not exist yet.

- [ ] **Step 4: Implement controller and service filtering**

In `ForumThreadController.list`, add:

```java
            @RequestParam(required = false) String tag,
```

between `q` and `sort`, and change the branch to:

```java
        if (categoryId != null || hasText(q) || hasText(tag)) {
            return threadService.search(categoryId, q, tag, pageable);
        }
```

Add:

```java
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
```

In `ForumThreadService`, replace the existing search method with:

```java
    public Page<ForumThread> search(Long categoryId, String q, Pageable pageable) {
        return search(categoryId, q, null, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, String tag, Pageable pageable) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        String normalizedTag = normalizeTag(tag);

        Specification<ForumThread> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(VISIBLE_STATUSES));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (!keyword.isBlank()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("contentMarkdown")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("tags"), "")), pattern)
                ));
            }
            if (!normalizedTag.isBlank()) {
                var compactTags = cb.function(
                        "replace",
                        String.class,
                        cb.lower(cb.coalesce(root.get("tags"), "")),
                        cb.literal(" "),
                        cb.literal("")
                );
                var delimitedTags = cb.concat(cb.concat(",", compactTags), ",");
                predicates.add(cb.like(delimitedTags, "%," + normalizedTag + ",%"));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return threadRepo.findAll(spec, pageable);
    }
```

Add:

```java
    private String normalizeTag(String tag) {
        if (tag == null) return "";
        return tag.trim().toLowerCase().replace(" ", "");
    }
```

- [ ] **Step 5: Run backend tests**

Run from `backend`: `mvn -q -Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest test`

Expected: PASS.

## Task 2: Frontend Tag Filter

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add API parameter**

Change `forumApi.threads` params to:

```ts
{ categoryId?: number; q?: string; tag?: string; sort?: 'latest' | 'newest' | 'popular'; page?: number; size?: number }
```

- [ ] **Step 2: Add tag state and URL sync**

In `Forum.vue`, add:

```ts
const selectedTag = ref(typeof route.query.tag === 'string' ? route.query.tag : '')
```

Include in `loadThreads`:

```ts
      tag: selectedTag.value || undefined,
```

Include in `syncQuery`:

```ts
  if (selectedTag.value) query.tag = selectedTag.value
```

Add:

```ts
async function selectTag(tag: string) {
  selectedTag.value = tag
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}

async function clearTag() {
  selectedTag.value = ''
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}
```

- [ ] **Step 3: Add active tag indicator and clickable chips**

After the `.thread-head` block, add:

```vue
        <div v-if="selectedTag" class="active-filter">
          <span>标签：{{ selectedTag }}</span>
          <button type="button" @click="clearTag">清除</button>
        </div>
```

Change tag chips from:

```vue
<span v-for="tag in tagsOf(t.tags)" :key="tag" class="tag">{{ tag }}</span>
```

to:

```vue
<button v-for="tag in tagsOf(t.tags)" :key="tag" type="button" class="tag tag-button" @click.prevent="selectTag(tag)">
  {{ tag }}
</button>
```

Add CSS:

```css
.active-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--text-soft);
  font-size: 13px;
}
.active-filter button,
.tag-button {
  border: 0;
  cursor: pointer;
}
.active-filter button {
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  padding: 5px 8px;
}
.tag-button:hover {
  color: var(--primary);
  border-color: var(--primary);
}
```

- [ ] **Step 4: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.

## Task 3: Final Verification

- [ ] **Step 1: Run backend targeted tests**

Run from `backend`: `mvn -q -Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest test`

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`: `npm run build`

Expected: PASS.
