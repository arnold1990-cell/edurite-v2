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
@Table(name = "plagiarism_reports")
@Getter
@Setter
public class PlagiarismReport extends BaseEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "compared_submission_id")
    private UUID comparedSubmissionId;

    @Column(name = "similarity_percentage", nullable = false)
    private BigDecimal similarityPercentage;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "report_details")
    private String reportDetails;
}


