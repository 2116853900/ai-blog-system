package com.aiblog.controller;

import com.aiblog.dto.SubmissionRequest;
import com.aiblog.entity.Submission;
import com.aiblog.repository.SubmissionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionRepository repo;

    public SubmissionController(SubmissionRepository repo) {
        this.repo = repo;
    }

    /** 访客投稿（进入待审核） */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SubmissionRequest req) {
        Submission s = new Submission();
        s.setType(req.getType());
        s.setPayloadJson(req.getPayloadJson());
        s.setContactInfo(req.getContactInfo());
        s.setStatus(Submission.Status.PENDING);
        repo.save(s);
        return ResponseEntity.ok(Map.of("message", "投稿已提交，感谢分享！我们会尽快审核"));
    }
}
