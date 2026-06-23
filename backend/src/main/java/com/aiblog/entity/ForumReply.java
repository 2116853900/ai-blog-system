package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forum_reply",
        uniqueConstraints = @UniqueConstraint(name = "uk_forum_reply_thread_floor", columnNames = {"threadId", "floorNumber"}),
        indexes = {
    @Index(name = "idx_thread_floor", columnList = "threadId,floorNumber"),
    @Index(name = "idx_reply_author", columnList = "authorId"),
    @Index(name = "idx_reply_author_status_created", columnList = "authorId,status,createdAt")
})
public class ForumReply {

    public enum ReplyStatus { NORMAL, HIDDEN, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long threadId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private int floorNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contentMarkdown;

    /** 引用的回复 ID */
    private Long replyToId;

    /** 引用的回复作者 ID */
    private Long replyToUserId;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplyStatus status = ReplyStatus.NORMAL;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }

    public Long getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(Long replyToUserId) { this.replyToUserId = replyToUserId; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public ReplyStatus getStatus() { return status; }
    public void setStatus(ReplyStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
