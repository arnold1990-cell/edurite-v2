package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourcesAggregatorServiceTest {

    private final UniversitySourcesAggregatorService service = new UniversitySourcesAggregatorService();

    @Test
    void prioritisesRelevantTechnologyContent() {
        StudentProfile profile = new StudentProfile();
        profile.setInterests("software engineering technology");

        UniversitySourcePageResult relevant = new UniversitySourcePageResult(
                "https://www.unisa.ac.za/a", "Computer Science Programme", UniversityPageType.PROGRAMME_DETAIL,
                "Computer science software engineering modules", Set.of("computer science"), true, null, null);
        UniversitySourcePageResult lessRelevant = new UniversitySourcePageResult(
                "https://www.unisa.ac.za/b", "History", UniversityPageType.UNKNOWN,
                "History and arts", Set.of("arts"), true, null, null);

        String context = service.buildCombinedContext(
                List.of(lessRelevant, relevant),
                profile,
                new UniversitySourcesAnalysisRequest(List.of(), "Software Engineering", "Developer", "Undergraduate", 10)
        );

        assertThat(context).contains("Computer Science Programme");
        assertThat(context).contains("Source URL: https://www.unisa.ac.za/a");
        assertThat(context).contains("Source URL: https://www.unisa.ac.za/b");
    }
}

