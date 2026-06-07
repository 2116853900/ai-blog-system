# Forum Popular Tags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show popular forum tags in the forum sidebar so visitors can quickly filter threads by active topics.

**Architecture:** Add a small public backend endpoint under `/api/forum/threads/tags/popular` that counts comma-delimited tags from visible threads. The frontend loads that summary with the forum categories and renders compact tag buttons that reuse the existing tag filter state in `Forum.vue`.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

## Project Analysis

The forum list already supports `tag` query filtering and clickable tags inside each thread card. That path only works after users see a thread with the desired tag. A sidebar tag summary improves discovery without changing the thread list contract. The backend stores tags as comma-delimited text in `ForumThread.tags`, so the endpoint should fetch visible tag strings, split and normalize them in service code, then return top tags with counts.

The current worktree already has pending forum list enhancements. This plan only touches the forum thread repository/service/controller, targeted tests, frontend API typing, `Forum.vue`, README, and this plan document.

## File Structure

- Create `backend/src/main/java/com/aiblog/dto/ForumTagSummaryResponse.java`
  - Public response record with `tag` and `count`.

- Modify `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
  - Add `findTagTextsByStatusIn(...)` to fetch non-blank tag strings for visible statuses.

- Modify `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
  - Add `popularTags(int limit)` that parses comma-delimited tags, merges case-insensitive duplicates, sorts by count descending and tag ascending, and caps the requested limit.

- Modify `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
  - Add `GET /api/forum/threads/tags/popular?limit=20`.

- Modify `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`
  - Add service tests for tag counting, trimming, case-insensitive merging, sorting, and limit capping.

- Modify `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
  - Add a controller forwarding test for the popular tags endpoint.

- Modify `frontend/src/api/types.ts`
  - Add `ForumTagSummary`.

- Modify `frontend/src/api/index.ts`
  - Add `forumApi.popularThreadTags`.

- Modify `frontend/src/views/Forum.vue`
  - Load popular tags with categories.
  - Render sidebar tag buttons that call the existing `selectTag(tag)` helper.

- Modify `README.md`
  - Document the richer forum thread list query parameters and popular tag endpoint.

## Task 1: Backend Popular Tags API

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ForumTagSummaryResponse.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`

- [ ] **Step 1: Add failing tests**

In `ForumThreadServiceSearchTest`, add:

```java
    @Test
    void popularTagsCountsVisibleThreadTagsCaseInsensitively() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        when(threadRepo.findTagTextsByStatusIn(anyCollection())).thenReturn(List.of(
                "MCP, Prompt, AI",
                "mcp,Prompt",
                "API, , prompt"
        ));

        List<ForumTagSummaryResponse> result = service.popularTags(3);

        assertThat(result).extracting(ForumTagSummaryResponse::tag).containsExactly("Prompt", "MCP", "AI");
        assertThat(result).extracting(ForumTagSummaryResponse::count).containsExactly(3L, 2L, 1L);
        verify(threadRepo).findTagTextsByStatusIn(anyCollection());
    }

    @Test
    void popularTagsCapsRequestedLimit() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        when(threadRepo.findTagTextsByStatusIn(anyCollection())).thenReturn(List.of("a,b,c,d,e,f"));

        List<ForumTagSummaryResponse> result = service.popularTags(2);

        assertThat(result).hasSize(2);
    }
```

Also add a private `newService(...)` helper in that test class to remove repeated constructor setup.

In `ForumThreadControllerTest`, add:

```java
    @Test
    void popularTagsForwardsLimit() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        List<ForumTagSummaryResponse> tags = List.of(new ForumTagSummaryResponse("MCP", 4));
        when(threadService.popularTags(12)).thenReturn(tags);

        List<ForumTagSummaryResponse> response = controller.popularTags(12);

        assertThat(response).isSameAs(tags);
        verify(threadService).popularTags(12);
    }
```

- [ ] **Step 2: Run backend tests to verify failure**

Run from `backend`:

```bash
mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test
```

Expected: FAIL because the DTO, repository method, service method, and controller method do not exist yet.

- [ ] **Step 3: Implement DTO and repository query**

Create `ForumTagSummaryResponse`:

```java
package com.aiblog.dto;

public record ForumTagSummaryResponse(String tag, long count) {
}
```

Add to `ForumThreadRepository`:

```java
    @Query("""
            select t.tags from ForumThread t
            where t.status in :visibleStatuses
              and t.tags is not null
              and trim(t.tags) <> ''
            """)
    List<String> findTagTextsByStatusIn(@Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses);
```

- [ ] **Step 4: Implement service counting**

Add imports:

```java
import com.aiblog.dto.ForumTagSummaryResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
```

Add to `ForumThreadService`:

```java
    public List<ForumTagSummaryResponse> popularTags(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 50));
        Map<String, TagCounter> counters = new LinkedHashMap<>();
        for (String tagText : threadRepo.findTagTextsByStatusIn(VISIBLE_STATUSES)) {
            for (String rawTag : tagText.split(",")) {
                String tag = rawTag.trim();
                if (tag.isBlank()) continue;
                String key = tag.toLowerCase(Locale.ROOT);
                counters.computeIfAbsent(key, ignored -> new TagCounter(tag)).increment();
            }
        }
        return counters.values().stream()
                .sorted(Comparator.comparingLong(TagCounter::count).reversed()
                        .thenComparing(counter -> counter.tag().toLowerCase(Locale.ROOT)))
                .limit(cappedLimit)
                .map(counter -> new ForumTagSummaryResponse(counter.tag(), counter.count()))
                .toList();
    }

    private static final class TagCounter {
        private final String tag;
        private long count;

        private TagCounter(String tag) {
            this.tag = tag;
        }

        private void increment() {
            count++;
        }

        private String tag() {
            return tag;
        }

        private long count() {
            return count;
        }
    }
```

- [ ] **Step 5: Implement controller endpoint**

Add import:

```java
import com.aiblog.dto.ForumTagSummaryResponse;
```

Add before `get(Long id)`:

```java
    @GetMapping("/tags/popular")
    public List<ForumTagSummaryResponse> popularTags(@RequestParam(defaultValue = "20") int limit) {
        return threadService.popularTags(limit);
    }
```

- [ ] **Step 6: Run backend tests**

Run from `backend`:

```bash
mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test
```

Expected: PASS.

## Task 2: Frontend Popular Tags Sidebar

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`
- Modify: `README.md`

- [ ] **Step 1: Add frontend type and API method**

Add to `types.ts`:

```ts
export interface ForumTagSummary {
  tag: string
  count: number
}
```

Import `ForumTagSummary` in `api/index.ts` and add to `forumApi`:

```ts
  popularThreadTags: (params?: { limit?: number }) =>
    http.get<ForumTagSummary[]>('/forum/threads/tags/popular', { params }).then(r => r.data),
```

- [ ] **Step 2: Load popular tags**

In `Forum.vue`, import `ForumTagSummary`, add:

```ts
const popularTags = ref<ForumTagSummary[]>([])
```

In `onMounted`, replace the category load with:

```ts
    const [categoryResult, tagResult] = await Promise.all([
      forumApi.categories(),
      forumApi.popularThreadTags({ limit: 16 })
    ])
    categories.value = categoryResult
    popularTags.value = tagResult
```

- [ ] **Step 3: Render sidebar tag buttons**

Inside `<aside class="card categories">`, after the category groups, add:

```vue
        <div v-if="popularTags.length" class="popular-tags">
          <p class="popular-tags-title mono">热门标签</p>
          <button
            v-for="item in popularTags"
            :key="item.tag"
            type="button"
            class="popular-tag"
            :class="{ active: selectedTag === item.tag }"
            @click="selectTag(item.tag)"
          >
            <span>{{ item.tag }}</span>
            <small>{{ item.count }}</small>
          </button>
        </div>
```

Add scoped CSS:

```css
.popular-tags {
  border-top: 1px solid var(--border);
  margin-top: 14px;
  padding-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.popular-tags-title {
  width: 100%;
  margin: 0 0 2px;
  color: var(--text-dim);
  font-size: 12px;
}
.popular-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  color: var(--text-soft);
  cursor: pointer;
  font-size: 12px;
  padding: 6px 8px;
}
.popular-tag small {
  color: var(--text-dim);
  font-family: var(--font-mono);
}
.popular-tag:hover,
.popular-tag.active {
  border-color: color-mix(in srgb, var(--primary) 45%, var(--border));
  background: var(--primary-soft);
  color: var(--primary);
}
```

- [ ] **Step 4: Update README API summary**

Change the forum API line to mention `q`, `tag`, `unanswered`, `sort`, and `/api/forum/threads/tags/popular`.

- [ ] **Step 5: Run frontend build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

## Task 3: Regression Verification

- [ ] **Step 1: Run backend targeted tests**

Run from `backend`:

```bash
mvn -q "-Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest" test
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.
