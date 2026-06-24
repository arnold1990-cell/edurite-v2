package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task_submissions")
@Getter
@Setter
public class TaskSubmission extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "learner_user_id", nullable = false)
    private UUID learnerUserId;

    @Column(name = "submission_text")
    private String submissionText;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "late", nullable = false)
    private boolean late = false;

    @Column(name = "status", nullable = false)
    private String status = "SUBMITTED";
}


