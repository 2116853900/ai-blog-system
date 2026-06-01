package com.aiblog.controller;

import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.service.ForumInteractionService;
import com.aiblog.service.ForumUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/threads/{threadId}")
public class ForumInteractionController {

    private final ForumInteractionService interactionService;
    private final ForumUserService userService;

    public ForumInteractionController(ForumInteractionService interactionService, ForumUserService userService) {
        this.interactionService = interactionService;
        this.userService = userService;
    }

    @GetMapping("/interaction")
    public ResponseEntity<?> interaction(@PathVariable Long threadId, Authentication auth) {
        try {
            return ResponseEntity.ok(interactionService.getInteraction(threadId, resolveUserId(auth)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/like")
    public ResponseEntity<?> like(@PathVariable Long threadId, Authentication auth) {
        Long userId = requireActiveUser(auth);
        if (userId == null) return unauthorized();
        try {
            ForumInteractionResponse response = interactionService.like(threadId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/like")
    public ResponseEntity<?> unlike(@PathVariable Long threadId, Authentication auth) {
        Long userId = requireActiveUser(auth);
        if (userId == null) return unauthorized();
        try {
            ForumInteractionResponse response = interactionService.unlike(threadId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/favorite")
    public ResponseEntity<?> favorite(@PathVariable Long threadId, Authentication auth) {
        Long userId = requireActiveUser(auth);
        if (userId == null) return unauthorized();
        try {
            ForumInteractionResponse response = interactionService.favorite(threadId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/favorite")
    public ResponseEntity<?> unfavorite(@PathVariable Long threadId, Authentication auth) {
        Long userId = requireActiveUser(auth);
        if (userId == null) return unauthorized();
        try {
            ForumInteractionResponse response = interactionService.unfavorite(threadId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
    }

    private Long requireActiveUser(Authentication auth) {
        return resolveUserId(auth);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
    }
}
