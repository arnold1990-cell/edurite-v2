package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricAssessmentRepository extends JpaRepository<PsychometricAssessment, UUID> {
    List<PsychometricAssessment> findByActiveTrueOrderByCreatedAtAsc();

    Optional<PsychometricAssessment> findByIdAndActiveTrue(UUID id);

    Optional<PsychometricAssessment> findByCodeAndActiveTrue(String code);
}

