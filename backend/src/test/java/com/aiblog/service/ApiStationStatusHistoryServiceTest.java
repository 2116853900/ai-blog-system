package com.aiblog.service;

import com.aiblog.dto.ApiStationStatusCheckResponse;
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

    private ApiStation station(ApiStation.Status status, Integer latencyMs) {
        ApiStation station = new ApiStation();
        station.setId(STATION_ID);
        station.setName("OpenAI 官方");
        station.setBaseUrl("https://api.openai.com");
        station.setStatus(status);
        station.setLatencyMs(latencyMs);
        return station;
    }

    private ApiStationStatusCheck check(Long id, ApiStation.Status status, Integer latencyMs, String errorMessage) {
        ApiStationStatusCheck check = new ApiStationStatusCheck();
        check.setId(id);
        check.setStationId(STATION_ID);
        check.setStatus(status);
        check.setLatencyMs(latencyMs);
        check.setCheckedAt(Instant.parse("2026-06-02T10:15:30Z").plusSeconds(id));
        check.setErrorMessage(errorMessage);
        return check;
    }
}
