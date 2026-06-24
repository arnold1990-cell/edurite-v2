package com.edurite.psychometric.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.psychometric.dto.PsychometricAssessmentDto;
import com.edurite.psychometric.dto.PsychometricAttemptRequest;
import com.edurite.psychometric.dto.PsychometricAttemptResultDto;
import com.edurite.psychometric.dto.PsychometricQuestionDto;
import com.edurite.psychometric.dto.PsychometricSubmissionRequest;
import com.edurite.psychometric.dto.PsychometricSubmissionResponse;
import com.edurite.psychometric.entity.PsychometricAnswer;
import com.edurite.psychometric.entity.PsychometricAssessment;
import com.edurite.psychometric.entity.PsychometricAttempt;
import com.edurite.psychometric.entity.PsychometricQuestion;
import com.edurite.psychometric.entity.PsychometricResult;
import com.edurite.psychometric.entity.PsychometricSubmission;
import com.edurite.psychometric.repository.PsychometricAnswerRepository;
import com.edurite.psychometric.repository.PsychometricAssessmentRepository;
import com.edurite.psychometric.repository.PsychometricAttemptRepository;
import com.edurite.psychometric.repository.PsychometricQuestionRepository;
import com.edurite.psychometric.repository.PsychometricResultRepository;
import com.edurite.psychometric.repository.PsychometricSubmissionRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PsychometricService {

    private static final String DEFAULT_ASSESSMENT_CODE = "EDURITE_CORE_V1";
    private static final int MAX_ACTIVE_ASSESSMENT_QUESTIONS = 25;

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final PsychometricAssessmentRepository psychometricAssessmentRepository;
    private final PsychometricQuestionRepository psychometricQuestionRepository;
    private final PsychometricAttemptRepository psychometricAttemptRepository;
    private final PsychometricAnswerRepository psychometricAnswerRepository;
    private final PsychometricResultRepository psychometricResultRepository;
    private final ObjectMapper objectMapper;

    public PsychometricService(
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            PsychometricSubmissionRepository psychometricSubmissionRepository,
            PsychometricAssessmentRepository psychometricAssessmentRepository,
            PsychometricQuestionRepository psychometricQuestionRepository,
            PsychometricAttemptRepository psychometricAttemptRepository,
            PsychometricAnswerRepository psychometricAnswerRepository,
            PsychometricResultRepository psychometricResultRepository,
            ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.psychometricAssessmentRepository = psychometricAssessmentRepository;
        this.psychometricQuestionRepository = psychometricQuestionRepository;
        this.psychometricAttemptRepository = psychometricAttemptRepository;
        this.psychometricAnswerRepository = psychometricAnswerRepository;
        this.psychometricResultRepository = psychometricResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PsychometricAssessmentDto> listAssessments() {
        return psychometricAssessmentRepository.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(assessment -> new PsychometricAssessmentDto(
                        assessment.getId(),
                        assessment.getCode(),
                        assessment.getName(),
                        assessment.getDescription(),
                        assessment.getVersion(),
                        assessment.isPublicAvailable(),
                        psychometricQuestionRepository.findTop25ByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(assessment.getId()).size()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PsychometricQuestionDto> assessmentQuestions(UUID assessmentId) {
        findAssessmentOrThrow(assessmentId);
        return psychometricQuestionRepository.findTop25ByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(assessmentId).stream()
                .map(question -> new PsychometricQuestionDto(
                        question.getId(),
                        question.getQuestionKey(),
                        question.getPrompt(),
                        question.getDimensionKey(),
                        question.getMinScore(),
                        question.getMaxScore(),
                        question.getDisplayOrder()
                ))
                .toList();
    }

    @Transactional
    public PsychometricAttemptResultDto submitAssessmentAttempt(Principal principal, UUID assessmentId, PsychometricAttemptRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found."));

        PsychometricAssessment assessment = findAssessmentOrThrow(assessmentId);
        List<PsychometricQuestion> questions = psychometricQuestionRepository.findTop25ByAssessmentIdAndActiveTrueOrderByDisplayOrderAsc(assessment.getId());
        if (questions.isEmpty()) {
            throw new ResourceConflictException("Assessment has no active questions.");
        }

        Map<UUID, PsychometricQuestion> questionById = questions.stream()
                .collect(java.util.stream.Collectors.toMap(PsychometricQuestion::getId, question -> question));

        List<PsychometricAttemptRequest.AnswerItem> answerItems = request.answers();
        if (answerItems == null || answerItems.isEmpty()) {
            throw new ResourceConflictException("At least one answer is required.");
        }
        if (answerItems.size() != questions.size()) {
            throw new ResourceConflictException("Assessment requires " + questions.size() + " answered questions.");
        }
        if (answerItems.size() > MAX_ACTIVE_ASSESSMENT_QUESTIONS) {
            throw new ResourceConflictException("Assessment supports at most " + MAX_ACTIVE_ASSESSMENT_QUESTIONS + " questions per attempt.");
        }

        PsychometricAttempt attempt = new PsychometricAttempt();
        attempt.setAssessmentId(assessment.getId());
        attempt.setStudentId(profile.getId());
        attempt.setUserId(user.getId());
        attempt.setSubmissionMode("AUTHENTICATED");
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setSubmittedAt(OffsetDateTime.now());
        attempt.setStatus("COMPLETED");
        PsychometricAttempt savedAttempt = psychometricAttemptRepository.save(attempt);

        List<PsychometricAnswer> answers = answerItems.stream().map(item -> {
            PsychometricQuestion question = questionById.get(item.questionId());
            if (question == null) {
                throw new ResourceConflictException("Answer references a question outside this assessment.");
            }
            int score = item.score();
            if (score < question.getMinScore() || score > question.getMaxScore()) {
                throw new ResourceConflictException("Answer score is out of range for question " + question.getQuestionKey());
            }
            PsychometricAnswer answer = new PsychometricAnswer();
            answer.setAttemptId(savedAttempt.getId());
            answer.setQuestionId(question.getId());
            answer.setDimensionKey(question.getDimensionKey());
            answer.setScore(score);
            return answer;
        }).toList();
        psychometricAnswerRepository.saveAll(answers);

        Map<String, Double> scores = aggregateScoresByQuestion(answerItems, questionById);
        List<String> strengths = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        List<String> growth = scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        String interpretation = buildInterpretation(scores);

        PsychometricResult result = new PsychometricResult();
        result.setAttemptId(savedAttempt.getId());
        result.setStudentId(profile.getId());
        result.setSummaryText(interpretation);
        result.setStrongestAreas(writeValueOrThrow(strengths));
        result.setGrowthAreas(writeValueOrThrow(growth));
        result.setScores(writeValueOrThrow(scores));
        result.setCalculatedAt(OffsetDateTime.now());
        psychometricResultRepository.save(result);

        // Keep legacy submission history for existing recommendation and dashboard integrations.
        List<PsychometricSubmissionRequest.AnswerItem> legacyAnswers = answers.stream()
                .map(answer -> new PsychometricSubmissionRequest.AnswerItem(answer.getDimensionKey(), answer.getScore()))
                .toList();
        saveSubmission(
                "AUTHENTICATED",
                profile.getId(),
                user.getId(),
                null,
                new PsychometricSubmissionRequest(legacyAnswers)
        );

        return new PsychometricAttemptResultDto(
                savedAttempt.getId(),
                assessment.getId(),
                scores,
                strengths,
                growth,
                interpretation,
                savedAttempt.getSubmittedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<PsychometricAttemptResultDto> assessmentAttemptHistory(Principal principal, UUID assessmentId) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found."));
        findAssessmentOrThrow(assessmentId);

        return psychometricAttemptRepository.findByStudentIdAndAssessmentIdOrderBySubmittedAtDesc(profile.getId(), assessmentId).stream()
                .map(attempt -> psychometricResultRepository.findByAttemptId(attempt.getId())
                        .map(result -> new PsychometricAttemptResultDto(
                                attempt.getId(),
                                attempt.getAssessmentId(),
                                parseScoreMap(result.getScores()),
                                parseStringList(result.getStrongestAreas()),
                                parseStringList(result.getGrowthAreas()),
                                result.getSummaryText(),
                                attempt.getSubmittedAt().toString()
                        ))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public PsychometricSubmissionResponse submitForStudent(Principal principal, PsychometricSubmissionRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found."));
        PsychometricSubmission submission = saveSubmission("AUTHENTICATED", profile.getId(), user.getId(), null, request);
        return toResponse(submission);
    }

    @Transactional
    public PsychometricSubmissionResponse submitPublic(String publicSessionId, PsychometricSubmissionRequest request) {
        String resolvedPublicSessionId = (publicSessionId == null || publicSessionId.isBlank())
                ? "PUBLIC-" + UUID.randomUUID()
                : publicSessionId.trim();
        PsychometricSubmission submission = saveSubmission("PUBLIC", null, null, resolvedPublicSessionId, request);
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public PsychometricSubmissionResponse latestForStudent(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found."));
        PsychometricSubmission submission = psychometricSubmissionRepository.findTopByStudentIdOrderByCreatedAtDesc(profile.getId())
                .orElseThrow(() -> new ResourceConflictException("No psychometric submission found yet."));
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<String> findGrowthAreasByStudentProfileId(UUID studentProfileId) {
        return psychometricSubmissionRepository.findTopByStudentIdOrderByCreatedAtDesc(studentProfileId)
                .map(this::scoresFromSubmission)
                .map(scores -> scores.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public boolean hasSubmissionForStudentProfileId(UUID studentProfileId) {
        return psychometricSubmissionRepository.existsByStudentId(studentProfileId);
    }

    private PsychometricAssessment findAssessmentOrThrow(UUID assessmentId) {
        if (assessmentId != null) {
            return psychometricAssessmentRepository.findByIdAndActiveTrue(assessmentId)
                    .orElseThrow(() -> new ResourceConflictException("Psychometric assessment not found."));
        }
        return psychometricAssessmentRepository.findByCodeAndActiveTrue(DEFAULT_ASSESSMENT_CODE)
                .orElseThrow(() -> new ResourceConflictException("No default psychometric assessment is available."));
    }

    private PsychometricSubmission saveSubmission(
            String mode,
            UUID studentId,
            UUID userId,
            String publicSessionId,
            PsychometricSubmissionRequest request
    ) {
        Map<String, Double> scores = aggregateScores(request.answers());
        PsychometricSubmission submission = new PsychometricSubmission();
        submission.setSubmissionMode(mode);
        submission.setStudentId(studentId);
        submission.setUserId(userId);
        submission.setPublicSessionId(publicSessionId);
        submission.setAnswers(writeValueOrThrow(request.answers()));
        submission.setScores(writeValueOrThrow(scores));
        submission.setInterpretation(buildInterpretation(scores));
        return psychometricSubmissionRepository.save(submission);
    }

    private Map<String, Double> aggregateScores(List<PsychometricSubmissionRequest.AnswerItem> answers) {
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (PsychometricSubmissionRequest.AnswerItem answer : answers) {
            grouped.computeIfAbsent(answer.dimension().trim().toLowerCase(), ignored -> new java.util.ArrayList<>()).add(answer.score());
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        grouped.forEach((dimension, values) -> {
            double average = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            scores.put(dimension, Math.round(average * 100.0) / 100.0);
        });
        return scores;
    }

    private Map<String, Double> aggregateScoresByQuestion(
            List<PsychometricAttemptRequest.AnswerItem> answers,
            Map<UUID, PsychometricQuestion> questionById
    ) {
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (PsychometricAttemptRequest.AnswerItem answer : answers) {
            PsychometricQuestion question = questionById.get(answer.questionId());
            if (question == null) {
                continue;
            }
            grouped.computeIfAbsent(question.getDimensionKey().trim().toLowerCase(), ignored -> new java.util.ArrayList<>())
                    .add(answer.score());
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        grouped.forEach((dimension, values) -> {
            double average = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            scores.put(dimension, Math.round(average * 100.0) / 100.0);
        });
        return scores;
    }

    private String buildInterpretation(Map<String, Double> scores) {
        if (scores.isEmpty()) {
            return "Not enough answers to generate insights.";
        }
        List<Map.Entry<String, Double>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();
        String strongest = sorted.get(0).getKey();
        String growth = sorted.get(sorted.size() - 1).getKey();
        return "Strongest area: " + strongest + ". Suggested growth area: " + growth + ".";
    }

    private PsychometricSubmissionResponse toResponse(PsychometricSubmission submission) {
        Map<String, Double> scores = scoresFromSubmission(submission);
        List<String> strengths = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        List<String> growth = scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
        return new PsychometricSubmissionResponse(
                submission.getId(),
                submission.getSubmissionMode(),
                scores,
                strengths,
                growth,
                submission.getInterpretation(),
                submission.getCreatedAt().toString()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> scoresFromSubmission(PsychometricSubmission submission) {
        try {
            Map<String, Object> raw = objectMapper.readValue(submission.getScores(), Map.class);
            Map<String, Double> parsed = new LinkedHashMap<>();
            raw.forEach((key, value) -> parsed.put(String.valueOf(key), Double.parseDouble(String.valueOf(value))));
            return parsed;
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Stored psychometric scores are invalid.");
        }
    }

    private Map<String, Double> parseScoreMap(String rawScores) {
        try {
            Map<String, Object> raw = objectMapper.readValue(rawScores, new TypeReference<Map<String, Object>>() {
            });
            Map<String, Double> parsed = new LinkedHashMap<>();
            raw.forEach((key, value) -> parsed.put(String.valueOf(key), Double.parseDouble(String.valueOf(value))));
            return parsed;
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Stored psychometric result scores are invalid.");
        }
    }

    private List<String> parseStringList(String rawList) {
        try {
            return objectMapper.readValue(rawList, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Stored psychometric result list is invalid.");
        }
    }

    private String writeValueOrThrow(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Unable to store psychometric result.");
        }
    }
}

