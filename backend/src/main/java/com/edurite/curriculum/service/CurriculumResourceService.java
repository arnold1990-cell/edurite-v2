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
import java.time.OffsetDateTime;
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
                .map(this::toAssetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> getDistrictResourcesForTeacher(UUID schoolId, UUID teacherUserId, CurriculumDtos.CurriculumResourceQuery query) {
        School school = requireSchool(schoolId);
        List<CurriculumAsset> visible = findVisibleResourcesForSchool(school);
        Set<String> assignedSubjects = assignedSubjects(schoolId, teacherUserId);
        Set<String> assignedGrades = assignedGrades(schoolId, teacherUserId);
        boolean hasAssignments = !assignedSubjects.isEmpty() || !assignedGrades.isEmpty();
        List<CurriculumAsset> scoped = visible.stream()
                .filter(asset -> isTeacherLessonPlan(asset, teacherUserId) || !hasAssignments || matchesTeacherScope(asset, assignedSubjects, assignedGrades))
                .filter(asset -> matchesQuery(asset, query))
                .toList();
        if (!scoped.isEmpty() || !hasAssignments) {
            return scoped.stream()
                    .map(this::toAssetDto)
                    .toList();
        }
        return visible.stream()
                .filter(asset -> matchesQuery(asset, query))
                .map(this::toAssetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> getActiveDistrictResources(UUID districtId, CurriculumDtos.CurriculumResourceQuery query) {
        return activeDistrictAssets().stream()
                .filter(asset -> districtId == null || Objects.equals(asset.getDistrictId(), districtId))
                .filter(asset -> matchesQuery(asset, query))
                .map(this::toAssetDto)
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
                hasContent(asset.getExcelBytes(), asset.getExcelBase64())
        );
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
                hasFileReference(asset.getExcelFileName())
        );
    }

    public record ResourceFileResponse(
            String fileName,
            String contentType,
            byte[] content,
            boolean inline
    ) {}

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
        List<CurriculumAsset> resources = getDistrictResourcesForTeacher(schoolId, teacherUserId, new CurriculumDtos.CurriculumResourceQuery(null, null, null, null, null, null, null)).stream()
                .map(item -> curriculumAssetRepository.findById(item.id()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return resources.stream()
                .filter(asset -> asset.getId().equals(resourceId))
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
        log.info("Curriculum resource download resourceId={} fileName={} contentType={} fileSize={} status=SUCCESS", asset.getId(), firstNonBlank(fileName, label.toLowerCase(Locale.ROOT) + "-resource"), firstNonBlank(contentType, defaultContentType(label)), content.length);
        return new ResourceFileResponse(
                firstNonBlank(fileName, label.toLowerCase(Locale.ROOT) + "-resource"),
                firstNonBlank(contentType, defaultContentType(label)),
                content,
                inline
        );
    }

    private boolean matchesTeacherScope(CurriculumAsset asset, Set<String> assignedSubjects, Set<String> assignedGrades) {
        boolean subjectMatch = assignedSubjects.isEmpty() || assignedSubjects.contains(normalizeKey(asset.getSubject()));
        boolean gradeMatch = assignedGrades.isEmpty() || assignedGrades.stream().anyMatch(grade -> gradesOverlap(asset.getGrade(), grade));
        return subjectMatch && gradeMatch;
    }

    private boolean isTeacherLessonPlan(CurriculumAsset asset, UUID teacherUserId) {
        return asset != null
                && "LESSON_PLAN".equalsIgnoreCase(asset.getRepositoryType())
                && teacherUserId != null
                && Objects.equals(asset.getUploadedByUserId(), teacherUserId);
    }

    private Set<String> assignedSubjects(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        if (assignments.isEmpty()) {
            return Set.of();
        }
        var subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolSubject::getId, SchoolSubject::getSubjectName, (left, right) -> left));
        Set<String> values = new LinkedHashSet<>();
        for (TeacherAssignment assignment : assignments) {
            String subjectName = subjectById.get(assignment.getSubjectId());
            if (subjectName != null && !subjectName.isBlank()) {
                values.add(normalizeKey(subjectName));
            }
        }
        return values;
    }

    private Set<String> assignedGrades(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        if (assignments.isEmpty()) {
            return Set.of();
        }
        var subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolSubject::getId, subject -> subject, (left, right) -> left));
        Set<String> grades = new LinkedHashSet<>();
        for (TeacherAssignment assignment : assignments) {
            addGrade(grades, assignment.getGrade());
            SchoolSubject subject = subjectById.get(assignment.getSubjectId());
            if (subject != null) {
                addGrade(grades, subject.getGrade());
                addGrade(grades, subject.getGradeRange());
            }
        }
        return grades;
    }

    private void addGrade(Set<String> grades, String value) {
        String normalized = normalizeGrade(value);
        if (!normalized.isBlank()) {
            grades.add(normalized);
        }
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
        String source = normalizeSource(asset);
        if (SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(source)) {
            return "Subject Advisor Uploaded";
        }
        return isDistrictAsset(asset) ? "District Approved" : "School Uploaded";
    }

    private String badgeForAsset(CurriculumAssetSummaryView asset) {
        String source = normalizeSource(asset);
        if (SOURCE_SUBJECT_ADVISOR.equalsIgnoreCase(source)) {
            return "Subject Advisor Uploaded";
        }
        return isDistrictAsset(asset) ? "District Approved" : "School Uploaded";
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
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("grade", "")
                .replaceAll("\\s+", "");
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
}
