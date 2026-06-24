package com.edurite.ai.university;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiUniversityPageFetcherServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void discoversHomepageLinksBeforeFallbackPathsAndFiltersLowValueLinks() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::rootPage);
        server.createContext("/courses", exchange -> respond(exchange, 200, "<html><title>Courses</title><body>courses</body></html>"));
        server.createContext("/programmes", exchange -> respond(exchange, 200, "<html><title>Programmes</title><body>entry requirements degree</body></html>"));
        server.start();

        int port = server.getAddress().getPort();
        UniversityRegistryProperties properties = buildProperties(port);
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());
        MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
                registryService,
                new UniversityPageClassifier(),
                new UniversityUrlNormalizer(),
                properties
        );

        var university = properties.getRegistry().get(0);
        List<String> candidates = service.discoverCandidateUrls(university, 15);

        assertThat(candidates.get(0)).isEqualTo("http://localhost:" + port + "/");
        assertThat(candidates).contains("http://localhost:" + port + "/programmes");
        assertThat(candidates).doesNotContain("http://localhost:" + port + "/news/latest");
        assertThat(candidates).doesNotContain("https://external.example.com/programmes");
    }



    @Test
    void recordsLoginAndNotFoundFailuresWithTerminalReasons() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login", exchange -> respond(exchange, 200, "<html><title>Login</title><body>Please log in with your password</body></html>"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "missing"));
        server.start();

        int port = server.getAddress().getPort();
        UniversityRegistryProperties properties = buildProperties(port);
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());
        MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
                registryService,
                new UniversityPageClassifier(),
                new UniversityUrlNormalizer(),
                properties
        );

        var results = service.fetchPages(List.of(
                "http://localhost:" + port + "/login",
                "http://localhost:" + port + "/missing"
        ));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).failureReason()).contains("Rejected login-style page");
        assertThat(results.get(1).success()).isFalse();
        assertThat(results.get(1).failureType()).isEqualTo(UniversityCrawlFailureType.NOT_FOUND);
        assertThat(results.get(1).failureReason()).contains("HTTP 404");
    }

    @Test
    void retriesTransientFailuresWithBackoffUntilSuccess() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/flaky", exchange -> {
            if (attempts.incrementAndGet() < 2) {
                respond(exchange, 503, "temporary failure");
                return;
            }
            respond(exchange, 200, "<html><title>Programmes</title><body><main>programme degree entry requirements undergraduate admissions application dates faculty handbook prospectus " +
                    "This page contains enough official university content to pass the current extraction threshold after a retry succeeds.</main></body></html>");
        });
        server.start();

        int port = server.getAddress().getPort();
        UniversityRegistryProperties properties = buildProperties(port);
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());
        MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
                registryService,
                new UniversityPageClassifier(),
                new UniversityUrlNormalizer(),
                properties
        );

        var results = service.fetchPages(List.of("http://localhost:" + port + "/flaky"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isTrue();
        assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void keepsProcessingAllUrlsWhenSomeAreBlockedAndClassifiesFailures() {
        UniversityRegistryProperties properties = buildProperties(8080);
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());
        MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
                registryService,
                new UniversityPageClassifier(),
                new UniversityUrlNormalizer(),
                properties
        );

        var results = service.fetchPages(List.of(
                "https://malicious.invalid/phishing",
                "http://localhost:8080/not-found"
        ));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).failureType()).isEqualTo(UniversityCrawlFailureType.ACCESS_DENIED);
        assertThat(results.get(1).success()).isFalse();
    }

    private void rootPage(HttpExchange exchange) throws IOException {
        String body = """
                <html>
                  <body>
                    <a href=\"/programmes\">Undergraduate programmes and degrees</a>
                    <a href=\"/news/latest\">Latest news</a>
                    <a href=\"https://external.example.com/programmes\">External</a>
                  </body>
                </html>
                """;
        respond(exchange, 200, body);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(data);
        }
    }

    private UniversityRegistryProperties buildProperties(int port) {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
        entry.setUniversityName("University A");
        entry.setBaseDomain("localhost");
        entry.setAllowedDomains(List.of("localhost"));
        entry.setSeedUrls(List.of("http://localhost:" + port + "/"));
        properties.getRegistry().add(entry);
        return properties;
    }
}

