package com.aiblog.controller.admin;

import com.aiblog.dto.AdminForumActionRequest;
import com.aiblog.dto.AdminForumBatchRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumReply;
import com.aiblog.service.ForumReplyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/forum/replies")
public class AdminForumReplyController {

    private final ForumReplyService replyService;

    public AdminForumReplyController(ForumReplyService replyService) {
        this.replyService = replyService;
    }

    @GetMapping
    public Page<ForumReply> list(
            @RequestParam(required = false) Long threadId,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) ForumReply.ReplyStatus status,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return replyService.adminSearch(threadId, author, authorId, status, reported, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumReply> get(@PathVariable Long id) {
        return replyService.adminFindById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/operation-logs")
    public List<AdminOperationLog> operationLogs(@PathVariable Long id) {
        return replyService.adminOperationLogs(id);
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ForumReply> hide(@PathVariable Long id,
                                           @RequestBody(required = false) AdminForumActionRequest body,
                                           Authentication auth) {
        return replyService.hide(id, operator(auth), body == null ? null : body.getReason())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ForumReply> restore(@PathVariable Long id,
                                              @RequestBody(required = false) AdminForumActionRequest body,
                                              Authentication auth) {
        return replyService.restore(id, operator(auth), body == null ? null : body.getReason())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestBody(required = false) AdminForumActionRequest body,
                                       Authentication auth) {
        return replyService.adminDelete(id, operator(auth), body == null ? null : body.getReason())
                .map(r -> ResponseEntity.noContent().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch-hide")
    public Map<String, Integer> batchHide(@RequestBody AdminForumBatchRequest body, Authentication auth) {
        int affected = replyService.batchHide(body == null ? null : body.getIds(), operator(auth), body == null ? null : body.getReason());
        return Map.of("affected", affected);
    }

    @PostMapping("/batch-delete")
    public Map<String, Integer> batchDelete(@RequestBody AdminForumBatchRequest body, Authentication auth) {
        int affected = replyService.batchDelete(body == null ? null : body.getIds(), operator(auth), body == null ? null : body.getReason());
        return Map.of("affected", affected);
    }

    private String operator(Authentication auth) {
        return auth == null ? "unknown" : auth.getName();
    }
}
