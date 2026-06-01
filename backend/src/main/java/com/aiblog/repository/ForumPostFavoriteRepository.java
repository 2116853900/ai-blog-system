package com.aiblog.repository;

import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumPostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ForumPostFavoriteRepository extends JpaRepository<ForumPostFavorite, Long> {
    Optional<ForumPostFavorite> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);

    @Query(value = """
            select t from ForumThread t, ForumPostFavorite f
            where f.postId = t.id
              and f.userId = :userId
              and t.status in :visibleStatuses
            order by f.createdAt desc
            """, countQuery = """
            select count(t) from ForumThread t, ForumPostFavorite f
            where f.postId = t.id
              and f.userId = :userId
              and t.status in :visibleStatuses
            """)
    Page<ForumThread> findFavoriteThreadsByUserId(
            @Param("userId") Long userId,
            @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses,
            Pageable pageable);
}
