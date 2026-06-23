package com.aiblog.repository;

import com.aiblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRefTypeAndRefIdAndApprovedTrueAndStatusOrderByCreatedAtDesc(
            Comment.RefType refType,
            Long refId,
            Comment.CommentStatus status);
    List<Comment> findByApprovedFalseOrderByCreatedAtDesc();
    List<Comment> findByApprovedFalseAndStatusOrderByCreatedAtDesc(Comment.CommentStatus status);
    Page<Comment> findByApprovedFalseAndStatusOrderByCreatedAtDesc(Comment.CommentStatus status, Pageable pageable);
    long countByApprovedFalseAndStatus(Comment.CommentStatus status);
    List<Comment> findByStatusOrderByCreatedAtDesc(Comment.CommentStatus status);
    Page<Comment> findByStatusOrderByCreatedAtDesc(Comment.CommentStatus status, Pageable pageable);
    List<Comment> findAllByOrderByCreatedAtDesc();
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
