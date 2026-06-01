# User History And Comment Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add admin-visible forum user content/report history and make ordinary comment moderation consistent with forum post/reply governance.

**Architecture:** Reuse the existing admin forum search services for user thread/reply history so hidden and deleted records remain visible to admins. Add focused report history queries by reporter and target author. Move admin comment mutations behind a small service that performs status transitions, soft deletion, and operation logging.

**Tech Stack:** Spring Boot 3, Spring Data JPA, MySQL, Vue 3, TypeScript, Vite.

---

## File Structure

- Modify `backend/src/main/java/com/aiblog/repository/ContentReportRepository.java`: add pageable report history queries by `reporterId` and `targetAuthorId`.
- Modify `backend/src/main/java/com/aiblog/repository/CommentRepository.java`: add status-aware admin list query methods.
- Modify `backend/src/main/java/com/aiblog/service/ContentReportService.java`: expose submitted and received report history pages for user detail.
- Create `backend/src/main/java/com/aiblog/service/AdminCommentService.java`: status-aware comment listing, approve, hide, restore, and soft delete with admin operation logs.
- Modify `backend/src/main/java/com/aiblog/controller/admin/AdminForumUserController.java`: add `/threads`, `/replies`, `/reports`, and `/reported` detail-history endpoints.
- Modify `backend/src/main/java/com/aiblog/controller/admin/AdminCommentController.java`: switch to `AdminCommentService`, add status filtering, hide, restore, and soft-delete behavior.
- Modify `frontend/src/api/types.ts`: export `CommentStatus` and use it on `Comment`.
- Modify `frontend/src/api/index.ts`: add user-history APIs and status-aware comment governance APIs.
- Modify `frontend/src/views/admin/AdminUsers.vue`: add tabs in the detail modal for overview, posts, replies, submitted reports, and received reports.
- Modify `frontend/src/views/admin/AdminComments.vue`: add comment status filter, status badges, hide, restore, and soft-delete actions.
- Modify `docs/forum-governance-implementation-plan.md`: append execution record and validation results.

---

### Task 1: Backend User History APIs

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/ContentReportRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminForumUserController.java`

- [x] Add `Page<ContentReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable)` to `ContentReportRepository`.
- [x] Add `Page<ContentReport> findByTargetAuthorIdOrderByCreatedAtDesc(Long targetAuthorId, Pageable pageable)` to `ContentReportRepository`.
- [x] Add `submittedByUser(Long userId, Pageable pageable)` and `receivedByUser(Long userId, Pageable pageable)` to `ContentReportService`.
- [x] Inject `ForumThreadService`, `ForumReplyService`, and `ContentReportService` into `AdminForumUserController`.
- [x] Add `GET /api/admin/users/{id}/threads`, using `ForumThreadService.adminSearch(null, null, id, null, null, null, null, pageable)`.
- [x] Add `GET /api/admin/users/{id}/replies`, using `ForumReplyService.adminSearch(null, null, id, null, null, null, null, pageable)`.
- [x] Add `GET /api/admin/users/{id}/reports`, returning reports where the user is the reporter.
- [x] Add `GET /api/admin/users/{id}/reported`, returning reports where the user is the target author.

### Task 2: Backend Comment Governance

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/CommentRepository.java`
- Create: `backend/src/main/java/com/aiblog/service/AdminCommentService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminCommentController.java`

- [x] Add `findByApprovedFalseAndStatusOrderByCreatedAtDesc`, `findByStatusOrderByCreatedAtDesc`, and `findAllByOrderByCreatedAtDesc` to `CommentRepository`.
- [x] Create `AdminCommentService` with `list(Boolean pending, Comment.CommentStatus status)`.
- [x] Make pending comment lists default to `NORMAL` comments unless a status filter is explicitly provided.
- [x] Add `approve(Long id, String operatorUsername)` that sets `approved=true` and records `APPROVE_COMMENT`.
- [x] Add `hide(Long id, String operatorUsername)` that changes non-deleted comments to `HIDDEN` and records `HIDE_COMMENT`.
- [x] Add `restore(Long id, String operatorUsername)` that changes comments to `NORMAL` and records `RESTORE_COMMENT`.
- [x] Add `softDelete(Long id, String operatorUsername)` that changes comments to `DELETED` and records `DELETE_COMMENT`.
- [x] Update `AdminCommentController` to support `GET /api/admin/comments?pending=true&status=NORMAL`.
- [x] Add `POST /api/admin/comments/{id}/hide` and `POST /api/admin/comments/{id}/restore`.
- [x] Keep `DELETE /api/admin/comments/{id}` but make it soft-delete instead of hard-delete.

### Task 3: Frontend User Detail History Tabs

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/admin/AdminUsers.vue`

- [x] Export `CommentStatus` from `types.ts` without changing the existing `Comment` payload shape.
- [x] Add API methods:
  - `adminApi.forumUserThreads(id, params)`
  - `adminApi.forumUserReplies(id, params)`
  - `adminApi.forumUserReports(id, params)`
  - `adminApi.forumUserReported(id, params)`
- [x] Add `overview`, `threads`, `replies`, `reports`, and `reported` tabs to the user detail modal.
- [x] Load each non-overview tab lazily when selected and keep a separate page number for each tab.
- [x] Show thread history with title, status, replies, reports, and creation time.
- [x] Show reply history with thread id, floor number, status, reports, and creation time.
- [x] Show submitted/received report history with target, reason, status, and creation time.

### Task 4: Frontend Comment Governance

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/admin/AdminComments.vue`

- [x] Change `adminApi.comments` to accept `{ pending?: boolean; status?: CommentStatus }`.
- [x] Add `adminApi.hideComment(id)` and `adminApi.restoreComment(id)`.
- [x] Add a status filter with `全部状态`, `正常`, `已隐藏`, and `已删除`.
- [x] Display both approval and moderation status badges on each comment.
- [x] Show actions:
  - `通过` when `approved=false`.
  - `隐藏` when `status=NORMAL`.
  - `恢复` when `status=HIDDEN` or `status=DELETED`.
  - `软删除` when `status` is not `DELETED`.
- [x] Reload the list after each successful operation.

### Task 5: Documentation And Verification

**Files:**
- Modify: `docs/forum-governance-implementation-plan.md`

- [x] Append a 2026-06-01 execution record for user history and comment governance.
- [x] Run backend verification: `mvn -q -DskipTests compile` from `backend`.
- [x] Run frontend verification: `npm run build` from `frontend`.
- [x] Review `git status --short` and `git diff --stat`.
