package com.edurite.company.repository;

import com.edurite.company.entity.CompanyInvitation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInvitationRepository extends JpaRepository<CompanyInvitation, UUID> {
    List<CompanyInvitation> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}

