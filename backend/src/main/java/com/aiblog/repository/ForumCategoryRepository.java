package com.aiblog.repository;

import com.aiblog.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {
    List<ForumCategory> findByIsActiveTrueOrderBySortOrderAsc();
    List<ForumCategory> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);
    boolean existsBySlug(String slug);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ForumCategory c set c.threadCount = c.threadCount + 1 where c.id = :id")
    int incrementThreadCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ForumCategory c set c.threadCount = case when c.threadCount > 0 then c.threadCount - 1 else 0 end where c.id = :id")
    int decrementThreadCount(@Param("id") Long id);
}
