package com.aiblog.repository;

import com.aiblog.entity.ForumThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>, JpaSpecificationExecutor<ForumThread> {
    Page<ForumThread> findByCategoryIdAndStatusIn(Long categoryId, Collection<ForumThread.ThreadStatus> statuses, Pageable pageable);
    Page<ForumThread> findByStatusIn(Collection<ForumThread.ThreadStatus> statuses, Pageable pageable);
    boolean existsByIdAndStatusIn(Long id, Collection<ForumThread.ThreadStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ForumThread t where t.id = :id")
    Optional<ForumThread> findByIdForUpdate(@Param("id") Long id);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.viewCount = t.viewCount + 1 where t.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.viewCount = t.viewCount + :delta where t.id = :id")
    int incrementViewCountBy(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.likeCount = t.likeCount + 1 where t.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.likeCount = case when t.likeCount > 0 then t.likeCount - 1 else 0 end where t.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.favoriteCount = t.favoriteCount + 1 where t.id = :id")
    int incrementFavoriteCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.favoriteCount = case when t.favoriteCount > 0 then t.favoriteCount - 1 else 0 end where t.id = :id")
    int decrementFavoriteCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update ForumThread t
            set t.replyCount = t.replyCount + 1,
                t.lastReplyUserId = :authorId,
                t.lastReplyAt = :lastReplyAt
            where t.id = :id
            """)
    int incrementReplyCount(@Param("id") Long id,
                            @Param("authorId") Long authorId,
                            @Param("lastReplyAt") Instant lastReplyAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.replyCount = case when t.replyCount > 0 then t.replyCount - 1 else 0 end where t.id = :id")
    int decrementReplyCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ForumThread t set t.reportCount = t.reportCount + 1 where t.id = :id")
    int incrementReportCount(@Param("id") Long id);
}
