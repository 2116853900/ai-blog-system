package com.aiblog.repository;

import com.aiblog.entity.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ForumUserRepository extends JpaRepository<ForumUser, Long>, JpaSpecificationExecutor<ForumUser> {
    Optional<ForumUser> findByUsername(String username);
    Optional<ForumUser> findByEmail(String email);
    List<ForumUser> findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String username, String nickname);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByStatus(ForumUser.Status status);
}
