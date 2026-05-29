package com.aiblog.repository;

import com.aiblog.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByPublishedTrueOrderByCreatedAtDesc();
}
