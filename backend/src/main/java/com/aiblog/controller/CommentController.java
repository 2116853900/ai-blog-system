package com.aiblog.controller;

import com.aiblog.dto.CommentRequest;
import com.aiblog.entity.Comment;
import com.aiblog.repository.CommentRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentRepository repo;

    public CommentController(CommentRepository repo) {
        this.repo = repo;
    }

    /** 获取某条内容下已审核通过的评论 */
    @GetMapping
    public List<Comment> list(@RequestParam Comment.RefType type, @RequestParam Long refId) {
        return repo.findByRefTypeAndRefIdAndApprovedTrueOrderByCreatedAtDesc(type, refId);
    }

    /** 访客提交评论（进入待审核） */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CommentRequest req) {
        Comment c = new Comment();
        c.setRefType(req.getRefType());
        c.setRefId(req.getRefId());
        c.setAuthor(req.getAuthor());
        c.setContent(req.getContent());
        c.setApproved(false);
        repo.save(c);
        return ResponseEntity.ok(Map.of("message", "评论已提交，将在审核后显示"));
    }
}
