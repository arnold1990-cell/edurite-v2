package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "companies")
@Getter
@Setter
/**
 * This class named CompanyProfile is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyProfile extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private UUID userId;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String companyName;

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String industry;

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private String officialEmail;

    private String mobileNumber;
    private String contactPersonName;
    private String address;
    private String website;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String description;

// @Enumerated stores enum values in a readable form in the database.
    @Enumerated(EnumType.STRING)
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private CompanyApprovalStatus status = CompanyApprovalStatus.PENDING;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private boolean emailVerified;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private boolean mobileVerified;

    private OffsetDateTime reviewedAt;
    private UUID reviewedBy;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    private OffsetDateTime deletedAt;
    private UUID deletedBy;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String deletionReason;
}

