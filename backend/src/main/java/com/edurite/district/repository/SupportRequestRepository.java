package com.edurite.district.repository;

import com.edurite.district.entity.SupportRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {
    List<SupportRequest> findBySchoolIdInOrderByCreatedAtDesc(List<UUID> schoolIds);
    List<SupportRequest> findByAssignedToOrderByCreatedAtDesc(UUID assignedTo);
    List<SupportRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
}
