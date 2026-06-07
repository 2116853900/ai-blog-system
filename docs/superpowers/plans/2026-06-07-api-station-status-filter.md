# API Station Status Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public API station status filter so users can view all, online, offline, or unchecked stations.

**Architecture:** The backend accepts an optional `status=UP|DOWN|UNKNOWN` query parameter and combines it with the existing keyword/tag JPA specification. The public cache key includes status to prevent cached all-status results from being reused for filtered requests. The Vue list page passes the selected status to the existing list loader and renders a compact segmented status control in the toolbar.

**Tech Stack:** Spring Boot 3, Spring Data JPA Specification, JUnit 5, Mockito, Vue 3, TypeScript, Vite.

---

### Project Analysis

The project is an AI information site with a Spring Boot backend and Vue/Vite frontend. Public modules include tutorials, Skills, MCPs, API stations, comments, submissions, resource ratings/favorites, and a forum. The API station module already tracks `ApiStation.Status` as `UP`, `DOWN`, or `UNKNOWN`, renders status badges on cards, and has an index on the `status` column. The public `/api/api-stations` endpoint currently supports only keyword and tag filters, so users cannot narrow the list to online/offline stations even though the data exists.

### File Structure

- Modify: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
  - Add `status` to `apiStationsListKey(...)` so cached list entries are separated by status.
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
  - Add optional `ApiStation.Status status` request parameter.
  - Combine `SearchSpecs.build(...)` with a status equality specification.
- Modify: `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java`
  - Add a controller-level test that verifies the status-filtered list path calls the repository and returns the list.
- Modify: `frontend/src/api/index.ts`
  - Add `status?: ApiStatus` to `publicApi.apiStations(...)`.
- Modify: `frontend/src/views/ApiStations.vue`
  - Add a status segmented control.
  - Pass the selected status into the list fetcher.
  - Include status in clear-filter behavior and empty-state text.

### Task 1: Backend Status Query and Cache Key

**Files:**
- Modify: `backend/src/main/java/com/aiblog/cache/PublicContentCacheService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Test: `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java`

- [ ] **Step 1: Add a status-filter list test**

Add imports:

```java
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
```

Add this test after `apiStationDetailReturnsNotFoundWhenMissing()`:

```java
@Test
void apiStationListForwardsStatusFilter() {
    ApiStationRepository repo = mock(ApiStationRepository.class);
    ApiStation station = new ApiStation();
    station.setId(3L);
    station.setName("Fast Relay");
    station.setBaseUrl("https://relay.example.com");
    station.setStatus(ApiStation.Status.UP);
    Sort expectedSort = Sort.by(Sort.Direction.ASC, "name");
    when(repo.findAll(any(Specification.class), eq(expectedSort))).thenReturn(List.of(station));
    ApiStationController controller = new ApiStationController(repo, mock(ApiStationStatusHistoryService.class), cacheService());

    List<ApiStation> response = controller.list(null, null, ApiStation.Status.UP);

    assertThat(response).containsExactly(station);
    verify(repo).findAll(any(Specification.class), eq(expectedSort));
}
```

- [ ] **Step 2: Update cache key signature**

Replace:

```java
public String apiStationsListKey(String q, String tag) {
    return API_STATIONS_PREFIX + "list:" + params(q, tag, null);
}
```

with:

```java
public String apiStationsListKey(String q, String tag, ApiStation.Status status) {
    return API_STATIONS_PREFIX + "list:" + params(q, tag, status);
}
```

Add import:

```java
import com.aiblog.entity.ApiStation;
```

- [ ] **Step 3: Add backend status filtering**

Change the controller list signature to:

```java
public List<ApiStation> list(@RequestParam(required = false) String q,
                             @RequestParam(required = false) String tag,
                             @RequestParam(required = false) ApiStation.Status status) {
```

Inside the cache loader, build a composed specification:

```java
var spec = SearchSpecs.<ApiStation>build(q, tag, null, List.of("name", "description", "supportedModels", "tags"));
if (status != null) {
    spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
}
return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));
```

Also update the cache key call:

```java
cacheService.apiStationsListKey(q, tag, status)
```

- [ ] **Step 4: Run targeted backend test**

Run:

```bash
cd backend
mvn -Dtest=PublicResourceDetailControllerTest test
```

Expected: PASS.

### Task 2: Frontend Status Filter UI

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ApiStations.vue`

- [ ] **Step 1: Add API typing**

Change:

```ts
apiStations: (params?: { q?: string; tag?: string }) =>
```

to:

```ts
apiStations: (params?: { q?: string; tag?: string; status?: ApiStatus }) =>
```

- [ ] **Step 2: Add status state and fetcher**

In `ApiStations.vue`, import `computed` and `ApiStatus`:

```ts
import { computed, ref } from 'vue'
import type { ApiStation, ApiStatus } from '../api/types'
```

Add before `useListView(...)`:

```ts
type StatusFilter = 'ALL' | ApiStatus

const statusOptions: Array<{ value: StatusFilter; label: string; tone: string }> = [
  { value: 'ALL', label: '全部状态', tone: 'all' },
  { value: 'UP', label: '在线', tone: 'up' },
  { value: 'DOWN', label: '离线', tone: 'down' },
  { value: 'UNKNOWN', label: '未检测', tone: 'unknown' }
]
const statusFilter = ref<StatusFilter>('ALL')

function statusParam() {
  return statusFilter.value === 'ALL' ? undefined : statusFilter.value
}
```

Pass a fetcher closure:

```ts
} = useListView<ApiStation>((params) => publicApi.apiStations({
  ...params,
  status: statusParam()
}))
```

Add:

```ts
const hasAnyFilter = computed(() => hasFilter.value || statusFilter.value !== 'ALL')

function selectStatus(value: StatusFilter) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  load()
}

function resetFilters() {
  statusFilter.value = 'ALL'
  reset()
}
```

- [ ] **Step 3: Render status segmented control**

In the toolbar, after `SearchBar`, add:

```vue
<div class="status-filter" aria-label="API 状态筛选">
  <button
    v-for="option in statusOptions"
    :key="option.value"
    type="button"
    :class="['status-option', `tone-${option.tone}`, { active: statusFilter === option.value }]"
    @click="selectStatus(option.value)"
  >
    <span aria-hidden="true"></span>
    {{ option.label }}
  </button>
</div>
```

Change the clear button to:

```vue
<button v-if="hasAnyFilter" class="btn btn-sm" @click="resetFilters">清除筛选 ✕</button>
```

Change the empty text to:

```vue
<StateBlock :loading="loading" :empty="isEmpty" :empty-text="hasAnyFilter ? '没有匹配的 API 站点。' : '暂无 API 站点。'" class="block-area">
```

- [ ] **Step 4: Add status filter styles**

Add scoped CSS:

```css
.status-filter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  overflow-x: auto;
}
.status-option {
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 7px 10px;
  white-space: nowrap;
}
.status-option span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}
.status-option:hover,
.status-option.active {
  background: var(--primary-soft);
  color: var(--primary);
}
.status-option.tone-up.active { color: var(--accent); }
.status-option.tone-down.active { color: var(--danger); }
.status-option.tone-unknown.active { color: var(--text-soft); }
```

- [ ] **Step 5: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

### Task 3: Final Verification

**Files:**
- Verify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Verify: `frontend/src/views/ApiStations.vue`

- [ ] **Step 1: Run targeted backend tests**

Run:

```bash
cd backend
mvn -Dtest=PublicResourceDetailControllerTest test
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
git status --short
git diff --stat
```

Expected: Changes include this plan plus the API station backend/frontend files. Existing unrelated uncommitted changes from earlier work remain untouched.

### Self-Review

- Spec coverage: The plan analyzes the project, adds one public user-facing feature, starts with a written plan, implements backend and frontend behavior, and defines tests.
- Placeholder scan: No placeholders are present.
- Type consistency: `ApiStation.Status`, `ApiStatus`, `StatusFilter`, `statusFilter`, and `statusParam()` are used consistently.
