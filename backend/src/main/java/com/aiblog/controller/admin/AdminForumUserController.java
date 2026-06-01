package com.aiblog.controller.admin;

import com.aiblog.dto.AdminForumUserResponse;
import com.aiblog.dto.AdminUserBanRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumUser;
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

    public AdminForumUserController(ForumUserService userService) {
        this.userService = userService;
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
