# API Station Availability Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public API station availability analytics feature that summarizes recent health checks and shows the result on the API station detail page.

**Architecture:** Reuse the existing `api_station_status_check` history table. Add a response DTO and service method that computes summary metrics from the latest normalized window, expose it through `GET /api/api-stations/{id}/checks/summary`, then render a compact terminal-style analytics panel beside the existing recent-check list.

**Tech Stack:** Spring Boot 3, Spring Data JPA, JUnit 5/Mockito, Vue 3, TypeScript, Vite.

---

### Task 1: Backend Summary API

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ApiStationStatusSummaryResponse.java`
- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Test: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`

- [ ] **Step 1: Add DTO**

Create `ApiStationStatusSummaryResponse` with these fields:

```java
public record ApiStationStatusSummaryResponse(
        Long stationId,
        int sampleSize,
        int upCount,
        int downCount,
        int unknownCount,
        double uptimeRate,
        Integer averageLatencyMs,
        Integer fastestLatencyMs,
        Integer slowestLatencyMs,
        Instant firstCheckedAt,
        Instant lastCheckedAt,
        int longestFailureStreak,
        ApiStation.Status currentStatus
) {}
```

- [ ] **Step 2: Add service tests**

Extend `ApiStationStatusHistoryServiceTest` with coverage for:

```java
summaryComputesAvailabilityLatencyAndFailureStreak()
summaryIgnoresNullLatencyForLatencyAggregates()
summaryReturnsEmptyWhenStationDoesNotExist()
```

The main case should use a latest-first list of `UP`, `DOWN`, `DOWN`, `UNKNOWN`, `UP` checks and assert `sampleSize=5`, `upCount=2`, `downCount=2`, `unknownCount=1`, `uptimeRate=0.4`, `averageLatencyMs=150`, `fastestLatencyMs=100`, `slowestLatencyMs=200`, and `longestFailureStreak=2`.

- [ ] **Step 3: Implement service method**

Add `summary(Long stationId, int limit)` to `ApiStationStatusHistoryService`. It should:

```java
if (!stationRepo.existsById(stationId)) return Optional.empty();
List<ApiStationStatusCheck> checks = checkRepo.findByStationIdOrderByCheckedAtDesc(stationId, PageRequest.of(0, normalizeLimit(limit)));
// Compute counts, uptimeRate, latency aggregate from non-null latency values, first/last timestamps, longest consecutive DOWN streak.
return Optional.of(new ApiStationStatusSummaryResponse(...));
```

- [ ] **Step 4: Expose controller route**

Add:

```java
@GetMapping("/{id}/checks/summary")
public ResponseEntity<ApiStationStatusSummaryResponse> checkSummary(@PathVariable Long id,
        @RequestParam(defaultValue = "30") int limit) {
    return historyService.summary(id, limit)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

- [ ] **Step 5: Verify backend**

Run:

```powershell
mvn -q -Dtest=ApiStationStatusHistoryServiceTest test
```

Expected: the focused service tests pass.

### Task 2: Frontend Analytics Panel

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ApiStationDetail.vue`

- [ ] **Step 1: Add TypeScript type and API client**

Add `ApiStationStatusSummary` to `types.ts` and add `publicApi.apiStationCheckSummary(id, { limit })` in `index.ts`.

- [ ] **Step 2: Load summary with detail data**

In `ApiStationDetail.vue`, add `summary = ref<ApiStationStatusSummary | null>(null)` and load it with `publicApi.apiStationCheckSummary(id, { limit: 30 })` via `Promise.allSettled`.

- [ ] **Step 3: Render analytics panel**

Above the recent checks list, render:

```vue
<section class="availability">
  <div class="metric-card">可用率</div>
  <div class="metric-card">平均延迟</div>
  <div class="metric-card">故障样本</div>
  <div class="sparkline">...</div>
</section>
```

Use existing terminal theme variables and keep the panel responsive without nested cards.

- [ ] **Step 4: Verify frontend**

Run:

```powershell
npm run build
```

Expected: TypeScript and Vite production build complete successfully.

### Task 3: Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document the new route**

Add `GET /api/api-stations/{id}/checks/summary?limit=` to the main API table and mention that API station detail pages show recent availability analytics.

- [ ] **Step 2: Final verification**

Run backend focused tests and frontend build again after documentation changes.
