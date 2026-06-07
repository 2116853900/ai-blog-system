package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.Mcp;
import com.aiblog.repository.McpRepository;
import com.aiblog.service.ResourceTagService;
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
    private final ResourceTagService tagService;

    public McpController(McpRepository repo, PublicContentCacheService cacheService, ResourceTagService tagService) {
        this.repo = repo;
        this.cacheService = cacheService;
        this.tagService = tagService;
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

    @GetMapping("/tags/popular")
    public List<ResourceTagSummaryResponse> popularTags(@RequestParam(defaultValue = "20") int limit) {
        return cacheService.publicContent(
                cacheService.mcpsPopularTagsKey(limit),
                new TypeReference<List<ResourceTagSummaryResponse>>() {},
                () -> tagService.mcpPopularTags(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mcp> detail(@PathVariable Long id) {
        Mcp mcp = cacheService.publicContent(cacheService.mcpDetailKey(id), Mcp.class, () -> repo.findById(id).orElse(null));
        return mcp == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(mcp);
    }
}
