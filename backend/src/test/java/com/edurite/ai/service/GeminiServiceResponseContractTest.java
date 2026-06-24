package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceResponseContractTest {

    @Test
    void fallbackResponseSetsAiLiveAndFallbackFlags() {
        GeminiService service = GeminiServiceTestFactory.service("");

        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/page"), "Software", "Developer", "Undergraduate", 3),
                new StudentProfile(),
                List.of("https://www.unisa.ac.za/page"),
                List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/page", "t", UniversityPageType.QUALIFICATION_LIST,
                        "content", Set.of("computer science"), true, null, null)),
                "context"
        );

        assertThat(response.aiLive()).isFalse();
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.warningMessage()).isEqualTo("AI guidance is temporarily unavailable. Please try again later.");
    }
}



