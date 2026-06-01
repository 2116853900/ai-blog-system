package com.aiblog.controller.admin;

import com.aiblog.dto.ReportReviewRequest;
import com.aiblog.dto.ContentReportTargetResponse;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ContentReport;
import com.aiblog.service.ContentReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ContentReportService reportService;

    public AdminReportController(ContentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Page<ContentReport> list(
            @RequestParam(required = false) ContentReport.TargetType targetType,
            @RequestParam(required = false) ContentReport.ReasonType reasonType,
            @RequestParam(required = false) ContentReport.ReportStatus status,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportService.adminSearch(targetType, reasonType, status, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentReport> get(@PathVariable Long id) {
        return reportService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/operation-logs")
    public List<AdminOperationLog> operationLogs(@PathVariable Long id) {
        return reportService.adminOperationLogs(id);
    }

    @GetMapping("/{id}/target")
    public ResponseEntity<ContentReportTargetResponse> target(@PathVariable Long id) {
        return reportService.currentTarget(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ContentReport> approve(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) ReportReviewRequest req,
                                                 Authentication auth) {
        return reportService.approve(id, req, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ContentReport> reject(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) ReportReviewRequest req,
                                                Authentication auth) {
        return reportService.reject(id, req, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ContentReport> close(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) ReportReviewRequest req,
                                               Authentication auth) {
        return reportService.close(id, req, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String operator(Authentication auth) {
        return auth == null ? "unknown" : auth.getName();
    }
}
