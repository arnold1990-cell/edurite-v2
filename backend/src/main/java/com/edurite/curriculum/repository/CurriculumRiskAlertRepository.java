package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.CurriculumRiskAlert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumRiskAlertRepository extends JpaRepository<CurriculumRiskAlert, UUID> {
    List<CurriculumRiskAlert> findByDistrictIdAndStatusIgnoreCaseOrderByCreatedAtDesc(UUID districtId, String status);
    Optional<CurriculumRiskAlert> findByTeacherUserIdAndWeekPlanId(UUID teacherUserId, UUID weekPlanId);
}
