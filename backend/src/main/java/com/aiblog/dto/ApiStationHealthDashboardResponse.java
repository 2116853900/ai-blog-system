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
