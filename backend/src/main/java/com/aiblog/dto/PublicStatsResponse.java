package com.aiblog.dto;

import com.aiblog.entity.ApiStation;

import java.time.Instant;
import java.util.List;

public record PublicStatsResponse(
        Instant generatedAt,
        ContentMetrics content,
        CommunityMetrics community,
        ApiHealthMetrics apiHealth,
        List<TagMetric> popularTags,
        List<RecentItem> recentItems,
        List<HotThread> hotThreads
) {
    public record ContentMetrics(
            long posts,
            long skills,
            long mcps,
            long apiStations,
            long totalResources
    ) {
    }

    public record CommunityMetrics(
            long threads,
            long replies,
            long solvedThreads,
            long totalViews,
            long totalLikes,
            long totalFavorites
    ) {
    }

    public record ApiHealthMetrics(
            long total,
            long up,
            long down,
            long unknown,
            double uptimeRate,
            Integer averageLatencyMs
    ) {
    }

    public record TagMetric(
            String tag,
            long count,
            String url
    ) {
    }

    public record RecentItem(
            String type,
            String title,
            String description,
            String url,
            String category,
            String tags,
            Instant createdAt,
            String metric
    ) {
    }

    public record HotThread(
            Long id,
            String title,
            String url,
            String tags,
            int viewCount,
            int replyCount,
            int likeCount,
            Instant lastActivityAt,
            boolean solved
    ) {
    }

    public static double uptimeRate(long total, long up) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((up * 1000.0 / total)) / 10.0;
    }

    public static String apiStatusMetric(ApiStation.Status status, Integer latencyMs) {
        if (status == ApiStation.Status.UP && latencyMs != null) {
            return "UP · " + latencyMs + "ms";
        }
        return status.name();
    }
}
