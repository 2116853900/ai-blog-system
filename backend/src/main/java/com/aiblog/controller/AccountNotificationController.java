package com.aiblog.controller;

import com.aiblog.dto.UserNotificationResponse;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumUserService;
import com.aiblog.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/account/notifications")
public class AccountNotificationController {

    private final ForumUserService userService;
    private final NotificationService notificationService;

    public AccountNotificationController(ForumUserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<UserNotificationResponse> list(Authentication auth,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return notificationService.list(userId, PageRequest.of(normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        Long userId = requireForumUserId(auth);
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public UserNotificationResponse markRead(@PathVariable Long id, Authentication auth) {
        Long userId = requireForumUserId(auth);
        return notificationService.markRead(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "通知不存在"));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(Authentication auth) {
        Long userId = requireForumUserId(auth);
        return Map.of("affected", notificationService.markAllRead(userId));
    }

    private Long requireForumUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可查看通知"));
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.min(50, Math.max(1, size));
    }
}
