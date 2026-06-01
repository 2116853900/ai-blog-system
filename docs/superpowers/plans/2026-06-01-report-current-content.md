# Report Current Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the currently stored reported content beside the report snapshot in the admin report detail modal.

**Architecture:** Keep `ContentReport` unchanged and add a small DTO for the current target view. The backend resolves the report target by `targetType + targetId`; the frontend loads this DTO with report detail and operation logs, then renders a separate current-content panel.

**Tech Stack:** Spring Boot 3.3, Spring MVC, JPA repositories, Vue 3 Composition API, TypeScript, Maven, Vite.

---

## File Structure

- Create `backend/src/main/java/com/aiblog/dto/ContentReportTargetResponse.java`
  - DTO for current reported content, status, author, and timestamps.
- Modify `backend/src/main/java/com/aiblog/service/ContentReportService.java`
  - Add `currentTarget(Long reportId)` and target-specific mapping helpers.
- Modify `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`
  - Add `GET /api/admin/reports/{id}/target`.
- Modify `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`
  - Cover current post content and missing target behavior.
- Modify `frontend/src/api/types.ts`
  - Add `ContentReportTarget` type.
- Modify `frontend/src/api/index.ts`
  - Add `reportTarget(id)` API method.
- Modify `frontend/src/views/admin/AdminReports.vue`
  - Load current target data and render “当前内容” beside “内容快照”.
- Modify `docs/forum-governance-implementation-plan.md`
  - Append execution record and verification results.

## Task 1: Backend Current Target API

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ContentReportTargetResponse.java`
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`

- [x] **Step 1: Create DTO**

Create `ContentReportTargetResponse` with fields:

```java
private ContentReport.TargetType targetType;
private Long targetId;
private boolean exists;
private String status;
private Long authorId;
private String authorName;
private String title;
private String content;
private String refType;
private Long refId;
private Instant createdAt;
private Instant updatedAt;
```

Include public getters and setters for every field.

- [x] **Step 2: Add service resolver**

Add this method to `ContentReportService`:

```java
public Optional<ContentReportTargetResponse> currentTarget(Long reportId) {
    return reportRepo.findById(reportId)
            .map(report -> currentTarget(report.getTargetType(), report.getTargetId()));
}
```

Add a `baseTarget` helper that initializes `targetType`, `targetId`, and `exists=false`. Add target-specific helpers:

```java
private ContentReportTargetResponse currentPostTarget(ContentReport.TargetType targetType, Long targetId)
private ContentReportTargetResponse currentReplyTarget(ContentReport.TargetType targetType, Long targetId)
private ContentReportTargetResponse currentCommentTarget(ContentReport.TargetType targetType, Long targetId)
```

Each helper should set `exists=true` when the target row still exists and fill status/content/author fields.

- [x] **Step 3: Add controller endpoint**

Add to `AdminReportController`:

```java
@GetMapping("/{id}/target")
public ResponseEntity<ContentReportTargetResponse> target(@PathVariable Long id) {
    return reportService.currentTarget(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

- [x] **Step 4: Compile backend**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compile succeeds.

## Task 2: Backend Tests

**Files:**
- Modify: `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`

- [x] **Step 1: Add current post target test**

Add a test that creates a `ContentReport` pointing to `POST #11`, stubs `threadRepo.findById(11L)`, calls `service.currentTarget(100L)`, and asserts:

```java
assertThat(target).isPresent();
assertThat(target.orElseThrow().isExists()).isTrue();
assertThat(target.orElseThrow().getTitle()).isEqualTo("当前标题");
assertThat(target.orElseThrow().getContent()).isEqualTo("当前正文");
assertThat(target.orElseThrow().getStatus()).isEqualTo("HIDDEN");
```

- [x] **Step 2: Add missing target test**

Add a test that points to a missing reply target and asserts:

```java
assertThat(target).isPresent();
assertThat(target.orElseThrow().isExists()).isFalse();
assertThat(target.orElseThrow().getTargetType()).isEqualTo(ContentReport.TargetType.REPLY);
assertThat(target.orElseThrow().getTargetId()).isEqualTo(77L);
```

- [x] **Step 3: Run focused tests**

Run:

```bash
mvn -q -Dtest=ContentReportServiceTest test
```

Expected: tests pass.

## Task 3: Frontend Current Content Panel

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/admin/AdminReports.vue`

- [x] **Step 1: Add TypeScript type**

Add:

```ts
export interface ContentReportTarget {
  targetType: ReportTargetType
  targetId: number
  exists: boolean
  status?: string
  authorId?: number
  authorName?: string
  title?: string
  content?: string
  refType?: string
  refId?: number
  createdAt?: string
  updatedAt?: string
}
```

- [x] **Step 2: Add API method**

Import/use `ContentReportTarget` and add:

```ts
reportTarget: (id: number) =>
  http.get<ContentReportTarget>(`/admin/reports/${id}/target`).then(r => r.data),
```

- [x] **Step 3: Load current target with detail**

In `AdminReports.vue`, import `ContentReportTarget`, add:

```ts
const currentTarget = ref<ContentReportTarget | null>(null)
```

In `openDetail`, load `adminApi.reportTarget(report.id)` together with detail and logs. Clear it in `closeDetail`. After approve/reject/close, reload current target as well as logs.

- [x] **Step 4: Render current content panel**

After “内容快照”, render:

```vue
<h3>当前内容</h3>
<div v-if="!currentTarget" class="note-box">
  <span class="muted">当前内容加载失败</span>
</div>
<div v-else-if="!currentTarget.exists" class="note-box">
  <span class="muted">当前内容不存在或已被删除。</span>
</div>
<div v-else class="current-box">
  <div class="detail-meta">
    <span v-if="currentTarget.status">状态 {{ currentTarget.status }}</span>
    <span v-if="currentTarget.authorId">作者 #{{ currentTarget.authorId }}</span>
    <span v-if="currentTarget.authorName">作者 {{ currentTarget.authorName }}</span>
    <span v-if="currentTarget.updatedAt">更新 {{ fmt(currentTarget.updatedAt) }}</span>
  </div>
  <strong v-if="currentTarget.title">{{ currentTarget.title }}</strong>
  <pre class="content-preview">{{ currentTarget.content || '-' }}</pre>
</div>
```

Add `.current-box { margin-bottom: 14px; }`.

- [x] **Step 5: Build frontend**

Run:

```bash
npm run build
```

Expected: build succeeds.

## Task 4: Verify and Document

**Files:**
- Modify: `docs/forum-governance-implementation-plan.md`

- [x] **Step 1: Run full backend tests**

Run:

```bash
mvn -q test
```

Expected: all tests pass.

- [x] **Step 2: Run final compile and build**

Run:

```bash
mvn -q -DskipTests compile
npm run build
```

Expected: both commands pass.

- [x] **Step 3: Append execution record**

Append:

```markdown
## 20. 2026-06-01 举报当前内容对照执行记录

- 新增 `GET /api/admin/reports/{id}/target`。
- 后台举报详情弹窗新增当前内容展示，和举报时快照并列用于审核对照。
- 当前目标不存在时会明确展示缺失状态。
- 补充当前目标解析服务测试。

验证结果：

- 后端测试：`mvn -q test` 通过。
- 后端编译：`mvn -q -DskipTests compile` 通过。
- 前端构建：`npm run build` 通过。
```

## Self-Review

- Spec coverage: Implements the original report-review requirement to view both snapshot and current source content.
- Placeholder scan: No placeholders or unspecified paths remain.
- Type consistency: DTO, service, controller, API, and frontend types all use `ContentReportTarget` / `ContentReportTargetResponse` naming consistently.
