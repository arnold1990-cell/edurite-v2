package com.edurite.district.repository;

import com.edurite.district.entity.DistrictAdminProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictAdminProfileRepository extends JpaRepository<DistrictAdminProfile, UUID> {
    Optional<DistrictAdminProfile> findByUserIdAndDeletedFalse(UUID userId);

    List<DistrictAdminProfile> findByDistrictIdAndActiveTrueAndDeletedFalse(UUID districtId);
}
