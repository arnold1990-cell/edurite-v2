package com.edurite.roadmap.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "career_program_requirements")
@Getter
@Setter
public class CareerProgramRequirement extends BaseEntity {

    @Column(name = "career_name", nullable = false)
    private String careerName;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "institution_type")
    private String institutionType;

    @Column(name = "province")
    private String province;

    @Column(name = "qualification_name", nullable = false)
    private String qualificationName;

    @Column(name = "faculty")
    private String faculty;

    @Column(name = "aps_required")
    private Integer apsRequired;

    @Column(name = "mathematics_requirement")
    private String mathematicsRequirement;

    @Column(name = "mathematical_literacy_requirement")
    private String mathematicalLiteracyRequirement;

    @Column(name = "english_requirement")
    private String englishRequirement;

    @Column(name = "accounting_requirement")
    private String accountingRequirement;

    @Column(name = "physical_sciences_requirement")
    private String physicalSciencesRequirement;

    @Column(name = "life_sciences_requirement")
    private String lifeSciencesRequirement;

    @Column(name = "duration")
    private String duration;

    @Column(name = "nqf_level")
    private String nqfLevel;

    @Column(name = "application_url")
    private String applicationUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source")
    private String source;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;
}
