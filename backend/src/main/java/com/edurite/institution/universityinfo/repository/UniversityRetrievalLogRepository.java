package com.edurite.institution.universityinfo.repository;

import com.edurite.institution.universityinfo.entity.UniversityRetrievalLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRetrievalLogRepository extends JpaRepository<UniversityRetrievalLog, UUID> {

    List<UniversityRetrievalLog> findTop10ByInstitutionIdOrderByRetrievedAtDesc(UUID institutionId);
}
