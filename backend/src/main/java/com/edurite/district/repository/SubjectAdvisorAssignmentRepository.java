package com.edurite.district.repository;

import com.edurite.district.entity.SubjectAdvisorAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectAdvisorAssignmentRepository extends JpaRepository<SubjectAdvisorAssignment, UUID> {
    List<SubjectAdvisorAssignment> findByDistrictIdAndActiveTrueOrderBySubjectAscGradeAsc(UUID districtId);
    List<SubjectAdvisorAssignment> findByAdvisorUserIdAndActiveTrue(UUID advisorUserId);
}
