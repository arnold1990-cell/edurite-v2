package com.edurite.district.repository;

import com.edurite.district.entity.DistrictAnnouncement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictAnnouncementRepository extends JpaRepository<DistrictAnnouncement, UUID> {
    List<DistrictAnnouncement> findByDistrictIdAndActiveTrueOrderByCreatedAtDesc(UUID districtId);
}
