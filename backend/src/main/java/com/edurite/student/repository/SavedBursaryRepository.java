package com.edurite.student.repository;

import com.edurite.student.entity.SavedBursary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named SavedBursaryRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SavedBursaryRepository extends JpaRepository<SavedBursary, UUID> {
    long countByStudentId(UUID studentId);
    List<SavedBursary> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndBursaryId(UUID studentId, UUID bursaryId);
    void deleteByStudentIdAndBursaryId(UUID studentId, UUID bursaryId);
}

