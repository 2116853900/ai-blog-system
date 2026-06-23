package com.aiblog.service;

import com.aiblog.dto.ForumTagSummaryResponse;
import com.aiblog.dto.ThreadRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumCategoryRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ForumThreadService {

    private static final String TARGET_TYPE = "FORUM_THREAD";
    private static final char LIKE_ESCAPE_CHAR = '\\';
    private static final List<ForumThread.ThreadStatus> VISIBLE_STATUSES = List.of(
            ForumThread.ThreadStatus.NORMAL,
            ForumThread.ThreadStatus.PINNED,
            ForumThread.ThreadStatus.FEATURED,
            ForumThread.ThreadStatus.LOCKED
    );

    private final ForumThreadRepository threadRepo;
    private final ForumReplyRepository replyRepo;
    private final ForumCategoryRepository categoryRepo;
    private final ForumUserRepository userRepo;
    private final AdminOperationLogRepository operationLogRepo;
    private final ForumViewCountBuffer viewCountBuffer;

    public ForumThreadService(ForumThreadRepository threadRepo,
                              ForumReplyRepository replyRepo,
                              ForumCategoryRepository categoryRepo,
                              ForumUserRepository userRepo,
                              AdminOperationLogRepository operationLogRepo,
                              ForumViewCountBuffer viewCountBuffer) {
        this.threadRepo = threadRepo;
        this.replyRepo = replyRepo;
        this.categoryRepo = categoryRepo;
        this.userRepo = userRepo;
        this.operationLogRepo = operationLogRepo;
        this.viewCountBuffer = viewCountBuffer;
    }

    public Page<ForumThread> listByCategory(Long categoryId, Pageable pageable) {
        return threadRepo.findByCategoryIdAndStatusIn(categoryId, VISIBLE_STATUSES, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, Pageable pageable) {
        return search(categoryId, q, null, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, String tag, Pageable pageable) {
        return search(categoryId, q, tag, null, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, String tag, Boolean unanswered, Pageable pageable) {
        return search(categoryId, q, tag, unanswered, null, pageable);
    }

    public Page<ForumThread> search(Long categoryId, String q, String tag, Boolean unanswered, Boolean solved, Pageable pageable) {
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String normalizedTag = normalizeTag(tag);

        Specification<ForumThread> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(VISIBLE_STATUSES));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (!keyword.isBlank()) {
                String pattern = likeContainsPattern(keyword);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("contentMarkdown")), pattern, LIKE_ESCAPE_CHAR),
                        cb.like(cb.lower(cb.coalesce(root.get("tags"), "")), pattern, LIKE_ESCAPE_CHAR)
                ));
            }
            if (!normalizedTag.isBlank()) {
                var compactTags = cb.function(
                        "replace",
                        String.class,
                        cb.lower(cb.coalesce(root.get("tags"), "")),
                        cb.literal(" "),
                        cb.literal("")
                );
                var delimitedTags = cb.concat(cb.concat(",", compactTags), ",");
                predicates.add(cb.like(delimitedTags, "%," + escapeLike(normalizedTag) + ",%", LIKE_ESCAPE_CHAR));
            }
            if (Boolean.TRUE.equals(unanswered)) {
                predicates.add(cb.equal(root.get("replyCount"), 0));
            }
            if (Boolean.TRUE.equals(solved)) {
                predicates.add(cb.isNotNull(root.get("acceptedReplyId")));
            } else if (Boolean.FALSE.equals(solved)) {
                predicates.add(cb.isNull(root.get("acceptedReplyId")));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return threadRepo.findAll(spec, pageable);
    }

    public Page<ForumThread> listAll(Pageable pageable) {
        return threadRepo.findByStatusIn(VISIBLE_STATUSES, pageable);
    }

    public List<ForumTagSummaryResponse> popularTags(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 50));
        Map<String, TagCounter> counters = new LinkedHashMap<>();
        for (String tagText : threadRepo.findTagTextsByStatusIn(VISIBLE_STATUSES)) {
            for (String rawTag : tagText.split(",")) {
                String tag = rawTag.trim();
                if (tag.isBlank()) continue;
                String key = tag.toLowerCase(Locale.ROOT);
                counters.computeIfAbsent(key, ignored -> new TagCounter(tag)).increment();
            }
        }

        return counters.values().stream()
                .sorted(Comparator.comparingLong(TagCounter::count).reversed()
                        .thenComparing(counter -> counter.tag().toLowerCase(Locale.ROOT)))
                .limit(cappedLimit)
                .map(counter -> new ForumTagSummaryResponse(counter.tag(), counter.count()))
                .toList();
    }

    public Page<ForumThread> listByAuthor(Long authorId, Pageable pageable) {
        return threadRepo.findByAuthorIdAndStatusIn(authorId, VISIBLE_STATUSES, pageable);
    }

    public Optional<ForumThread> findById(Long id) {
        return threadRepo.findById(id)
                .filter(t -> isVisible(t.getStatus()));
    }

    public List<ForumThread> findLinked(String refType, Long refId) {
        return threadRepo.findByLinkedRefTypeAndLinkedRefIdAndStatusIn(refType, refId, VISIBLE_STATUSES);
    }

    public Page<ForumThread> adminSearch(String q,
                                         String author,
                                         Long authorId,
                                         ForumThread.ThreadStatus status,
                                         Boolean reported,
                                         Instant createdFrom,
                                         Instant createdTo,
                                         Pageable pageable) {
        List<Long> authorIds = resolveAuthorIds(author);
        if (authorIds != null && authorIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<ForumThread> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String pattern = likeContainsPattern(q.trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("contentMarkdown")), pattern, LIKE_ESCAPE_CHAR),
                        cb.like(cb.lower(cb.coalesce(root.get("tags"), "")), pattern, LIKE_ESCAPE_CHAR)
                ));
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

        return threadRepo.findAll(spec, pageable);
    }

    public Optional<ForumThread> adminFindById(Long id) {
        return threadRepo.findById(id);
    }

    @Transactional
    public Optional<ForumThread> acceptReply(Long threadId, Long replyId, Long userId, boolean canModerate) {
        if (replyId == null) return Optional.empty();
        return threadRepo.findById(threadId)
                .filter(t -> canManageThread(t, userId, canModerate))
                .flatMap(t -> replyRepo.findById(replyId)
                        .filter(r -> r.getThreadId().equals(threadId))
                        .filter(r -> r.getStatus() == ForumReply.ReplyStatus.NORMAL)
                        .map(r -> {
                            t.setAcceptedReplyId(r.getId());
                            t.setAcceptedReplyUserId(r.getAuthorId());
                            t.setAcceptedAt(Instant.now());
                            return threadRepo.save(t);
                        }));
    }

    @Transactional
    public Optional<ForumThread> clearAcceptedReply(Long threadId, Long userId, boolean canModerate) {
        return threadRepo.findById(threadId)
                .filter(t -> canManageThread(t, userId, canModerate))
                .map(t -> {
                    t.setAcceptedReplyId(null);
                    t.setAcceptedReplyUserId(null);
                    t.setAcceptedAt(null);
                    return threadRepo.save(t);
                });
    }

    public List<AdminOperationLog> adminOperationLogs(Long id) {
        return operationLogRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TARGET_TYPE, id);
    }

    @Transactional
    public ForumThread create(ThreadRequest req, Long authorId) {
        ForumThread t = new ForumThread();
        t.setCategoryId(req.getCategoryId());
        t.setAuthorId(authorId);
        t.setTitle(req.getTitle());
        t.setContentMarkdown(req.getContentMarkdown());
        t.setTags(req.getTags());
        t.setLinkedRefType(req.getLinkedRefType());
        t.setLinkedRefId(req.getLinkedRefId());

        ForumThread saved = threadRepo.save(t);

        // 更新板块帖子计数
        incrementCategoryCount(req.getCategoryId());

        return saved;
    }

    @Transactional
    public Optional<ForumThread> update(Long id, ThreadRequest req, Long userId, boolean canModerate) {
        return threadRepo.findById(id)
                .filter(t -> canModerate || t.getAuthorId().equals(userId))
                .map(t -> {
                    Long oldCategoryId = t.getCategoryId();
                    t.setCategoryId(req.getCategoryId());
                    t.setTitle(req.getTitle());
                    t.setContentMarkdown(req.getContentMarkdown());
                    t.setTags(req.getTags());
                    t.setLinkedRefType(req.getLinkedRefType());
                    t.setLinkedRefId(req.getLinkedRefId());
                    if (!oldCategoryId.equals(req.getCategoryId())) {
                        decrementCategoryCount(oldCategoryId);
                        incrementCategoryCount(req.getCategoryId());
                    }
                    return threadRepo.save(t);
                });
    }

    @Transactional
    public boolean delete(Long id, Long userId, boolean isAdmin) {
        return threadRepo.findById(id)
                .filter(t -> isAdmin || t.getAuthorId().equals(userId))
                .map(t -> {
                    boolean wasVisible = isVisible(t.getStatus());
                    t.setStatus(ForumThread.ThreadStatus.DELETED);
                    threadRepo.save(t);
                    if (wasVisible) {
                        decrementCategoryCount(t.getCategoryId());
                    }
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        viewCountBuffer.recordView(id);
    }

    @Transactional
    public Optional<ForumThread> hide(Long id, String operatorUsername, String reason) {
        return threadRepo.findById(id).map(t -> {
            if (t.getStatus() != ForumThread.ThreadStatus.DELETED) {
                boolean wasVisible = isVisible(t.getStatus());
                t.setStatus(ForumThread.ThreadStatus.HIDDEN);
                ForumThread saved = threadRepo.save(t);
                if (wasVisible) {
                    decrementCategoryCount(t.getCategoryId());
                }
                recordOperation(operatorUsername, "HIDE_FORUM_THREAD", id, reason);
                return saved;
            }
            recordOperation(operatorUsername, "HIDE_FORUM_THREAD_SKIPPED", id, "Thread already deleted. " + nullToEmpty(reason));
            return t;
        });
    }

    @Transactional
    public Optional<ForumThread> restore(Long id, String operatorUsername, String reason) {
        return threadRepo.findById(id).map(t -> {
            boolean wasVisible = isVisible(t.getStatus());
            t.setStatus(ForumThread.ThreadStatus.NORMAL);
            ForumThread saved = threadRepo.save(t);
            if (!wasVisible) {
                incrementCategoryCount(t.getCategoryId());
            }
            recordOperation(operatorUsername, "RESTORE_FORUM_THREAD", id, reason);
            return saved;
        });
    }

    @Transactional
    public Optional<ForumThread> adminDelete(Long id, String operatorUsername, String reason) {
        return threadRepo.findById(id).map(t -> {
            boolean wasVisible = isVisible(t.getStatus());
            t.setStatus(ForumThread.ThreadStatus.DELETED);
            ForumThread saved = threadRepo.save(t);
            if (wasVisible) {
                decrementCategoryCount(t.getCategoryId());
            }
            recordOperation(operatorUsername, "DELETE_FORUM_THREAD", id, reason);
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

    private boolean isVisible(ForumThread.ThreadStatus status) {
        return VISIBLE_STATUSES.contains(status);
    }

    private boolean canManageThread(ForumThread thread, Long userId, boolean canModerate) {
        return canModerate || (userId != null && thread.getAuthorId().equals(userId));
    }

    static String normalizeTag(String tag) {
        if (tag == null) return "";
        return tag.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    static String likeContainsPattern(String value) {
        return "%" + escapeLike(value) + "%";
    }

    static String escapeLike(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == LIKE_ESCAPE_CHAR || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    private static final class TagCounter {
        private final String tag;
        private long count;

        private TagCounter(String tag) {
            this.tag = tag;
        }

        private void increment() {
            count++;
        }

        private String tag() {
            return tag;
        }

        private long count() {
            return count;
        }
    }

    private void decrementCategoryCount(Long categoryId) {
        categoryRepo.decrementThreadCount(categoryId);
    }

    private void incrementCategoryCount(Long categoryId) {
        categoryRepo.incrementThreadCount(categoryId);
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
