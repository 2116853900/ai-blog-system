package com.aiblog.repository;

import com.aiblog.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStatusOrderByCreatedAtDesc(Submission.Status status);
    Page<Submission> findByStatusOrderByCreatedAtDesc(Submission.Status status, Pageable pageable);
    List<Submission> findAllByOrderByCreatedAtDesc();
    Page<Submission> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(Submission.Status status);
}
