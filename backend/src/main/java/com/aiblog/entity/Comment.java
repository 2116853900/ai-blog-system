package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_ref_visible", columnList = "refType,refId,approved,status,createdAt"),
        @Index(name = "idx_comment_moderation", columnList = "approved,status,createdAt"),
        @Index(name = "idx_comment_status_created", columnList = "status,createdAt")
})
public class Comment {
    /** 评论所属内容类型 */
    public enum RefType { POST, SKILL, MCP, API }
    public enum CommentStatus { NORMAL, HIDDEN, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefType refType = RefType.POST;

    /** 关联内容 id */
    @Column(nullable = false)
    private Long refId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean approved = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'NORMAL'")
    private CommentStatus status = CommentStatus.NORMAL;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RefType getRefType() { return refType; }
    public void setRefType(RefType refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
