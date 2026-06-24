package com.edurite.ai.university;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceCoverageService {

    private final UniversitySourceRegistryService registryService;
    private final CrawledUniversityPageRepository repository;

    public UniversitySourceCoverageService(UniversitySourceRegistryService registryService,
                                           CrawledUniversityPageRepository repository) {
        this.registryService = registryService;
        this.repository = repository;
    }

    public UniversitySourceCoverage getCoverage() {
        return new UniversitySourceCoverage(
                registryService.configuredUniversityCount(),
                registryService.getActiveUniversities().size(),
                repository.count(),
                repository.countByCrawlStatus(CrawlStatus.SUCCESS),
                repository.countByCrawlStatus(CrawlStatus.FAILED),
                repository.findAll().stream()
                        .map(CrawledUniversityPage::getLastCrawledAt)
                        .filter(java.util.Objects::nonNull)
                        .max(OffsetDateTime::compareTo)
                        .orElse(null)
        );
    }
}

