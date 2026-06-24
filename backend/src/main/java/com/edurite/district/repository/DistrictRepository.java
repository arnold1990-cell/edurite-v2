package com.edurite.district.repository;

import com.edurite.district.entity.District;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, UUID> {
    Optional<District> findByIdAndActiveTrue(UUID id);
    Optional<District> findByDistrictCodeIgnoreCase(String districtCode);
    List<District> findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(UUID provinceId);
    List<District> findByActiveTrueOrderByDistrictNameAsc();
}
