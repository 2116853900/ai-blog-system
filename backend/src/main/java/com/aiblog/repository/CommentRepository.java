package com.aiblog.repository;

import com.aiblog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRefTypeAndRefIdAndApprovedTrueAndStatusOrderByCreatedAtDesc(
            Comment.RefType refType,
            Long refId,
            Comment.CommentStatus status);
    List<Comment> findByApprovedFalseOrderByCreatedAtDesc();
    List<Comment> findByApprovedFalseAndStatusOrderByCreatedAtDesc(Comment.CommentStatus status);
    List<Comment> findByStatusOrderByCreatedAtDesc(Comment.CommentStatus status);
    List<Comment> findAllByOrderByCreatedAtDesc();
}
