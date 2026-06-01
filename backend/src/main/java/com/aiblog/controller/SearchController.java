package com.aiblog.controller;

import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.service.GlobalSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final GlobalSearchService searchService;

    public SearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public GlobalSearchResponse search(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Integer limit) {
        return searchService.search(q, limit);
    }
}
