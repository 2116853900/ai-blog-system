package com.aiblog.dto;

import jakarta.validation.constraints.NotBlank;

public class ReplyRequest {

    @NotBlank
    private String contentMarkdown;

    /** 引用的回复 ID（可选） */
    private Long replyToId;

    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
}
