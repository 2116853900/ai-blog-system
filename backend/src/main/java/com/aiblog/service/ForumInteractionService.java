package com.aiblog.service;

import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.dto.ForumSubscriptionSummaryResponse;
import com.aiblog.dto.ForumThreadSubscriptionItemResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumThreadSubscription;
import com.aiblog.repository.ForumPostFavoriteRepository;
import com.aiblog.repository.ForumPostLikeRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumThreadSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ForumInteractionService {

    private static final List<ForumThread.ThreadStatus> INTERACTABLE_STATUSES = List.of(
            ForumThread.ThreadStatus.NORMAL,
            ForumThread.ThreadStatus.PINNED,
            ForumThread.ThreadStatus.FEATURED,
            ForumThread.ThreadStatus.LOCKED
    );

    private final ForumThreadRepository threadRepo;
    private final ForumPostLikeRepository likeRepo;
    private final ForumPostFavoriteRepository favoriteRepo;
    private final ForumThreadSubscriptionRepository subscriptionRepo;
    private final ForumReplyRepository replyRepo;

    public ForumInteractionService(ForumThreadRepository threadRepo,
                                   ForumPostLikeRepository likeRepo,
                                   ForumPostFavoriteRepository favoriteRepo,
                                   ForumThreadSubscriptionRepository subscriptionRepo,
                                   ForumReplyRepository replyRepo) {
        this.threadRepo = threadRepo;
        this.likeRepo = likeRepo;
        this.favoriteRepo = favoriteRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.replyRepo = replyRepo;
    }

    @Transactional(readOnly = true)
    public ForumInteractionResponse getInteraction(Long threadId, Long userId) {
        ForumThread thread = findInteractableThread(threadId);
        boolean liked = userId != null && likeRepo.existsByPostIdAndUserId(threadId, userId);
        boolean favorited = userId != null && favoriteRepo.existsByPostIdAndUserId(threadId, userId);
        boolean subscribed = userId != null && subscriptionRepo.existsByThreadIdAndUserId(threadId, userId);
        int subscriberCount = Math.toIntExact(subscriptionRepo.countByThreadId(threadId));
        return new ForumInteractionResponse(liked, favorited, subscribed, thread.getLikeCount(), thread.getFavoriteCount(), subscriberCount);
    }

    @Transactional(readOnly = true)
    public Page<ForumThread> listFavoriteThreads(Long userId, Pageable pageable) {
        return favoriteRepo.findFavoriteThreadsByUserId(userId, INTERACTABLE_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ForumThread> listSubscribedThreads(Long userId, Pageable pageable) {
        return subscriptionRepo.findSubscribedThreadsByUserId(userId, INTERACTABLE_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ForumThreadSubscriptionItemResponse> listSubscriptionItems(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<ForumThreadSubscription> subscriptions = subscriptionRepo.findSubscriptionsByUserId(
                userId,
                INTERACTABLE_STATUSES,
                unreadOnly,
                pageable
        );
        List<Long> threadIds = subscriptions.getContent().stream()
                .map(ForumThreadSubscription::getThreadId)
                .toList();
        Map<Long, ForumThread> threads = threadRepo.findAllById(threadIds).stream()
                .collect(Collectors.toMap(ForumThread::getId, Function.identity()));
        return subscriptions.map(subscription -> toSubscriptionItem(userId, subscription, threads.get(subscription.getThreadId())));
    }

    @Transactional(readOnly = true)
    public ForumSubscriptionSummaryResponse subscriptionSummary(Long userId) {
        return new ForumSubscriptionSummaryResponse(
                subscriptionRepo.countByUserId(userId),
                subscriptionRepo.countReceivedSubscriptionsByAuthorId(userId),
                subscriptionRepo.countUnreadSubscribedThreads(userId, INTERACTABLE_STATUSES)
        );
    }

    @Transactional
    public ForumInteractionResponse like(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        if (likeRepo.insertIgnore(threadId, userId) > 0) {
            threadRepo.incrementLikeCount(threadId);
        }
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse unlike(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        if (likeRepo.deleteByPostIdAndUserId(threadId, userId) > 0) {
            threadRepo.decrementLikeCount(threadId);
        }
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse favorite(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        if (favoriteRepo.insertIgnore(threadId, userId) > 0) {
            threadRepo.incrementFavoriteCount(threadId);
        }
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse unfavorite(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        if (favoriteRepo.deleteByPostIdAndUserId(threadId, userId) > 0) {
            threadRepo.decrementFavoriteCount(threadId);
        }
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse subscribe(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        subscriptionRepo.insertIgnore(threadId, userId);
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse unsubscribe(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        subscriptionRepo.deleteByThreadIdAndUserId(threadId, userId);
        return getInteraction(threadId, userId);
    }

    @Transactional
    public ForumInteractionResponse markSubscriptionRead(Long threadId, Long userId) {
        ensureInteractableThread(threadId);
        subscriptionRepo.markRead(threadId, userId, Instant.now());
        return getInteraction(threadId, userId);
    }

    private ForumThreadSubscriptionItemResponse toSubscriptionItem(Long userId,
                                                                   ForumThreadSubscription subscription,
                                                                   ForumThread thread) {
        if (thread == null) {
            throw new IllegalStateException("订阅关联的帖子不存在: " + subscription.getThreadId());
        }
        Instant readAfter = subscription.getLastReadAt() == null ? subscription.getCreatedAt() : subscription.getLastReadAt();
        long unreadReplyCount = replyRepo.countUnreadRepliesAfter(
                thread.getId(),
                ForumReply.ReplyStatus.NORMAL,
                readAfter,
                userId
        );
        return new ForumThreadSubscriptionItemResponse(
                thread,
                Math.toIntExact(subscriptionRepo.countByThreadId(thread.getId())),
                unreadReplyCount,
                subscription.getCreatedAt(),
                subscription.getLastReadAt()
        );
    }

    private void ensureInteractableThread(Long threadId) {
        if (!threadRepo.existsByIdAndStatusIn(threadId, INTERACTABLE_STATUSES)) {
            throw new IllegalArgumentException("帖子不存在或不可互动");
        }
    }

    private ForumThread findInteractableThread(Long threadId) {
        return threadRepo.findById(threadId)
                .filter(thread -> INTERACTABLE_STATUSES.contains(thread.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在或不可互动"));
    }
}
