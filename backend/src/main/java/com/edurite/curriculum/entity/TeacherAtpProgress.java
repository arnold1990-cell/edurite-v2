package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_atp_progress")
@Getter
@Setter
public class TeacherAtpProgress extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "atp_calendar_item_id", nullable = false)
    private UUID atpCalendarItemId;

    @Column(name = "status", nullable = false)
    private String status = "NOT_STARTED";

    @Column(name = "completion_percentage", nullable = false)
    private Integer completionPercentage = 0;

    @Column(name = "evidence_file")
    private String evidenceFile;

    @Column(name = "comment")
    private String comment;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
