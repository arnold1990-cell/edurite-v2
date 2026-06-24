package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assessment_results")
@Getter
@Setter
public class AssessmentResult extends BaseEntity {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "learner_user_id", nullable = false)
    private UUID learnerUserId;

    @Column(name = "marks_awarded", nullable = false)
    private BigDecimal marksAwarded;

    @Column(name = "comments")
    private String comments;

    @Column(name = "released", nullable = false)
    private boolean released = false;
}


