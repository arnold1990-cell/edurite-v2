package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.TeacherCurriculumProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherCurriculumProgressRepository extends JpaRepository<TeacherCurriculumProgress, UUID> {
    Optional<TeacherCurriculumProgress> findByWeekPlanIdAndTeacherUserId(UUID weekPlanId, UUID teacherUserId);
    List<TeacherCurriculumProgress> findByTeacherUserIdAndSchoolId(UUID teacherUserId, UUID schoolId);
    List<TeacherCurriculumProgress> findByWeekPlanIdInAndTeacherUserId(List<UUID> weekPlanIds, UUID teacherUserId);
}
