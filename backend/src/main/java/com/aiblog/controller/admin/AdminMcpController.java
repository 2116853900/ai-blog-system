package com.aiblog.controller.admin;

import com.aiblog.entity.Mcp;
import com.aiblog.repository.McpRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mcps")
public class AdminMcpController {

    private final McpRepository repo;

    public AdminMcpController(McpRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Mcp> list() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PostMapping
    public Mcp create(@RequestBody Mcp m) {
        m.setId(null);
        return repo.save(m);
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
            return ResponseEntity.ok(repo.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
