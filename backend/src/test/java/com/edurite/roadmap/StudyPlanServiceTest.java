package com.edurite.roadmap;

import com.edurite.roadmap.service.*;
import com.edurite.roadmap.dto.CareerRoadmapDtos.*;
import com.edurite.roadmap.entity.SavedCareerRoadmap;
import com.edurite.roadmap.repository.SavedCareerRoadmapRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.ai.service.AiProviderOrchestratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudyPlanServiceTest {
    final SavedCareerRoadmapRepository store=mock(SavedCareerRoadmapRepository.class);
    final StudentContextService context=mock(StudentContextService.class);
    final StudentProfileRepository profiles=mock(StudentProfileRepository.class);
    final CareerRoadmapService roadmaps=mock(CareerRoadmapService.class);
    final AiProviderOrchestratorService ai=mock(AiProviderOrchestratorService.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final StudyPlanService service=new StudyPlanService(store,context,profiles,roadmaps,ai,mapper);
    final java.security.Principal principal=()->"alice";
    final StudentProfile student=new StudentProfile();
    final UUID key=UUID.randomUUID();
    @BeforeEach void setup() { student.setId(UUID.randomUUID()); student.setUserId(UUID.randomUUID()); student.setSelectedGrade("Grade 10"); student.setSubjectAchievementsJson("[{\"subjectName\":\"Maths\",\"markPercentage\":48}]"); when(context.requireStudent(principal)).thenReturn(student); when(profiles.findByUserIdForUpdate(student.getUserId())).thenReturn(Optional.of(student)); }
    CareerRoadmapGenerateResponse roadmap() { return new CareerRoadmapGenerateResponse("Nurse",null,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),new ApsReadiness(null,null,null,0,"Unavailable",0),null,List.of(),List.of()); }
    @Test void retryReturnsExistingPlanWithoutCallingAiOrSavingAgain() throws Exception {
        var saved=new SavedCareerRoadmap(); saved.setId(UUID.randomUUID()); saved.setRoadmapJson(mapper.writeValueAsString(roadmap()));
        when(store.findByLearnerIdAndRequestKey(student.getId(),key)).thenReturn(Optional.of(saved));
        assertThat(service.generate(principal,new StudyPlanService.Generate(key,"Nurse",null)).id()).isEqualTo(saved.getId());
        verifyNoInteractions(ai,roadmaps); verify(store,never()).saveAndFlush(any());
    }
    @Test void otherLearnersCannotUpdateAPlan() {
        assertThatThrownBy(()->service.progress(principal,UUID.randomUUID(),new StudyPlanService.Progress(Set.of(0)))).hasMessage("Study plan not found");
        verify(store,never()).save(any());
    }
    @Test void invalidAiOutputNeverPersistsOrDeletesHistory() {
        when(roadmaps.generate(eq(principal),any())).thenReturn(roadmap()); when(ai.generateContent(any())).thenReturn("[]");
        assertThatThrownBy(()->service.generate(principal,new StudyPlanService.Generate(key,"Nurse",null))).hasMessageContaining("incomplete");
        verify(store,never()).saveAndFlush(any()); verify(store,never()).delete(any());
    }
    @Test void generationUsesPersistedContextAndAppendsHistory() {
        student.setCareerGoals("Improve algebra for nursing");
        student.setPreferencesJson("{\"availableDays\":[\"Saturday\"]}");
        when(roadmaps.generate(eq(principal),any())).thenReturn(roadmap());
        String step="{\"title\":\"Weekly revision\",\"focus\":\"Maths practice\",\"actions\":[\"Monday 25 minutes\",\"Practice 5 questions\",\"Review mistakes\"]}";
        when(ai.generateContent(any())).thenReturn("["+String.join(",",Collections.nCopies(4,step))+"]");
        when(store.saveAndFlush(any())).thenAnswer(i->{ SavedCareerRoadmap saved=i.getArgument(0); saved.setId(UUID.randomUUID()); return saved; });
        assertThat(service.generate(principal,new StudyPlanService.Generate(key,"Nurse",null)).steps()).hasSize(4);
        verify(ai).generateContent(argThat(prompt->prompt.contains("Grade 10") && prompt.contains("48") && prompt.contains("Improve algebra for nursing") && prompt.contains("Saturday")));
        verify(store).saveAndFlush(argThat(saved->saved.getLearnerId().equals(student.getId()) && saved.getRequestKey().equals(key) && saved.isStudyPlan()));
        verify(store,never()).delete(any());
    }
    @Test void blankFocusDoesNotBecomeASuccessfulPlan() {
        when(roadmaps.generate(eq(principal),any())).thenReturn(roadmap());
        String step="{\"title\":\"Revision\",\"focus\":\"  \",\"actions\":[\"Read\",\"Practice\",\"Review\"]}";
        when(ai.generateContent(any())).thenReturn("["+String.join(",",Collections.nCopies(4,step))+"]");
        assertThatThrownBy(()->service.generate(principal,new StudyPlanService.Generate(key,"Nurse",null))).hasMessageContaining("incomplete");
        verify(store,never()).saveAndFlush(any());
    }
}
