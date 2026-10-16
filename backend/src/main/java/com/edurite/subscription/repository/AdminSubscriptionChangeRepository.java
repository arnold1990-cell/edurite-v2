package com.edurite.subscription.repository;

import com.edurite.subscription.entity.AdminSubscriptionChange;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSubscriptionChangeRepository extends JpaRepository<AdminSubscriptionChange, UUID> {
    Page<AdminSubscriptionChange> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
