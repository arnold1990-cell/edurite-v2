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
        loadGovernmentSnapshot();
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
                .filter(UniversityRegistryProperties.UniversityRegistryEntry::isCrawlingPermitted)
                .filter(this::registrationAllowsResearch)
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
        if (Set.of("www.education.gov.za", "www.dhet.gov.za").contains(host)) return true;
        return getActiveUniversities().stream().anyMatch(university -> matchesAllowedDomain(host, university));
    }

    public boolean isAllowedUrlForUniversity(String universityName, String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return getActiveUniversities().stream()
                .filter(university -> canonical(university.getUniversityName()).equals(canonical(universityName)))
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

    public List<UniversityRegistryProperties.UniversityRegistryEntry> getRegistryEntries() {
        return List.copyOf(properties.getRegistry());
    }

    public void observeOfficialLinks(UniversityRegistryProperties.UniversityRegistryEntry entry, java.util.Map<String,String> links) {
        var faculties=new LinkedHashSet<>(entry.getFacultyWebsites());
        var catalogues=new LinkedHashSet<>(entry.getProgrammeCatalogueUrls());
        links.forEach((url,label) -> {
            if(!isAllowedUrlForUniversity(entry.getUniversityName(),url)) return;
            String lower=label.toLowerCase(Locale.ROOT);
            if(lower.contains("facult") && faculties.size()<50) faculties.add(url);
            if(lower.matches(".*(programme|qualification|course finder|course catalogue).*" ) && catalogues.size()<100) catalogues.add(url);
            if(entry.getAdmissionsWebsite()==null && lower.contains("admission")) entry.setAdmissionsWebsite(url);
            if(entry.getApplicationPortalUrl()==null && lower.matches(".*(apply|application portal|online application).*")) entry.setApplicationPortalUrl(url);
        });
        entry.setFacultyWebsites(List.copyOf(faculties));entry.setProgrammeCatalogueUrls(List.copyOf(catalogues));
    }

    private boolean registrationAllowsResearch(UniversityRegistryProperties.UniversityRegistryEntry entry) {
        return !"PRIVATE_HIGHER_EDUCATION".equals(entry.getInstitutionType())
                || ("REGISTERED".equals(entry.getRegistrationStatus())
                    && entry.getRegistrationNumber() != null
                    && entry.getLastVerificationDate() != null
                    && entry.getLastVerificationDate().isAfter(java.time.LocalDate.now().minusDays(90)));
    }

    /** Merge government evidence without discarding reviewed institution-specific seeds/restrictions. */
    public synchronized void mergeGovernmentEntries(List<UniversityRegistryProperties.UniversityRegistryEntry> entries) {
        var merged = new java.util.ArrayList<>(properties.getRegistry());
        for (var incoming : entries) {
            var existing = merged.stream().filter(e -> canonical(e.getUniversityName()).equals(canonical(incoming.getUniversityName()))
                    || e.getBaseDomain().equalsIgnoreCase(incoming.getBaseDomain())).findFirst().orElse(null);
            if (existing != null) {
                var seeds = new LinkedHashSet<>(existing.getSeedUrls());
                seeds.addAll(incoming.getSeedUrls());
                incoming.setSeedUrls(List.copyOf(seeds));
                var domains = new LinkedHashSet<>(existing.getAllowedDomains());
                domains.add(existing.getBaseDomain());
                domains.add(incoming.getBaseDomain());
                incoming.setAllowedDomains(List.copyOf(domains));
                incoming.setActive(existing.isActive());
                incoming.setCrawlingPermitted(existing.isCrawlingPermitted());
                incoming.setAdmissionsWebsite(existing.getAdmissionsWebsite());
                incoming.setFacultyWebsites(existing.getFacultyWebsites());
                incoming.setProgrammeCatalogueUrls(existing.getProgrammeCatalogueUrls());
                incoming.setApplicationPortalUrl(existing.getApplicationPortalUrl());
                merged.remove(existing);
            }
            merged.add(incoming);
        }
        properties.setRegistry(List.copyOf(merged));
    }

    private static String canonical(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("\\([^)]*\\)", "").replaceAll("[^a-z0-9]", "");
    }

    private void loadGovernmentSnapshot() {
        try (var source = getClass().getResourceAsStream("/university-registry/sa-public-universities.json")) {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
            List<UniversityRegistryProperties.UniversityRegistryEntry> entries = mapper.readValue(source,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            mergeGovernmentEntries(entries);
            for (var entry : properties.getRegistry()) {
                if (entry.getInstitutionType() == null && !entry.getBaseDomain().endsWith(".bw")
                        && !entry.getBaseDomain().endsWith(".zw")
                        && (entry.getCountry()==null || "South Africa".equals(entry.getCountry()))) {
                    entry.setCountry("South Africa");
                    entry.setInstitutionType("PRIVATE_HIGHER_EDUCATION");
                    entry.setRegistrySourceUrl("https://www.dhet.gov.za/SitePages/DocRegisters.aspx");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Official public university registry snapshot cannot be loaded", ex);
        }
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

