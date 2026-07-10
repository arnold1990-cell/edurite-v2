package com.edurite.institution.universityinfo.entity;

import com.edurite.common.entity.BaseEntity;
import com.edurite.institution.entity.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "university_admission_requirements")
@Getter
@Setter
public class UniversityAdmissionRequirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id")
    private UniversityProgramme programme;

    @Column(name = "programme_name", length = 255)
    private String programmeName;

    @Column(name = "requirement_title", length = 255)
    private String requirementTitle;

    @Column(name = "aps_minimum")
    private Integer apsMinimum;

    @Column(name = "required_subjects", columnDefinition = "TEXT")
    private String requiredSubjects;

    @Column(name = "minimum_marks", columnDefinition = "TEXT")
    private String minimumMarks;

    @Column(name = "nsc_requirement", columnDefinition = "TEXT")
    private String nscRequirement;

    @Column(name = "language_requirement", columnDefinition = "TEXT")
    private String languageRequirement;

    @Column(name = "faculty_specific_requirement", columnDefinition = "TEXT")
    private String facultySpecificRequirement;

    @Column(name = "international_requirement", columnDefinition = "TEXT")
    private String internationalRequirement;

    @Column(name = "additional_tests", columnDefinition = "TEXT")
    private String additionalTests;

    @Column(name = "source_url", nullable = false, length = 1200)
    private String sourceUrl;

    @Column(name = "source_label", length = 120)
    private String sourceLabel;

    @Column(name = "retrieval_status", nullable = false, length = 40)
    private String retrievalStatus;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(name = "active", nullable = false)
    private boolean active;
}