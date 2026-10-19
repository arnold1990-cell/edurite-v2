package com.edurite.ai.research;

import com.edurite.ai.university.*;
import com.edurite.background.DistributedJobLockService;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.institution.universityinfo.service.UniversityInfoService;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared scheduled/on-demand pipeline. Budgets apply per institution, never to the registry's size. */
@Service
public class OfficialCatalogueResearchService {
    private final UniversitySourceRegistryService registry;
    private final MultiUniversityPageFetcherService fetcher;
    private final InstitutionRepository institutions;
    private final CrawledUniversityPageRepository pages;
    private final UniversityInfoService info;
    private final DistributedJobLockService locks;
    private final UniversityCrawlAttemptRepository attempts;
    private final AtomicInteger cursor=new AtomicInteger();
    public record RefreshOutcome(int discovered, int fetched, int failed, String message) {}

    public OfficialCatalogueResearchService(UniversitySourceRegistryService registry, MultiUniversityPageFetcherService fetcher,
            InstitutionRepository institutions, CrawledUniversityPageRepository pages, UniversityInfoService info, DistributedJobLockService locks,
            UniversityCrawlAttemptRepository attempts) {
        this.registry=registry;this.fetcher=fetcher;this.institutions=institutions;this.pages=pages;this.info=info;this.locks=locks;this.attempts=attempts;
    }

    public List<String> refreshOne(String preferredCountry) {
        var entries=registry.getActiveUniversities().stream().filter(e -> preferredCountry==null || preferredCountry.isBlank()
                || preferredCountry.equals(country(e.getBaseDomain()))).toList();
        if(entries.isEmpty()) return List.of("No approved institution sources are configured for this country.");
        int start=Math.floorMod(cursor.getAndIncrement(), entries.size());
        for(int i=0;i<entries.size();i++) {
            var outcome=refreshInstitution(entries.get((start+i)%entries.size()));
            if(!outcome.message().startsWith("No source")) return List.of(outcome.message());
        }
        return List.of("No stale official institution source was due within this country's refresh budget.");
    }

    public RefreshOutcome refreshInstitution(UniversityRegistryProperties.UniversityRegistryEntry entry) {
        var existing=pages.findByUniversityNameIgnoreCaseAndActiveTrue(entry.getUniversityName());
        var known=new LinkedHashMap<String,CrawledUniversityPage>();
        existing.forEach(p -> known.put(p.getSourceUrl(),p));
        var seeds=new LinkedHashSet<>(entry.getSeedUrls());
        seeds.addAll(entry.getFacultyWebsites()); seeds.addAll(entry.getProgrammeCatalogueUrls());
        if(entry.getAdmissionsWebsite()!=null) seeds.add(entry.getAdmissionsWebsite());
        var normalizer=new UniversityUrlNormalizer();
        var normalizedSeeds=seeds.stream().map(normalizer::normalize).filter(url -> !url.isBlank()).toList();
        seeds.clear();seeds.addAll(normalizedSeeds);
        boolean seedDue=seeds.stream().anyMatch(url -> due(known.get(url)));
        if(!seedDue && existing.stream().noneMatch(OfficialCatalogueResearchService::due))
            return new RefreshOutcome(0,0,0,"No source is due for "+entry.getUniversityName());
        var outcome=new RefreshOutcome[]{new RefreshOutcome(0,0,0,entry.getUniversityName()+": catalogue refresh already running or distributed lock unavailable; cached records retained.")};
        locks.runOnce("university.research."+entry.getBaseDomain(),Duration.ofMinutes(30),()-> {
            var current=pages.findByUniversityNameIgnoreCaseAndActiveTrue(entry.getUniversityName());
            known.clear(); current.forEach(p -> known.put(p.getSourceUrl(),p));
            if(current.isEmpty()) seeds.addAll(fetcher.discoverCandidateUrls(entry,24));
            var candidates=new LinkedHashSet<>(seeds); candidates.addAll(known.keySet());
            int budget=Math.max(1,fetcher.maxFetchBudgetPerUniversity());
            var dueUrls=candidates.stream().filter(url -> registry.isAllowedUrlForUniversity(entry.getUniversityName(),url))
                    .filter(url -> due(known.get(url)))
                    .sorted(Comparator.comparing(url -> Optional.ofNullable(known.get(url)).map(CrawledUniversityPage::getLastCrawledAt).orElse(OffsetDateTime.MIN)))
                    .limit(budget).toList();
            var fetched=fetcher.fetchPages(dueUrls);
            // Keep redirect aliases on the same freshness schedule as their terminal pages.
            for(int index=0;index<Math.min(dueUrls.size(),fetched.size());index++) {
                var terminal=fetched.get(index);String requested=dueUrls.get(index);
                if(terminal.success() && !requested.equals(terminal.sourceUrl())) {
                    var alias=known.getOrDefault(requested,new CrawledUniversityPage());
                    alias.setUniversityName(entry.getUniversityName());alias.setSourceUrl(requested);alias.setActive(true);
                    alias.setCrawlStatus(CrawlStatus.SKIPPED);alias.setLastCrawledAt(OffsetDateTime.now());
                    alias.setLastSuccessfulCrawledAt(OffsetDateTime.now());pages.save(alias);known.put(requested,alias);
                }
            }
            for(var page:fetched) {
                var attempt=new com.edurite.ai.university.entity.UniversityCrawlAttempt();
                attempt.setUniversityName(entry.getUniversityName());attempt.setSourceUrl(page.sourceUrl());
                attempt.setStatus(page.success()?"SUCCESS":page.failureType()==null?"FETCH_ERROR":page.failureType().name());
                String reason=page.failureReason();attempt.setReason(reason==null?null:reason.substring(0,Math.min(500,reason.length())));
                attempts.save(attempt);
            }
            var institution=institutions.findByNameIgnoreCase(entry.getUniversityName()).orElse(null);
            if(institution==null && fetched.stream().anyMatch(UniversitySourcePageResult::success)) {
                institution=new Institution();institution.setName(entry.getUniversityName());institution.setCountry(country(entry.getBaseDomain()));
                institution.setWebsite(entry.getOfficialWebsite()==null?"https://www."+entry.getBaseDomain():entry.getOfficialWebsite());
                institution.setProvince(entry.getProvince());institution.setCategory("PRIVATE_HIGHER_EDUCATION".equals(entry.getInstitutionType())?"PRIVATE_HIGHER_EDUCATION":"UNIVERSITY");
                institution.setActive(true);institution=institutions.save(institution);
            }
            if(institution!=null) info.ingestOfficialPages(institution,fetched);
            else for(var result:fetched) recordFailure(entry,result);
            var frontier=new LinkedHashSet<>(candidates);
            for(var page:fetched) if(page.success()) page.officialLinks().forEach((url,label)-> {
                if(label.toLowerCase(Locale.ROOT).matches(".*(facult|programme|program|qualification|course|undergraduate|postgraduate|admission|fees|funding|bursar|scholarship).*")) frontier.add(url);
            });
            for(var page:fetched) if(page.success()) registry.observeOfficialLinks(entry,page.officialLinks());
            int discovered=0;
            for(var url:frontier) {
                if(known.size()+discovered>=2000) break;
                if(url.length()>1200 || !registry.isAllowedUrlForUniversity(entry.getUniversityName(),url)
                        || fetched.stream().anyMatch(p -> p.sourceUrl().equals(url)) || known.containsKey(url)) continue;
                var queued=new CrawledUniversityPage();queued.setUniversityName(entry.getUniversityName());queued.setSourceUrl(url);
                queued.setCrawlStatus(CrawlStatus.SKIPPED);queued.setActive(true);pages.save(queued);discovered++;
            }
            int failures=(int)fetched.stream().filter(p -> !p.success()).count();
            outcome[0]=new RefreshOutcome(discovered,fetched.size(),failures,entry.getUniversityName()+": "+(fetched.size()-failures)
                    +" public pages retrieved; "+failures+" unavailable or restricted; "+discovered+" newly queued.");
        });
        return outcome[0];
    }

    public static boolean due(CrawledUniversityPage page) {
        if(page==null || page.getLastCrawledAt()==null) return true;
        if(page.getLastFailureAt()!=null && page.getLastFailureAt().isAfter(OffsetDateTime.now().minusHours(12))) return false;
        return page.getLastSuccessfulCrawledAt()==null || page.getLastSuccessfulCrawledAt().isBefore(OffsetDateTime.now().minusDays(14));
    }
    private void recordFailure(UniversityRegistryProperties.UniversityRegistryEntry entry,UniversitySourcePageResult result) {
        var attempt=pages.findBySourceUrl(result.sourceUrl()).orElseGet(CrawledUniversityPage::new);
        attempt.setUniversityName(entry.getUniversityName());attempt.setSourceUrl(result.sourceUrl());attempt.setCrawlStatus(CrawlStatus.FAILED);
        attempt.setLastCrawledAt(OffsetDateTime.now());attempt.setLastFailureAt(OffsetDateTime.now());
        String reason=result.failureReason();attempt.setFailureReason(reason==null?null:reason.substring(0,Math.min(500,reason.length())));
        attempt.setErrorType(result.failureType()==null?"FETCH_ERROR":result.failureType().name());attempt.setPageType(result.pageType().name());attempt.setActive(true);pages.save(attempt);
    }
    public static String country(String host) {
        return host.endsWith(".bw")?"Botswana":host.endsWith(".zw")?"Zimbabwe":host.endsWith(".za")?"South Africa":"Not recorded";
    }
}
