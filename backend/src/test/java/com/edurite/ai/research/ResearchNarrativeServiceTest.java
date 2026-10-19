package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.edurite.ai.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.*;

class ResearchNarrativeServiceTest {
    final GeminiService ai = mock(GeminiService.class);
    final ResearchNarrativeService service = new ResearchNarrativeService(ai, new ObjectMapper());

    AcademicAnalysis academic() {
        return new AcademicAnalysis(
                "Grade 12",
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Finance"),
                List.of(),
                "Accounting",
                List.of());
    }

    @Test
    void validGroundedInterpretationIsAccepted() {
        when(ai.getCareerResearchNarrative(anyString()))
                .thenReturn(
                        "{\"assessment\":\"Your saved finance interest supports"
                                + " exploration.\",\"researchSummary\":\"Programme evidence is"
                                + " missing.\",\"nextSteps\":[\"Review programme"
                                + " sources.\"],\"sourceUrls\":[]}");
        assertThat(service.generate(academic(), List.of(), List.of(), List.of()).assessment())
                .contains("finance");
    }

    @Test
    void unsupportedSourcesAndPercentagesFailValidation() {
        for (String extra :
                List.of("\"sourceUrls\":[\"https://invented.example\"]", "\"sourceUrls\":[]")) {
            when(ai.getCareerResearchNarrative(anyString()))
                    .thenReturn(
                            "{\"assessment\":\"Your mark is"
                                + " 97%.\",\"researchSummary\":\"Summary\",\"nextSteps\":[\"Review\"],"
                                    + extra
                                    + "}");
            assertThatThrownBy(() -> service.generate(academic(), List.of(), List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void malformedPartialAndAdditionalFactsCannotBeSaved() {
        for (String json :
                List.of(
                        "not json",
                        "{}",
                        "{\"assessment\":\"a\",\"researchSummary\":\"b\",\"nextSteps\":[],\"sourceUrls\":[]}",
                        "{\"assessment\":\"a\",\"researchSummary\":\"b\",\"nextSteps\":[\"c\"],\"sourceUrls\":[],\"inventedProgrammes\":[]}")) {
            when(ai.getCareerResearchNarrative(anyString())).thenReturn(json);
            assertThatThrownBy(() -> service.generate(academic(), List.of(), List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void trailingOutputAndCoercedNonTextValuesAreRejected() {
        String valid =
                "{\"assessment\":\"a\",\"researchSummary\":\"b\",\"nextSteps\":[\"c\"],\"sourceUrls\":[]}";
        for (String response :
                List.of(
                        valid + " extra output",
                        valid.replace("[\"c\"]", "[123]"),
                        valid.replace("\"assessment\":\"a\"", "\"assessment\":123"))) {
            when(ai.getCareerResearchNarrative(anyString())).thenReturn(response);
            assertThatThrownBy(() -> service.generate(academic(), List.of(), List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void spacedUnsupportedPercentagesAreRejected() {
        when(ai.getCareerResearchNarrative(anyString()))
                .thenReturn(
                        "{\"assessment\":\"You scored 97"
                            + " %\",\"researchSummary\":\"Summary\",\"nextSteps\":[\"Review\"],\"sourceUrls\":[]}");
        assertThatThrownBy(() -> service.generate(academic(), List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported percentage");
    }
}
