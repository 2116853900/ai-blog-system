package com.aiblog.repository;

import com.aiblog.entity.ForumThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>, JpaSpecificationExecutor<ForumThread> {
    Page<ForumThread> findByCategoryIdAndStatusIn(Long categoryId, Collection<ForumThread.ThreadStatus> statuses, Pageable pageable);
    Page<ForumThread> findByStatusIn(Collection<ForumThread.ThreadStatus> statuses, Pageable pageable);
    @Query("""
            select t from ForumThread t
            where t.status in :visibleStatuses
              and (:categoryId is null or t.categoryId = :categoryId)
              and (
                lower(t.title) like lower(concat('%', :q, '%'))
                or lower(t.contentMarkdown) like lower(concat('%', :q, '%'))
                or lower(coalesce(t.tags, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<ForumThread> searchVisible(
            @Param("categoryId") Long categoryId,
            @Param("q") String q,
            @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses,
            Pageable pageable);
    Page<ForumThread> findByAuthorIdAndStatusIn(Long authorId, Collection<ForumThread.ThreadStatus> statuses, Pageable pageable);
    List<ForumThread> findByLinkedRefTypeAndLinkedRefIdAndStatusIn(String refType, Long refId, Collection<ForumThread.ThreadStatus> statuses);
}
