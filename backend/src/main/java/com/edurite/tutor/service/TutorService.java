package com.edurite.tutor.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import com.edurite.tutor.dto.TutorDtos.TutorAskRequest;
import com.edurite.tutor.dto.TutorDtos.TutorAskResponse;
import com.edurite.tutor.dto.TutorDtos.TutorMessageResponse;
import com.edurite.tutor.dto.TutorDtos.TutorSessionRequest;
import com.edurite.tutor.dto.TutorDtos.TutorSessionResponse;
import com.edurite.tutor.entity.TutorMessage;
import com.edurite.tutor.entity.TutorSession;
import com.edurite.tutor.repository.TutorMessageRepository;
import com.edurite.tutor.repository.TutorSessionRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private static final Set<String> SUBJECTS = Set.of(
            "MATHEMATICS", "ENGLISH", "SCIENCE", "BIOLOGY", "CHEMISTRY", "PHYSICS",
            "ACCOUNTING", "COMPUTER_STUDIES", "GENERAL_STUDY_HELP"
    );

    private final StudentContextService studentContextService;
    private final TutorSessionRepository sessionRepository;
    private final TutorMessageRepository messageRepository;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;

    public TutorService(
            StudentContextService studentContextService,
            TutorSessionRepository sessionRepository,
            TutorMessageRepository messageRepository,
            AiProviderOrchestratorService aiProviderOrchestratorService
    ) {
        this.studentContextService = studentContextService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
    }

    @Transactional(readOnly = true)
    public List<TutorSessionResponse> listSessions(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        return sessionRepository.findByStudentIdOrderByUpdatedAtDesc(profile.getId()).stream()
                .map(session -> toSessionResponse(session, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TutorSessionResponse getSession(Principal principal, UUID sessionId) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        TutorSession session = sessionRepository.findByIdAndStudentId(sessionId, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("Tutor session not found"));
        return toSessionResponse(session, true);
    }

    @Transactional
    public TutorSessionResponse createSession(Principal principal, TutorSessionRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        TutorSession session = new TutorSession();
        session.setStudentId(profile.getId());
        session.setSubject(normalizeSubject(request.subject()));
        session.setTitle(clean(request.title()).isBlank() ? displaySubject(session.getSubject()) + " session" : clean(request.title()));
        session.setLastMessageAt(OffsetDateTime.now());
        return toSessionResponse(sessionRepository.save(session), true);
    }

    @Transactional
    public TutorAskResponse ask(Principal principal, TutorAskRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        String subject = normalizeSubject(request.subject());
        TutorSession session = request.sessionId() == null
                ? newSession(profile, subject, request.question())
                : sessionRepository.findByIdAndStudentId(request.sessionId(), profile.getId())
                        .orElseThrow(() -> new ResourceConflictException("Tutor session not found"));
        if (!session.getSubject().equals(subject)) {
            session.setSubject(subject);
        }

        TutorMessage studentMessage = new TutorMessage();
        studentMessage.setSessionId(session.getId());
        studentMessage.setSender("STUDENT");
        studentMessage.setMessage(clean(request.question()));
        messageRepository.save(studentMessage);

        String answer = answer(profile, subject, request.question());
        TutorMessage tutorMessage = new TutorMessage();
        tutorMessage.setSessionId(session.getId());
        tutorMessage.setSender("TUTOR");
        tutorMessage.setMessage(answer);
        messageRepository.save(tutorMessage);

        session.setLastMessageAt(OffsetDateTime.now());
        sessionRepository.save(session);
        return new TutorAskResponse(session.getId(), session.getSubject(), answer, messages(session.getId()));
    }

    private TutorSession newSession(StudentProfile profile, String subject, String question) {
        TutorSession session = new TutorSession();
        session.setStudentId(profile.getId());
        session.setSubject(subject);
        session.setTitle(shortTitle(question));
        session.setLastMessageAt(OffsetDateTime.now());
        return sessionRepository.save(session);
    }

    private String answer(StudentProfile profile, String subject, String question) {
        String fallback = """
                Let's work through this step by step.
                1. Identify what the question is asking.
                2. Write down the known information.
                3. Choose the formula, rule, or concept that applies.
                4. Try one small step and check your answer.

                For %s, focus on the core concept first, then practise a similar example. If you share your working, I can help you find the next step.
                """.formatted(displaySubject(subject));
        String prompt = """
                You are an EduRite academic tutor. Help a student with a %s question.
                Student level: %s
                Question: %s
                Give a safe, clear explanation with steps. Do not do harmful or dishonest work.
                """.formatted(displaySubject(subject), valueOr(profile.getQualificationLevel(), "not provided"), question);
        try {
            return aiProviderOrchestratorService.generateContent(prompt);
        } catch (RuntimeException ex) {
            return fallback.trim();
        }
    }

    private TutorSessionResponse toSessionResponse(TutorSession session, boolean includeMessages) {
        return new TutorSessionResponse(
                session.getId(),
                session.getSubject(),
                session.getTitle(),
                session.getLastMessageAt(),
                includeMessages ? messages(session.getId()) : List.of()
        );
    }

    private List<TutorMessageResponse> messages(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(message -> new TutorMessageResponse(message.getId(), message.getSender(), message.getMessage(), message.getCreatedAt()))
                .toList();
    }

    private String normalizeSubject(String value) {
        String normalized = clean(value).toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!SUBJECTS.contains(normalized)) {
            throw new ResourceConflictException("Unsupported tutor subject: " + value);
        }
        return normalized;
    }

    private String displaySubject(String subject) {
        return subject.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String shortTitle(String question) {
        String cleaned = clean(question);
        if (cleaned.length() <= 60) {
            return cleaned;
        }
        return cleaned.substring(0, 57) + "...";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

