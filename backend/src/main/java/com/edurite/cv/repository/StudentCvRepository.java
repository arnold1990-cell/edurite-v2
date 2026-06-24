package com.edurite.cv.repository;

import com.edurite.cv.entity.StudentCv;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCvRepository extends JpaRepository<StudentCv, UUID> {
    Optional<StudentCv> findByStudentId(UUID studentId);
}

