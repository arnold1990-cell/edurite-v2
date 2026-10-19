package com.edurite.ai.university;

import com.edurite.ai.university.entity.UniversityDirectoryRecord;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityDirectoryRepository extends JpaRepository<UniversityDirectoryRecord, UUID> {
    Optional<UniversityDirectoryRecord> findByUniversityName(String universityName);
}
