package com.edurite.company.repository;

import com.edurite.company.entity.CompanyShortlist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyShortlistRepository extends JpaRepository<CompanyShortlist, UUID> {
    List<CompanyShortlist> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}

