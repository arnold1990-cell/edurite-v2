package com.edurite.compliance.repository;

import com.edurite.compliance.entity.ConsentRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByUserIdOrderByAcceptedAtDesc(UUID userId);
}

