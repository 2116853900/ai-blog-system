package com.aiblog.controller.admin;

import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.StatusCheckService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-stations")
public class AdminApiStationController {

    private final ApiStationRepository repo;
    private final StatusCheckService statusCheckService;

    public AdminApiStationController(ApiStationRepository repo, StatusCheckService statusCheckService) {
        this.repo = repo;
        this.statusCheckService = statusCheckService;
    }

    @GetMapping
    public List<ApiStation> list() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @PostMapping
    public ApiStation create(@RequestBody ApiStation a) {
        a.setId(null);
        return repo.save(a);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiStation> update(@PathVariable Long id, @RequestBody ApiStation body) {
        return repo.findById(id).map(a -> {
            a.setName(body.getName());
            a.setBaseUrl(body.getBaseUrl());
            a.setDescription(body.getDescription());
            a.setSupportedModels(body.getSupportedModels());
            a.setTags(body.getTags());
            return ResponseEntity.ok(repo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** 手动触发单站点检测 */
    @PostMapping("/{id}/check")
    public ResponseEntity<ApiStation> check(@PathVariable Long id) {
        return repo.findById(id)
                .map(a -> ResponseEntity.ok(statusCheckService.checkAndSave(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 手动触发全部检测 */
    @PostMapping("/check-all")
    public ResponseEntity<Void> checkAll() {
        statusCheckService.checkAll();
        return ResponseEntity.ok().build();
    }
}
