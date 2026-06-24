package com.edurite.ai.university;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edurite.university")
public class UniversityRegistryProperties {

    private List<UniversityRegistryEntry> registry = new ArrayList<>();
    private CrawlProperties crawl = new CrawlProperties();

    public List<UniversityRegistryEntry> getRegistry() {
        return registry;
    }

    public void setRegistry(List<UniversityRegistryEntry> registry) {
        this.registry = registry;
    }

    public CrawlProperties getCrawl() {
        return crawl;
    }

    public void setCrawl(CrawlProperties crawl) {
        this.crawl = crawl;
    }

    public static class CrawlProperties {

        private List<String> candidatePaths = new ArrayList<>(List.of(
                "/programmes", "/programs", "/study", "/studies", "/courses", "/course-finder",
                "/undergraduate", "/postgraduate", "/faculties", "/admissions", "/academic-programmes",
                "/qualifications", "/prospectus", "/fees", "/financial-aid", "/funding", "/apply"
        ));

        private int maxDiscoveredCandidatesPerUniversity = 24;
        private int maxFetchedPagesPerUniversity = 6;
        private int maxCrawlDepth = 1;
        private int timeoutMs = 10_000;
        private int maxFetchRetries = 2;
        private int retryBackoffMaxMs = 2_000;
        private int discoveryScoreThreshold = 3;
        private int maxBodySizeBytes = 1_500_000;
        private int fetchConcurrency = 6;
        private int overallFetchTimeoutMs = 18_000;

        public List<String> getCandidatePaths() {
            return candidatePaths;
        }

        public void setCandidatePaths(List<String> candidatePaths) {
            this.candidatePaths = candidatePaths;
        }

        public int getMaxDiscoveredCandidatesPerUniversity() {
            return maxDiscoveredCandidatesPerUniversity;
        }

        public void setMaxDiscoveredCandidatesPerUniversity(int maxDiscoveredCandidatesPerUniversity) {
            this.maxDiscoveredCandidatesPerUniversity = maxDiscoveredCandidatesPerUniversity;
        }

        public int getMaxFetchedPagesPerUniversity() {
            return maxFetchedPagesPerUniversity;
        }

        public void setMaxFetchedPagesPerUniversity(int maxFetchedPagesPerUniversity) {
            this.maxFetchedPagesPerUniversity = maxFetchedPagesPerUniversity;
        }

        public int getMaxCrawlDepth() {
            return maxCrawlDepth;
        }

        public void setMaxCrawlDepth(int maxCrawlDepth) {
            this.maxCrawlDepth = maxCrawlDepth;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxFetchRetries() {
            return maxFetchRetries;
        }

        public void setMaxFetchRetries(int maxFetchRetries) {
            this.maxFetchRetries = maxFetchRetries;
        }

        public int getRetryBackoffMaxMs() {
            return retryBackoffMaxMs;
        }

        public void setRetryBackoffMaxMs(int retryBackoffMaxMs) {
            this.retryBackoffMaxMs = retryBackoffMaxMs;
        }

        public int getDiscoveryScoreThreshold() {
            return discoveryScoreThreshold;
        }

        public void setDiscoveryScoreThreshold(int discoveryScoreThreshold) {
            this.discoveryScoreThreshold = discoveryScoreThreshold;
        }

        public int getMaxBodySizeBytes() {
            return maxBodySizeBytes;
        }

        public void setMaxBodySizeBytes(int maxBodySizeBytes) {
            this.maxBodySizeBytes = maxBodySizeBytes;
        }

        public int getFetchConcurrency() {
            return fetchConcurrency;
        }

        public void setFetchConcurrency(int fetchConcurrency) {
            this.fetchConcurrency = fetchConcurrency;
        }

        public int getOverallFetchTimeoutMs() {
            return overallFetchTimeoutMs;
        }

        public void setOverallFetchTimeoutMs(int overallFetchTimeoutMs) {
            this.overallFetchTimeoutMs = overallFetchTimeoutMs;
        }
    }

    public static class UniversityRegistryEntry {

        @NotBlank
        private String universityName;
        @NotBlank
        private String baseDomain;
        private List<String> allowedDomains = new ArrayList<>();
        private List<String> seedUrls = new ArrayList<>();
        private List<String> qualificationLevelsSupported = new ArrayList<>();
        private boolean active = true;
        private int crawlPriority = 100;

        public String getUniversityName() {
            return universityName;
        }

        public void setUniversityName(String universityName) {
            this.universityName = universityName;
        }

        public String getBaseDomain() {
            return baseDomain;
        }

        public void setBaseDomain(String baseDomain) {
            this.baseDomain = baseDomain;
        }

        public List<String> getAllowedDomains() {
            return allowedDomains;
        }

        public void setAllowedDomains(List<String> allowedDomains) {
            this.allowedDomains = allowedDomains;
        }

        public List<String> getSeedUrls() {
            return seedUrls;
        }

        public void setSeedUrls(List<String> seedUrls) {
            this.seedUrls = seedUrls;
        }

        public List<String> getQualificationLevelsSupported() {
            return qualificationLevelsSupported;
        }

        public void setQualificationLevelsSupported(List<String> qualificationLevelsSupported) {
            this.qualificationLevelsSupported = qualificationLevelsSupported;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getCrawlPriority() {
            return crawlPriority;
        }

        public void setCrawlPriority(int crawlPriority) {
            this.crawlPriority = crawlPriority;
        }
    }
}

