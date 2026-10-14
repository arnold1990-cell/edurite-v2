package com.edurite.ai.service;

import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class ProviderCompletionTest {
    @Test void geminiPreservesRepeatedTextPartsInOrder() {
        var gemini=GeminiServiceTestFactory.service("");
        String body="{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"Step.\"},{\"text\":\"Step.\"}]}}]}";
        String answer=ReflectionTestUtils.invokeMethod(gemini,"extractModelText",body);
        assertThat(answer).isEqualTo("Step.Step.");
    }
    @Test void geminiRejectsTokenLimitedAnswerAndRequestsSufficientBudget() {
        var gemini=GeminiServiceTestFactory.service("");
        String body="{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"Partial answer\"}]}}]}";
        assertThatThrownBy(()->ReflectionTestUtils.invokeMethod(gemini,"extractModelText",body)).isInstanceOf(AiServiceException.class);
        String request=ReflectionTestUtils.invokeMethod(gemini,"serializeGenerateContentRequest","Explain algebra");
        assertThat(request).contains("maxOutputTokens", "8192");
    }
    @Test void openAiRejectsLengthFinishAndPreservesFullCompletedAnswer() {
        var service=new OpenAiService(new ObjectMapper(),new MockEnvironment());
        String incomplete="{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"Incomplete\"}}]}";
        assertThatThrownBy(()->ReflectionTestUtils.invokeMethod(service,"extractContent",incomplete)).isInstanceOf(AiServiceException.class);
        String full=ReflectionTestUtils.invokeMethod(service,"extractContent",incomplete.replace("length","stop"));
        assertThat(full).isEqualTo("Incomplete");
        String payload=ReflectionTestUtils.invokeMethod(service,"serializePayload","gpt-4o-mini","Explain");
        assertThat(payload).contains("max_completion_tokens", "8192");
    }
}
