package com.aiblog.repository;

import com.aiblog.entity.ResourceFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceFavoriteRepository extends JpaRepository<ResourceFavorite, Long> {
    Optional<ResourceFavorite> findByUserIdAndRefTypeAndRefId(Long userId, ResourceFavorite.RefType refType, Long refId);
    boolean existsByUserIdAndRefTypeAndRefId(Long userId, ResourceFavorite.RefType refType, Long refId);
    long countByRefTypeAndRefId(ResourceFavorite.RefType refType, Long refId);
    Page<ResourceFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
