package com.edurite.school.service;

import com.edurite.school.portal.dto.SchoolPortalDtos;
import com.edurite.school.portal.entity.TaskSubmission;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudentSchoolService {

    private final AssignmentService assignmentService;
    public StudentSchoolService(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    public SchoolPortalDtos.DashboardResponse dashboard(UUID schoolId, UUID studentUserId) {
        return assignmentService.dashboard(SchoolAccessService.ROLE_SCHOOL_STUDENT, schoolId, studentUserId);
    }

    public Object notes(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentNotes(schoolId, studentUserId);
    }

    public List<SchoolPortalDtos.LearnerSubjectView> subjects(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentSubjects(schoolId, studentUserId);
    }

    public List<SchoolPortalDtos.StudentTaskView> tasks(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentTasks(schoolId, studentUserId);
    }

    public List<SchoolPortalDtos.StudentTaskView> assessments(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentAssessments(schoolId, studentUserId);
    }

    public TaskSubmission submit(UUID schoolId, UUID studentUserId, SchoolPortalDtos.TaskSubmissionRequest request) {
        return assignmentService.submitTask(schoolId, studentUserId, request);
    }

    public List<SchoolPortalDtos.SubmissionView> submissions(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentSubmissionViews(schoolId, studentUserId);
    }

    public List<SchoolPortalDtos.SubmissionView> marks(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentMarks(schoolId, studentUserId);
    }

    public SchoolPortalDtos.ProgressSummaryResponse progress(UUID schoolId, UUID studentUserId) {
        return assignmentService.studentProgress(schoolId, studentUserId);
    }

}


