package com.aiblog.controller;

import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.aiblog.service.SearchSpecs;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-stations")
public class ApiStationController {

    private final ApiStationRepository repo;
    private final ApiStationStatusHistoryService historyService;

    public ApiStationController(ApiStationRepository repo, ApiStationStatusHistoryService historyService) {
        this.repo = repo;
        this.historyService = historyService;
    }

    @GetMapping
    public List<ApiStation> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String tag) {
        return repo.findAll(
                SearchSpecs.build(q, tag, null, List.of("name", "description", "supportedModels", "tags")),
                Sort.by(Sort.Direction.ASC, "name"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiStation> detail(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/checks")
    public ResponseEntity<List<ApiStationStatusCheckResponse>> checks(@PathVariable Long id,
                                                                      @RequestParam(defaultValue = "20") int limit) {
        return historyService.recent(id, limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
