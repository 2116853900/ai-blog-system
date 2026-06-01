# Function Gap Fill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill the highest-impact functional gaps found during the project check: forum search, thread editing, and editable user profiles.

**Architecture:** Keep the existing Vue/Spring Boot structure. Add small backend endpoints and query methods, then connect them to existing pages without changing database ownership boundaries or adding dependencies.

**Tech Stack:** Vue 3, TypeScript, Pinia, Spring Boot 3, Spring Data JPA, MySQL.

---

### Task 1: Forum Search API And UI

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumThreadService.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumThreadRepository.java`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Forum.vue`

- [ ] Add `q` support to `/api/forum/threads`.
- [ ] Query title, content markdown, and tags while excluding deleted threads.
- [ ] Add a compact search input on the forum page and preserve `q` in the route query.
- [ ] Verify with backend compile and frontend build.

### Task 2: Forum Thread Editing

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/ForumNew.vue`
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [ ] Add `/forum/threads/:id/edit` protected route.
- [ ] Reuse the existing thread editor for create and edit modes.
- [ ] Add an edit action for thread owners and moderators.
- [ ] Verify create mode still routes to the new thread and edit mode returns to the updated thread.

### Task 3: User Profile Editing

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ProfileUpdateRequest.java`
- Modify: `backend/src/main/java/com/aiblog/controller/AuthController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ForumUserService.java`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/Account.vue`

- [ ] Add `PUT /api/auth/profile` for forum users.
- [ ] Allow updating nickname, avatar URL, and bio with server-side length limits.
- [ ] Add a profile edit form to account center and refresh Pinia auth display data after save.
- [ ] Verify password change remains available.

### Validation

- [ ] Run `npm run build` in `frontend`.
- [ ] Run `mvn -q -DskipTests compile` in `backend`.
- [ ] Review `git diff --stat` and `git status --short`.
