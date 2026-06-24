package com.edurite.student.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "student_preferences")
@Getter
@Setter
public class StudentPreference extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID studentId;

    @Column(columnDefinition = "TEXT")
    private String preferredIndustries;

    @Column(columnDefinition = "TEXT")
    private String preferredLocations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String notificationPreferences = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String extra = "{}";
}

