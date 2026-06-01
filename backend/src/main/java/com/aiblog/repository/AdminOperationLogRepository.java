package com.aiblog.repository;

import com.aiblog.entity.AdminOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {
    List<AdminOperationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
