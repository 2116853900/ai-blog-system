package com.aiblog.dto;

import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @Size(max = 50)
    private String nickname;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 500)
    private String bio;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
