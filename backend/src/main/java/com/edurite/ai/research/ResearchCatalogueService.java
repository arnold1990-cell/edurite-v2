package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.institution.universityinfo.repository.UniversityProgrammeRepository;
import com.edurite.recommendation.dto.RecommendationItemDto;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;
import com.edurite.student.dto.StudentSubjectAchievementDto;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class ResearchCatalogueService {
    private final UniversityProgrammeRepository programmes;
    private final CareerProgramRequirementRepository requirements;
    private final ProgrammeEligibilityService eligibility;
    private final UniversitySourceRegistryService registry;

    public ResearchCatalogueService(
            UniversityProgrammeRepository programmes,
            CareerProgramRequirementRepository requirements,
            ProgrammeEligibilityService eligibility,
            UniversitySourceRegistryService registry) {
        this.programmes = programmes;
        this.requirements = requirements;
        this.eligibility = eligibility;
        this.registry = registry;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeFinding> match(
            List<RecommendationItemDto> careers, List<StudentSubjectAchievementDto> subjects) {
        Map<String, ProgrammeFinding> result = new LinkedHashMap<>();
        for (var row : requirements.findAll()) {
            List<String> related =
                    careers.stream()
                            .filter(c -> related(c.title(), row.getCareerName()))
                            .map(RecommendationItemDto::id)
                            .toList();
            if (related.isEmpty()) continue;
            String country = country(row.getSource());
            boolean official =
                    registry.isAllowedUrlForUniversity(row.getInstitutionName(), row.getSource());
            boolean fresh =
                    official
                            && row.isVerified()
                            && row.getLastVerifiedAt() != null
                            && row.getLastVerifiedAt().isAfter(OffsetDateTime.now().minusDays(90));
            var assessment =
                    official
                            ? eligibility.assess(row, subjects, country)
                            : unknown(
                                    "The programme's official source is not verified against the"
                                            + " institution registry.");
            var finding =
                    new ProgrammeFinding(
                            row.getId().toString(),
                            row.getInstitutionName(),
                            country,
                            row.getQualificationName(),
                            row.getQualificationType(),
                            row.getDuration(),
                            row.getProvince(),
                            null,
                            safeUrl(row.getSource()),
                            safeUrl(row.getApplicationUrl()),
                            join(
                                    row.getGeneralAdmissionRequirements(),
                                    row.getProgrammeAdmissionRequirements(),
                                    row.getAdditionalSelectionRequirements()),
                            safeUrl(row.getSource()),
                            row.getLastVerifiedAt() == null
                                    ? null
                                    : row.getLastVerifiedAt().toString(),
                            fresh
                                    ? "Verified published record"
                                    : "Admission requirements not yet verified.",
                            assessment,
                            related);
            String key = identity(finding);
            var previous = result.get(key);
            if (previous == null) result.put(key, finding);
            else if (Objects.equals(previous.sourceUrl(), finding.sourceUrl())
                    && Objects.equals(previous.requirements(), finding.requirements())
                    && Objects.equals(previous.eligibility(), finding.eligibility()))
                result.put(key, mergeLinks(previous, finding));
            else result.put(key + "|" + finding.id(), finding);
        }
        for (int page = 0; ; page++) {
            var batch = programmes.findByActiveTrue(PageRequest.of(page, 200));
            for (var row : batch.getContent()) {
                var related =
                        careers.stream()
                                .filter(c -> related(c.title(), row.getName()))
                                .map(RecommendationItemDto::id)
                                .toList();
                if (related.isEmpty()) continue;
                var institution = row.getInstitution();
                boolean official =
                        registry.isAllowedUrlForUniversity(
                                institution.getName(), row.getSourceUrl());
                boolean fresh =
                        official
                                && row.getLastVerifiedAt() != null
                                && row.getLastVerifiedAt()
                                        .isAfter(OffsetDateTime.now().minusDays(90))
                                && Set.of("VERIFIED", "SOURCE_CONFIRMED")
                                        .contains(Objects.toString(row.getRetrievalStatus(), ""));
                var finding =
                        new ProgrammeFinding(
                                row.getId().toString(),
                                institution.getName(),
                                institution.getCountry(),
                                row.getName(),
                                row.getQualificationType(),
                                row.getDuration(),
                                row.getCampus(),
                                row.getStudyMode(),
                                safeUrl(row.getProgrammeUrl()),
                                evidence(row) == null ? null : evidence(row).applicationUrl(),
                                evidence(row) == null ? null : evidence(row).publishedRequirements(),
                                safeUrl(row.getSourceUrl()),
                                row.getLastVerifiedAt() == null
                                        ? null
                                        : row.getLastVerifiedAt().toString(),
                                fresh
                                        ? "Programme title verified; admission requirements not yet"
                                                + " verified."
                                        : "Admission requirements not yet verified.",
                                official && fresh && evidence(row)!=null
                                        ? eligibility.assessPublished(row,subjects,institution.getCountry())
                                        : unknown("Information not yet verified."),
                                related, evidence(row));
                String key = identity(finding);
                if (!result.containsKey(key)) result.put(key, finding);
                else
                    result.replaceAll(
                            (k, existing) ->
                                    identity(existing).equals(key)
                                            ? mergeLinks(existing, finding)
                                            : existing);
            }
            if (!batch.hasNext()) break;
        }
        return List.copyOf(result.values());
    }

    private ProgrammeFinding mergeLinks(ProgrammeFinding kept, ProgrammeFinding other) {
        LinkedHashSet<String> careerIds = new LinkedHashSet<>(kept.careerIds());
        careerIds.addAll(other.careerIds());
        return new ProgrammeFinding(
                kept.id(),
                kept.institution(),
                kept.country(),
                kept.name(),
                kept.qualification(),
                kept.duration(),
                kept.campus(),
                kept.studyMode(),
                kept.programmeUrl(),
                kept.applicationUrl(),
                kept.requirements(),
                kept.sourceUrl(),
                kept.verifiedAt(),
                kept.verificationStatus(),
                kept.eligibility(),
                List.copyOf(careerIds), kept.publishedEvidence());
    }

    private PublishedProgrammeEvidence evidence(com.edurite.institution.universityinfo.entity.UniversityProgramme row) {
        return PublishedProgrammeEvidence.fromJson(row.getPublishedEvidenceJson());
    }

    public static Eligibility unknown(String reason) {
        return new Eligibility(
                ProgrammeEligibilityService.INSUFFICIENT,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(reason));
    }

    public static String safeUrl(String value) {
        try {
            var u = java.net.URI.create(value);
            return Set.of("https", "http").contains(u.getScheme())
                            && u.getHost() != null
                            && u.getUserInfo() == null
                    ? value
                    : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String identity(ProgrammeFinding p) {
        return (p.institution() + "|" + p.name()).strip().toLowerCase(Locale.ROOT);
    }

    public static boolean related(String career, String programme) {
        var tokens = words(career);
        var other = words(programme);
        return !tokens.isEmpty()
                && tokens.stream()
                        .allMatch(
                                a ->
                                        other.stream()
                                                .anyMatch(
                                                        b ->
                                                                a.equals(b)
                                                                        || a.length() >= 5
                                                                                && b.length() >= 5
                                                                                && commonPrefix(
                                                                                                a,
                                                                                                b)
                                                                                        >= 5));
    }

    public static List<String> words(String value) {
        return Arrays.stream(Objects.toString(value, "").toLowerCase(Locale.ROOT).split("[^a-z]+"))
                .filter(
                        t ->
                                t.length() > 3
                                        && !Set.of(
                                                        "bachelor",
                                                        "degree",
                                                        "diploma",
                                                        "certificate",
                                                        "honours",
                                                        "science",
                                                        "studies",
                                                        "senior",
                                                        "junior",
                                                        "professional")
                                                .contains(t))
                .distinct()
                .toList();
    }

    private static int commonPrefix(String a, String b) {
        int n = 0;
        while (n < Math.min(a.length(), b.length()) && a.charAt(n) == b.charAt(n)) n++;
        return n;
    }

    private String country(String source) {
        try {
            String host = java.net.URI.create(source).getHost();
            return host.endsWith(".bw")
                    ? "Botswana"
                    : host.endsWith(".zw")
                            ? "Zimbabwe"
                            : host.endsWith(".za") ? "South Africa" : "Not recorded";
        } catch (Exception ex) {
            return "Not recorded";
        }
    }

    private String join(String... values) {
        return String.join(
                "\n",
                Arrays.stream(values).filter(Objects::nonNull).filter(s -> !s.isBlank()).toList());
    }
}
