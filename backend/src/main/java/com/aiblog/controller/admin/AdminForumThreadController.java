package com.aiblog.controller.admin;

import com.aiblog.dto.AdminForumActionRequest;
import com.aiblog.dto.AdminForumBatchRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumThread;
import com.aiblog.service.ForumThreadService;
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
@RequestMapping("/api/admin/forum/posts")
public class AdminForumThreadController {

    private final ForumThreadService threadService;

    public AdminForumThreadController(ForumThreadService threadService) {
        this.threadService = threadService;
    }

    @GetMapping
    public Page<ForumThread> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) ForumThread.ThreadStatus status,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> result = threadService.adminSearch(q, author, authorId, status, reported, createdFrom, createdTo, pageable);
        result.getContent().forEach(t -> t.setContentMarkdown(null));
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumThread> get(@PathVariable Long id) {
        return threadService.adminFindById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/operation-logs")
    public List<AdminOperationLog> operationLogs(@PathVariable Long id) {
        return threadService.adminOperationLogs(id);
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ForumThread> hide(@PathVariable Long id,
                                            @RequestBody(required = false) AdminForumActionRequest body,
                                            Authentication auth) {
        return threadService.hide(id, operator(auth), body == null ? null : body.getReason())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ForumThread> restore(@PathVariable Long id,
                                               @RequestBody(required = false) AdminForumActionRequest body,
                                               Authentication auth) {
        return threadService.restore(id, operator(auth), body == null ? null : body.getReason())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestBody(required = false) AdminForumActionRequest body,
                                       Authentication auth) {
        return threadService.adminDelete(id, operator(auth), body == null ? null : body.getReason())
                .map(t -> ResponseEntity.noContent().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch-hide")
    public Map<String, Integer> batchHide(@RequestBody AdminForumBatchRequest body, Authentication auth) {
        int affected = threadService.batchHide(body == null ? null : body.getIds(), operator(auth), body == null ? null : body.getReason());
        return Map.of("affected", affected);
    }

    @PostMapping("/batch-delete")
    public Map<String, Integer> batchDelete(@RequestBody AdminForumBatchRequest body, Authentication auth) {
        int affected = threadService.batchDelete(body == null ? null : body.getIds(), operator(auth), body == null ? null : body.getReason());
        return Map.of("affected", affected);
    }

    private String operator(Authentication auth) {
        return auth == null ? "unknown" : auth.getName();
    }
}
