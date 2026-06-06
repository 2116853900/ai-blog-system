package com.aiblog.repository;

import com.aiblog.entity.AdminOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long>,
        JpaSpecificationExecutor<AdminOperationLog> {
    List<AdminOperationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
