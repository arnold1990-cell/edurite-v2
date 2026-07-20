package com.edurite.student.repository;

import com.edurite.student.entity.StudentProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/**
 * This interface named StudentProfileRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sp from StudentProfile sp where sp.userId = :userId")
    Optional<StudentProfile> findByUserIdForUpdate(@Param("userId") UUID userId);
}

