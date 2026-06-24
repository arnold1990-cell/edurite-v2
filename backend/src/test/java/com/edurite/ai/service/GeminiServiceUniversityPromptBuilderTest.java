package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceUniversityPromptBuilderTest {

    @Test
    void universityPromptIncludesMultipleSourcesMetadataAndMergedContext() {
        GeminiService service = GeminiServiceTestFactory.service();
        StudentProfile profile = new StudentProfile();
        profile.setFirstName("Ada");
        profile.setInterests("software, data");
        profile.setQualificationLevel("Undergraduate");

        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(
                List.of("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b"),
                "Computer Science",
                "Software Developer",
                "Undergraduate",
                10
        );

        List<UniversitySourcePageResult> pages = List.of(
                new UniversitySourcePageResult("https://www.unisa.ac.za/a", "UNISA qualifications", UniversityPageType.QUALIFICATION_LIST,
                        "UNISA content", Set.of("computer science"), true, null, null),
                new UniversitySourcePageResult("https://www.uj.ac.za/b", "UJ programmes", UniversityPageType.PROGRAMME_DETAIL,
                        "UJ content", Set.of("software"), true, null, null)
        );

        String prompt = (String) ReflectionTestUtils.invokeMethod(
                service,
                "buildUniversityPrompt",
                request,
                profile,
                pages,
                "MERGED-CONTEXT-BLOCK"
        );

        assertThat(prompt).contains("https://www.unisa.ac.za/a");
        assertThat(prompt).contains("https://www.uj.ac.za/b");
        assertThat(prompt).contains("MERGED-CONTEXT-BLOCK");
        assertThat(prompt).contains("\"minimumRequirements\"");
        assertThat(prompt).contains("Do not include application due dates");
    }
}


