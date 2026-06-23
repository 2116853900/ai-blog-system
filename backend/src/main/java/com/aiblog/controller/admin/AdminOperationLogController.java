package com.aiblog.controller.admin;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.service.AdminOperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    private final AdminOperationLogService operationLogService;

    public AdminOperationLogController(AdminOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Page<AdminOperationLog> list(@RequestParam(required = false) String operatorUsername,
                                        @RequestParam(required = false) String action,
                                        @RequestParam(required = false) String targetType,
                                        @RequestParam(required = false) Long targetId,
                                        @RequestParam(required = false) Instant createdFrom,
                                        @RequestParam(required = false) Instant createdTo,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return operationLogService.search(
                operatorUsername,
                action,
                targetType,
                targetId,
                createdFrom,
                createdTo,
                PageRequest.of(
                        normalizePage(page),
                        normalizeSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
