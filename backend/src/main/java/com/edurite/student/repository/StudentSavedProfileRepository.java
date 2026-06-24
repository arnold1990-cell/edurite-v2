package com.edurite.student.repository;

import com.edurite.student.entity.StudentSavedProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentSavedProfileRepository extends JpaRepository<StudentSavedProfile, UUID> {
    List<StudentSavedProfile> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<StudentSavedProfile> findByIdAndUserId(UUID id, UUID userId);
}

