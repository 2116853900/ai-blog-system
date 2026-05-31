package com.aiblog.controller;

import com.aiblog.dto.ReplyRequest;
import com.aiblog.entity.ForumReply;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class ForumReplyController {

    private final ForumReplyService replyService;
    private final ForumUserService userService;

    public ForumReplyController(ForumReplyService replyService, ForumUserService userService) {
        this.replyService = replyService;
        this.userService = userService;
    }

    /** 获取帖子的回复列表（分页） */
    @GetMapping("/threads/{threadId}/replies")
    public Page<ForumReply> list(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return replyService.listByThread(threadId, PageRequest.of(page, size));
    }

    /** 发表回复 */
    @PostMapping("/threads/{threadId}/replies")
    public ResponseEntity<?> create(
            @PathVariable Long threadId,
            @Valid @RequestBody ReplyRequest req,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        try {
            ForumReply reply = replyService.create(threadId, req, userId);
            return ResponseEntity.ok(reply);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 编辑回复 */
    @PutMapping("/replies/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ReplyRequest req, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        return replyService.update(id, req, userId)
                .map(r -> ResponseEntity.ok((Object) r))
                .orElse(ResponseEntity.status(403).body(Map.of("message", "无权编辑此回复")));
    }

    /** 删除回复 */
    @DeleteMapping("/replies/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));
        if (replyService.delete(id, userId, isAdmin)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(403).body(Map.of("message", "无权删除此回复"));
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        String username = auth.getName();
        return userService.findByUsername(username).map(u -> u.getId()).orElse(null);
    }
}
