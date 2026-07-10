package com.edurite.institution.universityinfo.service;

import com.edurite.ai.university.CrawlStatus;
import com.edurite.ai.university.CrawledUniversityPage;
import com.edurite.ai.university.CrawledUniversityPageRepository;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversityRegistryProperties;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityAdmissionRequirementResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityAdmissionRequirementsViewResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityInfoInstitutionResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityProgrammesViewResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityProgrammeResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityRefreshResponse;
import com.edurite.institution.universityinfo.dto.UniversityInfoDtos.UniversityRetrievalLogResponse;
import com.edurite.institution.universityinfo.entity.UniversityAdmissionRequirement;
import com.edurite.institution.universityinfo.entity.UniversityProgramme;
import com.edurite.institution.universityinfo.entity.UniversityRetrievalLog;
import com.edurite.institution.universityinfo.repository.UniversityAdmissionRequirementRepository;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.institution.universityinfo.repository.UniversityRetrievalLogRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversityInfoService {

    private static final Pattern PROGRAMME_PATTERN = Pattern.compile("\\b((?:Bachelor|Diploma|Higher Certificate|Certificate|Advanced Diploma|Postgraduate Diploma|Master|Doctor)\\s+(?:of|in)\\s+[A-Z][A-Za-z&(),/\\- ]{2,100})");
    private static final Pattern APS_PATTERN = Pattern.compile("\\bAPS\\b[^0-9]{0,20}(\\d{2})");
    private static final List<String> SUBJECT_KEYWORDS = List.of(
            "Mathematics", "Mathematical Literacy", "English", "Physical Sciences", "Life Sciences",
            "Accounting", "Geography", "History", "Business Studies", "Information Technology"
    );

    private final UniversitySlugService slugService;
    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final CrawledUniversityPageRepository crawledUniversityPageRepository;
    private final UniversityProgrammeRepository programmeRepository;
    private final UniversityAdmissionRequirementRepository requirementRepository;
    private final UniversityRetrievalLogRepository retrievalLogRepository;

    public UniversityInfoService(
            UniversitySlugService slugService,
            UniversitySourceRegistryService registryService,
            MultiUniversityPageFetcherService pageFetcherService,
            CrawledUniversityPageRepository crawledUniversityPageRepository,
            UniversityProgrammeRepository programmeRepository,
            UniversityAdmissionRequirementRepository requirementRepository,
            UniversityRetrievalLogRepository retrievalLogRepository
    ) {
        this.slugService = slugService;
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.crawledUniversityPageRepository = crawledUniversityPageRepository;
        this.programmeRepository = programmeRepository;
        this.requirementRepository = requirementRepository;
        this.retrievalLogRepository = retrievalLogRepository;
    }

    @Transactional(readOnly = true)
    public UniversityProgrammesViewResponse getProgrammes(String slug) {
        Institution institution = slugService.requireBySlug(slug);
        List<UniversityProgramme> stored = programmeRepository.findByInstitutionIdAndActiveTrueOrderByFacultyAscNameAsc(institution.getId());
        List<UniversityRetrievalLogResponse> retrievalLogs = logs(institution.getId());
        if (!stored.isEmpty()) {
            return new UniversityProgrammesViewResponse(
                    institutionSummary(institution),
                    "STORED_VERIFIED",
                    "Verified programme information retrieved from EduRite storage.",
                    stored.stream().map(UniversityProgramme::getLastVerifiedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(institution.getUpdatedAt()),
                    true,
                    false,
                    officialDomains(institution.getName()),
                    distinctValues(stored.stream().map(UniversityProgramme::getFaculty).toList()),
                    distinctValues(stored.stream().map(UniversityProgramme::getQualificationType).toList()),
                    distinctValues(stored.stream().map(UniversityProgramme::getStudyMode).toList()),
                    stored.stream().map(this::toProgrammeResponse).toList(),
                    retrievalLogs
            );
        }

        return new UniversityProgrammesViewResponse(
                institutionSummary(institution),
                "FALLBACK_ONLY",
                "We could not retrieve verified programme information for this institution at the moment. Please use the official university website to view the latest programmes.",
                latestLogTime(retrievalLogs),
                !retrievalLogs.isEmpty(),
                true,
                officialDomains(institution.getName()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                retrievalLogs
        );
    }

    @Transactional(readOnly = true)
    public UniversityAdmissionRequirementsViewResponse getAdmissionRequirements(String slug) {
        Institution institution = slugService.requireBySlug(slug);
        List<UniversityAdmissionRequirement> stored = requirementRepository.findByInstitutionIdAndActiveTrueOrderByProgrammeNameAscRequirementTitleAsc(institution.getId());
        List<UniversityRetrievalLogResponse> retrievalLogs = logs(institution.getId());
        if (!stored.isEmpty()) {
            return new UniversityAdmissionRequirementsViewResponse(
                    institutionSummary(institution),
                    "STORED_VERIFIED",
                    "Verified admission requirements retrieved from EduRite storage.",
                    stored.stream().map(UniversityAdmissionRequirement::getLastVerifiedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(institution.getUpdatedAt()),
                    true,
                    false,
                    officialDomains(institution.getName()),
                    stored.stream().map(this::toRequirementResponse).toList(),
                    retrievalLogs
            );
        }

        return new UniversityAdmissionRequirementsViewResponse(
                institutionSummary(institution),
                "FALLBACK_ONLY",
                "We could not retrieve verified admission requirements for this institution. Requirements may change, so please confirm the latest information on the official university website.",
                latestLogTime(retrievalLogs),
                !retrievalLogs.isEmpty(),
                true,
                officialDomains(institution.getName()),
                List.of(),
                retrievalLogs
        );
    }

    @Transactional
    public UniversityRefreshResponse refreshUniversityData(String slug) {
        Institution institution = slugService.requireBySlug(slug);
        Optional<UniversityRegistryProperties.UniversityRegistryEntry> registryEntry = registryEntry(institution.getName());
        if (registryEntry.isEmpty()) {
            logRetrieval(institution, institution.getWebsite(), "FAILED", "No allowlisted official university registry entry exists for this institution.", "MANUAL_REFRESH");
            return new UniversityRefreshResponse(slug, "FAILED", "No allowlisted official source configuration exists for this university.", OffsetDateTime.now());
        }

        List<String> candidateUrls = pageFetcherService.discoverCandidateUrls(registryEntry.get(), pageFetcherService.maxFetchBudgetPerUniversity());
        if (candidateUrls.isEmpty()) {
            logRetrieval(institution, institution.getWebsite(), "FAILED", "No candidate official URLs were discovered for this institution.", "MANUAL_REFRESH");
            return new UniversityRefreshResponse(slug, "FAILED", "No official programme or admissions pages could be discovered.", OffsetDateTime.now());
        }

        List<UniversitySourcePageResult> fetched = pageFetcherService.fetchPages(candidateUrls);
        persistCrawledPages(institution.getName(), fetched);
        programmeRepository.deleteByInstitutionId(institution.getId());
        requirementRepository.deleteByInstitutionId(institution.getId());

        int programmeCount = 0;
        int requirementCount = 0;
        for (UniversitySourcePageResult page : fetched) {
            logRetrieval(institution, page.sourceUrl(), page.success() ? "SUCCESS" : "FAILED",
                    page.success() ? "Official university page retrieved successfully." : safe(page.failureReason()),
                    classifyRetrievalType(page.pageType()));
            if (!page.success()) {
                continue;
            }
            programmeCount += extractProgrammes(institution, page);
            requirementCount += extractRequirements(institution, page);
        }

        String message = programmeCount > 0 || requirementCount > 0
                ? "University data refreshed from allowlisted official sources."
                : "Refresh completed, but no structured verified programme or admission data could be extracted. Official-site fallback will be shown.";
        return new UniversityRefreshResponse(slug, programmeCount > 0 || requirementCount > 0 ? "SUCCESS" : "PARTIAL", message, OffsetDateTime.now());
    }

    private int extractProgrammes(Institution institution, UniversitySourcePageResult page) {
        if (!(page.pageType() == UniversityPageType.PROGRAMME_DETAIL
                || page.pageType() == UniversityPageType.QUALIFICATION_LIST
                || page.pageType() == UniversityPageType.FILTERED_PROGRAMME_LIST)) {
            return 0;
        }

        Set<String> matches = new LinkedHashSet<>();
        Matcher matcher = PROGRAMME_PATTERN.matcher(page.cleanedText());
        while (matcher.find()) {
            String value = sanitizeProgrammeName(matcher.group(1));
            if (!value.isBlank()) {
                matches.add(value);
            }
        }

        int saved = 0;
        for (String match : matches) {
            if (programmeRepository.findByInstitutionIdAndNameIgnoreCaseAndSourceUrl(institution.getId(), match, page.sourceUrl()).isPresent()) {
                continue;
            }
            UniversityProgramme programme = new UniversityProgramme();
            programme.setInstitution(institution);
            programme.setName(match);
            programme.setQualificationType(qualificationType(match));
            programme.setFaculty(inferFaculty(page));
            programme.setDuration(inferDuration(page.cleanedText()));
            programme.setStudyMode(inferStudyMode(page.cleanedText()));
            programme.setProgrammeUrl(page.sourceUrl());
            programme.setSourceUrl(page.sourceUrl());
            programme.setSourceLabel(sourceLabel(page.pageType()));
            programme.setRetrievalStatus("VERIFIED");
            programme.setLastVerifiedAt(OffsetDateTime.now());
            programmeRepository.save(programme);
            saved++;
        }
        return saved;
    }

    private int extractRequirements(Institution institution, UniversitySourcePageResult page) {
        String cleaned = safe(page.cleanedText());
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (!(page.pageType() == UniversityPageType.ADMISSIONS_OVERVIEW || lower.contains("admission") || lower.contains("requirements"))) {
            return 0;
        }

        Integer aps = extractAps(cleaned);
        List<String> subjects = SUBJECT_KEYWORDS.stream()
                .filter(subject -> lower.contains(subject.toLowerCase(Locale.ROOT)))
                .toList();
        String nscRequirement = firstSentenceContaining(cleaned, List.of("National Senior Certificate", "NSC"));
        String languageRequirement = firstSentenceContaining(cleaned, List.of("English", "language requirement"));
        String internationalRequirement = firstSentenceContaining(cleaned, List.of("international applicant", "international students"));
        String additionalTests = firstSentenceContaining(cleaned, List.of("NBT", "National Benchmark", "audition", "portfolio"));
        String facultySpecific = firstSentenceContaining(cleaned, List.of("faculty", "school of"));
        String requirementTitle = trimToLength(page.pageTitle() == null || page.pageTitle().isBlank() ? "General admission requirements" : page.pageTitle(), 255);
        String programmeName = deriveProgrammeNameFromTitle(page.pageTitle());

        if (aps == null && subjects.isEmpty() && nscRequirement.isBlank() && languageRequirement.isBlank() && internationalRequirement.isBlank() && additionalTests.isBlank()) {
            return 0;
        }
        if (requirementRepository.findByInstitutionIdAndProgrammeNameIgnoreCaseAndSourceUrlAndRequirementTitleIgnoreCase(institution.getId(), programmeName, page.sourceUrl(), requirementTitle).isPresent()) {
            return 0;
        }

        UniversityAdmissionRequirement requirement = new UniversityAdmissionRequirement();
        requirement.setInstitution(institution);
        requirement.setProgrammeName(programmeName);
        requirement.setRequirementTitle(requirementTitle);
        requirement.setApsMinimum(aps);
        requirement.setRequiredSubjects(String.join("|", subjects));
        requirement.setMinimumMarks("");
        requirement.setNscRequirement(trimToLength(nscRequirement, 4000));
        requirement.setLanguageRequirement(trimToLength(languageRequirement, 4000));
        requirement.setFacultySpecificRequirement(trimToLength(facultySpecific, 4000));
        requirement.setInternationalRequirement(trimToLength(internationalRequirement, 4000));
        requirement.setAdditionalTests(trimToLength(additionalTests, 4000));
        requirement.setSourceUrl(page.sourceUrl());
        requirement.setSourceLabel(sourceLabel(page.pageType()));
        requirement.setRetrievalStatus("VERIFIED");
        requirement.setLastVerifiedAt(OffsetDateTime.now());
        requirementRepository.save(requirement);
        return 1;
    }

    private void persistCrawledPages(String universityName, List<UniversitySourcePageResult> fetched) {
        OffsetDateTime now = OffsetDateTime.now();
        for (UniversitySourcePageResult pageResult : fetched) {
            CrawledUniversityPage page = crawledUniversityPageRepository.findBySourceUrl(pageResult.sourceUrl()).orElseGet(CrawledUniversityPage::new);
            page.setUniversityName(universityName);
            page.setSourceUrl(pageResult.sourceUrl());
            page.setPageTitle(pageResult.pageTitle());
            page.setPageType(pageResult.pageType().name());
            page.setExtractedKeywords(pageResult.extractedKeywords());
            page.setCleanedContent(pageResult.cleanedText());
            page.setSummaryExcerpt(trimToLength(pageResult.cleanedText(), 320));
            page.setLastCrawledAt(now);
            if (pageResult.success()) {
                page.setCrawlStatus(CrawlStatus.SUCCESS);
                page.setLastSuccessfulCrawledAt(now);
                page.setFailureReason(null);
                page.setErrorType(null);
            } else {
                page.setCrawlStatus(CrawlStatus.FAILED);
                page.setLastFailureAt(now);
                page.setFailureReason(trimToLength(safe(pageResult.failureReason()), 500));
                page.setErrorType(pageResult.failureType() == null ? "FETCH_ERROR" : pageResult.failureType().name());
            }
            crawledUniversityPageRepository.save(page);
        }
    }

    private void logRetrieval(Institution institution, String sourceUrl, String status, String message, String retrievalType) {
        UniversityRetrievalLog log = new UniversityRetrievalLog();
        log.setInstitution(institution);
        log.setSourceUrl(sourceUrl);
        log.setStatus(status);
        log.setMessage(trimToLength(message, 1000));
        log.setRetrievalType(retrievalType);
        log.setRetrievedAt(OffsetDateTime.now());
        retrievalLogRepository.save(log);
    }

    private UniversityInfoInstitutionResponse institutionSummary(Institution institution) {
        return new UniversityInfoInstitutionResponse(
                institution.getId(),
                slugService.slugify(institution.getName()),
                institution.getName(),
                institution.getWebsite(),
                officialProgrammesUrl(institution.getName()).orElse(institution.getWebsite()),
                officialAdmissionsUrl(institution.getName()).orElse(institution.getWebsite())
        );
    }

    private Optional<UniversityRegistryProperties.UniversityRegistryEntry> registryEntry(String institutionName) {
        return registryService.getActiveUniversities().stream()
                .filter(entry -> entry.getUniversityName().equalsIgnoreCase(institutionName))
                .findFirst();
    }

    private List<String> officialDomains(String institutionName) {
        return registryEntry(institutionName)
                .map(entry -> {
                    Set<String> domains = new LinkedHashSet<>();
                    domains.add(entry.getBaseDomain());
                    domains.addAll(entry.getAllowedDomains());
                    return List.copyOf(domains);
                })
                .orElse(List.of());
    }

    private Optional<String> officialProgrammesUrl(String institutionName) {
        return registryEntry(institutionName)
                .flatMap(entry -> entry.getSeedUrls().stream()
                        .filter(url -> {
                            String lower = url.toLowerCase(Locale.ROOT);
                            return lower.contains("program") || lower.contains("programme") || lower.contains("study");
                        })
                        .findFirst());
    }

    private Optional<String> officialAdmissionsUrl(String institutionName) {
        Optional<String> fromCrawl = crawledUniversityPageRepository.findTop10ByUniversityNameIgnoreCaseAndActiveTrueOrderByLastSuccessfulCrawledAtDesc(institutionName).stream()
                .filter(page -> {
                    String lower = safe(page.getSourceUrl()).toLowerCase(Locale.ROOT);
                    return "ADMISSIONS_OVERVIEW".equalsIgnoreCase(page.getPageType()) || lower.contains("admission") || lower.contains("apply");
                })
                .map(CrawledUniversityPage::getSourceUrl)
                .findFirst();
        if (fromCrawl.isPresent()) {
            return fromCrawl;
        }
        return registryEntry(institutionName)
                .flatMap(entry -> entry.getSeedUrls().stream()
                        .filter(url -> {
                            String lower = url.toLowerCase(Locale.ROOT);
                            return lower.contains("admission") || lower.contains("apply");
                        })
                        .findFirst());
    }

    private UniversityProgrammeResponse toProgrammeResponse(UniversityProgramme programme) {
        return new UniversityProgrammeResponse(
                programme.getId(),
                programme.getName(),
                programme.getQualificationType(),
                programme.getFaculty(),
                programme.getDepartment(),
                programme.getDuration(),
                programme.getStudyMode(),
                programme.getCampus(),
                programme.getProgrammeUrl(),
                programme.getSourceUrl(),
                programme.getSourceLabel(),
                programme.getLastVerifiedAt(),
                programme.getRetrievalStatus(),
                programme.isActive()
        );
    }

    private UniversityAdmissionRequirementResponse toRequirementResponse(UniversityAdmissionRequirement requirement) {
        return new UniversityAdmissionRequirementResponse(
                requirement.getId(),
                requirement.getProgrammeName(),
                requirement.getRequirementTitle(),
                requirement.getApsMinimum(),
                splitPipeValues(requirement.getRequiredSubjects()),
                splitPipeValues(requirement.getMinimumMarks()),
                defaultUnavailable(requirement.getNscRequirement()),
                defaultUnavailable(requirement.getLanguageRequirement()),
                defaultUnavailable(requirement.getFacultySpecificRequirement()),
                defaultUnavailable(requirement.getInternationalRequirement()),
                defaultUnavailable(requirement.getAdditionalTests()),
                requirement.getSourceUrl(),
                requirement.getSourceLabel(),
                requirement.getLastVerifiedAt(),
                requirement.getRetrievalStatus(),
                requirement.isActive()
        );
    }

    private List<UniversityRetrievalLogResponse> logs(UUID institutionId) {
        return retrievalLogRepository.findTop10ByInstitutionIdOrderByRetrievedAtDesc(institutionId).stream()
                .map(log -> new UniversityRetrievalLogResponse(log.getId(), log.getSourceUrl(), log.getStatus(), log.getMessage(), log.getRetrievalType(), log.getRetrievedAt()))
                .toList();
    }

    private OffsetDateTime latestLogTime(List<UniversityRetrievalLogResponse> logs) {
        return logs.stream().map(UniversityRetrievalLogResponse::retrievedAt).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<String> distinctValues(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).distinct().sorted().toList();
    }

    private String sanitizeProgrammeName(String value) {
        return trimToLength(value.replaceAll("\\s+", " ").trim(), 255);
    }

    private String qualificationType(String programmeName) {
        String lower = safe(programmeName).toLowerCase(Locale.ROOT);
        if (lower.startsWith("bachelor")) return "Bachelor";
        if (lower.startsWith("master")) return "Master";
        if (lower.startsWith("doctor")) return "Doctorate";
        if (lower.startsWith("postgraduate diploma")) return "Postgraduate Diploma";
        if (lower.startsWith("advanced diploma")) return "Advanced Diploma";
        if (lower.startsWith("higher certificate")) return "Higher Certificate";
        if (lower.startsWith("diploma")) return "Diploma";
        if (lower.startsWith("certificate")) return "Certificate";
        return "Qualification";
    }

    private String inferFaculty(UniversitySourcePageResult page) {
        String text = (safe(page.pageTitle()) + " " + String.join(" ", page.headings()) + " " + safe(page.cleanedText())).toLowerCase(Locale.ROOT);
        for (String faculty : List.of("Engineering", "Health Sciences", "Humanities", "Commerce", "Science", "Education", "Law", "Agriculture", "Arts", "Social Sciences", "Medicine", "Information Technology")) {
            if (text.contains(faculty.toLowerCase(Locale.ROOT))) {
                return faculty;
            }
        }
        return null;
    }

    private String inferDuration(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{1,2}\\s+(?:year|years|month|months))\\b", Pattern.CASE_INSENSITIVE).matcher(safe(text));
        return matcher.find() ? trimToLength(matcher.group(1), 120) : null;
    }

    private String inferStudyMode(String text) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        if (lower.contains("distance") || lower.contains("online")) return "Distance / Online";
        if (lower.contains("part-time")) return "Part-time";
        if (lower.contains("full-time")) return "Full-time";
        return null;
    }

    private Integer extractAps(String text) {
        Matcher matcher = APS_PATTERN.matcher(safe(text));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstSentenceContaining(String text, List<String> needles) {
        List<String> sentences = Arrays.stream(safe(text).split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(sentence -> !sentence.isBlank())
                .limit(80)
                .toList();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase(Locale.ROOT);
            if (needles.stream().anyMatch(needle -> lower.contains(needle.toLowerCase(Locale.ROOT)))) {
                return sentence;
            }
        }
        return "";
    }

    private String deriveProgrammeNameFromTitle(String title) {
        String safeTitle = safe(title).trim();
        if (safeTitle.isBlank()) {
            return "General";
        }
        Matcher matcher = PROGRAMME_PATTERN.matcher(safeTitle);
        if (matcher.find()) {
            return sanitizeProgrammeName(matcher.group(1));
        }
        return "General";
    }

    private String sourceLabel(UniversityPageType pageType) {
        return switch (pageType) {
            case PROGRAMME_DETAIL -> "Official programme page";
            case QUALIFICATION_LIST, FILTERED_PROGRAMME_LIST -> "Official programmes listing";
            case ADMISSIONS_OVERVIEW -> "Official admissions page";
            default -> "Official university source";
        };
    }

    private String classifyRetrievalType(UniversityPageType pageType) {
        return switch (pageType) {
            case PROGRAMME_DETAIL, QUALIFICATION_LIST, FILTERED_PROGRAMME_LIST -> "PROGRAMMES";
            case ADMISSIONS_OVERVIEW -> "ADMISSIONS";
            case FEES_FUNDING -> "FEES_FUNDING";
            default -> "GENERAL";
        };
    }

    private List<String> splitPipeValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String defaultUnavailable(String value) {
        return value == null || value.isBlank() ? "Not available from the official source." : value;
    }

    private String trimToLength(String value, int maxLength) {
        String safeValue = safe(value).trim();
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
