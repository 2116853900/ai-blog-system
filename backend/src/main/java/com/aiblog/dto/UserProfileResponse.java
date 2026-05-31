package com.aiblog.dto;

import com.aiblog.entity.ForumUser;

import java.time.Instant;

/** 用户公开资料响应 */
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String role;
    private int level;
    private int experiencePoints;
    private Instant createdAt;

    public static UserProfileResponse from(ForumUser u) {
        UserProfileResponse r = new UserProfileResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.nickname = u.getNickname() != null ? u.getNickname() : u.getUsername();
        r.avatarUrl = u.getAvatarUrl();
        r.bio = u.getBio();
        r.role = u.getRole().name();
        r.level = u.getLevel();
        r.experiencePoints = u.getExperiencePoints();
        r.createdAt = u.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
