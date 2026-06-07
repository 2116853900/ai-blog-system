# Forum Solved Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a forum list filter for solved threads based on accepted-reply state.

**Architecture:** Extend the existing forum thread list query with an optional `solved` Boolean parameter. Backend filtering uses `acceptedReplyId is not null`; frontend passes `solved=true` from the forum list and preserves it in the URL query.

**Tech Stack:** Spring Boot 3.3, Spring Data JPA Specification, JUnit 5/Mockito, Vue 3, TypeScript.

---

### Task 1: Backend Solved Query Parameter

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`

- [ ] **Step 1: Add a controller forwarding test**

Add a test that calls:

```java
controller.list(null, null, null, null, true, "newest", 0, 20);
```

Expected service call:

```java
verify(threadService).search(null, null, null, null, true, expectedPage);
```

- [ ] **Step 2: Implement `solved` parameter**

Add `@RequestParam(required = false) Boolean solved` to `ForumThreadController.list`, pass it into `threadService.search`, and include `solved != null` in the branch condition.

Add `Boolean solved` to `ForumThreadService.search`. Apply:

```java
if (Boolean.TRUE.equals(solved)) {
    predicates.add(cb.isNotNull(root.get("acceptedReplyId")));
} else if (Boolean.FALSE.equals(solved)) {
    predicates.add(cb.isNull(root.get("acceptedReplyId")));
}
```

- [ ] **Step 3: Run backend targeted tests**

Run: `mvn -Dtest=ForumThreadControllerTest,ForumThreadServiceSearchTest test`

Expected: PASS.

### Task 2: Frontend Solved Filter

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Extend API params**

Add `solved?: boolean` to `forumApi.threads` params.

- [ ] **Step 2: Add forum UI state**

Add `solvedOnly` from `route.query.solved === 'true'`, include it in API params and URL query, render a “只看已解决” checkbox next to “只看未回复”, and reset it in `clearAllFilters`.

- [ ] **Step 3: Run frontend build**

Run: `npm run build`

Expected: PASS.

### Task 3: Full Verification

Run `mvn test` in `backend` and `npm run build` in `frontend`.

