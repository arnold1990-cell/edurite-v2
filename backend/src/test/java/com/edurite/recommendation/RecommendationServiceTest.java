package com.edurite.recommendation;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.psychometric.service.PsychometricService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.course.repository.CourseRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
class RecommendationServiceTest {
 @Test void interestsChangeTopMatchesAndUseRealIds() {
  var students=mock(StudentService.class); var access=mock(StudentPlanAccessService.class); var psycho=mock(PsychometricService.class);
  var careers=mock(CareerRepository.class); var courses=mock(CourseRepository.class);
  var entity=new StudentProfile(); entity.setId(UUID.randomUUID()); entity.setUserId(UUID.randomUUID());
  java.security.Principal principal=()->"student@example.com";
  when(students.getProfileEntity(principal)).thenReturn(entity);
  when(access.resolveByUserId(entity.getUserId())).thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_PREMIUM","ACTIVE",true,null,null));
  Career nurse=career("Nurse","health helping communication"), developer=career("Software Developer","technology software analytical");
  when(careers.findAll()).thenReturn(List.of(nurse,developer));
  var service=new RecommendationService(students,access,psycho,careers,courses,mock(com.edurite.student.repository.StudentPreferenceRepository.class),new com.fasterxml.jackson.databind.ObjectMapper());
  when(students.getProfile(principal)).thenReturn(profile(List.of("technology")));
  var first=service.generateForStudent(principal); assertThat(first.suggestedCareers().getFirst().id()).isEqualTo(developer.getId().toString());
  when(students.getProfile(principal)).thenReturn(profile(List.of("health")));
  var second=service.generateForStudent(principal); assertThat(second.suggestedCareers().getFirst().id()).isEqualTo(nurse.getId().toString());
  assertThat(second.suggestedCareers()).hasSize(1); assertThat(second.suggestedCoursesOrImprovements()).isEmpty();
  assertThat(second.suggestedBursaries()).isEmpty();
 }
 @Test void premiumReturnsEverySupportedCareerBeyondTwelveButBasicLimitRemainsExplicit() {
  var students=mock(StudentService.class);var access=mock(StudentPlanAccessService.class);
  var psycho=mock(PsychometricService.class);var careers=mock(CareerRepository.class);var courses=mock(CourseRepository.class);
  var entity=new StudentProfile();entity.setId(UUID.randomUUID());entity.setUserId(UUID.randomUUID());
  java.security.Principal principal=()->"student@example.com";
  when(students.getProfileEntity(principal)).thenReturn(entity);
  when(students.getProfile(principal)).thenReturn(profile(List.of("technology")));
  when(careers.findAll()).thenReturn(java.util.stream.IntStream.range(0,18).mapToObj(i->career("Technology career "+i,"technology analytical work")).toList());
  var service=new RecommendationService(students,access,psycho,careers,courses,mock(com.edurite.student.repository.StudentPreferenceRepository.class),new com.fasterxml.jackson.databind.ObjectMapper());
  when(access.resolveByUserId(entity.getUserId())).thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_PREMIUM","ACTIVE",true,null,null));
  assertThat(service.generateForStudent(principal).suggestedCareers()).hasSize(18);
  when(access.resolveByUserId(entity.getUserId())).thenReturn(new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC","ACTIVE",false,3,"Upgrade for more catalogue matches"));
  var basic=service.generateForStudent(principal);
  assertThat(basic.suggestedCareers()).hasSize(3);assertThat(basic.careerSuggestionsLimited()).isTrue();
 }
 private Career career(String name,String description) { var c=new Career();c.setId(UUID.randomUUID());c.setTitle(name);c.setDescription(description);return c; }
 private StudentProfileDto profile(List<String> interests) { return new StudentProfileDto(UUID.randomUUID(),"Test","Student","student@example.com",null,null,null,null,null,null,"Grade 12",List.of(),List.of(),List.of(),List.of(),interests,null,null,null,false,20,null); }
}
