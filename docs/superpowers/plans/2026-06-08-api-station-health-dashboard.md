# API Station Health Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a public API station health dashboard that summarizes current station availability, latency, degraded stations, and recent failures.

**Architecture:** Reuse existing `ApiStation` and `ApiStationStatusCheck` data. Add a read-only service method that aggregates station status and recent check history into DTO records, expose it through `/api/api-stations/health-dashboard`, and add a Vue page at `/api-stations/health` with navigation from the API station list and nav menu.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Mockito/JUnit 5/AssertJ, Vue 3 Composition API, TypeScript, Vite.

---

### Task 1: Backend Health Dashboard DTO and Service

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ApiStationHealthDashboardResponse.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ApiStationStatusCheckRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Test: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`

- [ ] **Step 1: Add DTO record**

Create `ApiStationHealthDashboardResponse.java`:

```java
package com.aiblog.dto;

import com.aiblog.entity.ApiStation;

import java.time.Instant;
import java.util.List;

public record ApiStationHealthDashboardResponse(
        Instant generatedAt,
        int stationCount,
        int upCount,
        int downCount,
        int unknownCount,
        double uptimeRate,
        Integer averageLatencyMs,
        List<StationHealth> stations,
        List<RecentFailure> recentFailures
) {
    public record StationHealth(
            Long id,
            String name,
            String baseUrl,
            ApiStation.Status status,
            Integer latencyMs,
            Instant lastCheckedAt,
            int sampleSize,
            int upCount,
            int downCount,
            int unknownCount,
            double uptimeRate,
            Integer averageLatencyMs,
            int longestFailureStreak,
            String healthLevel
    ) {}

    public record RecentFailure(
            Long stationId,
            String stationName,
            ApiStation.Status status,
            Instant checkedAt,
            String errorMessage
    ) {}
}
```

- [ ] **Step 2: Add repository query**

Add this method to `ApiStationStatusCheckRepository`:

```java
List<ApiStationStatusCheck> findByStatusNotOrderByCheckedAtDesc(ApiStation.Status status, Pageable pageable);
```

- [ ] **Step 3: Add failing service test**

Append a test to `ApiStationStatusHistoryServiceTest` that stubs three stations and their histories, then asserts dashboard counts, station ordering, health levels, and recent failures:

```java
@Test
void healthDashboardAggregatesStationHealthAndRecentFailures() {
    ApiStation stable = station(11L, "稳定站", ApiStation.Status.UP, 90);
    stable.setLastCheckedAt(Instant.parse("2026-06-08T09:00:00Z"));
    ApiStation degraded = station(12L, "波动站", ApiStation.Status.UP, 260);
    degraded.setLastCheckedAt(Instant.parse("2026-06-08T09:01:00Z"));
    ApiStation down = station(13L, "故障站", ApiStation.Status.DOWN, null);
    down.setLastCheckedAt(Instant.parse("2026-06-08T09:02:00Z"));
    when(stationRepo.findAll()).thenReturn(List.of(stable, degraded, down));
    when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(11L), any(Pageable.class))).thenReturn(List.of(
            checkForStation(11L, 5L, ApiStation.Status.UP, 90, null),
            checkForStation(11L, 4L, ApiStation.Status.UP, 100, null),
            checkForStation(11L, 3L, ApiStation.Status.UP, 80, null)
    ));
    when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(12L), any(Pageable.class))).thenReturn(List.of(
            checkForStation(12L, 5L, ApiStation.Status.UP, 260, null),
            checkForStation(12L, 4L, ApiStation.Status.DOWN, null, "timeout"),
            checkForStation(12L, 3L, ApiStation.Status.UP, 220, null)
    ));
    when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(13L), any(Pageable.class))).thenReturn(List.of(
            checkForStation(13L, 5L, ApiStation.Status.DOWN, null, "HTTP 500"),
            checkForStation(13L, 4L, ApiStation.Status.DOWN, null, "timeout")
    ));
    when(checkRepo.findByStatusNotOrderByCheckedAtDesc(eq(ApiStation.Status.UP), any(Pageable.class))).thenReturn(List.of(
            checkForStation(13L, 5L, ApiStation.Status.DOWN, null, "HTTP 500"),
            checkForStation(12L, 4L, ApiStation.Status.DOWN, null, "timeout")
    ));

    var dashboard = service.healthDashboard(20, 10);

    assertThat(dashboard.stationCount()).isEqualTo(3);
    assertThat(dashboard.upCount()).isEqualTo(2);
    assertThat(dashboard.downCount()).isEqualTo(1);
    assertThat(dashboard.unknownCount()).isZero();
    assertThat(dashboard.uptimeRate()).isEqualTo(2.0 / 3.0);
    assertThat(dashboard.averageLatencyMs()).isEqualTo(175);
    assertThat(dashboard.stations()).extracting("name").containsExactly("故障站", "波动站", "稳定站");
    assertThat(dashboard.stations()).extracting("healthLevel").containsExactly("down", "degraded", "healthy");
    assertThat(dashboard.recentFailures()).hasSize(2);
    assertThat(dashboard.recentFailures().getFirst().stationName()).isEqualTo("故障站");
}
```

- [ ] **Step 4: Implement service aggregation**

Add `healthDashboard(int sampleLimit, int failureLimit)` to `ApiStationStatusHistoryService`. It should:

```java
List<ApiStation> stations = stationRepo.findAll();
int normalizedSampleLimit = normalizeLimit(sampleLimit);
int normalizedFailureLimit = normalizeLimit(failureLimit);
Map<Long, ApiStation> stationsById = stations.stream().collect(Collectors.toMap(ApiStation::getId, Function.identity()));
List<StationHealth> stationHealth = stations.stream()
        .map(station -> toStationHealth(station, checkRepo.findByStationIdOrderByCheckedAtDesc(station.getId(), PageRequest.of(0, normalizedSampleLimit))))
        .sorted(Comparator.comparingInt((StationHealth item) -> healthRank(item.healthLevel()))
                .thenComparing(StationHealth::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
List<RecentFailure> recentFailures = checkRepo.findByStatusNotOrderByCheckedAtDesc(ApiStation.Status.UP, PageRequest.of(0, normalizedFailureLimit))
        .stream()
        .filter(check -> stationsById.containsKey(check.getStationId()))
        .map(check -> toRecentFailure(check, stationsById.get(check.getStationId())))
        .toList();
```

Use the same count and latency rules already tested by `summary(...)`. Classify health as `down` for current `DOWN`, `unknown` for current `UNKNOWN`, `degraded` when uptime is below `0.8` or average latency is at least `250`, otherwise `healthy`.

- [ ] **Step 5: Run service tests**

Run:

```powershell
mvn -q -Dtest=ApiStationStatusHistoryServiceTest test
```

Expected: PASS.

### Task 2: Backend Public Endpoint

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Test: `backend/src/test/java/com/aiblog/controller/ApiStationControllerTest.java`

- [ ] **Step 1: Add controller test**

Create `ApiStationControllerTest.java` with `@WebMvcTest(ApiStationController.class)`, mock `ApiStationStatusHistoryService`, `ApiStationRepository`, `PublicContentCacheService`, and `ResourceTagService`, then assert `GET /api/api-stations/health-dashboard?sampleLimit=10&failureLimit=5` returns `stationCount` and station data from the service.

- [ ] **Step 2: Add endpoint**

Add this method before `@GetMapping("/{id}")` in `ApiStationController`:

```java
@GetMapping("/health-dashboard")
public ApiStationHealthDashboardResponse healthDashboard(
        @RequestParam(defaultValue = "30") int sampleLimit,
        @RequestParam(defaultValue = "10") int failureLimit) {
    return historyService.healthDashboard(sampleLimit, failureLimit);
}
```

- [ ] **Step 3: Run controller test**

Run:

```powershell
mvn -q -Dtest=ApiStationControllerTest test
```

Expected: PASS.

### Task 3: Frontend API, Route, and Health Page

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/ApiStationHealth.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/views/ApiStations.vue`

- [ ] **Step 1: Add TypeScript types**

Add `ApiStationHealthDashboard`, `ApiStationHealth`, and `ApiStationRecentFailure` to `types.ts` matching the backend response fields.

- [ ] **Step 2: Add public API client**

Add:

```ts
apiStationHealthDashboard: (params?: { sampleLimit?: number; failureLimit?: number }) =>
  http.get<ApiStationHealthDashboard>('/api-stations/health-dashboard', { params }).then(r => r.data),
```

- [ ] **Step 3: Add Vue page**

Create `ApiStationHealth.vue` that loads `publicApi.apiStationHealthDashboard({ sampleLimit: 30, failureLimit: 8 })`, displays aggregate metrics, health-level segmented filters, a station health table/cards, recent failures, loading skeleton, and error retry state.

- [ ] **Step 4: Add route and navigation**

Add route `/api-stations/health` before `/api-stations/:id`, add “状态大盘” under resource nav, and add a button from `ApiStations.vue` page header to the new page.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

### Task 4: Documentation and Final Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document endpoint and page**

Add `/api/api-stations/health-dashboard` to the API table and mention `/api-stations/health` in the public frontend route list.

- [ ] **Step 2: Run backend and frontend verification**

Run:

```powershell
mvn -q test
npm run build
```

Expected: PASS.

- [ ] **Step 3: Self-review**

Check:
- Endpoint path does not conflict with `/{id}`.
- DTO field names match TypeScript interfaces.
- Page handles empty data, loading, retry, and mobile layout.
- No unrelated files are reverted or reformatted.
