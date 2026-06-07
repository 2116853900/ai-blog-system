package com.aiblog.dto;

public class ForumSubscriptionSummaryResponse {
    private long subscribedThreadCount;
    private long receivedSubscriberCount;
    private long unreadSubscribedThreadCount;

    public ForumSubscriptionSummaryResponse(long subscribedThreadCount,
                                            long receivedSubscriberCount,
                                            long unreadSubscribedThreadCount) {
        this.subscribedThreadCount = subscribedThreadCount;
        this.receivedSubscriberCount = receivedSubscriberCount;
        this.unreadSubscribedThreadCount = unreadSubscribedThreadCount;
    }

    public long getSubscribedThreadCount() { return subscribedThreadCount; }
    public void setSubscribedThreadCount(long subscribedThreadCount) { this.subscribedThreadCount = subscribedThreadCount; }

    public long getReceivedSubscriberCount() { return receivedSubscriberCount; }
    public void setReceivedSubscriberCount(long receivedSubscriberCount) { this.receivedSubscriberCount = receivedSubscriberCount; }

    public long getUnreadSubscribedThreadCount() { return unreadSubscribedThreadCount; }
    public void setUnreadSubscribedThreadCount(long unreadSubscribedThreadCount) { this.unreadSubscribedThreadCount = unreadSubscribedThreadCount; }
}
