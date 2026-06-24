package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.AtpCalendarItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtpCalendarItemRepository extends JpaRepository<AtpCalendarItem, UUID> {
    List<AtpCalendarItem> findByCurriculumResourceIdOrderByTermAscWeekNumberAsc(UUID curriculumResourceId);
    List<AtpCalendarItem> findByStatusIgnoreCaseOrderByAcademicYearDescTermAscWeekNumberAsc(String status);
    List<AtpCalendarItem> findBySubjectIgnoreCaseAndGradeIgnoreCaseAndStatusIgnoreCaseOrderByAcademicYearDescTermAscWeekNumberAsc(String subject, String grade, String status);
    List<AtpCalendarItem> findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(UUID curriculumResourceId, String status);
    List<AtpCalendarItem> findByCurriculumResourceIdAndStatusIgnoreCase(UUID curriculumResourceId, String status);
    Optional<AtpCalendarItem> findByIdAndStatusIgnoreCase(UUID id, String status);
    void deleteByCurriculumResourceId(UUID curriculumResourceId);
}
