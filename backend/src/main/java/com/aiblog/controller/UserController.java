package com.aiblog.controller;

import com.aiblog.dto.UserProfileResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ForumUserService userService;
    private final ForumThreadService threadService;
    private final ForumReplyService replyService;

    public UserController(ForumUserService userService,
                          ForumThreadService threadService,
                          ForumReplyService replyService) {
        this.userService = userService;
        this.threadService = threadService;
        this.replyService = replyService;
    }

    /** 用户公开资料 */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long id) {
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(UserProfileResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 用户公开帖子 */
    @GetMapping("/{id}/threads")
    public ResponseEntity<Page<ForumThread>> threads(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(threadService.listByAuthor(id, pageRequest(page, size)));
    }

    /** 用户公开回复 */
    @GetMapping("/{id}/replies")
    public ResponseEntity<Page<ForumReply>> replies(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        if (userService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(replyService.listVisibleByAuthor(id, pageRequest(page, size)));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(0, page),
                Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
