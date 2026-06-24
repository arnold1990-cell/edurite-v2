package com.edurite.psychometric.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "psychometric_answers", schema = "public")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class PsychometricAnswer extends BaseEntity {

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "dimension_key", nullable = false, length = 120)
    private String dimensionKey;

    @Column(name = "score", nullable = false)
    private int score;
}

