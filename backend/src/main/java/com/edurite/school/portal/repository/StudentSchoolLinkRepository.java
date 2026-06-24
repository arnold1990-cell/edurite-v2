package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.StudentSchoolLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentSchoolLinkRepository extends JpaRepository<StudentSchoolLink, UUID> {
    Optional<StudentSchoolLink> findByStudentId(UUID studentId);
    Optional<StudentSchoolLink> findByStudentIdAndStatusIgnoreCase(UUID studentId, String status);
    List<StudentSchoolLink> findBySchoolIdOrderByRequestedAtDesc(UUID schoolId);
    List<StudentSchoolLink> findBySchoolIdAndStatusIgnoreCaseOrderByRequestedAtDesc(UUID schoolId, String status);
    boolean existsByGeneratedUsernameIgnoreCase(String generatedUsername);
}
