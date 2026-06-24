package com.edurite.ai.university;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UniversityCrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(UniversityCrawlScheduler.class);

    private final UniversityCrawlOrchestrator orchestrator;

    public UniversityCrawlScheduler(UniversityCrawlOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(cron = "${edurite.university.crawl.cron:0 0 2 * * *}")
    public void runScheduledCrawl() {
        UniversityCrawlSummary summary = orchestrator.crawlAllActiveUniversities();
        log.info("University crawl summary: universitiesProcessed={}, seedUrlsProcessed={}, pagesDiscovered={}, pagesSaved={}, failures={}, durationMs={}",
                summary.universitiesProcessed(),
                summary.seedUrlsProcessed(),
                summary.pagesDiscovered(),
                summary.pagesSaved(),
                summary.failures(),
                summary.durationMs());
    }
}

