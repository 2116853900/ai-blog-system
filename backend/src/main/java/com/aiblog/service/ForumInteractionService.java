package com.aiblog.service;

import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.ForumPostFavoriteRepository;
import com.aiblog.repository.ForumPostLikeRepository;
import com.aiblog.repository.ForumThreadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public ForumInteractionService(ForumThreadRepository threadRepo,
                                   ForumPostLikeRepository likeRepo,
                                   ForumPostFavoriteRepository favoriteRepo) {
        this.threadRepo = threadRepo;
        this.likeRepo = likeRepo;
        this.favoriteRepo = favoriteRepo;
    }

    @Transactional(readOnly = true)
    public ForumInteractionResponse getInteraction(Long threadId, Long userId) {
        ForumThread thread = findInteractableThread(threadId);
        boolean liked = userId != null && likeRepo.existsByPostIdAndUserId(threadId, userId);
        boolean favorited = userId != null && favoriteRepo.existsByPostIdAndUserId(threadId, userId);
        return new ForumInteractionResponse(liked, favorited, thread.getLikeCount(), thread.getFavoriteCount());
    }

    @Transactional(readOnly = true)
    public Page<ForumThread> listFavoriteThreads(Long userId, Pageable pageable) {
        return favoriteRepo.findFavoriteThreadsByUserId(userId, INTERACTABLE_STATUSES, pageable);
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
