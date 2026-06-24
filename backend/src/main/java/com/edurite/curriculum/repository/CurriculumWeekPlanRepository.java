package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.CurriculumWeekPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumWeekPlanRepository extends JpaRepository<CurriculumWeekPlan, UUID> {
    List<CurriculumWeekPlan> findByCurriculumAssetIdOrderByTermAscWeekNumberAsc(UUID curriculumAssetId);
    void deleteByCurriculumAssetId(UUID curriculumAssetId);
    List<CurriculumWeekPlan> findByDistrictIdAndActiveTrueOrderBySubjectAscGradeAscTermAscWeekNumberAsc(UUID districtId);
    List<CurriculumWeekPlan> findByDistrictIdAndSubjectIgnoreCaseAndGradeIgnoreCaseAndActiveTrueOrderByTermAscWeekNumberAsc(UUID districtId, String subject, String grade);
    List<CurriculumWeekPlan> findByCurriculumAssetIdAndTermIgnoreCaseAndWeekNumber(UUID curriculumAssetId, String term, Integer weekNumber);
    java.util.Optional<CurriculumWeekPlan> findByCurriculumAssetIdAndTermIgnoreCaseAndWeekNumberAndTopicIgnoreCase(UUID curriculumAssetId, String term, Integer weekNumber, String topic);
    Optional<CurriculumWeekPlan> findByIdAndActiveTrue(UUID id);
}
