package com.edurite.subscription.repository;

import com.edurite.subscription.entity.PaymentRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named PaymentRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface PaymentRepository extends JpaRepository<PaymentRecord, UUID> {
    List<PaymentRecord> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
    Optional<PaymentRecord> findTopByReferenceOrderByCreatedAtDesc(String reference);
    Optional<PaymentRecord> findTopByProviderAndProviderOrderIdOrderByCreatedAtDesc(String provider, String providerOrderId);
    Optional<PaymentRecord> findTopByProviderAndProviderSessionIdOrderByCreatedAtDesc(String provider, String providerSessionId);
    Optional<PaymentRecord> findTopByProviderAndProviderPaymentIdOrderByCreatedAtDesc(String provider, String providerPaymentId);
    List<PaymentRecord> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

