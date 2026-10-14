package com.edurite.student;

import com.edurite.student.service.*;
import com.edurite.student.dto.StudentProfileAssociationDto.Type;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.course.repository.CourseRepository;
import com.edurite.course.entity.Course;
import com.edurite.career.repository.CareerRepository;
import com.edurite.career.entity.Career;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentProfileAssociationServiceTest {
    @Test void persistsBothKindsAcrossServiceReloadWithoutLeakingToAnotherLearner() throws Exception {
        var profiles = mock(StudentProfileRepository.class);
        var context = mock(StudentContextService.class);
        var current = mock(CurrentUserService.class);
        var courses = mock(CourseRepository.class);
        var careers = mock(CareerRepository.class);
        var mapper = new ObjectMapper();
        Principal alice = () -> "alice", bob = () -> "bob";
        var stored = new HashMap<UUID, String>();
        for (var principal : List.of(alice, bob)) {
            var user = new User(); user.setId(UUID.randomUUID());
            stored.put(user.getId(), "{\"existingPreference\":true}");
            when(current.requireUser(principal)).thenReturn(user);
            when(context.requireStudent(principal)).thenAnswer(i -> {
                var profile = new StudentProfile(); profile.setUserId(user.getId());
                profile.setPreferencesJson(stored.get(user.getId())); return profile;
            });
            when(profiles.findByUserIdForUpdate(user.getId())).thenAnswer(i -> Optional.of(context.requireStudent(principal)));
        }
        when(profiles.save(any())).thenAnswer(i -> {
            StudentProfile profile = i.getArgument(0);
            stored.put(profile.getUserId(), profile.getPreferencesJson()); return profile;
        });
        var courseId = UUID.randomUUID(); var careerId = UUID.randomUUID();
        var course = new Course(); course.setName("Computer Science");
        var career = new Career(); career.setTitle("Software Engineer");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(careers.findById(careerId)).thenReturn(Optional.of(career));
        var service = new StudentProfileAssociationService(context, current, profiles, courses, careers, mapper);
        service.link(alice, Type.COURSE, courseId);
        service.link(alice, Type.CAREER, careerId);
        service.link(alice, Type.COURSE, courseId);
        var reloaded = new StudentProfileAssociationService(context, current, profiles, courses, careers, mapper);
        assertThat(reloaded.list(alice)).hasSize(2).extracting(item -> item.title()).containsExactly("Computer Science", "Software Engineer");
        assertThat(reloaded.list(bob)).isEmpty();
        assertThat(mapper.readTree(stored.get(current.requireUser(alice).getId())).get("existingPreference").asBoolean()).isTrue();
        reloaded.unlink(alice, Type.COURSE, courseId);
        assertThat(service.list(alice)).hasSize(1).allMatch(item -> item.type() == Type.CAREER);
        assertThatThrownBy(() -> service.link(alice, Type.COURSE, UUID.randomUUID()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(service.list(alice)).hasSize(1);
        var programmes = mock(com.edurite.roadmap.repository.CareerProgramRequirementRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "programmes", programmes);
        var programme = new com.edurite.roadmap.entity.CareerProgramRequirement();
        programme.setCareerName("Software Engineer"); programme.setInstitutionName("Test institution"); programme.setQualificationName("Test programme");
        UUID programmeId = UUID.randomUUID();
        when(programmes.findById(programmeId)).thenReturn(Optional.of(programme));
        service.link(alice, Type.PROGRAMME, programmeId);
        service.link(alice, Type.PROGRAMME, programmeId);
        assertThat(reloaded.list(alice)).hasSize(2).anySatisfy(link -> {
            assertThat(link.type()).isEqualTo(Type.PROGRAMME);
            assertThat(link.careerName()).isEqualTo("Software Engineer");
        });
        assertThat(reloaded.list(bob)).isEmpty();
    }
}
