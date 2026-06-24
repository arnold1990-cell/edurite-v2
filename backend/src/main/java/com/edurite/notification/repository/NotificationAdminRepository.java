package com.edurite.notification.repository;

import com.edurite.notification.entity.Notification;
import com.edurite.notification.entity.NotificationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAdminRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
}

