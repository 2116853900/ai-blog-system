package com.aiblog.repository;

import com.aiblog.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    Page<ForumReply> findByThreadIdAndStatusOrderByFloorNumberAsc(Long threadId, ForumReply.ReplyStatus status, Pageable pageable);
    Page<ForumReply> findByAuthorIdAndStatusNot(Long authorId, ForumReply.ReplyStatus status, Pageable pageable);
    int countByThreadIdAndStatus(Long threadId, ForumReply.ReplyStatus status);
}
