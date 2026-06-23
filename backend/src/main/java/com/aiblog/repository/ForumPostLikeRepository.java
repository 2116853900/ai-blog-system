package com.aiblog.repository;

import com.aiblog.entity.ForumPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, Long> {
    Optional<ForumPostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);

    @Modifying
    @Query(value = """
            insert ignore into forum_post_like (post_id, user_id, created_at)
            values (:postId, :userId, current_timestamp(6))
            """, nativeQuery = true)
    int insertIgnore(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query("delete from ForumPostLike l where l.postId = :postId and l.userId = :userId")
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
