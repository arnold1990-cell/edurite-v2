package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "district_interventions")
@Getter
@Setter
public class DistrictIntervention extends BaseEntity {

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "intervention_type")
    private String interventionType;

    @Column(nullable = false)
    private String priority = "MEDIUM";

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column
    private String subject;

    @Column
    private String grade;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "target_scope", nullable = false)
    private String targetScope = "DISTRICT";

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "support_plan", columnDefinition = "text")
    private String supportPlan;

    @Column(nullable = false)
    private boolean active = true;
}
