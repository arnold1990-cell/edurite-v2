package com.edurite.student.service;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.gamification.dto.GamificationSummaryDto;
import com.edurite.gamification.service.GamificationService;
import com.edurite.notification.repository.UserNotificationRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.dto.StudentSavedProfileDto;
import com.edurite.student.dto.StudentSavedProfilePayload;
import com.edurite.student.dto.StudentSavedProfileSaveRequest;
import com.edurite.student.dto.StudentSavedProfileSummaryDto;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.student.dto.StudentSettingsDto;
import com.edurite.student.entity.SavedBursary;
import com.edurite.student.entity.SavedCareer;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.entity.StudentSavedProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.repository.StudentSavedProfileRepository;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * This class named StudentService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private static final TypeReference<List<StudentSubjectAchievementDto>> SUBJECT_ACHIEVEMENTS_TYPE = new TypeReference<>() { };
    private static final int READINESS_GREEN_THRESHOLD = 75;
    private static final int READINESS_ORANGE_THRESHOLD = 50;
    private static final Set<String> SUPPORTED_GRADES = Set.of("Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12");
    private static final Set<String> SENIOR_PHASE_SUBJECTS = Set.of(
            "Home Language",
            "First Additional Language",
            "Mathematics",
            "Natural Sciences",
            "Social Sciences",
            "Technology",
            "Economic and Management Sciences",
            "Life Orientation",
            "Creative Arts"
    );
    private static final Set<String> FET_SUBJECTS = Set.of(
            "Accounting",
            "Agricultural Management Practices",
            "Agricultural Sciences",
            "Agricultural Technology",
            "Business Studies",
            "Civil Technology",
            "Computer Applications Technology",
            "Consumer Studies",
            "Dance Studies",
            "Design",
            "Dramatic Arts",
            "Economics",
            "Electrical Technology",
            "Engineering Graphics and Design",
            "Geography",
            "History",
            "Hospitality Studies",
            "Information Technology",
            "Life Orientation",
            "Life Sciences",
            "Mathematical Literacy",
            "Mathematics",
            "Mechanical Technology",
            "Music",
            "Physical Sciences",
            "Religion Studies",
            "Tourism",
            "Visual Arts",
            "Home Language",
            "First Additional Language",
            "Second Additional Language"
    );
    private static final Map<String, String> SUBJECT_NAME_ALIASES = Map.ofEntries(
            Map.entry("accpount", "Accounting"),
            Map.entry("account", "Accounting"),
            Map.entry("accounting", "Accounting"),
            Map.entry("business study", "Business Studies"),
            Map.entry("business studies", "Business Studies"),
            Map.entry("maths", "Mathematics"),
            Map.entry("mathematics", "Mathematics"),
            Map.entry("math lit", "Mathematical Literacy"),
            Map.entry("mathematical literacy", "Mathematical Literacy"),
            Map.entry("english fal", "First Additional Language"),
            Map.entry("first additional language", "First Additional Language"),
            Map.entry("second additional language", "Second Additional Language"),
            Map.entry("life orientation", "Life Orientation"),
            Map.entry("physical science", "Physical Sciences"),
            Map.entry("physical sciences", "Physical Sciences"),
            Map.entry("life science", "Life Sciences"),
            Map.entry("life sciences", "Life Sciences")
    );

    private final StudentProfileRepository repository;
    private final StudentProfileCompletionService studentProfileCompletionService;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final SavedCareerRepository savedCareerRepository;
    private final SavedBursaryRepository savedBursaryRepository;
    private final ApplicationRepository applicationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final BursaryRepository bursaryRepository;
    private final GamificationService gamificationService;
    private final PsychometricService psychometricService;
    private final StudentSavedProfileRepository studentSavedProfileRepository;
    private final ObjectMapper objectMapper;
    private final StudentPlanAccessService studentPlanAccessService;

    public StudentService(
            StudentProfileRepository repository,
            StudentProfileCompletionService studentProfileCompletionService,
            CurrentUserService currentUserService,
            StorageService storageService,
            SavedCareerRepository savedCareerRepository,
            SavedBursaryRepository savedBursaryRepository,
            ApplicationRepository applicationRepository,
            UserNotificationRepository userNotificationRepository,
            BursaryRepository bursaryRepository,
            GamificationService gamificationService,
            PsychometricService psychometricService,
            StudentSavedProfileRepository studentSavedProfileRepository,
            ObjectMapper objectMapper,
            StudentPlanAccessService studentPlanAccessService
    ) {
        this.repository = repository;
        this.studentProfileCompletionService = studentProfileCompletionService;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
        this.savedCareerRepository = savedCareerRepository;
        this.savedBursaryRepository = savedBursaryRepository;
        this.applicationRepository = applicationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.bursaryRepository = bursaryRepository;
        this.gamificationService = gamificationService;
        this.psychometricService = psychometricService;
        this.studentSavedProfileRepository = studentSavedProfileRepository;
        this.objectMapper = objectMapper;
        this.studentPlanAccessService = studentPlanAccessService;
    }

    /**
     * this method handles the "getProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto getProfile(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        profile = syncProfileCompletion(profile);
        return toDto(profile, user.getEmail());
    }

    /**
     * this method handles the "upsertProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto upsertProfile(Principal principal, StudentProfileUpsertRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        String normalizedSelectedGrade = normalizeSelectedGrade(request.selectedGrade());
        String effectiveSelectedGrade = request.selectedGrade() == null
                ? profile.getSelectedGrade()
                : normalizedSelectedGrade;
        profile.setFirstName(mergeValue(profile.getFirstName(), request.firstName()));
        profile.setLastName(mergeValue(profile.getLastName(), request.lastName()));
        profile.setPhone(mergeValue(profile.getPhone(), request.phone()));
        profile.setDateOfBirth(request.dateOfBirth() == null ? profile.getDateOfBirth() : request.dateOfBirth());
        profile.setGender(mergeValue(profile.getGender(), request.gender()));
        profile.setLocation(mergeValue(profile.getLocation(), request.location()));
        profile.setBio(mergeValue(profile.getBio(), request.bio()));
        profile.setQualificationLevel(mergeValue(profile.getQualificationLevel(), request.qualificationLevel()));
        profile.setSelectedGrade(request.selectedGrade() == null ? profile.getSelectedGrade() : normalizedSelectedGrade);
        profile.setSubjectAchievementsJson(mergeSubjectAchievements(profile.getSubjectAchievementsJson(), request.subjectAchievements(), effectiveSelectedGrade));
        profile.setQualifications(mergeList(profile.getQualifications(), request.qualifications()));
        profile.setExperience(mergeList(profile.getExperience(), request.experience()));
        profile.setSkills(mergeList(profile.getSkills(), request.skills()));
        profile.setInterests(mergeList(profile.getInterests(), request.interests()));
        profile.setCareerGoals(mergeValue(profile.getCareerGoals(), request.careerGoals()));
        profile = repository.save(profile);
        profile = syncProfileCompletion(profile);
        return toDto(profile, user.getEmail());
    }

    public List<StudentSavedProfileSummaryDto> listSavedProfiles(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return studentSavedProfileRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(item -> new StudentSavedProfileSummaryDto(item.getId(), item.getName(), item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
    }

    public StudentSavedProfileDto saveProfileVersion(Principal principal, StudentSavedProfileSaveRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));

        StudentSavedProfilePayload payload = request.profile() == null
                ? toSavedProfilePayload(profile)
                : normalizePayload(request.profile());

        StudentSavedProfile savedProfile = new StudentSavedProfile();
        savedProfile.setUserId(user.getId());
        savedProfile.setStudentId(profile.getId());
        savedProfile.setName(request.name().trim());
        savedProfile.setProfileData(writeJson(payload));
        StudentSavedProfile persisted = studentSavedProfileRepository.save(savedProfile);
        return toSavedProfileDto(persisted);
    }

    public StudentSavedProfileDto savedProfileDetails(Principal principal, UUID savedProfileId) {
        User user = currentUserService.requireUser(principal);
        StudentSavedProfile savedProfile = studentSavedProfileRepository.findByIdAndUserId(savedProfileId, user.getId())
                .orElseThrow(() -> new ResourceConflictException("Saved profile not found."));
        return toSavedProfileDto(savedProfile);
    }

    public StudentProfileDto applySavedProfile(Principal principal, UUID savedProfileId) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        StudentSavedProfile savedProfile = studentSavedProfileRepository.findByIdAndUserId(savedProfileId, user.getId())
                .orElseThrow(() -> new ResourceConflictException("Saved profile not found."));
        StudentSavedProfilePayload payload = readJson(savedProfile.getProfileData(), StudentSavedProfilePayload.class);
        applyPayloadToProfile(profile, payload);
        profile = repository.save(profile);
        profile = syncProfileCompletion(profile);
        return toDto(profile, user.getEmail());
    }

    public void deleteSavedProfile(Principal principal, UUID savedProfileId) {
        User user = currentUserService.requireUser(principal);
        StudentSavedProfile savedProfile = studentSavedProfileRepository.findByIdAndUserId(savedProfileId, user.getId())
                .orElseThrow(() -> new ResourceConflictException("Saved profile not found."));
        studentSavedProfileRepository.delete(savedProfile);
    }

    /**
     * this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadDocument(Principal principal, MultipartFile file, String documentType) throws IOException {
        validateFile(file);
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        String path = storageService.putObject("student-documents", "%s/%s-%s".formatted(user.getId(), documentType, file.getOriginalFilename()), file.getBytes());
        if ("cv".equalsIgnoreCase(documentType)) {
            profile.setCvFileUrl(path);
        }
        if ("transcript".equalsIgnoreCase(documentType)) {
            profile.setTranscriptFileUrl(path);
        }
        profile = repository.save(profile);
        profile = syncProfileCompletion(profile);
        return toDto(profile, user.getEmail());
    }

    /**
     * this method handles the "dashboard" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> dashboard(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        profile = syncProfileCompletion(profile);
        int profileCompleteness = studentProfileCompletionService.calculateCompleteness(profile);
        StudentPlanAccessService.StudentPlanAccess planAccess = studentPlanAccessService.resolveByUserId(user.getId());
        PlanType planType = planAccess.premium() ? PlanType.PREMIUM : PlanType.BASIC;
        String subscriptionTier = planType.name();
        String subscriptionPlanCode = normalizeSubscriptionPlanCode(planAccess.planCode());
        boolean premiumUnlocked = planAccess.premium();

        long savedCareers = savedCareerRepository.countByStudentId(profile.getId());
        long savedBursaries = savedBursaryRepository.countByStudentId(profile.getId());
        long activeApplications = applicationRepository.countByStudentId(profile.getId());
        long draftApplications = applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT");
        long submittedApplications = applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED");
        long inReviewApplications = applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW");
        long shortlistedApplications = applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED");

        boolean psychometricCompleted = false;
        List<String> psychometricGrowthAreas;
        try {
            psychometricGrowthAreas = psychometricService.findGrowthAreasByStudentProfileId(profile.getId());
            psychometricCompleted = psychometricService.hasSubmissionForStudentProfileId(profile.getId());
        } catch (RuntimeException ex) {
            log.warn("Unable to load psychometric growth areas for student profile {}", profile.getId(), ex);
            psychometricGrowthAreas = List.of();
        }
        if (psychometricGrowthAreas.isEmpty()) {
            psychometricGrowthAreas = psychometricCompleted
                    ? List.of("No major growth areas detected from your latest psychometric attempt.")
                    : List.of("Complete the psychometric assessment to unlock personalised growth areas.");
        }

        boolean hasIdealCareerObjective = notBlank(profile.getCareerGoals());
        String idealCareerObjective = normalizeValue(profile.getCareerGoals());
        boolean hasTranscript = notBlank(profile.getTranscriptFileUrl());
        boolean hasCv = notBlank(profile.getCvFileUrl());
        boolean hasQualification = notBlank(profile.getQualificationLevel());
        boolean hasInterests = notBlank(profile.getInterests());
        boolean hasSkills = notBlank(profile.getSkills());
        boolean hasExperience = notBlank(profile.getExperience());

        ReadinessCard fieldReadiness = buildFieldOfStudyReadiness(
                profileCompleteness,
                hasIdealCareerObjective,
                hasQualification,
                hasTranscript,
                hasInterests,
                hasSkills,
                psychometricCompleted,
                psychometricGrowthAreas
        );
        ReadinessCard bursaryReadiness = buildBursaryReadiness(
                profileCompleteness,
                hasIdealCareerObjective,
                hasQualification,
                hasTranscript,
                psychometricCompleted,
                savedBursaries,
                submittedApplications,
                shortlistedApplications
        );
        ReadinessCard jobReadiness = buildJobReadiness(
                profileCompleteness,
                hasIdealCareerObjective,
                hasCv,
                hasSkills,
                hasExperience,
                psychometricCompleted,
                shortlistedApplications
        );

        List<Map<String, Object>> recommendedActionCards = buildRecommendedImprovementActions(
                profile,
                profileCompleteness,
                psychometricCompleted,
                psychometricGrowthAreas,
                savedBursaries,
                activeApplications,
                List.of(fieldReadiness, bursaryReadiness, jobReadiness)
        );
        List<String> recommendedImprovementTitles = recommendedActionCards.stream()
                .map(action -> String.valueOf(action.get("title")))
                .limit(6)
                .toList();

        Map<String, Object> trackProgress = buildTrackProgress(
                idealCareerObjective,
                hasIdealCareerObjective,
                List.of(fieldReadiness, bursaryReadiness, jobReadiness),
                recommendedImprovementTitles
        );

        GamificationSummaryDto gamificationSummary;
        try {
            gamificationSummary = gamificationService.getSummary(principal);
        } catch (RuntimeException ex) {
            log.warn("Unable to load gamification summary for user {}", user.getId(), ex);
            gamificationSummary = new GamificationSummaryDto(0L, 0L, 0L, "N/A", List.of(), List.of(), List.of());
        }

        long unreadNotifications;
        try {
            unreadNotifications = userNotificationRepository.countByUserIdAndIsReadFalse(user.getId());
        } catch (RuntimeException ex) {
            log.warn("Unable to load notification count for user {}", user.getId(), ex);
            unreadNotifications = 0L;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profileCompleteness", profileCompleteness);
        response.put("savedCareers", savedCareers);
        response.put("savedBursaries", savedBursaries);
        response.put("savedOpportunities", savedCareers + savedBursaries);
        response.put("activeApplications", activeApplications);
        response.put("applicationProgress", List.of(
                Map.of("label", "Draft", "count", draftApplications),
                Map.of("label", "Submitted", "count", submittedApplications),
                Map.of("label", "In review", "count", inReviewApplications),
                Map.of("label", "Shortlisted", "count", shortlistedApplications)
        ));
        response.put("skillGaps", psychometricGrowthAreas);
        response.put("recommendedImprovements", recommendedImprovementTitles);
        response.put("recommendedImprovementActions", recommendedActionCards);
        response.put("idealCareerObjective", idealCareerObjective);
        response.put("idealCareerObjectiveSet", hasIdealCareerObjective);
        response.put("idealCareerObjectiveEmptyStateMessage",
                hasIdealCareerObjective ? null : "Set your ideal career objective in your profile to track your progress.");
        response.put("trackProgress", trackProgress);
        response.put("readiness", Map.of(
                "fieldOfStudy", toReadinessMap(fieldReadiness),
                "bursary", toReadinessMap(bursaryReadiness),
                "job", toReadinessMap(jobReadiness)
        ));
        response.put("communicationGuidance", buildCommunicationGuidance());
        response.put("points", gamificationSummary.availablePoints());
        response.put("totalPoints", gamificationSummary.totalPoints());
        response.put("termCode", gamificationSummary.currentTermCode());
        response.put("notifications", unreadNotifications);
        response.put("planType", planType.name());
        response.put("subscriptionTier", subscriptionTier);
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("type", planType.name());
        plan.put("planCode", subscriptionPlanCode);
        plan.put("tier", subscriptionTier);
        plan.put("premium", premiumUnlocked);
        plan.put("careerSuggestionLimit", planAccess.careerSuggestionLimit());
        plan.put("upgradeMessage", premiumUnlocked
                ? "Premium plan active: full AI Guidance is unlocked."
                : planAccess.upgradeMessage());
        response.put("plan", plan);
        return response;
    }

    /**
     * this method handles the "getSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto getSettings(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return toSettingsDto(profile);
    }

    /**
     * this method handles the "updateSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto updateSettings(Principal principal, StudentSettingsDto request) {
        StudentProfile profile = getProfileEntity(principal);
        profile.setInAppNotificationsEnabled(request.inAppNotificationsEnabled());
        profile.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        profile.setSmsNotificationsEnabled(request.smsNotificationsEnabled());
        repository.save(profile);
        return toSettingsDto(profile);
    }

    /**
     * this method handles the "saveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveCareer(Principal principal, UUID careerId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedCareerRepository.existsByStudentIdAndCareerId(profile.getId(), careerId)) {
            SavedCareer saved = new SavedCareer();
            saved.setStudentId(profile.getId());
            saved.setCareerId(careerId);
            savedCareerRepository.save(saved);
        }
    }

    /**
     * this method handles the "saveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveBursary(Principal principal, UUID bursaryId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedBursaryRepository.existsByStudentIdAndBursaryId(profile.getId(), bursaryId)) {
            SavedBursary saved = new SavedBursary();
            saved.setStudentId(profile.getId());
            saved.setBursaryId(bursaryId);
            savedBursaryRepository.save(saved);
        }
    }


    /**
     * this method handles the "unsaveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveCareer(Principal principal, UUID careerId) {
        StudentProfile profile = getProfileEntity(principal);
        savedCareerRepository.deleteByStudentIdAndCareerId(profile.getId(), careerId);
    }

    /**
     * this method handles the "unsaveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveBursary(Principal principal, UUID bursaryId) {
        StudentProfile profile = getProfileEntity(principal);
        savedBursaryRepository.deleteByStudentIdAndBursaryId(profile.getId(), bursaryId);
    }

    /**
     * this method handles the "savedCareerIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedCareerIds(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return savedCareerRepository.findByStudentId(profile.getId()).stream()
                .map(SavedCareer::getCareerId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * this method handles the "savedBursaryIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedBursaryIds(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return savedBursaryRepository.findByStudentId(profile.getId()).stream().map(SavedBursary::getBursaryId).toList();
    }

    public List<Bursary> savedBursaries(Principal principal) {
        List<UUID> ids = savedBursaryIds(principal);
        if (ids.isEmpty()) {
            return List.of();
        }
        return bursaryRepository.findAllById(ids);
    }

    /**
     * this method handles the "getProfileEntity" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfile getProfileEntity(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        return syncProfileCompletion(profile);
    }

    /**
     * this method handles the "createDefault" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile createDefault(User user) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setProfileCompleted(false);
        profile.setInAppNotificationsEnabled(true);
        profile.setEmailNotificationsEnabled(false);
        profile.setSmsNotificationsEnabled(false);
        profile.setPreferencesJson("{}");
        return repository.save(profile);
    }

    /**
     * this method handles the "toDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfileDto toDto(StudentProfile profile, String email) {
        int completeness = studentProfileCompletionService.calculateCompleteness(profile);
        return new StudentProfileDto(
                profile.getId(), profile.getFirstName(), profile.getLastName(), email, profile.getPhone(), profile.getDateOfBirth(), profile.getGender(),
                profile.getLocation(), profile.getBio(), profile.getQualificationLevel(), profile.getSelectedGrade(),
                readSubjectAchievements(profile.getSubjectAchievementsJson()), split(profile.getQualifications()), split(profile.getExperience()),
                split(profile.getSkills()), split(profile.getInterests()), profile.getCareerGoals(), profile.getCvFileUrl(), profile.getTranscriptFileUrl(),
                profile.isProfileCompleted(), completeness
        );
    }

    /**
     * this method handles the "toSettingsDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentSettingsDto toSettingsDto(StudentProfile profile) {
        return new StudentSettingsDto(profile.isInAppNotificationsEnabled(), profile.isEmailNotificationsEnabled(), profile.isSmsNotificationsEnabled());
    }

    private String normalizeSubscriptionPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return "PLAN_BASIC";
        }
        String normalized = planCode.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("PLAN_") ? normalized : "PLAN_" + normalized;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private ReadinessCard buildFieldOfStudyReadiness(
            int profileCompleteness,
            boolean hasIdealCareerObjective,
            boolean hasQualification,
            boolean hasTranscript,
            boolean hasInterests,
            boolean hasSkills,
            boolean psychometricCompleted,
            List<String> psychometricGrowthAreas
    ) {
        List<ReadinessSignal> signals = List.of(
                new ReadinessSignal(20, profileCompleteness >= 80, "Your profile data is detailed enough for matching.",
                        "Profile completeness is below 80%.", "Complete missing profile fields."),
                new ReadinessSignal(15, hasIdealCareerObjective, "You have a clear objective guiding your study path.",
                        "Ideal career objective is not set.", "Set a clear ideal career objective."),
                new ReadinessSignal(15, hasQualification, "Qualification level is set for pathway matching.",
                        "Qualification level is missing.", "Add your qualification level and latest academic details."),
                new ReadinessSignal(15, hasTranscript, "Transcript is available for qualification checks.",
                        "Latest transcript is not uploaded.", "Upload your latest transcript."),
                new ReadinessSignal(10, hasInterests, "Career interests are defined.",
                        "Career interests are not specific yet.", "Add and refine your intended field/interests."),
                new ReadinessSignal(10, hasSkills, "Skills are available for field alignment.",
                        "Skills list is too limited.", "Add your strongest skills and subjects."),
                new ReadinessSignal(15, psychometricCompleted, "Psychometric signals are available for fit analysis.",
                        "Psychometric assessment has not been completed.", "Complete the psychometric assessment.")
        );
        ReadinessCard base = buildReadinessCard("Field of study", signals);
        if (psychometricCompleted && !psychometricGrowthAreas.isEmpty()
                && psychometricGrowthAreas.stream().noneMatch(area -> area.toLowerCase(Locale.ROOT).contains("no major"))) {
            List<String> additionalActions = new ArrayList<>(base.nextImprovements());
            additionalActions.add("Work on psychometric growth areas: " + String.join(", ", psychometricGrowthAreas.stream().limit(2).toList()) + ".");
            return new ReadinessCard(base.score(), base.band(), base.explanation(), base.strengths(), base.gaps(), topUnique(additionalActions, 4));
        }
        return base;
    }

    private ReadinessCard buildBursaryReadiness(
            int profileCompleteness,
            boolean hasIdealCareerObjective,
            boolean hasQualification,
            boolean hasTranscript,
            boolean psychometricCompleted,
            long savedBursaries,
            long submittedApplications,
            long shortlistedApplications
    ) {
        boolean hasActiveApplicationMomentum = (submittedApplications + shortlistedApplications) > 0;
        List<ReadinessSignal> signals = List.of(
                new ReadinessSignal(20, profileCompleteness >= 75, "Profile completeness supports bursary screening.",
                        "Profile completeness is too low for strong bursary matching.", "Complete remaining profile sections."),
                new ReadinessSignal(20, hasTranscript, "Transcript is available for bursary eligibility review.",
                        "Transcript is missing for bursary checks.", "Upload your latest transcript."),
                new ReadinessSignal(15, hasQualification, "Qualification level is available for eligibility filters.",
                        "Qualification level is missing.", "Add your qualification level."),
                new ReadinessSignal(10, hasIdealCareerObjective, "Your goal helps rank relevant funding options.",
                        "Career objective is not clear yet.", "Refine your ideal career objective."),
                new ReadinessSignal(10, psychometricCompleted, "Psychometric insights strengthen personalised funding suggestions.",
                        "Psychometric insights are missing.", "Complete your psychometric assessment."),
                new ReadinessSignal(10, savedBursaries > 0, "You are tracking bursaries aligned to your profile.",
                        "No bursaries saved yet.", "Save and shortlist relevant bursaries."),
                new ReadinessSignal(15, hasActiveApplicationMomentum, "You have already progressed bursary applications.",
                        "No submitted bursary applications yet.", "Submit at least one bursary application.")
        );
        return buildReadinessCard("Bursary", signals);
    }

    private ReadinessCard buildJobReadiness(
            int profileCompleteness,
            boolean hasIdealCareerObjective,
            boolean hasCv,
            boolean hasSkills,
            boolean hasExperience,
            boolean psychometricCompleted,
            long shortlistedApplications
    ) {
        List<ReadinessSignal> signals = List.of(
                new ReadinessSignal(20, profileCompleteness >= 70, "Profile completeness supports job matching.",
                        "Profile completeness is low for employer confidence.", "Complete core profile details."),
                new ReadinessSignal(20, hasCv, "CV is uploaded for job opportunity matching.",
                        "CV is not uploaded.", "Upload your latest CV."),
                new ReadinessSignal(20, hasSkills, "Skills inventory supports role matching.",
                        "Skills inventory is incomplete.", "Add role-relevant skills."),
                new ReadinessSignal(15, hasExperience, "Experience signals improve employability confidence.",
                        "Experience or extracurricular evidence is thin.", "Add projects or extracurricular activities."),
                new ReadinessSignal(10, hasIdealCareerObjective, "Career objective is clear for role targeting.",
                        "Career objective is not yet set.", "Set your ideal career objective."),
                new ReadinessSignal(10, psychometricCompleted, "Psychometric insights improve role-fit recommendations.",
                        "Psychometric insights are missing.", "Complete your psychometric assessment."),
                new ReadinessSignal(5, shortlistedApplications > 0, "You have application traction already.",
                        "No shortlist momentum yet.", "Apply to at least one suitable opportunity.")
        );
        return buildReadinessCard("Job opportunity", signals);
    }

    private ReadinessCard buildReadinessCard(String areaName, List<ReadinessSignal> signals) {
        // Weighted readiness model: each completed signal contributes to a 0-100 score.
        int score = 0;
        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        int metCount = 0;
        for (ReadinessSignal signal : signals) {
            if (signal.met()) {
                score += signal.weight();
                metCount++;
                strengths.add(signal.strengthMessage());
            } else {
                gaps.add(signal.gapMessage());
                actions.add(signal.nextAction());
            }
        }
        String band = scoreBand(score);
        String explanation = areaName + " readiness is " + band + " because " + metCount + " of " + signals.size()
                + " key readiness signals are currently complete.";
        return new ReadinessCard(
                score,
                band,
                explanation,
                topUnique(strengths, 3),
                topUnique(gaps, 3),
                topUnique(actions, 4)
        );
    }

    private String scoreBand(int score) {
        if (score >= READINESS_GREEN_THRESHOLD) {
            return "GREEN";
        }
        if (score >= READINESS_ORANGE_THRESHOLD) {
            return "ORANGE";
        }
        return "RED";
    }

    private Map<String, Object> toReadinessMap(ReadinessCard readiness) {
        Map<String, Object> readinessMap = new LinkedHashMap<>();
        readinessMap.put("score", readiness.score());
        readinessMap.put("status", readiness.band());
        readinessMap.put("explanation", readiness.explanation());
        readinessMap.put("strengths", readiness.strengths());
        readinessMap.put("gaps", readiness.gaps());
        readinessMap.put("nextImprovements", readiness.nextImprovements());
        return readinessMap;
    }

    private Map<String, Object> buildTrackProgress(
            String idealCareerObjective,
            boolean hasIdealCareerObjective,
            List<ReadinessCard> readinessCards,
            List<String> recommendedImprovementTitles
    ) {
        int averageScore = readinessCards.isEmpty()
                ? 0
                : (int) Math.round(readinessCards.stream().mapToInt(ReadinessCard::score).average().orElse(0));
        if (!hasIdealCareerObjective) {
            averageScore = Math.max(0, averageScore - 10);
        }
        String progressBand = scoreBand(averageScore);
        List<String> strengths = topUnique(readinessCards.stream()
                .flatMap(card -> card.strengths().stream())
                .toList(), 4);
        List<String> gaps = topUnique(readinessCards.stream()
                .flatMap(card -> card.gaps().stream())
                .toList(), 4);
        List<String> nextMilestones = topUnique(recommendedImprovementTitles, 4);

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("goalSet", hasIdealCareerObjective);
        progress.put("goalTitle", hasIdealCareerObjective ? idealCareerObjective : null);
        progress.put("progressScore", averageScore);
        progress.put("status", progressBand);
        progress.put("summary", hasIdealCareerObjective
                ? "Your progress is being tracked against your stated objective."
                : "Set your ideal career objective to track your progress against a clear target.");
        progress.put("strengths", strengths);
        progress.put("gaps", gaps);
        progress.put("nextMilestones", nextMilestones);
        return progress;
    }

    private List<Map<String, Object>> buildRecommendedImprovementActions(
            StudentProfile profile,
            int profileCompleteness,
            boolean psychometricCompleted,
            List<String> psychometricGrowthAreas,
            long savedBursaries,
            long activeApplications,
            List<ReadinessCard> readinessCards
    ) {
        LinkedHashMap<String, Map<String, Object>> actions = new LinkedHashMap<>();
        if (!notBlank(profile.getCareerGoals())) {
            addImprovementAction(actions, "refine-career-objective", "Refine your ideal career objective",
                    "A clear objective improves pathway and qualification matching.", "Field readiness", "HIGH");
        }
        if (profileCompleteness < 80) {
            addImprovementAction(actions, "complete-profile", "Complete remaining profile fields",
                    "Higher profile completeness increases recommendation confidence.", "Overall readiness", "HIGH");
        }
        if (!notBlank(profile.getTranscriptFileUrl())) {
            addImprovementAction(actions, "upload-transcript", "Upload your latest transcript",
                    "Transcript data is required for qualification and bursary readiness checks.", "Field and bursary readiness", "HIGH");
        }
        if (!psychometricCompleted) {
            addImprovementAction(actions, "complete-psychometric", "Complete psychometric test",
                    "Psychometric insights improve fit scoring and personalised guidance quality.", "Career and job readiness", "HIGH");
        }
        if (!notBlank(profile.getSkills())) {
            addImprovementAction(actions, "add-skills", "Add your strongest skills",
                    "Skills improve career and job matching accuracy.", "Job readiness", "MEDIUM");
        }
        if (!notBlank(profile.getExperience())) {
            addImprovementAction(actions, "add-experience", "Add extracurricular or project experience",
                    "Experience evidence improves job opportunity readiness.", "Job readiness", "MEDIUM");
        }
        if (!notBlank(profile.getInterests())) {
            addImprovementAction(actions, "set-intended-field", "Choose your intended field clearly",
                    "Clear interests help the system prioritise relevant study pathways.", "Field readiness", "MEDIUM");
        }
        if (savedBursaries == 0) {
            addImprovementAction(actions, "save-bursaries", "Save and apply to relevant bursaries",
                    "Tracking bursaries creates application momentum and raises bursary readiness.", "Bursary readiness", "MEDIUM");
        }
        if (activeApplications == 0) {
            addImprovementAction(actions, "start-application", "Start at least one application",
                    "Active applications signal readiness and improve opportunity tracking.", "Bursary and job readiness", "MEDIUM");
        }
        if (psychometricCompleted && !psychometricGrowthAreas.isEmpty()
                && psychometricGrowthAreas.stream().noneMatch(area -> area.toLowerCase(Locale.ROOT).contains("no major"))) {
            addImprovementAction(actions, "target-growth-area", "Work on top psychometric growth area",
                    "Targeted development in " + psychometricGrowthAreas.get(0) + " can improve overall readiness.",
                    "Career development", "MEDIUM");
        }

        for (ReadinessCard readinessCard : readinessCards) {
            if (!readinessCard.nextImprovements().isEmpty()) {
                String actionTitle = readinessCard.nextImprovements().get(0);
                addImprovementAction(actions, "readiness-action-" + actions.size(),
                        actionTitle, "This action directly addresses readiness gaps detected in your dashboard.",
                        "Readiness improvements", "LOW");
            }
        }

        if (actions.isEmpty()) {
            addImprovementAction(actions, "maintain-progress", "Maintain your current momentum",
                    "Your current profile and readiness signals are strong. Keep data updated as your goals change.",
                    "Overall readiness", "LOW");
        }
        return actions.values().stream().limit(10).toList();
    }

    private void addImprovementAction(
            Map<String, Map<String, Object>> actions,
            String id,
            String title,
            String reason,
            String impactArea,
            String priority
    ) {
        if (actions.containsKey(id)) {
            return;
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("title", title);
        action.put("reason", reason);
        action.put("impactArea", impactArea);
        action.put("priority", priority);
        actions.put(id, action);
    }

    private Map<String, Object> buildCommunicationGuidance() {
        return Map.of(
                "title", "How to get better recommendations",
                "description", "EduRite performs best when your profile and goals are accurate, complete, and current.",
                "tips", List.of(
                        "Complete your profile fully, including qualification level, interests, and skills.",
                        "Upload accurate academic records, especially your latest transcript.",
                        "Set your intended field or ideal career objective clearly.",
                        "Complete the psychometric test honestly for stronger fit analysis.",
                        "Update your goals and profile details whenever your plans change."
                )
        );
    }

    private List<String> topUnique(List<String> input, int limit) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (String value : input) {
            if (value == null || value.isBlank()) {
                continue;
            }
            ordered.add(value.trim());
            if (ordered.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(ordered);
    }

    private StudentSavedProfileDto toSavedProfileDto(StudentSavedProfile savedProfile) {
        return new StudentSavedProfileDto(
                savedProfile.getId(),
                savedProfile.getName(),
                readJson(savedProfile.getProfileData(), StudentSavedProfilePayload.class),
                savedProfile.getCreatedAt(),
                savedProfile.getUpdatedAt()
        );
    }

    private StudentSavedProfilePayload toSavedProfilePayload(StudentProfile profile) {
        String normalizedSelectedGrade = normalizeSelectedGrade(profile.getSelectedGrade());
        return new StudentSavedProfilePayload(
                normalizeValue(profile.getFirstName()),
                normalizeValue(profile.getLastName()),
                normalizeValue(profile.getPhone()),
                profile.getDateOfBirth(),
                normalizeValue(profile.getGender()),
                normalizeValue(profile.getLocation()),
                normalizeValue(profile.getBio()),
                normalizeValue(profile.getQualificationLevel()),
                normalizedSelectedGrade,
                normalizeSubjectAchievements(readSubjectAchievements(profile.getSubjectAchievementsJson()), normalizedSelectedGrade),
                split(profile.getQualifications()),
                split(profile.getExperience()),
                split(profile.getSkills()),
                split(profile.getInterests()),
                normalizeValue(profile.getCareerGoals())
        );
    }

    private StudentSavedProfilePayload normalizePayload(StudentSavedProfilePayload payload) {
        String normalizedSelectedGrade = normalizeSelectedGrade(payload.selectedGrade());
        return new StudentSavedProfilePayload(
                normalizeValue(payload.firstName()),
                normalizeValue(payload.lastName()),
                normalizeValue(payload.phone()),
                payload.dateOfBirth(),
                normalizeValue(payload.gender()),
                normalizeValue(payload.location()),
                normalizeValue(payload.bio()),
                normalizeValue(payload.qualificationLevel()),
                normalizedSelectedGrade,
                normalizeSubjectAchievements(payload.subjectAchievements(), normalizedSelectedGrade),
                normalizeList(payload.qualifications()),
                normalizeList(payload.experience()),
                normalizeList(payload.skills()),
                normalizeList(payload.interests()),
                normalizeValue(payload.careerGoals())
        );
    }

    private void applyPayloadToProfile(StudentProfile profile, StudentSavedProfilePayload payload) {
        StudentSavedProfilePayload normalized = normalizePayload(payload);
        profile.setFirstName(normalized.firstName());
        profile.setLastName(normalized.lastName());
        profile.setPhone(normalized.phone());
        profile.setDateOfBirth(normalized.dateOfBirth());
        profile.setGender(normalized.gender());
        profile.setLocation(normalized.location());
        profile.setBio(normalized.bio());
        profile.setQualificationLevel(normalized.qualificationLevel());
        profile.setSelectedGrade(normalized.selectedGrade());
        profile.setSubjectAchievementsJson(writeJson(normalized.subjectAchievements()));
        profile.setQualifications(join(normalized.qualifications()));
        profile.setExperience(join(normalized.experience()));
        profile.setSkills(join(normalized.skills()));
        profile.setInterests(join(normalized.interests()));
        profile.setCareerGoals(normalized.careerGoals());
    }

    private List<String> normalizeList(List<String> incoming) {
        if (incoming == null) {
            return List.of();
        }
        return incoming.stream()
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .toList();
    }

    private List<StudentSubjectAchievementDto> normalizeSubjectAchievements(List<StudentSubjectAchievementDto> incoming, String selectedGrade) {
        if (incoming == null) {
            return List.of();
        }
        Set<String> allowedSubjects = allowedSubjectsByGrade(selectedGrade);
        return incoming.stream()
                .filter(item -> item != null && item.subjectName() != null && !item.subjectName().isBlank())
                .map(item -> {
                    String normalizedSubjectName = canonicalizeSubjectName(item.subjectName());
                    if (!allowedSubjects.isEmpty() && !allowedSubjects.contains(normalizedSubjectName)) {
                        throw new ResourceConflictException("Subject '%s' is not valid for %s.".formatted(normalizedSubjectName, selectedGrade));
                    }
                    return new StudentSubjectAchievementDto(normalizedSubjectName, normalizeAchievementLevel(item.achievementLevel()));
                })
                .toList();
    }

    private Integer normalizeAchievementLevel(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(1, Math.min(7, value));
    }

    private String mergeSubjectAchievements(String existingJson, List<StudentSubjectAchievementDto> incoming, String selectedGrade) {
        if (incoming == null) {
            return existingJson == null ? "[]" : existingJson;
        }
        return writeJson(normalizeSubjectAchievements(incoming, selectedGrade));
    }

    private List<StudentSubjectAchievementDto> readSubjectAchievements(String value) {
        List<StudentSubjectAchievementDto> parsed = readJson(value, SUBJECT_ACHIEVEMENTS_TYPE);
        return normalizeSubjectAchievements(parsed, null);
    }

    private String normalizeSelectedGrade(String grade) {
        String normalized = normalizeValue(grade);
        if (normalized == null) {
            return null;
        }
        if (!SUPPORTED_GRADES.contains(normalized)) {
            throw new ResourceConflictException("Unsupported grade selection. Please choose a grade from Grade 8 to Grade 12.");
        }
        return normalized;
    }

    private Set<String> allowedSubjectsByGrade(String selectedGrade) {
        if ("Grade 8".equals(selectedGrade) || "Grade 9".equals(selectedGrade)) {
            return SENIOR_PHASE_SUBJECTS;
        }
        if ("Grade 10".equals(selectedGrade) || "Grade 11".equals(selectedGrade) || "Grade 12".equals(selectedGrade)) {
            return FET_SUBJECTS;
        }
        return Set.of();
    }

    private String canonicalizeSubjectName(String subjectName) {
        String trimmed = subjectName == null ? "" : subjectName.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }
        String alias = SUBJECT_NAME_ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        if (alias != null) {
            return alias;
        }
        Set<String> allowedSubjects = new LinkedHashSet<>();
        allowedSubjects.addAll(SENIOR_PHASE_SUBJECTS);
        allowedSubjects.addAll(FET_SUBJECTS);
        for (String allowed : allowedSubjects) {
            if (allowed.equalsIgnoreCase(trimmed)) {
                return allowed;
            }
        }
        return trimmed;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Unable to save profile version.");
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Saved profile data is invalid.");
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            if (value == null || value.isBlank()) {
                if (type == SUBJECT_ACHIEVEMENTS_TYPE) {
                    @SuppressWarnings("unchecked")
                    T empty = (T) List.<StudentSubjectAchievementDto>of();
                    return empty;
                }
                return objectMapper.readValue("{}", type);
            }
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            if (type == SUBJECT_ACHIEVEMENTS_TYPE) {
                @SuppressWarnings("unchecked")
                T empty = (T) List.<StudentSubjectAchievementDto>of();
                return empty;
            }
            throw new ResourceConflictException("Saved profile data is invalid.");
        }
    }

    /**
     * this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    /**
     * this method handles the "join" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String join(List<String> values) {
        if (values == null) return null;
        return String.join(",", values);
    }

    private String mergeValue(String existing, String incoming) {
        if (incoming == null) {
            return existing;
        }
        String trimmed = incoming.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String mergeList(String existing, List<String> incoming) {
        if (incoming == null) {
            return existing;
        }
        return join(incoming.stream().map(item -> item == null ? "" : item.trim()).filter(item -> !item.isBlank()).toList());
    }

    /**
     * this method handles the "calculateCompleteness" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile syncProfileCompletion(StudentProfile profile) {
        boolean completed = studentProfileCompletionService.isProfileCompleted(profile);
        if (profile.isProfileCompleted() != completed) {
            profile.setProfileCompleted(completed);
            return repository.save(profile);
        }
        return profile;
    }

    /**
     * this method handles the "validateFile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (file.getSize() > (5 * 1024 * 1024)) throw new IllegalArgumentException("File must be under 5MB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) {
            throw new IllegalArgumentException("Supported file types: pdf, doc, docx");
        }
    }

    private record ReadinessCard(
            int score,
            String band,
            String explanation,
            List<String> strengths,
            List<String> gaps,
            List<String> nextImprovements
    ) {
    }

    private record ReadinessSignal(
            int weight,
            boolean met,
            String strengthMessage,
            String gapMessage,
            String nextAction
    ) {
    }
}

