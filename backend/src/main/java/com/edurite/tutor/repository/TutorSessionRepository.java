package com.edurite.tutor.repository;

import com.edurite.tutor.entity.TutorSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorSessionRepository extends JpaRepository<TutorSession, UUID> {
    List<TutorSession> findByStudentIdOrderByUpdatedAtDesc(UUID studentId);
    Optional<TutorSession> findByIdAndStudentId(UUID id, UUID studentId);
    long countByStudentId(UUID studentId);
}

