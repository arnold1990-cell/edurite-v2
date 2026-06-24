package com.edurite.application.repository;

import com.edurite.application.entity.ApplicationRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named ApplicationRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface ApplicationRepository extends JpaRepository<ApplicationRecord, UUID> {

    long countByStudentId(UUID studentId);

    long countByStudentIdAndStatus(UUID studentId, String status);

    List<ApplicationRecord> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    long countByBursaryId(UUID bursaryId);
    List<ApplicationRecord> findByBursaryIdIn(List<UUID> bursaryIds);
    List<ApplicationRecord> findTop10ByOrderByCreatedAtDesc();
}

