# API Station Health Trends Finalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the API station health trends feature by tightening shared health analysis, removing small redundancies, preserving fallback behavior, and verifying backend/frontend integration.

**Architecture:** Keep persistence unchanged and continue routing all health statistics through `ApiStationHealthAnalyzer`. `ApiStationStatusHistoryService` should orchestrate repository reads and DTO assembly only; frontend `ApiStationHealth.vue` should keep dashboard/trend rendering in computed values instead of duplicating display rules in the template.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21 records, Mockito/JUnit 5/AssertJ, Vue 3 Composition API, TypeScript, Vite.

---

## File Structure

- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
  - Remove unused incident state and keep trend incident construction focused.
- Modify: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`
  - Add regression coverage for clamping, deleted station filtering, and open/resolved incident ordering.
- Modify: `frontend/src/views/ApiStationHealth.vue`
  - Add a typed incident display adapter so trend incidents and dashboard recent failures share one rendering path.
- Verify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
  - Confirm `/health-dashboard` and `/health-trends` remain before `/{id}`.
- Verify: `frontend/src/api/index.ts`
  - Confirm endpoint names and response types match backend DTO names.
- Verify: `frontend/src/api/types.ts`
  - Confirm optional fields map nullable backend fields correctly.
- Modify: `README.md`
  - Keep endpoint and frontend route documentation aligned with the final feature.

### Task 1: Tighten Incident Construction

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Test: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`

- [x] **Step 1: Remove unused incident field**

In `ApiStationStatusHistoryService.IncidentBuilder`, delete the unused field:

```java
private Instant lastFailureAt;
```

Then replace `add(...)` with:

```java
private void add(ApiStationStatusCheck failure) {
    failureCount++;
    if (failure.getErrorMessage() != null && !failure.getErrorMessage().isBlank()) {
        latestErrorMessage = failure.getErrorMessage();
    }
}
```

- [x] **Step 2: Add regression test for limit clamping and deleted stations**

Append this test to `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`:

```java
@Test
void healthTrendsClampsInputsAndIgnoresChecksForDeletedStations() {
    ApiStation existing = station(11L, "保留站", ApiStation.Status.UP, 90);
    when(stationRepo.findAll()).thenReturn(List.of(existing));
    List<ApiStationStatusCheck> checks = List.of(
            trendCheck(11L, 1L, ApiStation.Status.DOWN, null, "2026-06-08T01:00:00Z", "timeout"),
            trendCheck(99L, 2L, ApiStation.Status.DOWN, null, "2026-06-08T02:00:00Z", "deleted station"),
            trendCheck(11L, 3L, ApiStation.Status.UP, 110, "2026-06-08T03:00:00Z", null)
    );
    when(checkRepo.findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(any(Instant.class))).thenReturn(checks);

    var trends = service.healthTrends(0, 0);

    assertThat(trends.days()).isEqualTo(1);
    assertThat(trends.buckets()).hasSize(1);
    assertThat(trends.buckets().getFirst().sampleSize()).isEqualTo(3);
    assertThat(trends.incidents()).hasSize(1);
    assertThat(trends.incidents().getFirst().stationId()).isEqualTo(11L);
    assertThat(trends.incidents().getFirst().resolved()).isTrue();
    assertThat(trends.incidents().getFirst().latestErrorMessage()).isEqualTo("timeout");
}
```

- [x] **Step 3: Run targeted service test**

Run:

```powershell
cd backend
mvn -q -Dtest=ApiStationStatusHistoryServiceTest test
```

Expected: PASS.

- [x] **Step 4: Commit**

```powershell
git add backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java
git commit -m "test: harden api station health trend incidents"
```

### Task 2: Preserve Recent Failure Fallback in the Health Page

**Files:**
- Modify: `frontend/src/views/ApiStationHealth.vue`

- [x] **Step 1: Add a local display type**

In the `<script setup>` block, after `type LevelFilter = 'ALL' | ApiStationHealthLevel`, add:

```ts
type IncidentDisplayItem = {
  stationId: number
  stationName: string
  startedAt: string
  durationMinutes: number
  failureCount: number
  latestErrorMessage?: string
  resolved: boolean
}
```

- [x] **Step 2: Replace incident computed fallback**

Replace:

```ts
const incidentItems = computed(() => trends.value?.incidents ?? [])
```

with:

```ts
const incidentItems = computed<IncidentDisplayItem[]>(() => {
  if (trends.value) {
    return trends.value.incidents
  }
  return (dashboard.value?.recentFailures ?? []).map(failure => ({
    stationId: failure.stationId,
    stationName: failure.stationName,
    startedAt: failure.checkedAt,
    durationMinutes: 0,
    failureCount: 1,
    latestErrorMessage: failure.errorMessage,
    resolved: false
  }))
})
```

This keeps the existing incident template unchanged and avoids a second failure-list branch.

- [x] **Step 3: Keep trend panel conditional**

Confirm this block remains conditional so the page still renders when the dashboard succeeds but trends are unavailable:

```vue
<section v-if="trends" class="trend-panel card">
```

- [x] **Step 4: Run frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

- [x] **Step 5: Commit**

```powershell
git add frontend/src/views/ApiStationHealth.vue
git commit -m "fix: preserve api station failure fallback"
```

### Task 3: Verify Endpoint and DTO Alignment

**Files:**
- Verify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Verify: `backend/src/main/java/com/aiblog/dto/ApiStationHealthDashboardResponse.java`
- Verify: `backend/src/main/java/com/aiblog/dto/ApiStationHealthTrendResponse.java`
- Verify: `frontend/src/api/index.ts`
- Verify: `frontend/src/api/types.ts`
- Test: `backend/src/test/java/com/aiblog/controller/ApiStationControllerTest.java`

- [x] **Step 1: Confirm static routes precede `/{id}`**

In `ApiStationController`, verify this order:

```java
@GetMapping("/health-dashboard")
public ApiStationHealthDashboardResponse healthDashboard(...)

@GetMapping("/health-trends")
public ApiStationHealthTrendResponse healthTrends(...)

@GetMapping("/{id}")
public ResponseEntity<ApiStation> detail(@PathVariable Long id)
```

- [x] **Step 2: Confirm frontend API paths**

In `frontend/src/api/index.ts`, verify these client methods:

```ts
apiStationHealthDashboard: (params?: { sampleLimit?: number; failureLimit?: number }) =>
  http.get<ApiStationHealthDashboard>('/api-stations/health-dashboard', { params }).then(r => r.data),
apiStationHealthTrends: (params?: { days?: number; incidentLimit?: number }) =>
  http.get<ApiStationHealthTrendResponse>('/api-stations/health-trends', { params }).then(r => r.data),
```

- [x] **Step 3: Confirm nullable fields are optional in TypeScript**

In `frontend/src/api/types.ts`, nullable Java fields must remain optional:

```ts
averageLatencyMs?: number
endedAt?: string
latestErrorMessage?: string
```

- [x] **Step 4: Run controller tests**

Run:

```powershell
cd backend
mvn -q -Dtest=ApiStationControllerTest test
```

Expected: PASS.

### Task 4: Documentation and Full Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Confirm README endpoint rows**

The API table must include:

```markdown
| GET  | `/api/api-stations/health-dashboard?sampleLimit=&failureLimit=` | 公益 API 站点健康大盘：整体可用率、站点健康排行与最近故障 |
| GET  | `/api/api-stations/health-trends?days=&incidentLimit=` | 公益 API 站点趋势：按天聚合可用率、延迟与故障事件流 |
```

- [x] **Step 2: Confirm public frontend route**

The public route list must include:

```markdown
`/api-stations/health`
```

- [x] **Step 3: Run backend test suite**

Run:

```powershell
cd backend
mvn -q test
```

Expected: PASS.

- [x] **Step 4: Run frontend production build**

Run:

```powershell
cd frontend
npm run build
```

Expected: PASS.

- [x] **Step 5: Final self-review**

Check:

- `ApiStationHealthAnalyzer` remains the single place for count, latency, streak, and health-level rules.
- `ApiStationStatusHistoryService` has no duplicate statistics loops beyond current station status counts.
- `ApiStationHealth.vue` renders loading, error, empty state, zero-sample trend buckets, open incidents, resolved incidents, and dashboard failure fallback.
- No unrelated dirty files were reverted or reformatted.

- [x] **Step 6: Commit verification/documentation updates**

```powershell
git add README.md docs/superpowers/plans/2026-06-08-api-station-health-trends-finalization.md
git commit -m "docs: plan api station health trend finalization"
```
