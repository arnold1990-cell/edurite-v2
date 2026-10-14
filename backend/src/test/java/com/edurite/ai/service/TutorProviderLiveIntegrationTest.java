package com.edurite.ai.service;

import com.edurite.ai.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

/** Explicit opt-in only: uses configured credentials, without learner data or persistence. */
@EnabledIfEnvironmentVariable(named="EDURITE_LIVE_AI_TEST",matches="true")
class TutorProviderLiveIntegrationTest {
    private GeminiService provider() {
        var properties=new GeminiProperties();
        properties.setApiKey(System.getenv("GEMINI_API_KEY"));
        properties.setModel(System.getenv().getOrDefault("GEMINI_MODEL","gemini-2.5-flash"));
        properties.setMaxRetries(0);
        properties.setReadTimeoutSeconds(90); properties.setCallTimeoutSeconds(90);
        return new GeminiService(new ObjectMapper(),properties,null);
    }
    @Test void simpleQuestionCompletes() {
        assertThat(provider().generateContent("As a school tutor, answer briefly: what is 2 + 3?")).contains("5");
    }
    @Test void multiStepMathematicsCompletes() {
        String answer=provider().generateContent("Teach a Grade 10 learner how to solve x squared minus 5x plus 6 equals zero. Explain factoring and the quadratic formula, show each calculation with both methods, check both roots, explain one common mistake and provide two practice questions. Complete the explanation now, without asking for continuation.");
        assertThat(answer.length()).isGreaterThan(700);
        assertThat(answer).contains("2", "3");
    }
}
