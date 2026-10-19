package com.edurite.ai.research;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.universityinfo.entity.UniversityProgramme;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.recommendation.dto.RecommendationItemDto;
import com.edurite.roadmap.entity.CareerProgramRequirement;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.time.OffsetDateTime;
import java.util.*;

class ResearchCatalogueServiceTest {
    private final UniversityProgrammeRepository programmes =
            mock(UniversityProgrammeRepository.class);
    private final CareerProgramRequirementRepository requirements =
            mock(CareerProgramRequirementRepository.class);
    private final UniversitySourceRegistryService registry =
            mock(UniversitySourceRegistryService.class);
    private final ResearchCatalogueService service =
            new ResearchCatalogueService(
                    programmes, requirements, new ProgrammeEligibilityService(), registry);
    private final List<RecommendationItemDto> careers =
            List.of(
                    new RecommendationItemDto(
                            UUID.randomUUID().toString(),
                            "Accountant",
                            20,
                            "Declared Accounting interest"));

    @Test
    void cataloguePagesAreReadUntilExhaustedWithCanonicalCareerIdsAndNoEligibilityInvented() {
        when(requirements.findAll()).thenReturn(List.of());
        when(programmes.findByActiveTrue(any(Pageable.class)))
                .thenAnswer(
                        invocation -> {
                            Pageable page = invocation.getArgument(0);
                            return new PageImpl<>(
                                    List.of(programme("Accounting " + page.getPageNumber())),
                                    page,
                                    201);
                        });
        when(registry.isAllowedUrlForUniversity(anyString(), anyString())).thenReturn(true);
        var rows = service.match(careers, List.of());
        assertThat(rows).hasSize(2);
        verify(programmes, times(2)).findByActiveTrue(any(Pageable.class));
        assertThat(rows)
                .allSatisfy(
                        p -> {
                            assertThat(p.careerIds()).containsExactly(careers.getFirst().id());
                            assertThat(p.eligibility().status())
                                    .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
                        });
    }

    @Test
    void unofficialCuratedSourcesCannotOverrideTheOfficialExtractedProgrammeEvidence() {
        var curated = new CareerProgramRequirement();
        curated.setId(UUID.randomUUID());
        curated.setInstitutionName("Official Institution");
        curated.setCareerName("Accountant");
        curated.setQualificationName("Bachelor of Accounting");
        curated.setSource("https://untrusted.invalid/facts");
        curated.setVerified(true);
        curated.setLastVerifiedAt(OffsetDateTime.now());
        when(requirements.findAll()).thenReturn(List.of(curated));
        when(programmes.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(programme("Bachelor of Accounting"))));
        when(registry.isAllowedUrlForUniversity(eq("Official Institution"), anyString()))
                .thenAnswer(invocation -> !invocation.getArgument(1, String.class).contains("untrusted.invalid"));
        var rows = service.match(careers, List.of());
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().verificationStatus())
                .isEqualTo("Programme title verified; admission requirements not yet verified.");
        assertThat(rows.getFirst().eligibility().status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
        assertThat(rows.getFirst().sourceUrl()).isEqualTo("https://official.example/programme");
    }

    @Test
    void candidatesWithoutApprovedOfficialSourceEvidenceAreNotRecommendations() {
        var row = curated("Accountant");
        row.setSource("https://unapproved.invalid/programme");
        when(requirements.findAll()).thenReturn(List.of(row));
        when(programmes.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(programme("Bachelor of Accounting"))));
        assertThat(service.match(careers, List.of())).isEmpty();
        verify(requirements, never()).delete(any(CareerProgramRequirement.class));
        verify(programmes, never()).delete(any(UniversityProgramme.class));
    }

    @Test
    void unrelatedProgrammeTitlesAndUnsafeLinksAreNotPromoted() {
        assertThat(ResearchCatalogueService.related("Accountant", "Bachelor of Accounting"))
                .isTrue();
        assertThat(ResearchCatalogueService.related("Accountant", "Mechanical Engineering"))
                .isFalse();
        assertThat(ResearchCatalogueService.safeUrl("javascript:alert(1)")).isNull();
        assertThat(ResearchCatalogueService.safeUrl("https://user:secret@official.example"))
                .isNull();
    }

    @Test
    void identicalProgrammeRecordsPreserveAllCareerAssociations() {
        when(registry.isAllowedUrlForUniversity("Official Institution", "https://official.example/programme")).thenReturn(true);
        var first = curated("Accountant");
        var second = curated("Auditor");
        when(requirements.findAll()).thenReturn(List.of(first, second));
        when(programmes.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var auditor =
                new RecommendationItemDto(
                        UUID.randomUUID().toString(), "Auditor", 20, "Declared auditing interest");
        var rows = service.match(List.of(careers.getFirst(), auditor), List.of());
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().careerIds())
                .containsExactly(careers.getFirst().id(), auditor.id());
    }

    @Test
    void conflictingAdmissionVariantsKeepTheirOwnSourceEvidenceInsteadOfOverwriting() {
        var first = curated("Accountant");
        var second = curated("Accountant");
        second.setMathematicsRequirement("80%");
        when(requirements.findAll()).thenReturn(List.of(first, second));
        when(programmes.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(registry.isAllowedUrlForUniversity(anyString(), anyString())).thenReturn(true);
        var rows = service.match(careers, List.of());
        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(p -> p.id())
                .containsExactly(first.getId().toString(), second.getId().toString());
    }

    private CareerProgramRequirement curated(String career) {
        var p = new CareerProgramRequirement();
        p.setId(UUID.randomUUID());
        p.setInstitutionName("Official Institution");
        p.setCareerName(career);
        p.setQualificationName("Bachelor of Accounting");
        p.setSource("https://official.example/programme");
        p.setMathematicsRequirement("60%");
        return p;
    }

    private UniversityProgramme programme(String title) {
        var institution = new Institution();
        institution.setName("Official Institution");
        institution.setCountry("Botswana");
        var p = new UniversityProgramme();
        p.setId(UUID.randomUUID());
        p.setInstitution(institution);
        p.setName(title);
        p.setSourceUrl("https://official.example/programme");
        p.setRetrievalStatus("SOURCE_CONFIRMED");
        p.setLastVerifiedAt(OffsetDateTime.now());
        return p;
    }
}
