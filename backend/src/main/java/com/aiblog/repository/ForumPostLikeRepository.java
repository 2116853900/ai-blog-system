package com.aiblog.repository;

import com.aiblog.entity.ForumPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, Long> {
    Optional<ForumPostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
