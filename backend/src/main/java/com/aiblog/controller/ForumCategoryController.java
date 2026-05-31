package com.aiblog.controller;

import com.aiblog.entity.ForumCategory;
import com.aiblog.repository.ForumCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
public class ForumCategoryController {

    private final ForumCategoryRepository repo;

    public ForumCategoryController(ForumCategoryRepository repo) {
        this.repo = repo;
    }

    /** 获取所有活跃板块（含层级） */
    @GetMapping
    public List<ForumCategory> list() {
        return repo.findByIsActiveTrueOrderBySortOrderAsc();
    }

    /** 获取某板块详情 */
    @GetMapping("/{id}")
    public ResponseEntity<ForumCategory> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
