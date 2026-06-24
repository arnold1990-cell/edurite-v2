package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "company_documents")
@Getter
@Setter
/**
 * This class named CompanyVerificationDocument is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyVerificationDocument extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID companyId;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String documentType;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String objectKey;

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String verificationStatus = "PENDING";

    private String fileName;
    private UUID uploadedBy;
}

