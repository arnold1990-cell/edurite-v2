package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.ai.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
public class ResearchNarrativeService {
    private final GeminiService gemini;
    private final ObjectMapper mapper;

    public ResearchNarrativeService(GeminiService gemini, ObjectMapper mapper) {
        this.gemini = gemini;
        this.mapper = mapper;
    }

    public Narrative generate(
            AcademicAnalysis academic,
            List<CareerFinding> careers,
            List<ProgrammeFinding> programmes,
            List<String> limitations) {
        return generate(academic, careers, programmes, List.of(), limitations);
    }

    public Narrative generate(
            AcademicAnalysis academic,
            List<CareerFinding> careers,
            List<ProgrammeFinding> programmes,
            List<com.edurite.bursary.dto.BursaryResultDto> funding,
            List<String> limitations) {
        try {
            // Deliberately omit name/contact details, document IDs and storage URLs.
            List<Map<String, Object>> subjects = new ArrayList<>();
            for (var s : academic.subjects()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("subject", s.subjectName());
                values.put("mark", s.markPercentage());
                values.put("level", s.achievementLevel());
                values.put("curriculum", s.curriculum());
                values.put("examination", s.examination());
                subjects.add(values);
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("grade", academic.grade());
            evidence.put("subjects", subjects);
            evidence.put("interests", academic.interests());
            evidence.put("skills", academic.skills());
            evidence.put("goals", academic.goals());
            evidence.put("academicProgression", academic.progression());
            evidence.put(
                    "careerEvidence",
                    careers.stream()
                            .limit(40)
                            .map(c -> Map.of("title", c.title(), "reason", c.explanation()))
                            .toList());
            evidence.put("programmes", programmes.stream().limit(60).toList());
            evidence.put(
                    "funding",
                    funding.stream()
                            .limit(20)
                            .map(
                                    f -> {
                                        Map<String, Object> item = new LinkedHashMap<>();
                                        item.put("title", f.title());
                                        item.put("provider", f.provider());
                                        item.put("publishedDeadline", f.deadline());
                                        item.put("sourceUrl", f.applicationLink());
                                        item.put("availabilityNote", f.dataFreshnessNote());
                                        return item;
                                    })
                            .toList());
            evidence.put("limitations", limitations);
            String raw = gemini.getCareerResearchNarrative(mapper.writeValueAsString(evidence));
            if (raw == null || raw.length() > 60_000)
                throw new IllegalArgumentException("Invalid AI report response");
            String json =
                    raw.strip().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            com.fasterxml.jackson.databind.JsonNode node =
                    mapper.reader()
                            .with(
                                    com.fasterxml.jackson.databind.DeserializationFeature
                                            .FAIL_ON_TRAILING_TOKENS)
                            .readTree(json);
            if (!node.isObject()
                    || !node.path("assessment").isTextual()
                    || !node.path("researchSummary").isTextual()
                    || !node.path("nextSteps").isArray()
                    || !node.path("sourceUrls").isArray())
                throw new IllegalArgumentException("AI report response types are invalid");
            for (var value : node.path("nextSteps"))
                if (!value.isTextual())
                    throw new IllegalArgumentException("AI next steps must be text");
            for (var value : node.path("sourceUrls"))
                if (!value.isTextual())
                    throw new IllegalArgumentException("AI source references must be text");
            Set<String> keys = new HashSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(Set.of("assessment", "researchSummary", "nextSteps", "sourceUrls")))
                throw new IllegalArgumentException("AI report response schema is invalid");
            Narrative narrative = mapper.treeToValue(node, Narrative.class);
            if (narrative.assessment() == null
                    || narrative.assessment().isBlank()
                    || narrative.researchSummary() == null
                    || narrative.researchSummary().isBlank()
                    || narrative.nextSteps() == null
                    || narrative.nextSteps().isEmpty()
                    || narrative.nextSteps().size() > 20
                    || narrative.nextSteps().stream().anyMatch(s -> s == null || s.isBlank())
                    || narrative.sourceUrls() == null)
                throw new IllegalArgumentException("AI report is incomplete");
            Set<String> allowed = new HashSet<>();
            programmes.stream()
                    .limit(60)
                    .forEach(
                            p -> {
                                if (p.sourceUrl() != null) allowed.add(p.sourceUrl());
                            });
            funding.stream()
                    .limit(20)
                    .forEach(
                            f -> {
                                String source =
                                        ResearchCatalogueService.safeUrl(f.applicationLink());
                                if (source != null) allowed.add(source);
                            });
            if (!allowed.containsAll(narrative.sourceUrls()))
                throw new IllegalArgumentException("AI report referenced an unsupported source");
            String prose =
                    narrative.assessment()
                            + " "
                            + narrative.researchSummary()
                            + " "
                            + String.join(" ", narrative.nextSteps());
            if (prose.matches("(?s).*https?://.*"))
                throw new IllegalArgumentException("AI report inserted an unverified URL");
            Set<String> actualMarks = new HashSet<>();
            academic.subjects()
                    .forEach(
                            s -> {
                                if (s.markPercentage() != null)
                                    actualMarks.add(s.markPercentage() + "%");
                            });
            academic.progression()
                    .forEach(
                            p -> {
                                actualMarks.add(p.previousMark() + "%");
                                actualMarks.add(p.currentMark() + "%");
                            });
            Matcher percentages = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*%").matcher(prose);
            while (percentages.find())
                if (!actualMarks.contains(
                        new java.math.BigDecimal(percentages.group(1))
                                        .stripTrailingZeros()
                                        .toPlainString()
                                + "%"))
                    throw new IllegalArgumentException(
                            "AI report introduced an unsupported percentage");
            return new Narrative(
                    narrative.assessment(),
                    narrative.researchSummary(),
                    narrative.nextSteps().stream().distinct().toList(),
                    narrative.sourceUrls().stream().distinct().toList());
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("AI report could not be validated", ex);
        }
    }
}
