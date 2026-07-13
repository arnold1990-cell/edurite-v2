package com.edurite.curriculum.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.edurite.curriculum.repository.CurriculumAssetSummaryView;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurriculumResourceService {
    private static final Logger log = LoggerFactory.getLogger(CurriculumResourceService.class);

    public static final String OWNER_DISTRICT = "DISTRICT";
    public static final String OWNER_SCHOOL = "SCHOOL";
    public static final String SOURCE_DISTRICT = "DISTRICT";
    public static final String SOURCE_SUBJECT_ADVISOR = "SUBJECT_ADVISOR";
    public static final String SOURCE_SCHOOL = "SCHOOL";
    public static final String VISIBILITY_DISTRICT_WIDE = "DISTRICT_WIDE";
    public static final String VISIBILITY_SCHOOL_ONLY = "SCHOOL_ONLY";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final CurriculumAssetRepository curriculumAssetRepository;
    private final SchoolRepository schoolRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final UserRepository userRepository;

    public CurriculumResourceService(
            CurriculumAssetRepository curriculumAssetRepository,
            SchoolRepository schoolRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            UserRepository userRepository
    ) {
        this.curriculumAssetRepository = curriculumAssetRepository;
        this.schoolRepository = schoolRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> getDistrictResourcesForSchool(UUID schoolId, CurriculumDtos.CurriculumResourceQuery query) {
        School school = requireSchool(schoolId);
        return findVisibleResourcesForSchool(school).stream()
                .filter(asset -> matchesQuery(asset, query))
                .map(asset -> toAssetDto(asset, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> getDistrictResourcesForTeacher(UUID schoolId, UUID teacherUserId, CurriculumDtos.CurriculumResourceQuery query) {
        School school = requireSchool(schoolId);
        AssignmentScope scope = assignmentScope(schoolId, teacherUserId);
        return findVisibleResourcesForSchool(school).stream()
                .filter(asset -> matchesQuery(asset, query))
                .map(asset -> toTeacherAssetDto(asset, teacherUserId, scope))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> getActiveDistrictResources(UUID districtId, CurriculumDtos.CurriculumResourceQuery query) {
        return activeDistrictAssets().stream()
                .filter(asset -> districtId == null || Objects.equals(asset.getDistrictId(), districtId))
                .filter(asset -> matchesQuery(asset, query))
                .map(asset -> toAssetDto(asset, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceFileResponse downloadResourceForSchool(UUID schoolId, UUID resourceId, String format) {
        CurriculumAsset asset = requireSchoolVisibleAsset(schoolId, resourceId);
        return selectFile(asset, format, false);
    }

    @Transactional(readOnly = true)
    public ResourceFileResponse viewResourceForSchool(UUID schoolId, UUID resourceId, String format) {
        CurriculumAsset asset = requireSchoolVisibleAsset(schoolId, resourceId);
        return selectFile(asset, format, true);
    }

    @Transactional(readOnly = true)
    public ResourceFileResponse downloadResourceForTeacher(UUID schoolId, UUID teacherUserId, UUID resourceId, String format) {
        CurriculumAsset asset = requireTeacherVisibleAsset(schoolId, teacherUserId, resourceId);
        return selectFile(asset, format, false);
    }

    @Transactional(readOnly = true)
    public ResourceFileResponse viewResourceForTeacher(UUID schoolId, UUID teacherUserId, UUID resourceId, String format) {
        CurriculumAsset asset = requireTeacherVisibleAsset(schoolId, teacherUserId, resourceId);
        return selectFile(asset, format, true);
    }

    public CurriculumDtos.CurriculumAssetDto toAssetDto(CurriculumAsset asset) {
        return toAssetDto(asset, null, null);
    }

    public CurriculumDtos.CurriculumAssetDto toAssetDto(CurriculumAssetSummaryView asset, String uploadedBy) {
        return new CurriculumDtos.CurriculumAssetDto(
                asset.getId(),
                asset.getRepositoryType(),
                asset.getContentSource(),
                normalizeSource(asset),
                firstNonBlank(asset.getVisibility(), defaultVisibility(asset)),
                firstNonBlank(asset.getStatus(), STATUS_ACTIVE),
                firstNonBlank(asset.getExtractionStatus(), "PENDING"),
                asset.getExtractionError(),
                badgeForAsset(asset),
                asset.getTitle(),
                asset.getSubject(),
                asset.getGrade(),
                asset.getCurriculumPhase(),
                asset.getAcademicYear(),
                asset.getProvince(),
                asset.getVersionNumber(),
                asset.getDescription(),
                asset.getTerm(),
                asset.getWeekNumber(),
                firstNonBlank(uploadedBy, "System"),
                asset.getUploadDate(),
                asset.getExtractedAt(),
                asset.isArchived(),
                asset.isActive(),
                asset.isDeleted(),
                hasFileReference(asset.getPdfFileName()),
                hasFileReference(asset.getDocxFileName()),
                hasFileReference(asset.getExcelFileName()),
                summaryScopeLabel(asset),
                null,
                null,
                null,
                false
        );
    }

    public record ResourceFileResponse(
            String fileName,
            String contentType,
            byte[] content,
            boolean inline
    ) {}

    private CurriculumDtos.CurriculumAssetDto toTeacherAssetDto(CurriculumAsset asset, UUID teacherUserId, AssignmentScope scope) {
        MatchResult match = matchForTeacher(asset, teacherUserId, scope);
        if (!match.visible()) {
            return null;
        }
        return toAssetDto(asset, match.assignmentMatched(), match.reason());
    }

    private CurriculumDtos.CurriculumAssetDto toAssetDto(CurriculumAsset asset, Boolean assignmentMatched, String assignmentReason) {
        return new CurriculumDtos.CurriculumAssetDto(
                asset.getId(),
                asset.getRepositoryType(),
                asset.getContentSource(),
                normalizeSource(asset),
                firstNonBlank(asset.getVisibility(), defaultVisibility(asset)),
                firstNonBlank(asset.getStatus(), STATUS_ACTIVE),
                firstNonBlank(asset.getExtractionStatus(), "PENDING"),
                asset.getExtractionError(),
                badgeForAsset(asset),
                asset.getTitle(),
                asset.getSubject(),
                asset.getGrade(),
                asset.getCurriculumPhase(),
                asset.getAcademicYear(),
                asset.getProvince(),
                asset.getVersionNumber(),
                asset.getDescription(),
                asset.getTerm(),
                asset.getWeekNumber(),
                uploadedByName(asset.getUploadedByUserId()),
                asset.getUploadDate(),
                asset.getExtractedAt(),
                asset.isArchived(),
                asset.isActive(),
                asset.isDeleted(),
                hasContent(asset.getPdfBytes(), asset.getPdfBase64()),
                hasContent(asset.getDocxBytes(), asset.getDocxBase64()),
                hasContent(asset.getExcelBytes(), asset.getExcelBase64()),
                scopeLabel(asset),
                assignmentMatched,
                assignmentReason,
                firstNonBlank(asset.getLessonPlanStatus(), null),
                asset.isGeneratedByAi()
        );
    }

    private MatchResult matchForTeacher(CurriculumAsset asset, UUID teacherUserId, AssignmentScope scope) {
        if (isTeacherOwnedLessonPlan(asset, teacherUserId)) {
            return new MatchResult(true, true, "Generated by you.");
        }
        if ("LESSON_PLAN".equalsIgnoreCase(asset.getRepositoryType())) {
            boolean matched = matchesTeacherScope(asset, scope.subjects(), scope.grades(), scope.phases());
            boolean published = "PUBLISHED".equalsIgnoreCase(firstNonBlank(asset.getLessonPlanStatus(), ""));
            if (matched && published) {
                return new MatchResult(true, true, "Matches your teaching assignment.");
            }
            return new MatchResult(false, false, "");
        }
        if (scope.empty()) {
            return new MatchResult(true, null, "No teaching assignment is mapped to your account yet.");
        }
        boolean subjectMatch = scope.subjects().isEmpty() || scope.subjects().contains(normalizeKey(asset.getSubject()));
        boolean gradeMatch = scope.grades().isEmpty() || scope.grades().stream().anyMatch(item -> gradesOverlap(asset.getGrade(), item));
        boolean phaseMatch = scope.phases().isEmpty() || scope.phases().contains(normalizeKey(asset.getCurriculumPhase()));
        boolean matched = subjectMatch && gradeMatch && phaseMatch;
        return new MatchResult(true, matched, matched ? "Matches your teaching assignment." : mismatchReason(subjectMatch, gradeMatch, phaseMatch));
    }

    private String mismatchReason(boolean subjectMatch, boolean gradeMatch, boolean phaseMatch) {
        List<String> reasons = new ArrayList<>();
        if (!subjectMatch) {
            reasons.add("Subject does not match your assignment");
        }
        if (!gradeMatch) {
            reasons.add("Grade does not match your assignment");
        }
        if (!phaseMatch) {
            reasons.add("Phase does not match your assignment");
        }
        return reasons.isEmpty() ? null : String.join("; ", reasons) + ".";
    }

    private AssignmentScope assignmentScope(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        if (assignments.isEmpty()) {
            return new AssignmentScope(Set.of(), Set.of(), Set.of());
        }
        Map<UUID, SchoolSubject> subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolSubject::getId, item -> item, (left, right) -> left));
        Set<String> subjects = new LinkedHashSet<>();
        Set<String> grades = new LinkedHashSet<>();
        Set<String> phases = new LinkedHashSet<>();
        for (TeacherAssignment assignment : assignments) {
            SchoolSubject subject = subjectById.get(assignment.getSubjectId());
            if (subject != null) {
                subjects.add(normalizeKey(subject.getSubjectName()));
                addGrade(grades, firstNonBlank(subject.getGrade(), subject.getGradeRange()));
                addPhase(phases, subject.getPhase());
            }
            addGrade(grades, assignment.getGrade());
            addPhase(phases, assignment.getPhase());
        }
        return new AssignmentScope(subjects, grades, phases);
    }

    private List<CurriculumAsset> findVisibleDistrictResourcesForSchool(School school) {
        return activeDistrictAssets().stream()
                .filter(asset -> Objects.equals(asset.getDistrictId(), school.getDistrictId()))
                .filter(asset -> isVisibleToSchool(asset, school))
                .sorted(Comparator.comparing(CurriculumAsset::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<CurriculumAsset> findVisibleResourcesForSchool(School school) {
        List<CurriculumAsset> merged = new ArrayList<>(findVisibleDistrictResourcesForSchool(school));
        merged.addAll(curriculumAssetRepository.findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(school.getId()).stream()
                .filter(this::isActiveResource)
                .toList());
        return merged.stream()
                .sorted(Comparator.comparing(CurriculumAsset::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private CurriculumAsset requireSchoolVisibleAsset(UUID schoolId, UUID resourceId) {
        School school = requireSchool(schoolId);
        return findVisibleResourcesForSchool(school).stream()
                .filter(asset -> asset.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Curriculum resource not found."));
    }

    private CurriculumAsset requireTeacherVisibleAsset(UUID schoolId, UUID teacherUserId, UUID resourceId) {
        AssignmentScope scope = assignmentScope(schoolId, teacherUserId);
        School school = requireSchool(schoolId);
        return findVisibleResourcesForSchool(school).stream()
                .filter(asset -> Objects.equals(asset.getId(), resourceId))
                .filter(asset -> matchForTeacher(asset, teacherUserId, scope).visible())
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Curriculum resource not found."));
    }

    private ResourceFileResponse selectFile(CurriculumAsset asset, String requestedFormat, boolean inline) {
        String normalized = requestedFormat == null ? "" : requestedFormat.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return fileForFormat(asset, normalized, inline);
        }
        if (hasContent(asset.getPdfBytes(), asset.getPdfBase64())) {
            return fileForFormat(asset, "PDF", inline);
        }
        if (hasContent(asset.getDocxBytes(), asset.getDocxBase64())) {
            return fileForFormat(asset, "DOCX", inline);
        }
        if (hasContent(asset.getExcelBytes(), asset.getExcelBase64())) {
            return fileForFormat(asset, "EXCEL", inline);
        }
        throw new ResourceConflictException("No file is available for this curriculum resource.");
    }

    private ResourceFileResponse fileForFormat(CurriculumAsset asset, String format, boolean inline) {
        return switch (format) {
            case "PDF" -> buildFileResponse(asset, asset.getPdfFileName(), asset.getPdfContentType(), asset.getPdfBytes(), asset.getPdfBase64(), inline, "PDF");
            case "DOC", "DOCX" -> buildFileResponse(asset, asset.getDocxFileName(), asset.getDocxContentType(), asset.getDocxBytes(), asset.getDocxBase64(), false, "DOCX");
            case "EXCEL", "XLS", "XLSX" -> buildFileResponse(asset, asset.getExcelFileName(), asset.getExcelContentType(), asset.getExcelBytes(), asset.getExcelBase64(), false, "Excel");
            default -> throw new ResourceConflictException("Unsupported format requested.");
        };
    }

    private ResourceFileResponse buildFileResponse(CurriculumAsset asset, String fileName, String contentType, byte[] binaryContent, String legacyBase64Content, boolean inline, String label) {
        byte[] content = resolveFileBytes(binaryContent, legacyBase64Content);
        if (content == null || content.length == 0) {
            log.warn("Curriculum resource download failed resourceId={} fileName={} contentType={} status=MISSING_FILE", asset.getId(), fileName, contentType);
            throw new ResourceConflictException(label + " file is not available for this curriculum resource.");
        }
        return new ResourceFileResponse(
                firstNonBlank(fileName, label.toLowerCase(Locale.ROOT) + "-resource"),
                firstNonBlank(contentType, defaultContentType(label)),
                content,
                inline
        );
    }

    private boolean matchesTeacherScope(CurriculumAsset asset, Set<String> assignedSubjects, Set<String> assignedGrades, Set<String> assignedPhases) {
        boolean subjectMatch = assignedSubjects.isEmpty() || assignedSubjects.contains(normalizeKey(asset.getSubject()));
        boolean gradeMatch = assignedGrades.isEmpty() || assignedGrades.stream().anyMatch(grade -> gradesOverlap(asset.getGrade(), grade));
        boolean phaseMatch = assignedPhases.isEmpty() || assignedPhases.contains(normalizeKey(asset.getCurriculumPhase()));
        return subjectMatch && gradeMatch && phaseMatch;
    }

    private boolean isTeacherOwnedLessonPlan(CurriculumAsset asset, UUID teacherUserId) {
        return asset != null
                && "LESSON_PLAN".equalsIgnoreCase(asset.getRepositoryType())
                && teacherUserId != null
                && (Objects.equals(asset.getTeacherUserId(), teacherUserId) || Objects.equals(asset.getUploadedByUserId(), teacherUserId));
    }

    private boolean matchesQuery(CurriculumAsset asset, CurriculumDtos.CurriculumResourceQuery query) {
        if (query == null) {
            return true;
        }
        return matchesValue(normalizeType(asset.getRepositoryType()), normalizeType(query.type()))
                && matchesValue(normalizeKey(asset.getSubject()), normalizeKey(query.subject()))
                && gradeMatchesQuery(asset.getGrade(), query.grade())
                && matchesValue(normalizeKey(asset.getCurriculumPhase()), normalizeKey(query.phase()))
                && matchesValue(normalizeKey(asset.getTerm()), normalizeKey(query.term()))
                && matchesNumber(asset.getWeekNumber(), query.week())
                && matchesNumber(asset.getAcademicYear(), query.academicYear());
    }

    private boolean gradeMatchesQuery(String assetGrade, String requestedGrade) {
        String normalizedRequested = normalizeGrade(requestedGrade);
        return normalizedRequested.isBlank() || gradesOverlap(assetGrade, requestedGrade);
    }

    private boolean matchesValue(String actual, String expected) {
        return expected.isBlank() || Objects.equals(actual, expected);
    }

    private boolean matchesNumber(Integer actual, Integer expected) {
        return expected == null || Objects.equals(actual, expected);
    }

    private boolean isVisibleToSchool(CurriculumAsset asset, School school) {
        if (asset.getSchoolId() != null && !Objects.equals(asset.getSchoolId(), school.getId())) {
            return false;
        }
        if (!Objects.equals(asset.getDistrictId(), school.getDistrictId())) {
            return false;
        }
        if (asset.getProvince() != null && school.getProvince() != null
                && !asset.getProvince().isBlank()
                && !school.getProvince().isBlank()
                && !asset.getProvince().equalsIgnoreCase(school.getProvince())) {
            return false;
        }
        return true;
    }

    private List<CurriculumAsset> activeDistrictAssets() {
        return curriculumAssetRepository.findAll().stream()
                .filter(this::isDistrictAsset)
                .filter(this::isActiveResource)
                .toList();
    }

    private boolean isDistrictAsset(CurriculumAsset asset) {
        return OWNER_DISTRICT.equalsIgnoreCase(firstNonBlank(asset.getOwnerScope(), ""))
                || SOURCE_DISTRICT.equalsIgnoreCase(firstNonBlank(asset.getSource(), ""))
                || SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(firstNonBlank(asset.getSource(), ""))
                || "OFFICIAL".equalsIgnoreCase(firstNonBlank(asset.getContentSource(), ""));
    }

    private boolean isDistrictAsset(CurriculumAssetSummaryView asset) {
        return OWNER_DISTRICT.equalsIgnoreCase(firstNonBlank(asset.getOwnerScope(), ""))
                || SOURCE_DISTRICT.equalsIgnoreCase(firstNonBlank(asset.getSource(), ""))
                || SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(firstNonBlank(asset.getSource(), ""))
                || "OFFICIAL".equalsIgnoreCase(firstNonBlank(asset.getContentSource(), ""));
    }

    private String badgeForAsset(CurriculumAsset asset) {
        return SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(normalizeSource(asset)) ? "Subject Advisor Uploaded" : isDistrictAsset(asset) ? "District Approved" : "School Uploaded";
    }

    private String badgeForAsset(CurriculumAssetSummaryView asset) {
        return SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(normalizeSource(asset)) ? "Subject Advisor Uploaded" : isDistrictAsset(asset) ? "District Approved" : "School Uploaded";
    }

    private boolean isActiveResource(CurriculumAsset asset) {
        return asset.isActive()
                && !asset.isArchived()
                && !asset.isDeleted()
                && STATUS_ACTIVE.equalsIgnoreCase(firstNonBlank(asset.getStatus(), STATUS_ACTIVE));
    }

    private String normalizeSource(CurriculumAsset asset) {
        if (asset.getSource() != null && !asset.getSource().isBlank()) {
            return asset.getSource().trim().toUpperCase(Locale.ROOT);
        }
        return isDistrictAsset(asset) ? SOURCE_DISTRICT : SOURCE_SCHOOL;
    }

    private String normalizeSource(CurriculumAssetSummaryView asset) {
        if (asset.getSource() != null && !asset.getSource().isBlank()) {
            return asset.getSource().trim().toUpperCase(Locale.ROOT);
        }
        return isDistrictAsset(asset) ? SOURCE_DISTRICT : SOURCE_SCHOOL;
    }

    private String defaultVisibility(CurriculumAsset asset) {
        return isDistrictAsset(asset) ? VISIBILITY_DISTRICT_WIDE : VISIBILITY_SCHOOL_ONLY;
    }

    private String defaultVisibility(CurriculumAssetSummaryView asset) {
        return isDistrictAsset(asset) ? VISIBILITY_DISTRICT_WIDE : VISIBILITY_SCHOOL_ONLY;
    }

    private String scopeLabel(CurriculumAsset asset) {
        if ("LESSON_PLAN".equalsIgnoreCase(asset.getRepositoryType()) && asset.isGeneratedByAi()) {
            return "AI-generated lesson plan";
        }
        if (isDistrictAsset(asset) && asset.getSchoolId() == null) {
            return "District-wide";
        }
        if (isDistrictAsset(asset)) {
            return "School-specific district resource";
        }
        return "School upload";
    }

    private String summaryScopeLabel(CurriculumAssetSummaryView asset) {
        if (isDistrictAsset(asset) && asset.getVisibility() != null && VISIBILITY_DISTRICT_WIDE.equalsIgnoreCase(asset.getVisibility())) {
            return "District-wide";
        }
        return isDistrictAsset(asset) ? "District resource" : "School upload";
    }

    private boolean hasFileReference(String fileName) {
        return fileName != null && !fileName.isBlank();
    }

    private boolean hasContent(byte[] binaryContent, String legacyBase64Content) {
        byte[] content = resolveFileBytes(binaryContent, legacyBase64Content);
        return content != null && content.length > 0;
    }

    private byte[] resolveFileBytes(byte[] binaryContent, String legacyBase64Content) {
        if (binaryContent != null && binaryContent.length > 0) {
            return binaryContent;
        }
        if (legacyBase64Content == null || legacyBase64Content.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(legacyBase64Content);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean gradesOverlap(String left, String right) {
        Set<String> leftTokens = expandGradeTokens(left);
        Set<String> rightTokens = expandGradeTokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }
        for (String token : leftTokens) {
            if (rightTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> expandGradeTokens(String value) {
        String normalized = normalizeGrade(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Map<Integer, Integer> digitPositions = new LinkedHashMap<>();
        StringBuilder currentDigits = new StringBuilder();
        int tokenStart = -1;
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (Character.isDigit(character)) {
                if (currentDigits.isEmpty()) {
                    tokenStart = i;
                }
                currentDigits.append(character);
            } else if (!currentDigits.isEmpty()) {
                digitPositions.put(tokenStart, Integer.parseInt(currentDigits.toString()));
                currentDigits.setLength(0);
                tokenStart = -1;
            }
        }
        if (!currentDigits.isEmpty()) {
            digitPositions.put(tokenStart, Integer.parseInt(currentDigits.toString()));
        }
        if (digitPositions.isEmpty()) {
            return Set.of(normalized);
        }
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(digitPositions.entrySet());
        Set<String> expanded = new LinkedHashSet<>();
        for (Map.Entry<Integer, Integer> entry : entries) {
            expanded.add(String.valueOf(entry.getValue()));
        }
        if (entries.size() >= 2) {
            for (int i = 0; i < entries.size() - 1; i++) {
                Map.Entry<Integer, Integer> current = entries.get(i);
                Map.Entry<Integer, Integer> next = entries.get(i + 1);
                String between = normalized.substring(current.getKey() + String.valueOf(current.getValue()).length(), next.getKey());
                if (between.contains("-") || between.contains("to")) {
                    int start = Math.min(current.getValue(), next.getValue());
                    int end = Math.max(current.getValue(), next.getValue());
                    if (end - start <= 12) {
                        for (int grade = start; grade <= end; grade++) {
                            expanded.add(String.valueOf(grade));
                        }
                    }
                }
            }
        }
        return expanded;
    }

    private void addGrade(Set<String> grades, String value) {
        String normalized = normalizeGrade(value);
        if (!normalized.isBlank()) {
            grades.add(normalized);
        }
    }

    private void addPhase(Set<String> phases, String value) {
        String normalized = normalizeKey(value);
        if (!normalized.isBlank()) {
            phases.add(normalized);
        }
    }

    private String normalizeType(String value) {
        return normalizeKey(value).replace(' ', '_');
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGrade(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace("grade", "").replaceAll("\\s+", "");
    }

    private String uploadedByName(UUID userId) {
        if (userId == null) {
            return "System";
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return "System";
        }
        String fullName = (firstNonBlank(user.get().getFirstName(), "") + " " + firstNonBlank(user.get().getLastName(), "")).trim();
        return fullName.isBlank() ? firstNonBlank(user.get().getEmail(), "System") : fullName;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultContentType(String label) {
        return switch (label.toUpperCase(Locale.ROOT)) {
            case "PDF" -> "application/pdf";
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private School requireSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceConflictException("School not found."));
    }

    private record AssignmentScope(Set<String> subjects, Set<String> grades, Set<String> phases) {
        private boolean empty() {
            return subjects.isEmpty() && grades.isEmpty() && phases.isEmpty();
        }
    }

    private record MatchResult(boolean visible, Boolean assignmentMatched, String reason) {}
}
