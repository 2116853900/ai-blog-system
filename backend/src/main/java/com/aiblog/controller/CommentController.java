package com.aiblog.controller;

import com.aiblog.dto.CommentRequest;
import com.aiblog.entity.Comment;
import com.aiblog.repository.CommentRepository;
import com.aiblog.service.ForumUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentRepository repo;
    private final ForumUserService userService;

    public CommentController(CommentRepository repo, ForumUserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    /** 获取某条内容下已审核通过的评论 */
    @GetMapping
    public List<Comment> list(@RequestParam Comment.RefType type, @RequestParam Long refId) {
        return repo.findByRefTypeAndRefIdAndApprovedTrueAndStatusOrderByCreatedAtDesc(
                type, refId, Comment.CommentStatus.NORMAL);
    }

    /** 访客提交评论（进入待审核） */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CommentRequest req, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId != null && !userService.isActiveForumUser(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "账号已被封禁，暂不能评论"));
        }
        Comment c = new Comment();
        c.setRefType(req.getRefType());
        c.setRefId(req.getRefId());
        c.setAuthor(req.getAuthor());
        c.setContent(req.getContent());
        c.setApproved(false);
        repo.save(c);
        return ResponseEntity.ok(Map.of("message", "评论已提交，将在审核后显示"));
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
    }
}
