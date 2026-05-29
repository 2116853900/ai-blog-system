package com.aiblog.controller.admin;

import com.aiblog.entity.Post;
import com.aiblog.repository.PostRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final PostRepository repo;

    public AdminPostController(PostRepository repo) {
        this.repo = repo;
    }

    /** 全部教程（含未发布） */
    @GetMapping
    public List<Post> list() {
        List<Post> posts = repo.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
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
        return ResponseEntity.ok(repo.save(body));
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
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 发布/下架切换 */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Post> togglePublish(@PathVariable Long id, @RequestParam boolean published) {
        return repo.findById(id).map(p -> {
            p.setPublished(published);
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
