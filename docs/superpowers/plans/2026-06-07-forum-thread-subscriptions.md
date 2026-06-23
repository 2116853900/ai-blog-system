# Forum Thread Subscriptions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add forum thread subscriptions so logged-in users can follow threads, receive reply notifications, and review subscribed threads from the account center.

**Architecture:** Store subscriptions in a dedicated `forum_thread_subscription` table keyed by `threadId + userId`. Extend the existing forum interaction service and response DTO so like/favorite/subscribe state is returned from one endpoint. Reuse the existing notification pipeline when a new reply is created, while excluding the reply author and users already notified as thread author or direct reply target.

**Tech Stack:** Spring Boot 3, Spring Data JPA, MySQL, JUnit 5/Mockito, Vue 3, Vite, TypeScript.

---

### File Structure

- Create: `backend/src/main/java/com/aiblog/entity/ForumThreadSubscription.java`
  - JPA entity for thread follow records.
- Create: `backend/src/main/java/com/aiblog/repository/ForumThreadSubscriptionRepository.java`
  - Idempotent insert/delete, existence checks, subscriber lookup, account listing.
- Modify: `backend/src/main/java/com/aiblog/entity/UserNotification.java`
  - Add `THREAD_SUBSCRIPTION_REPLY` notification type.
- Modify: `backend/src/main/java/com/aiblog/dto/ForumInteractionResponse.java`
  - Add `subscribed` state.
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
  - Add subscribe/unsubscribe/account-list methods and include state in `getInteraction`.
- Modify: `backend/src/main/java/com/aiblog/service/NotificationService.java`
  - Notify subscribers on reply creation.
- Modify: `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`
  - Add `POST/DELETE /api/forum/threads/{threadId}/subscription`.
- Modify: `backend/src/main/java/com/aiblog/controller/AccountActivityController.java`
  - Add `GET /api/account/subscriptions`.
- Modify: `frontend/src/api/types.ts`
  - Add `subscribed` and notification enum value.
- Modify: `frontend/src/api/index.ts`
  - Add forum subscription and account subscription APIs.
- Modify: `frontend/src/views/ForumThreadDetail.vue`
  - Add follow/unfollow button beside like/favorite.
- Modify: `frontend/src/views/Account.vue`
  - Add "帖子关注" activity tab.
- Test: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`
  - Cover idempotent subscribe/unsubscribe and account list.
- Test: `backend/src/test/java/com/aiblog/service/NotificationServiceTest.java`
  - Cover subscriber notification fan-out and exclusion rules.

### Task 1: Backend Subscription Model

- [ ] **Step 1: Create `ForumThreadSubscription` entity**

Create `backend/src/main/java/com/aiblog/entity/ForumThreadSubscription.java`:

```java
package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forum_thread_subscription",
        uniqueConstraints = @UniqueConstraint(name = "uk_forum_thread_subscription", columnNames = {"threadId", "userId"}),
        indexes = {
                @Index(name = "idx_subscription_thread", columnList = "threadId"),
                @Index(name = "idx_subscription_user_created", columnList = "userId,createdAt")
        })
public class ForumThreadSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long threadId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Create repository**

Create `backend/src/main/java/com/aiblog/repository/ForumThreadSubscriptionRepository.java` with MySQL `insert ignore`, delete, subscriber id lookup, and account listing.

### Task 2: Interaction API

- [ ] **Step 1: Extend response DTO**

Add `subscribed` to `ForumInteractionResponse` constructor, getters, and setters.

- [ ] **Step 2: Update service**

Inject `ForumThreadSubscriptionRepository`, include subscribed state in `getInteraction`, add `subscribe`, `unsubscribe`, and `listSubscribedThreads`.

- [ ] **Step 3: Update controller**

Add subscription endpoints under `/api/forum/threads/{threadId}` and account listing under `/api/account/subscriptions`.

### Task 3: Subscriber Notifications

- [ ] **Step 1: Add notification type**

Extend `UserNotification.NotificationType` with `THREAD_SUBSCRIPTION_REPLY`.

- [ ] **Step 2: Notify subscribers**

In `NotificationService.notifyReplyCreated`, collect existing direct recipients, then notify thread subscribers except reply author and existing recipients with title `你关注的帖子有新回复`.

### Task 4: Frontend

- [ ] **Step 1: Update API types and clients**

Add `subscribed` to `ForumInteraction`, `THREAD_SUBSCRIPTION_REPLY` to `NotificationType`, `subscribeThread`, `unsubscribeThread`, and `accountApi.subscriptions`.

- [ ] **Step 2: Update thread detail**

Add a button that toggles `interaction.subscribed`, redirects to login when anonymous, and displays `已关注` / `关注更新`.

- [ ] **Step 3: Update account center**

Add a `subscriptions` activity tab and render subscribed threads using the same list item treatment as favorites.

### Task 5: Verification

- [ ] **Step 1: Run targeted backend tests**

Run:

```bash
cd backend
mvn -Dtest=ForumInteractionServiceTest,NotificationServiceTest test
```

Expected: all selected tests pass.

- [ ] **Step 2: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build complete without errors.

- [ ] **Step 3: Run broader backend tests if targeted tests pass**

Run:

```bash
cd backend
mvn test
```

Expected: all backend tests pass.

### Self-Review

- Spec coverage: Adds a substantial user-facing feature across backend, frontend, notifications, and account center.
- Placeholder scan: No implementation placeholders are required by the plan; concrete files and behavior are specified.
- Type consistency: `subscribed` and `THREAD_SUBSCRIPTION_REPLY` are named consistently across backend DTOs and frontend types.
