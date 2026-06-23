# API Station Status History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record every API station status probe and show recent probe history on the public API station detail page.

**Architecture:** Add a small API Monitoring bounded context around the existing `StatusCheckService`. The current `ApiStation` keeps the latest status for list and dashboard speed; a new `ApiStationStatusCheck` entity stores immutable probe events, and a focused service owns recording and query rules so controllers stay thin.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21 records, Vue 3 Composition API, TypeScript, Axios.

---

## Project Analysis

Current API station support has a useful latest-state model:

- `ApiStation.status`, `latencyMs`, and `lastCheckedAt` power badges and admin dashboard counts.
- `StatusCheckService.checkAndSave(ApiStation)` runs scheduled and manual checks, then overwrites the latest state.
- `ApiStationDetail.vue` displays only the latest result.

Missing capability:

- Users cannot inspect recent reliability, repeated failures, or latency changes.
- Admin manual checks and scheduled checks leave no audit trail.
- Frontend detail pages cannot distinguish a stable site from a site with intermittent failures.

Chosen feature:

- Persist recent status check history and expose it publicly for API station detail pages.
- Keep scope narrow: no chart library, no alerting, no background cleanup job.

## File Structure

- Create `backend/src/main/java/com/aiblog/entity/ApiStationStatusCheck.java`
  - Immutable-style JPA record of one probe result.
  - Stores `stationId` instead of a JPA relation to avoid serialization and cascade coupling.
- Create `backend/src/main/java/com/aiblog/repository/ApiStationStatusCheckRepository.java`
  - Query recent checks by station id with a `Pageable` limit.
- Create `backend/src/main/java/com/aiblog/dto/ApiStationStatusCheckResponse.java`
  - Public response shape for the history endpoint.
- Create `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
  - Records a check and lists recent checks.
  - Verifies station existence before public reads.
- Modify `backend/src/main/java/com/aiblog/service/StatusCheckService.java`
  - Injects `ApiStationStatusHistoryService`.
  - Records one history row after saving latest station status.
- Modify `backend/src/main/java/com/aiblog/controller/ApiStationController.java`
  - Adds `GET /api/api-stations/{id}/checks?limit=20`.
- Modify `backend/src/test/java/com/aiblog/controller/PublicResourceDetailControllerTest.java`
  - Updates constructor usage and verifies the history endpoint.
- Create `backend/src/test/java/com/aiblog/service/ApiStationStatusHistoryServiceTest.java`
  - Unit tests for recording and bounded recent-history queries.
- Modify `frontend/src/api/types.ts`
  - Adds `ApiStationStatusCheck`.
- Modify `frontend/src/api/index.ts`
  - Adds `publicApi.apiStationChecks(id, { limit })`.
- Modify `frontend/src/views/ApiStationDetail.vue`
  - Loads recent checks with station detail and renders a compact recent-check list.
- Modify `README.md`
  - Documents the new public endpoint and history behavior.

## Task 1: Backend Status History Model

**Files:**
- Create: `backend/src/main/java/com/aiblog/entity/ApiStationStatusCheck.java`
- Create: `backend/src/main/java/com/aiblog/repository/ApiStationStatusCheckRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/ApiStationStatusCheckResponse.java`

- [ ] **Step 1: Add the status check entity**

Create `ApiStationStatusCheck` with fields:

```java
@Entity
@Table(name = "api_station_status_check", indexes = {
        @Index(name = "idx_api_station_status_check_station_checked", columnList = "stationId,checkedAt")
})
public class ApiStationStatusCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiStation.Status status = ApiStation.Status.UNKNOWN;

    private Integer latencyMs;

    @Column(nullable = false)
    private Instant checkedAt = Instant.now();

    @Column(length = 500)
    private String errorMessage;
}
```

- [ ] **Step 2: Add the repository**

Create:

```java
public interface ApiStationStatusCheckRepository extends JpaRepository<ApiStationStatusCheck, Long> {
    List<ApiStationStatusCheck> findByStationIdOrderByCheckedAtDesc(Long stationId, Pageable pageable);
}
```

- [ ] **Step 3: Add the response DTO**

Create:

```java
public record ApiStationStatusCheckResponse(
        Long id,
        Long stationId,
        ApiStation.Status status,
        Integer latencyMs,
        Instant checkedAt,
        String errorMessage
) {
    public static ApiStationStatusCheckResponse from(ApiStationStatusCheck check) {
        return new ApiStationStatusCheckResponse(
                check.getId(),
                check.getStationId(),
                check.getStatus(),
                check.getLatencyMs(),
                check.getCheckedAt(),
                check.getErrorMessage()
        );
    }
}
```

## Task 2: Backend Service And Public Endpoint

**Files:**
- Create: `backend/src/main/java/com/aiblog/service/ApiStationStatusHistoryService.java`
- Modify: `backend/src/main/java/com/aiblog/service/StatusCheckService.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ApiStationController.java`

- [ ] **Step 1: Add history service tests first**

Expected tests:

```java
@Test
void recordCreatesHistoryRowFromStationSnapshot() {
    ApiStation station = new ApiStation();
    station.setId(7L);
    station.setStatus(ApiStation.Status.UP);
    station.setLatencyMs(123);
    station.setLastCheckedAt(Instant.parse("2026-06-02T10:15:30Z"));

    when(checkRepo.save(any(ApiStationStatusCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ApiStationStatusCheck saved = service.record(station, null);

    assertThat(saved.getStationId()).isEqualTo(7L);
    assertThat(saved.getStatus()).isEqualTo(ApiStation.Status.UP);
    assertThat(saved.getLatencyMs()).isEqualTo(123);
    assertThat(saved.getCheckedAt()).isEqualTo(station.getLastCheckedAt());
}
```

- [ ] **Step 2: Implement the service**

Rules:

- `record(ApiStation station, String errorMessage)` copies `id`, `status`, `latencyMs`, and `lastCheckedAt`.
- If `lastCheckedAt` is null, use `Instant.now()`.
- Clamp public query limit to `1..50`, default controller limit is `20`.
- `recent(Long stationId, int limit)` returns `Optional.empty()` when the station does not exist.

- [ ] **Step 3: Wire check recording**

In `StatusCheckService.checkAndSave`, save the station first, then call:

```java
ApiStation saved = repo.save(s);
historyService.record(saved, errorMessage);
return saved;
```

Capture the final failed exception message as `errorMessage`; successful checks pass `null`.

- [ ] **Step 4: Add public endpoint**

In `ApiStationController`:

```java
@GetMapping("/{id}/checks")
public ResponseEntity<List<ApiStationStatusCheckResponse>> checks(
        @PathVariable Long id,
        @RequestParam(defaultValue = "20") int limit) {
    return historyService.recent(id, limit)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

## Task 3: Frontend Detail Integration

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/ApiStationDetail.vue`

- [ ] **Step 1: Add TypeScript type**

```ts
export interface ApiStationStatusCheck {
  id: number
  stationId: number
  status: ApiStatus
  latencyMs?: number
  checkedAt: string
  errorMessage?: string
}
```

- [ ] **Step 2: Add API method**

```ts
apiStationChecks: (id: number, params?: { limit?: number }) =>
  http.get<ApiStationStatusCheck[]>(`/api-stations/${id}/checks`, { params }).then(r => r.data),
```

- [ ] **Step 3: Render recent checks**

In `ApiStationDetail.vue`:

- Load station and recent checks together.
- Keep station detail usable if history request fails.
- Show a `最近检测` section with status, latency, checked time, and optional failure message.
- Do not add a chart dependency; use a compact list to keep the change small.

## Task 4: Docs And Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document endpoint**

Add `GET /api/api-stations/{id}/checks?limit=` to the main interface table.

- [ ] **Step 2: Document behavior**

Update the API station status paragraph:

```markdown
公益 API 站点状态由 `StatusCheckService` 每 10 分钟自动检测一次（`app.status-check.cron` 可配），也可在后台手动触发。每次检测都会写入历史记录，前台详情页会展示最近检测结果。
```

- [ ] **Step 3: Run backend tests**

Run:

```bash
cd backend
mvn -q test
```

Expected: build success.

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build success.

## Self-Review

- Spec coverage: analysis, architecture design, missing functionality, plan, implementation, docs, and validation are covered.
- Placeholder scan: no `TBD`, deferred implementation, or unspecified test steps remain.
- Type consistency: backend `ApiStation.Status` maps to frontend `ApiStatus`; endpoint response uses `ApiStationStatusCheckResponse`; frontend uses `ApiStationStatusCheck`.
