package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "atp_topics")
@Getter
@Setter
public class AtpTopic extends BaseEntity {

    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "grade", nullable = false)
    private String grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_catalogue_id")
    private SubjectCatalogue subjectCatalogue;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(name = "term", nullable = false)
    private String term;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "subtopic")
    private String subtopic;

    @Column(name = "recommended_activities")
    private String recommendedActivities;

    @Column(name = "assessment_guidance")
    private String assessmentGuidance;

    @Column(name = "caps_reference")
    private String capsReference;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
