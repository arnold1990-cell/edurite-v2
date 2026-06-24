package com.edurite.career.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "careers")
@Getter
@Setter
/**
 * This class named Career is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class Career extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String title;

    private String description;
    private String industry;
    private String qualificationLevel;
    private String location;
    private String salaryRange;
    private String demandLevel;

    @Transient
    private Integer matchScore;
}

