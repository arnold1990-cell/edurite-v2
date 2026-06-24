package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricSubmissionRepository extends JpaRepository<PsychometricSubmission, UUID> {
    Optional<PsychometricSubmission> findTopByStudentIdOrderByCreatedAtDesc(UUID studentId);
    boolean existsByStudentId(UUID studentId);

    List<PsychometricSubmission> findTop20ByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<PsychometricSubmission> findTop20ByPublicSessionIdOrderByCreatedAtDesc(String publicSessionId);
}

