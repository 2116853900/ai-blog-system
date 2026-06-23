package com.aiblog.controller.admin;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.Mcp;
import com.aiblog.repository.McpRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mcps")
public class AdminMcpController {

    private final McpRepository repo;
    private final PublicContentCacheService cacheService;

    public AdminMcpController(McpRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    @GetMapping
    public Page<Mcp> list(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping
    public Mcp create(@RequestBody Mcp m) {
        m.setId(null);
        Mcp saved = repo.save(m);
        cacheService.evictMcps();
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mcp> update(@PathVariable Long id, @RequestBody Mcp body) {
        return repo.findById(id).map(m -> {
            m.setName(body.getName());
            m.setDescription(body.getDescription());
            m.setRepoUrl(body.getRepoUrl());
            m.setInstallCmd(body.getInstallCmd());
            m.setTags(body.getTags());
            m.setCategory(body.getCategory());
            m.setRecommendLevel(body.getRecommendLevel());
            Mcp saved = repo.save(m);
            cacheService.evictMcps();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        cacheService.evictMcps();
        return ResponseEntity.noContent().build();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
