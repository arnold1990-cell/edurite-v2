package com.edurite.student;

import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.gamification.dto.GamificationSummaryDto;
import com.edurite.gamification.service.GamificationService;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.dto.StudentSavedProfilePayload;
import com.edurite.student.dto.StudentSavedProfileSaveRequest;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentSettingsDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.entity.StudentSavedProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.repository.StudentSavedProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    StudentProfileRepository profileRepository;
    @Mock
    StudentProfileCompletionService studentProfileCompletionService;
    @Mock
    CurrentUserService currentUserService;
    @Mock
    StorageService storageService;
    @Mock
    SavedCareerRepository savedCareerRepository;
    @Mock
    SavedBursaryRepository savedBursaryRepository;
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    UserNotificationRepository userNotificationRepository;
    @Mock
    BursaryRepository bursaryRepository;
    @Mock
    GamificationService gamificationService;
    @Mock
    PsychometricService psychometricService;
    @Mock
    StudentSavedProfileRepository studentSavedProfileRepository;
    @Mock
    StudentPlanAccessService studentPlanAccessService;

    private StudentService studentService;
    private User user;
    private Principal principal;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                profileRepository,
                studentProfileCompletionService,
                currentUserService,
                storageService,
                savedCareerRepository,
                savedBursaryRepository,
                applicationRepository,
                userNotificationRepository,
                bursaryRepository,
                gamificationService,
                psychometricService,
                studentSavedProfileRepository,
                new ObjectMapper().findAndRegisterModules(),
                studentPlanAccessService
        );

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        user.setFirstName("Test");
        user.setLastName("Student");

        principal = () -> user.getEmail();
        when(currentUserService.requireUser(principal)).thenReturn(user);
        lenient().when(studentPlanAccessService.resolveByUserId(user.getId()))
                .thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC", "ACTIVE", false, 3, "Upgrade to Premium"));
    }

    @Test
    void dashboardFallsBackToBasicWhenSubscriptionPlanCodeIsNull() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L);
        when(userNotificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(0L);
        when(psychometricService.findGrowthAreasByStudentProfileId(profile.getId())).thenReturn(List.of());
        when(gamificationService.getSummary(principal)).thenReturn(new GamificationSummaryDto(0, 0, 0, "2026-T2", List.of(), List.of(), List.of()));

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result).containsEntry("planType", "BASIC");
        assertThat(result).containsEntry("subscriptionTier", "BASIC");
    }

    @Test
    void dashboardStillLoadsWhenPsychometricAndGamificationFail() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(1L);
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(2L);
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(3L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(1L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(1L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(1L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L);
        when(userNotificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(0L);

        when(psychometricService.findGrowthAreasByStudentProfileId(profile.getId()))
                .thenThrow(new RuntimeException("invalid psychometric payload"));
        when(gamificationService.getSummary(principal))
                .thenThrow(new RuntimeException("gamification service unavailable"));

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result).containsEntry("savedCareers", 1L);
        assertThat(result).containsEntry("savedBursaries", 2L);
        assertThat(result).containsEntry("points", 0L);
        assertThat(result).containsEntry("totalPoints", 0L);
        assertThat(result).containsEntry("termCode", "N/A");
        assertThat(result.get("skillGaps")).isEqualTo(List.of("Complete the psychometric assessment to unlock personalised growth areas."));
    }

    @Test
    void dashboardProvidesObjectiveEmptyStateReadinessAndActions() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setQualificationLevel("Grade 12");
        profile.setInterests("Engineering");
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L);
        when(userNotificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(1L);
        when(psychometricService.findGrowthAreasByStudentProfileId(profile.getId())).thenReturn(List.of());
        when(psychometricService.hasSubmissionForStudentProfileId(profile.getId())).thenReturn(false);
        when(gamificationService.getSummary(principal)).thenReturn(new GamificationSummaryDto(10, 0, 10, "2026-T2", List.of(), List.of(), List.of()));

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result).containsEntry("idealCareerObjectiveSet", false);
        assertThat(result.get("idealCareerObjectiveEmptyStateMessage"))
                .isEqualTo("Set your ideal career objective in your profile to track your progress.");
        assertThat(result).containsKey("readiness");
        assertThat(result).containsKey("recommendedImprovementActions");
        assertThat(result).containsKey("communicationGuidance");
        assertThat((List<?>) result.get("recommendedImprovements")).isNotEmpty();
    }

    @Test
    void dashboardCreatesIsolatedDefaultProfileForNewUser() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(savedCareerRepository.countByStudentId(any(UUID.class))).thenReturn(0L);
        when(savedBursaryRepository.countByStudentId(any(UUID.class))).thenReturn(0L);
        when(applicationRepository.countByStudentId(any(UUID.class))).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(any(UUID.class), any(String.class))).thenReturn(0L);
        when(userNotificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(0L);
        when(psychometricService.findGrowthAreasByStudentProfileId(any(UUID.class))).thenReturn(List.of());
        when(psychometricService.hasSubmissionForStudentProfileId(any(UUID.class))).thenReturn(false);
        when(gamificationService.getSummary(principal)).thenReturn(new GamificationSummaryDto(0, 0, 0, "2026-T2", List.of(), List.of(), List.of()));

        Map<String, Object> result = studentService.dashboard(principal);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileRepository).save(captor.capture());
        StudentProfile createdProfile = captor.getValue();
        assertThat(createdProfile.getUserId()).isEqualTo(user.getId());
        assertThat(createdProfile.getFirstName()).isEqualTo("Test");
        assertThat(createdProfile.getLastName()).isEqualTo("Student");
        assertThat(result).containsEntry("profileCompleteness", 0);
        assertThat(result).containsEntry("savedCareers", 0L);
        assertThat(result).containsEntry("savedBursaries", 0L);
        assertThat(result).containsEntry("activeApplications", 0L);
        assertThat(result).containsEntry("notifications", 0L);
        assertThat(result).containsEntry("idealCareerObjectiveSet", false);
    }

    @Test
    void getSettingsCreatesDefaultProfileWhenMissing() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        StudentSettingsDto settings = studentService.getSettings(principal);

        assertThat(settings.inAppNotificationsEnabled()).isTrue();
        assertThat(settings.emailNotificationsEnabled()).isFalse();
        assertThat(settings.smsNotificationsEnabled()).isFalse();
    }

    @Test
    void upsertProfilePartialUpdateDoesNotResetExistingValues() {
        StudentProfile existing = new StudentProfile();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        existing.setFirstName("Existing");
        existing.setLastName("Student");
        existing.setPhone("+27110000000");
        existing.setDateOfBirth(LocalDate.of(2004, 9, 15));
        existing.setQualificationLevel("High School");
        existing.setSkills("Java,Python");
        existing.setInterests("Technology");
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StudentProfileUpsertRequest request = new StudentProfileUpsertRequest(
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var updated = studentService.upsertProfile(principal, request);

        assertThat(updated.firstName()).isEqualTo("Updated");
        assertThat(updated.lastName()).isEqualTo("Student");
        assertThat(updated.phone()).isEqualTo("+27110000000");
        assertThat(updated.qualificationLevel()).isEqualTo("High School");
        assertThat(updated.skills()).containsExactly("Java", "Python");
        assertThat(updated.interests()).containsExactly("Technology");
    }

    @Test
    void saveProfileVersionStoresNamedProfileForFutureUse() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(studentSavedProfileRepository.save(any(StudentSavedProfile.class))).thenAnswer(invocation -> {
            StudentSavedProfile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var payload = new StudentSavedProfilePayload(
                "Alice",
                "Student",
                "+26771234567",
                LocalDate.of(2004, 1, 1),
                "Female",
                "Gaborone",
                "Focused learner",
                "High School",
                "Grade 12",
                List.of(),
                List.of("Maths"),
                List.of("Science club"),
                List.of("Python"),
                List.of("Technology"),
                "Software Engineering"
        );

        var saved = studentService.saveProfileVersion(principal, new StudentSavedProfileSaveRequest("Tech Track", payload));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.name()).isEqualTo("Tech Track");
        assertThat(saved.profile().careerGoals()).isEqualTo("Software Engineering");
        assertThat(saved.profile().skills()).containsExactly("Python");
    }

    @Test
    void applySavedProfileReplacesCurrentProfileFields() throws Exception {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setFirstName("Old");
        profile.setLastName("Name");
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = new StudentSavedProfilePayload(
                "New",
                "Profile",
                "+26770000000",
                LocalDate.of(2005, 5, 5),
                "Male",
                "Francistown",
                "Saved bio",
                "Undergraduate",
                "Grade 11",
                List.of(),
                List.of("Physics"),
                List.of("Tutor"),
                List.of("Java"),
                List.of("Engineering"),
                "Mechanical Engineering"
        );
        StudentSavedProfile savedProfile = new StudentSavedProfile();
        savedProfile.setId(UUID.randomUUID());
        savedProfile.setUserId(user.getId());
        savedProfile.setStudentId(profile.getId());
        savedProfile.setName("Engineering Track");
        savedProfile.setProfileData(new ObjectMapper().findAndRegisterModules().writeValueAsString(payload));
        when(studentSavedProfileRepository.findByIdAndUserId(savedProfile.getId(), user.getId()))
                .thenReturn(Optional.of(savedProfile));

        var result = studentService.applySavedProfile(principal, savedProfile.getId());

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.lastName()).isEqualTo("Profile");
        assertThat(result.skills()).containsExactly("Java");
        assertThat(result.careerGoals()).isEqualTo("Mechanical Engineering");
    }

    @Test
    void upsertProfileNormalizesCommonSubjectTypos() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileUpsertRequest request = new StudentProfileUpsertRequest(
                "Test",
                "Student",
                null,
                null,
                null,
                null,
                null,
                "High School",
                "Grade 12",
                List.of(new StudentSubjectAchievementDto("Accpount", 5)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null
        );

        var result = studentService.upsertProfile(principal, request);

        assertThat(result.subjectAchievements())
                .extracting(StudentSubjectAchievementDto::subjectName)
                .containsExactly("Accounting");
    }
}

