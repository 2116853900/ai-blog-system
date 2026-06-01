# Report Operation Logs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show report review operation logs in the admin report detail modal so audit trails are visible where moderation decisions are made.

**Architecture:** Reuse the existing `AdminOperationLogRepository` target lookup pattern used by forum posts, replies, and users. Add a report-specific service/controller method for `CONTENT_REPORT` logs, then load the logs alongside report detail in the Vue admin report view.

**Tech Stack:** Spring Boot 3.3, Spring MVC, JPA repository, Vue 3 Composition API, TypeScript, Maven, Vite.

---

## File Structure

- Modify `backend/src/main/java/com/aiblog/service/ContentReportService.java`
  - Add `adminOperationLogs(Long id)` returning `CONTENT_REPORT` logs.
- Modify `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`
  - Add `GET /api/admin/reports/{id}/operation-logs`.
- Modify `frontend/src/api/index.ts`
  - Add `reportLogs(id)` API method.
- Modify `frontend/src/views/admin/AdminReports.vue`
  - Import `AdminOperationLog`, load logs in `openDetail`, clear logs on close, show operation records in the modal.
- Modify `docs/forum-governance-implementation-plan.md`
  - Append execution record and verification results.

## Task 1: Backend Report Log API

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/admin/AdminReportController.java`

- [x] **Step 1: Add service method**

Add this method to `ContentReportService` near the other admin query methods:

```java
public List<AdminOperationLog> adminOperationLogs(Long id) {
    return operationLogRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("CONTENT_REPORT", id);
}
```

- [x] **Step 2: Add controller endpoint**

Add imports and endpoint to `AdminReportController`:

```java
import com.aiblog.entity.AdminOperationLog;
import java.util.List;

@GetMapping("/{id}/operation-logs")
public List<AdminOperationLog> operationLogs(@PathVariable Long id) {
    return reportService.adminOperationLogs(id);
}
```

- [x] **Step 3: Compile backend**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compile succeeds.

## Task 2: Frontend Report Log Detail

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/admin/AdminReports.vue`

- [x] **Step 1: Add API method**

Add to the `adminApi` report section:

```ts
reportLogs: (id: number) =>
  http.get<AdminOperationLog[]>(`/admin/reports/${id}/operation-logs`).then(r => r.data),
```

- [x] **Step 2: Load logs in report detail**

In `AdminReports.vue`, import `AdminOperationLog`, add:

```ts
const logs = ref<AdminOperationLog[]>([])
```

Update `openDetail`:

```ts
const [reportDetail, operationLogs] = await Promise.all([
  adminApi.report(report.id),
  adminApi.reportLogs(report.id)
])
detail.value = reportDetail
logs.value = operationLogs
```

Update `closeDetail` to set `logs.value = []`.

- [x] **Step 3: Refresh logs after review actions**

After `approve`, `reject`, and `closeReport` update `detail.value`, reload:

```ts
logs.value = await adminApi.reportLogs(detail.value.id)
```

- [x] **Step 4: Render operation log list**

Add this block in the report detail modal after review result / review action:

```vue
<h3>操作记录</h3>
<p v-if="!logs.length" class="muted">暂无操作记录。</p>
<ul v-else class="log-list">
  <li v-for="log in logs" :key="log.id">
    <span class="mono">{{ log.action }}</span>
    <span>{{ log.operatorUsername }}</span>
    <span class="muted">{{ fmt(log.createdAt) }}</span>
    <p v-if="log.detail" class="muted">{{ log.detail }}</p>
  </li>
</ul>
```

Add CSS matching existing admin log lists:

```css
.log-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.log-list li { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; }
.log-list li > span { margin-right: 10px; }
.log-list p { margin: 6px 0 0; }
```

- [x] **Step 5: Build frontend**

Run:

```bash
npm run build
```

Expected: build succeeds.

## Task 3: Verify and Document

**Files:**
- Modify: `docs/forum-governance-implementation-plan.md`

- [x] **Step 1: Run backend tests**

Run:

```bash
mvn -q test
```

Expected: all tests pass.

- [x] **Step 2: Run final backend compile and frontend build**

Run:

```bash
mvn -q -DskipTests compile
npm run build
```

Expected: both commands pass.

- [x] **Step 3: Append execution record**

Append:

```markdown
## 19. 2026-06-01 举报操作记录可视化执行记录

- 新增 `GET /api/admin/reports/{id}/operation-logs`。
- 后台举报详情弹窗展示 `CONTENT_REPORT` 操作记录。
- 审核通过、驳回、关闭后会刷新操作记录。

验证结果：

- 后端测试：`mvn -q test` 通过。
- 后端编译：`mvn -q -DskipTests compile` 通过。
- 前端构建：`npm run build` 通过。
```

## Self-Review

- Spec coverage: Adds a concrete admin-facing governance feature that makes existing report audit logs usable.
- Placeholder scan: No placeholders or unspecified paths remain.
- Type consistency: Uses existing `AdminOperationLog`, `ContentReportService`, and `adminApi` naming conventions.
