package com.edurite.roadmap.repository;

import com.edurite.roadmap.entity.CareerProgramRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerProgramRequirementRepository extends JpaRepository<CareerProgramRequirement, UUID> {

    List<CareerProgramRequirement> findByCareerNameContainingIgnoreCaseOrderByVerifiedDescApsRequiredAscInstitutionNameAsc(String careerName);
}
