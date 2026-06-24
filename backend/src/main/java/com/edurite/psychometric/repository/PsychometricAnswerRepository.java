package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricAnswerRepository extends JpaRepository<PsychometricAnswer, UUID> {
    List<PsychometricAnswer> findByAttemptId(UUID attemptId);
}

