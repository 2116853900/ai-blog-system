package com.aiblog.controller.admin;

import com.aiblog.entity.Skill;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/skills")
public class AdminSkillController {

    private final SkillRepository repo;

    public AdminSkillController(SkillRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Skill> list() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PostMapping
    public Skill create(@RequestBody Skill s) {
        s.setId(null);
        return repo.save(s);
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
            return ResponseEntity.ok(repo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
