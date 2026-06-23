package com.aiblog.repository;

import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumThreadSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ForumThreadSubscriptionRepository extends JpaRepository<ForumThreadSubscription, Long> {
    boolean existsByThreadIdAndUserId(Long threadId, Long userId);
    Optional<ForumThreadSubscription> findByThreadIdAndUserId(Long threadId, Long userId);
    long countByThreadId(Long threadId);
    long countByUserId(Long userId);

    @Modifying
    @Query(value = """
            insert ignore into forum_thread_subscription (thread_id, user_id, created_at, last_read_at)
            values (:threadId, :userId, current_timestamp(6), current_timestamp(6))
            """, nativeQuery = true)
    int insertIgnore(@Param("threadId") Long threadId, @Param("userId") Long userId);

    @Modifying
    @Query("delete from ForumThreadSubscription s where s.threadId = :threadId and s.userId = :userId")
    int deleteByThreadIdAndUserId(@Param("threadId") Long threadId, @Param("userId") Long userId);

    @Query("select s.userId from ForumThreadSubscription s where s.threadId = :threadId")
    List<Long> findSubscriberUserIdsByThreadId(@Param("threadId") Long threadId);

    @Query("""
            select count(s) from ForumThreadSubscription s, ForumThread t
            where s.threadId = t.id
              and t.authorId = :authorId
            """)
    long countReceivedSubscriptionsByAuthorId(@Param("authorId") Long authorId);

    @Query("""
            select count(s) from ForumThreadSubscription s, ForumThread t
            where s.threadId = t.id
              and s.userId = :userId
              and t.status in :visibleStatuses
              and t.lastReplyAt is not null
              and (s.lastReadAt is null or t.lastReplyAt > s.lastReadAt)
              and (t.lastReplyUserId is null or t.lastReplyUserId <> :userId)
            """)
    long countUnreadSubscribedThreads(
            @Param("userId") Long userId,
            @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses);

    @Query(value = """
            select s from ForumThreadSubscription s, ForumThread t
            where s.threadId = t.id
              and s.userId = :userId
              and t.status in :visibleStatuses
              and (
                :unreadOnly = false
                or (
                  t.lastReplyAt is not null
                  and (s.lastReadAt is null or t.lastReplyAt > s.lastReadAt)
                  and (t.lastReplyUserId is null or t.lastReplyUserId <> :userId)
                )
              )
            order by
              case
                when t.lastReplyAt is not null
                  and (s.lastReadAt is null or t.lastReplyAt > s.lastReadAt)
                  and (t.lastReplyUserId is null or t.lastReplyUserId <> :userId)
                then 0 else 1
              end,
              t.lastReplyAt desc,
              s.createdAt desc
            """, countQuery = """
            select count(s) from ForumThreadSubscription s, ForumThread t
            where s.threadId = t.id
              and s.userId = :userId
              and t.status in :visibleStatuses
              and (
                :unreadOnly = false
                or (
                  t.lastReplyAt is not null
                  and (s.lastReadAt is null or t.lastReplyAt > s.lastReadAt)
                  and (t.lastReplyUserId is null or t.lastReplyUserId <> :userId)
                )
              )
            """)
    Page<ForumThreadSubscription> findSubscriptionsByUserId(
            @Param("userId") Long userId,
            @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    @Modifying
    @Query("""
            update ForumThreadSubscription s
            set s.lastReadAt = :readAt
            where s.threadId = :threadId and s.userId = :userId
            """)
    int markRead(@Param("threadId") Long threadId, @Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Query(value = """
            select t from ForumThread t, ForumThreadSubscription s
            where s.threadId = t.id
              and s.userId = :userId
              and t.status in :visibleStatuses
            order by s.createdAt desc
            """, countQuery = """
            select count(t) from ForumThread t, ForumThreadSubscription s
            where s.threadId = t.id
              and s.userId = :userId
              and t.status in :visibleStatuses
            """)
    Page<ForumThread> findSubscribedThreadsByUserId(
            @Param("userId") Long userId,
            @Param("visibleStatuses") Collection<ForumThread.ThreadStatus> visibleStatuses,
            Pageable pageable);
}
