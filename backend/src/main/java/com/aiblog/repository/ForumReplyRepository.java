package com.aiblog.repository;

import com.aiblog.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long>, JpaSpecificationExecutor<ForumReply> {
    Page<ForumReply> findByThreadIdAndStatusOrderByFloorNumberAsc(Long threadId, ForumReply.ReplyStatus status, Pageable pageable);
    Page<ForumReply> findByAuthorIdAndStatusIn(Long authorId, Collection<ForumReply.ReplyStatus> statuses, Pageable pageable);
    int countByThreadIdAndStatus(Long threadId, ForumReply.ReplyStatus status);
}
