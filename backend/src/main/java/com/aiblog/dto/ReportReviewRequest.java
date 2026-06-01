package com.aiblog.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public class ReportReviewRequest {
    @Size(max = 1000)
    private String reviewNote;

    private boolean hideContent;
    private boolean banTargetAuthor;
    private String banReason;
    private Instant banEndTime;

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public boolean isHideContent() { return hideContent; }
    public void setHideContent(boolean hideContent) { this.hideContent = hideContent; }

    public boolean isBanTargetAuthor() { return banTargetAuthor; }
    public void setBanTargetAuthor(boolean banTargetAuthor) { this.banTargetAuthor = banTargetAuthor; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public Instant getBanEndTime() { return banEndTime; }
    public void setBanEndTime(Instant banEndTime) { this.banEndTime = banEndTime; }
}
