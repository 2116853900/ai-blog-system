package com.aiblog.dto;

public class ResourceFavoriteInteractionResponse {
    private boolean favorited;
    private int favoriteCount;

    public ResourceFavoriteInteractionResponse(boolean favorited, int favoriteCount) {
        this.favorited = favorited;
        this.favoriteCount = favoriteCount;
    }

    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
}
