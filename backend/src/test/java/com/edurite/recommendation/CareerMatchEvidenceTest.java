package com.edurite.recommendation;

import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.service.StudentService;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.entity.StudentPreference;
import com.edurite.student.repository.StudentPreferenceRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.repository.CourseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CareerMatchEvidenceTest {
    final StudentService students = mock(StudentService.class);
    final PsychometricService psychometric = mock(PsychometricService.class);
    final StudentPlanAccessService access = mock(StudentPlanAccessService.class);
    final CareerRepository careers = mock(CareerRepository.class);
    final CourseRepository courses = mock(CourseRepository.class);
    final StudentPreferenceRepository preferences = mock(StudentPreferenceRepository.class);
    final Principal alice = () -> "alice", bob = () -> "bob";
    final StudentProfile entity = new StudentProfile();
    final List<StudentSubjectAchievementDto> subjects = new ArrayList<>();
    final List<String> interests = new ArrayList<>();
    final StudentProfileDto profile = profile(subjects, interests);
    final RecommendationService service = new RecommendationService(students, access, psychometric, careers, courses, preferences, new ObjectMapper());

    @BeforeEach void setup() {
        entity.setId(UUID.randomUUID()); entity.setUserId(UUID.randomUUID());
        when(students.getProfile(alice)).thenReturn(profile);
        when(students.getProfileEntity(alice)).thenReturn(entity);
        when(access.resolveByUserId(any())).thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_PREMIUM", "ACTIVE", true, 10, null));
        var career = new Career(); career.setId(UUID.randomUUID()); career.setTitle("Data analyst");
        career.setDescription("Mathematics and analytical problem solving in technology");
        when(careers.findAll()).thenReturn(List.of(career));
    }
    void academic() {
        subjects.add(new StudentSubjectAchievementDto("Mathematics", null, 72, "MANUAL", false, null, null));
    }
    void assessment() {
        when(psychometric.hasSubmissionForStudentProfileId(entity.getId())).thenReturn(true);
        when(psychometric.findStrengthAreasByStudentProfileId(entity.getId())).thenReturn(List.of("analytical"));
    }

    @ParameterizedTest @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void explainsAvailableEvidenceWithoutInventingMissingResults(boolean academics, boolean assessment) {
        if (academics) academic(); if (assessment) assessment();
        var result = service.generateForStudent(alice);
        assertThat(result.suggestedCareers()).hasSize(academics || assessment ? 1 : 0);
        assertThat(String.join(" ", result.inputsUsed()).contains("Mathematics 72%")).isEqualTo(academics);
        assertThat(String.join(" ", result.inputsUsed()).contains("psychometric")).isEqualTo(assessment);
        assertThat(result.missingInformation().contains("Academic subjects and marks")).isEqualTo(!academics);
        assertThat(result.missingInformation().contains("Psychometric assessment (optional)")).isEqualTo(!assessment);
        if (academics || assessment) {
            String reason = result.suggestedCareers().getFirst().rationale();
            assertThat(reason.contains("Mathematics 72%")).isEqualTo(academics);
            assertThat(reason.contains("analytical")).isEqualTo(assessment);
        }
        assertThat(result.limitations()).anyMatch(text -> text.contains("not a diagnosis"));
    }

    @Test void assessmentOutageStillUsesAcademicEvidence() {
        academic();
        when(psychometric.hasSubmissionForStudentProfileId(entity.getId())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        var result = service.generateForStudent(alice);
        assertThat(result.suggestedCareers()).hasSize(1);
        assertThat(result.missingInformation()).anyMatch(text -> text.contains("temporarily unavailable"));
    }

    @Test void preferencesAndInterestsAreRealEvidenceAndChangesRefreshMatching() {
        var preference = new StudentPreference(); preference.setPreferredIndustries("technology");
        when(preferences.findByStudentId(entity.getId())).thenReturn(Optional.of(preference));
        var result = service.generateForStudent(alice);
        assertThat(result.suggestedCareers().getFirst().rationale()).contains("preferences: technology");
        when(preferences.findByStudentId(entity.getId())).thenReturn(Optional.empty());
        assertThat(service.generateForStudent(alice).suggestedCareers()).isEmpty();
        interests.add("technology");
        assertThat(service.generateForStudent(alice).suggestedCareers().getFirst().rationale()).contains("interests in technology");
    }

    @Test void recommendationsResolveEvidenceOnlyForAuthenticatedLearner() {
        academic(); assessment();
        var other = new StudentProfile(); other.setId(UUID.randomUUID()); other.setUserId(UUID.randomUUID());
        when(students.getProfileEntity(bob)).thenReturn(other);
        when(students.getProfile(bob)).thenReturn(profile(List.of(), List.of()));
        assertThat(service.generateForStudent(alice).suggestedCareers()).hasSize(1);
        assertThat(service.generateForStudent(bob).suggestedCareers()).isEmpty();
        verify(psychometric).hasSubmissionForStudentProfileId(other.getId());
        verify(preferences).findByStudentId(other.getId());
    }
    private static StudentProfileDto profile(List<StudentSubjectAchievementDto> subjects, List<String> interests) {
        return new StudentProfileDto(UUID.randomUUID(), "Test", "Learner", "test@example.test", null, null, null, null, null, null,
                "Grade 12", subjects, List.of(), List.of(), List.of(), interests, null, null, null, false, 0, null);
    }
}
