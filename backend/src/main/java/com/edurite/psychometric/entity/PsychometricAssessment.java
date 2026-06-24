package com.edurite.psychometric.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "psychometric_assessments", schema = "public")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class PsychometricAssessment extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "version", nullable = false, length = 40)
    private String version = "v1.0";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "public_available", nullable = false)
    private boolean publicAvailable = false;
}

