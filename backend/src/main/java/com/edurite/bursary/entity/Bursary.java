package com.edurite.bursary.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "bursaries")
@Getter
@Setter
/**
 * This class named Bursary is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class Bursary extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String title;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID companyId;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String description;

    private String provider;
    private String fieldOfStudy;
    private String qualificationLevel;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private BigDecimal fundingAmount;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String benefits;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String requiredSubjects;

    private String minimumGrade;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String demographics;

    private String location;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String eligibility;

    private String status;
    private OffsetDateTime deletedAt;
    private UUID deletedBy;

    @Column(columnDefinition = "TEXT")
    private String deletionReason;
}

