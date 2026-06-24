package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolTask;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolTaskRepository extends JpaRepository<SchoolTask, UUID> {
    List<SchoolTask> findBySchoolIdAndTeacherUserId(UUID schoolId, UUID teacherUserId);
    List<SchoolTask> findBySchoolIdAndClassIdAndSubjectId(UUID schoolId, UUID classId, UUID subjectId);
}



