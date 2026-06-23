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
