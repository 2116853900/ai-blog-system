package com.aiblog.controller;

import com.aiblog.cache.CacheProperties;
import com.aiblog.cache.HybridCacheService;
import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import com.aiblog.service.ResourceReviewBatchAggregator;
import com.aiblog.service.ResourceTagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostControllerTest {

    @Test
    void listWithoutFiltersUsesPublishedListAndStripsBody() {
        PostRepository repo = mock(PostRepository.class);
        Post post = post(1L, "intro", true);
        post.setBodyMarkdown("# full body");
        when(repo.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(post));
        PostController controller = new PostController(repo, cacheService(), mock(ResourceTagService.class), mock(ResourceReviewBatchAggregator.class));

        List<Post> response = controller.list(null, null, null);

        assertThat(response).containsExactly(post);
        assertThat(response.get(0).getBodyMarkdown()).isNull();
        verify(repo).findByPublishedTrueOrderByCreatedAtDesc();
    }

    @Test
    void listWithFiltersUsesSpecificationSearchAndStripsBody() {
        PostRepository repo = mock(PostRepository.class);
        Post post = post(2L, "advanced", true);
        post.setBodyMarkdown("# full body");
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        when(repo.findAll(any(Specification.class), eq(expectedSort))).thenReturn(List.of(post));
        PostController controller = new PostController(repo, cacheService(), mock(ResourceTagService.class), mock(ResourceReviewBatchAggregator.class));

        List<Post> response = controller.list("prompt", "AI", "教程");

        assertThat(response).containsExactly(post);
        assertThat(response.get(0).getBodyMarkdown()).isNull();
        verify(repo).findAll(any(Specification.class), eq(expectedSort));
    }

    @Test
    void popularTagsForwardsLimit() {
        PostRepository repo = mock(PostRepository.class);
        ResourceTagService tagService = mock(ResourceTagService.class);
        List<ResourceTagSummaryResponse> tags = List.of(new ResourceTagSummaryResponse("Prompt", 3));
        when(tagService.postPopularTags(9)).thenReturn(tags);
        PostController controller = new PostController(repo, cacheService(), tagService, mock(ResourceReviewBatchAggregator.class));

        List<ResourceTagSummaryResponse> response = controller.popularTags(9);

        assertThat(response).isSameAs(tags);
        verify(tagService).postPopularTags(9);
    }

    @Test
    void detailReturnsOnlyPublishedPost() {
        PostRepository repo = mock(PostRepository.class);
        Post post = post(1L, "intro", true);
        when(repo.findBySlug("intro")).thenReturn(Optional.of(post));
        PostController controller = new PostController(repo, cacheService(), mock(ResourceTagService.class), mock(ResourceReviewBatchAggregator.class));

        var response = controller.detail("intro");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(post);
    }

    @Test
    void detailReturnsNotFoundForDraft() {
        PostRepository repo = mock(PostRepository.class);
        Post post = post(1L, "draft", false);
        when(repo.findBySlug("draft")).thenReturn(Optional.of(post));
        PostController controller = new PostController(repo, cacheService(), mock(ResourceTagService.class), mock(ResourceReviewBatchAggregator.class));

        var response = controller.detail("draft");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Post post(Long id, String slug, boolean published) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(slug);
        post.setSlug(slug);
        post.setPublished(published);
        post.setCreatedAt(Instant.parse("2026-06-07T00:00:00Z"));
        return post;
    }

    private PublicContentCacheService cacheService() {
        CacheProperties properties = new CacheProperties();
        properties.setKeyPrefix("post-controller-test");
        properties.setRedisEnabled(false);
        return new PublicContentCacheService(new HybridCacheService(properties, new ObjectMapper()), properties);
    }
}
