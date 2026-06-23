# API Station Health Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a public API station health trends feature that shows recent availability buckets and failure incidents, while refactoring duplicated health-statistics code into a reusable analyzer.

**Architecture:** Keep persistence unchanged and derive trends from `ApiStationStatusCheck` history. Introduce a small package-private analyzer under `com.aiblog.service` so `summary(...)`, `healthDashboard(...)`, and the new `healthTrends(...)` path all use one statistics implementation. Expose `/api/api-stations/health-trends`, then extend the existing `/api-stations/health` Vue page with an availability trend strip and incident stream.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21 records, Mockito/JUnit 5/AssertJ, Vue 3 Composition API, TypeScript, Vite.

---

### Task 1: Extract Shared Health Analyzer

**Files:**
- Create: `backend/src/main/java/com/aiblog/service/ApiStationHealthAnalyzer.java`
- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Test: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`

- [ ] **Step 1: Create analyzer class**

Create `backend/src/main/java/com/aiblog/service/ApiStationHealthAnalyzer.java`:

```java
package com.aiblog.service;

import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ApiStationStatusCheck;

import java.time.Instant;
import java.util.IntSummaryStatistics;
import java.util.List;

final class ApiStationHealthAnalyzer {

    private ApiStationHealthAnalyzer() {}

    static HealthStats summarize(List<ApiStationStatusCheck> checks) {
        int sampleSize = checks.size();
        int upCount = 0;
        int downCount = 0;
        int unknownCount = 0;
        int currentFailureStreak = 0;
        int longestFailureStreak = 0;

        for (ApiStationStatusCheck check : checks) {
            if (check.getStatus() == ApiStation.Status.UP) {
                upCount++;
                currentFailureStreak = 0;
            } else if (check.getStatus() == ApiStation.Status.DOWN) {
                downCount++;
                currentFailureStreak++;
                longestFailureStreak = Math.max(longestFailureStreak, currentFailureStreak);
            } else {
                unknownCount++;
                currentFailureStreak = 0;
            }
        }

        IntSummaryStatistics latencyStats = checks.stream()
                .map(ApiStationStatusCheck::getLatencyMs)
                .filter(latency -> latency != null)
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        Integer averageLatencyMs = latencyStats.getCount() == 0 ? null : (int) Math.round(latencyStats.getAverage());
        Integer fastestLatencyMs = latencyStats.getCount() == 0 ? null : latencyStats.getMin();
        Integer slowestLatencyMs = latencyStats.getCount() == 0 ? null : latencyStats.getMax();
        Instant lastCheckedAt = checks.isEmpty() ? null : checks.getFirst().getCheckedAt();
        Instant firstCheckedAt = checks.isEmpty() ? null : checks.getLast().getCheckedAt();
        ApiStation.Status currentStatus = checks.isEmpty() ? ApiStation.Status.UNKNOWN : checks.getFirst().getStatus();
        double uptimeRate = sampleSize == 0 ? 0 : (double) upCount / sampleSize;

        return new HealthStats(
                sampleSize,
                upCount,
                downCount,
                unknownCount,
                uptimeRate,
                averageLatencyMs,
                fastestLatencyMs,
                slowestLatencyMs,
                firstCheckedAt,
                lastCheckedAt,
                longestFailureStreak,
                currentStatus
        );
    }

    static String classifyHealth(ApiStation.Status status, double uptimeRate, Integer averageLatencyMs) {
        if (status == ApiStation.Status.DOWN) {
            return "down";
        }
        if (status == ApiStation.Status.UNKNOWN) {
            return "unknown";
        }
        if (uptimeRate < 0.8 || (averageLatencyMs != null && averageLatencyMs >= 250)) {
            return "degraded";
        }
        return "healthy";
    }

    static int healthRank(String healthLevel) {
        return switch (healthLevel) {
            case "down" -> 0;
            case "degraded" -> 1;
            case "unknown" -> 2;
            default -> 3;
        };
    }

    record HealthStats(
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
}
```

- [ ] **Step 2: Replace duplicated service code**

In `ApiStationStatusHistoryService`, remove imports for `java.util.IntSummaryStatistics`, remove private methods `summarize(...)`, `classifyHealth(...)`, `healthRank(...)`, and remove private record `HealthStats`. Use:

```java
ApiStationHealthAnalyzer.HealthStats stats = ApiStationHealthAnalyzer.summarize(checks);
String healthLevel = ApiStationHealthAnalyzer.classifyHealth(station.getStatus(), stats.uptimeRate(), stats.averageLatencyMs());
Comparator.comparingInt((StationHealth item) -> ApiStationHealthAnalyzer.healthRank(item.healthLevel()))
```

In `summary(...)`, use the analyzer result to build `ApiStationStatusSummaryResponse`:

```java
ApiStationHealthAnalyzer.HealthStats stats = ApiStationHealthAnalyzer.summarize(checks);
return Optional.of(new ApiStationStatusSummaryResponse(
        stationId,
        stats.sampleSize(),
        stats.upCount(),
        stats.downCount(),
        stats.unknownCount(),
        stats.uptimeRate(),
        stats.averageLatencyMs(),
        stats.fastestLatencyMs(),
        stats.slowestLatencyMs(),
        stats.firstCheckedAt(),
        stats.lastCheckedAt(),
        stats.longestFailureStreak(),
        stats.currentStatus()
));
```

- [ ] **Step 3: Run existing service tests**

Run:

```powershell
mvn -q -Dtest=ApiStationStatusHistoryServiceTest test
```

Expected: PASS. This proves the refactor preserved existing summary and health dashboard behavior.

### Task 2: Backend Health Trends DTO, Repository Query, and Service

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/ApiStationHealthTrendResponse.java`
- Modify: `backend/src/main/java/com/aiblog/repository/ApiStationStatusCheckRepository.java`
- Modify: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Test: `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`

- [ ] **Step 1: Add trend DTO**

Create `backend/src/main/java/com/aiblog/dto/ApiStationHealthTrendResponse.java`:

```java
package com.aiblog.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApiStationHealthTrendResponse(
        Instant generatedAt,
        int days,
        Instant startAt,
        Instant endAt,
        List<TrendBucket> buckets,
        List<Incident> incidents
) {
    public record TrendBucket(
            LocalDate date,
            int sampleSize,
            int upCount,
            int downCount,
            int unknownCount,
            double uptimeRate,
            Integer averageLatencyMs
    ) {}

    public record Incident(
            Long stationId,
            String stationName,
            Instant startedAt,
            Instant endedAt,
            long durationMinutes,
            int failureCount,
            String latestErrorMessage,
            boolean resolved
    ) {}
}
```

- [ ] **Step 2: Add repository range query**

Add this method to `ApiStationStatusCheckRepository`:

```java
List<ApiStationStatusCheck> findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(Instant checkedAt);
```

Also add `import java.time.Instant;`.

- [ ] **Step 3: Write service test for trends**

Append this test to `ApiStationStatusHistoryServiceTest`:

```java
@Test
void healthTrendsBuildsDailyBucketsAndIncidents() {
    ApiStation stable = station(11L, "稳定站", ApiStation.Status.UP, 90);
    ApiStation flaky = station(12L, "波动站", ApiStation.Status.UP, 180);
    when(stationRepo.findAll()).thenReturn(List.of(stable, flaky));
    List<ApiStationStatusCheck> checks = List.of(
            trendCheck(11L, 1L, ApiStation.Status.UP, 90, "2026-06-07T01:00:00Z", null),
            trendCheck(12L, 2L, ApiStation.Status.DOWN, null, "2026-06-07T02:00:00Z", "timeout"),
            trendCheck(12L, 3L, ApiStation.Status.DOWN, null, "2026-06-07T02:10:00Z", "HTTP 500"),
            trendCheck(12L, 4L, ApiStation.Status.UP, 200, "2026-06-07T02:30:00Z", null),
            trendCheck(11L, 5L, ApiStation.Status.UNKNOWN, null, "2026-06-08T01:00:00Z", null),
            trendCheck(12L, 6L, ApiStation.Status.DOWN, null, "2026-06-08T03:00:00Z", "timeout")
    );
    when(checkRepo.findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(any(Instant.class))).thenReturn(checks);

    var trends = service.healthTrends(2, 5);

    assertThat(trends.days()).isEqualTo(2);
    assertThat(trends.buckets()).hasSize(2);
    assertThat(trends.buckets().get(0).date()).isEqualTo(java.time.LocalDate.parse("2026-06-07"));
    assertThat(trends.buckets().get(0).sampleSize()).isEqualTo(4);
    assertThat(trends.buckets().get(0).upCount()).isEqualTo(2);
    assertThat(trends.buckets().get(0).downCount()).isEqualTo(2);
    assertThat(trends.buckets().get(0).uptimeRate()).isEqualTo(0.5);
    assertThat(trends.buckets().get(1).date()).isEqualTo(java.time.LocalDate.parse("2026-06-08"));
    assertThat(trends.incidents()).hasSize(2);
    assertThat(trends.incidents().get(0).stationName()).isEqualTo("波动站");
    assertThat(trends.incidents().get(0).failureCount()).isEqualTo(1);
    assertThat(trends.incidents().get(0).resolved()).isFalse();
    assertThat(trends.incidents().get(1).failureCount()).isEqualTo(2);
    assertThat(trends.incidents().get(1).resolved()).isTrue();
}
```

Add this helper:

```java
private ApiStationStatusCheck trendCheck(Long stationId, Long id, ApiStation.Status status, Integer latencyMs, String checkedAt, String errorMessage) {
    ApiStationStatusCheck check = checkForStation(stationId, id, status, latencyMs, errorMessage);
    check.setCheckedAt(Instant.parse(checkedAt));
    return check;
}
```

- [ ] **Step 4: Implement service method**

Add to `ApiStationStatusHistoryService`:

```java
@Transactional(readOnly = true)
public ApiStationHealthTrendResponse healthTrends(int days, int incidentLimit) {
    int normalizedDays = normalizeDays(days);
    int normalizedIncidentLimit = normalizeLimit(incidentLimit);
    Instant endAt = Instant.now();
    Instant startAt = endAt.minus(normalizedDays - 1L, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
    List<ApiStationStatusCheck> checks = checkRepo.findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(startAt);
    Map<Long, ApiStation> stationsById = stationRepo.findAll().stream()
            .filter(station -> station.getId() != null)
            .collect(Collectors.toMap(ApiStation::getId, Function.identity()));

    List<TrendBucket> buckets = buildTrendBuckets(startAt, normalizedDays, checks);
    List<Incident> incidents = buildIncidents(checks, stationsById, normalizedIncidentLimit);
    return new ApiStationHealthTrendResponse(endAt, normalizedDays, startAt, endAt, buckets, incidents);
}
```

Implement `normalizeDays(...)` to clamp to `1..30`, `buildTrendBuckets(...)` to group checks by `LocalDate` in system default zone and fill days with zero samples, and `buildIncidents(...)` to group consecutive non-UP checks per station until the next UP check resolves the incident. Sort incidents newest first by `startedAt`, limit with `normalizedIncidentLimit`, and ignore checks whose station no longer exists.

- [ ] **Step 5: Run service tests**

Run:

```powershell
mvn -q -Dtest=ApiStationStatusHistoryServiceTest test
```

Expected: PASS.

### Task 3: Backend Endpoint and Controller Test

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
- Modify: `backend/src/test/java/com/aiblog/controller/ApiStationControllerTest.java`

- [ ] **Step 1: Add controller method**

Add before `@GetMapping("/{id}")`:

```java
@GetMapping("/health-trends")
public ApiStationHealthTrendResponse healthTrends(
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "10") int incidentLimit) {
    return historyService.healthTrends(days, incidentLimit);
}
```

Add import:

```java
import com.aiblog.dto.ApiStationHealthTrendResponse;
```

- [ ] **Step 2: Add controller test**

Append to `ApiStationControllerTest`:

```java
@Test
void healthTrendsForwardsDaysAndIncidentLimit() {
    ApiStationStatusHistoryService historyService = mock(ApiStationStatusHistoryService.class);
    ApiStationHealthTrendResponse response = new ApiStationHealthTrendResponse(
            Instant.parse("2026-06-08T09:00:00Z"),
            7,
            Instant.parse("2026-06-02T00:00:00Z"),
            Instant.parse("2026-06-08T09:00:00Z"),
            List.of(new ApiStationHealthTrendResponse.TrendBucket(
                    java.time.LocalDate.parse("2026-06-08"),
                    1,
                    1,
                    0,
                    0,
                    1.0,
                    88
            )),
            List.of()
    );
    when(historyService.healthTrends(7, 4)).thenReturn(response);
    ApiStationController controller = new ApiStationController(
            mock(ApiStationRepository.class),
            historyService,
            cacheService(),
            mock(ResourceTagService.class)
    );

    ApiStationHealthTrendResponse result = controller.healthTrends(7, 4);

    assertThat(result).isSameAs(response);
    assertThat(result.buckets()).hasSize(1);
    verify(historyService).healthTrends(7, 4);
}
```

- [ ] **Step 3: Run controller tests**

Run:

```powershell
mvn -q -Dtest=ApiStationControllerTest test
```

Expected: PASS.

### Task 4: Frontend Types, API Client, and Health Page UI

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ApiStationHealth.vue`

- [ ] **Step 1: Add TypeScript types**

Add to `frontend/src/api/types.ts`:

```ts
export interface ApiStationHealthTrendBucket {
  date: string
  sampleSize: number
  upCount: number
  downCount: number
  unknownCount: number
  uptimeRate: number
  averageLatencyMs?: number
}

export interface ApiStationIncident {
  stationId: number
  stationName: string
  startedAt: string
  endedAt?: string
  durationMinutes: number
  failureCount: number
  latestErrorMessage?: string
  resolved: boolean
}

export interface ApiStationHealthTrendResponse {
  generatedAt: string
  days: number
  startAt: string
  endAt: string
  buckets: ApiStationHealthTrendBucket[]
  incidents: ApiStationIncident[]
}
```

- [ ] **Step 2: Add API client**

Import `ApiStationHealthTrendResponse` in `frontend/src/api/index.ts`, then add:

```ts
apiStationHealthTrends: (params?: { days?: number; incidentLimit?: number }) =>
  http.get<ApiStationHealthTrendResponse>('/api-stations/health-trends', { params }).then(r => r.data),
```

- [ ] **Step 3: Load trends beside dashboard**

In `ApiStationHealth.vue`, add:

```ts
import type { ApiStationHealthDashboard, ApiStationHealthLevel, ApiStationHealthTrendResponse } from '../api/types'

const trends = ref<ApiStationHealthTrendResponse | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [dashboardResponse, trendResponse] = await Promise.all([
      publicApi.apiStationHealthDashboard({ sampleLimit: 30, failureLimit: 8 }),
      publicApi.apiStationHealthTrends({ days: 7, incidentLimit: 8 })
    ])
    dashboard.value = dashboardResponse
    trends.value = trendResponse
  } catch {
    error.value = '状态大盘加载失败'
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 4: Add trend strip and incident stream UI**

Add a section below the metric grid:

```vue
<section v-if="trends" class="trend-panel card">
  <div class="panel-head">
    <div>
      <h2 class="mono">7 日趋势</h2>
      <p class="muted">按检测日期聚合可用率与样本量。</p>
    </div>
  </div>
  <div class="trend-bars" aria-label="最近 7 日可用率趋势">
    <div v-for="bucket in trends.buckets" :key="bucket.date" class="trend-day">
      <div class="bar-shell">
        <span class="bar-fill" :style="{ height: `${Math.max(bucket.uptimeRate * 100, 4)}%` }"></span>
      </div>
      <strong class="mono">{{ formatPercent(bucket.uptimeRate) }}</strong>
      <small>{{ bucket.date.slice(5) }} · {{ bucket.sampleSize }} 次</small>
    </div>
  </div>
</section>
```

Replace the right-side failure panel content source with `trends.incidents` when available, while preserving the existing `dashboard.recentFailures` fallback:

```ts
const incidentItems = computed(() => trends.value?.incidents ?? [])
```

Display incident resolved state, duration, and latest error. If no incidents exist, keep the existing quiet state.

- [ ] **Step 5: Add CSS**

Add scoped CSS for `.trend-panel`, `.trend-bars`, `.trend-day`, `.bar-shell`, `.bar-fill`, and incident state badges. Use the existing terminal theme variables only; do not introduce a new palette.

- [ ] **Step 6: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS.

### Task 5: Documentation and Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document endpoint**

Add this API table row after `health-dashboard`:

```markdown
| GET  | `/api/api-stations/health-trends?days=&incidentLimit=` | 公益 API 站点趋势：按天聚合可用率、延迟与故障事件流 |
```

- [ ] **Step 2: Run all verification**

Run:

```powershell
mvn -q test
npm run build
```

Expected: PASS.

- [ ] **Step 3: Self-review**

Check:
- `summary(...)` and `healthDashboard(...)` no longer maintain separate health-stat counting loops.
- Trend endpoint is declared before `/{id}` so route matching is unambiguous.
- Trend DTO field names match TypeScript interfaces.
- UI handles loading, error, empty data, zero-sample buckets, open incidents, resolved incidents, and mobile layout.
- No unrelated generated build files are committed.
