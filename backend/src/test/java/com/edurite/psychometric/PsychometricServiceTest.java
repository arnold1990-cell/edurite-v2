package com.edurite.psychometric;

import com.edurite.psychometric.dto.PsychometricSubmissionRequest;
import com.edurite.psychometric.dto.PsychometricAttemptRequest;
import com.edurite.psychometric.entity.PsychometricAssessment;
import com.edurite.psychometric.entity.PsychometricQuestion;
import com.edurite.psychometric.repository.PsychometricAnswerRepository;
import com.edurite.psychometric.repository.PsychometricAssessmentRepository;
import com.edurite.psychometric.repository.PsychometricAttemptRepository;
import com.edurite.psychometric.repository.PsychometricQuestionRepository;
import com.edurite.psychometric.repository.PsychometricResultRepository;
import com.edurite.psychometric.entity.PsychometricSubmission;
import com.edurite.psychometric.repository.PsychometricSubmissionRepository;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PsychometricServiceTest {

    @Test
    void submitForStudentStoresScoresAndReturnsResponse() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        PsychometricSubmissionRepository psychometricSubmissionRepository = mock(PsychometricSubmissionRepository.class);
        PsychometricAssessmentRepository psychometricAssessmentRepository = mock(PsychometricAssessmentRepository.class);
        PsychometricQuestionRepository psychometricQuestionRepository = mock(PsychometricQuestionRepository.class);
        PsychometricAttemptRepository psychometricAttemptRepository = mock(PsychometricAttemptRepository.class);
        PsychometricAnswerRepository psychometricAnswerRepository = mock(PsychometricAnswerRepository.class);
        PsychometricResultRepository psychometricResultRepository = mock(PsychometricResultRepository.class);
        PsychometricService psychometricService = new PsychometricService(
                currentUserService,
                studentProfileRepository,
                psychometricSubmissionRepository,
                psychometricAssessmentRepository,
                psychometricQuestionRepository,
                psychometricAttemptRepository,
                psychometricAnswerRepository,
                psychometricResultRepository,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> user.getEmail();

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(psychometricSubmissionRepository.save(any(PsychometricSubmission.class))).thenAnswer(invocation -> {
            PsychometricSubmission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            submission.setCreatedAt(java.time.OffsetDateTime.now());
            return submission;
        });

        PsychometricSubmissionRequest request = new PsychometricSubmissionRequest(List.of(
                new PsychometricSubmissionRequest.AnswerItem("analytical", 5),
                new PsychometricSubmissionRequest.AnswerItem("analytical", 4),
                new PsychometricSubmissionRequest.AnswerItem("communication", 2)
        ));

        var response = psychometricService.submitForStudent(principal, request);

        assertThat(response.scores()).containsKeys("analytical", "communication");
        assertThat(response.strengthAreas()).contains("analytical");
        assertThat(response.growthAreas()).contains("communication");
    }

    @Test
    void submitAssessmentAttemptRejectsIncompleteAnswerSet() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        PsychometricSubmissionRepository psychometricSubmissionRepository = mock(PsychometricSubmissionRepository.class);
        PsychometricAssessmentRepository psychometricAssessmentRepository = mock(PsychometricAssessmentRepository.class);
        PsychometricQuestionRepository psychometricQuestionRepository = mock(PsychometricQuestionRepository.class);
        PsychometricAttemptRepository psychometricAttemptRepository = mock(PsychometricAttemptRepository.class);
        PsychometricAnswerRepository psychometricAnswerRepository = mock(PsychometricAnswerRepository.class);
        PsychometricResultRepository psychometricResultRepository = mock(PsychometricResultRepository.class);
        PsychometricService psychometricService = new PsychometricService(
                currentUserService,
                studentProfileRepository,
                psychometricSubmissionRepository,
                psychometricAssessmentRepository,
                psychometricQuestionRepository,
                psychometricAttemptRepository,
                psychometricAnswerRepository,
                psychometricResultRepository,
                new ObjectMapper()
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> user.getEmail();

        PsychometricAssessment assessment = new PsychometricAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setCode("EDURITE_CORE_V1");
        assessment.setActive(true);

        PsychometricQuestion questionOne = new PsychometricQuestion();
        questionOne.setId(UUID.randomUUID());
        questionOne.setAssessmentId(assessment.getId());
        questionOne.setQuestionKey("q1");
        questionOne.setDimensionKey("analytical");
        questionOne.setMinScore(1);
        questionOne.setMaxScore(5);

        PsychometricQuestion questionTwo = new PsychometricQuestion();
        questionTwo.setId(UUID.randomUUID());
        questionTwo.setAssessmentId(assessment.getId());
        questionTwo.setQuestionKey("q2");
        questionTwo.setDimensionKey("communication");
        questionTwo.setMinScore(1);
        questionTwo.setMaxScore(5);

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(psychometricAssessmentRepository.findByIdAndActiveTrue(assessment.getId())).thenReturn(Optional.of(assessment));
        when(psychometricQuestionRepository.findTop25ByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(assessment.getId()))
                .thenReturn(List.of(questionOne, questionTwo));

        PsychometricAttemptRequest request = new PsychometricAttemptRequest(List.of(
                new PsychometricAttemptRequest.AnswerItem(questionOne.getId(), 4)
        ));

        assertThatThrownBy(() -> psychometricService.submitAssessmentAttempt(principal, assessment.getId(), request))
                .hasMessageContaining("requires 2 answered questions");
    }
}

