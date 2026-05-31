package com.aiblog.repository;

import com.aiblog.entity.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumUserRepository extends JpaRepository<ForumUser, Long> {
    Optional<ForumUser> findByUsername(String username);
    Optional<ForumUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
