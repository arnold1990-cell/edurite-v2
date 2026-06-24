package com.edurite.subscription.repository;

import com.edurite.subscription.entity.SubscriptionRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named SubscriptionRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SubscriptionRepository extends JpaRepository<SubscriptionRecord, UUID> {
    Optional<SubscriptionRecord> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SubscriptionRecord> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

