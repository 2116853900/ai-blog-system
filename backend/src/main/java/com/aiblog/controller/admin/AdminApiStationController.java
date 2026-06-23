package com.aiblog.controller.admin;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.StatusCheckService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/api-stations")
public class AdminApiStationController {

    private final ApiStationRepository repo;
    private final StatusCheckService statusCheckService;
    private final PublicContentCacheService cacheService;

    public AdminApiStationController(ApiStationRepository repo,
                                     StatusCheckService statusCheckService,
                                     PublicContentCacheService cacheService) {
        this.repo = repo;
        this.statusCheckService = statusCheckService;
        this.cacheService = cacheService;
    }

    @GetMapping
    public Page<ApiStation> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.ASC, "name")));
    }

    @PostMapping
    public ApiStation create(@RequestBody ApiStation a) {
        a.setId(null);
        ApiStation saved = repo.save(a);
        cacheService.evictApiStations();
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiStation> update(@PathVariable Long id, @RequestBody ApiStation body) {
        return repo.findById(id).map(a -> {
            a.setName(body.getName());
            a.setBaseUrl(body.getBaseUrl());
            a.setDescription(body.getDescription());
            a.setSupportedModels(body.getSupportedModels());
            a.setTags(body.getTags());
            ApiStation saved = repo.save(a);
            cacheService.evictApiStations();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        cacheService.evictApiStations();
        return ResponseEntity.noContent().build();
    }

    /** 手动触发单站点检测 */
    @PostMapping("/{id}/check")
    public ResponseEntity<ApiStation> check(@PathVariable Long id) {
        return repo.findById(id)
                .map(a -> {
                    ApiStation checked = statusCheckService.checkAndSave(a);
                    cacheService.evictApiStations();
                    return ResponseEntity.ok(checked);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** 手动触发全部检测 */
    @PostMapping("/check-all")
    public ResponseEntity<Void> checkAll() {
        statusCheckService.checkAll();
        cacheService.evictApiStations();
        return ResponseEntity.ok().build();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
