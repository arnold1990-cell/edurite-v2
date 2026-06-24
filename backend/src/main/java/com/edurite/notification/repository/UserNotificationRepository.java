package com.edurite.notification.repository;

import com.edurite.notification.entity.UserNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    Page<UserNotification> findByUserId(UUID userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(UUID userId);
    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);
    List<UserNotification> findByUserIdAndIsReadFalse(UUID userId);
    long countByUserId(UUID userId);
    long countByNotificationId(UUID notificationId);
    void deleteByIdAndUserId(UUID id, UUID userId);
    void deleteByNotificationId(UUID notificationId);
}

