package com.edurite.roadmap;

import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsSubjectInput;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;
import com.edurite.roadmap.repository.CareerRoadmapRepository;
import com.edurite.roadmap.repository.SavedCareerRoadmapRepository;
import com.edurite.roadmap.service.AiCareerRoadmapService;
import com.edurite.roadmap.service.CareerRoadmapService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CareerRoadmapServiceTest {

    @Mock
    CareerRoadmapRepository repository;
    @Mock
    SavedCareerRoadmapRepository savedCareerRoadmapRepository;
    @Mock
    CareerProgramRequirementRepository requirementRepository;
    @Mock
    StudentProfileRepository studentProfileRepository;
    @Mock
    InstitutionRepository institutionRepository;
    @Mock
    CurrentUserService currentUserService;
    @Mock
    AiCareerRoadmapService aiCareerRoadmapService;

    private CareerRoadmapService service;

    @BeforeEach
    void setUp() {
        service = new CareerRoadmapService(
                repository,
                savedCareerRoadmapRepository,
                requirementRepository,
                studentProfileRepository,
                institutionRepository,
                currentUserService,
                new ObjectMapper().findAndRegisterModules(),
                aiCareerRoadmapService
        );
    }

    @Test
    void calculateApsDerivesLevelsFromMarksAndExcludesLifeOrientation() {
        ApsCalculationResponse response = service.calculateAps(new ApsCalculationRequest(
                "Grade 12",
                "Gauteng",
                List.of(
                        new ApsSubjectInput("Accounting", 75, 3, 3),
                        new ApsSubjectInput("Mathematical Literacy", 75, 4, 4),
                        new ApsSubjectInput("English FAL", 75, 3, 3),
                        new ApsSubjectInput("Business Studies", 45, 7, 7),
                        new ApsSubjectInput("History", 39, null, null),
                        new ApsSubjectInput("Life Orientation", 88, 1, 1)
                )
        ));

        assertThat(response.status()).isEqualTo("PROVISIONAL");
        assertThat(response.totalAps()).isEqualTo(23);
        assertThat(response.lifeOrientationIncluded()).isFalse();
        assertThat(response.calculationRule()).contains("Life Orientation excluded");
        assertThat(response.subjects())
                .extracting(item -> item.subjectName() + ":" + item.level() + ":" + item.apsPoints() + ":" + item.included())
                .containsExactly(
                        "Accounting:6:6:true",
                        "Mathematical Literacy:6:6:true",
                        "English FAL:6:6:true",
                        "Business Studies:3:3:true",
                        "History:2:2:true",
                        "Life Orientation:7:7:false"
                );
        assertThat(response.missingRequirements()).singleElement().asString().contains("1 more APS-counted subject");
    }

    @Test
    void calculateApsReturnsUnavailableWhenNoValidSubjectsExist() {
        ApsCalculationResponse response = service.calculateAps(new ApsCalculationRequest(
                "Grade 12",
                "Gauteng",
                List.of(
                        new ApsSubjectInput("", 75, 6, 6),
                        new ApsSubjectInput("   ", 65, 5, 5)
                )
        ));

        assertThat(response.status()).isEqualTo("UNAVAILABLE");
        assertThat(response.totalAps()).isNull();
        assertThat(response.missingRequirements()).containsExactly("Add at least one valid subject result.");
    }

    @Test
    void calculateApsTreatsOutOfRangeMarksAsIncompleteInsteadOfInventingValues() {
        ApsCalculationResponse response = service.calculateAps(new ApsCalculationRequest(
                "Grade 12",
                "Gauteng",
                List.of(
                        new ApsSubjectInput("Accounting", -1, 7, 7),
                        new ApsSubjectInput("Mathematics", 101, 7, 7),
                        new ApsSubjectInput("English FAL", null, null, null)
                )
        ));

        assertThat(response.status()).isEqualTo("PROVISIONAL");
        assertThat(response.totalAps()).isEqualTo(0);
        assertThat(response.subjects())
                .extracting(item -> item.level() == null && item.apsPoints() == null)
                .containsOnly(true);
    }
}
