package com.edurite.ai.research;

import com.edurite.background.DistributedJobLockService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ResearchCatalogueScheduler {
    private final OfficialCatalogueResearchService research;
    private final DistributedJobLockService locks;
    private final boolean enabled;
    private int country;

    public ResearchCatalogueScheduler(
            OfficialCatalogueResearchService research,
            DistributedJobLockService locks,
            @Value("${edurite.career-research.refresh-enabled:false}") boolean enabled) {
        this.research = research;
        this.locks = locks;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${edurite.career-research.refresh-delay-ms:7200000}")
    public void refresh() {
        if (enabled)
            locks.runOnce(
                    "career-research.catalogue-refresh",
                    Duration.ofMinutes(5),
                    () ->
                            research.refreshOne(
                                    java.util.List.of("South Africa", "Botswana", "Zimbabwe")
                                            .get(country++ % 3)));
    }
}
