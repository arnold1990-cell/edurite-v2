package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_interventions")
@Getter
@Setter
public class SchoolIntervention extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "learner_user_id", nullable = false)
    private UUID learnerUserId;

    @Column(name = "assigned_by_user_id", nullable = false)
    private UUID assignedByUserId;

    @Column(name = "support_type", nullable = false)
    private String supportType;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(nullable = false)
    private boolean active = true;
}
