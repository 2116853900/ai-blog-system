package com.aiblog.service;

import com.aiblog.dto.UserNotificationResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.UserNotification;
import com.aiblog.repository.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long THREAD_ID = 10L;
    private static final long THREAD_AUTHOR_ID = 20L;
    private static final long REPLY_AUTHOR_ID = 30L;
    private static final long REPLY_TO_AUTHOR_ID = 40L;

    @Mock
    private UserNotificationRepository notificationRepo;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepo);
    }

    @Test
    void notifyReplyCreatedNotifiesThreadAuthorAndReplyAuthor() {
        ForumThread thread = thread(THREAD_AUTHOR_ID);
        ForumReply reply = reply(REPLY_AUTHOR_ID, REPLY_TO_AUTHOR_ID);

        service.notifyReplyCreated(thread, reply);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<UserNotification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepo).saveAll(captor.capture());
        List<UserNotification> saved = toList(captor.getValue());
        assertThat(saved)
                .extracting(UserNotification::getUserId)
                .containsExactly(THREAD_AUTHOR_ID, REPLY_TO_AUTHOR_ID);
        assertThat(saved)
                .extracting(UserNotification::getType)
                .containsExactly(UserNotification.NotificationType.THREAD_REPLY, UserNotification.NotificationType.REPLY_REPLY);
    }

    @Test
    void notifyReplyCreatedSkipsSelfNotificationsAndDuplicateRecipient() {
        ForumThread thread = thread(THREAD_AUTHOR_ID);
        ForumReply reply = reply(THREAD_AUTHOR_ID, THREAD_AUTHOR_ID);

        service.notifyReplyCreated(thread, reply);

        verify(notificationRepo, never()).saveAll(any());
    }

    @Test
    void listMapsReadStateAndUnreadCount() {
        PageRequest pageable = PageRequest.of(0, 10);
        UserNotification unread = notification(1L, null);
        UserNotification read = notification(2L, Instant.parse("2026-06-02T01:00:00Z"));
        when(notificationRepo.findByUserIdOrderByCreatedAtDesc(THREAD_AUTHOR_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(unread, read), pageable, 2));
        when(notificationRepo.countByUserIdAndReadAtIsNull(THREAD_AUTHOR_ID)).thenReturn(1L);

        Page<UserNotificationResponse> page = service.list(THREAD_AUTHOR_ID, pageable);
        long unreadCount = service.unreadCount(THREAD_AUTHOR_ID);

        assertThat(unreadCount).isEqualTo(1);
        assertThat(page.getContent())
                .extracting(UserNotificationResponse::isRead)
                .containsExactly(false, true);
    }

    @Test
    void markReadOnlyUpdatesRecipientOwnedNotification() {
        UserNotification notification = notification(1L, null);
        when(notificationRepo.findByIdAndUserId(1L, THREAD_AUTHOR_ID)).thenReturn(Optional.of(notification));
        when(notificationRepo.findByIdAndUserId(2L, THREAD_AUTHOR_ID)).thenReturn(Optional.empty());

        Optional<UserNotificationResponse> updated = service.markRead(THREAD_AUTHOR_ID, 1L);
        Optional<UserNotificationResponse> missing = service.markRead(THREAD_AUTHOR_ID, 2L);

        assertThat(updated).isPresent();
        assertThat(updated.get().isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        assertThat(missing).isEmpty();
        verify(notificationRepo).save(notification);
    }

    private ForumThread thread(Long authorId) {
        ForumThread thread = new ForumThread();
        thread.setId(THREAD_ID);
        thread.setAuthorId(authorId);
        thread.setTitle("MCP 调试经验");
        thread.setContentMarkdown("content");
        return thread;
    }

    private ForumReply reply(Long authorId, Long replyToUserId) {
        ForumReply reply = new ForumReply();
        reply.setId(99L);
        reply.setThreadId(THREAD_ID);
        reply.setAuthorId(authorId);
        reply.setReplyToUserId(replyToUserId);
        reply.setFloorNumber(2);
        reply.setContentMarkdown("reply content");
        return reply;
    }

    private UserNotification notification(Long id, Instant readAt) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setUserId(THREAD_AUTHOR_ID);
        notification.setActorId(REPLY_AUTHOR_ID);
        notification.setType(UserNotification.NotificationType.THREAD_REPLY);
        notification.setTitle("你的帖子有新回复");
        notification.setMessage("帖子《MCP 调试经验》收到一条新回复");
        notification.setLinkUrl("/forum/threads/10");
        notification.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));
        notification.setReadAt(readAt);
        return notification;
    }

    private List<UserNotification> toList(Iterable<UserNotification> notifications) {
        List<UserNotification> result = new java.util.ArrayList<>();
        notifications.forEach(result::add);
        return result;
    }
}
