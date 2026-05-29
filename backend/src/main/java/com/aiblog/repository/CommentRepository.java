package com.aiblog.repository;

import com.aiblog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRefTypeAndRefIdAndApprovedTrueOrderByCreatedAtDesc(Comment.RefType refType, Long refId);
    List<Comment> findByApprovedFalseOrderByCreatedAtDesc();
}
