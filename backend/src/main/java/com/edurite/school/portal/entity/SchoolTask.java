package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_tasks")
@Getter
@Setter
public class SchoolTask extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "atp_topic_id")
    private UUID atpTopicId;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "phase")
    private String phase;

    @Column(name = "grade")
    private String grade;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "assessment_type")
    private String assessmentType;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "term")
    private String term;

    @Column(name = "max_marks", nullable = false)
    private BigDecimal maxMarks;

    @Column(name = "rubric")
    private String rubric;

    @Column(name = "resources_materials")
    private String resourcesMaterials;

    @Column(name = "cognitive_level")
    private String cognitiveLevel;

    @Column(name = "assessment_category")
    private String assessmentCategory;

    @Column(name = "released", nullable = false)
    private boolean released = false;
}


