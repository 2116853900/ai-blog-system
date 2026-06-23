package com.aiblog.controller;

import com.aiblog.dto.ResourceFavoriteItemResponse;
import com.aiblog.entity.ForumUser;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.service.ForumUserService;
import com.aiblog.service.ResourceFavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class ResourceFavoriteController {

    private final ResourceFavoriteService favoriteService;
    private final ForumUserService userService;

    public ResourceFavoriteController(ResourceFavoriteService favoriteService, ForumUserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    @GetMapping("/api/resource-favorites/{refType}/{refId}")
    public ResponseEntity<?> interaction(@PathVariable ResourceFavorite.RefType refType,
                                         @PathVariable Long refId,
                                         Authentication auth) {
        try {
            return ResponseEntity.ok(favoriteService.getInteraction(refType, refId, resolveUserId(auth)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/account/resource-favorites")
    public Page<ResourceFavoriteItemResponse> list(Authentication auth,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Long userId = requireForumUserId(auth);
        return favoriteService.listFavorites(userId, PageRequest.of(normalizePage(page), normalizeSize(size)));
    }

    @PostMapping("/api/account/resource-favorites/{refType}/{refId}")
    public ResponseEntity<?> favorite(@PathVariable ResourceFavorite.RefType refType,
                                      @PathVariable Long refId,
                                      Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return ResponseEntity.ok(favoriteService.favorite(refType, refId, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/account/resource-favorites/{refType}/{refId}")
    public ResponseEntity<?> unfavorite(@PathVariable ResourceFavorite.RefType refType,
                                        @PathVariable Long refId,
                                        Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return ResponseEntity.ok(favoriteService.unfavorite(refType, refId, userId));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可使用资源收藏"));
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.min(50, Math.max(1, size));
    }
}
