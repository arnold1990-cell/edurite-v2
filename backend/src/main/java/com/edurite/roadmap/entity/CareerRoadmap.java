package com.edurite.roadmap.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "career_roadmaps")
@Getter
@Setter
public class CareerRoadmap extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "required_subjects", columnDefinition = "TEXT")
    private String requiredSubjects;

    @Column(name = "recommended_skills", columnDefinition = "TEXT")
    private String recommendedSkills;

    @Column(name = "study_path", columnDefinition = "TEXT")
    private String studyPath;

    @Column(name = "entry_level_jobs", columnDefinition = "TEXT")
    private String entryLevelJobs;

    @Column(name = "long_term_growth", columnDefinition = "TEXT")
    private String longTermGrowth;

    @Column(name = "learning_resources", columnDefinition = "TEXT")
    private String learningResources;

    @Column(nullable = false)
    private boolean active = true;
}

