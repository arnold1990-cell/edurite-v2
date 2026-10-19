package com.edurite.ai.university;
import com.edurite.ai.university.entity.UniversityCrawlAttempt;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UniversityCrawlAttemptRepository extends JpaRepository<UniversityCrawlAttempt,UUID> {
    List<UniversityCrawlAttempt> findTop100ByUniversityNameIgnoreCaseOrderByCreatedAtDesc(String universityName);
}
