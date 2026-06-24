package com.edurite.learning.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "learning_outcome_mappings")
@Getter
@Setter
public class LearningOutcomeMapping extends BaseEntity {

    @Column(nullable = false)
    private String outcomeKey;

    private String outcomeLabel;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private int priority = 1;
}

