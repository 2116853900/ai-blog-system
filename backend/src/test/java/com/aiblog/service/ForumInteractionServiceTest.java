package com.aiblog.service;

import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.entity.ForumPostFavorite;
import com.aiblog.entity.ForumPostLike;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.ForumPostFavoriteRepository;
import com.aiblog.repository.ForumPostLikeRepository;
import com.aiblog.repository.ForumThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private ForumInteractionService service;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        service = new ForumInteractionService(threadRepo, likeRepo, favoriteRepo);
        thread = new ForumThread();
        thread.setId(THREAD_ID);
        thread.setCategoryId(1L);
        thread.setAuthorId(2L);
        thread.setTitle("治理测试帖");
        thread.setContentMarkdown("content");
        thread.setStatus(ForumThread.ThreadStatus.NORMAL);
        when(threadRepo.findById(THREAD_ID)).thenReturn(Optional.of(thread));
    }

    @Test
    void likeIsIdempotentAndIncrementsCountOnce() {
        when(likeRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID))
                .thenReturn(false, true, true, true);

        ForumInteractionResponse first = service.like(THREAD_ID, USER_ID);
        ForumInteractionResponse second = service.like(THREAD_ID, USER_ID);

        assertThat(first.isLiked()).isTrue();
        assertThat(second.isLiked()).isTrue();
        assertThat(thread.getLikeCount()).isEqualTo(1);
        verify(likeRepo, times(1)).save(any(ForumPostLike.class));
        verify(threadRepo, times(1)).save(thread);
    }

    @Test
    void unlikeIsIdempotentAndDoesNotMakeCountNegative() {
        ForumPostLike like = new ForumPostLike();
        like.setPostId(THREAD_ID);
        like.setUserId(USER_ID);
        thread.setLikeCount(0);
        when(likeRepo.findByPostIdAndUserId(THREAD_ID, USER_ID))
                .thenReturn(Optional.of(like), Optional.empty());
        when(likeRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(false);

        service.unlike(THREAD_ID, USER_ID);
        service.unlike(THREAD_ID, USER_ID);

        assertThat(thread.getLikeCount()).isZero();
        verify(likeRepo, times(1)).delete(like);
        verify(threadRepo, times(1)).save(thread);
    }

    @Test
    void favoriteIsIdempotentAndIncrementsCountOnce() {
        when(favoriteRepo.existsByPostIdAndUserId(THREAD_ID, USER_ID))
                .thenReturn(false, true, true, true);

        ForumInteractionResponse first = service.favorite(THREAD_ID, USER_ID);
        ForumInteractionResponse second = service.favorite(THREAD_ID, USER_ID);

        assertThat(first.isFavorited()).isTrue();
        assertThat(second.isFavorited()).isTrue();
        assertThat(thread.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepo, times(1)).save(any(ForumPostFavorite.class));
        verify(threadRepo, times(1)).save(thread);
    }

    @Test
    void unfavoriteWithoutExistingFavoriteDoesNotChangeCount() {
        thread.setFavoriteCount(0);
        when(favoriteRepo.findByPostIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        ForumInteractionResponse response = service.unfavorite(THREAD_ID, USER_ID);

        assertThat(response.isFavorited()).isFalse();
        assertThat(thread.getFavoriteCount()).isZero();
        verify(favoriteRepo, never()).delete(any());
        verify(threadRepo, never()).save(thread);
    }
}
