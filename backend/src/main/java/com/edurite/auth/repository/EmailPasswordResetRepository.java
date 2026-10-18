package com.edurite.auth.repository;

import com.edurite.auth.entity.EmailPasswordReset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailPasswordResetRepository extends JpaRepository<EmailPasswordReset, UUID> {
    Optional<EmailPasswordReset> findByUserId(UUID userId);
}
