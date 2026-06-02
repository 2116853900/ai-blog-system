package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepo;
    private final PublicContentCacheService cacheService;

    public PostController(PostRepository postRepo, PublicContentCacheService cacheService) {
        this.postRepo = postRepo;
        this.cacheService = cacheService;
    }

    /** 已发布教程列表（不含正文，减小体积） */
    @GetMapping
    public List<Post> list() {
        return cacheService.publicContent(
                cacheService.postsListKey(),
                new TypeReference<List<Post>>() {},
                () -> {
                    List<Post> posts = postRepo.findByPublishedTrueOrderByCreatedAtDesc();
                    posts.forEach(p -> p.setBodyMarkdown(null));
                    return posts;
                });
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
}
