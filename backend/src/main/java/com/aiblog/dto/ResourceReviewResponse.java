package com.aiblog.dto;

import com.aiblog.entity.ResourceReview;

import java.time.Instant;

public class ResourceReviewResponse {
    private Long id;
    private Long userId;
    private ResourceReview.RefType refType;
    private Long refId;
    private Integer rating;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public static ResourceReviewResponse from(ResourceReview review) {
        ResourceReviewResponse response = new ResourceReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setRefType(review.getRefType());
        response.setRefId(review.getRefId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ResourceReview.RefType getRefType() { return refType; }
    public void setRefType(ResourceReview.RefType refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
