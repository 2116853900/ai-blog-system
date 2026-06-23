package com.aiblog.dto;

public record RelatedResourceResponse(
        String type,
        Long id,
        String title,
        String description,
        String url,
        String category,
        String tags,
        int score,
        String reason
) {
}
