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
    private final UniversityCrawlAttemptRepository attempts=mock(UniversityCrawlAttemptRepository.class);
    private final OfficialCatalogueResearchService service =
            new OfficialCatalogueResearchService(
                    registry, fetcher, institutions, pages, info, locks, attempts);

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
        cached.setSourceUrl(entry.getSeedUrls().get(0));
        cached.setLastCrawledAt(OffsetDateTime.now());
        cached.setLastSuccessfulCrawledAt(OffsetDateTime.now());
        when(pages
                        .findByUniversityNameIgnoreCaseAndActiveTrue(
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
    void freshHomepageDoesNotStarveQueuedProgrammePagesAndFrontierStaysOfficial() {
        var entry=entry("University","official.ac.za");
        var home=new CrawledUniversityPage();home.setSourceUrl(entry.getSeedUrls().get(0));
        home.setLastCrawledAt(OffsetDateTime.now());home.setLastSuccessfulCrawledAt(OffsetDateTime.now());
        var queued=new CrawledUniversityPage();queued.setSourceUrl("https://official.ac.za/accounting");
        when(pages.findByUniversityNameIgnoreCaseAndActiveTrue("University")).thenReturn(List.of(home,queued));
        when(locks.runOnce(anyString(),any(),any())).thenAnswer(i->{i.getArgument(2,Runnable.class).run();return true;});
        when(registry.isAllowedUrlForUniversity(eq("University"),anyString())).thenAnswer(i->i.getArgument(1,String.class).startsWith("https://official.ac.za/"));
        when(fetcher.maxFetchBudgetPerUniversity()).thenReturn(4);
        when(fetcher.fetchPages(any())).thenReturn(List.of(new UniversitySourcePageResult(queued.getSourceUrl(),"Bachelor of Accounting",UniversityPageType.PROGRAMME_DETAIL,
                "Entry requirements",Set.of(),List.of("Bachelor of Accounting"),true,null,null,
                Map.of("https://official.ac.za/postgraduate","Postgraduate programmes","https://evil.example/programmes","Programmes"))));
        when(institutions.findByNameIgnoreCase("University")).thenReturn(Optional.of(new com.edurite.institution.entity.Institution()));
        var result=service.refreshInstitution(entry);
        verify(fetcher).fetchPages(List.of(queued.getSourceUrl()));
        verify(fetcher,never()).discoverCandidateUrls(any(),anyInt());
        assertThat(result.discovered()).isEqualTo(1);
        verify(pages).save(argThat(p->"https://official.ac.za/postgraduate".equals(p.getSourceUrl()) && p.getLastCrawledAt()==null));
        verify(attempts).save(argThat(a->a.getStatus().equals("SUCCESS")));
        verify(info).ingestOfficialPages(any(),any());
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

    @Test void redirectAliasesDoNotCauseRepeatedFreshPageRequests() {
        var entry=entry("University","official.ac.za");
        Map<String,CrawledUniversityPage> cache=new LinkedHashMap<>();
        when(pages.findByUniversityNameIgnoreCaseAndActiveTrue("University")).thenAnswer(i->List.copyOf(cache.values()));
        when(pages.save(any())).thenAnswer(i->{var p=i.getArgument(0,CrawledUniversityPage.class);cache.put(p.getSourceUrl(),p);return p;});
        when(registry.isAllowedUrlForUniversity(anyString(),anyString())).thenReturn(true);
        when(locks.runOnce(anyString(),any(),any())).thenAnswer(i->{i.getArgument(2,Runnable.class).run();return true;});
        when(fetcher.fetchPages(any())).thenReturn(List.of(new UniversitySourcePageResult("https://official.ac.za/current-study",
                "Study programmes",UniversityPageType.QUALIFICATION_LIST,"Published catalogue",Set.of(),true,null,null)));
        when(institutions.findByNameIgnoreCase("University")).thenReturn(Optional.of(new com.edurite.institution.entity.Institution()));
        service.refreshInstitution(entry);
        assertThat(service.refreshInstitution(entry).fetched()).isZero();
        verify(fetcher,times(1)).fetchPages(any());
    }
}
