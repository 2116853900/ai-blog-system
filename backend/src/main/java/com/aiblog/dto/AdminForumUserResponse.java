package com.aiblog.dto;

import com.aiblog.entity.ForumUser;
import java.time.Instant;

public class AdminForumUserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private ForumUser.Role role;
    private ForumUser.Status status;
    private String banReason;
    private Instant banStartTime;
    private Instant banEndTime;
    private String banOperatorUsername;
    private int experiencePoints;
    private int level;
    private Instant createdAt;
    private Instant lastLoginAt;

    public static AdminForumUserResponse from(ForumUser user) {
        AdminForumUserResponse r = new AdminForumUserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setEmail(user.getEmail());
        r.setNickname(user.getNickname());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setBio(user.getBio());
        r.setRole(user.getRole());
        r.setStatus(user.getStatus());
        r.setBanReason(user.getBanReason());
        r.setBanStartTime(user.getBanStartTime());
        r.setBanEndTime(user.getBanEndTime());
        r.setBanOperatorUsername(user.getBanOperatorUsername());
        r.setExperiencePoints(user.getExperiencePoints());
        r.setLevel(user.getLevel());
        r.setCreatedAt(user.getCreatedAt());
        r.setLastLoginAt(user.getLastLoginAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public ForumUser.Role getRole() { return role; }
    public void setRole(ForumUser.Role role) { this.role = role; }

    public ForumUser.Status getStatus() { return status; }
    public void setStatus(ForumUser.Status status) { this.status = status; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public Instant getBanStartTime() { return banStartTime; }
    public void setBanStartTime(Instant banStartTime) { this.banStartTime = banStartTime; }

    public Instant getBanEndTime() { return banEndTime; }
    public void setBanEndTime(Instant banEndTime) { this.banEndTime = banEndTime; }

    public String getBanOperatorUsername() { return banOperatorUsername; }
    public void setBanOperatorUsername(String banOperatorUsername) { this.banOperatorUsername = banOperatorUsername; }

    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
