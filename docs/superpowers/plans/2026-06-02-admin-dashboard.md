# Admin Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin dashboard that shows moderation workload and site totals with direct links to the relevant admin screens.

**Architecture:** Keep the dashboard read-only. The backend exposes one admin-only aggregate endpoint at `/api/admin/dashboard`; the frontend consumes that endpoint through the existing `adminApi` wrapper and renders a new `/admin` child route.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3, Vite, TypeScript.

---

## File Structure

- Modify: `backend/src/main/java/com/aiblog/repository/CommentRepository.java`
  - Add `countByApprovedFalseAndStatus(...)` for pending normal comments.
- Modify: `backend/src/main/java/com/aiblog/repository/SubmissionRepository.java`
  - Add `countByStatus(...)`.
- Modify: `backend/src/main/java/com/aiblog/repository/ContentReportRepository.java`
  - Add `countByStatus(...)`.
- Modify: `backend/src/main/java/com/aiblog/repository/ForumUserRepository.java`
  - Add `countByStatus(...)`.
- Create: `backend/src/main/java/com/aiblog/dto/AdminDashboardResponse.java`
  - Response DTO with `moderation`, `content`, `community`, and `apiStations` groups.
- Create: `backend/src/main/java/com/aiblog/service/AdminDashboardService.java`
  - Aggregates repository counts without changing data.
- Create: `backend/src/main/java/com/aiblog/controller/admin/AdminDashboardController.java`
  - Exposes `GET /api/admin/dashboard`.
- Create: `backend/src/test/java/com/aiblog/service/AdminDashboardServiceTest.java`
  - Verifies the aggregate counts and repository calls.
- Modify: `frontend/src/api/types.ts`
  - Add `AdminDashboard` interfaces.
- Modify: `frontend/src/api/index.ts`
  - Add `adminApi.dashboard()`.
- Create: `frontend/src/views/admin/AdminDashboard.vue`
  - Render workload cards, content totals, community totals, and API status summary.
- Modify: `frontend/src/router/index.ts`
  - Make `/admin` render the new dashboard instead of redirecting to posts.
- Modify: `frontend/src/views/admin/AdminLayout.vue`
  - Add dashboard navigation item.
- Modify: `README.md`
  - Mention the admin dashboard in the backend/admin feature list.

---

### Task 1: Backend Dashboard Aggregate

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/CommentRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/SubmissionRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ContentReportRepository.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ForumUserRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/AdminDashboardResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/AdminDashboardService.java`
- Create: `backend/src/main/java/com/aiblog/controller/admin/AdminDashboardController.java`

- [ ] **Step 1: Add repository count methods**

Add:

```java
long countByApprovedFalseAndStatus(Comment.CommentStatus status);
long countByStatus(Submission.Status status);
long countByStatus(ContentReport.ReportStatus status);
long countByStatus(ForumUser.Status status);
```

- [ ] **Step 2: Create `AdminDashboardResponse`**

Create immutable record DTOs:

```java
package com.aiblog.dto;

public record AdminDashboardResponse(
        Moderation moderation,
        Content content,
        Community community,
        ApiStations apiStations
) {
    public record Moderation(long pendingComments, long pendingSubmissions, long pendingReports) {}
    public record Content(long posts, long skills, long mcps, long apiStations) {}
    public record Community(long users, long activeUsers, long bannedUsers, long threads, long replies) {}
    public record ApiStations(long up, long down, long unknown) {}
}
```

- [ ] **Step 3: Create `AdminDashboardService`**

Inject the needed repositories and return one `AdminDashboardResponse`:

```java
return new AdminDashboardResponse(
        new AdminDashboardResponse.Moderation(
                commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL),
                submissionRepo.countByStatus(Submission.Status.PENDING),
                reportRepo.countByStatus(ContentReport.ReportStatus.PENDING)),
        new AdminDashboardResponse.Content(
                postRepo.count(),
                skillRepo.count(),
                mcpRepo.count(),
                apiStationRepo.count()),
        new AdminDashboardResponse.Community(
                forumUserRepo.count(),
                forumUserRepo.countByStatus(ForumUser.Status.ACTIVE),
                forumUserRepo.countByStatus(ForumUser.Status.BANNED),
                forumThreadRepo.count(),
                forumReplyRepo.count()),
        new AdminDashboardResponse.ApiStations(
                apiStationRepo.countByStatus(ApiStation.Status.UP),
                apiStationRepo.countByStatus(ApiStation.Status.DOWN),
                apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN)));
```

- [ ] **Step 4: Create `AdminDashboardController`**

Expose:

```java
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public AdminDashboardResponse overview() {
        return service.overview();
    }
}
```

---

### Task 2: Backend Test

**Files:**
- Create: `backend/src/test/java/com/aiblog/service/AdminDashboardServiceTest.java`

- [ ] **Step 1: Write aggregate test**

Use Mockito to mock repositories, seed count returns, call `overview()`, and assert:

```java
assertThat(response.moderation().pendingComments()).isEqualTo(2);
assertThat(response.moderation().pendingSubmissions()).isEqualTo(3);
assertThat(response.moderation().pendingReports()).isEqualTo(4);
assertThat(response.content().posts()).isEqualTo(5);
assertThat(response.community().bannedUsers()).isEqualTo(1);
assertThat(response.apiStations().down()).isEqualTo(2);
```

- [ ] **Step 2: Run backend test**

Run:

```bash
cd backend
mvn -q -Dtest=AdminDashboardServiceTest test
```

Expected: test passes.

---

### Task 3: Frontend Dashboard

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/admin/AdminDashboard.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/admin/AdminLayout.vue`

- [ ] **Step 1: Add TypeScript types**

Add:

```ts
export interface AdminDashboard {
  moderation: { pendingComments: number; pendingSubmissions: number; pendingReports: number }
  content: { posts: number; skills: number; mcps: number; apiStations: number }
  community: { users: number; activeUsers: number; bannedUsers: number; threads: number; replies: number }
  apiStations: { up: number; down: number; unknown: number }
}
```

- [ ] **Step 2: Add API wrapper**

Import `AdminDashboard` and add:

```ts
dashboard: () => http.get<AdminDashboard>('/admin/dashboard').then(r => r.data),
```

- [ ] **Step 3: Add dashboard route and menu item**

Route:

```ts
{ path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue'), meta: { requiresAdmin: true } },
```

Menu item:

```ts
{ to: '/admin', label: '总览' },
```

- [ ] **Step 4: Create `AdminDashboard.vue`**

Render loading/error/empty states and these cards:

- 待审核评论 -> `/admin/comments`
- 待审核投稿 -> `/admin/submissions`
- 待处理举报 -> `/admin/reports`
- 内容总量 -> posts/skills/mcps/apiStations
- 社区总量 -> users/threads/replies
- API 状态 -> up/down/unknown

---

### Task 4: Docs And Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README**

Mention that `/api/admin/dashboard` provides moderation and site aggregate statistics.

- [ ] **Step 2: Run full backend tests**

Run:

```bash
cd backend
mvn -q test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build pass.

---

## Self-Review

- Spec coverage: The selected feature adds a missing admin overview and preserves existing workflows.
- Placeholder scan: No TBD/TODO/fill-later implementation steps remain.
- Type consistency: Backend `AdminDashboardResponse` fields match frontend `AdminDashboard` names.
