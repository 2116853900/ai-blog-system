package com.aiblog.dto;

import com.aiblog.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommentRequest {
    @NotNull
    private Comment.RefType refType;
    @NotNull
    private Long refId;
    @NotBlank
    private String author;
    @NotBlank
    private String content;

    public Comment.RefType getRefType() { return refType; }
    public void setRefType(Comment.RefType refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
