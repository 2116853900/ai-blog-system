package com.aiblog.service;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.repository.AdminOperationLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminOperationLogService {

    private final AdminOperationLogRepository operationLogRepo;

    public AdminOperationLogService(AdminOperationLogRepository operationLogRepo) {
        this.operationLogRepo = operationLogRepo;
    }

    public Page<AdminOperationLog> search(String operatorUsername,
                                          String action,
                                          String targetType,
                                          Long targetId,
                                          Instant createdFrom,
                                          Instant createdTo,
                                          Pageable pageable) {
        return operationLogRepo.findAll(buildSpec(
                clean(operatorUsername),
                clean(action),
                clean(targetType),
                targetId,
                createdFrom,
                createdTo), pageable);
    }

    private Specification<AdminOperationLog> buildSpec(String operatorUsername,
                                                       String action,
                                                       String targetType,
                                                       Long targetId,
                                                       Instant createdFrom,
                                                       Instant createdTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (operatorUsername != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("operatorUsername").as(String.class)),
                        "%" + operatorUsername.toLowerCase() + "%"));
            }
            if (action != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("action").as(String.class)),
                        "%" + action.toLowerCase() + "%"));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType.toUpperCase()));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
