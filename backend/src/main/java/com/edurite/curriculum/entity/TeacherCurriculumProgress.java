package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_curriculum_progress")
@Getter
@Setter
public class TeacherCurriculumProgress extends BaseEntity {

    @Column(name = "week_plan_id", nullable = false)
    private UUID weekPlanId;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "status", nullable = false)
    private String status = "NOT_STARTED";

    @Column(name = "completion_percent", nullable = false)
    private Integer completionPercent = 0;

    @Column(name = "notes")
    private String notes;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
