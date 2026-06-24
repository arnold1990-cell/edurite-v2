package com.edurite.subscription.repository;

import com.edurite.subscription.entity.PaymentEventRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEventRecord, UUID> {
    Optional<PaymentEventRecord> findByProviderAndEventId(String provider, String eventId);
    Optional<PaymentEventRecord> findTopByPaymentIdAndProviderAndVerifiedTrueOrderByProcessedAtDesc(UUID paymentId, String provider);
}

