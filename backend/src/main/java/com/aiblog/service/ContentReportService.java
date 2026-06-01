package com.aiblog.service;

import com.aiblog.dto.ContentReportRequest;
import com.aiblog.dto.ContentReportTargetResponse;
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

    public List<AdminOperationLog> adminOperationLogs(Long id) {
        return operationLogRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("CONTENT_REPORT", id);
    }

    public Optional<ContentReportTargetResponse> currentTarget(Long reportId) {
        return reportRepo.findById(reportId)
                .map(report -> currentTarget(report.getTargetType(), report.getTargetId()));
    }

    public Page<ContentReport> submittedByUser(Long userId, Pageable pageable) {
        return reportRepo.findByReporterIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<ContentReport> receivedByUser(Long userId, Pageable pageable) {
        return reportRepo.findByTargetAuthorIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public Optional<ContentReport> approve(Long id, ReportReviewRequest req, String reviewerUsername) {
        return reportRepo.findById(id)
                .map(report -> {
                    if (!isPending(report)) {
                        return report;
                    }
                    ContentReport reviewed = applyReview(
                            report, ContentReport.ReportStatus.APPROVED, "APPROVED", req, reviewerUsername);
                    if (req != null && req.isHideContent()) {
                        hideReportedContent(reviewed, reviewerUsername, req.getReviewNote());
                    }
                    if (req != null && req.isBanTargetAuthor() && reviewed.getTargetAuthorId() != null) {
                        String banReason = clean(req.getBanReason());
                        userService.ban(
                                reviewed.getTargetAuthorId(),
                                banReason == null ? "举报审核通过" : banReason,
                                req.getBanEndTime(),
                                reviewerUsername);
                    }
                    return reviewed;
                });
    }

    @Transactional
    public Optional<ContentReport> reject(Long id, ReportReviewRequest req, String reviewerUsername) {
        return reportRepo.findById(id)
                .map(report -> isPending(report)
                        ? applyReview(report, ContentReport.ReportStatus.REJECTED, "REJECTED", req, reviewerUsername)
                        : report);
    }

    @Transactional
    public Optional<ContentReport> close(Long id, ReportReviewRequest req, String reviewerUsername) {
        return reportRepo.findById(id)
                .map(report -> isPending(report)
                        ? applyReview(report, ContentReport.ReportStatus.CLOSED, "CLOSED", req, reviewerUsername)
                        : report);
    }

    private ContentReport applyReview(ContentReport report,
                                      ContentReport.ReportStatus status,
                                      String result,
                                      ReportReviewRequest req,
                                      String reviewerUsername) {
        report.setStatus(status);
        report.setReviewResult(result);
        report.setReviewNote(req == null ? null : clean(req.getReviewNote()));
        report.setReviewerUsername(reviewerUsername);
        report.setReviewedAt(Instant.now());
        ContentReport saved = reportRepo.save(report);
        recordOperation(reviewerUsername, actionFor(status), saved.getId(), saved.getReviewNote(), "CONTENT_REPORT");
        return saved;
    }

    private boolean isPending(ContentReport report) {
        return report.getStatus() == ContentReport.ReportStatus.PENDING;
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

    private ContentReportTargetResponse currentTarget(ContentReport.TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> currentPostTarget(targetType, targetId);
            case REPLY -> currentReplyTarget(targetType, targetId);
            case COMMENT -> currentCommentTarget(targetType, targetId);
        };
    }

    private ContentReportTargetResponse currentPostTarget(ContentReport.TargetType targetType, Long targetId) {
        ContentReportTargetResponse response = baseTarget(targetType, targetId);
        threadRepo.findById(targetId).ifPresent(thread -> {
            response.setExists(true);
            response.setStatus(thread.getStatus().name());
            response.setAuthorId(thread.getAuthorId());
            response.setTitle(thread.getTitle());
            response.setContent(thread.getContentMarkdown());
            response.setRefType(thread.getLinkedRefType());
            response.setRefId(thread.getLinkedRefId());
            response.setCreatedAt(thread.getCreatedAt());
            response.setUpdatedAt(thread.getUpdatedAt());
        });
        return response;
    }

    private ContentReportTargetResponse currentReplyTarget(ContentReport.TargetType targetType, Long targetId) {
        ContentReportTargetResponse response = baseTarget(targetType, targetId);
        replyRepo.findById(targetId).ifPresent(reply -> {
            response.setExists(true);
            response.setStatus(reply.getStatus().name());
            response.setAuthorId(reply.getAuthorId());
            response.setTitle("回复 #" + reply.getId());
            response.setContent(reply.getContentMarkdown());
            response.setRefType("FORUM_THREAD");
            response.setRefId(reply.getThreadId());
            response.setCreatedAt(reply.getCreatedAt());
            response.setUpdatedAt(reply.getUpdatedAt());
        });
        return response;
    }

    private ContentReportTargetResponse currentCommentTarget(ContentReport.TargetType targetType, Long targetId) {
        ContentReportTargetResponse response = baseTarget(targetType, targetId);
        commentRepo.findById(targetId).ifPresent(comment -> {
            response.setExists(true);
            response.setStatus(comment.getStatus().name());
            response.setAuthorName(comment.getAuthor());
            response.setTitle("评论 #" + comment.getId());
            response.setContent(comment.getContent());
            response.setRefType(comment.getRefType().name());
            response.setRefId(comment.getRefId());
            response.setCreatedAt(comment.getCreatedAt());
        });
        return response;
    }

    private ContentReportTargetResponse baseTarget(ContentReport.TargetType targetType, Long targetId) {
        ContentReportTargetResponse response = new ContentReportTargetResponse();
        response.setTargetType(targetType);
        response.setTargetId(targetId);
        response.setExists(false);
        return response;
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
        recordOperation(operatorUsername, action, targetId, detail, "COMMENT");
    }

    private void recordOperation(String operatorUsername, String action, Long targetId, String detail, String targetType) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(truncate(detail, 1000));
        operationLogRepo.save(log);
    }

    private String actionFor(ContentReport.ReportStatus status) {
        return switch (status) {
            case APPROVED -> "APPROVE_CONTENT_REPORT";
            case REJECTED -> "REJECT_CONTENT_REPORT";
            case CLOSED -> "CLOSE_CONTENT_REPORT";
            case PENDING -> "REVIEW_CONTENT_REPORT";
        };
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
