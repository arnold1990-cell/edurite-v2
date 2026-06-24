package com.edurite.student.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "students")
@Getter
@Setter
/**
 * This class named StudentProfile is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentProfile extends BaseEntity {

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true)
    private UUID userId;

    private String firstName;
    private String lastName;
    private String interests;
    private String location;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String bio;
    private String qualificationLevel;
    private String selectedGrade;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String qualifications;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String experience;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String skills;

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT")
    private String careerGoals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String subjectAchievementsJson = "[]";

    private String cvFileUrl;
    private String transcriptFileUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String preferencesJson = "{}";

    private boolean profileCompleted;
    private boolean inAppNotificationsEnabled = true;
    private boolean emailNotificationsEnabled = false;
    private boolean smsNotificationsEnabled = false;
}

