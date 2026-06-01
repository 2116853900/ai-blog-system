package com.aiblog.dto;

import com.aiblog.entity.ContentReport;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ContentReportRequest {
    @NotNull
    private ContentReport.TargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    private ContentReport.ReasonType reasonType;

    @Size(max = 1000)
    private String reasonText;

    public ContentReport.TargetType getTargetType() { return targetType; }
    public void setTargetType(ContentReport.TargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public ContentReport.ReasonType getReasonType() { return reasonType; }
    public void setReasonType(ContentReport.ReasonType reasonType) { this.reasonType = reasonType; }

    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
}
