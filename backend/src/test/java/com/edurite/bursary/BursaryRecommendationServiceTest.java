package com.edurite.bursary;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchResponse;
import com.edurite.bursary.service.BursaryAggregationService;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BursaryRecommendationServiceTest {
    @Test
    void ranksByStudentProfileRelevance() {
        StudentService studentService = mock(StudentService.class);
        BursaryAggregationService aggregationService = mock(BursaryAggregationService.class);
        BursaryRecommendationService service = new BursaryRecommendationService(studentService, aggregationService);

        StudentProfile profile = new StudentProfile();
        profile.setQualificationLevel("Degree");
        profile.setLocation("Gauteng");
        profile.setSkills("python");
        profile.setCareerGoals("data science");
        when(studentService.getProfileEntity(any())).thenReturn(profile);

        BursaryResultDto b1 = new BursaryResultDto("1", "Data Science Bursary", "P", "data science", "Degree", "Gauteng", "python", LocalDate.now(), "", "OFFICIAL_PROVIDER", 70, List.of(), true, false, null);
        BursaryResultDto b2 = new BursaryResultDto("2", "Generic", "P", "general", "Diploma", "Cape Town", "none", null, "", "TRUSTED_PUBLIC_FALLBACK", 75, List.of("https://example.org"), false, true, "Unverified");
        when(aggregationService.search(any())).thenReturn(new BursarySearchResponse(List.of(b1, b2), 0, 20, 2));

        List<BursaryResultDto> recommendations = service.recommendForStudent((Principal) () -> "student@test");

        assertThat(recommendations.get(0).title()).isEqualTo("Data Science Bursary");
        assertThat(recommendations.get(0).relevanceScore()).isGreaterThan(recommendations.get(1).relevanceScore());
        assertThat(recommendations.get(0).officialSource()).isTrue();
    }
}

