package com.aiblog.repository;

import com.aiblog.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadAtIsNull(Long userId);
    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);
    List<UserNotification> findByUserIdAndReadAtIsNull(Long userId);
}
