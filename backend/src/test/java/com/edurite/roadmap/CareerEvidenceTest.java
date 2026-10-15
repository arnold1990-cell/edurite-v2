package com.edurite.roadmap;

import com.edurite.roadmap.service.*;
import com.edurite.roadmap.dto.CareerRoadmapDtos.*;
import com.edurite.roadmap.repository.*;
import com.edurite.career.repository.CareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentContextService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.career.entity.Career;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CareerEvidenceTest {
    @Test void emptyLegacyPreferencesAllowClearingProgrammeWithoutServerError() {
        for (String stored : Arrays.asList(null, "", "null", "{}")) {
            var profile = new StudentProfile(); profile.setPreferencesJson(stored);
            var context = mock(StudentContextService.class);
            var profiles = mock(StudentProfileRepository.class);
            java.security.Principal learner = () -> "synthetic@example.test";
            when(context.requireStudent(learner)).thenReturn(profile);
            var service = new CareerSelectionService(mock(CareerRepository.class), context, profiles, new ObjectMapper());
            assertThat(service.programme(learner, null).get("programmeId")).isEmpty();
            verify(profiles).save(profile);
        }
    }
    @Test void percentageThresholdIsNotRoundedDownToAnAchievementLevel() {
        var requirements=mock(CareerProgramRequirementRepository.class);
        var record=new com.edurite.roadmap.entity.CareerProgramRequirement(); record.setVerified(true); record.setApsRequired(24); record.setApsRule("NSC_ALL_EXCL_LO"); record.setMathematicsRequirement("55%");
        var service=new CareerRoadmapService(mock(CareerRoadmapRepository.class),mock(SavedCareerRoadmapRepository.class),requirements,mock(StudentProfileRepository.class),mock(com.edurite.institution.repository.InstitutionRepository.class),mock(com.edurite.security.service.CurrentUserService.class),new ObjectMapper(),mock(AiCareerRoadmapService.class));
        List<ApsSubjectResult> marks=new ArrayList<>(); marks.add(new ApsSubjectResult("Mathematics",52,4,4,true,null));
        for(int i=0;i<5;i++) marks.add(new ApsSubjectResult("Subject "+i,60,5,5,true,null));
        UniversityRequirementResponse result=org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"toRequirementResponse",record,29,marks);
        assertThat(result.requirementStatus()).isEqualTo("Not Yet Eligible");
        assertThat(result.apsGap()).isZero();
    }
    @Test void noRequirementRecordsMeansNoInventedInstitutionOrThreshold() {
        var requirements=mock(CareerProgramRequirementRepository.class);
        when(requirements.findByCareerNameContainingIgnoreCaseOrderByVerifiedDescApsRequiredAscInstitutionNameAsc("Nurse")).thenReturn(List.of());
        var institutions=mock(com.edurite.institution.repository.InstitutionRepository.class);
        var service=new CareerRoadmapService(mock(CareerRoadmapRepository.class),mock(SavedCareerRoadmapRepository.class),requirements,mock(StudentProfileRepository.class),institutions,mock(com.edurite.security.service.CurrentUserService.class),new ObjectMapper(),mock(AiCareerRoadmapService.class));
        assertThat(service.requirements("Nurse")).isEmpty();
        verifyNoInteractions(institutions);
    }
    @Test void scoringRequiresAnExplicitSupportedInstitutionRule() {
        var subjects=new ArrayList<ApsSubjectResult>();
        for(int i=0;i<7;i++) subjects.add(new ApsSubjectResult("Subject "+i,60,5,5,true,null));
        subjects.add(new ApsSubjectResult("Life Orientation",80,7,7,false,null));
        assertThat(CareerRoadmapService.programmeAps(null,subjects)).isNull();
        assertThat(CareerRoadmapService.programmeAps("UNKNOWN",subjects)).isNull();
        assertThat(CareerRoadmapService.programmeAps("NSC_BEST_6_EXCL_LO",subjects)).isEqualTo(30);
        assertThat(CareerRoadmapService.programmeAps("NSC_ALL_INCL_LO",subjects)).isEqualTo(42);
        assertThat(CareerRoadmapService.programmeAps("NSC_ALL_EXCL_LO",subjects.subList(0,2))).isNull();
    }
    @Test void selectionUsesCatalogueAndPreservesOtherPreferences() {
        var careers=mock(CareerRepository.class); var context=mock(StudentContextService.class); var profiles=mock(StudentProfileRepository.class);
        var profile=new StudentProfile(); profile.setPreferencesJson("{\"profileAssociations\":[]}");
        java.security.Principal principal=()->"learner";
        when(context.requireStudent(principal)).thenReturn(profile);
        var career=new Career(); career.setTitle("Nurse");
        when(careers.findFirstByTitleIgnoreCase("Nurse")).thenReturn(Optional.of(career));
        var service=new CareerSelectionService(careers,context,profiles,new ObjectMapper());
        assertThat(service.selected(principal," Nurse ").get("careerName")).isEqualTo("Nurse");
        assertThat(service.selected(principal,null).get("careerName")).isEqualTo("Nurse");
        assertThat(profile.getPreferencesJson()).contains("profileAssociations");
        assertThatThrownBy(()->service.selected(principal,"Imaginary Career")).hasMessageContaining("supported career");
        verify(profiles,times(1)).save(profile);
        assertThat(service.selected(principal, "").get("careerName")).isEmpty();
        assertThat(profile.getPreferencesJson()).contains("profileAssociations").doesNotContain("selectedCareer");
    }
    @Test void selectionInitializesMissingLegacyPreferences() {
        var context=mock(StudentContextService.class); var profile=new StudentProfile(); profile.setPreferencesJson(null);
        java.security.Principal principal=()->"learner";
        when(context.requireStudent(principal)).thenReturn(profile);
        var service=new CareerSelectionService(mock(CareerRepository.class),context,mock(StudentProfileRepository.class),new ObjectMapper());
        assertThat(service.selected(principal,null).get("careerName")).isEmpty();
    }
}
