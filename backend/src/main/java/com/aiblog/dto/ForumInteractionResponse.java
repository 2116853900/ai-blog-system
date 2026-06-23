package com.aiblog.dto;

public class ForumInteractionResponse {
    private boolean liked;
    private boolean favorited;
    private boolean subscribed;
    private int likeCount;
    private int favoriteCount;
    private int subscriberCount;

    public ForumInteractionResponse(boolean liked, boolean favorited, boolean subscribed, int likeCount, int favoriteCount, int subscriberCount) {
        this.liked = liked;
        this.favorited = favorited;
        this.subscribed = subscribed;
        this.likeCount = likeCount;
        this.favoriteCount = favoriteCount;
        this.subscriberCount = subscriberCount;
    }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }

    public boolean isSubscribed() { return subscribed; }
    public void setSubscribed(boolean subscribed) { this.subscribed = subscribed; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public int getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(int subscriberCount) { this.subscriberCount = subscriberCount; }
}
