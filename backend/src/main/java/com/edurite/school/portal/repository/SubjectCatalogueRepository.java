package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SubjectCatalogue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectCatalogueRepository extends JpaRepository<SubjectCatalogue, UUID> {
    List<SubjectCatalogue> findByActiveTrueOrderByPhaseAscNameAsc();
}
