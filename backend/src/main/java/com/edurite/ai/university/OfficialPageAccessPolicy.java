package com.edurite.ai.university;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Shared official-site transport: honest identity, public targets, robots and host pacing. */
@Component
public class OfficialPageAccessPolicy {
    public static final String AGENT = "EduRiteResearch/1.0 (+https://edurite.org)";
    private final UniversitySourceRegistryService registry;
    private final Map<String, Robots> robots = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequest = new ConcurrentHashMap<>();
    private final Map<String, Object> hostLocks = new ConcurrentHashMap<>();

    public OfficialPageAccessPolicy(UniversitySourceRegistryService registry) {
        this.registry = registry;
    }

    public Document fetch(String raw, int timeout, int bytes) throws IOException {
        String url = raw;
        URI initial = validateTarget(raw);
        var institutions =
                registry.getActiveUniversities().stream()
                        .map(
                                UniversityRegistryProperties.UniversityRegistryEntry
                                        ::getUniversityName)
                        .filter(name -> registry.isAllowedUrlForUniversity(name, raw))
                        .toList();
        for (int redirects = 0; redirects < 4; redirects++) {
            URI uri = validateTarget(url);
            String target = url;
            if (redirects > 0
                    && (institutions.isEmpty()
                            ? !initial.getHost().equalsIgnoreCase(uri.getHost())
                            : institutions.stream()
                                    .noneMatch(
                                            name ->
                                                    registry.isAllowedUrlForUniversity(
                                                            name, target))))
                throw new IOException(
                        "Redirect left the original official institution; attribution cannot be"
                            + " verified");
            url = uri.toString();
            String origin = uri.getScheme() + "://" + uri.getHost();
            Robots rules = robots.get(origin);
            if (rules == null || rules.expires().isBefore(Instant.now())) {
                synchronized (hostLocks.computeIfAbsent(origin, ignored -> new Object())) {
                    rules = robots.get(origin);
                    if (rules == null || rules.expires().isBefore(Instant.now())) {
                        var response =
                                request(origin + "/robots.txt", Math.min(timeout, 5000), 512_000);
                        if (Objects.toString(response.body(), "").length() >= 500_000)
                            throw new IOException(
                                    "Official robots file exceeds the safe parsing budget");
                        if (response.statusCode() == 404)
                            rules =
                                    new Robots(
                                            List.of(),
                                            List.of(),
                                            1000,
                                            Instant.now().plusSeconds(21600));
                        else if (response.statusCode() == 200
                                && !response.body().stripLeading().startsWith("<"))
                            rules = parseRobots(response.body());
                        else
                            throw new IOException(
                                    "Official site access policy could not be verified");
                        robots.put(origin, rules);
                        lastRequest.put(origin, System.currentTimeMillis());
                    }
                }
            }
            String path =
                    (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                            + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            if (!rules.permits(path))
                throw new IOException("Official site robots policy disallows this page");
            if (rules.delayMs() > 10_000)
                throw new IOException("Official site requests a longer crawl delay; deferred");
            synchronized (hostLocks.computeIfAbsent(origin, ignored -> new Object())) {
                long wait =
                        rules.delayMs()
                                - (System.currentTimeMillis()
                                        - lastRequest.getOrDefault(origin, 0L));
                if (wait > 0)
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Research interrupted", ex);
                    }
                lastRequest.put(origin, System.currentTimeMillis());
            }
            var response =
                    request(
                            url,
                            Math.min(10_000, Math.max(1000, timeout)),
                            Math.min(bytes, 1_500_000));
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.header("Location");
                if (location == null)
                    throw new IOException("Official page redirected without a location");
                url = uri.resolve(location).toString();
                continue;
            }
            if (response.statusCode() != 200)
                throw new HttpStatusException(
                        "Official source unavailable", response.statusCode(), url);
            Document document;
            if (Objects.toString(response.contentType(), "")
                    .toLowerCase(Locale.ROOT)
                    .contains("application/pdf")) {
                try (var pdf = org.apache.pdfbox.Loader.loadPDF(response.bodyAsBytes())) {
                    if (pdf.isEncrypted())
                        throw new IOException("Encrypted official document is not accessible");
                    var stripper = new org.apache.pdfbox.text.PDFTextStripper();
                    stripper.setEndPage(Math.min(30, pdf.getNumberOfPages()));
                    document =
                            org.jsoup.Jsoup.parse(
                                    "<html><head></head><body><main></main></body></html>", url);
                    document.title(
                            Objects.toString(
                                    pdf.getDocumentInformation().getTitle(),
                                    "Official prospectus"));
                    document.selectFirst("main").text(stripper.getText(pdf));
                }
            } else {
                String type = Objects.toString(response.contentType(), "").toLowerCase(Locale.ROOT);
                if (!type.isEmpty() && !type.contains("html") && !type.contains("text/plain"))
                    throw new IOException("Unsupported official document content type");
                document = response.parse();
            }
            String text = document.text().toLowerCase(Locale.ROOT);
            if (text.contains("verify you are human")
                    || text.contains("checking your browser")
                    || text.contains("captcha") && text.length() < 3000
                    || !document.select("input[type=password]").isEmpty())
                throw new IOException(
                        "Access challenge or authentication required; research stopped");
            return document;
        }
        throw new IOException("Official source redirect budget exceeded");
    }

    protected Connection.Response request(String url, int timeout, int bytes) throws IOException {
        return Jsoup.connect(url)
                .userAgent(AGENT)
                .timeout(timeout)
                .maxBodySize(bytes)
                .followRedirects(false)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .execute();
    }

    protected InetAddress[] resolve(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    URI validateTarget(String raw) throws IOException {
        try {
            URI uri = URI.create(raw).normalize();
            if (!Set.of("https", "http").contains(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80
                    || !registry.isAllowedUrl(raw))
                throw new IOException("Source is not an approved official public URL");
            for (InetAddress address : resolve(uri.getHost())) {
                byte[] ip = address.getAddress();
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || (ip.length == 16 && (ip[0] & 0xfe) == 0xfc)
                        || (ip.length == 4
                                && ((ip[0] & 255) == 0
                                        || (ip[0] & 255) == 100
                                                && (ip[1] & 255) >= 64
                                                && (ip[1] & 255) <= 127)))
                    throw new IOException("Non-public source address rejected");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid official source URL", ex);
        }
    }

    static Robots parseRobots(String content) {
        record Group(List<String> agents, List<String> deny, List<String> allow, long delay) {}
        List<Group> groups = new ArrayList<>();
        List<String> agents = new ArrayList<>(),
                deny = new ArrayList<>(),
                allow = new ArrayList<>();
        boolean directives = false;
        long delay = 1000;
        for (String original : content.split("\\R")) {
            String line = original.split("#", 2)[0].trim();
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                    value = line.substring(colon + 1).trim();
            if (key.equals("user-agent")) {
                if (directives) {
                    groups.add(
                            new Group(
                                    List.copyOf(agents),
                                    List.copyOf(deny),
                                    List.copyOf(allow),
                                    delay));
                    agents.clear();
                    deny.clear();
                    allow.clear();
                    delay = 1000;
                    directives = false;
                }
                agents.add(value.toLowerCase(Locale.ROOT));
            } else if (!agents.isEmpty()) {
                directives = true;
                if (key.equals("allow") && value.startsWith("/")) allow.add(value);
                if (key.equals("disallow") && value.startsWith("/")) deny.add(value);
                if (key.equals("crawl-delay"))
                    try {
                        double seconds = Double.parseDouble(value);
                        delay =
                                Double.isFinite(seconds) && seconds >= 0
                                        ? Math.max(delay, (long) (seconds * 1000))
                                        : 60_000;
                    } catch (NumberFormatException ex) {
                        delay = 60_000;
                    }
            }
        }
        groups.add(new Group(agents, deny, allow, delay));
        boolean specific = groups.stream().anyMatch(g -> g.agents().contains("eduriteresearch"));
        List<String> denied = new ArrayList<>(), allowed = new ArrayList<>();
        long selectedDelay = 1000;
        for (var group : groups)
            if (group.agents().contains(specific ? "eduriteresearch" : "*")) {
                denied.addAll(group.deny());
                allowed.addAll(group.allow());
                selectedDelay = Math.max(selectedDelay, group.delay());
            }
        return new Robots(
                List.copyOf(denied),
                List.copyOf(allowed),
                selectedDelay,
                Instant.now().plusSeconds(21600));
    }

    private static String robotPath(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '%' && i + 2 < text.length())
                try {
                    int value = Integer.parseInt(text.substring(i + 1, i + 3), 16);
                    if (value >= 'a' && value <= 'z'
                            || value >= 'A' && value <= 'Z'
                            || value >= '0' && value <= '9'
                            || "-._~".indexOf(value) >= 0) result.append((char) value);
                    else result.append(String.format("%%%02X", value));
                    i += 2;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            if (c > 127) {
                int point = text.codePointAt(i);
                for (byte b :
                        new String(Character.toChars(point))
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    result.append(String.format("%%%02X", b & 255));
                if (Character.isSupplementaryCodePoint(point)) i++;
            } else result.append(c);
        }
        return result.toString();
    }

    record Robots(List<String> denied, List<String> allowed, long delayMs, Instant expires) {
        boolean permits(String path) {
            return longest(allowed, robotPath(path)) >= longest(denied, robotPath(path));
        }

        private int longest(List<String> rules, String path) {
            return rules.stream()
                    .filter(rule -> matches(rule, path))
                    .mapToInt(String::length)
                    .max()
                    .orElse(-1);
        }

        private boolean matches(String rule, String path) {
            rule = robotPath(rule);
            String pattern =
                    Arrays.stream(rule.replaceFirst("\\$$", "").split("\\*", -1))
                            .map(java.util.regex.Pattern::quote)
                            .collect(java.util.stream.Collectors.joining(".*"));
            return path.matches("^" + pattern + (rule.endsWith("$") ? "$" : ".*"));
        }
    }
}
