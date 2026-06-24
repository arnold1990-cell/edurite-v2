package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.TaskSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, UUID> {
    Optional<TaskSubmission> findByTaskIdAndLearnerUserId(UUID taskId, UUID learnerUserId);
    List<TaskSubmission> findByTaskId(UUID taskId);
    List<TaskSubmission> findByLearnerUserId(UUID learnerUserId);
}



