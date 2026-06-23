package com.aiblog.controller;

import com.aiblog.cache.CacheProperties;
import com.aiblog.cache.HybridCacheService;
import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ApiStationHealthDashboardResponse;
import com.aiblog.dto.ApiStationHealthTrendResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.aiblog.service.ResourceReviewBatchAggregator;
import com.aiblog.service.ResourceTagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiStationControllerTest {

    @Test
    void healthDashboardForwardsLimitsAndReturnsServiceResponse() {
        ApiStationStatusHistoryService historyService = mock(ApiStationStatusHistoryService.class);
        ApiStationHealthDashboardResponse response = new ApiStationHealthDashboardResponse(
                Instant.parse("2026-06-08T09:00:00Z"),
                1,
                1,
                0,
                0,
                1.0,
                88,
                List.of(new ApiStationHealthDashboardResponse.StationHealth(
                        3L,
                        "Fast Relay",
                        "https://relay.example.com",
                        ApiStation.Status.UP,
                        88,
                        Instant.parse("2026-06-08T08:59:00Z"),
                        2,
                        2,
                        0,
                        0,
                        1.0,
                        88,
                        0,
                        "healthy"
                )),
                List.of()
        );
        when(historyService.healthDashboard(10, 5)).thenReturn(response);
        ApiStationController controller = new ApiStationController(
                mock(ApiStationRepository.class),
                historyService,
                cacheService(),
                mock(ResourceTagService.class),
                mock(ResourceReviewBatchAggregator.class)
        );

        ApiStationHealthDashboardResponse result = controller.healthDashboard(10, 5);

        assertThat(result).isSameAs(response);
        assertThat(result.stationCount()).isEqualTo(1);
        assertThat(result.stations().getFirst().name()).isEqualTo("Fast Relay");
        verify(historyService).healthDashboard(10, 5);
    }

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
                mock(ResourceTagService.class),
                mock(ResourceReviewBatchAggregator.class)
        );

        ApiStationHealthTrendResponse result = controller.healthTrends(7, 4);

        assertThat(result).isSameAs(response);
        assertThat(result.buckets()).hasSize(1);
        verify(historyService).healthTrends(7, 4);
    }

    private PublicContentCacheService cacheService() {
        CacheProperties properties = new CacheProperties();
        properties.setKeyPrefix("api-station-controller-test");
        properties.setRedisEnabled(false);
        return new PublicContentCacheService(new HybridCacheService(properties, new ObjectMapper()), properties);
    }
}
