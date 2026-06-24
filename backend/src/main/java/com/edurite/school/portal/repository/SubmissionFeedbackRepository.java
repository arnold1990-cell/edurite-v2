package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SubmissionFeedback;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionFeedbackRepository extends JpaRepository<SubmissionFeedback, UUID> {
    Optional<SubmissionFeedback> findBySubmissionId(UUID submissionId);
}



