package com.aiblog.service;

import com.aiblog.dto.ContentReportRequest;
import com.aiblog.dto.ReportReviewRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.Comment;
import com.aiblog.entity.ContentReport;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.CommentRepository;
import com.aiblog.repository.ContentReportRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ContentReportService {

    private final ContentReportRepository reportRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumReplyRepository replyRepo;
    private final CommentRepository commentRepo;
    private final ForumThreadService threadService;
    private final ForumReplyService replyService;
    private final ForumUserService userService;
    private final AdminOperationLogRepository operationLogRepo;

    public ContentReportService(ContentReportRepository reportRepo,
                                ForumThreadRepository threadRepo,
                                ForumReplyRepository replyRepo,
                                CommentRepository commentRepo,
                                ForumThreadService threadService,
                                ForumReplyService replyService,
                                ForumUserService userService,
                                AdminOperationLogRepository operationLogRepo) {
        this.reportRepo = reportRepo;
        this.threadRepo = threadRepo;
        this.replyRepo = replyRepo;
        this.commentRepo = commentRepo;
        this.threadService = threadService;
        this.replyService = replyService;
        this.userService = userService;
        this.operationLogRepo = operationLogRepo;
    }

    @Transactional
    public ContentReport submit(ContentReportRequest req, Long reporterId) {
        TargetSnapshot snapshot = snapshot(req.getTargetType(), req.getTargetId());

        ContentReport report = new ContentReport();
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setTargetAuthorId(snapshot.targetAuthorId());
        report.setReporterId(reporterId);
        report.setReasonType(req.getReasonType());
        report.setReasonText(clean(req.getReasonText()));
        report.setContentSnapshot(snapshot.content());

        ContentReport saved = reportRepo.save(report);
        incrementReportCount(req.getTargetType(), req.getTargetId());
        return saved;
    }

    public Page<ContentReport> adminSearch(ContentReport.TargetType targetType,
                                           ContentReport.ReasonType reasonType,
                                           ContentReport.ReportStatus status,
                                           Instant createdFrom,
                                           Instant createdTo,
                                           Pageable pageable) {
        Specification<ContentReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (reasonType != null) {
                predicates.add(cb.equal(root.get("reasonType"), reasonType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return reportRepo.findAll(spec, pageable);
    }

    public Optional<ContentReport> findById(Long id) {
        return reportRepo.findById(id);
    }

    @Transactional
    public Optional<ContentReport> approve(Long id, ReportReviewRequest req, String reviewerUsername) {
        return review(id, ContentReport.ReportStatus.APPROVED, "APPROVED", req, reviewerUsername)
                .map(report -> {
                    if (req != null && req.isHideContent()) {
                        hideReportedContent(report, reviewerUsername, req.getReviewNote());
                    }
                    if (req != null && req.isBanTargetAuthor() && report.getTargetAuthorId() != null) {
                        userService.ban(
                                report.getTargetAuthorId(),
                                clean(req.getBanReason()) == null ? "举报审核通过" : clean(req.getBanReason()),
                                req.getBanEndTime(),
                                reviewerUsername);
                    }
                    return report;
                });
    }

    @Transactional
    public Optional<ContentReport> reject(Long id, ReportReviewRequest req, String reviewerUsername) {
        return review(id, ContentReport.ReportStatus.REJECTED, "REJECTED", req, reviewerUsername);
    }

    @Transactional
    public Optional<ContentReport> close(Long id, ReportReviewRequest req, String reviewerUsername) {
        return review(id, ContentReport.ReportStatus.CLOSED, "CLOSED", req, reviewerUsername);
    }

    private Optional<ContentReport> review(Long id,
                                           ContentReport.ReportStatus status,
                                           String result,
                                           ReportReviewRequest req,
                                           String reviewerUsername) {
        return reportRepo.findById(id).map(report -> {
            report.setStatus(status);
            report.setReviewResult(result);
            report.setReviewNote(req == null ? null : clean(req.getReviewNote()));
            report.setReviewerUsername(reviewerUsername);
            report.setReviewedAt(Instant.now());
            return reportRepo.save(report);
        });
    }

    private TargetSnapshot snapshot(ContentReport.TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> threadRepo.findById(targetId)
                    .map(thread -> new TargetSnapshot(
                            thread.getAuthorId(),
                            "# " + thread.getTitle() + "\n\n" + thread.getContentMarkdown()))
                    .orElseThrow(() -> new IllegalArgumentException("被举报帖子不存在"));
            case REPLY -> replyRepo.findById(targetId)
                    .map(reply -> new TargetSnapshot(reply.getAuthorId(), reply.getContentMarkdown()))
                    .orElseThrow(() -> new IllegalArgumentException("被举报回复不存在"));
            case COMMENT -> commentRepo.findById(targetId)
                    .map(comment -> new TargetSnapshot(null, comment.getAuthor() + "\n\n" + comment.getContent()))
                    .orElseThrow(() -> new IllegalArgumentException("被举报评论不存在"));
        };
    }

    private void incrementReportCount(ContentReport.TargetType targetType, Long targetId) {
        if (targetType == ContentReport.TargetType.POST) {
            threadRepo.findById(targetId).ifPresent(thread -> {
                thread.setReportCount(thread.getReportCount() + 1);
                threadRepo.save(thread);
            });
        } else if (targetType == ContentReport.TargetType.REPLY) {
            replyRepo.findById(targetId).ifPresent(reply -> {
                reply.setReportCount(reply.getReportCount() + 1);
                replyRepo.save(reply);
            });
        }
    }

    private void hideReportedContent(ContentReport report, String reviewerUsername, String note) {
        String detail = clean(note) == null ? "举报审核通过" : clean(note);
        if (report.getTargetType() == ContentReport.TargetType.POST) {
            threadService.hide(report.getTargetId(), reviewerUsername, detail);
        } else if (report.getTargetType() == ContentReport.TargetType.REPLY) {
            replyService.hide(report.getTargetId(), reviewerUsername, detail);
        } else if (report.getTargetType() == ContentReport.TargetType.COMMENT) {
            commentRepo.findById(report.getTargetId()).ifPresent(comment -> {
                comment.setStatus(Comment.CommentStatus.HIDDEN);
                commentRepo.save(comment);
                recordOperation(reviewerUsername, "HIDE_COMMENT", comment.getId(), detail);
            });
        }
    }

    private void recordOperation(String operatorUsername, String action, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
        log.setAction(action);
        log.setTargetType("COMMENT");
        log.setTargetId(targetId);
        log.setDetail(truncate(detail, 1000));
        operationLogRepo.save(log);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private record TargetSnapshot(Long targetAuthorId, String content) {}
}
