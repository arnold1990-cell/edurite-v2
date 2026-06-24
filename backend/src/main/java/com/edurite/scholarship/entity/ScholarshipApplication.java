package com.edurite.scholarship.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scholarship_applications")
@Getter
@Setter
public class ScholarshipApplication extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "bursary_id")
    private UUID bursaryId;

    @Column(name = "scholarship_title", nullable = false)
    private String scholarshipTitle;

    private String provider;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(nullable = false)
    private String status = "NOT_STARTED";

    @Column(columnDefinition = "TEXT")
    private String checklist;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(name = "reminder_notes", columnDefinition = "TEXT")
    private String reminderNotes;

    @Column(name = "motivation_letter_draft", columnDefinition = "TEXT")
    private String motivationLetterDraft;

    @Column(nullable = false)
    private boolean saved = true;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

