package com.aiblog.controller;

import com.aiblog.dto.PublicStatsResponse;
import com.aiblog.service.PublicStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class PublicStatsController {

    private final PublicStatsService statsService;

    public PublicStatsController(PublicStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public PublicStatsResponse stats() {
        return statsService.getStats();
    }
}
