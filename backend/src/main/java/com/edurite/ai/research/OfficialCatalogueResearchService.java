package com.edurite.ai.research;

import com.edurite.ai.university.*;
import com.edurite.background.DistributedJobLockService;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.institution.universityinfo.service.UniversityInfoService;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/** Institution-specific discovery; student identity/results never enter discovery requests. */
@Service
public class OfficialCatalogueResearchService {
    private final UniversitySourceRegistryService registry;
    private final MultiUniversityPageFetcherService fetcher;
    private final InstitutionRepository institutions;
    private final CrawledUniversityPageRepository pages;
    private final UniversityInfoService info;
    private final DistributedJobLockService locks;

    public OfficialCatalogueResearchService(
            UniversitySourceRegistryService registry,
            MultiUniversityPageFetcherService fetcher,
            InstitutionRepository institutions,
            CrawledUniversityPageRepository pages,
            UniversityInfoService info,
            DistributedJobLockService locks) {
        this.registry = registry;
        this.fetcher = fetcher;
        this.institutions = institutions;
        this.pages = pages;
        this.info = info;
        this.locks = locks;
    }

    public List<String> refreshOne(String preferredCountry) {
        var entries =
                registry.getActiveUniversities().stream()
                        .filter(
                                e ->
                                        preferredCountry == null
                                                || preferredCountry.isBlank()
                                                || preferredCountry.equals(
                                                        country(e.getBaseDomain())))
                        .toList();
        for (var entry : entries) {
            var old =
                    pages
                            .findTop10ByUniversityNameIgnoreCaseAndActiveTrueOrderByLastSuccessfulCrawledAtDesc(
                                    entry.getUniversityName());
            if (old.stream()
                    .anyMatch(
                            p ->
                                    p.getLastSuccessfulCrawledAt() != null
                                            && p.getLastSuccessfulCrawledAt()
                                                    .isAfter(OffsetDateTime.now().minusDays(14))))
                continue;
            if (old.stream()
                    .anyMatch(
                            p ->
                                    p.getLastFailureAt() != null
                                            && p.getLastFailureAt()
                                                    .isAfter(OffsetDateTime.now().minusHours(12))))
                continue;
            List<String> outcome = new ArrayList<>();
            boolean acquired =
                    locks.runOnce(
                            "university.research." + entry.getBaseDomain(),
                            Duration.ofMinutes(3),
                            () -> {
                                LinkedHashSet<String> candidates =
                                        new LinkedHashSet<>(entry.getSeedUrls());
                                candidates.addAll(fetcher.discoverCandidateUrls(entry, 4));
                                var fetched =
                                        fetcher.fetchPages(
                                                candidates.stream()
                                                        .filter(
                                                                url ->
                                                                        registry
                                                                                .isAllowedUrlForUniversity(
                                                                                        entry
                                                                                                .getUniversityName(),
                                                                                        url))
                                                        .limit(4)
                                                        .toList());
                                var institution =
                                        institutions
                                                .findByNameIgnoreCase(entry.getUniversityName())
                                                .orElse(null);
                                if (institution == null
                                        && fetched.stream()
                                                .anyMatch(UniversitySourcePageResult::success)) {
                                    institution = new Institution();
                                    institution.setName(entry.getUniversityName());
                                    institution.setCountry(country(entry.getBaseDomain()));
                                    institution.setWebsite("https://www." + entry.getBaseDomain());
                                    institution.setActive(true);
                                    institution.setCategory("UNIVERSITY");
                                    institution = institutions.save(institution);
                                }
                                if (institution != null)
                                    info.ingestOfficialPages(institution, fetched);
                                else
                                    for (var result : fetched) {
                                        var attempt =
                                                pages.findBySourceUrl(result.sourceUrl())
                                                        .orElseGet(CrawledUniversityPage::new);
                                        attempt.setUniversityName(entry.getUniversityName());
                                        attempt.setSourceUrl(result.sourceUrl());
                                        attempt.setCrawlStatus(CrawlStatus.FAILED);
                                        attempt.setLastCrawledAt(OffsetDateTime.now());
                                        attempt.setLastFailureAt(OffsetDateTime.now());
                                        attempt.setFailureReason(result.failureReason());
                                        attempt.setErrorType(
                                                result.failureType() == null
                                                        ? "FETCH_ERROR"
                                                        : result.failureType().name());
                                        attempt.setPageType(result.pageType().name());
                                        attempt.setActive(true);
                                        pages.save(attempt);
                                    }
                                outcome.add(
                                        entry.getUniversityName()
                                                + ": "
                                                + fetched.stream()
                                                        .filter(UniversitySourcePageResult::success)
                                                        .count()
                                                + " public pages retrieved; "
                                                + fetched.stream().filter(p -> !p.success()).count()
                                                + " unavailable or restricted.");
                            });
            if (!acquired)
                outcome.add(
                        entry.getUniversityName()
                                + ": catalogue refresh already running or distributed lock"
                                + " unavailable; cached records retained.");
            return List.copyOf(outcome);
        }
        return List.of(
                "No stale official institution source was due within this country's refresh"
                        + " budget.");
    }

    public static String country(String host) {
        return host.endsWith(".bw")
                ? "Botswana"
                : host.endsWith(".zw")
                        ? "Zimbabwe"
                        : host.endsWith(".za") ? "South Africa" : "Not recorded";
    }
}
