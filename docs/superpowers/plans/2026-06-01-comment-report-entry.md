# Comment Report Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a front-end entry for logged-in users to report public comments, using the existing content report backend and admin review workflow.

**Architecture:** Reuse the existing `POST /api/reports` endpoint and `ReportTargetType = COMMENT`. Extend `CommentSection.vue` with a compact report button on each visible comment, a reason modal, login redirect for anonymous users, and toast feedback on submission. No backend schema or API changes are needed because `ContentReportService` already snapshots and reviews comments.

**Tech Stack:** Vue 3 Composition API, TypeScript, Pinia auth store, existing Spring Boot report API.

---

## File Structure

- Modify: `frontend/src/components/CommentSection.vue`
  - Add report button, reason modal, login redirect, and submit logic for comments.
- Modify: `README.md`
  - Document that logged-in users can report comments.

---

### Task 1: Frontend Comment Reporting

**Files:**
- Modify: `frontend/src/components/CommentSection.vue`

- [x] **Step 1: Add imports and state**

Update imports to include `reactive`, `useRoute`, `useRouter`, `forumApi`, `ReportReasonType`, and `useAuthStore`.

Add:

```ts
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const reportTarget = ref<Comment | null>(null)
const reportSaving = ref(false)
const reportForm = reactive<{ reasonType: ReportReasonType; reasonText: string }>({
  reasonType: 'SPAM',
  reasonText: ''
})
```

Define the same reason options used by forum reports:

```ts
const reportReasons: Array<{ value: ReportReasonType; label: string }> = [
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'ABUSE', label: '辱骂攻击' },
  { value: 'PORN', label: '色情低俗' },
  { value: 'POLITICS', label: '敏感内容' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'COPYRIGHT', label: '侵权' },
  { value: 'OTHER', label: '其他' }
]
```

- [x] **Step 2: Add modal control and submit functions**

Add:

```ts
function openReport(comment: Comment) {
  if (!auth.isLoggedIn()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  reportTarget.value = comment
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

function closeReport() {
  reportTarget.value = null
  reportForm.reasonType = 'SPAM'
  reportForm.reasonText = ''
}

async function submitReport() {
  if (!reportTarget.value) return
  reportSaving.value = true
  try {
    await forumApi.report({
      targetType: 'COMMENT',
      targetId: reportTarget.value.id,
      reasonType: reportForm.reasonType,
      reasonText: reportForm.reasonText.trim() || undefined
    })
    toast.success('举报已提交')
    closeReport()
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '举报失败')
  } finally {
    reportSaving.value = false
  }
}
```

- [x] **Step 3: Add report button and modal template**

In each comment header, add:

```vue
<button class="report-btn" type="button" @click="openReport(c)">举报</button>
```

Below the comment list, add a modal with reason select, optional text area, cancel button, and submit button.

- [x] **Step 4: Add scoped styles**

Add styles for `.comment-actions`, `.report-btn`, `.modal-mask`, `.modal`, `.modal-foot`, and mobile wrapping so comment metadata does not overlap.

- [x] **Step 5: Build frontend**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

---

### Task 2: Documentation and Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README**

Update the feature line to mention comment reporting. Update the end-to-end verification list with a step: log in, report a visible comment, then confirm it appears in `/admin/reports`.

- [x] **Step 2: Run frontend build**

Run from `frontend`:

```bash
npm run build
```

Expected: PASS.

- [x] **Step 3: Run backend tests**

Run from `backend`:

```bash
mvn -q test
```

Expected: PASS. Backend code should not change, but this verifies the existing report backend still passes.

---

## Self-Review

Spec coverage:
- Public comment report entry is covered in Task 1.
- Anonymous users are redirected to login before reporting.
- Existing admin report workflow receives `COMMENT` reports.
- Documentation and verification are covered in Task 2.

Placeholder scan:
- No TBD, TODO, or unspecified implementation steps remain.

Type consistency:
- Target type is the existing `COMMENT` value from `ReportTargetType`.
- Reason type uses the existing `ReportReasonType` union.
- API call reuses existing `forumApi.report`.
