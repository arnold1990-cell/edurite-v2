package com.edurite.curriculum.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "curriculum_assets")
@Getter
@Setter
public class CurriculumAsset extends BaseEntity {

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "teacher_user_id")
    private UUID teacherUserId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "source_curriculum_asset_id")
    private UUID sourceCurriculumAssetId;

    @Column(name = "source_atp_calendar_item_id")
    private UUID sourceAtpCalendarItemId;

    @Column(name = "owner_scope", nullable = false)
    private String ownerScope;

    @Column(name = "repository_type", nullable = false)
    private String repositoryType;

    @Column(name = "content_source", nullable = false)
    private String contentSource;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "visibility", nullable = false)
    private String visibility = "DISTRICT_WIDE";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "lesson_plan_status")
    private String lessonPlanStatus;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "curriculum_phase")
    private String curriculumPhase;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "province")
    private String province;

    @Column(name = "version_number")
    private String versionNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "term")
    private String term;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "lesson_topic")
    private String lessonTopic;

    @Column(name = "lesson_date")
    private LocalDate lessonDate;

    @Column(name = "lesson_duration_minutes")
    private Integer lessonDurationMinutes;

    @Column(name = "language")
    private String language;

    @Column(name = "generation_request_key")
    private String generationRequestKey;

    @Column(name = "ai_provider")
    private String aiProvider;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "ai_generated_at")
    private OffsetDateTime aiGeneratedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "generated_by_ai", nullable = false)
    private boolean generatedByAi;

    @Column(name = "lesson_plan_payload_json", columnDefinition = "TEXT")
    private String lessonPlanPayloadJson;

    @Column(name = "ai_metadata_json", columnDefinition = "TEXT")
    private String aiMetadataJson;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "upload_date", nullable = false)
    private OffsetDateTime uploadDate = OffsetDateTime.now();

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "extraction_status", nullable = false)
    private String extractionStatus = "PENDING";

    @Column(name = "extraction_error")
    private String extractionError;

    @Column(name = "extracted_at")
    private OffsetDateTime extractedAt;

    @Column(name = "pdf_file_name")
    private String pdfFileName;

    @Column(name = "pdf_content_type")
    private String pdfContentType;

    @Column(name = "pdf_base64", columnDefinition = "TEXT")
    private String pdfBase64;

    @Column(name = "pdf_bytes", columnDefinition = "BYTEA")
    private byte[] pdfBytes;

    @Column(name = "docx_file_name")
    private String docxFileName;

    @Column(name = "docx_content_type")
    private String docxContentType;

    @Column(name = "docx_base64", columnDefinition = "TEXT")
    private String docxBase64;

    @Column(name = "docx_bytes", columnDefinition = "BYTEA")
    private byte[] docxBytes;

    @Column(name = "excel_file_name")
    private String excelFileName;

    @Column(name = "excel_content_type")
    private String excelContentType;

    @Column(name = "excel_base64", columnDefinition = "TEXT")
    private String excelBase64;

    @Column(name = "excel_bytes", columnDefinition = "BYTEA")
    private byte[] excelBytes;
}
