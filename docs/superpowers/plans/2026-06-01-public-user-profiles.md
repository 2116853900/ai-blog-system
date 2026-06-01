# Public User Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add public user profile pages so forum readers can open an author, see their public profile, and browse their visible threads and replies.

**Architecture:** Extend the existing public user controller with paginated activity endpoints that reuse forum services and only return visible content. Add a Vue route `/users/:id` backed by the existing `userApi`, then link author names from forum thread detail pages into the new profile page.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21, JUnit 5/Mockito, Vue 3 Composition API, TypeScript, Vite.

---

## File Structure

- Modify: `backend/src/main/java/com/aiblog/repository/ForumReplyRepository.java`
  - Add a public-safe query for a user's visible replies whose parent threads are also visible.
- Modify: `backend/src/main/java/com/aiblog/service/ForumReplyService.java`
  - Add `listVisibleByAuthor(Long authorId, Pageable pageable)`.
- Modify: `backend/src/main/java/com/aiblog/controller/UserController.java`
  - Add `/api/users/{id}/threads` and `/api/users/{id}/replies` with page/size normalization.
- Create: `backend/src/test/java/com/aiblog/controller/PublicUserControllerTest.java`
  - Cover profile lookup, activity pagination, not-found behavior, and public-safe reply delegation.
- Modify: `frontend/src/api/index.ts`
  - Add `userApi.threads` and `userApi.replies`.
- Create: `frontend/src/views/UserProfile.vue`
  - Public profile page with profile summary, tabs for threads/replies, pagination, loading and empty states.
- Modify: `frontend/src/router/index.ts`
  - Add `/users/:id`.
- Modify: `frontend/src/views/ForumThreadDetail.vue`
  - Link thread and reply author names to `/users/:id`.
- Modify: `README.md`
  - Document public user profile/activity endpoints and route.

---

### Task 1: Backend Public User Activity

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ForumReplyRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumReplyService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/UserController.java`
- Test: `backend/src/test/java/com/aiblog/controller/PublicUserControllerTest.java`

- [ ] **Step 1: Add controller tests first**

Create `PublicUserControllerTest` with mocked `ForumUserService`, `ForumThreadService`, and `ForumReplyService`. Assert that `profile(7)` returns `200 OK`, `threads(7, -1, 99)` delegates to `PageRequest.of(0, 50, Sort.by(DESC, "createdAt"))`, `replies(7, 1, 5)` delegates to `replyService.listVisibleByAuthor`, and missing users return `404`.

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `backend`:

```bash
mvn -q -Dtest=PublicUserControllerTest test
```

Expected: FAIL because the controller constructor and activity methods have not been implemented.

- [ ] **Step 3: Add public-safe reply repository query**

Add this method to `ForumReplyRepository`:

```java
@Query(value = """
        select r from ForumReply r
        where r.authorId = :authorId
          and r.status in :replyStatuses
          and exists (
              select 1 from ForumThread t
              where t.id = r.threadId
                and t.status in :threadStatuses
          )
        """, countQuery = """
        select count(r) from ForumReply r
        where r.authorId = :authorId
          and r.status in :replyStatuses
          and exists (
              select 1 from ForumThread t
              where t.id = r.threadId
                and t.status in :threadStatuses
          )
        """)
Page<ForumReply> findVisibleByAuthorId(
        @Param("authorId") Long authorId,
        @Param("replyStatuses") Collection<ForumReply.ReplyStatus> replyStatuses,
        @Param("threadStatuses") Collection<ForumThread.ThreadStatus> threadStatuses,
        Pageable pageable);
```

- [ ] **Step 4: Add service method**

Add `VISIBLE_THREAD_STATUSES` to `ForumReplyService` and implement:

```java
public Page<ForumReply> listVisibleByAuthor(Long authorId, Pageable pageable) {
    return replyRepo.findVisibleByAuthorId(authorId, VISIBLE_STATUSES, VISIBLE_THREAD_STATUSES, pageable);
}
```

- [ ] **Step 5: Add controller endpoints**

Update `UserController` to inject `ForumThreadService` and `ForumReplyService`, then add:

```java
@GetMapping("/{id}/threads")
public ResponseEntity<Page<ForumThread>> threads(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
    if (userService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(threadService.listByAuthor(id, pageRequest(page, size)));
}

@GetMapping("/{id}/replies")
public ResponseEntity<Page<ForumReply>> replies(@PathVariable Long id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
    if (userService.findById(id).isEmpty()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(replyService.listVisibleByAuthor(id, pageRequest(page, size)));
}
```

- [ ] **Step 6: Run backend focused tests**

Run from `backend`:

```bash
mvn -q -Dtest=PublicUserControllerTest test
```

Expected: PASS.

---

### Task 2: Frontend Public User Profile

**Files:**
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/UserProfile.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [ ] **Step 1: Add frontend user activity APIs**

Add to `userApi`:

```ts
threads: (id: number, params?: { page?: number; size?: number }) =>
  http.get<Page<ForumThread>>(`/users/${id}/threads`, { params }).then(r => r.data),
replies: (id: number, params?: { page?: number; size?: number }) =>
  http.get<Page<ForumReply>>(`/users/${id}/replies`, { params }).then(r => r.data)
```

- [ ] **Step 2: Create `UserProfile.vue`**

Build a page that loads `userApi.profile(id)`, `userApi.threads(id)`, and `userApi.replies(id)`. Render an avatar or initials, username/nickname, role/level, bio, joined date, tabbed thread/reply lists, and pagination buttons. Threads link to `/forum/threads/:id`; replies link to `/forum/threads/:threadId`.

- [ ] **Step 3: Register route**

Add to `router/index.ts`:

```ts
{ path: '/users/:id', name: 'user-profile', component: () => import('../views/UserProfile.vue') },
```

- [ ] **Step 4: Link authors in thread detail**

In `ForumThreadDetail.vue`, wrap the thread author and each reply author with `RouterLink` to `/users/:id`.

- [ ] **Step 5: Build frontend**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

---

### Task 3: Documentation and Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document public user endpoints**

Add `GET /api/users/{id}`, `/api/users/{id}/threads`, and `/api/users/{id}/replies` to the API table. Add `/users/:id` to the public frontend route list.

- [ ] **Step 2: Compile backend**

Run from `backend`:

```bash
mvn -q -DskipTests compile
```

Expected: PASS.

- [ ] **Step 3: Run focused backend tests**

Run from `backend`:

```bash
mvn -q -Dtest=PublicUserControllerTest test
```

Expected: PASS.

- [ ] **Step 4: Build frontend**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

---

## Self-Review

Spec coverage:
- Public profile route is covered by Task 2.
- Public user activity endpoints are covered by Task 1.
- Hidden/deleted parent thread leakage is avoided by the new reply repository query.
- Documentation and verification are covered by Task 3.

Placeholder scan:
- No TBD, TODO, or unspecified implementation steps remain.

Type consistency:
- Backend methods are `threads`, `replies`, and `listVisibleByAuthor`.
- Frontend methods are `userApi.threads` and `userApi.replies`.
- Route name is `user-profile` and path is `/users/:id`.
