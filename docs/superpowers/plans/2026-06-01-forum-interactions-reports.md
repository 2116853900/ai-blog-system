# Forum Interactions And Reports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add forum thread like/favorite interactions, user-submitted content reports, and an admin report review workflow.

**Architecture:** Keep interaction writes in a dedicated service so count updates and unique user actions stay transactional. Put report submission and review in a report service that snapshots content, increments report counters, and reuses existing forum governance services for hide/ban side effects. Add small Vue admin and thread-detail UI surfaces using the existing API client and table/modal patterns.

**Tech Stack:** Spring Boot 3, Spring Data JPA, MySQL, Vue 3, TypeScript, Vite.

---

## File Structure

- Create `backend/src/main/java/com/aiblog/dto/ForumInteractionResponse.java`: response for like/favorite state and counts.
- Create `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`: idempotent like/favorite mutations and count reads.
- Create `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`: public/user interaction endpoints under `/api/forum/threads/{id}`.
- Create `backend/src/main/java/com/aiblog/dto/ContentReportRequest.java`: user report payload.
- Create `backend/src/main/java/com/aiblog/dto/ReportReviewRequest.java`: admin review options such as note, hide content, and ban target author.
- Create `backend/src/main/java/com/aiblog/service/ContentReportService.java`: report submission, admin search, approval/rejection/close, and side effects.
- Create `backend/src/main/java/com/aiblog/controller/ReportController.java`: `POST /api/reports`.
- Create `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`: admin report list/detail/review endpoints.
- Modify `backend/src/main/java/com/aiblog/controller/CommentController.java`: reject comments from banned forum users when authenticated as one.
- Modify `frontend/src/api/types.ts`: add interaction and report types.
- Modify `frontend/src/api/index.ts`: add forum interaction/report APIs and admin report APIs.
- Modify `frontend/src/views/ForumThreadDetail.vue`: add like/favorite controls and report modal for posts/replies.
- Create `frontend/src/views/admin/AdminReports.vue`: admin report list, detail, and review UI.
- Modify `frontend/src/router/index.ts`: add `/admin/reports`.
- Modify `frontend/src/views/admin/AdminLayout.vue`: add reports menu item.
- Modify `docs/forum-governance-implementation-plan.md`: append execution record.

---

### Task 1: Thread Like And Favorite API

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ForumInteractionResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/ForumInteractionService.java`
- Create: `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [x] Add a `ForumInteractionResponse` with `liked`, `favorited`, `likeCount`, and `favoriteCount`.
- [x] Implement `ForumInteractionService.like`, `unlike`, `favorite`, `unfavorite`, and `getInteraction`.
- [x] Ensure duplicate like/favorite calls do not create duplicate rows and unlike/unfavorite never make counts negative.
- [x] Add endpoints:
  - `GET /api/forum/threads/{threadId}/interaction`
  - `POST /api/forum/threads/{threadId}/like`
  - `DELETE /api/forum/threads/{threadId}/like`
  - `POST /api/forum/threads/{threadId}/favorite`
  - `DELETE /api/forum/threads/{threadId}/favorite`
- [x] Return `401` when a mutating interaction has no forum user and `403` when the forum user is banned.
- [x] Add TypeScript API methods and show like/favorite buttons on thread detail.
- [x] Run:
  - `mvn -q -DskipTests compile`
  - `npm run build`

### Task 2: User Report Submission

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ContentReportRequest.java`
- Create: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Create: `backend/src/main/java/com/aiblog/controller/ReportController.java`
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ForumThreadDetail.vue`

- [x] Add `ContentReportRequest` with `targetType`, `targetId`, `reasonType`, and `reasonText`.
- [x] Resolve report snapshots for:
  - `POST`: forum thread title and markdown body.
  - `REPLY`: forum reply markdown body.
  - `COMMENT`: comment author and content.
- [x] Save reports as `PENDING` with `reporterId`, `targetAuthorId`, and `contentSnapshot`.
- [x] Increment `ForumThread.reportCount` for post reports and `ForumReply.reportCount` for reply reports.
- [x] Add `POST /api/reports`.
- [x] Reject report submission with `401` when unauthenticated and `403` when the forum user is banned.
- [x] Add a report modal in thread detail for posts and replies with a reason select and note textarea.
- [x] Run:
  - `mvn -q -DskipTests compile`
  - `npm run build`

### Task 3: Admin Report Review Workflow

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ReportReviewRequest.java`
- Create: `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/admin/AdminReports.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/admin/AdminLayout.vue`

- [x] Add admin report endpoints:
  - `GET /api/admin/reports`
  - `GET /api/admin/reports/{id}`
  - `POST /api/admin/reports/{id}/approve`
  - `POST /api/admin/reports/{id}/reject`
  - `POST /api/admin/reports/{id}/close`
- [x] Filter reports by target type, reason type, status, and created time.
- [x] Approval writes reviewer username, review note, reviewed time, and status `APPROVED`.
- [x] Approval optionally hides the target content using existing thread/reply governance methods, or comment status `HIDDEN`.
- [x] Approval optionally bans the target author when `targetAuthorId` is present.
- [x] Reject and close actions preserve the report and write review metadata.
- [x] Add `/admin/reports` page with filters, detail modal, snapshot display, and review actions.
- [x] Run:
  - `mvn -q -DskipTests compile`
  - `npm run build`

### Task 4: Documentation And Verification

**Files:**
- Modify: `docs/forum-governance-implementation-plan.md`

- [x] Append a dated implementation record covering interactions and reports.
- [x] Run final verification:
  - `mvn -q -DskipTests compile`
  - `npm run build`
- [x] Review `git status --short` and `git diff --stat`.

