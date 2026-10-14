package com.edurite.roadmap;

import com.edurite.roadmap.service.*;
import com.edurite.roadmap.dto.CareerRoadmapDtos.*;
import com.edurite.roadmap.entity.SavedCareerRoadmap;
import com.edurite.roadmap.repository.SavedCareerRoadmapRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentContextService;
import com.edurite.ai.service.AiProviderOrchestratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.DriverManager;
import java.util.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL JSONB round trips across separate Hibernate sessions; no application accounts are changed. */
@EnabledIfEnvironmentVariable(named="EDURITE_TRIAL_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:.*")
class StudyPlanPersistenceIntegrationTest {
    @Test void generatedPlanAndProgressSurviveReloadAndRemainLearnerOwned() throws Exception {
        String schema="study_plan_test_"+UUID.randomUUID().toString().replace("-", "");
        String url=System.getenv("EDURITE_TRIAL_TEST_DB_URL"), user=System.getenv("EDURITE_TRIAL_TEST_DB_USER"), password=System.getenv("EDURITE_TRIAL_TEST_DB_PASSWORD");
        try(var connection=DriverManager.getConnection(url,user,password); var statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try(var sessions=new Configuration().addAnnotatedClass(SavedCareerRoadmap.class)
                    .setProperty("hibernate.connection.url",url).setProperty("hibernate.connection.username",user).setProperty("hibernate.connection.password",password)
                    .setProperty("hibernate.default_schema",schema).setProperty("hibernate.hbm2ddl.auto","create-drop")
                    .setProperty("hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy").buildSessionFactory()) {
                var store=mock(SavedCareerRoadmapRepository.class);
                when(store.saveAndFlush(any())).thenAnswer(i->save(sessions,i.getArgument(0)));
                when(store.save(any())).thenAnswer(i->save(sessions,i.getArgument(0)));
                when(store.findByLearnerIdAndRequestKey(any(),any())).thenAnswer(i->{try(var db=sessions.openSession()) {return db.createSelectionQuery("from SavedCareerRoadmap where learnerId=:learner and requestKey=:key",SavedCareerRoadmap.class).setParameter("learner",i.getArgument(0)).setParameter("key",i.getArgument(1)).uniqueResultOptional();}});
                when(store.findByIdAndLearnerId(any(),any())).thenAnswer(i->{try(var db=sessions.openSession()) {return db.createSelectionQuery("from SavedCareerRoadmap where id=:id and learnerId=:learner",SavedCareerRoadmap.class).setParameter("id",i.getArgument(0)).setParameter("learner",i.getArgument(1)).uniqueResultOptional();}});
                when(store.findByLearnerIdAndStudyPlanTrueOrderByCreatedAtDesc(any())).thenAnswer(i->{try(var db=sessions.openSession()) {return db.createSelectionQuery("from SavedCareerRoadmap where learnerId=:learner and studyPlan=true order by createdAt desc",SavedCareerRoadmap.class).setParameter("learner",i.getArgument(0)).list();}});
                var context=mock(StudentContextService.class); var profiles=mock(StudentProfileRepository.class);
                var roadmaps=mock(CareerRoadmapService.class); var ai=mock(AiProviderOrchestratorService.class);
                var alice=new StudentProfile(); alice.setId(UUID.randomUUID()); alice.setUserId(UUID.randomUUID());
                var bob=new StudentProfile(); bob.setId(UUID.randomUUID());
                java.security.Principal a=()->"alice", b=()->"bob";
                when(context.requireStudent(a)).thenReturn(alice); when(context.requireStudent(b)).thenReturn(bob);
                when(profiles.findByUserIdForUpdate(alice.getUserId())).thenReturn(Optional.of(alice));
                when(roadmaps.generate(eq(a),any())).thenReturn(new CareerRoadmapGenerateResponse("Nurse",null,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),new ApsReadiness(null,null,null,0,"Unavailable",0),null,List.of(),List.of()));
                String step="{\"title\":\"Revision\",\"focus\":\"Diagnostic\",\"actions\":[\"Monday 20 minutes\",\"Try 5 questions\",\"Review errors\"]}";
                when(ai.generateContent(any())).thenReturn("["+String.join(",",Collections.nCopies(4,step))+"]");
                var service=new StudyPlanService(store,context,profiles,roadmaps,ai,new ObjectMapper().findAndRegisterModules());
                UUID key=UUID.randomUUID(); var plan=service.generate(a,new StudyPlanService.Generate(key,"Nurse",null));
                service.progress(a,plan.id(),new StudyPlanService.Progress(Set.of(1)));
                assertThat(service.history(a)).singleElement().satisfies(reloaded->{assertThat(reloaded.id()).isEqualTo(plan.id()); assertThat(reloaded.completedSteps()).containsExactly(1); assertThat(reloaded.steps()).hasSize(4);});
                assertThat(service.history(b)).isEmpty();
                assertThatThrownBy(()->service.progress(b,plan.id(),new StudyPlanService.Progress(Set.of(0)))).hasMessage("Study plan not found");
                assertThat(service.generate(a,new StudyPlanService.Generate(key,"Nurse",null)).id()).isEqualTo(plan.id());
                verify(ai,times(1)).generateContent(any());
                service.generate(a,new StudyPlanService.Generate(UUID.randomUUID(),"Nurse",null));
                assertThat(service.history(a)).hasSize(2);
            } finally {statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
    private SavedCareerRoadmap save(SessionFactory sessions,SavedCareerRoadmap entity) {
        try(var db=sessions.openSession()) {var tx=db.beginTransaction(); var saved=db.merge(entity); tx.commit(); return saved;}
    }
}
