package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.PlagiarismReport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlagiarismReportRepository extends JpaRepository<PlagiarismReport, UUID> {
    List<PlagiarismReport> findBySubmissionId(UUID submissionId);
}



