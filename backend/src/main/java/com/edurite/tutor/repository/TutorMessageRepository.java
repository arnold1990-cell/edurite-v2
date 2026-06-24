package com.edurite.tutor.repository;

import com.edurite.tutor.entity.TutorMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorMessageRepository extends JpaRepository<TutorMessage, UUID> {
    List<TutorMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}

