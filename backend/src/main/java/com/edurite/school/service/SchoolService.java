package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.compliance.service.ConsentService;
import com.edurite.psychometric.repository.PsychometricSubmissionRepository;
import com.edurite.school.dto.SchoolDtos.LinkStudentRequest;
import com.edurite.school.dto.SchoolDtos.SchoolProfileRequest;
import com.edurite.school.dto.SchoolDtos.SchoolProfileResponse;
import com.edurite.school.dto.SchoolDtos.SchoolStudentResponse;
import com.edurite.school.dto.SchoolDtos.SchoolSummaryResponse;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.School;
import com.edurite.school.entity.SchoolProfile;
import com.edurite.school.entity.SchoolStudent;
import com.edurite.school.repository.SchoolProfileRepository;
import com.edurite.school.repository.SchoolStudentRepository;
import com.edurite.scholarship.repository.ScholarshipApplicationRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentProfileCompletionService;
import com.edurite.tutor.repository.TutorSessionRepository;
import com.edurite.universityapplication.repository.UniversityApplicationRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SchoolService {

    private final SchoolProfileRepository schoolProfileRepository;
    private final SchoolStudentRepository schoolStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileCompletionService profileCompletionService;
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final TutorSessionRepository tutorSessionRepository;
    private final ScholarshipApplicationRepository scholarshipApplicationRepository;
    private final UniversityApplicationRepository universityApplicationRepository;
    private final AssignmentService assignmentService;
    private final UserRepository userRepository;
    private final ConsentService consentService;
    private final SchoolPortalAnalyticsService schoolPortalAnalyticsService;

    public SchoolService(
            SchoolProfileRepository schoolProfileRepository,
            SchoolStudentRepository schoolStudentRepository,
            StudentProfileRepository studentProfileRepository,
            StudentProfileCompletionService profileCompletionService,
            PsychometricSubmissionRepository psychometricSubmissionRepository,
            TutorSessionRepository tutorSessionRepository,
            ScholarshipApplicationRepository scholarshipApplicationRepository,
            UniversityApplicationRepository universityApplicationRepository,
            AssignmentService assignmentService,
            UserRepository userRepository,
            ConsentService consentService,
            SchoolPortalAnalyticsService schoolPortalAnalyticsService
    ) {
        this.schoolProfileRepository = schoolProfileRepository;
        this.schoolStudentRepository = schoolStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.profileCompletionService = profileCompletionService;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.tutorSessionRepository = tutorSessionRepository;
        this.scholarshipApplicationRepository = scholarshipApplicationRepository;
        this.universityApplicationRepository = universityApplicationRepository;
        this.assignmentService = assignmentService;
        this.userRepository = userRepository;
        this.consentService = consentService;
        this.schoolPortalAnalyticsService = schoolPortalAnalyticsService;
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.DashboardResponse dashboard(UUID schoolId, UUID schoolAdminUserId) {
        return assignmentService.dashboard(SchoolAccessService.ROLE_SCHOOL_ADMIN, schoolId, schoolAdminUserId);
    }

    @Transactional
    public School upsertSchool(UUID schoolId, SchoolPortalDtos.SchoolProfileUpsertRequest request) {
        return assignmentService.upsertSchool(schoolId, request);
    }

    @Transactional
    public com.edurite.school.portal.entity.SchoolClass createClass(UUID schoolId, SchoolPortalDtos.SchoolClassRequest request) {
        return assignmentService.createClass(schoolId, request);
    }

    @Transactional
    public com.edurite.school.portal.entity.SchoolSubject createSubject(UUID schoolId, SchoolPortalDtos.SchoolSubjectRequest request) {
        return assignmentService.createSubject(schoolId, request);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.SchoolClass> classes(UUID schoolId) {
        return assignmentService.schoolClasses(schoolId);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.SchoolSubject> subjects(UUID schoolId) {
        return assignmentService.schoolSubjects(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SchoolSubjectView> subjectViews(UUID schoolId, boolean includeInactive) {
        return assignmentService.schoolSubjectViews(schoolId, includeInactive);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubjectCatalogueItem> subjectCatalogue() {
        return assignmentService.subjectCatalogue();
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.SubjectManagementSummary subjectSummary(UUID schoolId) {
        return assignmentService.subjectSummary(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.AtpTopicView> atpTopics(UUID schoolId, String phase, String grade, UUID subjectId, String term) {
        return assignmentService.atpTopics(schoolId, phase, grade, subjectId, term);
    }

    @Transactional
    public com.edurite.school.portal.entity.TeacherAssignment assignTeacher(UUID schoolId, SchoolPortalDtos.TeacherAssignmentRequest request) {
        return assignmentService.assignTeacher(schoolId, request);
    }

    @Transactional
    public List<SchoolPortalDtos.TeacherAssignmentView> replaceTeacherAssignments(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.TeacherAssignmentBulkRequest request) {
        return assignmentService.replaceTeacherAssignments(schoolId, teacherUserId, request);
    }

    @Transactional
    public com.edurite.school.portal.entity.LearnerEnrollment enrollLearner(UUID schoolId, SchoolPortalDtos.LearnerEnrollmentRequest request) {
        return assignmentService.enrollLearner(schoolId, request);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.TeacherAssignment> teacherAssignments(UUID schoolId) {
        return assignmentService.schoolTeacherAssignments(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherAssignmentView> teacherAssignmentViews(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherAssignmentViews(schoolId, teacherUserId);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.LearnerEnrollment> learnerEnrollments(UUID schoolId) {
        return assignmentService.schoolLearnerEnrollments(schoolId);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.SchoolTask> tasks(UUID schoolId) {
        return assignmentService.schoolTasks(schoolId);
    }

    @Transactional
    public com.edurite.school.portal.entity.SchoolTask createTask(UUID schoolId, SchoolPortalDtos.SchoolTaskRequest request) {
        return assignmentService.createTaskForAdmin(schoolId, request);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.SchoolTask> assessments(UUID schoolId) {
        return assignmentService.schoolAssessments(schoolId);
    }

    @Transactional(readOnly = true)
    public List<com.edurite.school.portal.entity.LearningNote> notes(UUID schoolId) {
        return assignmentService.schoolNotes(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> submissions(UUID schoolId) {
        return assignmentService.schoolSubmissions(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> results(UUID schoolId) {
        return assignmentService.schoolResults(schoolId);
    }

    @Transactional
    public com.edurite.school.portal.entity.SchoolClass updateClass(UUID schoolId, UUID classId, SchoolPortalDtos.SchoolClassUpdateRequest request) {
        return assignmentService.updateClass(schoolId, classId, request);
    }

    @Transactional
    public com.edurite.school.portal.entity.SchoolSubject updateSubject(UUID schoolId, UUID subjectId, SchoolPortalDtos.SchoolSubjectUpdateRequest request) {
        return assignmentService.updateSubject(schoolId, subjectId, request);
    }

    @Transactional
    public void deactivateClass(UUID schoolId, UUID classId) {
        assignmentService.deactivateClass(schoolId, classId);
    }

    @Transactional
    public void deactivateSubject(UUID schoolId, UUID subjectId) {
        assignmentService.deactivateSubject(schoolId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SchoolUserAdminView> teachers(UUID schoolId) {
        return assignmentService.schoolUsersByRole(schoolId, SchoolAccessService.ROLE_TEACHER);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SchoolUserAdminView> learners(UUID schoolId) {
        return assignmentService.schoolUsersByRole(schoolId, SchoolAccessService.ROLE_SCHOOL_STUDENT);
    }

    @Transactional
    public SchoolPortalDtos.SchoolUserAdminView createSchoolUser(UUID schoolId, SchoolPortalDtos.SchoolUserCreateRequest request) {
        SchoolPortalDtos.SchoolUserAdminView saved = assignmentService.createSchoolUser(schoolId, request);
        if (SchoolAccessService.ROLE_SCHOOL_STUDENT.equalsIgnoreCase(saved.roleName())) {
            User user = userRepository.findById(saved.userId()).orElseThrow(() -> new ResourceConflictException("Learner account was created but could not be loaded."));
            studentProfileRepository.findByUserId(saved.userId()).orElseGet(() -> {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(saved.userId());
                profile.setFirstName(request.firstName().trim());
                profile.setLastName(request.lastName().trim());
                profile.setSelectedGrade(clean(request.selectedGrade()));
                profile.setQualificationLevel(clean(request.selectedGrade()));
                profile.setCareerGoals(clean(request.careerGoal()));
                return studentProfileRepository.save(profile);
            });
            if (Boolean.TRUE.equals(request.popiaConsentAccepted())) {
                consentService.recordPopiaConsent(user, request.consentVersion());
            }
        }
        return saved;
    }

    @Transactional
    public SchoolPortalDtos.SchoolUserAdminView updateSchoolUser(UUID schoolId, UUID userId, SchoolPortalDtos.SchoolUserUpdateRequest request) {
        return assignmentService.updateSchoolUser(schoolId, userId, request);
    }

    @Transactional
    public void deactivateSchoolUser(UUID schoolId, UUID userId) {
        assignmentService.deactivateSchoolUser(schoolId, userId);
    }

    @Transactional
    public SchoolPortalDtos.BulkLearnerUploadResult bulkUploadLearners(UUID schoolId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResourceConflictException("CSV file is required.");
        }
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<List<String>> rows = parseCsv(reader);
            if (rows.isEmpty()) {
                throw new ResourceConflictException("CSV file is empty.");
            }
            Map<String, Integer> indexes = csvHeaderIndexes(rows.getFirst());
            validateCsvHeaders(indexes);
            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                List<String> cells = rows.get(rowIndex);
                if (cells.stream().allMatch(String::isBlank)) {
                    continue;
                }
                String email = csvValue(cells, indexes, "email");
                if (email.isBlank()) {
                    skipped++;
                    messages.add("Row " + (rowIndex + 1) + ": missing required email.");
                    continue;
                }
                String firstName = csvValue(cells, indexes, "firstname");
                String lastName = csvValue(cells, indexes, "lastname");
                if (firstName.isBlank() || lastName.isBlank()) {
                    skipped++;
                    messages.add("Row " + (rowIndex + 1) + ": first name and last name are required.");
                    continue;
                }
                String teacherEmail = csvValue(cells, indexes, "teacheremail");
                String grade = csvValue(cells, indexes, "grade");
                String className = csvValue(cells, indexes, "classname");
                String selectedGrade = csvValue(cells, indexes, "selectedgrade");
                String careerGoal = csvValue(cells, indexes, "careergoal");
                boolean consentAccepted = Boolean.parseBoolean(csvValue(cells, indexes, "popiaconsentaccepted"));
                SchoolPortalDtos.SchoolUserAdminView learner;
                User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
                if (existing != null) {
                    updated++;
                    learner = assignmentService.updateSchoolUser(schoolId, existing.getId(), new SchoolPortalDtos.SchoolUserUpdateRequest(firstName, lastName, email));
                    studentProfileRepository.findByUserId(existing.getId()).ifPresent(profile -> {
                        profile.setSelectedGrade(clean(selectedGrade.isBlank() ? grade : selectedGrade));
                        profile.setQualificationLevel(clean(selectedGrade.isBlank() ? grade : selectedGrade));
                        profile.setCareerGoals(clean(careerGoal));
                        studentProfileRepository.save(profile);
                    });
                } else {
                    String learnerPassword = csvValue(cells, indexes, "password").isBlank()
                            ? generateTemporaryPassword(firstName, lastName)
                            : csvValue(cells, indexes, "password");
                    learner = createSchoolUser(schoolId, new SchoolPortalDtos.SchoolUserCreateRequest(
                            email,
                            learnerPassword,
                            firstName,
                            lastName,
                            SchoolAccessService.ROLE_SCHOOL_STUDENT,
                            null,
                            null,
                            selectedGrade.isBlank() ? grade : selectedGrade,
                            careerGoal,
                            consentAccepted,
                            csvValue(cells, indexes, "consentversion")
                    ));
                    created++;
                    studentProfileRepository.findByUserId(learner.userId()).ifPresent(profile -> {
                        profile.setSelectedGrade(clean(selectedGrade.isBlank() ? grade : selectedGrade));
                        profile.setQualificationLevel(clean(selectedGrade.isBlank() ? grade : selectedGrade));
                        profile.setCareerGoals(clean(careerGoal));
                        studentProfileRepository.save(profile);
                    });
                }
                if (!grade.isBlank() && !className.isBlank()) {
                    assignmentService.schoolClasses(schoolId).stream()
                            .filter(item -> grade.equalsIgnoreCase(item.getGrade()) && className.equalsIgnoreCase(item.getClassName()))
                            .findFirst()
                            .ifPresent(clazz -> assignmentService.schoolSubjects(schoolId).stream().limit(1).forEach(subject ->
                                    assignmentService.enrollLearner(schoolId, new SchoolPortalDtos.LearnerEnrollmentRequest(learner.userId(), clazz.getId(), subject.getId()))
                            ));
                }
                if (!teacherEmail.isBlank()) {
                    messages.add("Row " + (rowIndex + 1) + ": learner " + email + " linked for follow-up with teacher " + teacherEmail + ".");
                }
            }
        } catch (IOException ex) {
            throw new ResourceConflictException("Could not read learner CSV.");
        }
        return new SchoolPortalDtos.BulkLearnerUploadResult(created, updated, skipped, messages);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ProgressSummaryResponse progress(UUID schoolId, UUID schoolAdminUserId) {
        return assignmentService.teacherProgress(schoolId, schoolAdminUserId);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.DashboardSnapshot portalDashboard(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolPortalAnalyticsService.dashboard(schoolId, viewerUserId, roleName);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.LearnerListResponse portalLearners(UUID schoolId, UUID viewerUserId, String roleName, String search, String grade, String className) {
        return schoolPortalAnalyticsService.learners(schoolId, viewerUserId, roleName, search, grade, className);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.LearnerProfileResponse portalLearnerProfile(UUID schoolId, UUID viewerUserId, String roleName, UUID learnerUserId) {
        return schoolPortalAnalyticsService.learnerProfile(schoolId, viewerUserId, roleName, learnerUserId);
    }


    @Transactional(readOnly = true)
    public SchoolPortalDtos.AcademicInsightsResponse academicInsights(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolPortalAnalyticsService.academicInsights(schoolId, viewerUserId, roleName);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.CareerReadinessResponse careerReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolPortalAnalyticsService.careerReadiness(schoolId, viewerUserId, roleName);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.BursaryReadinessResponse bursaryReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolPortalAnalyticsService.bursaryReadiness(schoolId, viewerUserId, roleName);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.InterventionReportItem> interventions(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolPortalAnalyticsService.interventions(schoolId, viewerUserId, roleName);
    }

    @Transactional
    public SchoolPortalDtos.InterventionReportItem createIntervention(UUID schoolId, UUID actorUserId, String roleName, SchoolPortalDtos.InterventionRequest request) {
        return schoolPortalAnalyticsService.createIntervention(schoolId, actorUserId, roleName, request);
    }

    @Transactional
    public SchoolPortalDtos.InterventionReportItem updateIntervention(UUID schoolId, UUID actorUserId, String roleName, UUID interventionId, SchoolPortalDtos.InterventionProgressRequest request) {
        return schoolPortalAnalyticsService.updateIntervention(schoolId, actorUserId, roleName, interventionId, request);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ReportExportResponse exportReport(UUID schoolId, UUID viewerUserId, String roleName, String reportType, String format) {
        return schoolPortalAnalyticsService.exportReport(schoolId, viewerUserId, roleName, reportType, format);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.PortalSettingsResponse portalSettings(UUID schoolId) {
        return schoolPortalAnalyticsService.settings(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolProfileResponse> listSchools() {
        return schoolProfileRepository.findAll().stream().map(this::toProfileResponse).toList();
    }

    @Transactional
    public SchoolProfileResponse create(SchoolProfileRequest request) {
        SchoolProfile school = new SchoolProfile();
        apply(school, request);
        return toProfileResponse(schoolProfileRepository.save(school));
    }

    @Transactional
    public SchoolProfileResponse update(UUID schoolId, SchoolProfileRequest request) {
        SchoolProfile school = schoolProfileRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceConflictException("School not found"));
        apply(school, request);
        return toProfileResponse(schoolProfileRepository.save(school));
    }

    @Transactional
    public SchoolSummaryResponse linkStudent(UUID schoolId, LinkStudentRequest request) {
        schoolProfileRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found"));
        studentProfileRepository.findById(request.studentId()).orElseThrow(() -> new ResourceConflictException("Student not found"));
        schoolStudentRepository.findBySchoolIdAndStudentId(schoolId, request.studentId()).orElseGet(() -> {
            SchoolStudent link = new SchoolStudent();
            link.setSchoolId(schoolId);
            link.setStudentId(request.studentId());
            return schoolStudentRepository.save(link);
        });
        return summary(schoolId);
    }

    @Transactional(readOnly = true)
    public SchoolSummaryResponse summary(UUID schoolId) {
        schoolProfileRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found"));
        List<SchoolStudent> links = schoolStudentRepository.findBySchoolId(schoolId);
        Map<UUID, StudentProfile> studentsById = studentProfileRepository.findAllById(
                        links.stream().map(SchoolStudent::getStudentId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, Function.identity()));
        List<StudentProfile> students = links.stream()
                .map(SchoolStudent::getStudentId)
                .map(studentsById::get)
                .filter(Objects::nonNull)
                .toList();
        long psychometricCompleted = students.stream().filter(student -> psychometricSubmissionRepository.existsByStudentId(student.getId())).count();
        long completeProfiles = students.stream().filter(profileCompletionService::isProfileCompleted).count();
        long tutorSessions = students.stream().mapToLong(student -> tutorSessionRepository.countByStudentId(student.getId())).sum();
        long trackedApplications = students.stream()
                .mapToLong(student -> scholarshipApplicationRepository.countByStudentId(student.getId())
                        + universityApplicationRepository.countByStudentId(student.getId()))
                .sum();
        return new SchoolSummaryResponse(
                schoolId,
                links.size(),
                psychometricCompleted,
                completeProfiles,
                tutorSessions,
                trackedApplications,
                students.stream().map(this::toStudentResponse).toList()
        );
    }

    private void apply(SchoolProfile school, SchoolProfileRequest request) {
        school.setSchoolName(clean(request.schoolName()));
        school.setCountry(clean(request.country()));
        school.setCity(clean(request.city()));
        school.setContactPerson(clean(request.contactPerson()));
        school.setContactEmail(clean(request.contactEmail()));
        school.setNotes(clean(request.notes()));
    }

    private SchoolProfileResponse toProfileResponse(SchoolProfile school) {
        return new SchoolProfileResponse(
                school.getId(),
                school.getSchoolName(),
                clean(school.getCountry()),
                clean(school.getCity()),
                clean(school.getContactPerson()),
                clean(school.getContactEmail()),
                clean(school.getNotes())
        );
    }

    private SchoolStudentResponse toStudentResponse(StudentProfile student) {
        return new SchoolStudentResponse(
                student.getId(),
                clean(student.getFirstName() + " " + student.getLastName()).trim(),
                clean(student.getQualificationLevel()),
                profileCompletionService.calculateCompleteness(student)
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<String, Integer> csvHeaderIndexes(List<String> header) {
        Map<String, Integer> indexes = new java.util.HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            indexes.put(header.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""), i);
        }
        return indexes;
    }

    private String csvValue(List<String> row, Map<String, Integer> indexes, String key) {
        Integer index = indexes.get(key.toLowerCase(Locale.ROOT));
        if (index == null || index >= row.size()) {
            return "";
        }
        return row.get(index).trim();
    }

    private void validateCsvHeaders(Map<String, Integer> indexes) {
        Set<String> required = Set.of("firstname", "lastname", "email");
        List<String> missing = required.stream().filter(header -> !indexes.containsKey(header)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new ResourceConflictException("CSV is missing required headers: " + String.join(", ", missing));
        }
    }

    private List<List<String>> parseCsv(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        int next;
        while ((next = reader.read()) != -1) {
            char ch = (char) next;
            if (inQuotes) {
                if (ch == '"') {
                    reader.mark(1);
                    int peek = reader.read();
                    if (peek == '"') {
                        currentCell.append('"');
                    } else {
                        inQuotes = false;
                        if (peek != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    currentCell.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
            } else if (ch == '\n') {
                currentRow.add(currentCell.toString());
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                currentCell.setLength(0);
            } else if (ch != '\r') {
                currentCell.append(ch);
            }
        }

        if (inQuotes) {
            throw new ResourceConflictException("CSV contains an unterminated quoted field.");
        }

        if (!currentRow.isEmpty() || currentCell.length() > 0) {
            currentRow.add(currentCell.toString());
            rows.add(currentRow);
        }

        return rows;
    }

    private String generateTemporaryPassword(String firstName, String lastName) {
        String safeFirstName = firstName == null ? "" : firstName.trim();
        String safeLastName = lastName == null ? "" : lastName.trim();
        String firstSegment = safeFirstName.isEmpty() ? "Edu" : safeFirstName.substring(0, Math.min(3, safeFirstName.length()));
        String lastSegment = safeLastName.isEmpty() ? "Rt" : safeLastName.substring(0, Math.min(2, safeLastName.length()));
        String prefix = (firstSegment + lastSegment).replaceAll("[^A-Za-z]", "E");
        return prefix + "@" + UUID.randomUUID().toString().substring(0, 6);
    }
}


