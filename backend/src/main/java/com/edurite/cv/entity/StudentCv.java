package com.edurite.cv.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_cvs")
@Getter
@Setter
public class StudentCv extends BaseEntity {

    @Column(name = "student_id", nullable = false, unique = true)
    private UUID studentId;

    @Column(name = "personal_summary", columnDefinition = "TEXT")
    private String personalSummary;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "references_text", columnDefinition = "TEXT")
    private String references;

    @Column(name = "career_objective", columnDefinition = "TEXT")
    private String careerObjective;
}

