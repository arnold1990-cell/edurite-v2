package com.edurite.school.admin.repository;

import com.edurite.school.admin.entity.SchoolAnnouncement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolAnnouncementRepository extends JpaRepository<SchoolAnnouncement, UUID> {
    List<SchoolAnnouncement> findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(UUID schoolId);
}
