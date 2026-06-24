package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subject_catalogue")
@Getter
@Setter
public class SubjectCatalogue extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "grade_range", nullable = false)
    private String gradeRange;

    @Column(name = "subject_type")
    private String subjectType;

    @Column(name = "language_level")
    private String languageLevel;

    @Column(name = "is_language", nullable = false)
    private boolean language;

    @Column(name = "is_compulsory", nullable = false)
    private boolean compulsory;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
