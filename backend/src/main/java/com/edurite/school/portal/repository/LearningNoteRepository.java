package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.LearningNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningNoteRepository extends JpaRepository<LearningNote, UUID> {
    List<LearningNote> findBySchoolIdAndClassIdAndSubjectIdAndPublishedTrue(UUID schoolId, UUID classId, UUID subjectId);
    List<LearningNote> findBySchoolIdAndTeacherUserId(UUID schoolId, UUID teacherUserId);
}



