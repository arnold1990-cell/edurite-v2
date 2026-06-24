package com.edurite.school.service;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.LearningNote;
import com.edurite.school.portal.entity.SchoolTask;
import com.edurite.school.portal.entity.SubmissionFeedback;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TeacherService {

    private final AssignmentService assignmentService;
    private final SchoolService schoolService;

    public TeacherService(AssignmentService assignmentService, SchoolService schoolService) {
        this.assignmentService = assignmentService;
        this.schoolService = schoolService;
    }

    public SchoolPortalDtos.DashboardResponse dashboard(UUID schoolId, UUID teacherUserId) {
        return assignmentService.dashboard(SchoolAccessService.ROLE_TEACHER, schoolId, teacherUserId);
    }

    public LearningNote createNote(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.LearningNoteRequest request) {
        return assignmentService.createNote(schoolId, teacherUserId, request);
    }

    public SchoolTask createTask(UUID schoolId, UUID teacherUserId, SchoolPortalDtos.SchoolTaskRequest request) {
        return assignmentService.createTask(schoolId, teacherUserId, request);
    }

    public List<SchoolPortalDtos.AtpTopicView> atpTopics(UUID schoolId, String phase, String grade, UUID subjectId, String term) {
        return assignmentService.atpTopics(schoolId, phase, grade, subjectId, term);
    }

    public List<SchoolTask> tasks(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherTasks(schoolId, teacherUserId);
    }

    public List<SchoolPortalDtos.SubmissionView> taskSubmissions(UUID schoolId, UUID teacherUserId, UUID taskId) {
        return assignmentService.taskSubmissionsForTeacher(schoolId, teacherUserId, taskId);
    }

    public SubmissionFeedback markSubmission(UUID schoolId, UUID teacherUserId, UUID submissionId, SchoolPortalDtos.MarkSubmissionRequest request) {
        return assignmentService.markSubmission(schoolId, teacherUserId, submissionId, request);
    }

    public SchoolPortalDtos.ProgressSummaryResponse progress(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherProgress(schoolId, teacherUserId);
    }

    public List<SchoolPortalDtos.TeacherClassView> classes(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherClasses(schoolId, teacherUserId);
    }

    public List<SchoolPortalDtos.TeacherSubjectView> subjects(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherSubjects(schoolId, teacherUserId);
    }

    public List<SchoolTask> assessments(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherAssessments(schoolId, teacherUserId);
    }

    public SchoolPortalDtos.TeacherAnalyticsResponse analytics(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherAnalytics(schoolId, teacherUserId);
    }

    public List<SchoolPortalDtos.TeacherActivityItem> activity(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherActivity(schoolId, teacherUserId);
    }

    public List<SchoolPortalDtos.TeacherCalendarItem> calendar(UUID schoolId, UUID teacherUserId) {
        return assignmentService.teacherCalendar(schoolId, teacherUserId);
    }

    public SchoolPortalDtos.DashboardSnapshot portalDashboard(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolService.portalDashboard(schoolId, viewerUserId, roleName);
    }

    public SchoolPortalDtos.LearnerListResponse portalLearners(UUID schoolId, UUID viewerUserId, String roleName, String search, String grade, String className) {
        return schoolService.portalLearners(schoolId, viewerUserId, roleName, search, grade, className);
    }

    public SchoolPortalDtos.LearnerProfileResponse portalLearnerProfile(UUID schoolId, UUID viewerUserId, String roleName, UUID learnerUserId) {
        return schoolService.portalLearnerProfile(schoolId, viewerUserId, roleName, learnerUserId);
    }

    public SchoolPortalDtos.AcademicInsightsResponse academicInsights(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolService.academicInsights(schoolId, viewerUserId, roleName);
    }

    public SchoolPortalDtos.CareerReadinessResponse careerReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolService.careerReadiness(schoolId, viewerUserId, roleName);
    }

    public SchoolPortalDtos.BursaryReadinessResponse bursaryReadiness(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolService.bursaryReadiness(schoolId, viewerUserId, roleName);
    }

    public List<SchoolPortalDtos.InterventionReportItem> interventions(UUID schoolId, UUID viewerUserId, String roleName) {
        return schoolService.interventions(schoolId, viewerUserId, roleName);
    }

    public SchoolPortalDtos.InterventionReportItem createIntervention(UUID schoolId, UUID actorUserId, String roleName, SchoolPortalDtos.InterventionRequest request) {
        return schoolService.createIntervention(schoolId, actorUserId, roleName, request);
    }

    public SchoolPortalDtos.ReportExportResponse exportReport(UUID schoolId, UUID viewerUserId, String roleName, String type, String format) {
        return schoolService.exportReport(schoolId, viewerUserId, roleName, type, format);
    }

    public SchoolPortalDtos.PortalSettingsResponse portalSettings(UUID schoolId) {
        return schoolService.portalSettings(schoolId);
    }
}


