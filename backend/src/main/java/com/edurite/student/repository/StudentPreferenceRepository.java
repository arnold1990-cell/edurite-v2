package com.edurite.student.repository;

import com.edurite.student.entity.StudentPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPreferenceRepository extends JpaRepository<StudentPreference, UUID> {
    Optional<StudentPreference> findByStudentId(UUID studentId);
}

