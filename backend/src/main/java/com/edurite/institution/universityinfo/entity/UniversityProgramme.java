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
@Table(name = "university_programmes")
@Getter
@Setter
public class UniversityProgramme extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "qualification_type", length = 120)
    private String qualificationType;

    @Column(name = "faculty", length = 180)
    private String faculty;

    @Column(name = "department", length = 180)
    private String department;

    @Column(name = "duration", length = 120)
    private String duration;

    @Column(name = "study_mode", length = 80)
    private String studyMode;

    @Column(name = "campus", length = 180)
    private String campus;

    @Column(name = "programme_url", length = 1200)
    private String programmeUrl;

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