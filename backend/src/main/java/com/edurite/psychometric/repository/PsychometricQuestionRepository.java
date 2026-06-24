package com.edurite.psychometric.repository;

import com.edurite.psychometric.entity.PsychometricQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychometricQuestionRepository extends JpaRepository<PsychometricQuestion, UUID> {
    List<PsychometricQuestion> findByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(UUID assessmentId);
    List<PsychometricQuestion> findTop25ByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(UUID assessmentId);
}

