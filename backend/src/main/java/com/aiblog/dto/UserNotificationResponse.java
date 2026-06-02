package com.aiblog.dto;

import com.aiblog.entity.UserNotification;

import java.time.Instant;

public class UserNotificationResponse {
    private Long id;
    private UserNotification.NotificationType type;
    private String title;
    private String message;
    private String linkUrl;
    private Long actorId;
    private boolean read;
    private Instant createdAt;
    private Instant readAt;

    public UserNotificationResponse(Long id,
                                    UserNotification.NotificationType type,
                                    String title,
                                    String message,
                                    String linkUrl,
                                    Long actorId,
                                    boolean read,
                                    Instant createdAt,
                                    Instant readAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.linkUrl = linkUrl;
        this.actorId = actorId;
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserNotification.NotificationType getType() { return type; }
    public void setType(UserNotification.NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
