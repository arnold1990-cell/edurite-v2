package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.roadmap.dto.CareerRoadmapDtos.ApsSubjectResult;
import com.edurite.roadmap.entity.CareerProgramRequirement;
import com.edurite.roadmap.service.CareerRoadmapService;
import com.edurite.student.dto.StudentSubjectAchievementDto;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.regex.*;

@Service
public class ProgrammeEligibilityService {
    public static final String INSUFFICIENT = "Insufficient information";
    public static final String POTENTIAL = "Potentially eligible";
    public static final String GAP = "Does not currently meet requirements";
    public static final String MEETS = "Meets published requirements";

    public Eligibility assessPublished(com.edurite.institution.universityinfo.entity.UniversityProgramme row,
            List<StudentSubjectAchievementDto> results, String country) {
        var evidence=PublishedProgrammeEvidence.fromJson(row.getPublishedEvidenceJson());
        if(evidence==null || !evidence.programmeSpecific()) return ResearchCatalogueService.unknown("Information not yet verified.");
        var requirement=new CareerProgramRequirement();
        requirement.setVerified(Set.of("VERIFIED","SOURCE_CONFIRMED").contains(Objects.toString(row.getRetrievalStatus(),"")));
        requirement.setLastVerifiedAt(row.getLastVerifiedAt());requirement.setAdmissionYear(evidence.intakeYear());
        requirement.setApsRequired(evidence.minimumAps());requirement.setApsRule(evidence.apsRule());
        var thresholds=evidence.subjectRequirements();
        requirement.setMathematicsRequirement(thresholds.get("Mathematics"));
        requirement.setMathematicalLiteracyRequirement(thresholds.get("Mathematical Literacy"));
        requirement.setEnglishRequirement(thresholds.get("English"));requirement.setAccountingRequirement(thresholds.get("Accounting"));
        requirement.setPhysicalSciencesRequirement(thresholds.get("Physical Sciences"));requirement.setLifeSciencesRequirement(thresholds.get("Life Sciences"));
        var checked=assess(requirement,results,country);
        var unknown=new ArrayList<>(checked.uncheckedConditions()); unknown.addAll(evidence.limitations());
        if(thresholds.keySet().stream().anyMatch(s -> !Set.of("Mathematics","Mathematical Literacy","English","Accounting","Physical Sciences","Life Sciences").contains(s)))
            unknown.add("Additional compulsory subjects require review.");
        // A partial extractor cannot certify that the complete published condition set was checked.
        return new Eligibility(checked.gaps().isEmpty()?"Eligibility cannot be determined":"Does not meet published minimum requirements",
                checked.studentAps(),checked.requiredAps(),checked.apsRule(),checked.subjects(),checked.gaps(),List.copyOf(unknown));
    }

    public Eligibility assess(
            CareerProgramRequirement programme,
            List<StudentSubjectAchievementDto> results,
            String country) {
        List<String> unknown = new ArrayList<>(), gaps = new ArrayList<>();
        List<SubjectCheck> checks = new ArrayList<>();
        boolean fresh =
                programme.isVerified()
                        && programme.getLastVerifiedAt() != null
                        && programme.getLastVerifiedAt().isAfter(OffsetDateTime.now().minusDays(90))
                        && (programme.getAdmissionYear() == null
                                || programme.getAdmissionYear() >= Year.now().getValue());
        if (!fresh) unknown.add("Admission requirements not yet verified for the current intake.");
        boolean nsc =
                !results.isEmpty()
                        && results.stream()
                                .allMatch(
                                        s ->
                                                Objects.toString(s.curriculum(), "")
                                                        .toLowerCase(Locale.ROOT)
                                                        .matches(".*\\b(nsc|caps|ieb)\\b.*"));
        if (!"South Africa".equals(country) || !nsc)
            unknown.add(
                    "The recorded curriculum is not verified as compatible with these NSC"
                            + " thresholds. Botswana/BGCSE and Zimbabwe/ZIMSEC results are not"
                            + " converted to APS.");
        boolean comparablePeriod =
                results.stream()
                                .map(
                                        s ->
                                                Objects.toString(s.academicYear(), "unknown")
                                                        + "|"
                                                        + Objects.toString(
                                                                s.examination(), "unknown"))
                                .distinct()
                                .count()
                        <= 1;
        if (!comparablePeriod)
            unknown.add(
                    "Results from different examination periods cannot be combined for admission"
                            + " assessment.");
        if (programme.getAdmissionYear() == null)
            unknown.add("The published intake year needs confirmation.");
        boolean duplicateSubjects =
                results.stream()
                                .map(
                                        s ->
                                                Objects.toString(s.subjectName(), "")
                                                        .strip()
                                                        .toLowerCase(Locale.ROOT))
                                .distinct()
                                .count()
                        != results.size();
        if (duplicateSubjects)
            unknown.add("Duplicate subject results need review before admission can be assessed.");
        boolean compatible =
                fresh
                        && nsc
                        && comparablePeriod
                        && !duplicateSubjects
                        && "South Africa".equals(country);
        String[][] thresholds = {
            {"Mathematics", programme.getMathematicsRequirement()},
            {"Mathematical Literacy", programme.getMathematicalLiteracyRequirement()},
            {"English", programme.getEnglishRequirement()},
            {"Accounting", programme.getAccountingRequirement()},
            {"Physical Sciences", programme.getPhysicalSciencesRequirement()},
            {"Life Sciences", programme.getLifeSciencesRequirement()}
        };
        for (var threshold : thresholds) {
            if (threshold[1] == null || threshold[1].isBlank()) continue;
            var subject =
                    results.stream()
                            .filter(s -> threshold[0].equalsIgnoreCase(s.subjectName()))
                            .findFirst()
                            .orElse(null);
            Matcher mark =
                    Pattern.compile("(?i)^(?:minimum\\s+|at least\\s+)?(\\d{1,3})\\s*%\\+?$")
                            .matcher(threshold[1].trim());
            Matcher level =
                    Pattern.compile("(?i)^(?:minimum\\s+)?level\\s+([1-7])\\+?$")
                            .matcher(threshold[1].trim());
            boolean percent = mark.matches(), levels = level.matches();
            Integer needed =
                    percent
                            ? Integer.valueOf(mark.group(1))
                            : levels ? Integer.valueOf(level.group(1)) : null;
            Integer actual =
                    subject == null
                            ? null
                            : percent
                                    ? subject.markPercentage()
                                    : levels ? subject.achievementLevel() : null;
            String status = "Unknown";
            if (compatible
                    && actual != null
                    && needed != null
                    && needed <= 100
                    && actual >= 0
                    && (percent ? actual <= 100 : actual >= 1 && actual <= 7)) {
                status = actual >= needed ? "Met" : "Gap";
                if (status.equals("Gap"))
                    gaps.add(
                            threshold[0] + ": " + actual + "; published threshold " + threshold[1]);
            } else unknown.add(threshold[0] + ": threshold or comparable result needs review.");
            checks.add(
                    new SubjectCheck(
                            threshold[0],
                            subject == null
                                    ? "Not recorded"
                                    : subject.markPercentage() != null
                                            ? subject.markPercentage() + "%"
                                            : "Level " + subject.achievementLevel(),
                            threshold[1],
                            status,
                            subject != null && subject.verified()));
        }
        // Alternatives must be interpreted as alternatives, never both compulsory.
        if (programme.getMathematicsRequirement() != null
                && programme.getMathematicalLiteracyRequirement() != null) {
            unknown.add(
                    "Mathematics / Mathematical Literacy alternatives need programme-specific"
                            + " interpretation.");
            gaps.removeIf(
                    text ->
                            text.startsWith("Mathematics:")
                                    || text.startsWith("Mathematical Literacy:"));
        }
        Integer aps = null;
        if (compatible && programme.getApsRequired() != null) {
            var apsSubjects =
                    results.stream()
                            .map(
                                    s ->
                                            new ApsSubjectResult(
                                                    s.subjectName(),
                                                    s.markPercentage(),
                                                    s.achievementLevel(),
                                                    null,
                                                    true,
                                                    null))
                            .toList();
            aps = CareerRoadmapService.programmeAps(programme.getApsRule(), apsSubjects);
            if (aps == null)
                unknown.add("Institution-specific APS rule or comparable levels are unavailable.");
            else if (aps < programme.getApsRequired())
                gaps.add("Institution APS: " + aps + "; required " + programme.getApsRequired());
        }
        for (String condition :
                new String[] {
                    programme.getGeneralAdmissionRequirements(),
                    programme.getProgrammeAdmissionRequirements(),
                    programme.getAdditionalSelectionRequirements()
                }) if (condition != null && !condition.isBlank()) unknown.add(condition);
        if (results.stream().anyMatch(s -> !s.verified()))
            unknown.add("Student results are not all reviewed.");
        if (checks.isEmpty() && programme.getApsRequired() == null)
            unknown.add("No machine-comparable requirements are recorded.");
        String status =
                !gaps.isEmpty()
                        ? GAP
                        : !compatible
                                        || checks.stream()
                                                .anyMatch(c -> c.status().equals("Unknown"))
                                        || programme.getApsRequired() != null && aps == null
                                        || checks.isEmpty() && programme.getApsRequired() == null
                                ? INSUFFICIENT
                                : unknown.isEmpty() ? MEETS : POTENTIAL;
        return new Eligibility(
                status,
                aps,
                programme.getApsRequired(),
                programme.getApsRule(),
                checks,
                gaps,
                unknown);
    }
}
