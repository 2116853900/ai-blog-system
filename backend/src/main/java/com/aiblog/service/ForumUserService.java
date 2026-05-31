package com.aiblog.service;

import com.aiblog.dto.RegisterRequest;
import com.aiblog.entity.ForumUser;
import com.aiblog.repository.ForumUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ForumUserService {

    private final ForumUserRepository userRepo;
    private final PasswordEncoder encoder;

    public ForumUserService(ForumUserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public ForumUser register(RegisterRequest req) {
        ForumUser user = new ForumUser();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setRole(ForumUser.Role.USER);
        user.setStatus(ForumUser.Status.ACTIVE);
        return userRepo.save(user);
    }

    public Optional<ForumUser> authenticate(String username, String password) {
        // 支持用户名或邮箱登录
        Optional<ForumUser> opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) {
            opt = userRepo.findByEmail(username);
        }
        return opt.filter(u -> u.getStatus() == ForumUser.Status.ACTIVE)
                  .filter(u -> encoder.matches(password, u.getPasswordHash()));
    }

    public void updateLastLogin(ForumUser user) {
        user.setLastLoginAt(Instant.now());
        userRepo.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepo.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    public Optional<ForumUser> findById(Long id) {
        return userRepo.findById(id);
    }

    public Optional<ForumUser> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public ForumUser save(ForumUser user) {
        return userRepo.save(user);
    }
}
