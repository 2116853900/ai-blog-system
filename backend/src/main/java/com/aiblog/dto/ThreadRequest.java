package com.aiblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ThreadRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String contentMarkdown;

    private String tags;

    /** 关联博客内容类型（可选） */
    private String linkedRefType;

    /** 关联博客内容 ID（可选） */
    private Long linkedRefId;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getLinkedRefType() { return linkedRefType; }
    public void setLinkedRefType(String linkedRefType) { this.linkedRefType = linkedRefType; }
    public Long getLinkedRefId() { return linkedRefId; }
    public void setLinkedRefId(Long linkedRefId) { this.linkedRefId = linkedRefId; }
}
