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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private static final Set<String> SUBJECTS = Set.of(
            "MATHEMATICS", "ENGLISH", "SCIENCE", "BIOLOGY", "CHEMISTRY", "PHYSICS",
            "ACCOUNTING", "COMPUTER_STUDIES", "GENERAL_STUDY_HELP"
    );
    private static final int MAX_CONTEXT_MESSAGES = 16;
    private static final Pattern FENCED_CODE_PATTERN = Pattern.compile("```+", Pattern.MULTILINE);
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^#{1,6}\\s*");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("(?m)^\\s*([-*_]\\s*){3,}$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern UNDERSCORE_EMPHASIS_PATTERN = Pattern.compile("__([^_]+)__");
    private static final Pattern ESCAPED_MARKDOWN_PATTERN = Pattern.compile("\\\\([*_`#\\-])");
    private static final Pattern BULLET_PATTERN = Pattern.compile("(?m)^\\s*[-*]\\s+");
    private static final Pattern EXTRA_BLANK_LINES_PATTERN = Pattern.compile("\\n{3,}");

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
        String cleanedQuestion = clean(request.question());
        TutorSession session = resolveSession(profile, subject, cleanedQuestion, request.sessionId());

        List<TutorMessage> existingMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        TutorMessage studentMessage = new TutorMessage();
        studentMessage.setSessionId(session.getId());
        studentMessage.setSender("STUDENT");
        studentMessage.setMessage(cleanedQuestion);
        TutorMessage savedStudentMessage = messageRepository.save(studentMessage);

        List<TutorMessage> conversation = new ArrayList<>(existingMessages);
        conversation.add(savedStudentMessage);
        String answer = answer(profile, session, conversation);

        TutorMessage tutorMessage = new TutorMessage();
        tutorMessage.setSessionId(session.getId());
        tutorMessage.setSender("TUTOR");
        tutorMessage.setMessage(answer);
        messageRepository.save(tutorMessage);

        if (existingMessages.isEmpty() && isDefaultSessionTitle(session)) {
            session.setTitle(shortTitle(cleanedQuestion));
        }
        session.setLastMessageAt(OffsetDateTime.now());
        sessionRepository.save(session);
        return new TutorAskResponse(session.getId(), session.getSubject(), answer, messages(session.getId()));
    }

    private TutorSession resolveSession(StudentProfile profile, String subject, String question, UUID sessionId) {
        TutorSession session = sessionId == null
                ? newSession(profile, subject, question)
                : sessionRepository.findByIdAndStudentId(sessionId, profile.getId())
                        .orElseThrow(() -> new ResourceConflictException("Tutor session not found"));
        if (!session.getSubject().equals(subject)) {
            session.setSubject(subject);
        }
        return session;
    }

    private TutorSession newSession(StudentProfile profile, String subject, String question) {
        TutorSession session = new TutorSession();
        session.setStudentId(profile.getId());
        session.setSubject(subject);
        session.setTitle(shortTitle(question));
        session.setLastMessageAt(OffsetDateTime.now());
        return sessionRepository.save(session);
    }

    private String answer(StudentProfile profile, TutorSession session, List<TutorMessage> conversation) {
        String fallback = sanitizeTutorResponse("""
                Let's work through this step by step.

                Start by telling me what part feels confusing, and I will explain it in simpler words.
                I can also give another example, show the next step, or make the question a little harder once you understand this part.

                For %s, focus on one step at a time and share your working if you want more help.
                """.formatted(displaySubject(session.getSubject())));
        String prompt = buildPrompt(profile, session.getSubject(), conversation);
        try {
            String generated = aiProviderOrchestratorService.generateContent(prompt);
            String cleaned = sanitizeTutorResponse(generated);
            return cleaned.isBlank() ? fallback : cleaned;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String buildPrompt(StudentProfile profile, String subject, List<TutorMessage> conversation) {
        List<TutorMessage> recentMessages = conversation.size() > MAX_CONTEXT_MESSAGES
                ? conversation.subList(conversation.size() - MAX_CONTEXT_MESSAGES, conversation.size())
                : conversation;
        String transcript = recentMessages.stream()
                .map(message -> ("STUDENT".equalsIgnoreCase(message.getSender()) ? "Student" : "Tutor") + ": " + clean(message.getMessage()))
                .collect(Collectors.joining("\n"));
        return """
                You are EduRite's academic tutor in an ongoing conversation with a learner.
                Subject: %s
                Student level: %s

                Follow these rules:
                - Continue naturally from the earlier conversation.
                - Answer follow-up questions like "Why?", "Explain further", "Give another example", "Continue", or "Simplify it" using the previous messages.
                - Use plain, student-friendly language.
                - Give step-by-step explanations when useful.
                - If the learner is confused, simplify and give one clear example.
                - Do not use markdown, headings, bold markers, code fences, backticks, raw hash symbols, horizontal rules, or technical formatting.
                - Do not wrap the answer in quotation marks.
                - Keep the answer conversational and helpful.

                Conversation so far:
                %s

                Respond to the student's latest message as the next turn in the same conversation.
                """.formatted(displaySubject(subject), profile.getQualificationLevel() == null || profile.getQualificationLevel().isBlank() ? "not provided" : profile.getQualificationLevel().trim(), transcript);
    }

    private String sanitizeTutorResponse(String raw) {
        String cleaned = raw == null ? "" : raw.replace("\r", "").trim();
        cleaned = FENCED_CODE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = INLINE_CODE_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = HEADING_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HORIZONTAL_RULE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = BOLD_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = UNDERSCORE_EMPHASIS_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = cleaned.replace("**", "").replace("__", "");
        cleaned = ESCAPED_MARKDOWN_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = BULLET_PATTERN.matcher(cleaned).replaceAll("-  ");
        cleaned = cleaned.replace("###", "").replace("##", "").replace("#", "");
        cleaned = cleaned.replace("```", "").replace("`", "").replace("\\", "");
        cleaned = cleaned.replace("---", "");
        cleaned = EXTRA_BLANK_LINES_PATTERN.matcher(cleaned).replaceAll("\n\n");
        cleaned = trimWrappingQuotes(cleaned);
        return cleaned.trim();
    }

    private String trimWrappingQuotes(String value) {
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
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
                .map(message -> new TutorMessageResponse(message.getId(), message.getSender(), sanitizeTutorResponse(message.getMessage()), message.getCreatedAt()))
                .toList();
    }

    private boolean isDefaultSessionTitle(TutorSession session) {
        return clean(session.getTitle()).equalsIgnoreCase(displaySubject(session.getSubject()) + " session");
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
}




