package com.edurite.ai.research;

import static org.assertj.core.api.Assertions.*;

import com.edurite.roadmap.entity.CareerProgramRequirement;
import com.edurite.student.dto.*;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

class AcademicAndEligibilityTest {
    StudentSubjectAchievementDto subject(
            String name, Integer mark, Integer level, String curriculum, boolean reviewed) {
        return new StudentSubjectAchievementDto(
                name,
                level,
                mark,
                "TRANSCRIPT",
                reviewed,
                null,
                null,
                curriculum,
                "NSC",
                "2026",
                "Final");
    }

    StudentProfileDto profile(List<StudentSubjectAchievementDto> results) {
        return new StudentProfileDto(
                UUID.randomUUID(),
                "Student",
                "Name",
                "private@example.test",
                null,
                null,
                null,
                "Botswana",
                null,
                null,
                "Grade 12",
                results,
                List.of(),
                List.of(),
                List.of("analysis"),
                List.of("accounting"),
                "Finance",
                null,
                null,
                true,
                100,
                null);
    }

    CareerProgramRequirement programme() {
        var p = new CareerProgramRequirement();
        p.setVerified(true);
        p.setLastVerifiedAt(OffsetDateTime.now());
        p.setAdmissionYear(Year.now().getValue());
        p.setMathematicsRequirement("60%");
        return p;
    }

    @Test
    void usesActualMarksAndRelativePrioritiesWithoutLabellingCareerPotential() {
        var a =
                new AcademicIntelligenceService()
                        .analyse(
                                profile(
                                        List.of(
                                                subject("Accounting", 78, 6, "NSC", true),
                                                subject("Maths", 45, 3, "NSC", true))));
        assertThat(a.percentageAverage()).isEqualTo(61.5);
        assertThat(a.relativeStrengths().getFirst().markPercentage()).isEqualTo(78);
        assertThat(a.improvementPriorities().getFirst().markPercentage()).isEqualTo(45);
    }

    @Test
    void doesNotCombineDifferentCurriculaOrConvertLevelsToMarks() {
        var a =
                new AcademicIntelligenceService()
                        .analyse(
                                profile(
                                        List.of(
                                                subject("Accounting", 78, 6, "NSC", true),
                                                subject("Maths", 45, 3, "BGCSE", true),
                                                subject("English", null, 5, "BGCSE", false))));
        assertThat(a.percentageAverage()).isNull();
        assertThat(a.relativeStrengths()).isEmpty();
        assertThat(a.limitations()).anyMatch(s -> s.contains("different curricula"));
    }

    @Test
    void emptyResultsRemainEmpty() {
        var a = new AcademicIntelligenceService().analyse(profile(List.of()));
        assertThat(a.percentageAverage()).isNull();
        assertThat(a.relativeStrengths()).isEmpty();
        assertThat(a.improvementPriorities()).isEmpty();
    }

    @Test
    void verifiedCompatibleThresholdsCanBeChecked() {
        var e =
                new ProgrammeEligibilityService()
                        .assess(
                                programme(),
                                List.of(subject("Mathematics", 70, 6, "NSC", true)),
                                "South Africa");
        assertThat(e.status()).isEqualTo(ProgrammeEligibilityService.MEETS);
        assertThat(e.subjects().getFirst().studentResult()).isEqualTo("70%");
    }

    @Test
    void admissionGapIsDifferentFromCareerSuitability() {
        var e =
                new ProgrammeEligibilityService()
                        .assess(
                                programme(),
                                List.of(subject("Mathematics", 45, 3, "NSC", true)),
                                "South Africa");
        assertThat(e.status()).isEqualTo(ProgrammeEligibilityService.GAP);
        assertThat(e.gaps()).hasSize(1);
    }

    @Test
    void botswanaAndZimbabweNeverReceiveAssumedNscAps() {
        for (String country : List.of("Botswana", "Zimbabwe")) {
            var p = programme();
            p.setApsRule("NSC_ALL_EXCL_LO");
            p.setApsRequired(30);
            var e =
                    new ProgrammeEligibilityService()
                            .assess(
                                    p,
                                    List.of(subject("Mathematics", 90, 7, "BGCSE", true)),
                                    country);
            assertThat(e.status()).isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
            assertThat(e.studentAps()).isNull();
        }
    }

    @Test
    void staleOrUnverifiedRequirementsNeverProduceEligibility() {
        var p = programme();
        p.setLastVerifiedAt(OffsetDateTime.now().minusDays(100));
        assertThat(
                        new ProgrammeEligibilityService()
                                .assess(
                                        p,
                                        List.of(subject("Mathematics", 90, 7, "NSC", true)),
                                        "South Africa")
                                .status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
    }

    @Test
    void missingAndCompoundThresholdsRemainUnknown() {
        var p = programme();
        p.setMathematicsRequirement("60% or Level 5");
        assertThat(
                        new ProgrammeEligibilityService()
                                .assess(
                                        p,
                                        List.of(subject("Mathematics", 90, 7, "NSC", true)),
                                        "South Africa")
                                .status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
        assertThat(
                        new ProgrammeEligibilityService()
                                .assess(programme(), List.of(), "South Africa")
                                .status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
    }

    @Test
    void unreviewedMarksAndAdditionalSelectionNeedConfirmation() {
        var p = programme();
        p.setAdditionalSelectionRequirements("Portfolio review");
        var e =
                new ProgrammeEligibilityService()
                        .assess(
                                p,
                                List.of(subject("Mathematics", 90, 7, "NSC", false)),
                                "South Africa");
        assertThat(e.status()).isEqualTo(ProgrammeEligibilityService.POTENTIAL);
        assertThat(e.uncheckedConditions()).contains("Portfolio review");
    }

    @Test
    void programmeSpecificApsIsNotGuessedFromAGeneralTotal() {
        var p = programme();
        p.setApsRequired(30);
        p.setApsRule("UNKNOWN_RULE");
        var e =
                new ProgrammeEligibilityService()
                        .assess(
                                p,
                                List.of(subject("Mathematics", 90, 7, "NSC", true)),
                                "South Africa");
        assertThat(e.studentAps()).isNull();
        assertThat(e.status()).isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
    }

    @Test
    void historicalProgressionRequiresMatchingCurriculumScaleExaminationAndKnownYears() {
        var current = subject("Accounting", 78, 6, "NSC", true);
        var previous =
                new StudentSubjectAchievementDto(
                        "Accounting",
                        5,
                        65,
                        "TRANSCRIPT",
                        true,
                        null,
                        null,
                        "NSC",
                        "NSC",
                        "2025",
                        "Final");
        var different =
                new StudentSubjectAchievementDto(
                        "Accounting",
                        6,
                        90,
                        "TRANSCRIPT",
                        true,
                        null,
                        null,
                        "BGCSE",
                        "NSC",
                        "2025",
                        "Final");
        var a =
                new AcademicIntelligenceService()
                        .analyse(profile(List.of(current)), List.of(previous, different));
        assertThat(a.progression()).hasSize(1);
        assertThat(a.progression().getFirst().percentagePointChange()).isEqualTo(13);
        assertThat(
                        new AcademicIntelligenceService()
                                .analyse(profile(List.of(current)), List.of(different))
                                .progression())
                .isEmpty();
    }

    @Test
    void duplicateResultsAreNotAveragedOrSelectedForEligibility() {
        var results =
                List.of(
                        subject("Mathematics", 45, 3, "NSC", true),
                        subject("Mathematics", 90, 7, "NSC", true));
        var a = new AcademicIntelligenceService().analyse(profile(results));
        assertThat(a.percentageAverage()).isNull();
        assertThat(a.relativeStrengths()).isEmpty();
        assertThat(
                        new ProgrammeEligibilityService()
                                .assess(programme(), results, "South Africa")
                                .status())
                .isEqualTo(ProgrammeEligibilityService.INSUFFICIENT);
    }

    @Test
    void conflictingHistoricalMarksDoNotProduceInventedProgression() {
        var older =
                new StudentSubjectAchievementDto(
                        "Accounting",
                        5,
                        65,
                        "TRANSCRIPT",
                        true,
                        null,
                        null,
                        "NSC",
                        "NSC",
                        "2025",
                        "Final");
        var conflict =
                new StudentSubjectAchievementDto(
                        "Accounting",
                        6,
                        85,
                        "TRANSCRIPT",
                        true,
                        null,
                        null,
                        "NSC",
                        "NSC",
                        "2025",
                        "Final");
        var a =
                new AcademicIntelligenceService()
                        .analyse(
                                profile(List.of(subject("Accounting", 78, 6, "NSC", true))),
                                List.of(older, conflict));
        assertThat(a.progression()).isEmpty();
    }

    @Test
    void blankAcademicMetadataCannotSupportHistoricalProgression() {
        var current =
                new StudentSubjectAchievementDto(
                        "Accounting", null, 78, "MANUAL", false, null, null, " ", "", "2026", " ");
        var older =
                new StudentSubjectAchievementDto(
                        "Accounting", null, 65, "MANUAL", false, null, null, " ", "", "2025", " ");
        assertThat(
                        new AcademicIntelligenceService()
                                .analyse(profile(List.of(current)), List.of(older))
                                .progression())
                .isEmpty();
    }
}
