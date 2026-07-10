package com.edurite.institution.universityinfo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.ai.university.CrawledUniversityPageRepository;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversityRegistryProperties;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.universityinfo.entity.UniversityAdmissionRequirement;
import com.edurite.institution.universityinfo.entity.UniversityProgramme;
import com.edurite.institution.universityinfo.entity.UniversityRetrievalLog;
import com.edurite.institution.universityinfo.repository.UniversityAdmissionRequirementRepository;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.institution.universityinfo.repository.UniversityRetrievalLogRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UniversityInfoServiceTest {

    @Mock
    private UniversitySlugService slugService;
    @Mock
    private UniversitySourceRegistryService registryService;
    @Mock
    private MultiUniversityPageFetcherService pageFetcherService;
    @Mock
    private CrawledUniversityPageRepository crawledUniversityPageRepository;
    @Mock
    private UniversityProgrammeRepository programmeRepository;
    @Mock
    private UniversityAdmissionRequirementRepository requirementRepository;
    @Mock
    private UniversityRetrievalLogRepository retrievalLogRepository;

    private UniversityInfoService service;
    private Institution institution;

    @BeforeEach
    void setUp() {
        service = new UniversityInfoService(
                slugService,
                registryService,
                pageFetcherService,
                crawledUniversityPageRepository,
                programmeRepository,
                requirementRepository,
                retrievalLogRepository
        );

        institution = new Institution();
        institution.setId(UUID.randomUUID());
        institution.setName("University of Cape Town");
        institution.setWebsite("https://www.uct.ac.za");
        institution.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void getProgrammesUsesStoredVerifiedDataWhenAvailable() {
        UniversityProgramme programme = new UniversityProgramme();
        programme.setId(UUID.randomUUID());
        programme.setInstitution(institution);
        programme.setName("Bachelor of Science");
        programme.setQualificationType("Bachelor");
        programme.setFaculty("Science");
        programme.setSourceUrl("https://www.uct.ac.za/programmes/science");
        programme.setSourceLabel("Official programme page");
        programme.setRetrievalStatus("VERIFIED");
        programme.setLastVerifiedAt(OffsetDateTime.now());

        when(slugService.requireBySlug("university-of-cape-town")).thenReturn(institution);
        when(programmeRepository.findByInstitutionIdAndActiveTrueOrderByFacultyAscNameAsc(institution.getId())).thenReturn(List.of(programme));
        when(retrievalLogRepository.findTop10ByInstitutionIdOrderByRetrievedAtDesc(institution.getId())).thenReturn(List.of());
        when(registryService.getActiveUniversities()).thenReturn(List.of());

        var response = service.getProgrammes("university-of-cape-town");

        assertThat(response.fallbackOnly()).isFalse();
        assertThat(response.retrievalStatus()).isEqualTo("STORED_VERIFIED");
        assertThat(response.programmes()).hasSize(1);
        assertThat(response.programmes().getFirst().name()).isEqualTo("Bachelor of Science");
    }

    @Test
    void refreshUniversityDataExtractsProgrammeAndRequirementFromOfficialPage() {
        UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
        entry.setUniversityName("University of Cape Town");
        entry.setBaseDomain("uct.ac.za");
        entry.setSeedUrls(List.of("https://www.uct.ac.za/programmes"));

        UniversitySourcePageResult page = new UniversitySourcePageResult(
                "https://www.uct.ac.za/programmes/science",
                "Science admission requirements",
                UniversityPageType.ADMISSIONS_OVERVIEW,
                "Bachelor of Science applicants require an APS of 34. National Senior Certificate with English and Mathematics is required.",
                Set.of("science"),
                List.of("Science admission requirements"),
                true,
                null,
                null
        );

        when(slugService.requireBySlug("university-of-cape-town")).thenReturn(institution);
        when(registryService.getActiveUniversities()).thenReturn(List.of(entry));
        when(pageFetcherService.maxFetchBudgetPerUniversity()).thenReturn(4);
        when(pageFetcherService.discoverCandidateUrls(entry, 4)).thenReturn(List.of(page.sourceUrl()));
        when(pageFetcherService.fetchPages(List.of(page.sourceUrl()))).thenReturn(List.of(page));
        when(crawledUniversityPageRepository.findBySourceUrl(page.sourceUrl())).thenReturn(Optional.empty());
        when(requirementRepository.findByInstitutionIdAndProgrammeNameIgnoreCaseAndSourceUrlAndRequirementTitleIgnoreCase(eq(institution.getId()), any(), eq(page.sourceUrl()), any())).thenReturn(Optional.empty());

        var response = service.refreshUniversityData("university-of-cape-town");

        assertThat(response.status()).isIn("SUCCESS", "PARTIAL");
        verify(programmeRepository).deleteByInstitutionId(institution.getId());
        verify(requirementRepository).deleteByInstitutionId(institution.getId());
        verify(requirementRepository).save(any(UniversityAdmissionRequirement.class));
        verify(retrievalLogRepository).save(any(UniversityRetrievalLog.class));
    }
}




