package com.aiblog.repository;

import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long>, JpaSpecificationExecutor<ForumReply> {
    Page<ForumReply> findByThreadIdAndStatusOrderByFloorNumberAsc(Long threadId, ForumReply.ReplyStatus status, Pageable pageable);
    Page<ForumReply> findByAuthorIdAndStatusIn(Long authorId, Collection<ForumReply.ReplyStatus> statuses, Pageable pageable);
    @Query(value = """
            select r from ForumReply r
            where r.authorId = :authorId
              and r.status in :replyStatuses
              and exists (
                  select 1 from ForumThread t
                  where t.id = r.threadId
                    and t.status in :threadStatuses
              )
            """, countQuery = """
            select count(r) from ForumReply r
            where r.authorId = :authorId
              and r.status in :replyStatuses
              and exists (
                  select 1 from ForumThread t
                  where t.id = r.threadId
                    and t.status in :threadStatuses
              )
            """)
    Page<ForumReply> findVisibleByAuthorId(
            @Param("authorId") Long authorId,
            @Param("replyStatuses") Collection<ForumReply.ReplyStatus> replyStatuses,
            @Param("threadStatuses") Collection<ForumThread.ThreadStatus> threadStatuses,
            Pageable pageable);
    int countByThreadIdAndStatus(Long threadId, ForumReply.ReplyStatus status);
}
