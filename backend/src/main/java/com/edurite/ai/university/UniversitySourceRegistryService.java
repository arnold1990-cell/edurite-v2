package com.edurite.ai.university;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceRegistryService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourceRegistryService.class);

    private final UniversityRegistryProperties properties;
    private final UniversityUrlNormalizer urlNormalizer;

    public UniversitySourceRegistryService(UniversityRegistryProperties properties, UniversityUrlNormalizer urlNormalizer) {
        this.properties = properties;
        this.urlNormalizer = urlNormalizer;
    }

    @PostConstruct
    void logRegistryStatus() {
        int configured = configuredUniversityCount();
        int active = getActiveUniversities().size();
        long seedUrls = getActiveUniversities().stream()
                .map(UniversityRegistryProperties.UniversityRegistryEntry::getSeedUrls)
                .filter(Objects::nonNull)
                .mapToLong(List::size)
                .sum();
        log.info("University registry initialised: configuredUniversities={}, activeUniversities={}, seedUrls={}", configured, active, seedUrls);
        if (active == 0) {
            log.error("University registry is empty or all entries are inactive. University AI guidance cannot discover official sources until configuration is fixed.");
        }
    }

    public List<UniversityRegistryProperties.UniversityRegistryEntry> getActiveUniversities() {
        return properties.getRegistry().stream()
                .filter(UniversityRegistryProperties.UniversityRegistryEntry::isActive)
                .sorted(Comparator.comparingInt(UniversityRegistryProperties.UniversityRegistryEntry::getCrawlPriority)
                        .thenComparing(UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName))
                .toList();
    }

    public List<String> getDefaultSources() {
        List<String> defaultSources = getActiveUniversities().stream()
                .flatMap(entry -> entry.getSeedUrls().stream())
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(100)
                .toList();
        if (defaultSources.isEmpty()) {
            log.warn("University registry returned zero default sources: configuredUniversities={}, activeUniversities={}",
                    configuredUniversityCount(), getActiveUniversities().size());
        }
        return defaultSources;
    }

    public List<String> getFallbackSources(int maxUrls) {
        int limit = Math.max(1, maxUrls);
        List<String> fallbackSources = getActiveUniversities().stream()
                .flatMap(entry -> entry.getSeedUrls().stream())
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(limit)
                .toList();
        if (fallbackSources.isEmpty()) {
            log.warn("University registry fallback sources are empty: limit={}, configuredUniversities={}", limit, configuredUniversityCount());
        }
        return fallbackSources;
    }

    public boolean isAllowedUrl(String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return properties.getRegistry().stream().anyMatch(university -> matchesAllowedDomain(host, university));
    }

    public boolean isAllowedUrlForUniversity(String universityName, String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return properties.getRegistry().stream()
                .filter(university -> university.getUniversityName().equalsIgnoreCase(universityName))
                .anyMatch(university -> matchesAllowedDomain(host, university));
    }

    public List<String> deduplicate(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        return urls.stream()
                .filter(Objects::nonNull)
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    public int configuredUniversityCount() {
        return properties.getRegistry().size();
    }

    private String host(String url) {
        try {
            URI uri = URI.create(urlNormalizer.normalize(url));
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean matchesAllowedDomain(String normalizedHost, UniversityRegistryProperties.UniversityRegistryEntry university) {
        Set<String> domains = new LinkedHashSet<>();
        domains.add(university.getBaseDomain().toLowerCase(Locale.ROOT));
        domains.addAll(university.getAllowedDomains().stream().map(value -> value.toLowerCase(Locale.ROOT)).toList());
        return domains.stream().anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
    }
}

