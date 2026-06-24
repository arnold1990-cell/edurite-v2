package com.edurite.district.repository;

import com.edurite.district.entity.DistrictIntervention;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictInterventionRepository extends JpaRepository<DistrictIntervention, UUID> {
    List<DistrictIntervention> findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(UUID districtId);
}
