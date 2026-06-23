# Forum Accepted Reply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add accepted-reply support so thread authors and moderators can mark one reply as the thread solution, show it in thread detail, and clear it if needed.

**Architecture:** Store solution state on `ForumThread` as `acceptedReplyId`, `acceptedReplyUserId`, and `acceptedAt`. Expose one authenticated mutation endpoint on `ForumThreadController`, implement ownership/moderator authorization in `ForumThreadService`, and render solution controls in `ForumThreadDetail.vue`.

**Tech Stack:** Spring Boot 3.3, Spring Data JPA, JUnit 5/Mockito, Vue 3 Composition API, TypeScript, Vite.

---

### Task 1: Backend Entity And Service

**Files:**
- Modify: `backend/src/main/java/com/aiblog/entity/ForumThread.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Test: `backend/src/test/java/com/aiblog/service/ForumThreadServiceSearchTest.java`

- [ ] **Step 1: Add failing service tests**

Add tests that verify:

```java
@Test
void acceptReplyStoresSolutionWhenOwnerSelectsVisibleReplyInSameThread() {
    ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
    ForumReplyRepository replyRepo = mock(ForumReplyRepository.class);
    ForumThreadService service = newService(threadRepo, replyRepo);
    ForumThread thread = thread(10L, 20L);
    ForumReply reply = reply(99L, 10L, 30L, ForumReply.ReplyStatus.NORMAL);
    when(threadRepo.findById(10L)).thenReturn(Optional.of(thread));
    when(replyRepo.findById(99L)).thenReturn(Optional.of(reply));
    when(threadRepo.save(any(ForumThread.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ForumThread> result = service.acceptReply(10L, 99L, 20L, false);

    assertThat(result).isPresent();
    assertThat(result.get().getAcceptedReplyId()).isEqualTo(99L);
    assertThat(result.get().getAcceptedReplyUserId()).isEqualTo(30L);
    assertThat(result.get().getAcceptedAt()).isNotNull();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ForumThreadServiceSearchTest test`

Expected: compile failure because `acceptReply`, `ForumReplyRepository`, and accepted-reply fields are not implemented.

- [ ] **Step 3: Implement entity and service**

Add nullable fields to `ForumThread`:

```java
private Long acceptedReplyId;
private Long acceptedReplyUserId;
private Instant acceptedAt;
```

Inject `ForumReplyRepository` into `ForumThreadService` and implement:

```java
@Transactional
public Optional<ForumThread> acceptReply(Long threadId, Long replyId, Long userId, boolean canModerate) {
    return threadRepo.findById(threadId)
            .filter(t -> canModerate || t.getAuthorId().equals(userId))
            .flatMap(t -> replyRepo.findById(replyId)
                    .filter(r -> r.getThreadId().equals(threadId))
                    .filter(r -> r.getStatus() == ForumReply.ReplyStatus.NORMAL)
                    .map(r -> {
                        t.setAcceptedReplyId(r.getId());
                        t.setAcceptedReplyUserId(r.getAuthorId());
                        t.setAcceptedAt(Instant.now());
                        return threadRepo.save(t);
                    }));
}

@Transactional
public Optional<ForumThread> clearAcceptedReply(Long threadId, Long userId, boolean canModerate) {
    return threadRepo.findById(threadId)
            .filter(t -> canModerate || t.getAuthorId().equals(userId))
            .map(t -> {
                t.setAcceptedReplyId(null);
                t.setAcceptedReplyUserId(null);
                t.setAcceptedAt(null);
                return threadRepo.save(t);
            });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ForumThreadServiceSearchTest test`

Expected: PASS.

### Task 2: Backend Controller API

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`

- [ ] **Step 1: Add controller tests**

Add tests that verify authenticated users can forward accept/clear requests and anonymous users get 401.

- [ ] **Step 2: Implement endpoint**

Add:

```java
@PostMapping("/{id}/solution")
public ResponseEntity<?> acceptReply(@PathVariable Long id,
                                     @RequestBody Map<String, Long> body,
                                     Authentication auth) {
    Long userId = resolveUserId(auth);
    boolean canModerate = hasModerationRole(auth);
    if (userId == null && !canModerate) {
        return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
    }
    Long replyId = body.get("replyId");
    if (replyId == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "replyId 不能为空"));
    }
    return threadService.acceptReply(id, replyId, userId, canModerate)
            .map(t -> ResponseEntity.ok((Object) t))
            .orElse(ResponseEntity.status(403).body(Map.of("message", "无权采纳此回复")));
}
```

Add:

```java
@DeleteMapping("/{id}/solution")
public ResponseEntity<?> clearAcceptedReply(@PathVariable Long id, Authentication auth) {
    Long userId = resolveUserId(auth);
    boolean canModerate = hasModerationRole(auth);
    if (userId == null && !canModerate) {
        return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
    }
    return threadService.clearAcceptedReply(id, userId, canModerate)
            .map(t -> ResponseEntity.ok((Object) t))
            .orElse(ResponseEntity.status(403).body(Map.of("message", "无权取消采纳")));
}
```

- [ ] **Step 3: Run controller tests**

Run: `mvn -Dtest=ForumThreadControllerTest test`

Expected: PASS.

### Task 3: Frontend API And Detail UI

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [ ] **Step 1: Extend TypeScript API**

Add fields to `ForumThread`:

```ts
acceptedReplyId?: number
acceptedReplyUserId?: number
acceptedAt?: string
```

Add API methods:

```ts
acceptReply: (threadId: number, replyId: number) =>
  http.post<ForumThread>(`/forum/threads/${threadId}/solution`, { replyId }).then(r => r.data),
clearAcceptedReply: (threadId: number) =>
  http.delete<ForumThread>(`/forum/threads/${threadId}/solution`).then(r => r.data),
```

- [ ] **Step 2: Render solution state and controls**

In `ForumThreadDetail.vue`, show a solved badge when `thread.acceptedReplyId` exists. On each reply, add a solution marker for the accepted reply and show “采纳”/“取消采纳” controls to `canManageThread`.

- [ ] **Step 3: Build frontend**

Run: `npm run build`

Expected: PASS.

### Task 4: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend test suite**

Run: `mvn test`

Expected: all tests pass.

- [ ] **Step 2: Run frontend build**

Run: `npm run build`

Expected: production build succeeds.

- [ ] **Step 3: Review git diff**

Run: `git diff --stat`

Expected: changes are limited to forum accepted-reply backend/frontend files plus this plan.

