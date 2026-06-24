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
@Table(name = "submission_feedback")
@Getter
@Setter
public class SubmissionFeedback extends BaseEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "marks_awarded")
    private BigDecimal marksAwarded;

    @Column(name = "comments")
    private String comments;

    @Column(name = "rubric_scoring")
    private String rubricScoring;

    @Column(name = "released", nullable = false)
    private boolean released = false;
}


