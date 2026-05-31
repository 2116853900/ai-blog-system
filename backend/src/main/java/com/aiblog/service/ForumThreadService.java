package com.aiblog.service;

import com.aiblog.dto.ThreadRequest;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.ForumCategoryRepository;
import com.aiblog.repository.ForumThreadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ForumThreadService {

    private final ForumThreadRepository threadRepo;
    private final ForumCategoryRepository categoryRepo;

    public ForumThreadService(ForumThreadRepository threadRepo, ForumCategoryRepository categoryRepo) {
        this.threadRepo = threadRepo;
        this.categoryRepo = categoryRepo;
    }

    public Page<ForumThread> listByCategory(Long categoryId, Pageable pageable) {
        return threadRepo.findByCategoryIdAndStatusNot(categoryId, ForumThread.ThreadStatus.DELETED, pageable);
    }

    public Page<ForumThread> listAll(Pageable pageable) {
        return threadRepo.findByStatusNot(ForumThread.ThreadStatus.DELETED, pageable);
    }

    public Page<ForumThread> listByAuthor(Long authorId, Pageable pageable) {
        return threadRepo.findByAuthorIdAndStatusNot(authorId, ForumThread.ThreadStatus.DELETED, pageable);
    }

    public Optional<ForumThread> findById(Long id) {
        return threadRepo.findById(id)
                .filter(t -> t.getStatus() != ForumThread.ThreadStatus.DELETED);
    }

    public List<ForumThread> findLinked(String refType, Long refId) {
        return threadRepo.findByLinkedRefTypeAndLinkedRefIdAndStatusNot(refType, refId, ForumThread.ThreadStatus.DELETED);
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
        categoryRepo.findById(req.getCategoryId()).ifPresent(c -> {
            c.setThreadCount(c.getThreadCount() + 1);
            categoryRepo.save(c);
        });

        return saved;
    }

    @Transactional
    public Optional<ForumThread> update(Long id, ThreadRequest req, Long userId) {
        return threadRepo.findById(id)
                .filter(t -> t.getAuthorId().equals(userId))
                .map(t -> {
                    t.setTitle(req.getTitle());
                    t.setContentMarkdown(req.getContentMarkdown());
                    t.setTags(req.getTags());
                    return threadRepo.save(t);
                });
    }

    @Transactional
    public boolean delete(Long id, Long userId, boolean isAdmin) {
        return threadRepo.findById(id)
                .filter(t -> isAdmin || t.getAuthorId().equals(userId))
                .map(t -> {
                    t.setStatus(ForumThread.ThreadStatus.DELETED);
                    threadRepo.save(t);
                    // 减少板块计数
                    categoryRepo.findById(t.getCategoryId()).ifPresent(c -> {
                        c.setThreadCount(Math.max(0, c.getThreadCount() - 1));
                        categoryRepo.save(c);
                    });
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        threadRepo.findById(id).ifPresent(t -> {
            t.setViewCount(t.getViewCount() + 1);
            threadRepo.save(t);
        });
    }
}
