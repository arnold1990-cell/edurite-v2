package com.edurite.auth.repository;

import com.edurite.auth.entity.SchoolPasswordResetOtp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolPasswordResetOtpRepository extends JpaRepository<SchoolPasswordResetOtp, UUID> {
    List<SchoolPasswordResetOtp> findBySchoolIdAndUserIdAndUsedFalseOrderByCreatedAtDesc(UUID schoolId, UUID userId);
    Optional<SchoolPasswordResetOtp> findFirstBySchoolIdAndUserIdAndUsedFalseOrderByCreatedAtDesc(UUID schoolId, UUID userId);
}
