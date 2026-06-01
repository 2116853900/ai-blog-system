package com.aiblog.repository;

import com.aiblog.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByPublishedTrueOrderByCreatedAtDesc();

    @Query("""
            select p from Post p
            where p.published = true
              and (
                lower(p.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.summary, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.tags, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.category, '')) like lower(concat('%', :q, '%'))
              )
            """)
    List<Post> searchPublished(@Param("q") String q, Pageable pageable);
}
