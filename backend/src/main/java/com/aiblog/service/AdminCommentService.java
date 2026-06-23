package com.aiblog.service;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.Comment;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AdminCommentService {

    private static final String TARGET_TYPE = "COMMENT";

    private final CommentRepository commentRepo;
    private final AdminOperationLogRepository operationLogRepo;

    public AdminCommentService(CommentRepository commentRepo,
                               AdminOperationLogRepository operationLogRepo) {
        this.commentRepo = commentRepo;
        this.operationLogRepo = operationLogRepo;
    }

    public Page<Comment> list(Boolean pending, Comment.CommentStatus status, Pageable pageable) {
        if (Boolean.TRUE.equals(pending)) {
            Comment.CommentStatus effectiveStatus = status == null ? Comment.CommentStatus.NORMAL : status;
            return commentRepo.findByApprovedFalseAndStatusOrderByCreatedAtDesc(effectiveStatus, pageable);
        }
        if (status != null) {
            return commentRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return commentRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Optional<Comment> approve(Long id, String operatorUsername) {
        return commentRepo.findById(id).map(comment -> {
            comment.setApproved(true);
            Comment saved = commentRepo.save(comment);
            recordOperation(operatorUsername, "APPROVE_COMMENT", id, null);
            return saved;
        });
    }

    @Transactional
    public Optional<Comment> hide(Long id, String operatorUsername) {
        return commentRepo.findById(id).map(comment -> {
            if (comment.getStatus() != Comment.CommentStatus.DELETED) {
                comment.setStatus(Comment.CommentStatus.HIDDEN);
                Comment saved = commentRepo.save(comment);
                recordOperation(operatorUsername, "HIDE_COMMENT", id, null);
                return saved;
            }
            recordOperation(operatorUsername, "HIDE_COMMENT_SKIPPED", id, "Comment already deleted.");
            return comment;
        });
    }

    @Transactional
    public Optional<Comment> restore(Long id, String operatorUsername) {
        return commentRepo.findById(id).map(comment -> {
            comment.setStatus(Comment.CommentStatus.NORMAL);
            Comment saved = commentRepo.save(comment);
            recordOperation(operatorUsername, "RESTORE_COMMENT", id, null);
            return saved;
        });
    }

    @Transactional
    public Optional<Comment> softDelete(Long id, String operatorUsername) {
        return commentRepo.findById(id).map(comment -> {
            comment.setStatus(Comment.CommentStatus.DELETED);
            Comment saved = commentRepo.save(comment);
            recordOperation(operatorUsername, "DELETE_COMMENT", id, null);
            return saved;
        });
    }

    private void recordOperation(String operatorUsername, String action, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
        log.setAction(action);
        log.setTargetType(TARGET_TYPE);
        log.setTargetId(targetId);
        log.setDetail(detail);
        operationLogRepo.save(log);
    }
}
