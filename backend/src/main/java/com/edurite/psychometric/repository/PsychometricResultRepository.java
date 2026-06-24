package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricResultRepository extends JpaRepository<PsychometricResult, UUID> {
    Optional<PsychometricResult> findByAttemptId(UUID attemptId);

    List<PsychometricResult> findTop20ByStudentIdOrderByCalculatedAtDesc(UUID studentId);
}

