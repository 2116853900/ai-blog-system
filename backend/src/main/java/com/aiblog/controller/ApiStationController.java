package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.aiblog.service.SearchSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-stations")
public class ApiStationController {

    private final ApiStationRepository repo;
    private final ApiStationStatusHistoryService historyService;
    private final PublicContentCacheService cacheService;

    public ApiStationController(ApiStationRepository repo,
                                ApiStationStatusHistoryService historyService,
                                PublicContentCacheService cacheService) {
        this.repo = repo;
        this.historyService = historyService;
        this.cacheService = cacheService;
    }

    @GetMapping
    public List<ApiStation> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String tag,
                                 @RequestParam(required = false) ApiStation.Status status) {
        return cacheService.publicContent(
                cacheService.apiStationsListKey(q, tag, status),
                new TypeReference<List<ApiStation>>() {},
                () -> {
                    var spec = SearchSpecs.<ApiStation>build(q, tag, null, List.of("name", "description", "supportedModels", "tags"));
                    if (status != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
                    }
                    return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));
                });
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiStation> detail(@PathVariable Long id) {
        ApiStation station = cacheService.publicContent(
                cacheService.apiStationDetailKey(id),
                ApiStation.class,
                () -> repo.findById(id).orElse(null));
        return station == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(station);
    }

    @GetMapping("/{id}/checks")
    public ResponseEntity<List<ApiStationStatusCheckResponse>> checks(@PathVariable Long id,
                                                                      @RequestParam(defaultValue = "20") int limit) {
        return historyService.recent(id, limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
