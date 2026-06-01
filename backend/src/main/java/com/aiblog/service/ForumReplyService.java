package com.aiblog.service;

import com.aiblog.dto.ReplyRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ForumReplyService {

    private static final String TARGET_TYPE = "FORUM_REPLY";
    private static final List<ForumReply.ReplyStatus> VISIBLE_STATUSES = List.of(ForumReply.ReplyStatus.NORMAL);

    private final ForumReplyRepository replyRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumUserRepository userRepo;
    private final AdminOperationLogRepository operationLogRepo;

    public ForumReplyService(ForumReplyRepository replyRepo,
                             ForumThreadRepository threadRepo,
                             ForumUserRepository userRepo,
                             AdminOperationLogRepository operationLogRepo) {
        this.replyRepo = replyRepo;
        this.threadRepo = threadRepo;
        this.userRepo = userRepo;
        this.operationLogRepo = operationLogRepo;
    }

    public Page<ForumReply> listByThread(Long threadId, Pageable pageable) {
        return replyRepo.findByThreadIdAndStatusOrderByFloorNumberAsc(threadId, ForumReply.ReplyStatus.NORMAL, pageable);
    }

    public Page<ForumReply> listByAuthor(Long authorId, Pageable pageable) {
        return replyRepo.findByAuthorIdAndStatusIn(authorId, VISIBLE_STATUSES, pageable);
    }

    public Page<ForumReply> adminSearch(Long threadId,
                                        String author,
                                        Long authorId,
                                        ForumReply.ReplyStatus status,
                                        Boolean reported,
                                        Instant createdFrom,
                                        Instant createdTo,
                                        Pageable pageable) {
        List<Long> authorIds = resolveAuthorIds(author);
        if (authorIds != null && authorIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<ForumReply> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (threadId != null) {
                predicates.add(cb.equal(root.get("threadId"), threadId));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("authorId"), authorId));
            }
            if (authorIds != null) {
                predicates.add(root.get("authorId").in(authorIds));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (Boolean.TRUE.equals(reported)) {
                predicates.add(cb.greaterThan(root.get("reportCount"), 0));
            } else if (Boolean.FALSE.equals(reported)) {
                predicates.add(cb.equal(root.get("reportCount"), 0));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return replyRepo.findAll(spec, pageable);
    }

    public Optional<ForumReply> adminFindById(Long id) {
        return replyRepo.findById(id);
    }

    public List<AdminOperationLog> adminOperationLogs(Long id) {
        return operationLogRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TARGET_TYPE, id);
    }

    @Transactional
    public ForumReply create(Long threadId, ReplyRequest req, Long authorId) {
        ForumThread thread = threadRepo.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        // 计算楼层号
        int floor = replyRepo.countByThreadIdAndStatus(threadId, ForumReply.ReplyStatus.NORMAL) + 1;

        ForumReply reply = new ForumReply();
        reply.setThreadId(threadId);
        reply.setAuthorId(authorId);
        reply.setFloorNumber(floor);
        reply.setContentMarkdown(req.getContentMarkdown());
        reply.setReplyToId(req.getReplyToId());

        // 如果引用了某条回复，记录其作者
        if (req.getReplyToId() != null) {
            replyRepo.findById(req.getReplyToId())
                    .ifPresent(r -> reply.setReplyToUserId(r.getAuthorId()));
        }

        ForumReply saved = replyRepo.save(reply);

        // 更新帖子的回复计数和最后回复信息
        thread.setReplyCount(thread.getReplyCount() + 1);
        thread.setLastReplyUserId(authorId);
        thread.setLastReplyAt(Instant.now());
        threadRepo.save(thread);

        return saved;
    }

    @Transactional
    public Optional<ForumReply> update(Long id, ReplyRequest req, Long userId) {
        return replyRepo.findById(id)
                .filter(r -> r.getAuthorId().equals(userId))
                .map(r -> {
                    r.setContentMarkdown(req.getContentMarkdown());
                    return replyRepo.save(r);
                });
    }

    @Transactional
    public boolean delete(Long id, Long userId, boolean isAdmin) {
        return replyRepo.findById(id)
                .filter(r -> isAdmin || r.getAuthorId().equals(userId))
                .map(r -> {
                    boolean wasVisible = isVisible(r.getStatus());
                    r.setStatus(ForumReply.ReplyStatus.DELETED);
                    replyRepo.save(r);
                    if (wasVisible) {
                        decrementThreadReplyCount(r.getThreadId());
                    }
                    return true;
                }).orElse(false);
    }

    @Transactional
    public Optional<ForumReply> hide(Long id, String operatorUsername, String reason) {
        return replyRepo.findById(id).map(r -> {
            if (r.getStatus() != ForumReply.ReplyStatus.DELETED) {
                boolean wasVisible = isVisible(r.getStatus());
                r.setStatus(ForumReply.ReplyStatus.HIDDEN);
                ForumReply saved = replyRepo.save(r);
                if (wasVisible) {
                    decrementThreadReplyCount(r.getThreadId());
                }
                recordOperation(operatorUsername, "HIDE_FORUM_REPLY", id, reason);
                return saved;
            }
            recordOperation(operatorUsername, "HIDE_FORUM_REPLY_SKIPPED", id, "Reply already deleted. " + nullToEmpty(reason));
            return r;
        });
    }

    @Transactional
    public Optional<ForumReply> restore(Long id, String operatorUsername, String reason) {
        return replyRepo.findById(id).map(r -> {
            boolean wasVisible = isVisible(r.getStatus());
            r.setStatus(ForumReply.ReplyStatus.NORMAL);
            ForumReply saved = replyRepo.save(r);
            if (!wasVisible) {
                incrementThreadReplyCount(r.getThreadId(), r.getAuthorId());
            }
            recordOperation(operatorUsername, "RESTORE_FORUM_REPLY", id, reason);
            return saved;
        });
    }

    @Transactional
    public Optional<ForumReply> adminDelete(Long id, String operatorUsername, String reason) {
        return replyRepo.findById(id).map(r -> {
            boolean wasVisible = isVisible(r.getStatus());
            r.setStatus(ForumReply.ReplyStatus.DELETED);
            ForumReply saved = replyRepo.save(r);
            if (wasVisible) {
                decrementThreadReplyCount(r.getThreadId());
            }
            recordOperation(operatorUsername, "DELETE_FORUM_REPLY", id, reason);
            return saved;
        });
    }

    @Transactional
    public int batchHide(List<Long> ids, String operatorUsername, String reason) {
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (Long id : ids) {
            if (hide(id, operatorUsername, reason).isPresent()) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int batchDelete(List<Long> ids, String operatorUsername, String reason) {
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (Long id : ids) {
            if (adminDelete(id, operatorUsername, reason).isPresent()) {
                count++;
            }
        }
        return count;
    }

    private List<Long> resolveAuthorIds(String author) {
        if (author == null || author.isBlank()) return null;
        String keyword = author.trim();
        return userRepo.findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(ForumUser::getId)
                .toList();
    }

    private boolean isVisible(ForumReply.ReplyStatus status) {
        return VISIBLE_STATUSES.contains(status);
    }

    private void decrementThreadReplyCount(Long threadId) {
        threadRepo.findById(threadId).ifPresent(t -> {
            t.setReplyCount(Math.max(0, t.getReplyCount() - 1));
            threadRepo.save(t);
        });
    }

    private void incrementThreadReplyCount(Long threadId, Long authorId) {
        threadRepo.findById(threadId).ifPresent(t -> {
            t.setReplyCount(t.getReplyCount() + 1);
            t.setLastReplyUserId(authorId);
            t.setLastReplyAt(Instant.now());
            threadRepo.save(t);
        });
    }

    private void recordOperation(String operatorUsername, String action, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
        log.setAction(action);
        log.setTargetType(TARGET_TYPE);
        log.setTargetId(targetId);
        log.setDetail(truncate(nullToEmpty(detail), 1000));
        operationLogRepo.save(log);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
