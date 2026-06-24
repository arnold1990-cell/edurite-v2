package com.edurite.company.repository;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * This interface named CompanyProfileRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {
    Optional<CompanyProfile> findByUserId(UUID userId);
    long countByStatus(CompanyApprovalStatus status);
    Optional<CompanyProfile> findByOfficialEmailIgnoreCase(String officialEmail);
    Optional<CompanyProfile> findByMobileNumber(String mobileNumber);
    List<CompanyProfile> findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus status);
    List<CompanyProfile> findTop10ByOrderByCreatedAtDesc();
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    @Query("""
            SELECT c
            FROM CompanyProfile c
            WHERE (:includeDeleted = true OR c.deletedAt IS NULL)
              AND (:status IS NULL OR c.status = :status)
              AND (
                :search = ''
                OR LOWER(COALESCE(c.companyName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.officialEmail, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(c.registrationNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY c.createdAt DESC
            """)
    List<CompanyProfile> searchForAdmin(
            @Param("search") String search,
            @Param("status") CompanyApprovalStatus status,
            @Param("includeDeleted") boolean includeDeleted
    );
}

