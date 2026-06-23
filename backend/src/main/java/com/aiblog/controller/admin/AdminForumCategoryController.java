package com.aiblog.controller.admin;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.ForumCategory;
import com.aiblog.repository.ForumCategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/forum-categories")
public class AdminForumCategoryController {

    private final ForumCategoryRepository repo;
    private final PublicContentCacheService cacheService;

    public AdminForumCategoryController(ForumCategoryRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    @GetMapping
    public List<ForumCategory> list() {
        return repo.findAll(Sort.by("sortOrder"));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ForumCategory body) {
        if (body.getSlug() == null || body.getSlug().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "slug 不能为空"));
        }
        if (repo.existsBySlug(body.getSlug())) {
            return ResponseEntity.badRequest().body(Map.of("message", "slug 已存在"));
        }
        body.setId(null);
        ForumCategory saved = repo.save(body);
        cacheService.evictForumCategories();
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForumCategory> update(@PathVariable Long id, @RequestBody ForumCategory body) {
        return repo.findById(id).map(c -> {
            c.setName(body.getName());
            c.setSlug(body.getSlug());
            c.setDescription(body.getDescription());
            c.setIcon(body.getIcon());
            c.setSortOrder(body.getSortOrder());
            c.setParentId(body.getParentId());
            c.setActive(body.isActive());
            ForumCategory saved = repo.save(c);
            cacheService.evictForumCategories();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        cacheService.evictForumCategories();
        return ResponseEntity.noContent().build();
    }
}
