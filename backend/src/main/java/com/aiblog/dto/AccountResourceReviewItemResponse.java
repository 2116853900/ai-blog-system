package com.aiblog.dto;

import com.aiblog.entity.ResourceReview;

import java.time.Instant;

public class AccountResourceReviewItemResponse {
    private Long id;
    private ResourceReview.RefType refType;
    private Long refId;
    private String title;
    private String url;
    private int rating;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountResourceReviewItemResponse(Long id,
                                             ResourceReview.RefType refType,
                                             Long refId,
                                             String title,
                                             String url,
                                             int rating,
                                             String content,
                                             Instant createdAt,
                                             Instant updatedAt) {
        this.id = id;
        this.refType = refType;
        this.refId = refId;
        this.title = title;
        this.url = url;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public ResourceReview.RefType getRefType() { return refType; }
    public Long getRefId() { return refId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}