package com.edurite.scholarship.repository;

import com.edurite.scholarship.entity.ScholarshipApplication;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScholarshipApplicationRepository extends JpaRepository<ScholarshipApplication, UUID> {
    List<ScholarshipApplication> findByStudentIdOrderByApplicationDeadlineAscCreatedAtDesc(UUID studentId);
    List<ScholarshipApplication> findByStudentIdAndApplicationDeadlineBetweenOrderByApplicationDeadlineAsc(UUID studentId, LocalDate from, LocalDate to);
    Optional<ScholarshipApplication> findByIdAndStudentId(UUID id, UUID studentId);
    long countByStudentId(UUID studentId);
    long countByStudentIdAndStatusIn(UUID studentId, List<String> statuses);
}

