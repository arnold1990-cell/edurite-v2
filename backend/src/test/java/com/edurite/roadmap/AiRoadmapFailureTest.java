package com.edurite.roadmap;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.roadmap.service.AiCareerRoadmapService;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiRoadmapFailureTest {
    @Test void providerOutageIsNotReportedAsAGeneratedRoadmap() {
        var ai=mock(AiProviderOrchestratorService.class);
        when(ai.generateContent(any())).thenThrow(new IllegalStateException("Provider unavailable"));
        var service=new AiCareerRoadmapService(ai,new ObjectMapper());
        assertThatThrownBy(()->service.generate(new CareerRoadmapGenerateRequest("Nurse",null,null,List.of()),null,"","",null))
            .hasMessage("Provider unavailable");
    }
    @Test void emptyAndMalformedRoadmapsFailWithRetryGuidance() {
        var ai=mock(AiProviderOrchestratorService.class);
        var service=new AiCareerRoadmapService(ai,new ObjectMapper());
        for(String payload:List.of("{}","null","not json")) {
            when(ai.generateContent(any())).thenReturn(payload);
            assertThatThrownBy(()->service.generate(new CareerRoadmapGenerateRequest("Nurse",null,null,List.of()),null,"","",null))
                .hasMessageContaining("retry");
        }
    }
}
