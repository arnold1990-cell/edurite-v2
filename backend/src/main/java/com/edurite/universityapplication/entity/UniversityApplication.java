package com.edurite.universityapplication.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "university_applications")
@Getter
@Setter
public class UniversityApplication extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "university_name", nullable = false)
    private String universityName;

    @Column(name = "programme_name", nullable = false)
    private String programmeName;

    private String country;

    @Column(name = "intake_year")
    private Integer intakeYear;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "application_status", nullable = false)
    private String applicationStatus = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "document_references", columnDefinition = "TEXT")
    private String documentReferences;
}

