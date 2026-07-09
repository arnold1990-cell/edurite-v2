package com.edurite.school.portal;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.LearnerEnrollment;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SubmissionFeedback;
import com.edurite.school.portal.entity.TaskSubmission;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.AtpTopicRepository;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.LearningNoteRepository;
import com.edurite.school.portal.repository.PlagiarismReportRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolTaskRepository;
import com.edurite.school.portal.repository.SubmissionFeedbackRepository;
import com.edurite.school.portal.repository.SubjectCatalogueRepository;
import com.edurite.school.portal.repository.TaskSubmissionRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.service.AssignmentService;
import com.edurite.school.service.SchoolAccessService;
import com.edurite.notification.service.NotificationService;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolPortalServiceTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private SchoolSubjectRepository schoolSubjectRepository;
    @Mock private SubjectCatalogueRepository subjectCatalogueRepository;
    @Mock private AtpTopicRepository atpTopicRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private LearnerEnrollmentRepository learnerEnrollmentRepository;
    @Mock private LearningNoteRepository learningNoteRepository;
    @Mock private SchoolTaskRepository schoolTaskRepository;
    @Mock private TaskSubmissionRepository taskSubmissionRepository;
    @Mock private SubmissionFeedbackRepository submissionFeedbackRepository;
    @Mock private PlagiarismReportRepository plagiarismReportRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SchoolUserProfileRepository schoolUserProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                schoolRepository,
                schoolClassRepository,
                schoolSubjectRepository,
                subjectCatalogueRepository,
                atpTopicRepository,
                teacherAssignmentRepository,
                learnerEnrollmentRepository,
                learningNoteRepository,
                schoolTaskRepository,
                taskSubmissionRepository,
                submissionFeedbackRepository,
                plagiarismReportRepository,
                userRepository,
                roleRepository,
                schoolUserProfileRepository,
                passwordEncoder,
                notificationService
        );
    }

    @Test
    void teacherCreatesTaskWhenAssigned() {
        UUID schoolId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        TeacherAssignment assignment = new TeacherAssignment();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setGrade("Grade 10");
        schoolClass.setAcademicYear(2026);
        SchoolSubject schoolSubject = new SchoolSubject();
        schoolSubject.setId(subjectId);
        schoolSubject.setSchoolId(schoolId);
        schoolSubject.setPhase("FET");
        when(teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, teacherId, classId, subjectId)).thenReturn(Optional.of(assignment));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(schoolSubjectRepository.findById(subjectId)).thenReturn(Optional.of(schoolSubject));
        when(schoolTaskRepository.save(any(SchoolTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolTask task = assignmentService.createTask(
                schoolId,
                teacherId,
                new SchoolPortalDtos.SchoolTaskRequest(null, classId, subjectId, null, "SBA", "Task 1", 2026, "FET", "Grade 10", "Do it", "Formal assessment", 1, OffsetDateTime.now().plusDays(2), "Term 1", new BigDecimal("100"), "Rubric", "Worksheet", "Analysis", "SBA")
        );

        assertThat(task.getTitle()).isEqualTo("Task 1");
        assertThat(task.isReleased()).isTrue();
    }

    @Test
    void studentSubmitsTaskAndTeacherMarksTask() {
        UUID schoolId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        SchoolTask task = new SchoolTask();
        task.setId(taskId);
        task.setSchoolId(schoolId);
        task.setClassId(classId);
        task.setSubjectId(subjectId);
        task.setDueAt(OffsetDateTime.now().plusDays(1));

        LearnerEnrollment enrollment = new LearnerEnrollment();
        TeacherAssignment assignment = new TeacherAssignment();

        when(schoolTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, learnerId, classId, subjectId)).thenReturn(Optional.of(enrollment));
        when(taskSubmissionRepository.findByTaskIdAndLearnerUserId(taskId, learnerId)).thenReturn(Optional.empty());
        when(taskSubmissionRepository.save(any(TaskSubmission.class))).thenAnswer(invocation -> {
            TaskSubmission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(submissionId);
            }
            return submission;
        });
        when(taskSubmissionRepository.findByTaskId(taskId)).thenReturn(List.of());
        when(teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, teacherId, classId, subjectId)).thenReturn(Optional.of(assignment));
        when(taskSubmissionRepository.findById(submissionId)).thenAnswer(invocation -> {
            TaskSubmission submission = new TaskSubmission();
            submission.setId(submissionId);
            submission.setTaskId(taskId);
            submission.setLearnerUserId(learnerId);
            return Optional.of(submission);
        });
        when(submissionFeedbackRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskSubmission submission = assignmentService.submitTask(schoolId, learnerId, new SchoolPortalDtos.TaskSubmissionRequest(taskId, "my work", null));
        SubmissionFeedback feedback = assignmentService.markSubmission(
                schoolId,
                teacherId,
                submission.getId(),
                new SchoolPortalDtos.MarkSubmissionRequest(new BigDecimal("84"), "Good work", "Rubric 84", true)
        );

        assertThat(submission.getStatus()).isEqualTo("SUBMITTED");
        assertThat(feedback.getMarksAwarded()).isEqualByComparingTo("84");
        assertThat(feedback.isReleased()).isTrue();
    }

    @Test
    void teacherCannotCreateTaskForUnassignedClass() {
        UUID schoolId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        when(teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndClassIdAndSubjectIdAndActiveTrue(schoolId, teacherId, classId, subjectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.createTask(
                schoolId,
                teacherId,
                new SchoolPortalDtos.SchoolTaskRequest(null, classId, subjectId, null, "ASSIGNMENT", "Task", 2026, "FET", "Grade 10", "", "Assignment", null, OffsetDateTime.now().plusDays(1), "Term 1", new BigDecimal("50"), null, null, null, "Informal")
        )).isInstanceOf(ResourceConflictException.class).hasMessageContaining("not assigned");
    }

    @Test
    void schoolAdminDashboardCountsOnlyCurrentSchoolSubmissions() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID taskInScopeId = UUID.randomUUID();
        UUID taskOutOfScopeId = UUID.randomUUID();

        SchoolTask taskInScope = new SchoolTask();
        taskInScope.setId(taskInScopeId);
        taskInScope.setSchoolId(schoolId);

        SchoolTask taskOutOfScope = new SchoolTask();
        taskOutOfScope.setId(taskOutOfScopeId);
        taskOutOfScope.setSchoolId(otherSchoolId);

        TaskSubmission submissionInScope = new TaskSubmission();
        submissionInScope.setId(UUID.randomUUID());
        submissionInScope.setTaskId(taskInScopeId);

        TaskSubmission submissionOutOfScope = new TaskSubmission();
        submissionOutOfScope.setId(UUID.randomUUID());
        submissionOutOfScope.setTaskId(taskOutOfScopeId);

        when(schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of());
        when(schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of());
        when(learningNoteRepository.findAll()).thenReturn(List.of());
        when(schoolTaskRepository.findAll()).thenReturn(List.of(taskInScope, taskOutOfScope));
        when(taskSubmissionRepository.findAll()).thenReturn(List.of(submissionInScope, submissionOutOfScope));
        when(schoolTaskRepository.findById(taskInScopeId)).thenReturn(Optional.of(taskInScope));
        when(schoolTaskRepository.findById(taskOutOfScopeId)).thenReturn(Optional.of(taskOutOfScope));

        SchoolPortalDtos.DashboardResponse dashboard = assignmentService.dashboard(
                SchoolAccessService.ROLE_SCHOOL_ADMIN,
                schoolId,
                UUID.randomUUID()
        );

        assertThat(dashboard.totalTasks()).isEqualTo(1);
        assertThat(dashboard.totalSubmissions()).isEqualTo(1);
    }

    @Test
    void schoolStudentDashboardCountsOnlyOwnSchoolSubmissions() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        UUID taskInScopeId = UUID.randomUUID();
        UUID taskOutOfScopeId = UUID.randomUUID();

        SchoolTask taskInScope = new SchoolTask();
        taskInScope.setId(taskInScopeId);
        taskInScope.setSchoolId(schoolId);

        SchoolTask taskOutOfScope = new SchoolTask();
        taskOutOfScope.setId(taskOutOfScopeId);
        taskOutOfScope.setSchoolId(otherSchoolId);

        TaskSubmission submissionInScope = new TaskSubmission();
        submissionInScope.setId(UUID.randomUUID());
        submissionInScope.setTaskId(taskInScopeId);
        submissionInScope.setLearnerUserId(learnerId);

        TaskSubmission submissionOutOfScope = new TaskSubmission();
        submissionOutOfScope.setId(UUID.randomUUID());
        submissionOutOfScope.setTaskId(taskOutOfScopeId);
        submissionOutOfScope.setLearnerUserId(learnerId);

        when(schoolClassRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of());
        when(schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of());
        when(learningNoteRepository.findAll()).thenReturn(List.of());
        when(schoolTaskRepository.findAll()).thenReturn(List.of(taskInScope));
        when(taskSubmissionRepository.findByLearnerUserId(learnerId)).thenReturn(List.of(submissionInScope, submissionOutOfScope));
        when(schoolTaskRepository.findById(taskInScopeId)).thenReturn(Optional.of(taskInScope));
        when(schoolTaskRepository.findById(taskOutOfScopeId)).thenReturn(Optional.of(taskOutOfScope));

        SchoolPortalDtos.DashboardResponse dashboard = assignmentService.dashboard(
                SchoolAccessService.ROLE_SCHOOL_STUDENT,
                schoolId,
                learnerId
        );

        assertThat(dashboard.totalTasks()).isEqualTo(1);
        assertThat(dashboard.totalSubmissions()).isEqualTo(1);
    }

    @Test
    void studentCannotSeeAnotherLearnerSubmission() {
        UUID schoolId = UUID.randomUUID();
        UUID learnerA = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskSubmissionRepository.findByLearnerUserId(learnerA)).thenReturn(List.of());

        var views = assignmentService.studentSubmissionViews(schoolId, learnerA);

        assertThat(views).isEmpty();
    }

    @Test
    void studentViewsReleasedFeedbackOnly() {
        UUID schoolId = UUID.randomUUID();
        UUID learnerId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        SchoolTask task = new SchoolTask();
        task.setId(taskId);
        task.setSchoolId(schoolId);

        TaskSubmission submission = new TaskSubmission();
        submission.setId(UUID.randomUUID());
        submission.setTaskId(taskId);
        submission.setLearnerUserId(learnerId);

        SubmissionFeedback feedback = new SubmissionFeedback();
        feedback.setSubmissionId(submission.getId());
        feedback.setMarksAwarded(new BigDecimal("70"));
        feedback.setComments("Released feedback");
        feedback.setReleased(true);

        User learner = new User();
        learner.setId(learnerId);
        learner.setFirstName("Learner");
        learner.setLastName("One");

        when(taskSubmissionRepository.findByLearnerUserId(learnerId)).thenReturn(List.of(submission));
        when(schoolTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(submissionFeedbackRepository.findBySubmissionId(submission.getId())).thenReturn(Optional.of(feedback));
        when(plagiarismReportRepository.findBySubmissionId(submission.getId())).thenReturn(List.of());
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(learner));

        var views = assignmentService.studentSubmissionViews(schoolId, learnerId);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().marks()).isEqualByComparingTo("70");
        assertThat(views.getFirst().feedback()).isEqualTo("Released feedback");
    }
}

