package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.AtpTopic;
import com.edurite.school.portal.entity.LearnerEnrollment;
import com.edurite.school.portal.entity.LearningNote;
import com.edurite.school.portal.entity.PlagiarismReport;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.SubjectCatalogue;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SubmissionFeedback;
import com.edurite.school.portal.entity.TaskSubmission;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.LearningNoteRepository;
import com.edurite.school.portal.repository.PlagiarismReportRepository;
import com.edurite.school.portal.repository.AtpTopicRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SubmissionFeedbackRepository;
import com.edurite.school.portal.repository.SubjectCatalogueRepository;
import com.edurite.school.portal.repository.TaskSubmissionRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final SubjectCatalogueRepository subjectCatalogueRepository;
    private final AtpTopicRepository atpTopicRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final LearningNoteRepository learningNoteRepository;
    private final SchoolTaskRepository schoolTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final SubmissionFeedbackRepository submissionFeedbackRepository;
    private final PlagiarismReportRepository plagiarismReportRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AssignmentService(
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            SubjectCatalogueRepository subjectCatalogueRepository,
            AtpTopicRepository atpTopicRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            LearnerEnrollmentRepository learnerEnrollmentRepository,
            LearningNoteRepository learningNoteRepository,
            SchoolTaskRepository schoolTaskRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            SubmissionFeedbackRepository submissionFeedbackRepository,
            PlagiarismReportRepository plagiarismReportRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService
    ) {
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.subjectCatalogueRepository = subjectCatalogueRepository;
        this.atpTopicRepository = atpTopicRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.learnerEnrollmentRepository = learnerEnrollmentRepository;
        this.learningNoteRepository = learningNoteRepository;
        this.schoolTaskRepository = schoolTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.submissionFeedbackRepository = submissionFeedbackRepository;
        this.plagiarismReportRepository = plagiarismReportRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public School upsertSchool(UUID schoolId, SchoolPortalDtos.SchoolProfileUpsertRequest request) {
        School school = schoolId == null
                ? new School()
                : schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found"));
        school.setSchoolName(request.schoolName().trim());
        school.setRegistrationNumber(trim(request.registrationNumber()));
        school.setDistrict(trim(request.district()));
        school.setProvince(trim(request.province()));
        school.setContactEmail(trim(request.contactEmail()));
        school.setContactPhone(trim(request.contactPhone()));
        school.setAddress(trim(request.address()));
        return schoolRepository.save(school);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.DashboardResponse dashboard(String role, UUID schoolId, UUID userId) {
        long classes = schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId).size();
        long subjects = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).size();
        long notes = role.equals(SchoolAccessService.ROLE_TEACHER)
                ? learningNoteRepository.findBySchoolIdAndTeacherUserId(schoolId, userId).size()
                : learningNoteRepository.findAll().stream().filter(note -> note.getSchoolId().equals(schoolId)).count();
        long tasks = role.equals(SchoolAccessService.ROLE_TEACHER)
                ? schoolTaskRepository.findBySchoolIdAndTeacherUserId(schoolId, userId).size()
                : schoolTaskRepository.findAll().stream().filter(task -> task.getSchoolId().equals(schoolId)).count();
        long submissions = role.equals(SchoolAccessService.ROLE_SCHOOL_STUDENT)
                ? submissionsForLearnerInSchool(schoolId, userId).size()
                : submissionsForSchool(schoolId).size();
        return new SchoolPortalDtos.DashboardResponse(role, schoolId, classes, subjects, tasks, notes, submissions);
    }

    @Transactional
    public SchoolClass createClass(UUID schoolId, SchoolPortalDtos.SchoolClassRequest request) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setGrade(request.grade().trim());
        schoolClass.setClassName(request.className().trim());
        schoolClass.setAcademicYear(request.academicYear());
        schoolClass.setTerm(trim(request.term()));
        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public SchoolSubject createSubject(UUID schoolId, SchoolPortalDtos.SchoolSubjectRequest request) {
        validateSubjectRequest(request.subjectName(), request.phase(), request.grade(), request.gradeRange(), request.languageLevel(), request.subjectType());
        ensureNoDuplicateSchoolSubject(schoolId, null, request.subjectName(), request.phase(), request.grade(), request.gradeRange(), request.languageLevel());
        SubjectCatalogue catalogue = resolveSubjectCatalogue(request.subjectCatalogueId());
        SchoolSubject subject = new SchoolSubject();
        subject.setSchoolId(schoolId);
        subject.setSubjectName(request.subjectName().trim());
        subject.setPhase(request.phase().trim());
        subject.setGrade(trim(request.grade()));
        subject.setGradeRange(trim(request.gradeRange()));
        subject.setLanguageLevel(trim(request.languageLevel()));
        subject.setSubjectType(trim(request.subjectType()));
        subject.setLanguage(Boolean.TRUE.equals(request.isLanguage()) || catalogue != null && catalogue.isLanguage());
        subject.setCompulsory(Boolean.TRUE.equals(request.isCompulsory()) || catalogue != null && catalogue.isCompulsory());
        subject.setHodUserId(resolveTeacherReference(schoolId, request.hodUserId()));
        subject.setCapsAligned(resolveCapsAligned(catalogue, request.capsAligned()));
        subject.setSubjectCatalogue(catalogue);
        if (request.active() != null) {
            subject.setActive(request.active());
        }
        SchoolSubject saved = schoolSubjectRepository.save(subject);
        notifySchoolAdmins(
                schoolId,
                "SUBJECT_ASSIGNED",
                "Subject assigned",
                "A new subject has been added: " + saved.getSubjectName() + "."
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SchoolClass> schoolClasses(UUID schoolId) {
        return schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolSubject> schoolSubjects(UUID schoolId) {
        return schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SchoolSubjectView> schoolSubjectViews(UUID schoolId, boolean includeInactive) {
        List<SchoolSubject> subjects = schoolSubjectRepository.findBySchoolIdOrderByPhaseAscSubjectNameAsc(schoolId);
        Map<UUID, Long> teacherCountBySubject = teacherAssignmentRepository.findAll().stream()
                .filter(assignment -> schoolId.equals(assignment.getSchoolId()) && assignment.isActive())
                .collect(java.util.stream.Collectors.groupingBy(TeacherAssignment::getSubjectId, java.util.stream.Collectors.counting()));
        Map<UUID, Long> learnerCountBySubject = learnerEnrollmentRepository.findAll().stream()
                .filter(enrollment -> schoolId.equals(enrollment.getSchoolId()) && enrollment.isActive())
                .collect(java.util.stream.Collectors.groupingBy(LearnerEnrollment::getSubjectId, java.util.stream.Collectors.counting()));
        return subjects.stream()
                .filter(subject -> includeInactive || subject.isActive())
                .map(subject -> new SchoolPortalDtos.SchoolSubjectView(
                        subject.getId(),
                        subject.getSubjectCatalogue() == null ? null : subject.getSubjectCatalogue().getId(),
                        subject.getSubjectName(),
                        subject.getPhase(),
                        subject.getGrade(),
                        subject.getGradeRange(),
                        subject.getLanguageLevel(),
                        subject.getSubjectType(),
                        subject.isLanguage(),
                        subject.isCompulsory(),
                        subject.getHodUserId(),
                        subject.isCapsAligned(),
                        subject.isActive(),
                        teacherCountBySubject.getOrDefault(subject.getId(), 0L),
                        learnerCountBySubject.getOrDefault(subject.getId(), 0L),
                        hasLinkedAcademicRecords(schoolId, subject.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubjectCatalogueItem> subjectCatalogue() {
        return subjectCatalogueRepository.findByActiveTrueOrderByPhaseAscNameAsc().stream()
                .map(item -> new SchoolPortalDtos.SubjectCatalogueItem(
                        item.getId(),
                        item.getName(),
                        item.getPhase(),
                        item.getGradeRange(),
                        item.getSubjectType(),
                        item.getLanguageLevel(),
                        item.isLanguage(),
                        item.isCompulsory(),
                        item.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.SubjectManagementSummary subjectSummary(UUID schoolId) {
        List<SchoolPortalDtos.SchoolSubjectView> subjects = schoolSubjectViews(schoolId, true);
        long assignedTeachers = teacherAssignmentRepository.findAll().stream()
                .filter(assignment -> schoolId.equals(assignment.getSchoolId()) && assignment.isActive())
                .map(TeacherAssignment::getSubjectId)
                .distinct()
                .count();
        long unassigned = subjects.stream().filter(item -> item.active() && item.assignedTeacherCount() == 0).count();
        List<SchoolPortalDtos.SubjectSummaryMetric> metrics = List.of(
                new SchoolPortalDtos.SubjectSummaryMetric("Total subjects offered", subjects.stream().filter(SchoolPortalDtos.SchoolSubjectView::active).count(), "Active subjects in the school catalogue"),
                new SchoolPortalDtos.SubjectSummaryMetric("Foundation Phase subjects", countByPhase(subjects, "Foundation"), "Grades 1-3"),
                new SchoolPortalDtos.SubjectSummaryMetric("Intermediate Phase subjects", countByPhase(subjects, "Intermediate"), "Grades 4-6"),
                new SchoolPortalDtos.SubjectSummaryMetric("Senior Phase subjects", countByPhase(subjects, "Senior"), "Grades 7-9"),
                new SchoolPortalDtos.SubjectSummaryMetric("FET subjects", countByPhase(subjects, "FET"), "Grades 10-12"),
                new SchoolPortalDtos.SubjectSummaryMetric("Subjects assigned to teachers", assignedTeachers, "Active subjects with at least one teacher assignment"),
                new SchoolPortalDtos.SubjectSummaryMetric("Unassigned subjects", unassigned, "Active subjects without a teacher assignment")
        );
        return new SchoolPortalDtos.SubjectManagementSummary(metrics);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.AtpTopicView> atpTopics(UUID schoolId, String phase, String grade, UUID subjectId, String term) {
        SchoolSubject schoolSubject = subjectId == null
                ? null
                : schoolSubjectRepository.findById(subjectId)
                .filter(subject -> schoolId.equals(subject.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        String requestedPhase = trim(phase);
        String requestedGrade = trim(grade);
        String requestedTerm = trim(term);
        UUID subjectCatalogueId = schoolSubject != null && schoolSubject.getSubjectCatalogue() != null ? schoolSubject.getSubjectCatalogue().getId() : null;
        String subjectName = schoolSubject == null ? null : schoolSubject.getSubjectName();
        return atpTopicRepository.findByActiveTrueOrderByPhaseAscGradeAscAcademicYearAscTermAscWeekNumberAscTopicAsc().stream()
                .filter(item -> requestedPhase == null || requestedPhase.equalsIgnoreCase(item.getPhase()))
                .filter(item -> requestedGrade == null || requestedGrade.equalsIgnoreCase(item.getGrade()))
                .filter(item -> requestedTerm == null || requestedTerm.equalsIgnoreCase(item.getTerm()))
                .filter(item -> {
                    if (subjectCatalogueId != null && item.getSubjectCatalogue() != null) {
                        return subjectCatalogueId.equals(item.getSubjectCatalogue().getId());
                    }
                    return subjectName == null || subjectName.equalsIgnoreCase(item.getSubjectName());
                })
                .map(this::toAtpTopicView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignment> schoolTeacherAssignments(UUID schoolId) {
        return teacherAssignmentRepository.findAll().stream()
                .filter(assignment -> schoolId.equals(assignment.getSchoolId()) && assignment.isActive())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearnerEnrollment> schoolLearnerEnrollments(UUID schoolId) {
        return learnerEnrollmentRepository.findAll().stream()
                .filter(enrollment -> schoolId.equals(enrollment.getSchoolId()) && enrollment.isActive())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolTask> schoolTasks(UUID schoolId) {
        return schoolTaskRepository.findAll().stream()
                .filter(task -> schoolId.equals(task.getSchoolId()))
                .sorted(Comparator.comparing(SchoolTask::getDueAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolTask> schoolAssessments(UUID schoolId) {
        return schoolTasks(schoolId).stream()
                .filter(task -> Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT", "CAT").contains(task.getTaskType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningNote> schoolNotes(UUID schoolId) {
        return learningNoteRepository.findAll().stream()
                .filter(note -> schoolId.equals(note.getSchoolId()))
                .sorted(Comparator.comparing(LearningNote::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> schoolSubmissions(UUID schoolId) {
        List<TaskSubmission> submissions = taskSubmissionRepository.findAll().stream()
                .filter(submission -> {
                    SchoolTask task = schoolTaskRepository.findById(submission.getTaskId()).orElse(null);
                    return task != null && schoolId.equals(task.getSchoolId());
                })
                .toList();
        return toSubmissionViews(submissions, true);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> schoolResults(UUID schoolId) {
        return schoolSubmissions(schoolId).stream()
                .filter(submission -> submission.marks() != null && submission.released())
                .toList();
    }

    @Transactional
    public SchoolClass updateClass(UUID schoolId, UUID classId, SchoolPortalDtos.SchoolClassUpdateRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Class not found"));
        schoolClass.setGrade(request.grade().trim());
        schoolClass.setClassName(request.className().trim());
        schoolClass.setAcademicYear(request.academicYear());
        schoolClass.setTerm(trim(request.term()));
        schoolClass.setActive(request.active());
        return schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public SchoolSubject updateSubject(UUID schoolId, UUID subjectId, SchoolPortalDtos.SchoolSubjectUpdateRequest request) {
        validateSubjectRequest(request.subjectName(), request.phase(), request.grade(), request.gradeRange(), request.languageLevel(), request.subjectType());
        SchoolSubject subject = schoolSubjectRepository.findById(subjectId)
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        ensureNoDuplicateSchoolSubject(schoolId, subjectId, request.subjectName(), request.phase(), request.grade(), request.gradeRange(), request.languageLevel());
        SubjectCatalogue catalogue = resolveSubjectCatalogue(request.subjectCatalogueId());
        subject.setSubjectName(request.subjectName().trim());
        subject.setPhase(request.phase().trim());
        subject.setGrade(trim(request.grade()));
        subject.setGradeRange(trim(request.gradeRange()));
        subject.setLanguageLevel(trim(request.languageLevel()));
        subject.setSubjectType(trim(request.subjectType()));
        subject.setLanguage(Boolean.TRUE.equals(request.isLanguage()) || catalogue != null && catalogue.isLanguage());
        subject.setCompulsory(Boolean.TRUE.equals(request.isCompulsory()) || catalogue != null && catalogue.isCompulsory());
        subject.setHodUserId(resolveTeacherReference(schoolId, request.hodUserId()));
        subject.setCapsAligned(resolveCapsAligned(catalogue, request.capsAligned()));
        subject.setSubjectCatalogue(catalogue);
        subject.setActive(request.active());
        return schoolSubjectRepository.save(subject);
    }

    @Transactional
    public void deactivateClass(UUID schoolId, UUID classId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Class not found"));
        schoolClass.setActive(false);
        schoolClassRepository.save(schoolClass);
    }

    @Transactional
    public void deactivateSubject(UUID schoolId, UUID subjectId) {
        SchoolSubject subject = schoolSubjectRepository.findById(subjectId)
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        subject.setActive(false);
        schoolSubjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SchoolUserAdminView> schoolUsersByRole(UUID schoolId, String roleName) {
        return schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, roleName).stream()
                .map(profile -> userRepository.findById(profile.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .map(user -> new SchoolPortalDtos.SchoolUserAdminView(
                        user.getId(),
                        (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim(),
                        user.getEmail(),
                        roleName,
                        user.getStatus().name(),
                        user.getStatus() == UserStatus.ACTIVE,
                        trim(user.getPhoneNumber())
                ))
                .toList();
    }

    @Transactional
    public SchoolPortalDtos.SchoolUserAdminView createSchoolUser(UUID schoolId, SchoolPortalDtos.SchoolUserCreateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("Email already exists");
        }
        Role role = roleRepository.findByName(request.roleName().trim())
                .or(() -> roleRepository.findByName("ROLE_" + request.roleName().trim().toUpperCase(Locale.ROOT)))
                .orElseThrow(() -> new ResourceConflictException("Role not found"));
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhoneNumber(trim(request.phoneNumber()));
        user.setStatus(resolveUserStatus(request.status()));
        user.setEmailVerified(true);
        user.getRoles().add(role);
        User saved = userRepository.save(user);

        com.edurite.school.portal.entity.SchoolUserProfile profile = new com.edurite.school.portal.entity.SchoolUserProfile();
        profile.setSchoolId(schoolId);
        profile.setUserId(saved.getId());
        profile.setRoleName(role.getName());
        profile.setActive(true);
        profile.setDeleted(false);
        schoolUserProfileRepository.save(profile);
        if ("ROLE_SCHOOL_STUDENT".equals(role.getName())) {
            notifySchoolAdmins(
                    schoolId,
                    "NEW_LEARNER_ADDED",
                    "New learner added",
                    "Learner " + ((safe(saved.getFirstName()) + " " + safe(saved.getLastName())).trim()) + " has been added."
            );
        }

        return new SchoolPortalDtos.SchoolUserAdminView(
                saved.getId(),
                (safe(saved.getFirstName()) + " " + safe(saved.getLastName())).trim(),
                saved.getEmail(),
                role.getName(),
                saved.getStatus().name(),
                saved.getStatus() == UserStatus.ACTIVE,
                trim(saved.getPhoneNumber())
        );
    }

    @Transactional
    public SchoolPortalDtos.SchoolUserAdminView updateSchoolUser(UUID schoolId, UUID userId, SchoolPortalDtos.SchoolUserUpdateRequest request) {
        schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, userId)
                .orElseThrow(() -> new ResourceConflictException("School user not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceConflictException("User not found"));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        User saved = userRepository.save(user);
        String role = saved.getRoles().stream().findFirst().map(Role::getName).orElse("ROLE_USER");
        return new SchoolPortalDtos.SchoolUserAdminView(
                saved.getId(),
                (safe(saved.getFirstName()) + " " + safe(saved.getLastName())).trim(),
                saved.getEmail(),
                role,
                saved.getStatus().name(),
                saved.getStatus() == UserStatus.ACTIVE,
                trim(saved.getPhoneNumber())
        );
    }

    @Transactional
    public void deactivateSchoolUser(UUID schoolId, UUID userId) {
        com.edurite.school.portal.entity.SchoolUserProfile profile = schoolUserProfileRepository
                .findBySchoolIdAndUserIdAndDeletedFalse(schoolId, userId)
                .orElseThrow(() -> new ResourceConflictException("School user not found"));
        profile.setActive(false);
        profile.setDeleted(true);
        schoolUserProfileRepository.save(profile);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceConflictException("User not found"));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Transactional
    public TeacherAssignment assignTeacher(UUID schoolId, SchoolPortalDtos.TeacherAssignmentRequest request) {
        schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, request.teacherUserId())
                .filter(profile -> SchoolAccessService.ROLE_TEACHER.equals(profile.getRoleName()) && profile.isActive())
                .orElseThrow(() -> new ResourceConflictException("Teacher not found in this school"));
        schoolClassRepository.findById(request.classId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()) && existing.isActive())
                .orElseThrow(() -> new ResourceConflictException("Class not found"));
        SchoolSubject subject = schoolSubjectRepository.findById(request.subjectId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, request.teacherUserId(), request.classId(), request.subjectId())
                .ifPresent(existing -> {
                    throw new ResourceConflictException("This teacher is already assigned to the selected subject and class.");
                });
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacherUserId(request.teacherUserId());
        assignment.setClassId(request.classId());
        assignment.setSubjectId(request.subjectId());
        assignment.setPhase(trim(request.phase()) == null ? subject.getPhase() : request.phase().trim());
        assignment.setGrade(trim(request.grade()));
        assignment.setClassTeacher(Boolean.TRUE.equals(request.isClassTeacher()));
        TeacherAssignment saved = teacherAssignmentRepository.save(assignment);
        String subjectName = subject.getSubjectName();
        String className = schoolClassRepository.findById(saved.getClassId()).map(c -> c.getGrade() + " " + c.getClassName()).orElse("class");
        notificationService.createInApp(
                saved.getTeacherUserId(),
                "TEACHER_ASSIGNED",
                "Teacher assigned",
                "You have been assigned to " + subjectName + " for " + className + "."
        );
        notifySchoolAdmins(
                schoolId,
                "TEACHER_ASSIGNED",
                "Teacher assigned",
                "A teacher has been assigned to " + subjectName + " for " + className + "."
        );
        return saved;
    }

    @Transactional
    public List<SchoolPortalDtos.TeacherAssignmentView> replaceTeacherAssignments(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.TeacherAssignmentBulkRequest request) {
        schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, teacherUserId)
                .filter(profile -> SchoolAccessService.ROLE_TEACHER.equals(profile.getRoleName()) && profile.isActive())
                .orElseThrow(() -> new ResourceConflictException("Teacher not found in this school"));
        List<TeacherAssignment> existing = teacherAssignmentRepository.findBySchoolIdAndTeacherUserId(schoolId, teacherUserId);
        for (TeacherAssignment item : existing) {
            item.setActive(false);
        }
        teacherAssignmentRepository.saveAll(existing);
        for (SchoolPortalDtos.TeacherAssignmentRequest item : request.assignments()) {
            if (!teacherUserId.equals(item.teacherUserId())) {
                throw new ResourceConflictException("Assignment payload teacher mismatch.");
            }
            assignTeacher(schoolId, item);
        }
        return teacherAssignmentViews(schoolId, teacherUserId);
    }

    @Transactional
    public LearnerEnrollment enrollLearner(UUID schoolId, SchoolPortalDtos.LearnerEnrollmentRequest request) {
        schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, request.learnerUserId())
                .filter(profile -> SchoolAccessService.ROLE_SCHOOL_STUDENT.equals(profile.getRoleName()) && profile.isActive())
                .orElseThrow(() -> new ResourceConflictException("Learner not found in this school"));
        schoolClassRepository.findById(request.classId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()) && existing.isActive())
                .orElseThrow(() -> new ResourceConflictException("Class not found"));
        schoolSubjectRepository.findById(request.subjectId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()) && existing.isActive())
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        LearnerEnrollment existing = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndClassIdAndSubjectIdAndActiveTrue(
                schoolId,
                request.learnerUserId(),
                request.classId(),
                request.subjectId()
        ).orElse(null);
        if (existing != null) {
            return existing;
        }
        LearnerEnrollment enrollment = new LearnerEnrollment();
        enrollment.setSchoolId(schoolId);
        enrollment.setLearnerUserId(request.learnerUserId());
        enrollment.setClassId(request.classId());
        enrollment.setSubjectId(request.subjectId());
        return learnerEnrollmentRepository.save(enrollment);
    }

    @Transactional
    public LearningNote createNote(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.LearningNoteRequest request) {
        ensureTeacherAssignment(schoolId, teacherUserId, request.classId(), request.subjectId());
        LearningNote note = new LearningNote();
        note.setSchoolId(schoolId);
        note.setTeacherUserId(teacherUserId);
        note.setClassId(request.classId());
        note.setSubjectId(request.subjectId());
        note.setTitle(request.title().trim());
        note.setNoteText(trim(request.noteText()));
        note.setPdfUrl(trim(request.pdfUrl()));
        return learningNoteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<LearningNote> studentNotes(UUID schoolId, UUID learnerUserId) {
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, learnerUserId);
        List<LearningNote> notes = new ArrayList<>();
        for (LearnerEnrollment enrollment : enrollments) {
            notes.addAll(learningNoteRepository.findBySchoolIdAndClassIdAndSubjectIdAndPublishedTrue(
                    schoolId,
                    enrollment.getClassId(),
                    enrollment.getSubjectId()
            ));
        }
        return notes.stream().distinct().sorted(Comparator.comparing(LearningNote::getCreatedAt).reversed()).toList();
    }

    @Transactional
    public SchoolTask createTask(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.SchoolTaskRequest request) {
        ensureTeacherAssignment(schoolId, teacherUserId, request.classId(), request.subjectId());
        return saveTask(schoolId, teacherUserId, request);
    }

    @Transactional
    public SchoolTask createTaskForAdmin(UUID schoolId, SchoolPortalDtos.SchoolTaskRequest request) {
        UUID teacherUserId = resolveTaskTeacherUserId(schoolId, request);
        return saveTask(schoolId, teacherUserId, request);
    }

    private SchoolTask saveTask(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.SchoolTaskRequest request) {
        SchoolSubject subject = schoolSubjectRepository.findById(request.subjectId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()))
                .orElseThrow(() -> new ResourceConflictException("Subject not found"));
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .filter(existing -> schoolId.equals(existing.getSchoolId()) && existing.isActive())
                .orElseThrow(() -> new ResourceConflictException("Class not found"));
        AtpTopic atpTopic = resolveAtpTopic(request.atpTopicId());
        SchoolTask task = new SchoolTask();
        task.setSchoolId(schoolId);
        task.setTeacherUserId(teacherUserId);
        task.setClassId(request.classId());
        task.setSubjectId(request.subjectId());
        task.setAtpTopicId(atpTopic == null ? null : atpTopic.getId());
        task.setTaskType(request.taskType().trim().toUpperCase(Locale.ROOT));
        task.setTitle(request.title().trim());
        task.setAcademicYear(request.academicYear() == null ? schoolClass.getAcademicYear() : request.academicYear());
        task.setPhase(trim(request.phase()) == null ? subject.getPhase() : request.phase().trim());
        task.setGrade(trim(request.grade()) == null ? schoolClass.getGrade() : request.grade().trim());
        task.setInstructions(trim(request.instructions()));
        task.setAssessmentType(trim(request.assessmentType()));
        task.setWeekNumber(request.weekNumber() == null && atpTopic != null ? atpTopic.getWeekNumber() : request.weekNumber());
        task.setDueAt(request.dueAt());
        task.setTerm(trim(request.term()) == null && atpTopic != null ? atpTopic.getTerm() : trim(request.term()));
        task.setMaxMarks(request.maxMarks());
        task.setRubric(trim(request.rubric()));
        task.setResourcesMaterials(trim(request.resources()));
        task.setCognitiveLevel(trim(request.cognitiveLevel()));
        task.setAssessmentCategory(trim(request.assessmentCategory()));
        task.setReleased(true);
        SchoolTask saved = schoolTaskRepository.save(task);
        String eventType = Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT", "CAT").contains(saved.getTaskType())
                ? "ASSESSMENT_CREATED"
                : "ASSIGNMENT_CREATED";
        String title = Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT", "CAT").contains(saved.getTaskType())
                ? "Assessment created"
                : "Assignment created";
        String dueDate = saved.getDueAt() == null ? "" : " Due: " + saved.getDueAt().toLocalDate() + ".";
        notifySchoolAdmins(schoolId, eventType, title, saved.getTitle() + " has been created." + dueDate);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SchoolTask> teacherTasks(UUID schoolId, UUID teacherUserId) {
        return schoolTaskRepository.findBySchoolIdAndTeacherUserId(schoolId, teacherUserId);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.StudentTaskView> studentTasks(UUID schoolId, UUID learnerUserId) {
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, learnerUserId);
        List<SchoolTask> tasks = new ArrayList<>();
        for (LearnerEnrollment enrollment : enrollments) {
            tasks.addAll(schoolTaskRepository.findBySchoolIdAndClassIdAndSubjectId(schoolId, enrollment.getClassId(), enrollment.getSubjectId()));
        }
        return tasks.stream()
                .distinct()
                .sorted(Comparator.comparing(SchoolTask::getDueAt))
                .map(task -> new SchoolPortalDtos.StudentTaskView(task.getId(), task.getTaskType(), task.getTitle(), task.getInstructions(), task.getDueAt(), task.getMaxMarks(), task.getTerm()))
                .toList();
    }

    @Transactional
    public TaskSubmission submitTask(UUID schoolId, UUID learnerUserId, SchoolPortalDtos.TaskSubmissionRequest request) {
        SchoolTask task = schoolTaskRepository.findById(request.taskId()).orElseThrow(() -> new ResourceConflictException("Task not found"));
        if (!task.getSchoolId().equals(schoolId)) {
            throw new ResourceConflictException("Task does not belong to your school");
        }
        learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndClassIdAndSubjectIdAndActiveTrue(
                schoolId,
                learnerUserId,
                task.getClassId(),
                task.getSubjectId()
        ).orElseThrow(() -> new ResourceConflictException("Learner is not enrolled for this class/subject"));

        TaskSubmission submission = taskSubmissionRepository.findByTaskIdAndLearnerUserId(task.getId(), learnerUserId)
                .orElseGet(TaskSubmission::new);
        submission.setTaskId(task.getId());
        submission.setLearnerUserId(learnerUserId);
        submission.setSubmissionText(trim(request.submissionText()));
        submission.setFileUrl(trim(request.fileUrl()));
        submission.setLate(OffsetDateTime.now().isAfter(task.getDueAt()));
        submission.setStatus("SUBMITTED");
        TaskSubmission saved = taskSubmissionRepository.save(submission);
        runPlagiarismCheck(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> taskSubmissionsForTeacher(UUID schoolId, UUID teacherUserId, UUID taskId) {
        SchoolTask task = schoolTaskRepository.findById(taskId).orElseThrow(() -> new ResourceConflictException("Task not found"));
        ensureTeacherAssignment(schoolId, teacherUserId, task.getClassId(), task.getSubjectId());
        return toSubmissionViews(taskSubmissionRepository.findByTaskId(taskId), true);
    }

    @Transactional
    public SubmissionFeedback markSubmission(UUID schoolId, UUID teacherUserId, UUID submissionId, SchoolPortalDtos.MarkSubmissionRequest request) {
        TaskSubmission submission = taskSubmissionRepository.findById(submissionId).orElseThrow(() -> new ResourceConflictException("Submission not found"));
        SchoolTask task = schoolTaskRepository.findById(submission.getTaskId()).orElseThrow(() -> new ResourceConflictException("Task not found"));
        ensureTeacherAssignment(schoolId, teacherUserId, task.getClassId(), task.getSubjectId());

        SubmissionFeedback feedback = submissionFeedbackRepository.findBySubmissionId(submissionId).orElseGet(SubmissionFeedback::new);
        feedback.setSubmissionId(submissionId);
        feedback.setTeacherUserId(teacherUserId);
        feedback.setMarksAwarded(request.marksAwarded());
        feedback.setComments(trim(request.comments()));
        feedback.setRubricScoring(trim(request.rubricScoring()));
        feedback.setReleased(request.released());

        submission.setStatus("MARKED");
        taskSubmissionRepository.save(submission);
        SubmissionFeedback saved = submissionFeedbackRepository.save(feedback);
        if (saved.isReleased()) {
            notificationService.createInApp(
                    submission.getLearnerUserId(),
                    "RESULTS_UPLOADED",
                    "Results uploaded",
                    "Your results for " + task.getTitle() + " are now available."
            );
            notifySchoolAdmins(
                    schoolId,
                    "RESULTS_UPLOADED",
                    "Results uploaded",
                    "Results have been released for " + task.getTitle() + "."
            );
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> studentSubmissionViews(UUID schoolId, UUID learnerUserId) {
        return toSubmissionViews(submissionsForLearnerInSchool(schoolId, learnerUserId), false);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.SubmissionView> studentMarks(UUID schoolId, UUID learnerUserId) {
        return studentSubmissionViews(schoolId, learnerUserId).stream()
                .filter(submission -> submission.released() && submission.marks() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ProgressSummaryResponse teacherProgress(UUID schoolId, UUID teacherUserId) {
        List<SchoolTask> tasks = schoolTaskRepository.findBySchoolIdAndTeacherUserId(schoolId, teacherUserId);
        long totalTasks = tasks.size();
        long submitted = 0;
        long late = 0;
        long missing = 0;
        for (SchoolTask task : tasks) {
            List<LearnerEnrollment> expected = learnerEnrollmentRepository.findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, task.getClassId(), task.getSubjectId());
            List<TaskSubmission> taskSubs = taskSubmissionRepository.findByTaskId(task.getId());
            submitted += taskSubs.size();
            late += taskSubs.stream().filter(TaskSubmission::isLate).count();
            long diff = expected.size() - taskSubs.size();
            if (diff > 0) {
                missing += diff;
            }
        }
        return new SchoolPortalDtos.ProgressSummaryResponse(totalTasks, submitted, missing, late);
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.ProgressSummaryResponse studentProgress(UUID schoolId, UUID learnerUserId) {
        List<SchoolPortalDtos.StudentTaskView> tasks = studentTasks(schoolId, learnerUserId);
        List<TaskSubmission> submissions = submissionsForLearnerInSchool(schoolId, learnerUserId);
        Set<UUID> submittedTaskIds = submissions.stream().map(TaskSubmission::getTaskId).collect(java.util.stream.Collectors.toSet());
        long late = submissions.stream().filter(TaskSubmission::isLate).count();
        long submitted = tasks.stream().filter(task -> submittedTaskIds.contains(task.taskId())).count();
        long missing = Math.max(0, tasks.size() - submitted);
        return new SchoolPortalDtos.ProgressSummaryResponse(tasks.size(), submitted, missing, late);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherClassView> teacherClasses(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findAll().stream()
                .filter(enrollment -> schoolId.equals(enrollment.getSchoolId()) && enrollment.isActive())
                .toList();
        Map<UUID, Long> learnerCountByClass = new HashMap<>();
        for (LearnerEnrollment enrollment : enrollments) {
            learnerCountByClass.merge(enrollment.getClassId(), 1L, Long::sum);
        }
        return assignments.stream().map(assignment -> {
            SchoolClass schoolClass = schoolClassRepository.findById(assignment.getClassId()).orElse(null);
            SchoolSubject subject = schoolSubjectRepository.findById(assignment.getSubjectId()).orElse(null);
            if (schoolClass == null || subject == null) {
                return null;
            }
            return new SchoolPortalDtos.TeacherClassView(
                    schoolClass.getId(),
                    schoolClass.getGrade(),
                    schoolClass.getClassName(),
                    schoolClass.getAcademicYear(),
                    subject.getSubjectName(),
                    learnerCountByClass.getOrDefault(schoolClass.getId(), 0L)
            );
        }).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherSubjectView> teacherSubjects(UUID schoolId, UUID teacherUserId) {
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        Map<UUID, Long> classCountBySubject = new HashMap<>();
        for (TeacherAssignment assignment : assignments) {
            classCountBySubject.merge(assignment.getSubjectId(), 1L, Long::sum);
        }
        return classCountBySubject.entrySet().stream().map(entry -> {
            SchoolSubject subject = schoolSubjectRepository.findById(entry.getKey()).orElse(null);
            if (subject == null) {
                return null;
            }
            return new SchoolPortalDtos.TeacherSubjectView(
                    subject.getId(),
                    subject.getSubjectName(),
                    subject.getPhase(),
                    subject.getGrade(),
                    entry.getValue()
            );
        }).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherAssignmentView> teacherAssignmentViews(UUID schoolId, UUID teacherUserId) {
        return teacherAssignmentRepository.findBySchoolIdAndTeacherUserId(schoolId, teacherUserId).stream()
                .map(item -> new SchoolPortalDtos.TeacherAssignmentView(
                        item.getId(),
                        item.getTeacherUserId(),
                        item.getClassId(),
                        item.getSubjectId(),
                        item.getPhase(),
                        item.getGrade(),
                        item.isClassTeacher(),
                        item.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolTask> teacherAssessments(UUID schoolId, UUID teacherUserId) {
        return teacherTasks(schoolId, teacherUserId).stream()
                .filter(task -> Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT").contains(task.getTaskType()))
                .sorted(Comparator.comparing(SchoolTask::getDueAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.LearnerSubjectView> studentSubjects(UUID schoolId, UUID learnerUserId) {
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, learnerUserId);
        List<SchoolPortalDtos.StudentTaskView> allTasks = studentTasks(schoolId, learnerUserId);
        Map<UUID, List<SchoolPortalDtos.StudentTaskView>> tasksBySubject = new HashMap<>();
        Map<UUID, List<LearningNote>> notesBySubject = new HashMap<>();

        for (LearnerEnrollment enrollment : enrollments) {
            List<SchoolPortalDtos.StudentTaskView> subjectTasks = allTasks.stream()
                    .filter(task -> {
                        SchoolTask original = schoolTaskRepository.findById(task.taskId()).orElse(null);
                        return original != null && original.getSubjectId().equals(enrollment.getSubjectId());
                    })
                    .toList();
            tasksBySubject.put(enrollment.getSubjectId(), subjectTasks);
            notesBySubject.put(
                    enrollment.getSubjectId(),
                    learningNoteRepository.findBySchoolIdAndClassIdAndSubjectIdAndPublishedTrue(schoolId, enrollment.getClassId(), enrollment.getSubjectId())
            );
        }

        Set<UUID> submittedTaskIds = submissionsForLearnerInSchool(schoolId, learnerUserId).stream()
                .map(TaskSubmission::getTaskId)
                .collect(java.util.stream.Collectors.toSet());

        return enrollments.stream()
                .map(enrollment -> {
                    SchoolSubject subject = schoolSubjectRepository.findById(enrollment.getSubjectId()).orElse(null);
                    if (subject == null) return null;
                    TeacherAssignment teacherAssignment = teacherAssignmentRepository
                            .findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, enrollment.getClassId(), enrollment.getSubjectId())
                            .stream()
                            .findFirst()
                            .orElse(null);
                    String teacherName = "Teacher";
                    if (teacherAssignment != null) {
                        teacherName = userRepository.findById(teacherAssignment.getTeacherUserId())
                                .map(user -> (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                                .filter(name -> !name.isBlank())
                                .orElse("Teacher");
                    }

                    List<SchoolPortalDtos.StudentTaskView> subjectTasks = tasksBySubject.getOrDefault(subject.getId(), List.of());
                    long subjectSubmitted = subjectTasks.stream().filter(task -> submittedTaskIds.contains(task.taskId())).count();
                    int progress = subjectTasks.isEmpty() ? 0 : (int) Math.round((subjectSubmitted * 100.0) / subjectTasks.size());
                    String latestTaskTitle = subjectTasks.stream()
                            .sorted(Comparator.comparing(SchoolPortalDtos.StudentTaskView::dueAt).reversed())
                            .map(SchoolPortalDtos.StudentTaskView::title)
                            .findFirst()
                            .orElse(null);
                    String latestNoteTitle = notesBySubject.getOrDefault(subject.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(LearningNote::getCreatedAt).reversed())
                            .map(LearningNote::getTitle)
                            .findFirst()
                            .orElse(null);

                    return new SchoolPortalDtos.LearnerSubjectView(
                            subject.getId(),
                            subject.getSubjectName(),
                            subject.getPhase(),
                            subject.getGrade(),
                            teacherName,
                            progress,
                            latestTaskTitle,
                            latestNoteTitle
                    );
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.StudentTaskView> studentAssessments(UUID schoolId, UUID learnerUserId) {
        return studentTasks(schoolId, learnerUserId).stream()
                .filter(task -> Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT").contains(task.taskType()))
                .sorted(Comparator.comparing(SchoolPortalDtos.StudentTaskView::dueAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolPortalDtos.TeacherAnalyticsResponse teacherAnalytics(UUID schoolId, UUID teacherUserId) {
        List<SchoolTask> tasks = teacherTasks(schoolId, teacherUserId);
        List<TaskSubmission> submissions = tasks.stream().flatMap(task -> taskSubmissionRepository.findByTaskId(task.getId()).stream()).toList();
        long pendingMarking = submissions.stream().filter(submission -> submissionFeedbackRepository.findBySubmissionId(submission.getId()).isEmpty()).count();
        long sbaTasksDue = tasks.stream().filter(task -> "SBA".equals(task.getTaskType()) && task.getDueAt().isAfter(OffsetDateTime.now())).count();
        long learnerSubmissions = submissions.size();
        BigDecimal avg = submissions.isEmpty()
                ? BigDecimal.ZERO
                : submissions.stream()
                .map(submission -> submissionFeedbackRepository.findBySubmissionId(submission.getId()).map(SubmissionFeedback::getMarksAwarded).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(submissions.size()), 2, RoundingMode.HALF_UP);
        BigDecimal attendance = submissions.isEmpty()
                ? new BigDecimal("100.00")
                : BigDecimal.valueOf(100L - Math.round((submissions.stream().filter(TaskSubmission::isLate).count() * 100.0) / submissions.size()));
        long upcomingAssessments = tasks.stream().filter(task -> task.getDueAt().isAfter(OffsetDateTime.now())).count();
        return new SchoolPortalDtos.TeacherAnalyticsResponse(pendingMarking, sbaTasksDue, learnerSubmissions, avg, attendance, upcomingAssessments);
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherActivityItem> teacherActivity(UUID schoolId, UUID teacherUserId) {
        List<SchoolPortalDtos.TeacherActivityItem> items = new ArrayList<>();
        for (SchoolTask task : teacherTasks(schoolId, teacherUserId)) {
            items.add(new SchoolPortalDtos.TeacherActivityItem("TASK", "Task created", task.getTitle(), task.getCreatedAt(), "NORMAL"));
            List<TaskSubmission> taskSubmissions = taskSubmissionRepository.findByTaskId(task.getId());
            for (TaskSubmission submission : taskSubmissions) {
                items.add(new SchoolPortalDtos.TeacherActivityItem(
                        "SUBMISSION",
                        "New submission",
                        userRepository.findById(submission.getLearnerUserId()).map(user -> (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim()).orElse("Learner"),
                        submission.getCreatedAt(),
                        submission.isLate() ? "MEDIUM" : "NORMAL"
                ));
            }
        }
        return items.stream().sorted(Comparator.comparing(SchoolPortalDtos.TeacherActivityItem::occurredAt).reversed()).limit(20).toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolPortalDtos.TeacherCalendarItem> teacherCalendar(UUID schoolId, UUID teacherUserId) {
        List<SchoolPortalDtos.TeacherCalendarItem> items = new ArrayList<>();
        for (SchoolTask task : teacherTasks(schoolId, teacherUserId)) {
            String category = Set.of("EXAM", "TEST", "QUIZ", "ASSESSMENT").contains(task.getTaskType()) ? "ASSESSMENT" : "TASK";
            items.add(new SchoolPortalDtos.TeacherCalendarItem(task.getTitle(), category, task.getDueAt()));
        }
        return items.stream().sorted(Comparator.comparing(SchoolPortalDtos.TeacherCalendarItem::dueAt)).limit(20).toList();
    }

    private List<SchoolPortalDtos.SubmissionView> toSubmissionViews(List<TaskSubmission> submissions, boolean includeAllFeedback) {
        return submissions.stream().map(submission -> {
            SubmissionFeedback feedback = submissionFeedbackRepository.findBySubmissionId(submission.getId()).orElse(null);
            List<PlagiarismReport> reports = plagiarismReportRepository.findBySubmissionId(submission.getId());
            BigDecimal similarity = reports.stream().map(PlagiarismReport::getSimilarityPercentage).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            boolean flagged = reports.stream().anyMatch(PlagiarismReport::isFlagged);
            String learnerName = userRepository.findById(submission.getLearnerUserId())
                    .map(user -> (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                    .orElse("Learner");
            boolean released = feedback != null && feedback.isReleased();
            return new SchoolPortalDtos.SubmissionView(
                    submission.getId(),
                    submission.getTaskId(),
                    submission.getLearnerUserId(),
                    learnerName,
                    submission.getSubmissionText(),
                    submission.getFileUrl(),
                    submission.isLate(),
                    submission.getStatus(),
                    similarity,
                    flagged,
                    feedback == null ? null : (includeAllFeedback || released ? feedback.getMarksAwarded() : null),
                    feedback == null ? null : (includeAllFeedback || released ? feedback.getComments() : null),
                    released
            );
        }).toList();
    }

    private List<TaskSubmission> submissionsForLearnerInSchool(UUID schoolId, UUID learnerUserId) {
        return taskSubmissionRepository.findByLearnerUserId(learnerUserId).stream()
                .filter(submission -> belongsToSchool(submission.getTaskId(), schoolId))
                .toList();
    }

    private List<TaskSubmission> submissionsForSchool(UUID schoolId) {
        return taskSubmissionRepository.findAll().stream()
                .filter(submission -> belongsToSchool(submission.getTaskId(), schoolId))
                .toList();
    }

    private void runPlagiarismCheck(TaskSubmission saved) {
        if (saved.getSubmissionText() == null || saved.getSubmissionText().isBlank()) {
            return;
        }
        List<TaskSubmission> peers = taskSubmissionRepository.findByTaskId(saved.getTaskId()).stream()
                .filter(candidate -> !candidate.getId().equals(saved.getId()))
                .filter(candidate -> candidate.getSubmissionText() != null && !candidate.getSubmissionText().isBlank())
                .toList();

        for (TaskSubmission peer : peers) {
            BigDecimal similarity = similarityPercent(saved.getSubmissionText(), peer.getSubmissionText());
            PlagiarismReport report = new PlagiarismReport();
            report.setSubmissionId(saved.getId());
            report.setComparedSubmissionId(peer.getId());
            report.setSimilarityPercentage(similarity);
            report.setFlagged(similarity.compareTo(new BigDecimal("70.00")) >= 0);
            report.setReportDetails("Token overlap comparison");
            plagiarismReportRepository.save(report);
        }
    }

    private BigDecimal similarityPercent(String left, String right) {
        Set<String> leftTokens = new HashSet<>(List.of(left.toLowerCase(Locale.ROOT).split("\\W+")));
        Set<String> rightTokens = new HashSet<>(List.of(right.toLowerCase(Locale.ROOT).split("\\W+")));
        leftTokens.remove("");
        rightTokens.remove("");
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        int denominator = Math.max(leftTokens.size(), rightTokens.size());
        return BigDecimal.valueOf(intersection.size())
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private void ensureTeacherAssignment(UUID schoolId, UUID teacherUserId, UUID classId, UUID subjectId) {
        teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, teacherUserId, classId, subjectId)
                .orElseThrow(() -> new ResourceConflictException("Teacher is not assigned to this class and subject"));
    }

    private boolean belongsToSchool(UUID taskId, UUID schoolId) {
        return schoolTaskRepository.findById(taskId)
                .map(task -> schoolId.equals(task.getSchoolId()))
                .orElse(false);
    }

    private void validateSubjectRequest(String subjectName, String phase, String grade, String gradeRange, String languageLevel, String subjectType) {
        if (subjectName == null || subjectName.isBlank()) {
            throw new ResourceConflictException("Subject name is required.");
        }
        if (phase == null || phase.isBlank()) {
            throw new ResourceConflictException("Phase is required.");
        }
        if (trim(grade) == null && trim(gradeRange) == null) {
            throw new ResourceConflictException("Grade or grade range is required.");
        }
        if (trim(languageLevel) != null && !Set.of("Home Language", "First Additional Language", "Second Additional Language").contains(languageLevel.trim())) {
            throw new ResourceConflictException("Language level is invalid.");
        }
        if (trim(subjectType) != null && subjectType.trim().length() > 80) {
            throw new ResourceConflictException("Subject type is too long.");
        }
    }

    private void ensureNoDuplicateSchoolSubject(UUID schoolId, UUID currentSubjectId, String subjectName, String phase, String grade, String gradeRange, String languageLevel) {
        String normalizedName = subjectName.trim().toLowerCase(Locale.ROOT);
        String normalizedPhase = phase.trim().toLowerCase(Locale.ROOT);
        String normalizedGrade = safe(trim(grade) != null ? trim(grade) : trim(gradeRange)).toLowerCase(Locale.ROOT);
        String normalizedLanguageLevel = safe(trim(languageLevel)).toLowerCase(Locale.ROOT);
        boolean duplicate = schoolSubjectRepository.findBySchoolIdOrderByPhaseAscSubjectNameAsc(schoolId).stream()
                .filter(existing -> !Objects.equals(existing.getId(), currentSubjectId))
                .anyMatch(existing ->
                        normalizedName.equals(safe(existing.getSubjectName()).toLowerCase(Locale.ROOT))
                                && normalizedPhase.equals(safe(existing.getPhase()).toLowerCase(Locale.ROOT))
                                && normalizedGrade.equals(safe(trim(existing.getGrade()) == null ? existing.getGradeRange() : existing.getGrade()).toLowerCase(Locale.ROOT))
                                && normalizedLanguageLevel.equals(safe(existing.getLanguageLevel()).toLowerCase(Locale.ROOT)));
        if (duplicate) {
            throw new ResourceConflictException("This subject already exists for the selected phase, grade, and language level.");
        }
    }

    private SubjectCatalogue resolveSubjectCatalogue(UUID subjectCatalogueId) {
        if (subjectCatalogueId == null) {
            return null;
        }
        return subjectCatalogueRepository.findById(subjectCatalogueId)
                .filter(SubjectCatalogue::isActive)
                .orElseThrow(() -> new ResourceConflictException("Subject catalogue item not found."));
    }

    private UUID resolveTeacherReference(UUID schoolId, UUID teacherUserId) {
        if (teacherUserId == null) {
            return null;
        }
        schoolUserProfileRepository.findBySchoolIdAndUserIdAndDeletedFalse(schoolId, teacherUserId)
                .filter(profile -> SchoolAccessService.ROLE_TEACHER.equals(profile.getRoleName()) && profile.isActive())
                .orElseThrow(() -> new ResourceConflictException("Teacher not found in this school"));
        return teacherUserId;
    }

    private boolean resolveCapsAligned(SubjectCatalogue catalogue, Boolean requestedCapsAligned) {
        if (catalogue != null) {
            return true;
        }
        return requestedCapsAligned == null || requestedCapsAligned;
    }

    private AtpTopic resolveAtpTopic(UUID atpTopicId) {
        if (atpTopicId == null) {
            return null;
        }
        return atpTopicRepository.findById(atpTopicId)
                .filter(AtpTopic::isActive)
                .orElseThrow(() -> new ResourceConflictException("ATP topic not found."));
    }

    private UUID resolveTaskTeacherUserId(UUID schoolId, SchoolPortalDtos.SchoolTaskRequest request) {
        if (request.teacherUserId() != null) {
            ensureTeacherAssignment(schoolId, request.teacherUserId(), request.classId(), request.subjectId());
            return request.teacherUserId();
        }
        return teacherAssignmentRepository.findBySchoolIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, request.classId(), request.subjectId()).stream()
                .map(TeacherAssignment::getTeacherUserId)
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Assign a teacher to this class and subject before creating an ATP assignment."));
    }

    private SchoolPortalDtos.AtpTopicView toAtpTopicView(AtpTopic item) {
        return new SchoolPortalDtos.AtpTopicView(
                item.getId(),
                item.getPhase(),
                item.getGrade(),
                item.getSubjectCatalogue() == null ? null : item.getSubjectCatalogue().getId(),
                item.getSubjectName(),
                item.getAcademicYear(),
                item.getTerm(),
                item.getWeekNumber(),
                item.getTopic(),
                item.getSubtopic(),
                item.getRecommendedActivities(),
                item.getAssessmentGuidance(),
                item.getCapsReference(),
                item.isActive()
        );
    }

    private boolean hasLinkedAcademicRecords(UUID schoolId, UUID subjectId) {
        boolean hasTeacherAssignments = teacherAssignmentRepository.findAll().stream()
                .anyMatch(item -> schoolId.equals(item.getSchoolId()) && subjectId.equals(item.getSubjectId()) && item.isActive());
        boolean hasLearnerEnrollments = learnerEnrollmentRepository.findAll().stream()
                .anyMatch(item -> schoolId.equals(item.getSchoolId()) && subjectId.equals(item.getSubjectId()) && item.isActive());
        boolean hasTasks = schoolTaskRepository.findAll().stream()
                .anyMatch(item -> schoolId.equals(item.getSchoolId()) && subjectId.equals(item.getSubjectId()));
        return hasTeacherAssignments || hasLearnerEnrollments || hasTasks;
    }

    private long countByPhase(List<SchoolPortalDtos.SchoolSubjectView> subjects, String phase) {
        return subjects.stream().filter(item -> item.active() && phase.equalsIgnoreCase(item.phase())).count();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private UserStatus resolveUserStatus(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return UserStatus.ACTIVE;
        }
        try {
            return UserStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResourceConflictException("User status is invalid.");
        }
    }

    private void notifySchoolAdmins(UUID schoolId, String eventType, String title, String message) {
        List<com.edurite.school.portal.entity.SchoolUserProfile> schoolAdmins = schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(schoolId, SchoolAccessService.ROLE_SCHOOL_ADMIN);
        if (schoolAdmins == null) {
            schoolAdmins = Collections.emptyList();
        }
        schoolAdmins.stream()
                .filter(profile -> profile.isActive() && !profile.isDeleted())
                .map(com.edurite.school.portal.entity.SchoolUserProfile::getUserId)
                .distinct()
                .forEach(userId -> notificationService.createInApp(userId, eventType, title, message));
    }
}


