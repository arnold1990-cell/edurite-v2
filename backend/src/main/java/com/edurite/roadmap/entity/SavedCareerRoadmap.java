package com.edurite.roadmap.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "saved_career_roadmaps")
@Getter
@Setter
public class SavedCareerRoadmap extends BaseEntity {

    @Column(name = "learner_id", nullable = false)
    private UUID learnerId;

    @Column(name = "career_name", nullable = false)
    private String careerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roadmap_json", nullable = false, columnDefinition = "jsonb")
    private String roadmapJson;

    @Column(name = "learner_aps", nullable = false)
    private Integer learnerAps = 0;

    @Column(name = "required_aps")
    private Integer requiredAps;

    @Column(name = "aps_gap")
    private Integer apsGap;

    @Column(name = "readiness_score", nullable = false)
    private Integer readinessScore = 0;
}
