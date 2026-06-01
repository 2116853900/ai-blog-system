package com.aiblog.controller.admin;

import com.aiblog.entity.Comment;
import com.aiblog.service.AdminCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentService commentService;

    public AdminCommentController(AdminCommentService commentService) {
        this.commentService = commentService;
    }

    /** 全部评论，可按 pending 和 status 过滤 */
    @GetMapping
    public List<Comment> list(@RequestParam(required = false) Boolean pending,
                              @RequestParam(required = false) Comment.CommentStatus status) {
        return commentService.list(pending, status);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Comment> approve(@PathVariable Long id, Authentication auth) {
        return commentService.approve(id, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<Comment> hide(@PathVariable Long id, Authentication auth) {
        return commentService.hide(id, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Comment> restore(@PathVariable Long id, Authentication auth) {
        return commentService.restore(id, operator(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        return commentService.softDelete(id, operator(auth))
                .map(c -> ResponseEntity.noContent().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    private String operator(Authentication auth) {
        return auth == null ? "unknown" : auth.getName();
    }
}
