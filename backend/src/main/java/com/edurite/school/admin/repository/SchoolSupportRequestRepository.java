package com.edurite.school.admin.repository;

import com.edurite.school.admin.entity.SchoolSupportRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolSupportRequestRepository extends JpaRepository<SchoolSupportRequest, UUID> {
    List<SchoolSupportRequest> findBySchoolIdAndActiveTrueOrderByCreatedAtDesc(UUID schoolId);
    List<SchoolSupportRequest> findBySchoolIdInAndActiveTrueOrderByCreatedAtDesc(List<UUID> schoolIds);
}
