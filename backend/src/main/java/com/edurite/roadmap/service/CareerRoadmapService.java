package com.edurite.roadmap.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsCalculationResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsReadiness;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsSubjectInput;
import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsSubjectResult;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerOverview;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapSaveRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.GapAnalysis;
import com.edurite.roadmap.dto.CareerRoadmapDtos.PathwayStep;
import com.edurite.roadmap.dto.CareerRoadmapDtos.RoadmapTimelineStage;
import com.edurite.roadmap.dto.CareerRoadmapDtos.SavedCareerRoadmapResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.StudyPlanStep;
import com.edurite.roadmap.dto.CareerRoadmapDtos.SubjectRequirement;
import com.edurite.roadmap.dto.CareerRoadmapDtos.UniversityRequirementResponse;
import com.edurite.roadmap.entity.CareerProgramRequirement;
import com.edurite.roadmap.entity.CareerRoadmap;
import com.edurite.roadmap.entity.SavedCareerRoadmap;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;
import com.edurite.roadmap.repository.CareerRoadmapRepository;
import com.edurite.roadmap.repository.SavedCareerRoadmapRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentSubjectAchievementDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerRoadmapService {

    private static final TypeReference<List<StudentSubjectAchievementDto>> SUBJECT_ACHIEVEMENTS_TYPE = new TypeReference<>() { };
    private static final Pattern LEVEL_PATTERN = Pattern.compile("(?i)(?:level\\s*)?(\\d)");

    private final CareerRoadmapRepository repository;
    private final SavedCareerRoadmapRepository savedCareerRoadmapRepository;
    private final CareerProgramRequirementRepository requirementRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final InstitutionRepository institutionRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final AiCareerRoadmapService aiCareerRoadmapService;

    public CareerRoadmapService(
            CareerRoadmapRepository repository,
            SavedCareerRoadmapRepository savedCareerRoadmapRepository,
            CareerProgramRequirementRepository requirementRepository,
            StudentProfileRepository studentProfileRepository,
            InstitutionRepository institutionRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper,
            AiCareerRoadmapService aiCareerRoadmapService
    ) {
        this.repository = repository;
        this.savedCareerRoadmapRepository = savedCareerRoadmapRepository;
        this.requirementRepository = requirementRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.institutionRepository = institutionRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.aiCareerRoadmapService = aiCareerRoadmapService;
    }

    @Transactional(readOnly = true)
    public List<CareerRoadmapResponse> list() {
        return repository.findByActiveTrueOrderByTitleAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CareerRoadmapResponse detail(String slug) {
        return repository.findBySlugAndActiveTrue(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceConflictException("Career roadmap not found"));
    }

    @Transactional(readOnly = true)
    public ApsCalculationResponse calculateAps(ApsCalculationRequest request) {
        return calculateAps("MANUAL", request.grade(), request.province(), request.subjects(), "Manual subject entry", null);
    }

    @Transactional(readOnly = true)
    public ApsCalculationResponse apsProfile(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> createDefaultProfile(user.getId()));
        List<ApsSubjectInput> inputs = readSubjectAchievements(profile.getSubjectAchievementsJson()).stream()
                .map(item -> new ApsSubjectInput(item.subjectName(), null, item.achievementLevel(), item.achievementLevel()))
                .toList();
        String resultSetLabel = firstNonBlank(profile.getSelectedGrade(), "Profile subjects")
                + (notBlank(profile.getTranscriptFileUrl()) ? " transcript" : " profile");
        String resultSetId = notBlank(profile.getTranscriptFileUrl()) ? profile.getTranscriptFileUrl() : "profile-" + profile.getId();
        return calculateAps("PROFILE", profile.getSelectedGrade(), profile.getLocation(), inputs, resultSetLabel.trim(), resultSetId);
    }

    @Transactional(readOnly = true)
    public List<UniversityRequirementResponse> requirements(String career) {
        return buildUniversityRequirements(career, null, null);
    }

    @Transactional
    public CareerRoadmapGenerateResponse generate(Principal principal, CareerRoadmapGenerateRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> createDefaultProfile(user.getId()));
        ApsCalculationResponse aps = calculateAps(
                "MANUAL",
                firstNonBlank(request.grade(), profile.getSelectedGrade()),
                firstNonBlank(request.province(), profile.getLocation()),
                normalizeSubjectInputs(request.subjects(), profile),
                "Manual subject entry",
                null
        );
        List<UniversityRequirementResponse> universityRequirements = buildUniversityRequirements(request.careerName(), aps.totalAps(), aps.subjects());
        CareerRoadmap legacy = repository.findTopByTitleContainingIgnoreCaseAndActiveTrue(request.careerName()).orElse(null);
        Integer requiredAps = universityRequirements.stream()
                .filter(UniversityRequirementResponse::verified)
                .map(UniversityRequirementResponse::apsRequired)
                .filter(value -> value != null && value > 0)
                .min(Integer::compareTo)
                .orElse(null);
        CareerRoadmapGenerateResponse aiRoadmap = aiCareerRoadmapService.generate(
                new CareerRoadmapGenerateRequest(request.careerName(), aps.grade(), aps.province(), request.subjects()),
                profile,
                legacyContext(legacy),
                requirementsContext(universityRequirements),
                aps.totalAps()
        );
        CareerRoadmapGenerateResponse base = aiRoadmap != null ? aiRoadmap : fallbackRoadmap(request.careerName(), legacy);
        ApsReadiness readiness = buildReadiness(aps.totalAps(), requiredAps, universityRequirements);
        GapAnalysis gapAnalysis = buildGapAnalysis(aps, requiredAps, universityRequirements);
        return new CareerRoadmapGenerateResponse(
                firstNonBlank(base.careerName(), request.careerName()),
                base.overview(),
                mergeRequiredSubjects(base, universityRequirements),
                mergeRecommendedSubjects(base),
                ensurePathway(base.universityPathway(), legacy == null ? null : legacy.getStudyPath(), "University pathway"),
                ensureProfessionalPathway(base.professionalPathway(), request.careerName()),
                ensureTimeline(base.roadmapTimeline()),
                universityRequirements,
                readiness,
                gapAnalysis,
                ensureAlternativePathways(base.alternativePathways(), request.careerName()),
                ensureStudyPlan(base.studyPlan(), gapAnalysis)
        );
    }

    @Transactional
    public SavedCareerRoadmapResponse save(Principal principal, CareerRoadmapSaveRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> createDefaultProfile(user.getId()));
        SavedCareerRoadmap entity = new SavedCareerRoadmap();
        entity.setLearnerId(profile.getId());
        entity.setCareerName(request.careerName().trim());
        entity.setLearnerAps(nullSafe(request.learnerAps()));
        entity.setRequiredAps(request.requiredAps());
        entity.setApsGap(request.apsGap());
        entity.setReadinessScore(nullSafe(request.readinessScore()));
        entity.setRoadmapJson(writeJson(request.roadmap()));
        SavedCareerRoadmap saved = savedCareerRoadmapRepository.save(entity);
        return toSavedResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedCareerRoadmapResponse> saved(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> createDefaultProfile(user.getId()));
        return savedCareerRoadmapRepository.findByLearnerIdOrderByUpdatedAtDesc(profile.getId()).stream()
                .map(this::toSavedResponse)
                .toList();
    }

    private List<ApsSubjectInput> normalizeSubjectInputs(List<ApsSubjectInput> inputs, StudentProfile profile) {
        if (inputs != null && !inputs.isEmpty()) {
            return inputs;
        }
        return readSubjectAchievements(profile.getSubjectAchievementsJson()).stream()
                .map(item -> new ApsSubjectInput(item.subjectName(), null, item.achievementLevel(), item.achievementLevel()))
                .toList();
    }

    private List<UniversityRequirementResponse> buildUniversityRequirements(String career, Integer learnerAps, List<ApsSubjectResult> learnerSubjects) {
        List<CareerProgramRequirement> verified = requirementRepository
                .findByCareerNameContainingIgnoreCaseOrderByVerifiedDescApsRequiredAscInstitutionNameAsc(career == null ? "" : career);
        List<UniversityRequirementResponse> rows = verified.stream()
                .map(item -> toRequirementResponse(item, learnerAps, learnerSubjects))
                .toList();
        if (!rows.isEmpty()) {
            return rows;
        }
        return estimatedRequirements(career, learnerAps, learnerSubjects);
    }

    private List<UniversityRequirementResponse> estimatedRequirements(String career, Integer learnerAps, List<ApsSubjectResult> learnerSubjects) {
        String normalizedCareer = career == null ? "" : career.toLowerCase(Locale.ROOT);
        int estimatedAps = switch (normalizedCareer) {
            case "doctor", "medicine", "mbchb" -> 42;
            case "lawyer", "law" -> 33;
            case "chartered accountant", "accountant" -> 30;
            case "software engineer", "engineer", "engineering" -> 32;
            case "nurse", "nursing" -> 28;
            default -> 28;
        };
        String mathematics = normalizedCareer.contains("engineer") || normalizedCareer.contains("account") || normalizedCareer.contains("doctor")
                ? "Level 5"
                : "Level 4";
        String english = "Level 4";
        String physicalSciences = normalizedCareer.contains("doctor") || normalizedCareer.contains("engineer") ? "Level 5" : null;
        String lifeSciences = normalizedCareer.contains("doctor") || normalizedCareer.contains("nurse") ? "Level 5" : null;
        List<Institution> institutions = institutionRepository.findByActiveTrueOrderByFeaturedDescNameAsc().stream()
                .filter(item -> isSouthAfrican(item.getCountry()))
                .limit(6)
                .toList();
        List<UniversityRequirementResponse> estimates = new ArrayList<>();
        for (Institution institution : institutions) {
            estimates.add(toEstimatedRequirementResponse(
                    institution,
                    career,
                    estimatedAps,
                    mathematics,
                    english,
                    normalizedCareer.contains("account") ? "Level 4" : null,
                    physicalSciences,
                    lifeSciences,
                    learnerAps,
                    learnerSubjects
            ));
        }
        return estimates;
    }

    private UniversityRequirementResponse toEstimatedRequirementResponse(
            Institution institution,
            String career,
            Integer apsRequired,
            String mathematicsRequirement,
            String englishRequirement,
            String accountingRequirement,
            String physicalSciencesRequirement,
            String lifeSciencesRequirement,
            Integer learnerAps,
            List<ApsSubjectResult> learnerSubjects
    ) {
        String status = "Unable to verify";
        return new UniversityRequirementResponse(
                null,
                institution.getName(),
                firstNonBlank(institution.getCategory(), "University"),
                institution.getProvince(),
                "Related qualification for " + career,
                null,
                apsRequired,
                mathematicsRequirement,
                null,
                englishRequirement,
                accountingRequirement,
                physicalSciencesRequirement,
                lifeSciencesRequirement,
                "3-4 years",
                null,
                institution.getWebsite(),
                "Estimated pathway only. EduRite could not verify institution-specific admission requirements for this programme.",
                "AI estimated from career pathway alignment",
                false,
                "AI Estimate - Verify with University",
                status,
                null
        );
    }

    private UniversityRequirementResponse toRequirementResponse(CareerProgramRequirement item, Integer learnerAps, List<ApsSubjectResult> learnerSubjects) {
        boolean verified = item.isVerified();
        String status = determineStatus(
                verified,
                learnerAps,
                item.getApsRequired(),
                learnerSubjects,
                item.getMathematicsRequirement(),
                item.getMathematicalLiteracyRequirement(),
                item.getEnglishRequirement(),
                item.getAccountingRequirement(),
                item.getPhysicalSciencesRequirement(),
                item.getLifeSciencesRequirement()
        );
        return new UniversityRequirementResponse(
                item.getId(),
                item.getInstitutionName(),
                item.getInstitutionType(),
                item.getProvince(),
                item.getQualificationName(),
                item.getFaculty(),
                item.getApsRequired(),
                item.getMathematicsRequirement(),
                item.getMathematicalLiteracyRequirement(),
                item.getEnglishRequirement(),
                item.getAccountingRequirement(),
                item.getPhysicalSciencesRequirement(),
                item.getLifeSciencesRequirement(),
                item.getDuration(),
                item.getNqfLevel(),
                item.getApplicationUrl(),
                item.getNotes(),
                item.getSource(),
                verified,
                verified ? "Verified Requirement" : "AI Estimate - Verify with University",
                status,
                verified ? apsGap(learnerAps, item.getApsRequired()) : null
        );
    }

    private ApsReadiness buildReadiness(Integer learnerAps, Integer requiredAps, List<UniversityRequirementResponse> rows) {
        boolean hasVerifiedRequirements = rows.stream().anyMatch(UniversityRequirementResponse::verified);
        if (!hasVerifiedRequirements || requiredAps == null || learnerAps == null) {
            return new ApsReadiness(learnerAps, requiredAps, null, 0, hasVerifiedRequirements ? "Unable to determine" : "Requirements not verified", 0);
        }
        int gap = Math.max(0, requiredAps - nullSafe(learnerAps));
        long bestFit = rows.stream()
                .filter(UniversityRequirementResponse::verified)
                .filter(item -> "Eligible".equalsIgnoreCase(item.requirementStatus()) || "Almost Eligible".equalsIgnoreCase(item.requirementStatus()))
                .count();
        int score = Math.max(0, Math.min(100, 100 - (gap * 12) + (int) bestFit * 5));
        String status = gap == 0 && bestFit > 0 ? "Eligible" : gap <= 3 ? "Almost Eligible" : "Not Yet Eligible";
        return new ApsReadiness(nullSafe(learnerAps), requiredAps, gap, score, status, (int) bestFit);
    }

    private GapAnalysis buildGapAnalysis(ApsCalculationResponse aps, Integer requiredAps, List<UniversityRequirementResponse> rows) {
        List<UniversityRequirementResponse> verifiedRows = rows.stream()
                .filter(UniversityRequirementResponse::verified)
                .toList();
        Integer learnerAps = aps.totalAps();
        Integer gap = learnerAps == null || requiredAps == null ? null : Math.max(0, requiredAps - learnerAps);
        Set<String> missingSubjects = new LinkedHashSet<>();
        Set<String> improvements = new LinkedHashSet<>();
        List<String> bestFit = verifiedRows.stream()
                .filter(item -> "Eligible".equalsIgnoreCase(item.requirementStatus()) || "Almost Eligible".equalsIgnoreCase(item.requirementStatus()))
                .sorted(Comparator.comparing(item -> nullSafe(item.apsGap())))
                .map(item -> item.institutionName() + " - " + item.qualificationName())
                .limit(4)
                .toList();

        Map<String, Integer> subjectLevels = subjectLevelMap(aps.subjects());
        collectSubjectGap(missingSubjects, improvements, subjectLevels, "Mathematics", verifiedRows.stream().map(UniversityRequirementResponse::mathematicsRequirement).toList());
        collectSubjectGap(missingSubjects, improvements, subjectLevels, "English", verifiedRows.stream().map(UniversityRequirementResponse::englishRequirement).toList());
        collectSubjectGap(missingSubjects, improvements, subjectLevels, "Accounting", verifiedRows.stream().map(UniversityRequirementResponse::accountingRequirement).toList());
        collectSubjectGap(missingSubjects, improvements, subjectLevels, "Physical Sciences", verifiedRows.stream().map(UniversityRequirementResponse::physicalSciencesRequirement).toList());
        collectSubjectGap(missingSubjects, improvements, subjectLevels, "Life Sciences", verifiedRows.stream().map(UniversityRequirementResponse::lifeSciencesRequirement).toList());

        List<String> suggestions = new ArrayList<>();
        if (gap != null && gap > 0) {
            suggestions.add("Improve your APS by " + gap + " point" + (gap == 1 ? "" : "s") + ".");
        }
        for (String improvement : improvements) {
            suggestions.add("Improve " + improvement + ".");
        }
        if (missingSubjects.contains("Accounting")) {
            suggestions.add("Add Accounting if your school offers it and the career pathway values it.");
        }
        if (verifiedRows.isEmpty()) {
            suggestions.add("Institution-specific admission requirements could not be verified. Treat these universities as exploratory guidance only.");
        } else {
            suggestions.add("Consider diploma, college, or TVET alternatives as a backup pathway.");
        }

        String risk = gap == null ? "Unable to determine" : gap == 0 ? "Low" : gap <= 3 ? "Medium" : "High";
        return new GapAnalysis(
                learnerAps,
                requiredAps,
                gap,
                List.copyOf(missingSubjects),
                List.copyOf(improvements),
                bestFit,
                risk,
                suggestions.stream().distinct().limit(6).toList()
        );
    }

    private void collectSubjectGap(Set<String> missingSubjects, Set<String> improvements, Map<String, Integer> subjectLevels, String subject, List<String> requirements) {
        int requiredLevel = requirements.stream()
                .map(this::extractLevel)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0);
        if (requiredLevel == 0) {
            return;
        }
        Integer learnerLevel = resolveLevel(subjectLevels, subject);
        if (learnerLevel == null) {
            missingSubjects.add(subject);
            return;
        }
        if (learnerLevel < requiredLevel) {
            improvements.add(subject + " from Level " + learnerLevel + " to Level " + requiredLevel);
        }
    }

    private Integer resolveLevel(Map<String, Integer> subjectLevels, String subject) {
        for (Map.Entry<String, Integer> entry : subjectLevels.entrySet()) {
            if (entry.getKey().contains(subject.toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Integer> subjectLevelMap(List<ApsSubjectResult> rows) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (ApsSubjectResult row : rows) {
            if (row == null || row.subjectName() == null) {
                continue;
            }
            map.put(row.subjectName().toLowerCase(Locale.ROOT), row.level());
        }
        return map;
    }

    private String determineStatus(
            boolean verifiedRequirement,
            Integer learnerAps,
            Integer requiredAps,
            List<ApsSubjectResult> learnerSubjects,
            String mathematicsRequirement,
            String mathematicalLiteracyRequirement,
            String englishRequirement,
            String accountingRequirement,
            String physicalSciencesRequirement,
            String lifeSciencesRequirement
    ) {
        if (!verifiedRequirement || requiredAps == null || learnerAps == null) {
            return "Unable to verify";
        }
        boolean subjectRequirementsMet = meetsRequirement(learnerSubjects, "mathematics", mathematicsRequirement)
                && meetsRequirement(learnerSubjects, "mathematical literacy", mathematicalLiteracyRequirement)
                && meetsRequirement(learnerSubjects, "english", englishRequirement)
                && meetsRequirement(learnerSubjects, "accounting", accountingRequirement)
                && meetsRequirement(learnerSubjects, "physical sciences", physicalSciencesRequirement)
                && meetsRequirement(learnerSubjects, "life sciences", lifeSciencesRequirement);
        int gap = apsGap(learnerAps, requiredAps);
        if (subjectRequirementsMet && gap == 0) {
            return "Eligible";
        }
        if (gap <= 3 && subjectRequirementsMet) {
            return "Almost Eligible";
        }
        return "Not Yet Eligible";
    }

    private boolean meetsRequirement(List<ApsSubjectResult> learnerSubjects, String keyword, String requirement) {
        Integer neededLevel = extractLevel(requirement);
        if (neededLevel == null || neededLevel <= 0) {
            return true;
        }
        if (learnerSubjects == null || learnerSubjects.isEmpty()) {
            return false;
        }
        for (ApsSubjectResult subject : learnerSubjects) {
            if (subject.subjectName() != null
                    && subject.subjectName().toLowerCase(Locale.ROOT).contains(keyword)
                    && nullSafe(subject.level()) >= neededLevel) {
                return true;
            }
        }
        return false;
    }

    private Integer extractLevel(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return null;
        }
        Matcher matcher = LEVEL_PATTERN.matcher(requirement);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int apsGap(Integer learnerAps, Integer requiredAps) {
        return Math.max(0, nullSafe(requiredAps) - nullSafe(learnerAps));
    }

    private List<SubjectRequirement> mergeRequiredSubjects(CareerRoadmapGenerateResponse base, List<UniversityRequirementResponse> requirements) {
        LinkedHashMap<String, SubjectRequirement> merged = new LinkedHashMap<>();
        if (base.requiredSubjects() != null) {
            for (SubjectRequirement item : base.requiredSubjects()) {
                merged.put(item.subject().toLowerCase(Locale.ROOT), item);
            }
        }
        mergeRequirementSubject(merged, "Mathematics", requirements.stream().map(UniversityRequirementResponse::mathematicsRequirement).toList(), true);
        mergeRequirementSubject(merged, "Mathematical Literacy", requirements.stream().map(UniversityRequirementResponse::mathematicalLiteracyRequirement).toList(), true);
        mergeRequirementSubject(merged, "English", requirements.stream().map(UniversityRequirementResponse::englishRequirement).toList(), true);
        mergeRequirementSubject(merged, "Accounting", requirements.stream().map(UniversityRequirementResponse::accountingRequirement).toList(), false);
        mergeRequirementSubject(merged, "Physical Sciences", requirements.stream().map(UniversityRequirementResponse::physicalSciencesRequirement).toList(), false);
        mergeRequirementSubject(merged, "Life Sciences", requirements.stream().map(UniversityRequirementResponse::lifeSciencesRequirement).toList(), false);
        return new ArrayList<>(merged.values());
    }

    private void mergeRequirementSubject(
            Map<String, SubjectRequirement> target,
            String subject,
            List<String> requirements,
            boolean required
    ) {
        int level = requirements.stream()
                .map(this::extractLevel)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0);
        if (level == 0) {
            return;
        }
        target.putIfAbsent(subject.toLowerCase(Locale.ROOT),
                new SubjectRequirement(subject, level, "Level " + level, levelToMark(level), required, "Based on programme requirements"));
    }

    private List<SubjectRequirement> mergeRecommendedSubjects(CareerRoadmapGenerateResponse base) {
        return base.recommendedSubjects() == null ? List.of() : base.recommendedSubjects();
    }

    private List<PathwayStep> ensurePathway(List<PathwayStep> steps, String fallbackText, String fallbackTitle) {
        if (steps != null && !steps.isEmpty()) {
            return steps;
        }
        if (fallbackText == null || fallbackText.isBlank()) {
            return List.of();
        }
        return List.of(new PathwayStep(fallbackTitle, fallbackText.trim()));
    }

    private List<PathwayStep> ensureProfessionalPathway(List<PathwayStep> steps, String careerName) {
        if (steps != null && !steps.isEmpty()) {
            return steps;
        }
        return List.of(
                new PathwayStep("Qualification completion", "Complete the core qualification aligned to " + careerName + "."),
                new PathwayStep("Workplace exposure", "Build experience through internship, practical training, or articles where relevant."),
                new PathwayStep("Professional progression", "Pursue registration, licensing, or board assessments required in the field.")
        );
    }

    private List<RoadmapTimelineStage> ensureTimeline(List<RoadmapTimelineStage> timeline) {
        if (timeline != null && !timeline.isEmpty()) {
            return timeline;
        }
        return List.of(
                new RoadmapTimelineStage(1, "Grade 9", "Choose the right subject combination early."),
                new RoadmapTimelineStage(2, "Grade 10-12", "Strengthen marks in required subjects and monitor APS."),
                new RoadmapTimelineStage(3, "Grade 12", "Calculate APS and submit applications on time."),
                new RoadmapTimelineStage(4, "Higher education", "Complete the qualification pathway."),
                new RoadmapTimelineStage(5, "Career entry", "Transition into internship, graduate, or professional entry roles.")
        );
    }

    private List<String> ensureAlternativePathways(List<String> alternatives, String careerName) {
        if (alternatives != null && !alternatives.isEmpty()) {
            return alternatives;
        }
        return List.of(
                "Consider diploma or extended curriculum programmes linked to " + careerName + ".",
                "Explore college or TVET pathways where available before articulating into degree study.",
                "Build supporting skills and re-apply with stronger APS and subject results."
        );
    }

    private List<StudyPlanStep> ensureStudyPlan(List<StudyPlanStep> studyPlan, GapAnalysis gapAnalysis) {
        if (studyPlan != null && !studyPlan.isEmpty()) {
            return studyPlan;
        }
        return List.of(
                new StudyPlanStep("APS recovery", "Lift the subjects that most affect your admission score.", gapAnalysis.improvementSuggestions()),
                new StudyPlanStep("Application readiness", "Track requirements, deadlines, and supporting documents.", List.of(
                        "Verify final requirements directly with each university.",
                        "Prepare your Grade 11 and Grade 12 results for applications.",
                        "Shortlist backup programmes and institutions."
                ))
        );
    }

    private CareerRoadmapGenerateResponse fallbackRoadmap(String careerName, CareerRoadmap legacy) {
        return new CareerRoadmapGenerateResponse(
                careerName,
                new CareerOverview(
                        clean(legacy == null ? null : legacy.getOverview()),
                        List.of("Understand the day-to-day work expected in this career."),
                        splitText(legacy == null ? null : legacy.getRecommendedSkills()),
                        "Check current labour-market demand by province and field.",
                        "Varies by employer, experience, and specialisation.",
                        ""
                ),
                splitSubjects(legacy == null ? null : legacy.getRequiredSubjects(), true),
                List.of(),
                ensurePathway(List.of(), legacy == null ? null : legacy.getStudyPath(), "University pathway"),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private List<SubjectRequirement> splitSubjects(String value, boolean required) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return splitText(value).stream()
                .map(item -> new SubjectRequirement(item, null, "", "", required, ""))
                .toList();
    }

    private List<String> splitText(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String legacyContext(CareerRoadmap legacy) {
        if (legacy == null) {
            return "";
        }
        return """
                Overview: %s
                Required subjects: %s
                Recommended skills: %s
                Study path: %s
                Entry roles: %s
                Long term growth: %s
                Resources: %s
                """.formatted(
                clean(legacy.getOverview()),
                clean(legacy.getRequiredSubjects()),
                clean(legacy.getRecommendedSkills()),
                clean(legacy.getStudyPath()),
                clean(legacy.getEntryLevelJobs()),
                clean(legacy.getLongTermGrowth()),
                clean(legacy.getLearningResources())
        );
    }

    private String requirementsContext(List<UniversityRequirementResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        return rows.stream()
                .limit(8)
                .map(item -> "%s | %s | APS %s | Maths %s | English %s".formatted(
                        item.institutionName(),
                        item.qualificationName(),
                        item.apsRequired(),
                        clean(item.mathematicsRequirement()),
                        clean(item.englishRequirement())
                ))
                .toList()
                .toString();
    }

    private ApsCalculationResponse calculateAps(String source, String grade, String province, List<ApsSubjectInput> subjects, String resultSetLabel, String resultSetId) {
        List<ApsSubjectResult> resolved = new ArrayList<>();
        List<String> missingRequirements = new ArrayList<>();
        int total = 0;
        int includedSubjects = 0;
        if (subjects != null) {
            for (ApsSubjectInput subject : subjects) {
                if (subject == null || subject.subjectName() == null || subject.subjectName().isBlank()) {
                    continue;
                }
                Integer level = normalizeLevel(subject.markPercentage(), subject.level(), subject.apsPoints());
                Integer aps = level == null ? null : Math.max(1, Math.min(7, level));
                boolean includeInTotal = !isLifeOrientation(subject.subjectName());
                String exclusionReason = includeInTotal ? null : "Excluded from general APS preview";
                resolved.add(new ApsSubjectResult(subject.subjectName().trim(), subject.markPercentage(), level, aps, includeInTotal, exclusionReason));
                if (includeInTotal && aps != null) {
                    total += aps;
                    includedSubjects += 1;
                }
            }
        }
        String status;
        Integer totalValue;
        if (resolved.isEmpty()) {
            status = "UNAVAILABLE";
            totalValue = null;
            missingRequirements.add("Add at least one valid subject result.");
        } else if (includedSubjects < 6) {
            status = "PROVISIONAL";
            totalValue = total;
            int remainingSubjects = 6 - includedSubjects;
            missingRequirements.add("Add " + remainingSubjects + " more APS-counted subject" + (remainingSubjects == 1 ? "" : "s") + " for a fuller APS preview.");
        } else {
            status = "COMPLETE";
            totalValue = total;
        }
        return new ApsCalculationResponse(source, status, grade, province, resolved, totalValue, missingRequirements, "General NSC APS preview (Life Orientation excluded)", false, resultSetLabel, resultSetId);
    }

    private Integer normalizeLevel(Integer markPercentage, Integer level, Integer apsPoints) {
        if (markPercentage != null) {
            if (markPercentage < 0 || markPercentage > 100) {
                return null;
            }
            if (markPercentage >= 80) return 7;
            if (markPercentage >= 70) return 6;
            if (markPercentage >= 60) return 5;
            if (markPercentage >= 50) return 4;
            if (markPercentage >= 40) return 3;
            if (markPercentage >= 30) return 2;
            return 1;
        }
        if (level != null) {
            return Math.max(1, Math.min(7, level));
        }
        if (apsPoints != null) {
            return Math.max(1, Math.min(7, apsPoints));
        }
        return null;
    }

    private boolean isLifeOrientation(String subjectName) {
        return subjectName != null && subjectName.toLowerCase(Locale.ROOT).contains("life orientation");
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private SavedCareerRoadmapResponse toSavedResponse(SavedCareerRoadmap saved) {
        return new SavedCareerRoadmapResponse(
                saved.getId(),
                saved.getCareerName(),
                readSavedRoadmap(saved.getRoadmapJson()),
                saved.getLearnerAps(),
                saved.getRequiredAps(),
                saved.getApsGap(),
                saved.getReadinessScore(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private CareerRoadmapGenerateResponse readSavedRoadmap(String value) {
        try {
            return objectMapper.readValue(value, CareerRoadmapGenerateResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Saved roadmap data is invalid.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Unable to save roadmap.");
        }
    }

    private List<StudentSubjectAchievementDto> readSubjectAchievements(String value) {
        try {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(value, SUBJECT_ACHIEVEMENTS_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private StudentProfile createDefaultProfile(UUID userId) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setSubjectAchievementsJson("[]");
        return profile;
    }

    private CareerRoadmapResponse toResponse(CareerRoadmap roadmap) {
        return new CareerRoadmapResponse(
                roadmap.getId(),
                roadmap.getSlug(),
                roadmap.getTitle(),
                clean(roadmap.getOverview()),
                clean(roadmap.getRequiredSubjects()),
                clean(roadmap.getRecommendedSkills()),
                clean(roadmap.getStudyPath()),
                clean(roadmap.getEntryLevelJobs()),
                clean(roadmap.getLongTermGrowth()),
                clean(roadmap.getLearningResources())
        );
    }

    private boolean isSouthAfrican(String country) {
        if (country == null || country.isBlank()) {
            return true;
        }
        String normalized = country.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("south africa") || normalized.equals("sa");
    }

    private Integer nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String levelToMark(Integer level) {
        return switch (nullSafe(level)) {
            case 7 -> "80%+";
            case 6 -> "70%+";
            case 5 -> "60%+";
            case 4 -> "50%+";
            case 3 -> "40%+";
            case 2 -> "30%+";
            default -> "Below 30%";
        };
    }
}
