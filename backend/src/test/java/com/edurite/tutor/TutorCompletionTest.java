package com.edurite.tutor;

import com.edurite.tutor.service.TutorService;
import com.edurite.tutor.repository.*;
import com.edurite.tutor.entity.*;
import com.edurite.tutor.dto.TutorDtos.*;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import com.edurite.ai.service.AiProviderOrchestratorService;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TutorCompletionTest {
    final StudentContextService context=mock(StudentContextService.class);
    final TutorSessionRepository sessions=mock(TutorSessionRepository.class);
    final TutorMessageRepository messages=mock(TutorMessageRepository.class);
    final AiProviderOrchestratorService ai=mock(AiProviderOrchestratorService.class);
    final TutorService service=new TutorService(context,sessions,messages,ai);
    final java.security.Principal principal=()->"alice";
    final StudentProfile student=new StudentProfile();
    final TutorSession session=new TutorSession();
    @BeforeEach void setup() {
        student.setId(UUID.randomUUID()); student.setSelectedGrade("Grade 10"); student.setSubjectAchievementsJson("[]");
        session.setId(UUID.randomUUID()); session.setSubject("MATHEMATICS"); session.setTitle("Algebra");
        when(context.requireStudent(principal)).thenReturn(student);
        when(sessions.findByIdAndStudentId(session.getId(),student.getId())).thenReturn(Optional.of(session));
        when(messages.save(any())).thenAnswer(i->i.getArgument(0));
    }
    @Test void longWorkedSolutionAndFormulasAreNotClippedOrStripped() {
        String answer="\\frac{1}{2} + 3 = 3.5\n"+"Detailed worked step. ".repeat(1000)+"Final answer: 3.5";
        when(ai.generateContent(any())).thenReturn(answer);
        assertThat(service.ask(principal,new TutorAskRequest(session.getId(),"MATHEMATICS","Explain the calculation")).answer()).isEqualTo(answer);
        verify(ai).generateContent(argThat(p->p.contains("Grade 10") && p.contains("Complete the requested explanation")));
    }
    @Test void providerFailureIsVisibleAndNoFakeTutorReplyIsSaved() {
        when(ai.generateContent(any())).thenThrow(new IllegalStateException("Provider unavailable"));
        assertThatThrownBy(()->service.ask(principal,new TutorAskRequest(session.getId(),"MATHEMATICS","Help"))).hasMessage("Provider unavailable");
        verify(messages,never()).save(argThat(message->"TUTOR".equals(message.getSender())));
    }
    @Test void followupUsesEarlierConversation() {
        var earlier=new TutorMessage(); earlier.setSender("TUTOR"); earlier.setMessage("Factor x squared minus four.");
        when(messages.findBySessionIdOrderByCreatedAtAsc(session.getId())).thenReturn(List.of(earlier));
        when(ai.generateContent(any())).thenReturn("Use the difference of squares.");
        service.ask(principal,new TutorAskRequest(session.getId(),"MATHEMATICS","Why?"));
        verify(ai).generateContent(argThat(p->p.contains("Factor x squared minus four.") && p.contains("Student: Why?")));
    }
    @Test void anotherLearnerCannotOpenTheSession() {
        assertThatThrownBy(()->service.getSession(principal,UUID.randomUUID())).hasMessage("Tutor session not found");
    }
}
