package com.edurite.district.repository;

import com.edurite.district.entity.Circuit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CircuitRepository extends JpaRepository<Circuit, UUID> {
    List<Circuit> findByDistrictIdAndActiveTrueOrderByNameAsc(UUID districtId);
    Optional<Circuit> findByIdAndDistrictIdAndActiveTrue(UUID id, UUID districtId);
    Optional<Circuit> findByDistrictIdAndManagerUserIdAndActiveTrue(UUID districtId, UUID managerUserId);
}
