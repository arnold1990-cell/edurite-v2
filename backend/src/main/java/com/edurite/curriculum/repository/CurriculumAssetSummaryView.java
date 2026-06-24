package com.edurite.curriculum.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CurriculumAssetSummaryView {
    UUID getId();
    String getOwnerScope();
    String getRepositoryType();
    String getContentSource();
    String getSource();
    String getVisibility();
    String getStatus();
    String getExtractionStatus();
    String getExtractionError();
    String getTitle();
    String getSubject();
    String getGrade();
    String getCurriculumPhase();
    Integer getAcademicYear();
    String getProvince();
    String getVersionNumber();
    String getDescription();
    String getTerm();
    Integer getWeekNumber();
    UUID getUploadedByUserId();
    OffsetDateTime getUploadDate();
    OffsetDateTime getExtractedAt();
    boolean isArchived();
    boolean isActive();
    boolean isDeleted();
    String getPdfFileName();
    String getDocxFileName();
    String getExcelFileName();
}
