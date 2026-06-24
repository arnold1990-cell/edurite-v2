package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricAttemptRepository extends JpaRepository<PsychometricAttempt, UUID> {
    List<PsychometricAttempt> findByStudentIdAndAssessmentIdOrderBySubmittedAtDesc(UUID studentId, UUID assessmentId);
}

