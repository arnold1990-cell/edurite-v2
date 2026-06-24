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
@Table(name = "school_visit_schedules")
@Getter
@Setter
public class SchoolVisitSchedule extends BaseEntity {

    @Column(name = "circuit_manager_id", nullable = false)
    private UUID circuitManagerId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String status = "SCHEDULED";

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "text")
    private String outcome;
}
