package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.TeacherAtpProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAtpProgressRepository extends JpaRepository<TeacherAtpProgress, UUID> {
    Optional<TeacherAtpProgress> findByTeacherIdAndSchoolIdAndAtpCalendarItemId(UUID teacherId, UUID schoolId, UUID atpCalendarItemId);
    List<TeacherAtpProgress> findByTeacherIdAndSchoolId(UUID teacherId, UUID schoolId);
    List<TeacherAtpProgress> findByAtpCalendarItemIdInAndTeacherId(List<UUID> atpCalendarItemIds, UUID teacherId);
}
