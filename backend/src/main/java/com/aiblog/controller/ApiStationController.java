package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ApiStationHealthDashboardResponse;
import com.aiblog.dto.ApiStationHealthTrendResponse;
import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.dto.ApiStationStatusSummaryResponse;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ResourceReview;
import com.aiblog.service.ResourceReviewBatchAggregator;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.aiblog.service.ResourceTagService;
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
    private final ResourceTagService tagService;
    private final ResourceReviewBatchAggregator reviewBatch;

    public ApiStationController(ApiStationRepository repo,
                                ApiStationStatusHistoryService historyService,
                                PublicContentCacheService cacheService,
                                ResourceTagService tagService,
                                ResourceReviewBatchAggregator reviewBatch) {
        this.repo = repo;
        this.historyService = historyService;
        this.cacheService = cacheService;
        this.tagService = tagService;
        this.reviewBatch = reviewBatch;
    }

    @GetMapping
    public List<ApiStation> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String tag,
                                 @RequestParam(required = false) ApiStation.Status status) {
        List<ApiStation> stations = cacheService.publicContent(
                cacheService.apiStationsListKey(q, tag, status),
                new TypeReference<List<ApiStation>>() {},
                () -> {
                    var spec = SearchSpecs.<ApiStation>build(q, tag, null, List.of("name", "description", "supportedModels", "tags"));
                    if (status != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
                    }
                    return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));
                });
        reviewBatch.apply(ResourceReview.RefType.API, stations);
        return stations;
    }

    @GetMapping("/tags/popular")
    public List<ResourceTagSummaryResponse> popularTags(@RequestParam(defaultValue = "20") int limit) {
        return cacheService.publicContent(
                cacheService.apiStationsPopularTagsKey(limit),
                new TypeReference<List<ResourceTagSummaryResponse>>() {},
                () -> tagService.apiStationPopularTags(limit));
    }

    @GetMapping("/health-dashboard")
    public ApiStationHealthDashboardResponse healthDashboard(
            @RequestParam(defaultValue = "30") int sampleLimit,
            @RequestParam(defaultValue = "10") int failureLimit) {
        return historyService.healthDashboard(sampleLimit, failureLimit);
    }

    @GetMapping("/health-trends")
    public ApiStationHealthTrendResponse healthTrends(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int incidentLimit) {
        return historyService.healthTrends(days, incidentLimit);
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

    @GetMapping("/{id}/checks/summary")
    public ResponseEntity<ApiStationStatusSummaryResponse> checkSummary(@PathVariable Long id,
                                                                        @RequestParam(defaultValue = "30") int limit) {
        return historyService.summary(id, limit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
