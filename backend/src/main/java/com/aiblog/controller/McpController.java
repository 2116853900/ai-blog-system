package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.Mcp;
import com.aiblog.repository.McpRepository;
import com.aiblog.service.SearchSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcps")
public class McpController {

    private final McpRepository repo;
    private final PublicContentCacheService cacheService;

    public McpController(McpRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    @GetMapping
    public List<Mcp> list(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) String category) {
        return cacheService.publicContent(
                cacheService.mcpsListKey(q, tag, category),
                new TypeReference<List<Mcp>>() {},
                () -> repo.findAll(
                        SearchSpecs.build(q, tag, category, List.of("name", "description", "tags")),
                        Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mcp> detail(@PathVariable Long id) {
        Mcp mcp = cacheService.publicContent(cacheService.mcpDetailKey(id), Mcp.class, () -> repo.findById(id).orElse(null));
        return mcp == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(mcp);
    }
}
