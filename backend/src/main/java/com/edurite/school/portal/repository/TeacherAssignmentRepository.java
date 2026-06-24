
package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.TeacherAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {
    List<TeacherAssignment> findBySchoolIdAndActiveTrue(UUID schoolId);
    List<TeacherAssignment> findBySchoolIdAndTeacherUserIdAndActiveTrue(UUID schoolId, UUID teacherUserId);
    List<TeacherAssignment> findBySchoolIdAndTeacherUserId(UUID schoolId, UUID teacherUserId);
    Optional<TeacherAssignment> findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(UUID schoolId, UUID teacherUserId, UUID classId, UUID subjectId);
    List<TeacherAssignment> findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(UUID schoolId, UUID classId, UUID subjectId);
}


