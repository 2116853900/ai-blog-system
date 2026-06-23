package com.aiblog.controller;

import com.aiblog.dto.AccountResourceReviewItemResponse;
import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.entity.ForumUser;
import com.aiblog.entity.ResourceReview;
import com.aiblog.service.ForumUserService;
import com.aiblog.service.ResourceReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class ResourceReviewController {

    private final ResourceReviewService reviewService;
    private final ForumUserService userService;

    public ResourceReviewController(ResourceReviewService reviewService, ForumUserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/api/resource-reviews/{refType}/{refId}/summary")
    public ResponseEntity<?> summary(@PathVariable ResourceReview.RefType refType,
                                     @PathVariable Long refId,
                                     Authentication auth) {
        try {
            return ResponseEntity.ok(reviewService.summary(refType, refId, resolveUserId(auth)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> list(@PathVariable ResourceReview.RefType refType,
                                  @PathVariable Long refId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        try {
            Page<?> reviews = reviewService.list(
                    refType,
                    refId,
                    PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/account/resource-reviews")
    public Page<AccountResourceReviewItemResponse> listMine(Authentication auth,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireForumUserId(auth);
        return reviewService.listMine(userId, PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
    }

    @PostMapping("/api/account/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> upsert(@PathVariable ResourceReview.RefType refType,
                                    @PathVariable Long refId,
                                    @Valid @RequestBody ResourceReviewRequest request,
                                    Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return ResponseEntity.ok(reviewService.upsert(refType, refId, userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/account/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> deleteOwn(@PathVariable ResourceReview.RefType refType,
                                       @PathVariable Long refId,
                                       Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return reviewService.deleteOwn(refType, refId, userId)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName()).map(ForumUser::getId).orElse(null);
    }

    private Long requireForumUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可评价资源"));
    }
}
