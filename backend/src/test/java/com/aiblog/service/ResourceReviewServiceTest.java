package com.aiblog.service;

import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.dto.ResourceReviewSummaryResponse;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceReview;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceReviewRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.aiblog.entity.ResourceReview.RefType.API;
import static com.aiblog.entity.ResourceReview.RefType.POST;
import static com.aiblog.entity.ResourceReview.RefType.SKILL;
import static com.aiblog.entity.ResourceReview.ReviewStatus.DELETED;
import static com.aiblog.entity.ResourceReview.ReviewStatus.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceReviewServiceTest {

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final long POST_ID = 10L;
    private static final long SKILL_ID = 11L;
    private static final long API_ID = 12L;

    @Mock
    private ResourceReviewRepository reviewRepo;

    @Mock
    private PostRepository postRepo;

    @Mock
    private SkillRepository skillRepo;

    @Mock
    private McpRepository mcpRepo;

    @Mock
    private ApiStationRepository apiRepo;

    private ResourceReviewService service;

    @BeforeEach
    void setUp() {
        service = new ResourceReviewService(reviewRepo, postRepo, skillRepo, mcpRepo, apiRepo);
    }

    @Test
    void summaryReturnsAverageCountAndCurrentUsersReview() {
        ResourceReview ownReview = review(USER_ID, SKILL, SKILL_ID, 4, "稳定好用", NORMAL);
        when(skillRepo.existsById(SKILL_ID)).thenReturn(true);
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(USER_ID, SKILL, SKILL_ID)).thenReturn(Optional.of(ownReview));
        when(reviewRepo.averageRating(SKILL, SKILL_ID, NORMAL)).thenReturn(4.25);
        when(reviewRepo.countByRefTypeAndRefIdAndStatus(SKILL, SKILL_ID, NORMAL)).thenReturn(2L);

        ResourceReviewSummaryResponse response = service.summary(SKILL, SKILL_ID, USER_ID);

        assertThat(response.getAverageRating()).isEqualTo(4.3);
        assertThat(response.getReviewCount()).isEqualTo(2);
        assertThat(response.getMyReview()).isNotNull();
        assertThat(response.getMyReview().getRating()).isEqualTo(4);
        assertThat(response.getMyReview().getContent()).isEqualTo("稳定好用");
        verify(reviewRepo).averageRating(SKILL, SKILL_ID, NORMAL);
    }

    @Test
    void listReturnsOnlyNormalReviewsForExistingResource() {
        PageRequest pageable = PageRequest.of(0, 10);
        ResourceReview review = review(OTHER_USER_ID, API, API_ID, 5, "速度快", NORMAL);
        when(apiRepo.existsById(API_ID)).thenReturn(true);
        when(reviewRepo.findByRefTypeAndRefIdAndStatusOrderByCreatedAtDesc(API, API_ID, NORMAL, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        var page = service.list(API, API_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(page.getContent().get(0).getRating()).isEqualTo(5);
    }

    @Test
    void upsertCreatesNewReviewForPublishedTutorial() {
        ResourceReviewRequest request = request(5, "  很适合入门  ");
        ResourceReview saved = review(USER_ID, POST, POST_ID, 5, "很适合入门", NORMAL);
        saved.setId(99L);
        when(postRepo.findById(POST_ID)).thenReturn(Optional.of(post(true)));
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(USER_ID, POST, POST_ID)).thenReturn(Optional.empty());
        when(reviewRepo.save(any(ResourceReview.class))).thenReturn(saved);

        var response = service.upsert(POST, POST_ID, USER_ID, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("很适合入门");
        verify(reviewRepo).save(any(ResourceReview.class));
    }

    @Test
    void upsertRestoresDeletedExistingReview() {
        ResourceReview existing = review(USER_ID, SKILL, SKILL_ID, 2, "旧评价", DELETED);
        ResourceReviewRequest request = request(4, "更新后不错");
        when(skillRepo.existsById(SKILL_ID)).thenReturn(true);
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(USER_ID, SKILL, SKILL_ID)).thenReturn(Optional.of(existing));
        when(reviewRepo.save(existing)).thenReturn(existing);

        var response = service.upsert(SKILL, SKILL_ID, USER_ID, request);

        assertThat(existing.getStatus()).isEqualTo(NORMAL);
        assertThat(existing.getRating()).isEqualTo(4);
        assertThat(existing.getContent()).isEqualTo("更新后不错");
        assertThat(response.getRating()).isEqualTo(4);
    }

    @Test
    void deleteOwnSoftDeletesExistingReview() {
        ResourceReview existing = review(USER_ID, SKILL, SKILL_ID, 3, "一般", NORMAL);
        when(skillRepo.existsById(SKILL_ID)).thenReturn(true);
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(USER_ID, SKILL, SKILL_ID)).thenReturn(Optional.of(existing));

        boolean deleted = service.deleteOwn(SKILL, SKILL_ID, USER_ID);

        assertThat(deleted).isTrue();
        assertThat(existing.getStatus()).isEqualTo(DELETED);
        verify(reviewRepo).save(existing);
    }

    @Test
    void rejectsUnpublishedTutorial() {
        when(postRepo.findById(POST_ID)).thenReturn(Optional.of(post(false)));

        assertThatThrownBy(() -> service.upsert(POST, POST_ID, USER_ID, request(5, "内容")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("资源不存在");

        verifyNoInteractions(reviewRepo);
    }

    @Test
    void rejectsMissingSkillWithoutSaving() {
        when(skillRepo.existsById(SKILL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.upsert(SKILL, SKILL_ID, USER_ID, request(5, "内容")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("资源不存在");

        verify(reviewRepo, never()).save(any(ResourceReview.class));
    }

    private ResourceReviewRequest request(Integer rating, String content) {
        ResourceReviewRequest request = new ResourceReviewRequest();
        request.setRating(rating);
        request.setContent(content);
        return request;
    }

    private ResourceReview review(Long userId,
                                  ResourceReview.RefType refType,
                                  Long refId,
                                  Integer rating,
                                  String content,
                                  ResourceReview.ReviewStatus status) {
        ResourceReview review = new ResourceReview();
        review.setUserId(userId);
        review.setRefType(refType);
        review.setRefId(refId);
        review.setRating(rating);
        review.setContent(content);
        review.setStatus(status);
        review.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));
        review.setUpdatedAt(Instant.parse("2026-06-02T00:00:00Z"));
        return review;
    }

    private Post post(boolean published) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setTitle("新手入门");
        post.setSlug("getting-started");
        post.setPublished(published);
        return post;
    }
}
