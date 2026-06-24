package com.edurite.curriculum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "atp_teacher_reminders")
@Getter
@Setter
public class AtpTeacherReminder {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "atp_calendar_item_id", nullable = false)
    private UUID atpCalendarItemId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "reminder_type", nullable = false)
    private String reminderType;

    @Column(name = "reminder_date", nullable = false)
    private OffsetDateTime reminderDate;

    @Column(name = "reminder_message", nullable = false)
    private String reminderMessage;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
