package com.aiblog.controller;

import com.aiblog.dto.ThreadRequest;
import com.aiblog.entity.ForumThread;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/threads")
public class ForumThreadController {

    private final ForumThreadService threadService;
    private final ForumUserService userService;

    public ForumThreadController(ForumThreadService threadService, ForumUserService userService) {
        this.threadService = threadService;
        this.userService = userService;
    }

    /** 帖子列表（分页，可按板块筛选） */
    @GetMapping
    public Page<ForumThread> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        if (categoryId != null) {
            return threadService.listByCategory(categoryId, pageable);
        }
        return threadService.listAll(pageable);
    }

    /** 帖子详情 */
    @GetMapping("/{id}")
    public ResponseEntity<ForumThread> get(@PathVariable Long id) {
        return threadService.findById(id)
                .map(t -> {
                    threadService.incrementViewCount(id);
                    return ResponseEntity.ok(t);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** 关联帖子（博客内容关联的讨论） */
    @GetMapping("/linked")
    public List<ForumThread> linked(@RequestParam String refType, @RequestParam Long refId) {
        return threadService.findLinked(refType, refId);
    }

    /** 发帖 */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ThreadRequest req, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        ForumThread thread = threadService.create(req, userId);
        return ResponseEntity.ok(thread);
    }

    /** 编辑帖子 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ThreadRequest req, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        return threadService.update(id, req, userId)
                .map(t -> ResponseEntity.ok((Object) t))
                .orElse(ResponseEntity.status(403).body(Map.of("message", "无权编辑此帖子")));
    }

    /** 删除帖子 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));
        if (threadService.delete(id, userId, isAdmin)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(403).body(Map.of("message", "无权删除此帖子"));
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        String username = auth.getName();
        return userService.findByUsername(username).map(u -> u.getId()).orElse(null);
    }
}
