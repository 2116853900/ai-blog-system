package com.aiblog.service;

import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.dto.ForumSubscriptionSummaryResponse;
import com.aiblog.dto.ForumThreadSubscriptionItemResponse;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumThreadSubscription;
import com.aiblog.repository.ForumPostFavoriteRepository;
import com.aiblog.repository.ForumPostLikeRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumThreadSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumInteractionServiceTest {

    private static final long THREAD_ID = 10L;
    private static final long USER_ID = 20L;

    @Mock
    private ForumThreadRepository threadRepo;

    @Mock
    private ForumPostLikeRepository likeRepo;

    @Mock
    private ForumPostFavoriteRepository favoriteRepo;

    @Mock
    private ForumThreadSubscriptionRepository subscriptionRepo;

    @Mock
    private ForumReplyRepository replyRepo;

    private ForumInteractionService service;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        service = new ForumInteractionService(threadRepo, likeRepo, favoriteRepo, subscriptionRepo, replyRepo);
        thread = new ForumThread();
        thread.setId(THREAD_ID);
        thread.setCategoryId(1L);
        thread.setAuthorId(2L);
        thread.setTitle("治理测试帖");
        thread.setContentMarkdown("content");
        thread.setStatus(ForumThread.ThreadStatus.NORMAL);
        thread.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        thread.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        lenient().when(threadRepo.findById(THREAD_ID)).thenReturn(Optional.of(thread));
        lenient().when(threadRepo.existsByIdAndStatusIn(any(), any())).thenReturn(true);
    }

    @Test
    void likeIsIdempotentAndIncrementsCountOnce() {
        when(likeRepo.insertIgnore(THREAD_ID, USER_ID)).thenReturn(1, 0);
        when(likeRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(true);
        when(threadRepo.incrementLikeCount(THREAD_ID)).thenAnswer(invocation -> {
            thread.setLikeCount(thread.getLikeCount() + 1);
            return 1;
        });

        ForumInteractionResponse first = service.like(THREAD_ID, USER_ID);
        ForumInteractionResponse second = service.like(THREAD_ID, USER_ID);

        assertThat(first.isLiked()).isTrue();
        assertThat(second.isLiked()).isTrue();
        assertThat(thread.getLikeCount()).isEqualTo(1);
        verify(likeRepo, times(2)).insertIgnore(THREAD_ID, USER_ID);
        verify(threadRepo, times(1)).incrementLikeCount(THREAD_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void unlikeIsIdempotentAndDoesNotMakeCountNegative() {
        thread.setLikeCount(1);
        when(likeRepo.deleteByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(1, 0);
        when(likeRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(false);
        when(threadRepo.decrementLikeCount(THREAD_ID)).thenAnswer(invocation -> {
            thread.setLikeCount(Math.max(0, thread.getLikeCount() - 1));
            return 1;
        });

        service.unlike(THREAD_ID, USER_ID);
        service.unlike(THREAD_ID, USER_ID);

        assertThat(thread.getLikeCount()).isZero();
        verify(likeRepo, times(2)).deleteByPostIdAndUserId(THREAD_ID, USER_ID);
        verify(threadRepo, times(1)).decrementLikeCount(THREAD_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void favoriteIsIdempotentAndIncrementsCountOnce() {
        when(favoriteRepo.insertIgnore(THREAD_ID, USER_ID)).thenReturn(1, 0);
        when(favoriteRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(true);
        when(threadRepo.incrementFavoriteCount(THREAD_ID)).thenAnswer(invocation -> {
            thread.setFavoriteCount(thread.getFavoriteCount() + 1);
            return 1;
        });

        ForumInteractionResponse first = service.favorite(THREAD_ID, USER_ID);
        ForumInteractionResponse second = service.favorite(THREAD_ID, USER_ID);

        assertThat(first.isFavorited()).isTrue();
        assertThat(second.isFavorited()).isTrue();
        assertThat(thread.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepo, times(2)).insertIgnore(THREAD_ID, USER_ID);
        verify(threadRepo, times(1)).incrementFavoriteCount(THREAD_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void unfavoriteWithoutExistingFavoriteDoesNotChangeCount() {
        thread.setFavoriteCount(0);
        when(favoriteRepo.deleteByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(0);

        ForumInteractionResponse response = service.unfavorite(THREAD_ID, USER_ID);

        assertThat(response.isFavorited()).isFalse();
        assertThat(thread.getFavoriteCount()).isZero();
        verify(favoriteRepo, times(1)).deleteByPostIdAndUserId(THREAD_ID, USER_ID);
        verify(threadRepo, never()).decrementFavoriteCount(THREAD_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void listFavoriteThreadsUsesInteractableStatuses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ForumThread> page = new PageImpl<>(List.of(thread), pageable, 1);
        List<ForumThread.ThreadStatus> visibleStatuses = List.of(
                ForumThread.ThreadStatus.NORMAL,
                ForumThread.ThreadStatus.PINNED,
                ForumThread.ThreadStatus.FEATURED,
                ForumThread.ThreadStatus.LOCKED
        );
        when(favoriteRepo.findFavoriteThreadsByUserId(USER_ID, visibleStatuses, pageable)).thenReturn(page);

        Page<ForumThread> result = service.listFavoriteThreads(USER_ID, pageable);

        assertThat(result.getContent()).containsExactly(thread);
        verify(favoriteRepo).findFavoriteThreadsByUserId(USER_ID, visibleStatuses, pageable);
    }

    @Test
    void subscribeIsIdempotentAndReturnsSubscribedState() {
        when(subscriptionRepo.insertIgnore(THREAD_ID, USER_ID)).thenReturn(1, 0);
        when(subscriptionRepo.existsByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(true);
        when(subscriptionRepo.countByThreadId(THREAD_ID)).thenReturn(3L);

        ForumInteractionResponse first = service.subscribe(THREAD_ID, USER_ID);
        ForumInteractionResponse second = service.subscribe(THREAD_ID, USER_ID);

        assertThat(first.isSubscribed()).isTrue();
        assertThat(first.getSubscriberCount()).isEqualTo(3);
        assertThat(second.isSubscribed()).isTrue();
        verify(subscriptionRepo, times(2)).insertIgnore(THREAD_ID, USER_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void unsubscribeIsIdempotentAndReturnsUnsubscribedState() {
        when(subscriptionRepo.deleteByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(1, 0);
        when(subscriptionRepo.existsByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(false);

        ForumInteractionResponse response = service.unsubscribe(THREAD_ID, USER_ID);
        service.unsubscribe(THREAD_ID, USER_ID);

        assertThat(response.isSubscribed()).isFalse();
        verify(subscriptionRepo, times(2)).deleteByThreadIdAndUserId(THREAD_ID, USER_ID);
        verify(threadRepo, never()).save(thread);
    }

    @Test
    void listSubscribedThreadsUsesInteractableStatuses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ForumThread> page = new PageImpl<>(List.of(thread), pageable, 1);
        List<ForumThread.ThreadStatus> visibleStatuses = List.of(
                ForumThread.ThreadStatus.NORMAL,
                ForumThread.ThreadStatus.PINNED,
                ForumThread.ThreadStatus.FEATURED,
                ForumThread.ThreadStatus.LOCKED
        );
        when(subscriptionRepo.findSubscribedThreadsByUserId(USER_ID, visibleStatuses, pageable)).thenReturn(page);

        Page<ForumThread> result = service.listSubscribedThreads(USER_ID, pageable);

        assertThat(result.getContent()).containsExactly(thread);
        verify(subscriptionRepo).findSubscribedThreadsByUserId(USER_ID, visibleStatuses, pageable);
    }

    @Test
    void subscriptionSummaryReturnsFollowedFollowerAndUnreadCounts() {
        List<ForumThread.ThreadStatus> visibleStatuses = List.of(
                ForumThread.ThreadStatus.NORMAL,
                ForumThread.ThreadStatus.PINNED,
                ForumThread.ThreadStatus.FEATURED,
                ForumThread.ThreadStatus.LOCKED
        );
        when(subscriptionRepo.countByUserId(USER_ID)).thenReturn(12L);
        when(subscriptionRepo.countReceivedSubscriptionsByAuthorId(USER_ID)).thenReturn(7L);
        when(subscriptionRepo.countUnreadSubscribedThreads(USER_ID, visibleStatuses)).thenReturn(4L);

        ForumSubscriptionSummaryResponse result = service.subscriptionSummary(USER_ID);

        assertThat(result.getSubscribedThreadCount()).isEqualTo(12);
        assertThat(result.getReceivedSubscriberCount()).isEqualTo(7);
        assertThat(result.getUnreadSubscribedThreadCount()).isEqualTo(4);
    }

    @Test
    void listSubscriptionItemsIncludesUnreadReplyAndSubscriberCounts() {
        Pageable pageable = PageRequest.of(0, 10);
        ForumThreadSubscription subscription = new ForumThreadSubscription();
        subscription.setId(99L);
        subscription.setThreadId(THREAD_ID);
        subscription.setUserId(USER_ID);
        subscription.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        subscription.setLastReadAt(Instant.parse("2026-06-02T00:00:00Z"));
        Page<ForumThreadSubscription> page = new PageImpl<>(List.of(subscription), pageable, 1);
        List<ForumThread.ThreadStatus> visibleStatuses = List.of(
                ForumThread.ThreadStatus.NORMAL,
                ForumThread.ThreadStatus.PINNED,
                ForumThread.ThreadStatus.FEATURED,
                ForumThread.ThreadStatus.LOCKED
        );
        when(subscriptionRepo.findSubscriptionsByUserId(USER_ID, visibleStatuses, true, pageable)).thenReturn(page);
        when(threadRepo.findAllById(List.of(THREAD_ID))).thenReturn(List.of(thread));
        when(subscriptionRepo.countByThreadId(THREAD_ID)).thenReturn(5L);
        when(replyRepo.countUnreadRepliesAfter(THREAD_ID, com.aiblog.entity.ForumReply.ReplyStatus.NORMAL,
                Instant.parse("2026-06-02T00:00:00Z"), USER_ID)).thenReturn(2L);

        Page<ForumThreadSubscriptionItemResponse> result = service.listSubscriptionItems(USER_ID, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        ForumThreadSubscriptionItemResponse item = result.getContent().getFirst();
        assertThat(item.getId()).isEqualTo(THREAD_ID);
        assertThat(item.getSubscriberCount()).isEqualTo(5);
        assertThat(item.getUnreadReplyCount()).isEqualTo(2);
        assertThat(item.isUnread()).isTrue();
        assertThat(item.getUrl()).isEqualTo("/forum/threads/10");
    }

    @Test
    void markSubscriptionReadUpdatesReadMarkerAndReturnsInteraction() {
        when(subscriptionRepo.markRead(any(), any(), any())).thenReturn(1);
        when(subscriptionRepo.existsByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(true);

        ForumInteractionResponse response = service.markSubscriptionRead(THREAD_ID, USER_ID);

        assertThat(response.isSubscribed()).isTrue();
        verify(subscriptionRepo).markRead(any(), any(), any());
    }
}
