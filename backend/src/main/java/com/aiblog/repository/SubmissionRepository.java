package com.aiblog.repository;

import com.aiblog.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStatusOrderByCreatedAtDesc(Submission.Status status);
    List<Submission> findAllByOrderByCreatedAtDesc();
}
