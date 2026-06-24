package com.edurite.gamification.entity;

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
@Table(name = "student_points_ledger")
@Getter
@Setter
public class StudentPointsLedger extends BaseEntity {

    @Column(nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private int points;

    private String referenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(nullable = false)
    private OffsetDateTime awardedAt = OffsetDateTime.now();

    private String termCode;
}

