package com.edurite.student.entity;

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
@Table(name = "saved_opportunities")
@Getter
@Setter
/**
 * This class named SavedCareer is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SavedCareer extends BaseEntity {
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID studentId;
    private UUID careerId;
    private String opportunityType;
    private String externalOpportunityKey;
    private String titleSnapshot;
}

