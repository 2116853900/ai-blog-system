package com.aiblog.controller.admin;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final PostRepository repo;
    private final PublicContentCacheService cacheService;

    public AdminPostController(PostRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    /** 全部教程（含未发布） */
    @GetMapping
    public Page<Post> list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        Page<Post> posts = repo.findAll(PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "updatedAt")));
        posts.forEach(p -> p.setBodyMarkdown(null));
        return posts;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Post body) {
        if (body.getSlug() == null || body.getSlug().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "slug 不能为空"));
        }
        if (repo.existsBySlug(body.getSlug())) {
            return ResponseEntity.badRequest().body(Map.of("message", "slug 已存在"));
        }
        body.setId(null);
        Post saved = repo.save(body);
        cacheService.evictPosts();
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> update(@PathVariable Long id, @RequestBody Post body) {
        return repo.findById(id).map(p -> {
            p.setTitle(body.getTitle());
            p.setSlug(body.getSlug());
            p.setSummary(body.getSummary());
            p.setBodyMarkdown(body.getBodyMarkdown());
            p.setTags(body.getTags());
            p.setCategory(body.getCategory());
            p.setPublished(body.isPublished());
            Post saved = repo.save(p);
            cacheService.evictPosts();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 发布/下架切换 */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Post> togglePublish(@PathVariable Long id, @RequestParam boolean published) {
        return repo.findById(id).map(p -> {
            p.setPublished(published);
            Post saved = repo.save(p);
            cacheService.evictPosts();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        cacheService.evictPosts();
        return ResponseEntity.noContent().build();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
