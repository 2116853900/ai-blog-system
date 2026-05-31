package com.aiblog.repository;

import com.aiblog.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {
    List<ForumCategory> findByIsActiveTrueOrderBySortOrderAsc();
    List<ForumCategory> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);
    boolean existsBySlug(String slug);
}
