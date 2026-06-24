package com.edurite.ai.university;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final Logger log = LoggerFactory.getLogger(MultiUniversityPageFetcherService.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "en-ZA,en;q=0.9,en-US;q=0.8";
    private static final String ACCEPT_ENCODING = "gzip, deflate";
    private static final int MAX_CHARS_PER_PAGE = 8_000;
    private static final int MAX_LINKS_PER_SEED = 30;
    private static final int MAX_HEADINGS = 8;
    private static final int MIN_USEFUL_TEXT_LENGTH = 250;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(400);

    private static final Map<String, Integer> LINK_RELEVANCE_TERMS = Map.ofEntries(
            Map.entry("programme", 5),
            Map.entry("program", 5),
            Map.entry("course", 3),
            Map.entry("study", 3),
            Map.entry("faculty", 4),
            Map.entry("admission", 4),
            Map.entry("requirements", 4),
            Map.entry("undergraduate", 4),
            Map.entry("postgraduate", 3),
            Map.entry("qualification", 3),
            Map.entry("degree", 3),
            Map.entry("diploma", 2),
            Map.entry("humanities", 4),
            Map.entry("arts", 4),
            Map.entry("prospectus", 4),
            Map.entry("handbook", 4),
            Map.entry("apply", 3)
    );

    private static final List<String> CONTENT_SELECTORS = List.of(
            "main", "article", "[role=main]", ".main-content", ".content", ".page-content", ".entry-content", ".content-wrapper"
    );

    private final UniversitySourceRegistryService registryService;
    private final UniversityPageClassifier classifier;
    private final UniversityUrlNormalizer urlNormalizer;
    private final UniversityRegistryProperties properties;

    public MultiUniversityPageFetcherService(
            UniversitySourceRegistryService registryService,
            UniversityPageClassifier classifier,
            UniversityUrlNormalizer urlNormalizer,
            UniversityRegistryProperties properties
    ) {
        this.registryService = registryService;
        this.classifier = classifier;
        this.urlNormalizer = urlNormalizer;
        this.properties = properties;
    }

    public List<String> discoverCandidateUrls(UniversityRegistryProperties.UniversityRegistryEntry university, int maxUrls) {
        int cappedMaxUrls = Math.min(Math.max(maxUrls, 1), crawl().getMaxDiscoveredCandidatesPerUniversity());
        String homepage = primaryHomepage(university);
        if (homepage.isBlank()) {
            return List.of();
        }

        Map<String, Integer> scored = new LinkedHashMap<>();
        scored.put(homepage, homepageScore(homepage));

        List<String> discoveredLinks = extractRankedInternalLinks(university, homepage, cappedMaxUrls);
        discoveredLinks.forEach(url -> scored.merge(url, scoreCandidate(url, ""), Math::max));

        if (scored.size() < cappedMaxUrls) {
            addFallbackPaths(scored, university, homepage, cappedMaxUrls);
        }

        List<String> results = scored.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .limit(cappedMaxUrls)
                .toList();

        log.info("University crawler discovery completed: university={}, homepage={}, discoveredUrls={}, requestedSources={}",
                university.getUniversityName(), homepage, results.size(), cappedMaxUrls);
        return results;
    }

    public int maxFetchBudgetPerUniversity() {
        return Math.max(1, crawl().getMaxFetchedPagesPerUniversity());
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        List<String> eligibleUrls = new ArrayList<>();
        List<Integer> eligibleResultIndexes = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<UniversitySourcePageResult> orderedResults = new ArrayList<>();

        for (String rawUrl : urls) {
            String url = urlNormalizer.normalize(rawUrl);
            if (url.isBlank() || !seen.add(url)) {
                continue;
            }
            int resultIndex = orderedResults.size();
            orderedResults.add(null);
            if (!registryService.isAllowedUrl(url)) {
                orderedResults.set(resultIndex, failedResult(url, url, "URL domain is not allowlisted", UniversityCrawlFailureType.ACCESS_DENIED));
                continue;
            }
            eligibleUrls.add(url);
            eligibleResultIndexes.add(resultIndex);
        }

        if (eligibleUrls.isEmpty()) {
            return List.copyOf(orderedResults);
        }

        int concurrency = Math.max(1, Math.min(crawl().getFetchConcurrency(), eligibleUrls.size()));
        long overallTimeoutMs = Math.max(crawl().getTimeoutMs(), crawl().getOverallFetchTimeoutMs());
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(overallTimeoutMs);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CompletionService<IndexedResult> completionService = new ExecutorCompletionService<>(executor);
        List<UniversitySourcePageResult> ordered = new ArrayList<>(Collections.nCopies(eligibleUrls.size(), null));

        try {
            for (int index = 0; index < eligibleUrls.size(); index++) {
                final int taskIndex = index;
                final String taskUrl = eligibleUrls.get(index);
                completionService.submit(() -> {
                    try {
                        return new IndexedResult(taskIndex, fetchSingle(taskUrl));
                    } catch (RuntimeException ex) {
                        String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                                ? "unknown error"
                                : ex.getMessage();
                        return new IndexedResult(taskIndex, failedResult(
                                taskUrl,
                                taskUrl,
                                "Fetch worker crashed: " + reason,
                                UniversityCrawlFailureType.FETCH_ERROR
                        ));
                    }
                });
            }

            int completed = 0;
            while (completed < eligibleUrls.size()) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    break;
                }

                Future<IndexedResult> future = completionService.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (future == null) {
                    break;
                }

                try {
                    IndexedResult result = future.get();
                    ordered.set(result.index(), result.result());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("University fetch wait interrupted while resolving worker result.");
                    break;
                } catch (Exception ex) {
                    log.warn("University fetch worker failed while awaiting result: message={}", ex.getMessage());
                }
                completed++;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("University fetch interrupted while waiting for URL results: pendingUrls={}", eligibleUrls.size());
        } finally {
            executor.shutdownNow();
        }

        for (int index = 0; index < eligibleUrls.size(); index++) {
            if (ordered.get(index) != null) {
                continue;
            }
            String timedOutUrl = eligibleUrls.get(index);
            ordered.set(index, failedResult(
                    timedOutUrl,
                    timedOutUrl,
                    "Fetch timed out before a terminal source result was produced within the overall pipeline budget.",
                    UniversityCrawlFailureType.TIMEOUT
            ));
        }

        for (int index = 0; index < ordered.size(); index++) {
            int resultIndex = eligibleResultIndexes.get(index);
            orderedResults.set(resultIndex, ordered.get(index));
        }
        return List.copyOf(orderedResults);
    }

    private record IndexedResult(int index, UniversitySourcePageResult result) {
    }

    private UniversitySourcePageResult fetchSingle(String requestedUrl) {
        IOException lastError = null;
        int maxRetries = Math.max(1, crawl().getMaxFetchRetries());
        String candidateUrl = requestedUrl;
        boolean attemptedHttpFallback = false;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("University fetch start: url={}, attempt={}", candidateUrl, attempt);
                Document document = connect(candidateUrl);
                String finalUrl = urlNormalizer.normalize(document.location());
                String title = truncate(document.title(), 200);
                String contentType = document.connection() == null || document.connection().response() == null
                        ? "unknown"
                        : document.connection().response().contentType();
                if (looksLikeLoginPage(document)) {
                    log.info("University fetch rejected login-style page: requestedUrl={}, finalUrl={}, title={}", requestedUrl, finalUrl, title);
                    return failedResult(requestedUrl, finalUrl, "Rejected login-style page", UniversityCrawlFailureType.ACCESS_DENIED, title, List.of());
                }
                String visibleText = extractVisibleBodyText(document);
                List<String> headings = extractHeadings(document);
                log.info("University fetch completed: requestedUrl={}, finalUrl={}, contentType={}, responseLength={}, title={}",
                        requestedUrl, finalUrl, contentType, visibleText.length(), title);
                if (visibleText.isBlank()) {
                    return failedResult(requestedUrl, finalUrl, "No meaningful visible body text was extracted", UniversityCrawlFailureType.EMPTY_CONTENT, title, headings);
                }
                if (looksLikeJavascriptShell(title, visibleText, headings)) {
                    return failedResult(requestedUrl, finalUrl, "Page appears to require client-side rendering and exposed too little HTML text", UniversityCrawlFailureType.EMPTY_CONTENT, title, headings);
                }
                UniversityPageType pageType = classifier.classify(finalUrl, title, visibleText);
                Set<String> keywords = classifier.extractKeywords(title, visibleText);
                if (visibleText.length() < MIN_USEFUL_TEXT_LENGTH
                        && !isThinButUsefulPage(finalUrl, title, visibleText, headings, pageType, keywords)
                        && !isFallbackAcceptablePage(finalUrl, title, visibleText, headings)) {
                    return failedResult(requestedUrl, finalUrl, "Visible body text was too thin for reliable grounding", UniversityCrawlFailureType.EMPTY_CONTENT, title, headings);
                }
                if (classifier.shouldSkipPage(finalUrl, title, visibleText) && !isFallbackAcceptablePage(finalUrl, title, visibleText, headings)) {
                    return failedResult(requestedUrl, finalUrl,
                            "Page was deprioritised because it does not look like a useful official programme or admissions page",
                            UniversityCrawlFailureType.FETCH_ERROR, title, headings);
                }
                return new UniversitySourcePageResult(finalUrl, title, pageType, visibleText, keywords, headings, true, null, null);
            } catch (IOException ex) {
                lastError = ex;
                log.warn("University fetch failed: url={}, attempt={}, retryable={}, message={}", candidateUrl, attempt, isRetryable(ex), ex.getMessage());
                if (ex instanceof SSLException
                        && !attemptedHttpFallback
                        && candidateUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
                    String httpFallbackUrl = "http://" + candidateUrl.substring("https://".length());
                    log.info("University fetch switching to HTTP fallback after SSL failure: httpsUrl={}, httpUrl={}",
                            candidateUrl, httpFallbackUrl);
                    candidateUrl = httpFallbackUrl;
                    attemptedHttpFallback = true;
                    continue;
                }
                if (!isRetryable(ex) || attempt >= maxRetries) {
                    break;
                }
                sleep(backoffMillis(attempt));
            } catch (RuntimeException ex) {
                log.error("University fetch crashed: url={}, message={}", requestedUrl, ex.getMessage(), ex);
                return failedResult(requestedUrl, requestedUrl, "Fetch crashed before content could be processed: " + ex.getMessage(), UniversityCrawlFailureType.FETCH_ERROR);
            }
        }
        String reason = buildFailureReason(lastError);
        return failedResult(requestedUrl, requestedUrl, reason, classifyFailure(lastError));
    }

    private String extractVisibleBodyText(Document document) {
        Document clone = document.clone();
        clone.select("script,style,noscript,svg,canvas,iframe,header,footer,nav,form,aside,.cookie,.cookies,.breadcrumbs,.menu,.modal,.search,.newsletter,.social,.share,.advert,.ad,.popup,.drawer,.banner,.pagination").remove();
        Element preferred = null;
        for (String selector : CONTENT_SELECTORS) {
            preferred = clone.selectFirst(selector);
            if (preferred != null) {
                break;
            }
        }
        Element body = preferred != null ? preferred : clone.body();
        if (body == null) {
            return "";
        }
        String text = body.text().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return truncate(text, MAX_CHARS_PER_PAGE);
    }

    private List<String> extractHeadings(Document document) {
        List<String> headings = new ArrayList<>();
        for (Element heading : document.select("h1, h2, h3")) {
            String text = truncate(heading.text(), 180);
            if (!text.isBlank() && headings.stream().noneMatch(text::equalsIgnoreCase)) {
                headings.add(text);
            }
            if (headings.size() >= MAX_HEADINGS) {
                break;
            }
        }
        return headings;
    }

    private boolean looksLikeLoginPage(Document document) {
        String title = (document.title() == null ? "" : document.title()).toLowerCase(Locale.ROOT);
        String bodyText = (document.body() == null ? document.text() : document.body().text()).toLowerCase(Locale.ROOT);
        boolean titleLooksLikeLogin = title.contains("sign in")
                || title.contains("log in")
                || title.contains("login")
                || title.contains("single sign on")
                || title.contains("sso");
        boolean hasPasswordInput = !document.select("input[type=password]").isEmpty();
        boolean hasLoginFormHints = !document.select("form[action*=login], form[action*=signin], form[id*=login], form[class*=login]").isEmpty();
        int authTermMatches = 0;
        if (bodyText.contains("sign in")) authTermMatches++;
        if (bodyText.contains("log in")) authTermMatches++;
        if (bodyText.contains("password")) authTermMatches++;
        if (bodyText.contains("single sign on") || bodyText.contains("sso")) authTermMatches++;
        boolean denseAuthPage = authTermMatches >= 2 && bodyText.length() < 2500;
        return titleLooksLikeLogin || hasPasswordInput || hasLoginFormHints || denseAuthPage;
    }

    private boolean looksLikeJavascriptShell(String title, String visibleText, List<String> headings) {
        String combined = (title + " " + String.join(" ", headings) + " " + visibleText).toLowerCase(Locale.ROOT);
        return visibleText.length() < 120 && (combined.contains("enable javascript") || combined.contains("loading") || combined.contains("app shell"));
    }

    private List<String> extractRankedInternalLinks(UniversityRegistryProperties.UniversityRegistryEntry university,
                                                    String seedUrl,
                                                    int maxUrls) {
        try {
            Document document = connect(seedUrl);
            Map<String, Integer> linkScores = new LinkedHashMap<>();
            for (Element link : document.select("a[href]")) {
                String normalized = urlNormalizer.normalize(link.absUrl("href"));
                if (normalized.isBlank()) {
                    continue;
                }
                if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), normalized) || !isSameHost(seedUrl, normalized)) {
                    continue;
                }
                String anchorText = link.text();
                int score = scoreCandidate(normalized, anchorText);
                if (score <= 0 || classifier.shouldDeprioritizeLink(normalized, anchorText)) {
                    log.debug("University candidate skipped: university={}, url={}, reason=low-score-or-deprioritised, score={}",
                            university.getUniversityName(), normalized, score);
                    continue;
                }
                linkScores.merge(normalized, score, Math::max);
                log.info("University candidate discovered: university={}, url={}, score={}, reason=homepage-link", university.getUniversityName(), normalized, score);
            }

            return linkScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .limit(Math.max(0, Math.min(maxUrls, MAX_LINKS_PER_SEED)))
                    .toList();
        } catch (IOException ex) {
            log.warn("University crawler could not extract internal links: university={}, seedUrl={}, message={}",
                    university.getUniversityName(), seedUrl, ex.getMessage());
            return List.of();
        }
    }

    private void addFallbackPaths(Map<String, Integer> scored,
                                  UniversityRegistryProperties.UniversityRegistryEntry university,
                                  String homepage,
                                  int cappedMaxUrls) {
        URI seedUri;
        try {
            seedUri = URI.create(homepage);
        } catch (RuntimeException ex) {
            return;
        }
        if (seedUri.getScheme() == null || seedUri.getHost() == null) {
            return;
        }
        String prefix = seedUri.getScheme() + "://" + seedUri.getHost();
        if (seedUri.getPort() != -1) {
            prefix += ":" + seedUri.getPort();
        }
        for (String candidatePath : crawl().getCandidatePaths()) {
            String path = candidatePath.startsWith("/") ? candidatePath : "/" + candidatePath;
            String candidate = urlNormalizer.normalize(prefix + path);
            if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), candidate)) {
                continue;
            }
            int score = scoreCandidate(candidate, path) - 2;
            if (score < crawl().getDiscoveryScoreThreshold()) {
                continue;
            }
            scored.putIfAbsent(candidate, score);
            log.info("University candidate discovered: university={}, url={}, score={}, reason=configured-fallback-path", university.getUniversityName(), candidate, score);
            if (scored.size() >= cappedMaxUrls) {
                break;
            }
        }
    }

    private String primaryHomepage(UniversityRegistryProperties.UniversityRegistryEntry university) {
        return university.getSeedUrls().stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .filter(this::isHomepage)
                .findFirst()
                .orElseGet(() -> university.getSeedUrls().stream()
                        .map(urlNormalizer::normalize)
                        .filter(url -> !url.isBlank())
                        .findFirst()
                        .orElse(""));
    }

    private int homepageScore(String homepage) {
        return scoreCandidate(homepage, "homepage") + 8;
    }

    private int scoreCandidate(String url, String anchorText) {
        String context = (url + " " + anchorText).toLowerCase(Locale.ROOT);
        int score = 0;
        for (Map.Entry<String, Integer> entry : LINK_RELEVANCE_TERMS.entrySet()) {
            if (context.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        if (isHomepage(url)) {
            score += 6;
        }
        if (context.contains("faculty") || context.contains("college") || context.contains("school")) {
            score += 2;
        }
        if (context.contains("news") || context.contains("event") || context.contains("privacy") || context.contains("research") || context.contains("blog") || context.contains("search")) {
            score -= 6;
        }
        return score;
    }

    private boolean isSameHost(String baseUrl, String candidateUrl) {
        try {
            URI base = URI.create(baseUrl);
            URI candidate = URI.create(candidateUrl);
            if (base.getHost() == null || candidate.getHost() == null) {
                return false;
            }
            return base.getHost().equalsIgnoreCase(candidate.getHost());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isThinButUsefulPage(String url,
                                        String title,
                                        String visibleText,
                                        List<String> headings,
                                        UniversityPageType pageType,
                                        Set<String> keywords) {
        if (pageType != UniversityPageType.UNKNOWN || !keywords.isEmpty()) {
            return true;
        }
        String headingContext = String.join(" ", headings);
        String combined = (title + " " + headingContext + " " + visibleText + " " + url).toLowerCase(Locale.ROOT);
        return combined.contains("programme")
                || combined.contains("program")
                || combined.contains("degree")
                || combined.contains("admission")
                || combined.contains("qualification")
                || combined.contains("faculty")
                || isHomepage(url);
    }

    private boolean isFallbackAcceptablePage(String url, String title, String visibleText, List<String> headings) {
        String combined = (url + " " + title + " " + String.join(" ", headings) + " " + visibleText).toLowerCase(Locale.ROOT);
        return combined.contains("admission")
                || combined.contains("apply")
                || combined.contains("programme")
                || combined.contains("program")
                || combined.contains("qualification")
                || combined.contains("undergraduate")
                || combined.contains("postgraduate")
                || combined.contains("faculty")
                || combined.contains("prospectus")
                || isHomepage(url);
    }

    private boolean isHomepage(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return path == null || path.isBlank() || "/".equals(path);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Document connect(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept-Encoding", ACCEPT_ENCODING)
                .timeout(crawl().getTimeoutMs())
                .followRedirects(true)
                .ignoreContentType(false)
                .maxBodySize(crawl().getMaxBodySizeBytes());
        return connection.get();
    }

    private boolean isRetryable(IOException ex) {
        if (ex instanceof MalformedURLException || ex instanceof UnsupportedMimeTypeException) {
            return false;
        }
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            return statusCode == 403 || statusCode == 408 || statusCode == 429 || statusCode >= 500;
        }
        return true;
    }

    private long backoffMillis(int attempt) {
        long factor = 1L << (attempt - 1);
        return Math.min(INITIAL_BACKOFF.toMillis() * factor, crawl().getRetryBackoffMaxMs());
    }

    private UniversityCrawlFailureType classifyFailure(IOException ex) {
        if (ex == null) {
            return UniversityCrawlFailureType.FETCH_ERROR;
        }
        if (ex instanceof SocketTimeoutException) {
            return UniversityCrawlFailureType.TIMEOUT;
        }
        if (ex instanceof SSLException) {
            return UniversityCrawlFailureType.SSL_ERROR;
        }
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            if (statusCode == 404) {
                return UniversityCrawlFailureType.NOT_FOUND;
            }
            if (statusCode == 401 || statusCode == 403) {
                return UniversityCrawlFailureType.ACCESS_DENIED;
            }
        }
        return UniversityCrawlFailureType.FETCH_ERROR;
    }

    private String buildFailureReason(IOException ex) {
        if (ex == null) {
            return "Unknown fetch failure";
        }
        if (ex instanceof HttpStatusException statusException) {
            return "HTTP " + statusException.getStatusCode() + " while fetching source";
        }
        return ex.getMessage() == null || ex.getMessage().isBlank() ? "Unknown fetch failure" : ex.getMessage();
    }

    private UniversitySourcePageResult failedResult(String requestedUrl,
                                                    String finalUrl,
                                                    String reason,
                                                    UniversityCrawlFailureType failureType) {
        return failedResult(requestedUrl, finalUrl, reason, failureType, "", List.of());
    }

    private UniversitySourcePageResult failedResult(String requestedUrl,
                                                    String finalUrl,
                                                    String reason,
                                                    UniversityCrawlFailureType failureType,
                                                    String title,
                                                    List<String> headings) {
        String terminalUrl = urlNormalizer.normalize(finalUrl);
        log.info("University fetch terminal failure: requestedUrl={}, finalUrl={}, failureType={}, reason={}",
                requestedUrl, terminalUrl, failureType, reason);
        return new UniversitySourcePageResult(terminalUrl.isBlank() ? urlNormalizer.normalize(requestedUrl) : terminalUrl,
                title == null ? "" : title,
                UniversityPageType.UNKNOWN,
                "",
                Set.of(),
                headings == null ? List.of() : headings,
                false,
                reason,
                failureType);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private UniversityRegistryProperties.CrawlProperties crawl() {
        return properties.getCrawl();
    }
}

