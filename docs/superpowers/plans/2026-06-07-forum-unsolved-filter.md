# Forum Unsolved Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a forum list filter that lets users view all, unsolved, or solved threads.

**Architecture:** The backend already accepts `solved=true|false` and filters by `acceptedReplyId`; the work exposes that capability in the Vue forum page and locks the controller contract with a unit test. The frontend replaces the single "solved only" checkbox with a compact segmented status control that syncs to the existing `solved` query parameter.

**Tech Stack:** Spring Boot 3, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

### Project Analysis

The repository is an AI information site with a Spring Boot backend and Vue/Vite frontend. Public content includes tutorials, Skills, MCPs, API stations, comments, submissions, resource favorites/reviews, and a forum. The forum module already supports categories, keyword search, tag filtering, unanswered filtering, sorting, accepted replies, likes, favorites, reports, public profiles, and admin moderation.

The backend `ForumThreadService.search(...)` already has `Boolean solved`, where `true` means `acceptedReplyId is not null` and `false` means `acceptedReplyId is null`. The controller also calls search when `solved != null`. The missing piece is frontend UX: `frontend/src/views/Forum.vue` only exposes "只看已解决", so users cannot request `solved=false`.

### File Structure

- Modify: `frontend/src/views/Forum.vue`
  - Add a `SolveFilter` union type.
  - Derive initial value from `route.query.solved`.
  - Send `solved: false` for the unsolved filter and `solved: true` for the solved filter.
  - Render a three-option status segmented control.
  - Update active filter text and clear behavior.
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
  - Add a unit test that verifies `solved=false` is forwarded to `ForumThreadService.search(...)`.

### Task 1: Backend Controller Contract Test

**Files:**
- Modify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`

- [ ] **Step 1: Add a failing controller test for unsolved filtering**

Add this test after `listForwardsSolvedFilterWithSort()`:

```java
@Test
void listForwardsUnsolvedFilterWithSort() {
    ForumThreadService threadService = mock(ForumThreadService.class);
    ForumUserService userService = mock(ForumUserService.class);
    ForumThreadController controller = new ForumThreadController(threadService, userService);
    PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
    when(threadService.search(null, null, null, null, false, expectedPage)).thenReturn(page);

    Page<ForumThread> response = controller.list(null, null, null, null, false, "newest", 0, 20);

    assertThat(response).isSameAs(page);
    verify(threadService).search(null, null, null, null, false, expectedPage);
}
```

- [ ] **Step 2: Run the targeted test**

Run:

```bash
cd backend
mvn -Dtest=ForumThreadControllerTest test
```

Expected: PASS. This confirms the backend behavior already supports the new frontend control.

### Task 2: Frontend Forum Status Filter

**Files:**
- Modify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Add filter state and labels**

Replace the `solvedOnly` state with a `solveFilter` state:

```ts
type SolveFilter = 'all' | 'unsolved' | 'solved'

const solveFilterOptions: Array<{ value: SolveFilter; label: string }> = [
  { value: 'all', label: '全部状态' },
  { value: 'unsolved', label: '未解决' },
  { value: 'solved', label: '已解决' }
]

const solveFilter = ref<SolveFilter>(parseSolveFilter(route.query.solved))
```

Add these helpers near `parseSort(...)`:

```ts
function parseSolveFilter(value: unknown): SolveFilter {
  if (value === 'true') return 'solved'
  if (value === 'false') return 'unsolved'
  return 'all'
}

function solvedParam() {
  if (solveFilter.value === 'solved') return true
  if (solveFilter.value === 'unsolved') return false
  return undefined
}

function solveFilterLabel(value: SolveFilter) {
  return solveFilterOptions.find(option => option.value === value)?.label || '全部状态'
}
```

- [ ] **Step 2: Update active filters, query sync, and loading**

Use `solveFilter` instead of `solvedOnly`:

```ts
if (solveFilter.value !== 'all') filters.push(`状态：${solveFilterLabel(solveFilter.value)}`)
```

```ts
solved: solvedParam(),
```

```ts
if (solveFilter.value !== 'all') query.solved = String(solvedParam())
```

```ts
solveFilter.value = 'all'
```

Add the setter:

```ts
async function selectSolveFilter(value: SolveFilter) {
  if (solveFilter.value === value) return
  solveFilter.value = value
  page.value = 0
  await syncQuery()
  await loadThreads(0)
}
```

- [ ] **Step 3: Replace the solved checkbox in the template**

Replace the existing "只看已解决" checkbox with:

```vue
<div class="status-tabs" aria-label="解决状态筛选">
  <button
    v-for="option in solveFilterOptions"
    :key="option.value"
    type="button"
    :class="{ active: solveFilter === option.value }"
    @click="selectSolveFilter(option.value)"
  >
    {{ option.label }}
  </button>
</div>
```

Update the empty text condition to include:

```vue
solveFilter !== 'all'
```

- [ ] **Step 4: Add scoped styles**

Reuse the sort tab visual language by extending the selectors:

```css
.sort-tabs,
.status-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
}
.sort-tabs button,
.status-tabs button {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  font-size: 13px;
  padding: 7px 10px;
  white-space: nowrap;
}
.sort-tabs button:hover,
.sort-tabs button.active,
.status-tabs button:hover,
.status-tabs button.active {
  background: var(--primary-soft);
  color: var(--primary);
}
```

- [ ] **Step 5: Build the frontend**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS with `vue-tsc -b && vite build`.

### Task 3: Final Verification

**Files:**
- Verify: `backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java`
- Verify: `frontend/src/views/Forum.vue`

- [ ] **Step 1: Run targeted backend tests**

Run:

```bash
cd backend
mvn -Dtest=ForumThreadControllerTest test
```

Expected: PASS.

- [ ] **Step 2: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 3: Check changed files**

Run:

```bash
git diff -- docs/superpowers/plans/2026-06-07-forum-unsolved-filter.md backend/src/test/java/com/aiblog/controller/ForumThreadControllerTest.java frontend/src/views/Forum.vue
```

Expected: Diff only contains the plan, the controller test, and forum status filter UI.

### Self-Review

- Spec coverage: The plan analyzes the project, chooses one user-facing feature, adds a plan before implementation, implements the feature, and defines tests.
- Placeholder scan: No placeholder tasks are present.
- Type consistency: `SolveFilter`, `solveFilter`, `solvedParam`, and `selectSolveFilter` names are consistent across the frontend steps.
