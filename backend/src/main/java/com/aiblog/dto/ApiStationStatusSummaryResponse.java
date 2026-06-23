package com.aiblog.dto;

import com.aiblog.entity.ApiStation;

import java.time.Instant;

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
) {
}
