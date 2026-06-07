package com.aiblog.dto;

import com.aiblog.entity.ForumThread;

import java.time.Instant;

public class ForumThreadSubscriptionItemResponse {
    private Long id;
    private Long categoryId;
    private Long authorId;
    private String title;
    private String contentMarkdown;
    private String tags;
    private ForumThread.ThreadStatus status;
    private int viewCount;
    private int replyCount;
    private int likeCount;
    private int favoriteCount;
    private int reportCount;
    private Long lastReplyUserId;
    private Instant lastReplyAt;
    private Long acceptedReplyId;
    private Long acceptedReplyUserId;
    private Instant acceptedAt;
    private String linkedRefType;
    private Long linkedRefId;
    private Instant createdAt;
    private Instant updatedAt;
    private int subscriberCount;
    private long unreadReplyCount;
    private boolean unread;
    private Instant subscribedAt;
    private Instant lastReadAt;
    private String url;

    public ForumThreadSubscriptionItemResponse(ForumThread thread,
                                               int subscriberCount,
                                               long unreadReplyCount,
                                               Instant subscribedAt,
                                               Instant lastReadAt) {
        this.id = thread.getId();
        this.categoryId = thread.getCategoryId();
        this.authorId = thread.getAuthorId();
        this.title = thread.getTitle();
        this.contentMarkdown = thread.getContentMarkdown();
        this.tags = thread.getTags();
        this.status = thread.getStatus();
        this.viewCount = thread.getViewCount();
        this.replyCount = thread.getReplyCount();
        this.likeCount = thread.getLikeCount();
        this.favoriteCount = thread.getFavoriteCount();
        this.reportCount = thread.getReportCount();
        this.lastReplyUserId = thread.getLastReplyUserId();
        this.lastReplyAt = thread.getLastReplyAt();
        this.acceptedReplyId = thread.getAcceptedReplyId();
        this.acceptedReplyUserId = thread.getAcceptedReplyUserId();
        this.acceptedAt = thread.getAcceptedAt();
        this.linkedRefType = thread.getLinkedRefType();
        this.linkedRefId = thread.getLinkedRefId();
        this.createdAt = thread.getCreatedAt();
        this.updatedAt = thread.getUpdatedAt();
        this.subscriberCount = subscriberCount;
        this.unreadReplyCount = unreadReplyCount;
        this.unread = unreadReplyCount > 0;
        this.subscribedAt = subscribedAt;
        this.lastReadAt = lastReadAt;
        this.url = "/forum/threads/" + thread.getId();
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public Long getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getContentMarkdown() { return contentMarkdown; }
    public String getTags() { return tags; }
    public ForumThread.ThreadStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getReplyCount() { return replyCount; }
    public int getLikeCount() { return likeCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public int getReportCount() { return reportCount; }
    public Long getLastReplyUserId() { return lastReplyUserId; }
    public Instant getLastReplyAt() { return lastReplyAt; }
    public Long getAcceptedReplyId() { return acceptedReplyId; }
    public Long getAcceptedReplyUserId() { return acceptedReplyUserId; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public String getLinkedRefType() { return linkedRefType; }
    public Long getLinkedRefId() { return linkedRefId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getSubscriberCount() { return subscriberCount; }
    public long getUnreadReplyCount() { return unreadReplyCount; }
    public boolean isUnread() { return unread; }
    public Instant getSubscribedAt() { return subscribedAt; }
    public Instant getLastReadAt() { return lastReadAt; }
    public String getUrl() { return url; }
}
