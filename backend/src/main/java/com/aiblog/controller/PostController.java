package com.aiblog.controller;

import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepo;

    public PostController(PostRepository postRepo) {
        this.postRepo = postRepo;
    }

    /** 已发布教程列表（不含正文，减小体积） */
    @GetMapping
    public List<Post> list() {
        List<Post> posts = postRepo.findByPublishedTrueOrderByCreatedAtDesc();
        posts.forEach(p -> p.setBodyMarkdown(null));
        return posts;
    }

    /** 教程详情（仅已发布） */
    @GetMapping("/{slug}")
    public ResponseEntity<Post> detail(@PathVariable String slug) {
        return postRepo.findBySlug(slug)
                .filter(Post::isPublished)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
