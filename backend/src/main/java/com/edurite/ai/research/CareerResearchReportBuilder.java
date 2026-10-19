package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.ai.university.*;
import com.edurite.bursary.dto.*;
import com.edurite.bursary.source.ProviderBursarySource;
import com.edurite.career.repository.CareerRepository;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.service.StudentService;

import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

@Service
public class CareerResearchReportBuilder {
    private final StudentService students;
    private final RecommendationService recommendations;
    private final AcademicIntelligenceService academic;
    private final ResearchCatalogueService catalogue;
    private final OfficialCatalogueResearchService research;
    private final CareerRepository careers;
    private final ResearchNarrativeService narrative;
    private final ProviderBursarySource funding;
    private final CrawledUniversityPageRepository pages;
    private final UniversitySourceRegistryService registry;
    private final com.edurite.student.repository.StudentPreferenceRepository studentPreferences;

    public CareerResearchReportBuilder(
            StudentService students,
            RecommendationService recommendations,
            AcademicIntelligenceService academic,
            ResearchCatalogueService catalogue,
            OfficialCatalogueResearchService research,
            CareerRepository careers,
            ResearchNarrativeService narrative,
            ProviderBursarySource funding,
            CrawledUniversityPageRepository pages,
            UniversitySourceRegistryService registry,
            com.edurite.student.repository.StudentPreferenceRepository studentPreferences) {
        this.students = students;
        this.recommendations = recommendations;
        this.academic = academic;
        this.catalogue = catalogue;
        this.research = research;
        this.careers = careers;
        this.narrative = narrative;
        this.funding = funding;
        this.pages = pages;
        this.registry = registry;
        this.studentPreferences = studentPreferences;
    }

    public Report build(Principal principal, Consumer<String> progress) {
        progress.accept("Gathering academic and preference evidence");
        var profile = students.getProfile(principal);
        var preferences=studentPreferences.findByStudentId(profile.id()).orElse(null);
        String preferredLocation=preferences!=null && preferences.getPreferredLocations()!=null && !preferences.getPreferredLocations().isBlank()
                ? preferences.getPreferredLocations() : profile.location();
        java.math.BigDecimal tuitionBudget=null;
        boolean fundingPreferred=false;
        if(preferences!=null) {
            try {
                var extra=new com.fasterxml.jackson.databind.ObjectMapper().readTree(preferences.getExtra());
                var budget=extra.path("annualTuitionBudgetZar");
                if(budget.isNumber() && budget.decimalValue().signum()>0 && budget.decimalValue().compareTo(new java.math.BigDecimal("10000000"))<=0)
                    tuitionBudget=budget.decimalValue();
                fundingPreferred=extra.path("fundingRequired").isBoolean() && extra.path("fundingRequired").booleanValue();
            } catch(Exception ignored) { /* Missing financial preferences are never inferred. */ }
        }
        List<com.edurite.student.dto.StudentSubjectAchievementDto> historical = new ArrayList<>();
        var converter = new com.fasterxml.jackson.databind.ObjectMapper();
        for (var entry : students.transcriptHistory(principal)) {
            if (entry.get("previousResults") != null)
                historical.addAll(
                        converter.convertValue(
                                entry.get("previousResults"),
                                new com.fasterxml.jackson.core.type.TypeReference<
                                        List<
                                                com.edurite.student.dto
                                                        .StudentSubjectAchievementDto>>() {}));
        }
        var analysis = academic.analyse(profile, historical);
        progress.accept("Discovering and ranking supported catalogue careers");
        var ranked = recommendations.generateForStudent(principal);
        if (ranked.inputsUsed().isEmpty() || ranked.suggestedCareers().isEmpty())
            throw new IllegalArgumentException(
                    "No supported career evidence. Update academic results, interests, skills or"
                            + " assessment responses.");
        List<String> limits = new ArrayList<>(ranked.limitations());
        limits.add(tuitionBudget==null ? "Financial suitability cannot be certified without a recorded budget and verified full costs. Published fees and funding sources are provided where available."
                : "Saved annual tuition budget: ZAR "+tuitionBudget.toPlainString()+". Only explicitly published annual ZAR tuition is compared; living costs and funding eligibility remain unverified.");
        if(fundingPreferred) limits.add("The student has recorded a need for funding; bursary eligibility and availability must be checked separately.");
        if(preferredLocation!=null && !preferredLocation.isBlank())
            limits.add("Location preferences inform programme ordering where a matching published province is recorded; other regions remain available.");
        limits.addAll(analysis.limitations());
        if (ranked.careerSuggestionsLimited())
            limits.add(
                    ranked.upgradeMessage() == null
                            ? "The current subscription limits the visible catalogue"
                                    + " recommendations."
                            : ranked.upgradeMessage());
        var matched = catalogue.match(ranked.suggestedCareers(), analysis.subjects());
        progress.accept("Checking source freshness and targeted official institution discovery");
        if (matched.isEmpty()
                || matched.stream()
                        .anyMatch(
                                p ->
                                        p.verifiedAt() == null
                                                || !p.verificationStatus()
                                                        .startsWith("Verified"))) {
            String location =
                    (Objects.toString(profile.location(), "")
                                    + " "
                                    + String.join(" ", analysis.curricula()))
                            .toLowerCase(Locale.ROOT);
            String country =
                    (location.contains("botswana") || location.contains("bgcse"))
                            ? "Botswana"
                            : (location.contains("zimbabwe") || location.contains("zimsec"))
                                    ? "Zimbabwe"
                                    : (location.contains("south africa")
                                                    || location.contains("nsc")
                                                    || location.contains("caps"))
                                            ? "South Africa"
                                            : null;
            try {
                limits.addAll(research.refreshOne(country));
            } catch (RuntimeException ex) {
                limits.add(
                        "The targeted official source refresh was unavailable. Existing catalogue"
                            + " records were retained; no newly verified findings are claimed.");
            }
            matched = catalogue.match(ranked.suggestedCareers(), analysis.subjects());
        }
        if (matched.isEmpty())
            limits.add(
                    "No supported programme records were found within this research budget. This is"
                            + " not evidence that no suitable programmes exist.");
        String preference=Objects.toString(preferredLocation, "").toLowerCase(Locale.ROOT);
        var preferredNames=registry.getRegistryEntries().stream().filter(e -> e.getProvince()!=null
                && Arrays.stream(e.getProvince().split(";")).map(String::strip).anyMatch(p -> preference.contains(p.toLowerCase(Locale.ROOT))))
                .map(e -> e.getUniversityName().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        java.math.BigDecimal budgetForOrdering=tuitionBudget;
        matched=matched.stream().sorted(Comparator.comparing((ProgrammeFinding p) -> !preferredNames.contains(p.institution().toLowerCase(Locale.ROOT)))
                .thenComparingInt(p -> ProgrammePreferenceOrdering.budgetOrder(p.publishedEvidence(),budgetForOrdering))).toList();
        progress.accept("Assessing programme-specific admission conditions");
        Map<String, com.edurite.career.entity.Career> canonical = new HashMap<>();
        careers.findAllById(
                        ranked.suggestedCareers().stream()
                                .map(c -> UUID.fromString(c.id()))
                                .toList())
                .forEach(c -> canonical.put(c.getId().toString(), c));
        List<CareerFinding> findings = new ArrayList<>();
        for (var row : ranked.suggestedCareers()) {
            var c = canonical.get(row.id());
            String description = c == null ? "" : Objects.toString(c.getDescription(), "");
            var related =
                    analysis.subjects().stream()
                            .filter(
                                    s ->
                                            !ResearchCatalogueService.words(s.subjectName())
                                                            .isEmpty()
                                                    && ResearchCatalogueService.words(
                                                                    s.subjectName())
                                                            .stream()
                                                            .anyMatch(
                                                                    t ->
                                                                            (row.title()
                                                                                            + " "
                                                                                            + description
                                                                                            + " "
                                                                                            + row
                                                                                                    .rationale())
                                                                                    .toLowerCase(
                                                                                            Locale
                                                                                                    .ROOT)
                                                                                    .contains(t)))
                            .toList();
            var programmeIds =
                    matched.stream()
                            .filter(p -> p.careerIds().contains(row.id()))
                            .map(ProgrammeFinding::id)
                            .toList();
            var strengths =
                    related.stream()
                            .filter(analysis.relativeStrengths()::contains)
                            .map(s -> s.subjectName() + ": " + s.markPercentage() + "%")
                            .toList();
            var gaps =
                    matched.stream()
                            .filter(p -> p.careerIds().contains(row.id()))
                            .flatMap(p -> p.eligibility().gaps().stream())
                            .distinct()
                            .toList();
            findings.add(
                    new CareerFinding(
                            row.id(),
                            row.title(),
                            c == null || c.getIndustry() == null
                                    ? "Other Careers"
                                    : c.getIndustry(),
                            description,
                            row.score(),
                            "Evidence-aligned exploration",
                            row.rationale(),
                            related.stream()
                                    .map(
                                            s ->
                                                    s.subjectName()
                                                            + (s.markPercentage() == null
                                                                    ? " (level "
                                                                            + s.achievementLevel()
                                                                            + ")"
                                                                    : " ("
                                                                            + s.markPercentage()
                                                                            + "%)"))
                                    .toList(),
                            strengths,
                            gaps,
                            c != null && c.getQualificationLevel() != null
                                    ? List.of(c.getQualificationLevel())
                                    : List.of(),
                            programmeIds));
        }
        progress.accept("Checking current funding records and official funding sources");
        List<BursaryResultDto> bursaries = new ArrayList<>();
        // Provider lookup stays inside EduRite; no private profile is sent to a web search
        // provider.
        for (var career : findings)
            for (var item :
                    funding.fetch(new BursarySearchRequest(career.title(), "", "", "", 0, 100)))
                if (item.deadline() != null && !item.deadline().isBefore(LocalDate.now()))
                    bursaries.add(item);
        for (var page : pages.findByActiveTrueAndCrawlStatus(CrawlStatus.SUCCESS)) {
            String title = Objects.toString(page.getPageTitle(), "");
            String body = Objects.toString(page.getCleanedContent(), "");
            if (!title.toLowerCase(Locale.ROOT).matches(".*(scholarship|bursary|bursaries).*")
                    || page.getLastSuccessfulCrawledAt() == null
                    || page.getLastSuccessfulCrawledAt()
                            .isBefore(OffsetDateTime.now().minusDays(30))
                    || !registry.isAllowedUrlForUniversity(
                            page.getUniversityName(), page.getSourceUrl())) continue;
            if (findings.stream().noneMatch(c -> ResearchCatalogueService.related(c.title(), body)))
                continue;
            Matcher date =
                    Pattern.compile("(?i)(?:closing date|deadline)\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})")
                            .matcher(body);
            LocalDate deadline = null;
            try {
                if (date.find()) deadline = LocalDate.parse(date.group(1));
            } catch (java.time.DateTimeException ignored) {
            }
            if (deadline != null && deadline.isBefore(LocalDate.now())
                    || body.toLowerCase(Locale.ROOT).contains("applications closed")) continue;
            bursaries.add(
                    new BursaryResultDto(
                            page.getId().toString(),
                            title,
                            page.getUniversityName(),
                            body.substring(0, Math.min(600, body.length())),
                            null,
                            null,
                            null,
                            deadline,
                            page.getSourceUrl(),
                            "OFFICIAL_UNIVERSITY",
                            0,
                            List.of(page.getSourceUrl()),
                            true,
                            deadline == null,
                            deadline == null
                                    ? "Deadline/open status not verified; confirm on the official"
                                            + " page."
                                    : "Published deadline; confirm current availability with the"
                                            + " institution."));
        }
        var distinctFunding =
                bursaries.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        BursaryResultDto::externalId,
                                        b -> b,
                                        (a, b) -> a,
                                        LinkedHashMap::new))
                        .values()
                        .stream()
                        .toList();
        List<Source> sources =
                new ArrayList<>(
                        matched.stream()
                                .filter(p -> p.sourceUrl() != null)
                                .map(
                                        p ->
                                                new Source(
                                                        p.sourceUrl(),
                                                        p.institution(),
                                                        p.verifiedAt(),
                                                        p.verificationStatus()))
                                .distinct()
                                .toList());
        for (var item : distinctFunding) {
            String url = ResearchCatalogueService.safeUrl(item.applicationLink());
            if (url != null)
                sources.add(
                        new Source(
                                url,
                                item.provider(),
                                null,
                                "Funding catalogue / official source; current availability must be"
                                        + " confirmed"));
        }
        sources = sources.stream().distinct().toList();
        limits.add(
                "Catalogue title matching is lexical relevance, not proof of a professional"
                        + " qualification pathway. Confirm programme scope and professional"
                        + " registration.");
        limits.add(
                "One stale institution at most is researched per generation; additional sources"
                        + " refresh separately. Sources blocked by robots, authentication or access"
                        + " challenges are not bypassed.");
        limits.add(
                "AI interpretation summarises up to 40 ranked careers, 60 programme records and 20"
                    + " funding records for a bounded prompt; all supported catalogue results"
                    + " remain in the report.");
        progress.accept("Generating and validating the Gemini research interpretation");
        var interpretation =
                narrative.generate(analysis, findings, matched, distinctFunding, limits);
        var alternatives =
                matched.stream()
                        .filter(
                                p ->
                                        p.verificationStatus().startsWith("Verified")
                                                || p.verificationStatus()
                                                        .startsWith("Programme title verified"))
                        .filter(
                                p ->
                                        Objects.toString(p.qualification(), "")
                                                .toLowerCase(Locale.ROOT)
                                                .matches(
                                                        ".*(diploma|certificate|vocational|foundation|bridging).*"))
                        .toList();
        return new Report(
                "career-research-v1",
                Instant.now().toString(),
                analysis,
                List.copyOf(findings),
                matched,
                alternatives,
                distinctFunding,
                interpretation,
                sources,
                ranked.inputsUsed(),
                List.copyOf(limits),
                "Existing profile-catalogue weights: interests up to 60; skills up to 20; recorded"
                        + " subject performance up to 15; assessment dimensions up to 5;"
                        + " aspirations/preferences +10 per match; total capped at 100. Internal"
                        + " ordering only, not a probability. Programme eligibility is assessed"
                        + " separately.");
    }
}
