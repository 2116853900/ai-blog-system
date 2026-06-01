package com.aiblog.controller;

import com.aiblog.dto.LoginRequest;
import com.aiblog.dto.LoginResponse;
import com.aiblog.dto.ProfileUpdateRequest;
import com.aiblog.dto.RegisterRequest;
import com.aiblog.dto.UserProfileResponse;
import com.aiblog.entity.ForumUser;
import com.aiblog.repository.AdminUserRepository;
import com.aiblog.security.JwtUtil;
import com.aiblog.service.ForumUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminUserRepository adminRepo;
    private final ForumUserService userService;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(AdminUserRepository adminRepo, ForumUserService userService,
                          PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.adminRepo = adminRepo;
        this.userService = userService;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    /** 统一登录：先查 ForumUser，再 fallback 到 AdminUser */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // 1. 尝试论坛用户登录
        var forumUser = userService.authenticate(req.getUsername(), req.getPassword());
        if (forumUser.isPresent()) {
            ForumUser u = forumUser.get();
            userService.updateLastLogin(u);
            String token = jwtUtil.generateToken(u.getId(), u.getUsername(), u.getRole().name());
            return ResponseEntity.ok(new LoginResponse(token, u.getUsername(),
                    u.getRole().name(), u.getId(), u.getNickname()));
        }

        // 2. Fallback: 管理员登录（向后兼容）
        return adminRepo.findByUsername(req.getUsername())
                .filter(u -> encoder.matches(req.getPassword(), u.getPasswordHash()))
                .<ResponseEntity<?>>map(u -> {
                    String token = jwtUtil.generateToken(null, u.getUsername(), "ADMIN");
                    return ResponseEntity.ok(new LoginResponse(token, u.getUsername(), "ADMIN", null, u.getUsername()));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误")));
    }

    /** 用户注册 */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已被占用"));
        }
        if (userService.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "邮箱已被注册"));
        }

        ForumUser user = userService.register(req);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "userId", user.getId(),
                "nickname", user.getNickname(),
                "message", "注册成功"
        ));
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        String username = auth.getName();
        // 先查论坛用户
        var forumUser = userService.findByUsername(username);
        if (forumUser.isPresent()) {
            return ResponseEntity.ok(UserProfileResponse.from(forumUser.get()));
        }
        // Fallback: 管理员
        return adminRepo.findByUsername(username)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
                        "username", u.getUsername(),
                        "role", "ADMIN"
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "用户不存在")));
    }

    /** 更新论坛用户资料 */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth, @Valid @RequestBody ProfileUpdateRequest req) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        return userService.findByUsername(auth.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(UserProfileResponse.from(userService.updateProfile(u, req))))
                .orElse(ResponseEntity.badRequest().body(Map.of("message", "仅论坛用户可编辑资料")));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "新密码至少8位"));
        }

        String username = auth.getName();
        var forumUser = userService.findByUsername(username);
        if (forumUser.isPresent()) {
            ForumUser u = forumUser.get();
            if (!encoder.matches(oldPassword, u.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("message", "原密码错误"));
            }
            u.setPasswordHash(encoder.encode(newPassword));
            userService.save(u);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
    }
}
