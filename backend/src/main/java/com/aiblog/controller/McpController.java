package com.aiblog.controller;

import com.aiblog.entity.Mcp;
import com.aiblog.repository.McpRepository;
import com.aiblog.service.SearchSpecs;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcps")
public class McpController {

    private final McpRepository repo;

    public McpController(McpRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Mcp> list(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String tag,
                          @RequestParam(required = false) String category) {
        return repo.findAll(
                SearchSpecs.build(q, tag, category, List.of("name", "description", "tags")),
                Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mcp> detail(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
