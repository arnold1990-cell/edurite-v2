package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "curriculum_risk_alerts")
@Getter
@Setter
public class CurriculumRiskAlert extends BaseEntity {

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "week_plan_id", nullable = false)
    private UUID weekPlanId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "detail", nullable = false)
    private String detail;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
