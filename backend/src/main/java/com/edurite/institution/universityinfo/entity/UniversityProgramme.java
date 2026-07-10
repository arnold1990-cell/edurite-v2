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

//noinspection JpaDataSourceORMInspection
@Entity
@Table(name = "university_programmes")
@Getter
@Setter
public class UniversityProgramme extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 120)
    private String qualificationType;

    @Column(length = 180)
    private String faculty;

    @Column(length = 180)
    private String department;

    @Column(length = 120)
    private String duration;

    @Column(length = 80)
    private String studyMode;

    @Column(length = 180)
    private String campus;

    @Column(length = 1200)
    private String programmeUrl;

    @Column(nullable = false, length = 1200)
    private String sourceUrl;

    @Column(length = 120)
    private String sourceLabel;
    @Column(nullable = false, length = 40)
    private String retrievalStatus;

    private OffsetDateTime lastVerifiedAt;
    @Column(nullable = false)
    private boolean active;
}


