package com.edurite.ai.research;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.edurite.ai.service.GeminiService;
import com.edurite.ai.university.*;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.source.ProviderBursarySource;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.repository.CourseRepository;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.roadmap.entity.CareerProgramRequirement;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;
import com.edurite.student.dto.*;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentPreferenceRepository;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;

import java.security.Principal;
import java.time.*;
import java.util.*;

/**
 * Runs real ranking, academic, programme-matching, eligibility and narrative validation together.
 * Repositories, student retrieval and the external AI provider remain isolated test fixtures.
 */
class CareerResearchEvidenceFlowTest {
    private final StudentService students = mock(StudentService.class);
    private final CareerRepository careers = mock(CareerRepository.class);
    private final UniversityProgrammeRepository programmes =
            mock(UniversityProgrammeRepository.class);
    private final CareerProgramRequirementRepository requirements =
            mock(CareerProgramRequirementRepository.class);
    private final OfficialCatalogueResearchService research =
            mock(OfficialCatalogueResearchService.class);
    private final UniversitySourceRegistryService registry =
            mock(UniversitySourceRegistryService.class);
    private final CrawledUniversityPageRepository pages =
            mock(CrawledUniversityPageRepository.class);
    private final ProviderBursarySource funding = mock(ProviderBursarySource.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Principal principal = () -> "private-student@example.test";
    private final UUID accountantId = UUID.randomUUID(), developerId = UUID.randomUUID();
    private final String source = "https://institution.ac.za/programmes/accounting";
    private CareerProgramRequirement requirement;
    private CareerResearchReportBuilder builder;

    @BeforeEach
    void setup() throws Exception {
        var entity = new StudentProfile();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        when(students.getProfileEntity(principal)).thenReturn(entity);
        when(students.getProfile(principal))
                .thenReturn(
                        profile(
                                List.of("Accounting"),
                                List.of(
                                        subject("Accounting", 78),
                                        subject("Mathematics", 45),
                                        subject("English", 65))));
        when(students.transcriptHistory(principal)).thenReturn(List.of());
        var plan = mock(StudentPlanAccessService.class);
        when(plan.resolveByUserId(entity.getUserId()))
                .thenReturn(
                        new StudentPlanAccessService.StudentPlanAccess(
                                "PLAN_PREMIUM", "ACTIVE", true, null, null));
        Career accountant = new Career();
        accountant.setId(accountantId);
        accountant.setTitle("Accountant");
        accountant.setIndustry("Finance");
        accountant.setDescription("Accounting, Mathematics and finance analysis.");
        Career developer = new Career();
        developer.setId(developerId);
        developer.setTitle("Software Developer");
        developer.setIndustry("Technology");
        developer.setDescription("Software technology.");
        when(careers.findAll()).thenReturn(List.of(accountant, developer));
        when(careers.findAllById(any())).thenReturn(List.of(accountant, developer));
        requirement = new CareerProgramRequirement();
        requirement.setId(UUID.randomUUID());
        requirement.setCareerName("Accountant");
        requirement.setInstitutionName("Recorded Institution");
        requirement.setQualificationName("Bachelor of Accounting");
        requirement.setQualificationType("Degree");
        requirement.setSource(source);
        requirement.setVerified(true);
        requirement.setLastVerifiedAt(OffsetDateTime.now());
        requirement.setAdmissionYear(Year.now().getValue());
        requirement.setMathematicsRequirement("60%");
        when(requirements.findAll()).thenReturn(List.of(requirement));
        when(programmes.findByActiveTrue(any(Pageable.class))).thenReturn(Page.empty());
        when(registry.isAllowedUrlForUniversity("Recorded Institution", source)).thenReturn(true);
        when(gemini.getCareerResearchNarrative(anyString()))
                .thenAnswer(
                        invocation -> {
                            var evidence = mapper.readTree(invocation.getArgument(0, String.class));
                            String title =
                                    evidence.path("careerEvidence").get(0).path("title").asText();
                            return mapper.writeValueAsString(
                                    Map.of(
                                            "assessment",
                                            "Your recorded evidence supports exploring " + title,
                                            "researchSummary",
                                            "Review the supplied programme evidence and current"
                                                + " admission gaps for "
                                                    + title,
                                            "nextSteps",
                                            List.of(
                                                    "Review the checked programme requirements for "
                                                            + title),
                                            "sourceUrls",
                                            evidence.path("programmes").isEmpty()
                                                    ? List.of()
                                                    : List.of(source)));
                        });
        var ranking =
                new RecommendationService(
                        students,
                        plan,
                        mock(PsychometricService.class),
                        careers,
                        mock(CourseRepository.class),
                        mock(StudentPreferenceRepository.class),
                        mapper);
        builder =
                new CareerResearchReportBuilder(
                        students,
                        ranking,
                        new AcademicIntelligenceService(),
                        new ResearchCatalogueService(
                                programmes,
                                requirements,
                                new ProgrammeEligibilityService(),
                                registry),
                        research,
                        careers,
                        new ResearchNarrativeService(gemini, mapper),
                        funding,
                        pages,
                        registry);
    }

    @Test
    void reportUsesStudentRecordsAndCanonicalCatalogueIdsThroughEveryStage() throws Exception {
        List<String> stages = new ArrayList<>();
        var report = builder.build(principal, stages::add);
        assertThat(report.careers())
                .extracting(c -> c.id())
                .containsExactly(accountantId.toString());
        assertThat(report.careers().getFirst().explanation())
                .contains("Accounting 78%", "Mathematics 45%");
        assertThat(report.academic().relativeStrengths())
                .extracting(StudentSubjectAchievementDto::subjectName)
                .containsExactly("Accounting");
        assertThat(report.academic().improvementPriorities())
                .extracting(StudentSubjectAchievementDto::subjectName)
                .containsExactly("Mathematics");
        assertThat(report.academic().percentageAverage()).isCloseTo(62.6667, within(0.001));
        var programme = report.programmes().getFirst();
        assertThat(programme.eligibility().status()).isEqualTo(ProgrammeEligibilityService.GAP);
        assertThat(programme.eligibility().subjects().getFirst().studentResult()).isEqualTo("45%");
        assertThat(programme.careerIds()).containsExactly(accountantId.toString());
        assertThat(report.careers().getFirst().programmeIds())
                .containsExactly(requirement.getId().toString());
        assertThat(report.sources())
                .anySatisfy(
                        s -> {
                            assertThat(s.url()).isEqualTo(source);
                            assertThat(s.verifiedAt())
                                    .isEqualTo(requirement.getLastVerifiedAt().toString());
                        });
        assertThat(report.interpretation().researchSummary()).contains("Accountant");
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(gemini).getCareerResearchNarrative(payload.capture());
        var evidence = mapper.readTree(payload.getValue());
        assertThat(evidence.path("subjects").get(0).path("mark").asInt()).isEqualTo(78);
        assertThat(evidence.path("programmes").get(0).path("sourceUrl").asText()).isEqualTo(source);
        assertThat(payload.getValue())
                .doesNotContain(
                        "private-student@example.test", "PrivateFirstName", "private-transcript");
        assertThat(stages)
                .contains(
                        "Discovering and ranking supported catalogue careers",
                        "Generating and validating the Gemini research interpretation");
        verifyNoInteractions(research);
    }

    @Test
    void changingSavedInterestsAndResultsChangesBothCareersAndAiEvidence() throws Exception {
        when(students.getProfile(principal)).thenReturn(profile(List.of("Technology"), List.of()));
        var report = builder.build(principal, s -> {});
        assertThat(report.careers())
                .extracting(c -> c.id())
                .containsExactly(developerId.toString());
        assertThat(report.programmes()).isEmpty();
        assertThat(report.academic().percentageAverage()).isNull();
        assertThat(report.interpretation().assessment()).contains("Software Developer");
        verify(research).refreshOne("South Africa");
    }

    @Test
    void unavailableOrUnverifiedSourcesDoNotBecomeConfirmedAdmissionFacts() {
        requirement.setVerified(false);
        when(research.refreshOne(any())).thenThrow(new IllegalStateException("Source unavailable"));
        var report = builder.build(principal, s -> {});
        assertThat(report.programmes().getFirst().eligibility().status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
        assertThat(report.programmes().getFirst().verificationStatus())
                .contains("not yet verified");
        assertThat(report.sources().getFirst().url()).isEqualTo(source);
        assertThat(report.limitations()).anyMatch(s -> s.contains("refresh was unavailable"));
    }

    @Test
    void expiredFundingIsExcludedAndCurrentFundingSourcesReachTheAiEvidence() throws Exception {
        when(funding.fetch(any()))
                .thenReturn(
                        List.of(
                                bursary("current", LocalDate.now().plusDays(30)),
                                bursary("expired", LocalDate.now().minusDays(1))));
        var report = builder.build(principal, s -> {});
        assertThat(report.funding())
                .extracting(BursaryResultDto::externalId)
                .containsExactly("current");
        assertThat(report.sources())
                .anyMatch(s -> s.url().equals("https://provider.example/current"));
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(gemini).getCareerResearchNarrative(payload.capture());
        assertThat(
                        mapper.readTree(payload.getValue())
                                .path("funding")
                                .get(0)
                                .path("title")
                                .asText())
                .isEqualTo("Accounting current");
    }

    @Test
    void noEvidenceDoesNotProduceAnInventedSuccessfulReport() {
        when(students.getProfile(principal)).thenReturn(profile(List.of(), List.of()));
        assertThatThrownBy(() -> builder.build(principal, s -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No supported career evidence");
        verifyNoInteractions(gemini, research, funding);
    }

    private StudentProfileDto profile(
            List<String> interests, List<StudentSubjectAchievementDto> subjects) {
        return new StudentProfileDto(
                UUID.randomUUID(),
                "PrivateFirstName",
                "Student",
                principal.getName(),
                null,
                null,
                null,
                "South Africa",
                null,
                null,
                "Grade 12",
                subjects,
                List.of(),
                List.of(),
                List.of(),
                interests,
                null,
                null,
                "https://private-transcript.example/file.pdf",
                true,
                100,
                null);
    }

    private StudentSubjectAchievementDto subject(String name, int mark) {
        return new StudentSubjectAchievementDto(
                name,
                null,
                mark,
                "TRANSCRIPT",
                true,
                null,
                null,
                "NSC",
                "Percentage",
                "2026",
                "Final");
    }

    private BursaryResultDto bursary(String id, LocalDate deadline) {
        return new BursaryResultDto(
                id,
                "Accounting " + id,
                "Recorded provider",
                "Published provider record",
                null,
                null,
                null,
                deadline,
                "https://provider.example/" + id,
                "OFFICIAL_PROVIDER",
                0,
                List.of(),
                true,
                false,
                null);
    }
}
