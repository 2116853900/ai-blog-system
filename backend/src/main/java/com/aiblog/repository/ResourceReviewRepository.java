package com.aiblog.repository;

import com.aiblog.entity.ResourceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceReviewRepository extends JpaRepository<ResourceReview, Long> {

    Optional<ResourceReview> findByUserIdAndRefTypeAndRefId(
            Long userId,
            ResourceReview.RefType refType,
            Long refId);

    Page<ResourceReview> findByRefTypeAndRefIdAndStatusOrderByCreatedAtDesc(
            ResourceReview.RefType refType,
            Long refId,
            ResourceReview.ReviewStatus status,
            Pageable pageable);

    long countByRefTypeAndRefIdAndStatus(
            ResourceReview.RefType refType,
            Long refId,
            ResourceReview.ReviewStatus status);

    @Query("""
            select coalesce(avg(r.rating), 0)
            from ResourceReview r
            where r.refType = :refType
              and r.refId = :refId
              and r.status = :status
            """)
    double averageRating(@Param("refType") ResourceReview.RefType refType,
                         @Param("refId") Long refId,
                         @Param("status") ResourceReview.ReviewStatus status);
}
