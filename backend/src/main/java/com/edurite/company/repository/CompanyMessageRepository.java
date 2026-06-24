package com.edurite.company.repository;

import com.edurite.company.entity.CompanyMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMessageRepository extends JpaRepository<CompanyMessage, UUID> {
    List<CompanyMessage> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}

