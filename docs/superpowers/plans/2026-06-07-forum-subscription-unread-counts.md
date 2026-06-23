# Forum Subscription Unread Counts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make forum subscriptions scalable for users who follow many threads by adding unread update tracking, unread-only filtering, and follow/follower counts.

**Architecture:** Extend `forum_thread_subscription` with `lastReadAt` so the backend can tell whether a followed thread has replies newer than the user's last read marker. Return subscription-specific DTOs from account APIs instead of raw `ForumThread` entities, including `subscriberCount`, `unreadReplyCount`, `subscribedAt`, and `lastReadAt`. Keep the frontend account center dense and operational: a compact stats strip, an unread-only control, and per-thread unread badges.

**Tech Stack:** Spring Boot 3, Spring Data JPA, MySQL/Hibernate auto-DDL, JUnit 5/Mockito, Vue 3, Vite, TypeScript.

---

### Task 1: Backend Read State And Counts

**Files:**
- Modify: `backend/src/main/java/com/aiblog/entity/ForumThreadSubscription.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadSubscriptionRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumReplyRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/ForumSubscriptionSummaryResponse.java`
- Create: `backend/src/main/java/com/aiblog/dto/ForumThreadSubscriptionItemResponse.java`
- Modify: `backend/src/main/java/com/aiblog/dto/ForumInteractionResponse.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
- Test: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`

- [ ] **Step 1: Add read marker**

Add `lastReadAt` to `ForumThreadSubscription`. New subscriptions set it to creation time through the repository insert.

- [ ] **Step 2: Add repository queries**

Add count queries for subscribed thread count, received subscriber count, subscriber count per thread, unread subscribed thread count, paged subscription listing with `unreadOnly`, and `markRead`.

- [ ] **Step 3: Add response DTOs**

`ForumSubscriptionSummaryResponse` carries:

```java
long subscribedThreadCount;
long receivedSubscriberCount;
long unreadSubscribedThreadCount;
```

`ForumThreadSubscriptionItemResponse` carries thread fields plus:

```java
int subscriberCount;
long unreadReplyCount;
boolean unread;
Instant subscribedAt;
Instant lastReadAt;
String url;
```

- [ ] **Step 4: Update service**

Return subscriber counts in interaction responses, list subscription DTOs, return summary counts, and mark a subscription read when the user opens a followed thread.

### Task 2: API Surface

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/AccountActivityController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`
- Modify: `README.md`

- [ ] **Step 1: Account APIs**

Change `GET /api/account/subscriptions` to accept `unreadOnly` and return subscription item DTOs. Add `GET /api/account/subscription-summary`.

- [ ] **Step 2: Thread APIs**

Add `POST /api/forum/threads/{threadId}/subscription/read` to mark a followed thread as read.

### Task 3: Frontend UX

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ForumThreadDetail.vue`
- Modify: `frontend/src/views/Account.vue`

- [ ] **Step 1: API types**

Add `subscriberCount` to `ForumInteraction`, add `ForumSubscriptionSummary`, and add `ForumThreadSubscriptionItem`.

- [ ] **Step 2: Thread detail**

Show the thread subscriber count in the follow button and call the mark-read endpoint after loading a subscribed thread.

- [ ] **Step 3: Account center**

Add a compact stats strip for followed/follower/unread counts, add a subscriptions unread-only toggle, and show unread badges plus unread reply counts in the subscription list.

### Task 4: Verification

**Files:**
- Test: `backend/src/test/java/com/aiblog/service/ForumInteractionServiceTest.java`

- [ ] **Step 1: Run targeted backend tests**

Run:

```bash
cd backend
mvn "-Dtest=ForumInteractionServiceTest,NotificationServiceTest" test
```

Expected: all selected tests pass.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: `vue-tsc` and Vite build pass.

- [ ] **Step 3: Run full backend tests**

Run:

```bash
cd backend
mvn test
```

Expected: all backend tests pass.

### Self-Review

- Spec coverage: The plan handles high subscription volume through paginated unread filtering and gives both followed and follower-side counts.
- Placeholder scan: No TBD placeholders; all endpoints, DTOs, and verification commands are concrete.
- Type consistency: `subscriberCount`, `unreadReplyCount`, `unreadOnly`, and summary names are consistent across backend and frontend.
