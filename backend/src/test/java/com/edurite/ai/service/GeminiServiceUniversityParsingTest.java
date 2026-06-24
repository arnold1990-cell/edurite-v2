package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceUniversityParsingTest {

    @Test
    void parseUniversityAdviceFallsBackToSectionParsingAndAddsMinimumRequirements() throws JsonProcessingException {
        GeminiService service = GeminiServiceTestFactory.service();

        String messyText = """
                Recommended careers
                - Mathematician
                - Data Scientist

                Recommended programmes
                - Bachelor of Science (BSc) in Mathematics

                Recommended universities
                - University of Cape Town (UCT)

                Skill gaps
                - Programming and Computational Skills

                Recommended next steps
                - Attend university open days

                Warnings
                - Verify admission requirements directly with universities

                Summary
                - Strong fit for mathematics-oriented pathways.
                """;

        UniversitySourcesAnalysisResponse response = (UniversitySourcesAnalysisResponse) ReflectionTestUtils.invokeMethod(
                service,
                "parseUniversityAdvice",
                messyText,
                List.of("https://www.uct.ac.za"),
                List.of("https://www.uct.ac.za"),
                List.of()
        );

        assertThat(response).isNotNull();
        assertThat(response.recommendedCareers()).extracting(UniversitySourcesAnalysisResponse.RecommendedCareer::name)
                .contains("Mathematician", "Data Scientist");
        assertThat(response.minimumRequirements()).anyMatch(item -> item.contains("Grade 12 passes"));
        assertThat(response.minimumRequirements()).anyMatch(item -> item.contains("Mathematics"));
        assertThat(response.minimumRequirements()).anyMatch(item -> item.contains("English"));
        assertThat(response.summary()).contains("Strong fit");
    }
}


