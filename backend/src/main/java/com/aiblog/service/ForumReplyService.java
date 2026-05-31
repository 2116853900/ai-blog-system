package com.aiblog.service;

import com.aiblog.dto.ReplyRequest;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ForumReplyService {

    private final ForumReplyRepository replyRepo;
    private final ForumThreadRepository threadRepo;

    public ForumReplyService(ForumReplyRepository replyRepo, ForumThreadRepository threadRepo) {
        this.replyRepo = replyRepo;
        this.threadRepo = threadRepo;
    }

    public Page<ForumReply> listByThread(Long threadId, Pageable pageable) {
        return replyRepo.findByThreadIdAndStatusOrderByFloorNumberAsc(threadId, ForumReply.ReplyStatus.NORMAL, pageable);
    }

    public Page<ForumReply> listByAuthor(Long authorId, Pageable pageable) {
        return replyRepo.findByAuthorIdAndStatusNot(authorId, ForumReply.ReplyStatus.DELETED, pageable);
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
                    r.setStatus(ForumReply.ReplyStatus.DELETED);
                    replyRepo.save(r);
                    // 减少帖子回复计数
                    threadRepo.findById(r.getThreadId()).ifPresent(t -> {
                        t.setReplyCount(Math.max(0, t.getReplyCount() - 1));
                        threadRepo.save(t);
                    });
                    return true;
                }).orElse(false);
    }
}
