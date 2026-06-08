package com.aiblog.service;

import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.dto.ApiStationStatusSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ApiStationStatusCheck;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ApiStationStatusCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiStationStatusHistoryServiceTest {

    private static final long STATION_ID = 7L;

    @Mock
    private ApiStationStatusCheckRepository checkRepo;

    @Mock
    private ApiStationRepository stationRepo;

    private ApiStationStatusHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ApiStationStatusHistoryService(checkRepo, stationRepo);
    }

    @Test
    void recordCreatesHistoryRowFromStationSnapshot() {
        ApiStation station = station(ApiStation.Status.UP, 123);
        station.setLastCheckedAt(Instant.parse("2026-06-02T10:15:30Z"));
        when(checkRepo.save(any(ApiStationStatusCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiStationStatusCheck saved = service.record(station, null);

        assertThat(saved.getStationId()).isEqualTo(STATION_ID);
        assertThat(saved.getStatus()).isEqualTo(ApiStation.Status.UP);
        assertThat(saved.getLatencyMs()).isEqualTo(123);
        assertThat(saved.getCheckedAt()).isEqualTo(station.getLastCheckedAt());
        assertThat(saved.getErrorMessage()).isNull();
    }

    @Test
    void recordTruncatesLongFailureMessage() {
        ApiStation station = station(ApiStation.Status.DOWN, null);
        when(checkRepo.save(any(ApiStationStatusCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiStationStatusCheck saved = service.record(station, "x".repeat(600));

        assertThat(saved.getErrorMessage()).hasSize(500);
    }

    @Test
    void recentReturnsMappedChecksAndClampsLimit() {
        ApiStationStatusCheck first = check(1L, ApiStation.Status.DOWN, null, "timeout");
        ApiStationStatusCheck second = check(2L, ApiStation.Status.UP, 88, null);
        when(stationRepo.existsById(STATION_ID)).thenReturn(true);
        when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(STATION_ID), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        Optional<List<ApiStationStatusCheckResponse>> result = service.recent(STATION_ID, 99);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).hasSize(2);
        assertThat(result.orElseThrow().get(0).status()).isEqualTo(ApiStation.Status.DOWN);
        assertThat(result.orElseThrow().get(0).errorMessage()).isEqualTo("timeout");
        verify(checkRepo).findByStationIdOrderByCheckedAtDesc(eq(STATION_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void recentReturnsEmptyWhenStationDoesNotExist() {
        when(stationRepo.existsById(STATION_ID)).thenReturn(false);

        Optional<List<ApiStationStatusCheckResponse>> result = service.recent(STATION_ID, 20);

        assertThat(result).isEmpty();
        verifyNoInteractions(checkRepo);
    }

    @Test
    void summaryComputesAvailabilityLatencyAndFailureStreak() {
        List<ApiStationStatusCheck> checks = List.of(
                check(5L, ApiStation.Status.UP, 100, null),
                check(4L, ApiStation.Status.DOWN, null, "timeout"),
                check(3L, ApiStation.Status.DOWN, 200, "HTTP 503"),
                check(2L, ApiStation.Status.UNKNOWN, null, null),
                check(1L, ApiStation.Status.UP, 150, null)
        );
        when(stationRepo.existsById(STATION_ID)).thenReturn(true);
        when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(STATION_ID), any(Pageable.class)))
                .thenReturn(checks);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        Optional<ApiStationStatusSummaryResponse> result = service.summary(STATION_ID, 30);

        assertThat(result).isPresent();
        ApiStationStatusSummaryResponse summary = result.orElseThrow();
        assertThat(summary.stationId()).isEqualTo(STATION_ID);
        assertThat(summary.sampleSize()).isEqualTo(5);
        assertThat(summary.upCount()).isEqualTo(2);
        assertThat(summary.downCount()).isEqualTo(2);
        assertThat(summary.unknownCount()).isEqualTo(1);
        assertThat(summary.uptimeRate()).isEqualTo(0.4);
        assertThat(summary.averageLatencyMs()).isEqualTo(150);
        assertThat(summary.fastestLatencyMs()).isEqualTo(100);
        assertThat(summary.slowestLatencyMs()).isEqualTo(200);
        assertThat(summary.firstCheckedAt()).isEqualTo(checks.getLast().getCheckedAt());
        assertThat(summary.lastCheckedAt()).isEqualTo(checks.getFirst().getCheckedAt());
        assertThat(summary.longestFailureStreak()).isEqualTo(2);
        assertThat(summary.currentStatus()).isEqualTo(ApiStation.Status.UP);
        verify(checkRepo).findByStationIdOrderByCheckedAtDesc(eq(STATION_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void summaryIgnoresNullLatencyForLatencyAggregates() {
        List<ApiStationStatusCheck> checks = List.of(
                check(2L, ApiStation.Status.DOWN, null, "timeout"),
                check(1L, ApiStation.Status.UP, null, null)
        );
        when(stationRepo.existsById(STATION_ID)).thenReturn(true);
        when(checkRepo.findByStationIdOrderByCheckedAtDesc(eq(STATION_ID), any(Pageable.class)))
                .thenReturn(checks);

        Optional<ApiStationStatusSummaryResponse> result = service.summary(STATION_ID, 0);

        assertThat(result).isPresent();
        ApiStationStatusSummaryResponse summary = result.orElseThrow();
        assertThat(summary.averageLatencyMs()).isNull();
        assertThat(summary.fastestLatencyMs()).isNull();
        assertThat(summary.slowestLatencyMs()).isNull();
        assertThat(summary.sampleSize()).isEqualTo(2);
    }

    @Test
    void summaryReturnsEmptyWhenStationDoesNotExist() {
        when(stationRepo.existsById(STATION_ID)).thenReturn(false);

        Optional<ApiStationStatusSummaryResponse> result = service.summary(STATION_ID, 20);

        assertThat(result).isEmpty();
        verifyNoInteractions(checkRepo);
    }

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
        assertThat(dashboard.stations().get(0).longestFailureStreak()).isEqualTo(2);
        assertThat(dashboard.recentFailures()).hasSize(2);
        assertThat(dashboard.recentFailures().getFirst().stationName()).isEqualTo("故障站");
        assertThat(dashboard.recentFailures().getFirst().errorMessage()).isEqualTo("HTTP 500");
    }

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

    private ApiStation station(ApiStation.Status status, Integer latencyMs) {
        return station(STATION_ID, "OpenAI 官方", status, latencyMs);
    }

    private ApiStation station(Long id, String name, ApiStation.Status status, Integer latencyMs) {
        ApiStation station = new ApiStation();
        station.setId(id);
        station.setName(name);
        station.setBaseUrl("https://api.example.com/" + id);
        station.setStatus(status);
        station.setLatencyMs(latencyMs);
        return station;
    }

    private ApiStationStatusCheck check(Long id, ApiStation.Status status, Integer latencyMs, String errorMessage) {
        return checkForStation(STATION_ID, id, status, latencyMs, errorMessage);
    }

    private ApiStationStatusCheck checkForStation(Long stationId, Long id, ApiStation.Status status, Integer latencyMs, String errorMessage) {
        ApiStationStatusCheck check = new ApiStationStatusCheck();
        check.setId(id);
        check.setStationId(stationId);
        check.setStatus(status);
        check.setLatencyMs(latencyMs);
        check.setCheckedAt(Instant.parse("2026-06-02T10:15:30Z").plusSeconds(id));
        check.setErrorMessage(errorMessage);
        return check;
    }

    private ApiStationStatusCheck trendCheck(Long stationId, Long id, ApiStation.Status status, Integer latencyMs, String checkedAt, String errorMessage) {
        ApiStationStatusCheck check = checkForStation(stationId, id, status, latencyMs, errorMessage);
        check.setCheckedAt(Instant.parse(checkedAt));
        return check;
    }
}
