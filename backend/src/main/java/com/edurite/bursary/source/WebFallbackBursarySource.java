package com.edurite.bursary.source;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class WebFallbackBursarySource implements BursarySource {

    private static final String SEARCH_ENDPOINT = "https://html.duckduckgo.com/html/?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteBursarySearch/1.0; +https://edurite.org/bot)";

    private final BursarySourceProperties properties;

    public WebFallbackBursarySource(BursarySourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<BursaryResultDto> fetch(BursarySearchRequest request) {
        Map<String, BursaryResultDto> results = new LinkedHashMap<>();
        for (String domain : properties.getFallback().getTrustedDomains()) {
            for (String query : buildQueries(request, domain)) {
                if (results.size() >= properties.getFallback().getMaxResults()) {
                    return new ArrayList<>(results.values());
                }
                try {
                    Document document = Jsoup.connect(SEARCH_ENDPOINT + URLEncoder.encode(query, StandardCharsets.UTF_8))
                            .userAgent(USER_AGENT)
                            .timeout(10_000)
                            .get();
                    for (Element result : document.select("div.result")) {
                        Element link = result.selectFirst("a.result__a");
                        if (link == null) {
                            continue;
                        }
                        String href = normalizeUrl(link.absUrl("href"));
                        if (href.isBlank()) {
                            href = normalizeUrl(link.attr("href"));
                        }
                        if (href.isBlank() || !href.contains(domain)) {
                            continue;
                        }
                        String title = normalizeWhitespace(link.text());
                        if (title.isBlank()) {
                            continue;
                        }
                        String snippet = normalizeWhitespace(result.select("a.result__snippet, .result__snippet").text());
                        String key = (title + "|" + href).toLowerCase(Locale.ROOT);
                        results.putIfAbsent(key, new BursaryResultDto(
                                href,
                                title,
                                providerFromDomain(domain),
                                snippet.isBlank() ? "Not found in fetched sources" : snippet,
                                safe(request.qualificationLevel()),
                                safe(request.region()),
                                safe(request.eligibility()),
                                null,
                                href,
                                sourceType(),
                                55,
                                List.of(href),
                                false,
                                true,
                                "Qualification, deadline, and benefits were not confirmed on an official provider page."
                        ));
                        if (results.size() >= properties.getFallback().getMaxResults()) {
                            break;
                        }
                    }
                } catch (IOException ignored) {
                    // Keep fallback resilient; callers will still have provider data and deterministic guidance.
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    @Override
    public String sourceType() {
        return "TRUSTED_PUBLIC_FALLBACK";
    }

    private List<String> buildQueries(BursarySearchRequest request, String domain) {
        String qualification = safe(request.qualificationLevel());
        String region = safe(request.region());
        String eligibility = safe(request.eligibility());
        String query = safe(request.query());
        return List.of(
                "site:" + domain + " bursary " + query + " " + qualification,
                "site:" + domain + " scholarship " + query + " " + region,
                "site:" + domain + " funding " + query + " " + eligibility
        );
    }

    private String providerFromDomain(String domain) {
        return domain.replace("www.", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String normalizeUrl(String value) {
        return value == null ? "" : value.trim();
    }
}

