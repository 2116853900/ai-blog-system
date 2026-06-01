package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forum_thread", indexes = {
    @Index(name = "idx_category_status", columnList = "categoryId,status,lastReplyAt"),
    @Index(name = "idx_author", columnList = "authorId"),
    @Index(name = "idx_linked", columnList = "linkedRefType,linkedRefId")
})
public class ForumThread {

    public enum ThreadStatus { NORMAL, PINNED, FEATURED, LOCKED, HIDDEN, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String contentMarkdown;

    @Column(length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadStatus status = ThreadStatus.NORMAL;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int replyCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int favoriteCount = 0;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int reportCount = 0;

    private Long lastReplyUserId;

    private Instant lastReplyAt;

    /** 关联博客系统内容类型 */
    private String linkedRefType;

    /** 关联博客系统内容 ID */
    private Long linkedRefId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public ThreadStatus getStatus() { return status; }
    public void setStatus(ThreadStatus status) { this.status = status; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public Long getLastReplyUserId() { return lastReplyUserId; }
    public void setLastReplyUserId(Long lastReplyUserId) { this.lastReplyUserId = lastReplyUserId; }

    public Instant getLastReplyAt() { return lastReplyAt; }
    public void setLastReplyAt(Instant lastReplyAt) { this.lastReplyAt = lastReplyAt; }

    public String getLinkedRefType() { return linkedRefType; }
    public void setLinkedRefType(String linkedRefType) { this.linkedRefType = linkedRefType; }

    public Long getLinkedRefId() { return linkedRefId; }
    public void setLinkedRefId(Long linkedRefId) { this.linkedRefId = linkedRefId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
