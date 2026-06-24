package com.edurite.company.repository;

import com.edurite.company.entity.CompanyBookmark;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyBookmarkRepository extends JpaRepository<CompanyBookmark, UUID> {
    List<CompanyBookmark> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<CompanyBookmark> findByCompanyIdAndStudentId(UUID companyId, UUID studentId);
}

