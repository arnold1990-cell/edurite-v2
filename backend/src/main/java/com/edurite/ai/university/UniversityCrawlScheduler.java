package com.edurite.ai.university;

import com.edurite.background.DistributedJobLockService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UniversityCrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(UniversityCrawlScheduler.class);

    private final UniversityCrawlOrchestrator orchestrator;
    private final DistributedJobLockService jobLockService;

    public UniversityCrawlScheduler(UniversityCrawlOrchestrator orchestrator, DistributedJobLockService jobLockService) {
        this.orchestrator = orchestrator;
        this.jobLockService = jobLockService;
    }

    @Scheduled(cron = "${edurite.university.crawl.cron:0 0 2 * * *}")
    public void runScheduledCrawl() {
        jobLockService.runOnce("university.scheduled-crawl", Duration.ofHours(6), this::runScheduledCrawlOnce);
    }

    private void runScheduledCrawlOnce() {
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

