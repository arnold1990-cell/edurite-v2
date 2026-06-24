package com.edurite.ai.university;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversityCrawlOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(UniversityCrawlOrchestrator.class);

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final CrawledUniversityPageRepository repository;

    public UniversityCrawlOrchestrator(UniversitySourceRegistryService registryService,
                                       MultiUniversityPageFetcherService pageFetcherService,
                                       CrawledUniversityPageRepository repository) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.repository = repository;
    }

    public UniversityCrawlSummary crawlAllActiveUniversities() {
        long startedAt = System.currentTimeMillis();
        int universitiesProcessed = 0;
        int seedUrlsProcessed = 0;
        int pagesDiscovered = 0;
        int pagesSaved = 0;
        int failures = 0;

        for (UniversityRegistryProperties.UniversityRegistryEntry university : registryService.getActiveUniversities()) {
            universitiesProcessed++;
            try {
                List<String> discovered = pageFetcherService.discoverCandidateUrls(university, 40);
                seedUrlsProcessed += university.getSeedUrls().size();
                pagesDiscovered += discovered.size();

                for (UniversitySourcePageResult page : pageFetcherService.fetchPages(discovered)) {
                    pagesSaved++;
                    upsert(university.getUniversityName(), page);
                    if (!page.success()) {
                        failures++;
                    }
                }
            } catch (Exception ex) {
                failures++;
                log.warn("Crawl failed for university={}: {}", university.getUniversityName(), ex.getMessage());
            }
        }

        return new UniversityCrawlSummary(universitiesProcessed, seedUrlsProcessed, pagesDiscovered,
                pagesSaved, failures, System.currentTimeMillis() - startedAt);
    }

    private void upsert(String universityName, UniversitySourcePageResult pageResult) {
        CrawledUniversityPage page = repository.findBySourceUrl(pageResult.sourceUrl()).orElseGet(CrawledUniversityPage::new);
        page.setUniversityName(universityName);
        page.setSourceUrl(pageResult.sourceUrl());
        page.setPageTitle(pageResult.pageTitle());
        page.setPageType(pageResult.pageType().name());
        page.setExtractedKeywords(pageResult.extractedKeywords());
        page.setCleanedContent(pageResult.cleanedText());
        page.setSummaryExcerpt(buildSummary(pageResult.cleanedText()));
        page.setLastCrawledAt(OffsetDateTime.now());
        if (pageResult.success()) {
            page.setCrawlStatus(CrawlStatus.SUCCESS);
            page.setLastSuccessfulCrawledAt(OffsetDateTime.now());
            page.setFailureReason(null);
            page.setErrorType(null);
        } else {
            page.setCrawlStatus(CrawlStatus.FAILED);
            page.setFailureReason(pageResult.failureReason());
            page.setErrorType(pageResult.failureType() == null ? UniversityCrawlFailureType.FETCH_ERROR.name() : pageResult.failureType().name());
            page.setLastFailureAt(OffsetDateTime.now());
        }
        repository.save(page);
    }

    private String buildSummary(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 320 ? normalized : normalized.substring(0, 320);
    }
}

