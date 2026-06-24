package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "curriculum_reminder_dispatches")
@Getter
@Setter
public class CurriculumReminderDispatch extends BaseEntity {

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "week_plan_id", nullable = false)
    private UUID weekPlanId;

    @Column(name = "reminder_type", nullable = false)
    private String reminderType;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;
}
