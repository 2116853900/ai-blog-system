package com.aiblog.repository;

import com.aiblog.entity.ContentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long>, JpaSpecificationExecutor<ContentReport> {
    long countByTargetTypeAndTargetId(ContentReport.TargetType targetType, Long targetId);
    List<ContentReport> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ContentReport.TargetType targetType, Long targetId);
}
