package com.aiblog.controller;

import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.service.SearchSpecs;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-stations")
public class ApiStationController {

    private final ApiStationRepository repo;

    public ApiStationController(ApiStationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ApiStation> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String tag) {
        return repo.findAll(
                SearchSpecs.build(q, tag, null, List.of("name", "description", "supportedModels", "tags")),
                Sort.by(Sort.Direction.ASC, "name"));
    }
}
