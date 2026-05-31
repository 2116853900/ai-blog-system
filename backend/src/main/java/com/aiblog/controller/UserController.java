package com.aiblog.controller;

import com.aiblog.dto.UserProfileResponse;
import com.aiblog.service.ForumUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ForumUserService userService;

    public UserController(ForumUserService userService) {
        this.userService = userService;
    }

    /** 用户公开资料 */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long id) {
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(UserProfileResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }
}
