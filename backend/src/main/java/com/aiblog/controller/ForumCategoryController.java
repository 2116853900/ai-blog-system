package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.ForumCategory;
import com.aiblog.repository.ForumCategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
public class ForumCategoryController {

    private final ForumCategoryRepository repo;
    private final PublicContentCacheService cacheService;

    public ForumCategoryController(ForumCategoryRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    /** 获取所有活跃板块（含层级） */
    @GetMapping
    public List<ForumCategory> list() {
        return cacheService.publicContent(
                cacheService.forumCategoriesListKey(),
                new TypeReference<List<ForumCategory>>() {},
                repo::findByIsActiveTrueOrderBySortOrderAsc);
    }

    /** 获取某板块详情 */
    @GetMapping("/{id}")
    public ResponseEntity<ForumCategory> get(@PathVariable Long id) {
        ForumCategory category = cacheService.publicContent(
                cacheService.forumCategoryDetailKey(id),
                ForumCategory.class,
                () -> repo.findById(id).orElse(null));
        return category == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(category);
    }
}
