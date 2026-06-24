package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_assignments")
@Getter
@Setter
public class TeacherAssignment extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "phase")
    private String phase;

    @Column(name = "grade")
    private String grade;

    @Column(name = "is_class_teacher", nullable = false)
    private boolean classTeacher;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}


