package com.aiblog.repository;

import com.aiblog.entity.ForumPostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumPostFavoriteRepository extends JpaRepository<ForumPostFavorite, Long> {
    Optional<ForumPostFavorite> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
