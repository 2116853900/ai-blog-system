package com.aiblog.controller.admin;

import com.aiblog.entity.*;
import com.aiblog.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/submissions")
public class AdminSubmissionController {

    private final SubmissionRepository repo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminSubmissionController(SubmissionRepository repo, SkillRepository skillRepo,
                                     McpRepository mcpRepo, ApiStationRepository apiRepo) {
        this.repo = repo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
    }

    @GetMapping
    public List<Submission> list(@RequestParam(required = false) Submission.Status status) {
        if (status != null) {
            return repo.findByStatusOrderByCreatedAtDesc(status);
        }
        return repo.findAllByOrderByCreatedAtDesc();
    }

    /** 通过投稿：落地为正式条目 */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return repo.findById(id).map(s -> {
            try {
                materialize(s);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "投稿内容解析失败: " + e.getMessage()));
            }
            s.setStatus(Submission.Status.APPROVED);
            repo.save(s);
            return ResponseEntity.ok(Map.of("message", "已通过并发布"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        return repo.findById(id).map(s -> {
            s.setStatus(Submission.Status.REJECTED);
            repo.save(s);
            return ResponseEntity.ok(Map.of("message", "已拒绝"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void materialize(Submission s) throws Exception {
        JsonNode n = mapper.readTree(s.getPayloadJson());
        switch (s.getType()) {
            case SKILL -> {
                Skill x = new Skill();
                x.setName(text(n, "name"));
                x.setDescription(text(n, "description"));
                x.setLink(text(n, "link"));
                x.setTags(text(n, "tags"));
                x.setCategory(text(n, "category"));
                x.setRecommendLevel(n.has("recommendLevel") ? n.get("recommendLevel").asInt(3) : 3);
                skillRepo.save(x);
            }
            case MCP -> {
                Mcp x = new Mcp();
                x.setName(text(n, "name"));
                x.setDescription(text(n, "description"));
                x.setRepoUrl(text(n, "repoUrl"));
                x.setInstallCmd(text(n, "installCmd"));
                x.setTags(text(n, "tags"));
                x.setCategory(text(n, "category"));
                x.setRecommendLevel(n.has("recommendLevel") ? n.get("recommendLevel").asInt(3) : 3);
                mcpRepo.save(x);
            }
            case API -> {
                ApiStation x = new ApiStation();
                x.setName(text(n, "name"));
                x.setBaseUrl(text(n, "baseUrl"));
                x.setDescription(text(n, "description"));
                x.setSupportedModels(text(n, "supportedModels"));
                x.setTags(text(n, "tags"));
                apiRepo.save(x);
            }
        }
    }

    private String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : null;
    }
}
