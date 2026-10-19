package com.edurite.ai.research;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.edurite.ai.university.*;
import com.edurite.background.DistributedJobLockService;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.institution.universityinfo.service.UniversityInfoService;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

class OfficialCatalogueResearchServiceTest {
    private final UniversitySourceRegistryService registry =
            mock(UniversitySourceRegistryService.class);
    private final MultiUniversityPageFetcherService fetcher =
            mock(MultiUniversityPageFetcherService.class);
    private final InstitutionRepository institutions = mock(InstitutionRepository.class);
    private final CrawledUniversityPageRepository pages =
            mock(CrawledUniversityPageRepository.class);
    private final UniversityInfoService info = mock(UniversityInfoService.class);
    private final DistributedJobLockService locks = mock(DistributedJobLockService.class);
    private final OfficialCatalogueResearchService service =
            new OfficialCatalogueResearchService(
                    registry, fetcher, institutions, pages, info, locks);

    private UniversityRegistryProperties.UniversityRegistryEntry entry(String name, String host) {
        var e = new UniversityRegistryProperties.UniversityRegistryEntry();
        e.setUniversityName(name);
        e.setBaseDomain(host);
        e.setSeedUrls(List.of("https://" + host + "/study"));
        return e;
    }

    @Test
    void freshCatalogueAndFailureBackoffPreventRepeatedRequests() {
        var entry = entry("University", "official.ac.bw");
        when(registry.getActiveUniversities()).thenReturn(List.of(entry));
        var cached = new CrawledUniversityPage();
        cached.setLastSuccessfulCrawledAt(OffsetDateTime.now());
        when(pages
                        .findTop10ByUniversityNameIgnoreCaseAndActiveTrueOrderByLastSuccessfulCrawledAtDesc(
                                "University"))
                .thenReturn(List.of(cached));
        service.refreshOne("Botswana");
        verifyNoInteractions(fetcher, locks);
        cached.setLastSuccessfulCrawledAt(null);
        cached.setLastFailureAt(OffsetDateTime.now());
        service.refreshOne("Botswana");
        verifyNoInteractions(fetcher, locks);
    }

    @Test
    void failedNewInstitutionAttemptIsRecordedWithoutInventingAnInstitutionOrProgramme() {
        var entry = entry("University", "official.ac.bw");
        when(registry.getActiveUniversities())
                .thenReturn(List.of(entry, entry("Other", "other.ac.bw")));
        when(locks.runOnce(anyString(), any(), any()))
                .thenAnswer(
                        i -> {
                            i.getArgument(2, Runnable.class).run();
                            return true;
                        });
        when(registry.isAllowedUrlForUniversity(anyString(), anyString())).thenReturn(true);
        when(fetcher.fetchPages(any()))
                .thenReturn(
                        List.of(
                                new UniversitySourcePageResult(
                                        "https://official.ac.bw/study",
                                        "",
                                        UniversityPageType.UNKNOWN,
                                        "",
                                        Set.of(),
                                        false,
                                        "Robots denied",
                                        UniversityCrawlFailureType.ACCESS_DENIED)));
        var outcome = service.refreshOne("Botswana");
        assertThat(outcome).anyMatch(s -> s.contains("0 public pages"));
        verify(fetcher, times(1)).fetchPages(any());
        verify(institutions, never()).save(any());
        verifyNoInteractions(info);
        verify(pages)
                .save(
                        argThat(
                                p ->
                                        p.getCrawlStatus() == CrawlStatus.FAILED
                                                && p.getLastFailureAt() != null));
    }

    @Test
    void unavailableDistributedLockFailsClosedWithoutNetworkRequests() {
        when(registry.getActiveUniversities())
                .thenReturn(List.of(entry("University", "official.ac.zw")));
        assertThat(service.refreshOne("Zimbabwe")).anyMatch(s -> s.contains("lock unavailable"));
        verifyNoInteractions(fetcher, info);
    }

    @Test
    void scheduledRefreshIsDisabledUnlessConfiguredAndRotatesCountryWithOneRefreshPerRun() {
        var target = mock(OfficialCatalogueResearchService.class);
        new ResearchCatalogueScheduler(target, locks, false).refresh();
        verifyNoInteractions(target, locks);
        when(locks.runOnce(anyString(), any(), any()))
                .thenAnswer(
                        i -> {
                            i.getArgument(2, Runnable.class).run();
                            return true;
                        });
        var scheduler = new ResearchCatalogueScheduler(target, locks, true);
        scheduler.refresh();
        scheduler.refresh();
        scheduler.refresh();
        verify(target).refreshOne("South Africa");
        verify(target).refreshOne("Botswana");
        verify(target).refreshOne("Zimbabwe");
        verifyNoMoreInteractions(target);
    }
}
