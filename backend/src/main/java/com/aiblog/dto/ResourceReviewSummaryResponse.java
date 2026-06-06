package com.aiblog.dto;

public class ResourceReviewSummaryResponse {
    private double averageRating;
    private long reviewCount;
    private ResourceReviewResponse myReview;

    public ResourceReviewSummaryResponse(double averageRating, long reviewCount, ResourceReviewResponse myReview) {
        this.averageRating = Math.round(averageRating * 10.0) / 10.0;
        this.reviewCount = reviewCount;
        this.myReview = myReview;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    public ResourceReviewResponse getMyReview() { return myReview; }
    public void setMyReview(ResourceReviewResponse myReview) { this.myReview = myReview; }
}
