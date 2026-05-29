package com.aiblog.controller;

import com.aiblog.dto.LoginRequest;
import com.aiblog.dto.LoginResponse;
import com.aiblog.repository.AdminUserRepository;
import com.aiblog.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminUserRepository adminRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(AdminUserRepository adminRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.adminRepo = adminRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return adminRepo.findByUsername(req.getUsername())
                .filter(u -> encoder.matches(req.getPassword(), u.getPasswordHash()))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(
                        new LoginResponse(jwtUtil.generateToken(u.getUsername()), u.getUsername())))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误")));
    }
}
