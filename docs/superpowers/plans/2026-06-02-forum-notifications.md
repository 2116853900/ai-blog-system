# Forum Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an account notification center so forum users can see when their threads or replies receive new replies.

**Architecture:** Treat notifications as a small Community bounded-context module with its own entity, repository, service, controller, DTO, and tests. `ForumReplyService` remains the reply use case owner and calls `NotificationService.notifyReplyCreated(...)` after a reply is persisted; listing, unread counts, and read-state mutations stay inside the notification module.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3, TypeScript, Vite.

---

## File Structure

- Create `backend/src/main/java/com/aiblog/entity/UserNotification.java`: notification aggregate persisted per recipient.
- Create `backend/src/main/java/com/aiblog/repository/UserNotificationRepository.java`: account list, unread count, recipient-scoped lookup.
- Create `backend/src/main/java/com/aiblog/dto/UserNotificationResponse.java`: frontend-safe notification payload.
- Create `backend/src/main/java/com/aiblog/service/NotificationService.java`: notification creation, listing, unread count, mark-read operations.
- Create `backend/src/main/java/com/aiblog/controller/AccountNotificationController.java`: `/api/account/notifications` endpoints.
- Modify `backend/src/main/java/com/aiblog/service/ForumReplyService.java`: inject `NotificationService` and emit notifications after reply creation.
- Create `backend/src/test/java/com/aiblog/service/NotificationServiceTest.java`: notification generation and read-state tests.
- Modify `frontend/src/api/types.ts`: add `UserNotification` and `NotificationType`.
- Modify `frontend/src/api/index.ts`: add account notification API methods.
- Modify `frontend/src/views/Account.vue`: add a notifications tab, unread badge, mark-read controls.
- Modify `README.md`: document account notification endpoints and smoke path.

---

### Task 1: Backend Notification Domain

**Files:**
- Create: `backend/src/main/java/com/aiblog/entity/UserNotification.java`
- Create: `backend/src/main/java/com/aiblog/repository/UserNotificationRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/UserNotificationResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/NotificationService.java`
- Create: `backend/src/test/java/com/aiblog/service/NotificationServiceTest.java`

- [ ] **Step 1: Write notification service tests**

Create tests for:

```java
@Test
void notifyReplyCreatedNotifiesThreadAuthorAndReplyAuthor()

@Test
void notifyReplyCreatedSkipsSelfNotificationsAndDuplicateRecipient()

@Test
void listMapsReadStateAndUnreadCount()

@Test
void markReadOnlyUpdatesRecipientOwnedNotification()
```

Run: `mvn -q -Dtest=NotificationServiceTest test`

Expected: FAIL because the notification classes do not exist.

- [ ] **Step 2: Add entity, repository, DTO, and service**

Create `UserNotification` with:

```java
public enum NotificationType { THREAD_REPLY, REPLY_REPLY }
```

Fields:
- `id`
- `userId`
- `actorId`
- `type`
- `title`
- `message`
- `linkUrl`
- `readAt`
- `createdAt`

`NotificationService` must provide:

```java
void notifyReplyCreated(ForumThread thread, ForumReply reply)
Page<UserNotificationResponse> list(Long userId, Pageable pageable)
long unreadCount(Long userId)
Optional<UserNotificationResponse> markRead(Long userId, Long notificationId)
int markAllRead(Long userId)
```

Rules:
- Thread author receives `THREAD_REPLY` unless replying to their own thread.
- Replied-to reply author receives `REPLY_REPLY` unless replying to their own reply.
- If the reply author replied directly to a reply by the thread author, do not create two notifications for the same recipient.
- Links point to `/forum/threads/{threadId}`.

- [ ] **Step 3: Run focused backend tests**

Run: `mvn -q -Dtest=NotificationServiceTest test`

Expected: PASS.

---

### Task 2: Backend API And Reply Integration

**Files:**
- Create: `backend/src/main/java/com/aiblog/controller/AccountNotificationController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumReplyService.java`
- Modify: `README.md`

- [ ] **Step 1: Add account notification endpoints**

Expose:

```text
GET  /api/account/notifications?page=&size=
GET  /api/account/notifications/unread-count
POST /api/account/notifications/{id}/read
POST /api/account/notifications/read-all
```

Account endpoints use the existing authenticated account boundary and resolve `ForumUser` by `Authentication.getName()`.

- [ ] **Step 2: Emit notifications from reply creation**

After `ForumReply saved = replyRepo.save(reply);` and thread metadata updates in `ForumReplyService.create(...)`, call:

```java
notificationService.notifyReplyCreated(thread, saved);
```

This keeps notification fan-out inside a service instead of controller code.

- [ ] **Step 3: Run backend tests**

Run: `mvn -q test`

Expected: PASS.

---

### Task 3: Frontend Notification API And Account UI

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Account.vue`

- [ ] **Step 1: Add frontend types and API methods**

Add:

```ts
export type NotificationType = 'THREAD_REPLY' | 'REPLY_REPLY'

export interface UserNotification {
  id: number
  type: NotificationType
  title: string
  message: string
  linkUrl: string
  actorId?: number
  read: boolean
  createdAt: string
  readAt?: string
}
```

Add account API methods:

```ts
notifications(params?: { page?: number; size?: number })
unreadNotificationCount()
markNotificationRead(id: number)
markAllNotificationsRead()
```

- [ ] **Step 2: Add notifications tab in account center**

Update `ActivityTab` to include `notifications`, load notifications with the existing pager, and render:
- title
- message
- created time
- unread chip for unread items
- item link to `linkUrl`
- a `全部已读` button when unread count is greater than zero

Keep existing tabs for threads, replies, post favorites, and resource favorites unchanged.

- [ ] **Step 3: Run frontend build**

Run: `npm run build`

Expected: PASS.

---

### Task 4: Full Verification

**Files:**
- No additional code files unless verification reveals a required fix.

- [ ] **Step 1: Run backend tests**

Run: `mvn -q test`

Expected: PASS.

- [ ] **Step 2: Run frontend production build**

Run: `npm run build`

Expected: PASS.

- [ ] **Step 3: Manual smoke path**

Without starting services automatically:
- Start backend/frontend manually if needed.
- User A creates a thread.
- User B replies to that thread.
- User A opens `/account`, enters `通知`, and sees the new unread notification.
- User A clicks the notification; it routes to the thread.
- User A marks one notification read and then uses `全部已读`.

---

## Self-Review

- Spec coverage: The plan includes architecture boundaries, backend notification generation, account APIs, frontend UI, docs, and verification.
- Placeholder scan: No task depends on vague or deferred behavior.
- Type consistency: Backend enum values and frontend union values both use `THREAD_REPLY` and `REPLY_REPLY`.
