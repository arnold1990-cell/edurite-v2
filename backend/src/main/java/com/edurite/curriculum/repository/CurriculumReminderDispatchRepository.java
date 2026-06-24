package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.CurriculumReminderDispatch;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumReminderDispatchRepository extends JpaRepository<CurriculumReminderDispatch, UUID> {
    boolean existsByTeacherUserIdAndWeekPlanIdAndReminderTypeAndReminderDate(UUID teacherUserId, UUID weekPlanId, String reminderType, LocalDate reminderDate);
}
