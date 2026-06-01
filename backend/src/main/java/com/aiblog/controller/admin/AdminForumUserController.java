package com.aiblog.controller.admin;

import com.aiblog.dto.AdminForumUserResponse;
import com.aiblog.dto.AdminUserBanRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ContentReport;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ContentReportService;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminForumUserController {

    private final ForumUserService userService;
    private final ForumThreadService threadService;
    private final ForumReplyService replyService;
    private final ContentReportService reportService;

    public AdminForumUserController(ForumUserService userService,
                                    ForumThreadService threadService,
                                    ForumReplyService replyService,
                                    ContentReportService reportService) {
        this.userService = userService;
        this.threadService = threadService;
        this.replyService = replyService;
        this.reportService = reportService;
    }

    @GetMapping
    public Page<AdminForumUserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ForumUser.Status status,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userService.adminSearch(q, status, createdFrom, createdTo, pageable)
                .map(AdminForumUserResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminForumUserResponse> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(AdminForumUserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/operation-logs")
    public List<AdminOperationLog> operationLogs(@PathVariable Long id) {
        return userService.adminOperationLogs(id);
    }

    @GetMapping("/{id}/threads")
    public Page<ForumThread> threads(@PathVariable Long id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> result = threadService.adminSearch(null, null, id, null, null, null, null, pageable);
        result.getContent().forEach(t -> t.setContentMarkdown(null));
        return result;
    }

    @GetMapping("/{id}/replies")
    public Page<ForumReply> replies(@PathVariable Long id,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return replyService.adminSearch(null, null, id, null, null, null, null, pageable);
    }

    @GetMapping("/{id}/reports")
    public Page<ContentReport> reports(@PathVariable Long id,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportService.submittedByUser(id, pageable);
    }

    @GetMapping("/{id}/reported")
    public Page<ContentReport> reported(@PathVariable Long id,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportService.receivedByUser(id, pageable);
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<AdminForumUserResponse> ban(@PathVariable Long id,
                                                      @RequestBody(required = false) AdminUserBanRequest body,
                                                      Authentication auth) {
        String reason = body == null ? null : body.getReason();
        Instant banEndTime = body == null ? null : body.getBanEndTime();
        return userService.ban(id, reason, banEndTime, operator(auth))
                .map(AdminForumUserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<AdminForumUserResponse> unban(@PathVariable Long id, Authentication auth) {
        return userService.unban(id, operator(auth))
                .map(AdminForumUserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String operator(Authentication auth) {
        return auth == null ? "unknown" : auth.getName();
    }
}
