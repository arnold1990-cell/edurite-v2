package com.edurite.institution.universityinfo.repository;

import com.edurite.institution.universityinfo.entity.UniversityProgramme;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityProgrammeRepository extends JpaRepository<UniversityProgramme, UUID> {

    List<UniversityProgramme> findByInstitutionIdAndActiveTrueOrderByFacultyAscNameAsc(UUID institutionId);

    Optional<UniversityProgramme> findByInstitutionIdAndNameIgnoreCaseAndSourceUrl(UUID institutionId, String name, String sourceUrl);

    void deleteByInstitutionId(UUID institutionId);
}
