# Report Review Finality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent already-reviewed content reports from being reviewed again and retriggering moderation side effects.

**Architecture:** Keep the protection in `ContentReportService` so all controller and future service callers share the same state transition rule. Treat non-`PENDING` reports as terminal records and return them unchanged without writing review logs or applying hide/ban actions.

**Tech Stack:** Spring Boot service layer, JUnit 5, Mockito, AssertJ.

---

### Task 1: Guard Terminal Report Review State

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Test: `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`
- Document: `docs/forum-governance-implementation-plan.md`

- [x] **Step 1: Add a failing service test**

Add `approveDoesNotReprocessReviewedReport` to `ContentReportServiceTest`. The test builds an already `APPROVED` report, calls `approve(...)` again with hide and ban options enabled, and verifies the original review fields are unchanged.

- [x] **Step 2: Assert no duplicate side effects**

In the same test, verify `reportRepo.save(...)`, `threadService.hide(...)`, `userService.ban(...)`, and `operationLogRepo.save(...)` are never called.

- [x] **Step 3: Add service-layer transition guard**

Update `ContentReportService.approve`, `reject`, and `close` to apply review changes only when the report status is `PENDING`.

- [x] **Step 4: Keep review persistence centralized**

Move the shared mutation logic into `applyReview(...)`, and add `isPending(...)` to make the terminal-state rule explicit.

- [x] **Step 5: Update implementation record**

Append section 21 to `docs/forum-governance-implementation-plan.md` documenting the terminal-state protection and verification status.

- [x] **Step 6: Run final verification**

Run:

```bash
mvn -q test
mvn -q -DskipTests compile
npm run build
git diff --check
```

Expected: all commands pass before committing.
