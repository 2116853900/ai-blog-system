package com.aiblog.service;

import com.aiblog.dto.UserNotificationResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.UserNotification;
import com.aiblog.repository.ForumThreadSubscriptionRepository;
import com.aiblog.repository.UserNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepo;
    private final ForumThreadSubscriptionRepository subscriptionRepo;

    public NotificationService(UserNotificationRepository notificationRepo,
                               ForumThreadSubscriptionRepository subscriptionRepo) {
        this.notificationRepo = notificationRepo;
        this.subscriptionRepo = subscriptionRepo;
    }

    @Transactional
    public void notifyReplyCreated(ForumThread thread, ForumReply reply) {
        List<UserNotification> notifications = new ArrayList<>();
        Set<Long> notifiedUserIds = new LinkedHashSet<>();
        Long actorId = reply.getAuthorId();
        String linkUrl = "/forum/threads/" + thread.getId();

        if (!thread.getAuthorId().equals(actorId)) {
            notifications.add(build(
                    thread.getAuthorId(),
                    actorId,
                    UserNotification.NotificationType.THREAD_REPLY,
                    "你的帖子有新回复",
                    "帖子《" + thread.getTitle() + "》收到一条新回复",
                    linkUrl
            ));
            notifiedUserIds.add(thread.getAuthorId());
        }

        Long replyToUserId = reply.getReplyToUserId();
        if (replyToUserId != null && !replyToUserId.equals(actorId) && !replyToUserId.equals(thread.getAuthorId())) {
            notifications.add(build(
                    replyToUserId,
                    actorId,
                    UserNotification.NotificationType.REPLY_REPLY,
                    "你的回复有新回应",
                    "你在《" + thread.getTitle() + "》中的回复收到新回应",
                    linkUrl
            ));
            notifiedUserIds.add(replyToUserId);
        }

        for (Long subscriberId : subscriptionRepo.findSubscriberUserIdsByThreadId(thread.getId())) {
            if (subscriberId == null || subscriberId.equals(actorId) || notifiedUserIds.contains(subscriberId)) {
                continue;
            }
            notifications.add(build(
                    subscriberId,
                    actorId,
                    UserNotification.NotificationType.THREAD_SUBSCRIPTION_REPLY,
                    "你关注的帖子有新回复",
                    "你关注的帖子《" + thread.getTitle() + "》收到一条新回复",
                    linkUrl
            ));
            notifiedUserIds.add(subscriberId);
        }

        if (!notifications.isEmpty()) {
            notificationRepo.saveAll(notifications);
        }
    }

    @Transactional(readOnly = true)
    public Page<UserNotificationResponse> list(Long userId, Pageable pageable) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public Optional<UserNotificationResponse> markRead(Long userId, Long notificationId) {
        return notificationRepo.findByIdAndUserId(notificationId, userId).map(notification -> {
            if (notification.getReadAt() == null) {
                notification.setReadAt(Instant.now());
                notificationRepo.save(notification);
            }
            return toResponse(notification);
        });
    }

    @Transactional
    public int markAllRead(Long userId) {
        List<UserNotification> unread = notificationRepo.findByUserIdAndReadAtIsNull(userId);
        Instant now = Instant.now();
        unread.forEach(notification -> notification.setReadAt(now));
        notificationRepo.saveAll(unread);
        return unread.size();
    }

    private UserNotification build(Long userId,
                                   Long actorId,
                                   UserNotification.NotificationType type,
                                   String title,
                                   String message,
                                   String linkUrl) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        return notification;
    }

    private UserNotificationResponse toResponse(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLinkUrl(),
                notification.getActorId(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
