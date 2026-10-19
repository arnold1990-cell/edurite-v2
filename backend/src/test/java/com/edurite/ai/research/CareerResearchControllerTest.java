package com.edurite.ai.research;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.edurite.ai.research.CareerResearchDtos.JobStatus;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

class CareerResearchControllerTest {
    @Test
    void postAcceptsJsonIdempotencyKeyUsingExistingCorsContentType() throws Exception {
        var jobs = mock(CareerResearchJobService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CareerResearchController(jobs)).build();
        Principal principal = () -> "student@example.test";
        String key = UUID.randomUUID().toString();
        when(jobs.start(principal, key))
                .thenReturn(new JobStatus("job", "QUEUED", "Queued", null, null, null));
        mvc.perform(
                        post("/api/ai/career-research")
                                .principal(principal)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"requestKey\":\"" + key + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
        verify(jobs).start(principal, key);
        verifyNoMoreInteractions(jobs);
    }

    @Test
    void statusEndpointOnlyRetrievesAuthenticatedOwnersJob() throws Exception {
        var jobs = mock(CareerResearchJobService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CareerResearchController(jobs)).build();
        Principal principal = () -> "student@example.test";
        when(jobs.status(principal))
                .thenReturn(new JobStatus(null, "IDLE", null, null, null, null));
        mvc.perform(get("/api/v1/ai/career-research").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IDLE"));
        verify(jobs).status(principal);
        verifyNoMoreInteractions(jobs);
    }
}
