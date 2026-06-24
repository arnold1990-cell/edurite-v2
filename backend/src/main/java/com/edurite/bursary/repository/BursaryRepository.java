package com.edurite.bursary.repository;

import com.edurite.bursary.entity.Bursary;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * This interface named BursaryRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface BursaryRepository extends JpaRepository<Bursary, UUID> {
    Page<Bursary> findByStatusIgnoreCaseAndDeletedAtIsNullAndTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
            String status,
            String title,
            String qualificationLevel,
            String location,
            String eligibility,
            Pageable pageable
    );

    List<Bursary> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<Bursary> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
    List<Bursary> findTop10ByDeletedAtIsNullOrderByCreatedAtDesc();
    long countByStatusIgnoreCase(String status);
    long countByApplicationEndDateBeforeAndStatusIn(LocalDate date, List<String> statuses);
    List<Bursary> findByApplicationEndDateBeforeAndStatusIn(LocalDate date, List<String> statuses);
    long countByDeletedAtIsNull();

    @Query("""
            SELECT b FROM Bursary b
            WHERE b.deletedAt IS NULL
              AND UPPER(COALESCE(b.status, '')) = 'ACTIVE'
              AND (
                :query = ''
                OR LOWER(COALESCE(b.title, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(b.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(b.fieldOfStudy, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(b.eligibility, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
              AND (
                :qualification = ''
                OR LOWER(COALESCE(b.qualificationLevel, '')) LIKE LOWER(CONCAT('%', :qualification, '%'))
              )
              AND (
                :region = ''
                OR LOWER(COALESCE(b.location, '')) LIKE LOWER(CONCAT('%', :region, '%'))
                OR REPLACE(LOWER(COALESCE(b.location, '')), ' ', '') LIKE CONCAT('%', REPLACE(LOWER(:region), ' ', ''), '%')
              )
              AND (
                :eligibility = ''
                OR LOWER(COALESCE(b.eligibility, '')) LIKE LOWER(CONCAT('%', :eligibility, '%'))
              )
            """)
    Page<Bursary> searchPublic(
            @Param("query") String query,
            @Param("qualification") String qualification,
            @Param("region") String region,
            @Param("eligibility") String eligibility,
            Pageable pageable
    );

    @Query("""
            SELECT b
            FROM Bursary b
            WHERE (:includeDeleted = true OR b.deletedAt IS NULL)
              AND (:status = '' OR UPPER(COALESCE(b.status, '')) = UPPER(:status))
              AND (:companyId IS NULL OR b.companyId = :companyId)
              AND b.createdAt >= COALESCE(:fromDate, b.createdAt)
              AND b.createdAt <= COALESCE(:toDate, b.createdAt)
            ORDER BY b.createdAt DESC
            """)
    List<Bursary> searchForAdmin(
            @Param("status") String status,
            @Param("companyId") UUID companyId,
            @Param("fromDate") java.time.OffsetDateTime fromDate,
            @Param("toDate") java.time.OffsetDateTime toDate,
            @Param("includeDeleted") boolean includeDeleted
    );
}

