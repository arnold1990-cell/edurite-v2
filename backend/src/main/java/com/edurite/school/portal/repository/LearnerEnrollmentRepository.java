package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.LearnerEnrollment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerEnrollmentRepository extends JpaRepository<LearnerEnrollment, UUID> {
    List<LearnerEnrollment> findBySchoolIdAndActiveTrue(UUID schoolId);
    List<LearnerEnrollment> findBySchoolIdAndLearnerUserIdAndActiveTrue(UUID schoolId, UUID learnerUserId);
    List<LearnerEnrollment> findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(UUID schoolId, UUID classId, UUID subjectId);
    List<LearnerEnrollment> findBySchoolIdAndClassIdInAndSubjectIdInAndActiveTrue(UUID schoolId, Collection<UUID> classIds, Collection<UUID> subjectIds);
    Optional<LearnerEnrollment> findBySchoolIdAndLearnerUserIdAndClassIdAndSubjectIdAndActiveTrue(UUID schoolId, UUID learnerUserId, UUID classId, UUID subjectId);
}



