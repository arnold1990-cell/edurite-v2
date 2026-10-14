package com.edurite.psychometric;

import com.edurite.psychometric.controller.PsychometricController;
import com.edurite.psychometric.entity.*;
import com.edurite.psychometric.repository.*;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PsychometricHistoryIsolationTest {
    @Test void historyAndLatestIgnoreForgedLearnerIdsAndRetainArchivedAttempts() throws Exception {
        var current = mock(CurrentUserService.class);
        var profiles = mock(StudentProfileRepository.class);
        var submissions = mock(PsychometricSubmissionRepository.class);
        var assessments = mock(PsychometricAssessmentRepository.class);
        var attempts = mock(PsychometricAttemptRepository.class);
        var results = mock(PsychometricResultRepository.class);
        var service = new PsychometricService(current, profiles, submissions, assessments,
                mock(PsychometricQuestionRepository.class), attempts, mock(PsychometricAnswerRepository.class), results, new ObjectMapper());
        Principal alice = () -> "alice", bob = () -> "bob";
        var aliceProfile = new StudentProfile(); aliceProfile.setId(UUID.randomUUID());
        var bobProfile = new StudentProfile(); bobProfile.setId(UUID.randomUUID());
        for (var entry : Map.of(alice, aliceProfile, bob, bobProfile).entrySet()) {
            var user = new User(); user.setId(UUID.randomUUID());
            when(current.requireUser(entry.getKey())).thenReturn(user);
            when(profiles.findByUserId(user.getId())).thenReturn(Optional.of(entry.getValue()));
            var submission = new PsychometricSubmission(); submission.setId(UUID.randomUUID());
            submission.setStudentId(entry.getValue().getId()); submission.setScores("{\"analytical\":4}");
            submission.setInterpretation(entry.getKey().getName() + " private result"); submission.setCreatedAt(OffsetDateTime.now());
            when(submissions.findTopByStudentIdOrderByCreatedAtDesc(entry.getValue().getId())).thenReturn(Optional.of(submission));
        }
        UUID archivedAssessment = UUID.randomUUID();
        var attempt = new PsychometricAttempt(); attempt.setId(UUID.randomUUID()); attempt.setStudentId(aliceProfile.getId());
        attempt.setAssessmentId(archivedAssessment); attempt.setSubmittedAt(OffsetDateTime.now());
        var result = new PsychometricResult(); result.setStudentId(aliceProfile.getId()); result.setScores("{\"analytical\":4}");
        result.setStrongestAreas("[\"analytical\"]"); result.setGrowthAreas("[]"); result.setSummaryText("alice archived result");
        when(attempts.findByStudentIdOrderBySubmittedAtDesc(aliceProfile.getId())).thenReturn(List.of(attempt));
        when(attempts.findByStudentIdAndAssessmentIdOrderBySubmittedAtDesc(aliceProfile.getId(), archivedAssessment)).thenReturn(List.of(attempt));
        when(results.findByAttemptId(attempt.getId())).thenReturn(Optional.of(result));
        var mvc = MockMvcBuilders.standaloneSetup(new PsychometricController(service)).build();
        for (String prefix : List.of("/api", "/api/v1")) {
            mvc.perform(get(prefix + "/student/psychometric/history").principal(alice)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].interpretation").value("alice archived result"));
            mvc.perform(get(prefix + "/student/psychometric/history").principal(bob).param("studentId", aliceProfile.getId().toString()))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
            mvc.perform(get(prefix + "/student/psychometric/assessments/" + archivedAssessment + "/attempts").principal(bob)
                    .param("studentId", aliceProfile.getId().toString())).andExpect(status().isOk()).andExpect(content().json("[]"));
            mvc.perform(get(prefix + "/student/psychometric/latest").principal(bob).param("studentId", aliceProfile.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.interpretation").value("bob private result"));
        }
        assertThat(service.assessmentAttemptHistory(alice, archivedAssessment)).hasSize(1);
        verifyNoInteractions(assessments); // Archived definitions need not remain active to view saved results.
        result.setStudentId(bobProfile.getId());
        assertThat(service.assessmentHistory(alice)).isEmpty(); // Defense against an inconsistent result owner.
    }
}
