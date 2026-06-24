package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_subjects")
@Getter
@Setter
public class SchoolSubject extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "grade")
    private String grade;

    @Column(name = "grade_range")
    private String gradeRange;

    @Column(name = "language_level")
    private String languageLevel;

    @Column(name = "subject_type")
    private String subjectType;

    @Column(name = "is_language", nullable = false)
    private boolean language;

    @Column(name = "is_compulsory", nullable = false)
    private boolean compulsory;

    @Column(name = "hod_user_id")
    private UUID hodUserId;

    @Column(name = "caps_aligned", nullable = false)
    private boolean capsAligned = true;

    @ManyToOne
    @JoinColumn(name = "subject_catalogue_id")
    private SubjectCatalogue subjectCatalogue;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}


