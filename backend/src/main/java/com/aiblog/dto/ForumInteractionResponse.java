package com.aiblog.dto;

public class ForumInteractionResponse {
    private boolean liked;
    private boolean favorited;
    private int likeCount;
    private int favoriteCount;

    public ForumInteractionResponse(boolean liked, boolean favorited, int likeCount, int favoriteCount) {
        this.liked = liked;
        this.favorited = favorited;
        this.likeCount = likeCount;
        this.favoriteCount = favoriteCount;
    }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
}
