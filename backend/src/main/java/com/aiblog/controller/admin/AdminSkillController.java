package com.aiblog.controller.admin;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.Skill;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/skills")
public class AdminSkillController {

    private final SkillRepository repo;
    private final PublicContentCacheService cacheService;

    public AdminSkillController(SkillRepository repo, PublicContentCacheService cacheService) {
        this.repo = repo;
        this.cacheService = cacheService;
    }

    @GetMapping
    public Page<Skill> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping
    public Skill create(@RequestBody Skill s) {
        s.setId(null);
        Skill saved = repo.save(s);
        cacheService.evictSkills();
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skill> update(@PathVariable Long id, @RequestBody Skill body) {
        return repo.findById(id).map(s -> {
            s.setName(body.getName());
            s.setDescription(body.getDescription());
            s.setLink(body.getLink());
            s.setTags(body.getTags());
            s.setCategory(body.getCategory());
            s.setRecommendLevel(body.getRecommendLevel());
            Skill saved = repo.save(s);
            cacheService.evictSkills();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        cacheService.evictSkills();
        return ResponseEntity.noContent().build();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
