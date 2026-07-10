package com.edurite.ai.university;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawledUniversityPageRepository extends JpaRepository<CrawledUniversityPage, java.util.UUID> {

    Optional<CrawledUniversityPage> findBySourceUrl(String sourceUrl);

    List<CrawledUniversityPage> findByActiveTrueAndCrawlStatus(CrawlStatus crawlStatus);

    List<CrawledUniversityPage> findTop10ByUniversityNameIgnoreCaseAndActiveTrueOrderByLastSuccessfulCrawledAtDesc(String universityName);

    long countByCrawlStatus(CrawlStatus crawlStatus);
}
