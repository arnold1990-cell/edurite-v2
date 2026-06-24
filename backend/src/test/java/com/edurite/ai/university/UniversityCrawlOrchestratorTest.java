package com.edurite.ai.university;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edurite.ai.university.UniversityPageType.PROGRAMME_DETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityCrawlOrchestratorTest {

    @Mock
    private UniversitySourceRegistryService registryService;
    @Mock
    private MultiUniversityPageFetcherService fetcherService;
    @Mock
    private CrawledUniversityPageRepository repository;

    @InjectMocks
    private UniversityCrawlOrchestrator orchestrator;

    @Test
    void crawlProcessesLargeUniversitySetWithoutStoppingOnFailures() {
        List<UniversityRegistryProperties.UniversityRegistryEntry> universities = new ArrayList<>();
        for (int index = 1; index <= 50; index++) {
            UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
            entry.setUniversityName("University " + index);
            entry.setSeedUrls(List.of("https://www.university-" + index + ".ac.za/programmes"));
            universities.add(entry);
        }

        when(registryService.getActiveUniversities()).thenReturn(universities);
        when(fetcherService.discoverCandidateUrls(any(), any(Integer.class))).thenAnswer(invocation -> List.of(invocation.getArgument(0, UniversityRegistryProperties.UniversityRegistryEntry.class).getSeedUrls().get(0)));
        when(fetcherService.fetchPages(any())).thenReturn(List.of(
                new UniversitySourcePageResult("https://www.university-1.ac.za/programmes", "Programmes", PROGRAMME_DETAIL, "text", Set.of("software"), true, null, null),
                new UniversitySourcePageResult("https://www.university-1.ac.za/forbidden", "", UniversityPageType.UNKNOWN, "", Set.of(), false, "forbidden", UniversityCrawlFailureType.ACCESS_DENIED)
        ));
        when(repository.findBySourceUrl(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UniversityCrawlSummary summary = orchestrator.crawlAllActiveUniversities();

        assertThat(summary.universitiesProcessed()).isEqualTo(50);
        assertThat(summary.pagesSaved()).isEqualTo(100);
        assertThat(summary.failures()).isEqualTo(50);
    }
}

