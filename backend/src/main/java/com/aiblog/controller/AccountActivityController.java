package com.aiblog.controller;

import com.aiblog.dto.ForumSubscriptionSummaryResponse;
import com.aiblog.dto.ForumThreadSubscriptionItemResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumInteractionService;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountActivityController {

    private final ForumUserService userService;
    private final ForumThreadService threadService;
    private final ForumReplyService replyService;
    private final ForumInteractionService interactionService;

    public AccountActivityController(ForumUserService userService,
                                     ForumThreadService threadService,
                                     ForumReplyService replyService,
                                     ForumInteractionService interactionService) {
        this.userService = userService;
        this.threadService = threadService;
        this.replyService = replyService;
        this.interactionService = interactionService;
    }

    @GetMapping("/threads")
    public Page<ForumThread> threads(Authentication auth,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return threadService.listByAuthor(userId, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/replies")
    public Page<ForumReply> replies(Authentication auth,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return replyService.listByAuthor(userId, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/favorites")
    public Page<ForumThread> favorites(Authentication auth,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return interactionService.listFavoriteThreads(userId, PageRequest.of(normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/subscriptions")
    public Page<ForumThreadSubscriptionItemResponse> subscriptions(Authentication auth,
                                                                   @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return interactionService.listSubscriptionItems(userId, unreadOnly, PageRequest.of(normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/subscription-summary")
    public ForumSubscriptionSummaryResponse subscriptionSummary(Authentication auth) {
        Long userId = requireForumUserId(auth);
        return interactionService.subscriptionSummary(userId);
    }

    private Long requireForumUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可查看账号动态"));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(size), sort);
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.min(50, Math.max(1, size));
    }
}
