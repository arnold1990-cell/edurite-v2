package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceLivePathTest {

    private static final String FRIENDLY_UNAVAILABLE = "AI guidance is temporarily unavailable. Please try again later.";

    @Test
    void sourcePipelineEmptyStillAttemptsGeminiAndReturnsLiveResponse() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.0-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            String body = """
                    {
                      \"candidates\": [{
                        \"content\": {
                          \"parts\": [{
                            \"text\": \"{\\\"recommendedCareers\\\":[{\\\"name\\\":\\\"Software Developer\\\",\\\"reason\\\":\\\"Strong fit\\\",\\\"requirements\\\":[\\\"Maths\\\"],\\\"relatedProgrammes\\\":[\\\"BSc Computer Science\\\"]}],\\\"recommendedProgrammes\\\":[{\\\"name\\\":\\\"BSc Computer Science\\\",\\\"university\\\":\\\"UNISA\\\",\\\"admissionRequirements\\\":[\\\"Grade 12\\\"],\\\"notes\\\":\\\"Check admissions page\\\"}],\\\"recommendedUniversities\\\":[\\\"UNISA\\\"],\\\"minimumRequirements\\\":[\\\"Grade 12 passes\\\"],\\\"keyRequirements\\\":[\\\"English\\\"],\\\"skillGaps\\\":[\\\"Portfolio\\\"],\\\"recommendedNextSteps\\\":[\\\"Compare programmes\\\"],\\\"warnings\\\":[],\\\"summary\\\":\\\"Profile-based guidance\\\",\\\"suitabilityScore\\\":82}\" 
                          }]
                        }
                      }]
                    }
                    """;
            writeResponse(exchange, 200, body);
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.0-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of(), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of(),
                    List.of(),
                    ""
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isTrue();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).contains("No external university sources were available");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sourcePipelineEmptyAndGeminiFailureFallsBackAfterAttempt() {
        GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.0-flash", "http://127.0.0.1:1");

        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(List.of(), "Computer Science", "Developer", "Undergraduate", 5),
                new StudentProfile(),
                List.of(),
                List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/page", "title", UniversityPageType.QUALIFICATION_LIST,
                        "content", Set.of("admissions"), false, "read-error", null)),
                ""
        );

        assertThat(response.aiLive()).isFalse();
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
        assertThat(response.warnings()).contains(
                FRIENDLY_UNAVAILABLE,
                "No external university sources were available for this request; guidance was generated from profile context.",
                "No combined source context was available; guidance was generated from profile context."
        );
    }

    @Test
    void sourcePipelineWithSourcesStillUsesGeminiAsPrimary() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            String body = """
                    {
                      \"candidates\": [{
                        \"content\": {
                          \"parts\": [{
                            \"text\": \"{\\\"recommendedCareers\\\":[{\\\"name\\\":\\\"Electrical Engineer\\\",\\\"reason\\\":\\\"Strong fit\\\",\\\"requirements\\\":[\\\"Physics\\\"],\\\"relatedProgrammes\\\":[\\\"BSc Engineering\\\"]}],\\\"recommendedProgrammes\\\":[{\\\"name\\\":\\\"BSc Engineering\\\",\\\"university\\\":\\\"UJ\\\",\\\"admissionRequirements\\\":[\\\"Grade 12\\\"],\\\"notes\\\":\\\"Check admissions page\\\"}],\\\"recommendedUniversities\\\":[\\\"UJ\\\"],\\\"minimumRequirements\\\":[\\\"Grade 12 passes\\\"],\\\"keyRequirements\\\":[\\\"Mathematics\\\"],\\\"skillGaps\\\":[\\\"CAD\\\"],\\\"recommendedNextSteps\\\":[\\\"Review programme pages\\\"],\\\"warnings\\\":[],\\\"summary\\\":\\\"Source-grounded guidance\\\",\\\"suitabilityScore\\\":78}\"
                          }]
                        }
                      }]
                    }
                    """;
            writeResponse(exchange, 200, body);
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.uj.ac.za/programmes"), "Engineering", "Engineer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.uj.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.uj.ac.za/programmes", "UJ Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Engineering programmes", Set.of("engineering"), true, null, null)),
                    "Engineering source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isTrue();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void malformedLiveModelOutputIsNormalizedWithoutFallingBack() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            String body = """
                    {
                      "candidates": [{
                        "content": {
                          "parts": [{
                            "text": "Recommended careers:\\n- Software Developer\\n- Data Analyst\\n\\nRecommended next steps:\\n- Compare programme pages\\n- Verify requirements"
                          }]
                        }
                      }]
                    }
                    """;
            writeResponse(exchange, 200, body);
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.uj.ac.za/programmes"), "Engineering", "Engineer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.uj.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.uj.ac.za/programmes", "UJ Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Engineering programmes", Set.of("engineering"), true, null, null)),
                    "Engineering source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isTrue();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warnings()).contains("Live model response format differed from the required JSON schema; EduRite normalized the response.");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidModelConfigFallsBackToSupportedDefaultModel() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            String body = """
                    {
                      "candidates": [{
                        "content": {
                          "parts": [{
                            "text": "{\\"recommendedCareers\\":[{\\"name\\":\\"Software Developer\\",\\"reason\\":\\"Strong fit\\",\\"requirements\\":[\\"Maths\\"],\\"relatedProgrammes\\":[\\"BSc Computer Science\\"]}],\\"recommendedProgrammes\\":[{\\"name\\":\\"BSc Computer Science\\",\\"university\\":\\"UNISA\\",\\"admissionRequirements\\":[\\"Grade 12\\"],\\"notes\\":\\"Check admissions page\\"}],\\"recommendedUniversities\\":[\\"UNISA\\"],\\"minimumRequirements\\":[\\"Grade 12 passes\\"],\\"keyRequirements\\":[\\"English\\"],\\"skillGaps\\":[\\"Portfolio\\"],\\"recommendedNextSteps\\":[\\"Compare programmes\\"],\\"warnings\\":[],\\"summary\\":\\"Profile-based guidance\\",\\"suitabilityScore\\":82}"
                          }]
                        }
                      }]
                    }
                    """;
            writeResponse(exchange, 200, body);
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "invalid model name", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isTrue();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.rawModelUsed()).isEqualTo("gemini-2.5-flash");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerUnauthorizedResponseFallsBackWithFriendlyWarning() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            writeResponse(exchange, 403, "{\"error\":{\"code\":403,\"status\":\"PERMISSION_DENIED\",\"message\":\"API key not valid\"}}");
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isFalse();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
            assertThat(response.warnings()).contains(FRIENDLY_UNAVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void quotaOrServiceUnavailableResponseFallsBackWithFriendlyWarning() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            writeResponse(exchange, 429, "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Quota exceeded\"}}");
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isGreaterThanOrEqualTo(1);
            assertThat(response.aiLive()).isFalse();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void malformedProviderBodyFallsBackWithFriendlyWarning() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            writeResponse(exchange, 200, "{this-is-not-json}");
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isFalse();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void emptyCandidatesPayloadFallsBackWithFriendlyWarning() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            writeResponse(exchange, 200, "{\"candidates\":[]}");
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isFalse();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void blockedProviderResponseFallsBackWithFriendlyWarning() throws IOException {
        AtomicInteger callCounter = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCounter.incrementAndGet();
            writeResponse(exchange, 200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"},\"candidates\":[]}");
        });
        server.start();

        try {
            GeminiService service = GeminiServiceTestFactory.service("test-key", "gemini-2.5-flash", "http://localhost:" + server.getAddress().getPort());

            UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                    new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/programmes"), "Computer Science", "Developer", "Undergraduate", 5),
                    new StudentProfile(),
                    List.of("https://www.unisa.ac.za/programmes"),
                    List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/programmes", "UNISA Programmes", UniversityPageType.PROGRAMME_DETAIL,
                            "Computer science programmes", Set.of("software"), true, null, null)),
                    "source context"
            );

            assertThat(callCounter.get()).isEqualTo(1);
            assertThat(response.aiLive()).isFalse();
            assertThat(response.fallbackUsed()).isFalse();
            assertThat(response.warningMessage()).isEqualTo(FRIENDLY_UNAVAILABLE);
        } finally {
            server.stop(0);
        }
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}

