package com.edurite.institution.universityinfo.repository;

import com.edurite.institution.universityinfo.entity.UniversityAdmissionRequirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityAdmissionRequirementRepository extends JpaRepository<UniversityAdmissionRequirement, UUID> {

    List<UniversityAdmissionRequirement> findByInstitutionIdAndActiveTrueOrderByProgrammeNameAscRequirementTitleAsc(UUID institutionId);

    Optional<UniversityAdmissionRequirement> findByInstitutionIdAndProgrammeNameIgnoreCaseAndSourceUrlAndRequirementTitleIgnoreCase(
            UUID institutionId,
            String programmeName,
            String sourceUrl,
            String requirementTitle
    );

    void deleteByInstitutionId(UUID institutionId);
}
