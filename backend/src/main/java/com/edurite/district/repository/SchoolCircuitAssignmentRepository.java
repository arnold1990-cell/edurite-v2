package com.edurite.district.repository;

import com.edurite.district.entity.SchoolCircuitAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolCircuitAssignmentRepository extends JpaRepository<SchoolCircuitAssignment, UUID> {
    List<SchoolCircuitAssignment> findByCircuitIdIn(List<UUID> circuitIds);
    List<SchoolCircuitAssignment> findByCircuitId(UUID circuitId);
    List<SchoolCircuitAssignment> findBySchoolId(UUID schoolId);
}
