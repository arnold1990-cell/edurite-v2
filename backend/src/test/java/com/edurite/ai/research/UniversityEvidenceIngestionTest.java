package com.edurite.ai.research;

import com.edurite.ai.university.*;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.universityinfo.entity.*;
import com.edurite.institution.universityinfo.repository.*;
import com.edurite.institution.universityinfo.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UniversityEvidenceIngestionTest {
    @Test void unchangedPagesDoNotDuplicateProgrammesAndKeepSourceEvidence() {
        var registry=mock(UniversitySourceRegistryService.class);
        var pages=mock(CrawledUniversityPageRepository.class);
        var programmes=mock(UniversityProgrammeRepository.class);
        var requirements=mock(UniversityAdmissionRequirementRepository.class);
        var logs=mock(UniversityRetrievalLogRepository.class);
        var service=new UniversityInfoService(mock(UniversitySlugService.class),registry,mock(MultiUniversityPageFetcherService.class),pages,programmes,requirements,logs);
        var institution=new Institution();institution.setId(UUID.randomUUID());institution.setName("University");
        Map<String,CrawledUniversityPage> cache=new HashMap<>();
        when(registry.isAllowedUrlForUniversity(anyString(),anyString())).thenReturn(true);
        when(pages.findBySourceUrl(anyString())).thenAnswer(i->Optional.ofNullable(cache.get(i.getArgument(0,String.class))));
        when(pages.save(any())).thenAnswer(i->{var row=i.getArgument(0,CrawledUniversityPage.class);cache.put(row.getSourceUrl(),row);return row;});
        var page=new UniversitySourcePageResult("https://official.ac.za/accounting","Bachelor of Accounting",UniversityPageType.PROGRAMME_DETAIL,
                "Bachelor of Accounting. Admission year: 2027; Entry requirements: Mathematics level 5; APS: 30; Duration: 3 years;",Set.of("accounting"),List.of("Bachelor of Accounting"),true,null,null);
        service.ingestOfficialPages(institution,List.of(page));service.ingestOfficialPages(institution,List.of(page));
        var saved=org.mockito.ArgumentCaptor.forClass(UniversityProgramme.class);
        verify(programmes,times(1)).save(saved.capture());
        assertThat(saved.getValue().getPublishedEvidenceJson()).contains("Mathematics","Level 5",page.sourceUrl());
        assertThat(cache.get(page.sourceUrl()).getContentHash()).hasSize(64);
        verify(logs,times(2)).save(any());
    }
}
