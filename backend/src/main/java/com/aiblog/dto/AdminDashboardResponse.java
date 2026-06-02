package com.aiblog.dto;

public record AdminDashboardResponse(
        Moderation moderation,
        Content content,
        Community community,
        ApiStations apiStations
) {
    public record Moderation(long pendingComments, long pendingSubmissions, long pendingReports) {
    }

    public record Content(long posts, long skills, long mcps, long apiStations) {
    }

    public record Community(long users, long activeUsers, long bannedUsers, long threads, long replies) {
    }

    public record ApiStations(long up, long down, long unknown) {
    }
}
