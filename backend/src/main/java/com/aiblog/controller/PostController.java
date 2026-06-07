package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import com.aiblog.service.ResourceTagService;
import com.aiblog.service.SearchSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepo;
    private final PublicContentCacheService cacheService;
    private final ResourceTagService tagService;

    public PostController(PostRepository postRepo, PublicContentCacheService cacheService, ResourceTagService tagService) {
        this.postRepo = postRepo;
        this.cacheService = cacheService;
        this.tagService = tagService;
    }

    /** 已发布教程列表（不含正文，减小体积） */
    @GetMapping
    public List<Post> list(@RequestParam(required = false) String q,
                           @RequestParam(required = false) String tag,
                           @RequestParam(required = false) String category) {
        return cacheService.publicContent(
                cacheService.postsListKey(q, tag, category),
                new TypeReference<List<Post>>() {},
                () -> {
                    List<Post> posts;
                    if (hasText(q) || hasText(tag) || hasText(category)) {
                        var spec = SearchSpecs.<Post>build(q, tag, category, List.of("title", "summary", "tags", "category"))
                                .and((root, query, cb) -> cb.isTrue(root.get("published")));
                        posts = postRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
                    } else {
                        posts = postRepo.findByPublishedTrueOrderByCreatedAtDesc();
                    }
                    posts.forEach(p -> p.setBodyMarkdown(null));
                    return posts;
                });
    }

    @GetMapping("/tags/popular")
    public List<ResourceTagSummaryResponse> popularTags(@RequestParam(defaultValue = "20") int limit) {
        return cacheService.publicContent(
                cacheService.postsPopularTagsKey(limit),
                new TypeReference<List<ResourceTagSummaryResponse>>() {},
                () -> tagService.postPopularTags(limit));
    }

    /** 教程详情（仅已发布） */
    @GetMapping("/{slug}")
    public ResponseEntity<Post> detail(@PathVariable String slug) {
        Post post = cacheService.publicContent(
                cacheService.postDetailKey(slug),
                Post.class,
                () -> postRepo.findBySlug(slug).filter(Post::isPublished).orElse(null));
        return post == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(post);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
