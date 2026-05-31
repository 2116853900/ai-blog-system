package com.aiblog.repository;

import com.aiblog.entity.ForumThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>, JpaSpecificationExecutor<ForumThread> {
    Page<ForumThread> findByCategoryIdAndStatusNot(Long categoryId, ForumThread.ThreadStatus status, Pageable pageable);
    Page<ForumThread> findByStatusNot(ForumThread.ThreadStatus status, Pageable pageable);
    Page<ForumThread> findByAuthorIdAndStatusNot(Long authorId, ForumThread.ThreadStatus status, Pageable pageable);
    List<ForumThread> findByLinkedRefTypeAndLinkedRefIdAndStatusNot(String refType, Long refId, ForumThread.ThreadStatus status);
}
