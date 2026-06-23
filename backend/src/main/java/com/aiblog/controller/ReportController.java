package com.aiblog.controller;

import com.aiblog.dto.ContentReportRequest;
import com.aiblog.entity.ContentReport;
import com.aiblog.service.ContentReportService;
import com.aiblog.service.ForumUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ContentReportService reportService;
    private final ForumUserService userService;

    public ReportController(ContentReportService reportService, ForumUserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody ContentReportRequest req, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        try {
            ContentReport report = reportService.submit(req, userId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
    }
}
