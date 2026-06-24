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
@Table(name = "atp_calendar_items")
@Getter
@Setter
public class AtpCalendarItem extends BaseEntity {

    @Column(name = "curriculum_resource_id", nullable = false)
    private UUID curriculumResourceId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "phase")
    private String phase;

    @Column(name = "academic_year")
    private Integer academicYear;

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

    @Column(name = "learning_objectives")
    private String learningObjectives;

    @Column(name = "resources")
    private String resources;

    @Column(name = "assessment_task")
    private String assessmentTask;

    @Column(name = "lesson_focus")
    private String lessonFocus;

    @Column(name = "notes")
    private String notes;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "created_by")
    private UUID createdBy;
}
