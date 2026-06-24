package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolIntervention;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolInterventionRepository extends JpaRepository<SchoolIntervention, UUID> {
    List<SchoolIntervention> findBySchoolIdAndActiveTrue(UUID schoolId);
    List<SchoolIntervention> findBySchoolIdInAndActiveTrue(List<UUID> schoolIds);
    List<SchoolIntervention> findBySchoolIdAndLearnerUserIdAndActiveTrue(UUID schoolId, UUID learnerUserId);
}
