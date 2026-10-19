package com.edurite.ai.university;

import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/ai/university-intelligence", "/api/v1/ai/university-intelligence"})
public class UniversityIntelligenceController {
    private final UniversitySourceRegistryService registry;
    private final CrawledUniversityPageRepository pages;
    private final UniversityProgrammeRepository programmes;
    private final GovernmentUniversityDirectoryService directory;
    public UniversityIntelligenceController(UniversitySourceRegistryService registry, CrawledUniversityPageRepository pages,
            UniversityProgrammeRepository programmes, GovernmentUniversityDirectoryService directory) {
        this.registry=registry;this.pages=pages;this.programmes=programmes;this.directory=directory;
    }
    public record SourceStatus(String url,String status,String lastChecked,String reason) {}
    public record CoverageRow(UniversityRegistryProperties.UniversityRegistryEntry institution,long indexedProgrammes,
            long retrievedPages,long queuedPages,List<SourceStatus> unavailableSources,String status) {}
    public record Coverage(String generatedAt,int publicInstitutions,String directoryStatus,List<CoverageRow> institutions,String limitation) {}

    @GetMapping("/coverage")
    public Coverage coverage() {
        List<CoverageRow> rows=new ArrayList<>();
        for(var entry:registry.getRegistryEntries()) {
            if(!"South Africa".equals(entry.getCountry())) continue;
            var crawl=pages.findByUniversityNameIgnoreCaseAndActiveTrue(entry.getUniversityName());
            long retrieved=crawl.stream().filter(p -> p.getCrawlStatus()==CrawlStatus.SUCCESS).count();
            var unavailable=crawl.stream().filter(p -> p.getCrawlStatus()==CrawlStatus.FAILED).map(p -> new SourceStatus(p.getSourceUrl(),
                    p.getErrorType(),p.getLastCrawledAt()==null?null:p.getLastCrawledAt().toString(),p.getFailureReason())).toList();
            rows.add(new CoverageRow(entry,programmes.countByInstitution_NameIgnoreCaseAndActiveTrue(entry.getUniversityName()),retrieved,
                    crawl.stream().filter(p -> p.getLastCrawledAt()==null).count(),unavailable,
                    retrieved==0?"Information not yet verified.":"Partial source coverage; full programme coverage not verified."));
        }
        return new Coverage(Instant.now().toString(),(int)rows.stream().filter(r -> "PUBLIC_ESTABLISHED".equals(r.institution().getRegistrationStatus())).count(),
                directory.lastOutcome(),List.copyOf(rows),"A complete public institution registry is not evidence of complete programme coverage. Admission is never guaranteed.");
    }

    @PostMapping("/refresh-directory")
    public Map<String,String> refreshDirectory() { return Map.of("status",directory.refresh()); }

    @PostMapping("/private-registration")
    public Map<String,String> verifyPrivate(@RequestBody GovernmentUniversityDirectoryService.PrivateRegistration request) {
        return Map.of("status",directory.verifyPrivateRegistration(request));
    }
}
