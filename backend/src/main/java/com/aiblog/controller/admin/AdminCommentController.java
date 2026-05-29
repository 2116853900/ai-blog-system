package com.aiblog.controller.admin;

import com.aiblog.entity.Comment;
import com.aiblog.repository.CommentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentRepository repo;

    public AdminCommentController(CommentRepository repo) {
        this.repo = repo;
    }

    /** 全部评论，可按 pending 过滤 */
    @GetMapping
    public List<Comment> list(@RequestParam(required = false) Boolean pending) {
        if (Boolean.TRUE.equals(pending)) {
            return repo.findByApprovedFalseOrderByCreatedAtDesc();
        }
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Comment> approve(@PathVariable Long id) {
        return repo.findById(id).map(c -> {
            c.setApproved(true);
            return ResponseEntity.ok(repo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
