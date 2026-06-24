package com.edurite.psychometric.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "psychometric_results", schema = "public")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class PsychometricResult extends BaseEntity {

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strongest_areas", nullable = false, columnDefinition = "jsonb")
    private String strongestAreas = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_areas", nullable = false, columnDefinition = "jsonb")
    private String growthAreas = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scores", nullable = false, columnDefinition = "jsonb")
    private String scores = "{}";

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt = OffsetDateTime.now();
}

