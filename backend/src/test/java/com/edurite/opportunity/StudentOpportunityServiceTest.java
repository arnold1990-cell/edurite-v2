package com.edurite.opportunity;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.opportunity.dto.OpportunityType;
import com.edurite.opportunity.dto.UnifiedOpportunityResponse;
import com.edurite.opportunity.service.StudentOpportunityService;
import com.edurite.student.entity.SavedCareer;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentOpportunityServiceTest {

    @Test
    void searchCombinesCareerAndJobResultsWithoutChangingCareerEndpointContract() {
        CareerRepository careerRepository = Mockito.mock(CareerRepository.class);
        SavedCareerRepository savedCareerRepository = Mockito.mock(SavedCareerRepository.class);
        StudentService studentService = Mockito.mock(StudentService.class);
        StudentOpportunityService service = new StudentOpportunityService(careerRepository, savedCareerRepository, studentService);

        Principal principal = () -> "student@example.com";
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setInterests("Technology,Data Science");
        profile.setQualificationLevel("Undergraduate");
        profile.setLocation("Cape Town");

        Career career = new Career();
        career.setId(UUID.randomUUID());
        career.setTitle("Software Engineer");
        career.setIndustry("Technology");
        career.setQualificationLevel("Undergraduate");
        career.setLocation("Remote");
        career.setDemandLevel("High");

        SavedCareer savedCareer = new SavedCareer();
        savedCareer.setStudentId(profile.getId());
        savedCareer.setCareerId(career.getId());

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(savedCareerRepository.findByStudentId(profile.getId())).thenReturn(List.of(savedCareer));
        when(careerRepository.search(eq(""), eq(""), eq(""), eq(""), eq(""), eq(""), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(career)));

        List<UnifiedOpportunityResponse> results = service.search(principal, "", "", "", "", "", "", "ALL");

        assertThat(results).extracting(UnifiedOpportunityResponse::type)
                .contains(OpportunityType.CAREER, OpportunityType.JOB);
        assertThat(results).filteredOn(item -> item.type() == OpportunityType.CAREER)
                .singleElement()
                .satisfies(item -> assertThat(item.saved()).isTrue());
    }

    @Test
    void saveCareerDelegatesToExistingCareerSaveFlowSoLegacySavedCareersStillWork() {
        CareerRepository careerRepository = Mockito.mock(CareerRepository.class);
        SavedCareerRepository savedCareerRepository = Mockito.mock(SavedCareerRepository.class);
        StudentService studentService = Mockito.mock(StudentService.class);
        StudentOpportunityService service = new StudentOpportunityService(careerRepository, savedCareerRepository, studentService);

        Principal principal = () -> "student@example.com";
        UUID careerId = UUID.randomUUID();

        service.saveOpportunity(principal, OpportunityType.CAREER, careerId.toString(), "Software Engineer");

        verify(studentService).saveCareer(principal, careerId);
        verify(savedCareerRepository, never()).save(any(SavedCareer.class));
    }
}

