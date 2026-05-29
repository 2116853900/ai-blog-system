package com.aiblog.dto;

import com.aiblog.entity.Submission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmissionRequest {
    @NotNull
    private Submission.Type type;
    /** 投稿内容 JSON 字符串（name/description/link 等） */
    @NotBlank
    private String payloadJson;
    private String contactInfo;

    public Submission.Type getType() { return type; }
    public void setType(Submission.Type type) { this.type = type; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
