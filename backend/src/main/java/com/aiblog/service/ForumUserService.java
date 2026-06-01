package com.aiblog.service;

import com.aiblog.dto.ProfileUpdateRequest;
import com.aiblog.dto.RegisterRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.ForumUser;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ForumUserService {

    private static final String TARGET_TYPE = "FORUM_USER";

    private final ForumUserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AdminOperationLogRepository operationLogRepo;

    public ForumUserService(ForumUserRepository userRepo,
                            PasswordEncoder encoder,
                            AdminOperationLogRepository operationLogRepo) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.operationLogRepo = operationLogRepo;
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
        return opt.map(this::restoreIfBanExpired)
                  .filter(u -> u.getStatus() == ForumUser.Status.ACTIVE)
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

    public Page<ForumUser> adminSearch(String q,
                                       ForumUser.Status status,
                                       Instant createdFrom,
                                       Instant createdTo,
                                       Pageable pageable) {
        Specification<ForumUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("nickname"), "")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return userRepo.findAll(spec, pageable);
    }

    public List<AdminOperationLog> adminOperationLogs(Long id) {
        return operationLogRepo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TARGET_TYPE, id);
    }

    public boolean isActiveForumUser(Long userId) {
        if (userId == null) return false;
        return userRepo.findById(userId)
                .map(this::restoreIfBanExpired)
                .map(user -> user.getStatus() == ForumUser.Status.ACTIVE)
                .orElse(false);
    }

    @Transactional
    public Optional<ForumUser> ban(Long id, String reason, Instant banEndTime, String operatorUsername) {
        return userRepo.findById(id).map(user -> {
            user.setStatus(ForumUser.Status.BANNED);
            user.setBanReason(clean(reason, null));
            user.setBanStartTime(Instant.now());
            user.setBanEndTime(banEndTime);
            user.setBanOperatorUsername(operatorUsername);
            ForumUser saved = userRepo.save(user);
            recordOperation(operatorUsername, "BAN_FORUM_USER", id, saved.getBanReason());
            return saved;
        });
    }

    @Transactional
    public Optional<ForumUser> unban(Long id, String operatorUsername) {
        return userRepo.findById(id).map(user -> {
            user.setStatus(ForumUser.Status.ACTIVE);
            user.setBanReason(null);
            user.setBanStartTime(null);
            user.setBanEndTime(null);
            user.setBanOperatorUsername(null);
            ForumUser saved = userRepo.save(user);
            recordOperation(operatorUsername, "UNBAN_FORUM_USER", id, null);
            return saved;
        });
    }

    public ForumUser updateProfile(ForumUser user, ProfileUpdateRequest req) {
        user.setNickname(clean(req.getNickname(), user.getNickname()));
        user.setAvatarUrl(clean(req.getAvatarUrl(), user.getAvatarUrl()));
        user.setBio(clean(req.getBio(), user.getBio()));
        return userRepo.save(user);
    }

    public ForumUser save(ForumUser user) {
        return userRepo.save(user);
    }

    private String clean(String value, String currentValue) {
        if (value == null) return currentValue;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recordOperation(String operatorUsername, String action, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
        log.setAction(action);
        log.setTargetType(TARGET_TYPE);
        log.setTargetId(targetId);
        log.setDetail(detail == null || detail.length() <= 1000 ? detail : detail.substring(0, 1000));
        operationLogRepo.save(log);
    }

    private ForumUser restoreIfBanExpired(ForumUser user) {
        if (user.getStatus() == ForumUser.Status.BANNED
                && user.getBanEndTime() != null
                && !user.getBanEndTime().isAfter(Instant.now())) {
            user.setStatus(ForumUser.Status.ACTIVE);
            user.setBanReason(null);
            user.setBanStartTime(null);
            user.setBanEndTime(null);
            user.setBanOperatorUsername(null);
            ForumUser saved = userRepo.save(user);
            recordOperation("system", "AUTO_UNBAN_FORUM_USER", user.getId(), "Ban expired.");
            return saved;
        }
        return user;
    }
}
