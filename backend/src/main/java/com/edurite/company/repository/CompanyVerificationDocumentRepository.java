package com.edurite.company.repository;

import com.edurite.company.entity.CompanyVerificationDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named CompanyVerificationDocumentRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyVerificationDocumentRepository extends JpaRepository<CompanyVerificationDocument, UUID> {
    List<CompanyVerificationDocument> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}

