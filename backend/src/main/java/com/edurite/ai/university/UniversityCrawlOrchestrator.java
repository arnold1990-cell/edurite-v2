package com.edurite.ai.university;

import com.edurite.ai.research.OfficialCatalogueResearchService;
import org.springframework.stereotype.Service;

@Service
public class UniversityCrawlOrchestrator {
    private final UniversitySourceRegistryService registry;
    private final OfficialCatalogueResearchService research;
    public UniversityCrawlOrchestrator(UniversitySourceRegistryService registry,OfficialCatalogueResearchService research) {
        this.registry=registry;this.research=research;
    }
    public UniversityCrawlSummary crawlAllActiveUniversities() {
        long start=System.currentTimeMillis();int processed=0,seeds=0,discovered=0,saved=0,failed=0;
        for(var entry:registry.getActiveUniversities()) {
            if(Thread.currentThread().isInterrupted()) break;
            processed++;seeds+=entry.getSeedUrls().size();
            try {
                var outcome=research.refreshInstitution(entry);
                discovered+=outcome.discovered();saved+=outcome.fetched();failed+=outcome.failed();
            } catch(RuntimeException ex) { failed++; }
        }
        return new UniversityCrawlSummary(processed,seeds,discovered,saved,failed,System.currentTimeMillis()-start);
    }
}
