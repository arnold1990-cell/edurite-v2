package com.edurite.universityapplication.repository;

import com.edurite.universityapplication.entity.UniversityApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityApplicationRepository extends JpaRepository<UniversityApplication, UUID> {
    List<UniversityApplication> findByStudentIdOrderByApplicationDeadlineAscCreatedAtDesc(UUID studentId);
    Optional<UniversityApplication> findByIdAndStudentId(UUID id, UUID studentId);
    long countByStudentId(UUID studentId);
    long countByStudentIdAndApplicationStatusIn(UUID studentId, List<String> statuses);
}

