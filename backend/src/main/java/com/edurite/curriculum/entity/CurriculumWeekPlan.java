package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "curriculum_week_plans")
@Getter
@Setter
public class CurriculumWeekPlan extends BaseEntity {

    @Column(name = "curriculum_asset_id", nullable = false)
    private UUID curriculumAssetId;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "curriculum_phase")
    private String curriculumPhase;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "province")
    private String province;

    @Column(name = "term", nullable = false)
    private String term;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "subtopic")
    private String subtopic;

    @Column(name = "learning_outcomes")
    private String learningOutcomes;

    @Column(name = "assessment_activities")
    private String assessmentActivities;

    @Column(name = "resources_materials")
    private String resourcesMaterials;

    @Column(name = "lesson_focus")
    private String lessonFocus;

    @Column(name = "notes")
    private String notes;

    @Column(name = "status", nullable = false)
    private String status = "PUBLISHED";

    @Column(name = "expected_completion_label")
    private String expectedCompletionLabel;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
