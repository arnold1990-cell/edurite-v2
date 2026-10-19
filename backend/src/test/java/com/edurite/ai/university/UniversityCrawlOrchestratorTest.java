package com.edurite.ai.university;

import com.edurite.ai.research.OfficialCatalogueResearchService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UniversityCrawlOrchestratorTest {
    @Test void scheduledCrawlIncludesEveryInstitutionAndContinuesAfterIndividualFailure() {
        var registry=mock(UniversitySourceRegistryService.class);
        var research=mock(OfficialCatalogueResearchService.class);
        var entries=new ArrayList<UniversityRegistryProperties.UniversityRegistryEntry>();
        for(int i=0;i<26;i++) {
            var entry=new UniversityRegistryProperties.UniversityRegistryEntry();
            entry.setUniversityName("Public University "+i);entry.setSeedUrls(List.of("https://u"+i+".ac.za/"));entries.add(entry);
        }
        when(registry.getActiveUniversities()).thenReturn(entries);
        when(research.refreshInstitution(any())).thenReturn(new OfficialCatalogueResearchService.RefreshOutcome(3,6,1,"partial"));
        when(research.refreshInstitution(entries.get(4))).thenThrow(new IllegalStateException("restricted"));
        var result=new UniversityCrawlOrchestrator(registry,research).crawlAllActiveUniversities();
        assertThat(result.universitiesProcessed()).isEqualTo(26);
        assertThat(result.pagesSaved()).isEqualTo(150);
        assertThat(result.failures()).isEqualTo(26);
        verify(research).refreshInstitution(entries.get(25));
    }
}
