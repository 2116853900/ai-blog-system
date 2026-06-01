# Account Activity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an account-center activity area where logged-in forum users can revisit their own threads, replies, and favorited threads.

**Architecture:** Reuse the existing forum domain model and pagination contracts. Backend adds authenticated account activity endpoints under `/api/account/*`; frontend consumes those endpoints from `Account.vue` and renders tabbed paginated lists below profile/security settings.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3 Composition API, TypeScript, Vite.

---

## File Structure

- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostFavoriteRepository.java`
  - Add a paged query that returns visible favorited `ForumThread` rows ordered by favorite time.
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
  - Add a read-only `listFavoriteThreads` method using the repository query.
- Create: `backend/src/main/java/com/aiblog/controller/AccountActivityController.java`
  - Add `/api/account/threads`, `/api/account/replies`, and `/api/account/favorites`.
- Modify: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`
  - Add coverage for favorite-list status filtering and ordering delegation.
- Modify: `frontend/src/api/index.ts`
  - Add `accountApi` methods for the three new endpoints.
- Modify: `frontend/src/views/Account.vue`
  - Add tabbed account activity UI, pagination, loading and empty states.

## Task 1: Backend Favorite Listing

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ForumPostFavoriteRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
- Test: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`

- [ ] **Step 1: Add the repository query**

In `ForumPostFavoriteRepository.java`, add imports for `ForumThread`, `Page`, `Pageable`, `Query`, `Param`, and `Collection`, then add:

```java
@Query(value = """
        select t from ForumThread t, ForumPostFavorite f
        where f.postId = t.id
          and f.userId = :userId
          and t.status in :visibleStatuses
        order by f.createdAt desc
        """, countQuery = """
        select count(t) from ForumThread t, ForumPostFavorite f
        where f.postId = t.id
          and f.userId = :userId
          and t.status in :visibleStatuses
        """)
Page<ForumThread> findFavoriteThreadsByUserId(
        @Param("userId") Long userId,
        @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses,
        Pageable pageable);
```

- [ ] **Step 2: Add the service method**

In `ForumInteractionService.java`, add `Page`/`Pageable` imports and:

```java
@Transactional(readOnly = true)
public Page<ForumThread> listFavoriteThreads(Long userId, Pageable pageable) {
    return favoriteRepo.findFavoriteThreadsByUserId(userId, INTERACTABLE_STATUSES, pageable);
}
```

- [ ] **Step 3: Test the service delegation**

In `ForumInteractionServiceTest.java`, make the `threadRepo.findById` setup lenient and add:

```java
@Test
void listFavoriteThreadsUsesInteractableStatuses() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ForumThread> page = new PageImpl<>(List.of(thread), pageable, 1);
    when(favoriteRepo.findFavoriteThreadsByUserId(USER_ID,
            List.of(ForumThread.ThreadStatus.NORMAL, ForumThread.ThreadStatus.PINNED,
                    ForumThread.ThreadStatus.FEATURED, ForumThread.ThreadStatus.LOCKED),
            pageable)).thenReturn(page);

    Page<ForumThread> result = service.listFavoriteThreads(USER_ID, pageable);

    assertThat(result.getContent()).containsExactly(thread);
    verify(favoriteRepo).findFavoriteThreadsByUserId(USER_ID,
            List.of(ForumThread.ThreadStatus.NORMAL, ForumThread.ThreadStatus.PINNED,
                    ForumThread.ThreadStatus.FEATURED, ForumThread.ThreadStatus.LOCKED),
            pageable);
}
```

- [ ] **Step 4: Run the focused backend test**

Run: `mvn -q -Dtest=ForumInteractionServiceTest test`

Expected: all `ForumInteractionServiceTest` tests pass.

## Task 2: Authenticated Account Activity API

**Files:**
- Create: `backend/src/main/java/com/aiblog/controller/AccountActivityController.java`

- [ ] **Step 1: Add the controller**

Create `AccountActivityController.java`:

```java
package com.aiblog.controller;

import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumInteractionService;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountActivityController {

    private final ForumUserService userService;
    private final ForumThreadService threadService;
    private final ForumReplyService replyService;
    private final ForumInteractionService interactionService;

    public AccountActivityController(ForumUserService userService,
                                     ForumThreadService threadService,
                                     ForumReplyService replyService,
                                     ForumInteractionService interactionService) {
        this.userService = userService;
        this.threadService = threadService;
        this.replyService = replyService;
        this.interactionService = interactionService;
    }

    @GetMapping("/threads")
    public Page<ForumThread> threads(Authentication auth,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return threadService.listByAuthor(userId, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/replies")
    public Page<ForumReply> replies(Authentication auth,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return replyService.listByAuthor(userId, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/favorites")
    public Page<ForumThread> favorites(Authentication auth,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return interactionService.listFavoriteThreads(userId, PageRequest.of(normalizePage(page), normalizeSize(size)));
    }

    private Long requireForumUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可查看账号动态"));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(size), sort);
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.min(50, Math.max(1, size));
    }
}
```

- [ ] **Step 2: Compile backend**

Run: `mvn -q -DskipTests compile`

Expected: compile succeeds.

## Task 3: Frontend Account Activity UI

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Account.vue`

- [ ] **Step 1: Add frontend API methods**

In `frontend/src/api/index.ts`, add `ForumThread` and `ForumReply` to the type import if needed, then add:

```ts
export const accountApi = {
  threads: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/account/threads', { params }).then(r => r.data),
  replies: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumReply>>('/account/replies', { params }).then(r => r.data),
  favorites: (params?: { page?: number; size?: number }) =>
    http.get<Page<ForumThread>>('/account/favorites', { params }).then(r => r.data)
}
```

- [ ] **Step 2: Add account activity state**

In `Account.vue`, import `computed`, `RouterLink`, `accountApi`, and the forum/page types. Add refs for `activityTab`, `activityPage`, `activityLoading`, `activityError`, `myThreads`, `myReplies`, and `myFavorites`. Add helper functions for loading tabs, pagination, formatting dates, stripping markdown to previews, and counting totals.

- [ ] **Step 3: Render the tabbed activity panel**

Below the existing profile/security grid, add a card with three tab buttons:

```vue
<section class="card panel activity-panel">
  <div class="activity-head">
    <div>
      <p class="mono dim">// activity</p>
      <h2>我的动态</h2>
    </div>
    <span class="muted mono">{{ activityTotal }} items</span>
  </div>
  <div class="activity-tabs" role="tablist" aria-label="账号动态">
    <button class="tab-btn" :class="{ active: activityTab === 'threads' }" @click="selectActivity('threads')">我的帖子</button>
    <button class="tab-btn" :class="{ active: activityTab === 'replies' }" @click="selectActivity('replies')">我的回复</button>
    <button class="tab-btn" :class="{ active: activityTab === 'favorites' }" @click="selectActivity('favorites')">我的收藏</button>
  </div>
  <p v-if="activityError" class="err">{{ activityError }}</p>
  <div v-if="activityLoading" class="muted mono">加载中...</div>
  <div v-else-if="activityItems.length === 0" class="empty muted">暂无记录。</div>
  <div v-else class="activity-list">
    <RouterLink v-for="item in activityItems" :key="item.id" class="activity-item" :to="activityLink(item)">
      <strong>{{ activityTitle(item) }}</strong>
      <span class="muted">{{ activityPreview(item) }}</span>
    </RouterLink>
  </div>
</section>
```

- [ ] **Step 4: Run frontend typecheck/build**

Run: `npm run build`

Expected: Vue typecheck and Vite production build pass.

## Task 4: Full Verification

**Files:**
- No additional source files.

- [ ] **Step 1: Run backend tests**

Run: `mvn -q test`

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: build succeeds and no TypeScript errors remain.

- [ ] **Step 3: Manual smoke test**

Start backend and frontend, log in as a forum user, then verify:

1. `/account` shows profile/security plus `我的动态`.
2. `我的帖子` links to the user's visible threads.
3. `我的回复` links to the replied thread.
4. `我的收藏` shows posts favorited from a thread detail page.
5. Empty states and pagination controls do not shift layout on mobile width.

## Self-Review

- Spec coverage: The feature closes the current product gap where favorites exist but have no account-center retrieval path; it also adds personal thread/reply history.
- Placeholder scan: No TBD/TODO/fill-later language remains in implementation steps.
- Type consistency: Backend returns existing `Page<ForumThread>` and `Page<ForumReply>` contracts already modeled in `frontend/src/api/types.ts`; frontend uses those same types.
