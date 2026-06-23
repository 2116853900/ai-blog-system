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
