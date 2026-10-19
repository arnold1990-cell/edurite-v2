package com.edurite.ai.university;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.jsoup.Connection;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.*;
import java.util.*;

class OfficialPageAccessPolicyTest {
    private OfficialPageAccessPolicy publicPolicy() throws IOException {
        var registry = mock(UniversitySourceRegistryService.class);
        when(registry.isAllowedUrl(anyString())).thenReturn(true);
        var policy = spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[] {InetAddress.getByName("8.8.8.8")}).when(policy).resolve(anyString());
        var absentRobots = mock(Connection.Response.class);
        when(absentRobots.statusCode()).thenReturn(404);
        doReturn(absentRobots).when(policy).request(contains("robots.txt"), anyInt(), anyInt());
        return policy;
    }

    @Test
    void nullRobotsBodyFailsClosedWithoutRequestingTheProgramme() throws IOException {
        var policy = publicPolicy();
        var robots = mock(Connection.Response.class);
        when(robots.statusCode()).thenReturn(200); // No body was available.
        doReturn(robots).when(policy).request(contains("robots.txt"), anyInt(), anyInt());
        assertThatThrownBy(() -> policy.fetch("https://official.example/programme", 1000, 10000))
                .isInstanceOf(IOException.class).hasMessageContaining("access policy could not be verified");
        verify(policy, never()).request(eq("https://official.example/programme"), anyInt(), anyInt());
    }

    @Test
    void a304WithoutCachedEvidenceFailsWithAControlledError() throws IOException {
        var policy = publicPolicy();
        var response = mock(Connection.Response.class);
        when(response.statusCode()).thenReturn(304);
        doReturn(response).when(policy).request(eq("https://official.example/programme"), anyInt(), anyInt());
        assertThatThrownBy(() -> policy.fetch("https://official.example/programme", 1000, 10000))
                .isInstanceOf(IOException.class).hasMessageContaining("without a verified cached page");
    }

    @Test
    void cacheEvictionCannotInvalidateTheSnapshotUsedForA304() throws Exception {
        var policy = publicPolicy();
        var published = mock(Connection.Response.class);
        when(published.statusCode()).thenReturn(200);
        when(published.contentType()).thenReturn("text/html");
        when(published.parse()).thenReturn(org.jsoup.Jsoup.parse("<main>Verified evidence</main>", "https://official.example/programme"));
        var unchanged = mock(Connection.Response.class);
        when(unchanged.statusCode()).thenReturn(304);
        doReturn(published, unchanged).when(policy).request(eq("https://official.example/programme"), anyInt(), anyInt());
        policy.fetch("https://official.example/programme", 1000, 10000);

        // Deterministically model another worker evicting entries immediately after a lookup.
        var field = OfficialPageAccessPolicy.class.getDeclaredField("conditionalCache");
        field.setAccessible(true);
        Map<?, ?> original = (Map<?, ?>) field.get(policy);
        var evicting = new java.util.concurrent.ConcurrentHashMap<Object, Object>() {
            private static final long serialVersionUID = 1L;
            @Override public Object get(Object key) {
                Object snapshot = super.get(key); clear(); return snapshot;
            }
            @Override public boolean containsKey(Object key) {
                boolean present = super.containsKey(key); clear(); return present;
            }
        };
        evicting.putAll(original);
        field.set(policy, evicting);
        assertThat(policy.fetch("https://official.example/programme", 1000, 10000).text()).contains("Verified evidence");
        assertThat(evicting).isEmpty();
    }

    @Test
    void pdfExtractionCreatesItsMainElementWithoutANullableLookup() throws IOException {
        var policy = publicPolicy();
        byte[] bytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new ByteArrayOutputStream()) {
            pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            pdf.getDocumentInformation().setTitle("Official prospectus");
            pdf.save(output); bytes = output.toByteArray();
        }
        var response = mock(Connection.Response.class);
        when(response.statusCode()).thenReturn(200);
        when(response.contentType()).thenReturn("application/pdf");
        when(response.bodyAsBytes()).thenReturn(bytes);
        doReturn(response).when(policy).request(eq("https://official.example/prospectus.pdf"), anyInt(), anyInt());
        var document = policy.fetch("https://official.example/prospectus.pdf", 1000, 10000);
        assertThat(document.title()).isEqualTo("Official prospectus");
        assertThat(document.select("main")).hasSize(1);
    }

    @Test void conditionalResponsesReuseOnlyPreviouslyCheckedPublicContentAndTermsAreHonoured() throws Exception {
        var registry=mock(UniversitySourceRegistryService.class);when(registry.isAllowedUrl(anyString())).thenReturn(true);
        var client=spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[]{InetAddress.getByName("8.8.8.8")}).when(client).resolve(anyString());
        var robots=mock(Connection.Response.class);when(robots.statusCode()).thenReturn(404);
        var ok=mock(Connection.Response.class);when(ok.statusCode()).thenReturn(200);when(ok.contentType()).thenReturn("text/html");
        when(ok.header("ETag")).thenReturn("v1");when(ok.parse()).thenReturn(org.jsoup.Jsoup.parse("<main>Published programme evidence</main>","https://official.example/programme"));
        var unchanged=mock(Connection.Response.class);when(unchanged.statusCode()).thenReturn(304);
        doReturn(robots).when(client).request(contains("robots.txt"),anyInt(),anyInt());
        doReturn(ok,unchanged).when(client).request(eq("https://official.example/programme"),anyInt(),anyInt());
        assertThat(client.fetch("https://official.example/programme",1000,10000).text()).contains("Published programme");
        assertThat(client.fetch("https://official.example/programme",1000,10000).text()).contains("Published programme");
        var restricted=mock(Connection.Response.class);when(restricted.statusCode()).thenReturn(200);when(restricted.contentType()).thenReturn("text/html");
        when(restricted.parse()).thenReturn(org.jsoup.Jsoup.parse("<main>Automated access is prohibited</main>"));
        doReturn(restricted).when(client).request(eq("https://official.example/restricted"),anyInt(),anyInt());
        assertThatThrownBy(()->client.fetch("https://official.example/restricted",1000,10000)).hasMessageContaining("terms prohibit");
    }
    @Test
    void robotsDisallowAndLongestAllowAreHonoured() {
        var rules =
                OfficialPageAccessPolicy.parseRobots(
                        "User-agent: *\n"
                                + "Disallow: /private\n"
                                + "Allow: /private/public\n"
                                + "Crawl-delay: 2");
        assertThat(rules.permits("/private/marks")).isFalse();
        assertThat(rules.permits("/private/public/course")).isTrue();
        assertThat(rules.delayMs()).isEqualTo(2000);
    }

    @Test
    void wildcardAndEndAnchorsAreHonoured() {
        var rules =
                OfficialPageAccessPolicy.parseRobots(
                        "User-agent: EduRiteResearch\nDisallow: /*.pdf$\n");
        assertThat(rules.permits("/courses/guide.pdf")).isFalse();
        assertThat(rules.permits("/courses/guide.html")).isTrue();
    }

    @Test
    void specificRobotGroupsDoNotInheritWildcardAllowRulesAndEncodedPathsCannotBypassDenials() {
        var rules =
                OfficialPageAccessPolicy.parseRobots(
                        "User-agent: *\n"
                                + "Allow: /private\n"
                                + "User-agent: EduRiteResearch\n"
                                + "Disallow: /private\n"
                                + "User-agent: EduRiteResearch\n"
                                + "Disallow: /other");
        assertThat(rules.permits("/private")).isFalse();
        assertThat(rules.permits("/%70rivate")).isFalse();
        assertThat(rules.permits("/other")).isFalse();
    }

    @Test
    void privateAndCredentialTargetsAreRejected() throws Exception {
        var registry = mock(UniversitySourceRegistryService.class);
        when(registry.isAllowedUrl(anyString())).thenReturn(true);
        var client = spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[] {InetAddress.getByName("127.0.0.1")})
                .when(client)
                .resolve(anyString());
        assertThatThrownBy(() -> client.validateTarget("https://official.example/programmes"))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(
                        () ->
                                client.validateTarget(
                                        "https://user:password@official.example/programmes"))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> client.validateTarget("file:///etc/passwd"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void aRedirectCannotEscapeTheOfficialRegistry() throws Exception {
        var registry = mock(UniversitySourceRegistryService.class);
        when(registry.isAllowedUrl(startsWith("https://official.example"))).thenReturn(true);
        var client = spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[] {InetAddress.getByName("8.8.8.8")})
                .when(client)
                .resolve(anyString());
        var robot = mock(Connection.Response.class);
        when(robot.statusCode()).thenReturn(404);
        var redirect = mock(Connection.Response.class);
        when(redirect.statusCode()).thenReturn(302);
        when(redirect.header("Location")).thenReturn("https://attacker.example/internal");
        doReturn(robot)
                .when(client)
                .request(eq("https://official.example/robots.txt"), anyInt(), anyInt());
        doReturn(redirect)
                .when(client)
                .request(eq("https://official.example/programmes"), anyInt(), anyInt());
        assertThatThrownBy(() -> client.fetch("https://official.example/programmes", 1000, 10000))
                .isInstanceOf(IOException.class);
        verify(client, never()).request(startsWith("https://attacker"), anyInt(), anyInt());
    }

    @Test
    void robotsAccessRestrictionsFailClosed() throws Exception {
        var registry = mock(UniversitySourceRegistryService.class);
        when(registry.isAllowedUrl(anyString())).thenReturn(true);
        var client = spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[] {InetAddress.getByName("8.8.8.8")})
                .when(client)
                .resolve(anyString());
        var robot = mock(Connection.Response.class);
        when(robot.statusCode()).thenReturn(403);
        doReturn(robot).when(client).request(contains("robots.txt"), anyInt(), anyInt());
        assertThatThrownBy(() -> client.fetch("https://official.example/programmes", 1000, 10000))
                .isInstanceOf(IOException.class);
        verify(client, never())
                .request(eq("https://official.example/programmes"), anyInt(), anyInt());
    }

    @Test
    void redirectsToAnotherOfficialInstitutionCannotBeAttributedToTheOriginalUniversity()
            throws Exception {
        var registry = mock(UniversitySourceRegistryService.class);
        when(registry.isAllowedUrl(anyString())).thenReturn(true);
        var policy = spy(new OfficialPageAccessPolicy(registry));
        doReturn(new InetAddress[] {InetAddress.getByName("8.8.8.8")})
                .when(policy)
                .resolve(anyString());
        var robots = mock(Connection.Response.class);
        when(robots.statusCode()).thenReturn(404);
        var redirect = mock(Connection.Response.class);
        when(redirect.statusCode()).thenReturn(302);
        when(redirect.header("Location")).thenReturn("https://another-official.example/programme");
        doReturn(robots)
                .when(policy)
                .request(eq("https://original-official.example/robots.txt"), anyInt(), anyInt());
        doReturn(redirect)
                .when(policy)
                .request(eq("https://original-official.example/programme"), anyInt(), anyInt());
        assertThatThrownBy(
                        () ->
                                policy.fetch(
                                        "https://original-official.example/programme", 1000, 10000))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("original official institution");
        verify(policy, never()).request(startsWith("https://another-official"), anyInt(), anyInt());
    }
}
