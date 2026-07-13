package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.CurriculumAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurriculumAssetRepository extends JpaRepository<CurriculumAsset, UUID> {
    List<CurriculumAsset> findByDistrictIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(UUID districtId);
    List<CurriculumAsset> findByDistrictIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(UUID districtId, String repositoryType);
    List<CurriculumAsset> findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(UUID schoolId);
    List<CurriculumAsset> findBySchoolIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(UUID schoolId, String repositoryType);
    List<CurriculumAsset> findByRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(String repositoryType);
    Optional<CurriculumAsset> findByGenerationRequestKey(String generationRequestKey);

    @Query("""
            select
                a.id as id,
                a.ownerScope as ownerScope,
                a.repositoryType as repositoryType,
                a.contentSource as contentSource,
                a.source as source,
                a.visibility as visibility,
                a.status as status,
                a.extractionStatus as extractionStatus,
                a.extractionError as extractionError,
                a.title as title,
                a.subject as subject,
                a.grade as grade,
                a.curriculumPhase as curriculumPhase,
                a.academicYear as academicYear,
                a.province as province,
                a.versionNumber as versionNumber,
                a.description as description,
                a.term as term,
                a.weekNumber as weekNumber,
                a.uploadedByUserId as uploadedByUserId,
                a.uploadDate as uploadDate,
                a.extractedAt as extractedAt,
                a.archived as archived,
                a.active as active,
                a.deleted as deleted,
                a.pdfFileName as pdfFileName,
                a.docxFileName as docxFileName,
                a.excelFileName as excelFileName
            from CurriculumAsset a
            where a.districtId = :districtId
              and a.archived = false
              and a.active = true
              and a.deleted = false
              and upper(coalesce(a.status, 'ACTIVE')) = 'ACTIVE'
            order by a.repositoryType asc, a.subject asc, a.grade asc, a.uploadDate desc
            """)
    List<CurriculumAssetSummaryView> findActiveDistrictAssetSummaries(@Param("districtId") UUID districtId);

    @Query("""
            select
                a.id as id,
                a.ownerScope as ownerScope,
                a.repositoryType as repositoryType,
                a.contentSource as contentSource,
                a.source as source,
                a.visibility as visibility,
                a.status as status,
                a.extractionStatus as extractionStatus,
                a.extractionError as extractionError,
                a.title as title,
                a.subject as subject,
                a.grade as grade,
                a.curriculumPhase as curriculumPhase,
                a.academicYear as academicYear,
                a.province as province,
                a.versionNumber as versionNumber,
                a.description as description,
                a.term as term,
                a.weekNumber as weekNumber,
                a.uploadedByUserId as uploadedByUserId,
                a.uploadDate as uploadDate,
                a.extractedAt as extractedAt,
                a.archived as archived,
                a.active as active,
                a.deleted as deleted,
                a.pdfFileName as pdfFileName,
                a.docxFileName as docxFileName,
                a.excelFileName as excelFileName
            from CurriculumAsset a
            where a.districtId = :districtId
              and upper(a.repositoryType) = upper(:repositoryType)
              and a.archived = false
              and a.active = true
              and a.deleted = false
              and upper(coalesce(a.status, 'ACTIVE')) = 'ACTIVE'
            order by a.subject asc, a.grade asc, a.uploadDate desc
            """)
    List<CurriculumAssetSummaryView> findActiveDistrictAssetSummariesByRepositoryType(
            @Param("districtId") UUID districtId,
            @Param("repositoryType") String repositoryType
    );
}
