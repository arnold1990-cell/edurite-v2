package com.edurite.psychometric.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "psychometric_questions", schema = "public")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class PsychometricQuestion extends BaseEntity {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "question_key", nullable = false, length = 120)
    private String questionKey;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "dimension_key", nullable = false, length = 120)
    private String dimensionKey;

    @Column(name = "min_score", nullable = false)
    private int minScore = 1;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 5;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

