package com.edurite.curriculum.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.curriculum.entity.AtpCalendarItem;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.repository.AtpCalendarItemRepository;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAtpExtractionService {

    private static final Pattern WEEK_PATTERN = Pattern.compile("(?i)week\\s*(\\d{1,2})[:\\-\\s]+(.+)");
    private static final Pattern TERM_PATTERN = Pattern.compile("(?i)term\\s*(\\d)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");

    private final CurriculumAssetRepository curriculumAssetRepository;
    private final AtpCalendarItemRepository atpCalendarItemRepository;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;
    private final ObjectMapper objectMapper;

    public AiAtpExtractionService(
            CurriculumAssetRepository curriculumAssetRepository,
            AtpCalendarItemRepository atpCalendarItemRepository,
            AiProviderOrchestratorService aiProviderOrchestratorService,
            ObjectMapper objectMapper
    ) {
        this.curriculumAssetRepository = curriculumAssetRepository;
        this.atpCalendarItemRepository = atpCalendarItemRepository;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<AtpCalendarItem> extractAtpCalendarFromPdf(UUID curriculumResourceId, UUID actorUserId) {
        CurriculumAsset asset = curriculumAssetRepository.findById(curriculumResourceId)
                .orElseThrow(() -> new ResourceConflictException("Curriculum resource not found."));
        if (!"ATP".equalsIgnoreCase(asset.getRepositoryType())) {
            throw new ResourceConflictException("AI ATP extraction is only supported for ATP resources.");
        }
        if ((asset.getPdfBytes() == null || asset.getPdfBytes().length == 0) && (asset.getPdfBase64() == null || asset.getPdfBase64().isBlank())) {
            markFailed(asset, "No PDF file is attached to this ATP resource.");
            throw new ResourceConflictException("ATP PDF is missing.");
        }

        String pdfText = extractPdfText(asset);
        if (pdfText.isBlank()) {
            markFailed(asset, "The uploaded PDF did not contain readable text.");
            throw new ResourceConflictException("AI could not read the ATP PDF.");
        }

        ExtractionPayload payload = tryAiExtraction(asset, pdfText);
        if (payload == null || payload.calendarItems() == null || payload.calendarItems().isEmpty()) {
            payload = heuristicExtraction(asset, pdfText);
        }
        if (payload == null || payload.calendarItems() == null || payload.calendarItems().isEmpty()) {
            markFailed(asset, "AI could not confidently extract weekly ATP topics. Please review the document or manually map the calendar.");
            throw new ResourceConflictException("AI could not confidently extract weekly ATP topics. Please review the document or manually map the calendar.");
        }

        ExtractionPayload resolvedPayload = payload;
        atpCalendarItemRepository.deleteByCurriculumResourceId(curriculumResourceId);
        List<AtpCalendarItem> saved = resolvedPayload.calendarItems().stream()
                .filter(this::isUsableCalendarItem)
                .sorted(Comparator.comparing(ExtractionCalendarItem::term).thenComparing(ExtractionCalendarItem::weekNumber))
                .map(item -> toEntity(asset, actorUserId, resolvedPayload, item))
                .map(atpCalendarItemRepository::save)
                .toList();

        if (saved.isEmpty()) {
            markFailed(asset, "AI extracted weekly rows, but the content was too vague to publish. Review the ATP or map the calendar manually.");
            throw new ResourceConflictException("AI extracted weekly rows, but the content was too vague to publish. Review the ATP or map the calendar manually.");
        }

        asset.setExtractionStatus("EXTRACTED");
        asset.setExtractionError(null);
        asset.setExtractedAt(OffsetDateTime.now());
        curriculumAssetRepository.save(asset);
        return saved;
    }

    private AtpCalendarItem toEntity(CurriculumAsset asset, UUID actorUserId, ExtractionPayload payload, ExtractionCalendarItem item) {
        AtpCalendarItem entity = new AtpCalendarItem();
        entity.setCurriculumResourceId(asset.getId());
        entity.setSubject(firstNonBlank(item.subject(), firstNonBlank(payload.subject(), asset.getSubject())));
        entity.setGrade(normalizeGrade(firstNonBlank(item.grade(), firstNonBlank(payload.grade(), asset.getGrade()))));
        entity.setPhase(firstNonBlank(item.phase(), firstNonBlank(payload.phase(), asset.getCurriculumPhase())));
        entity.setAcademicYear(item.academicYear() == null ? firstNonNull(payload.academicYear(), asset.getAcademicYear()) : item.academicYear());
        entity.setTerm(normalizeTerm(item.term()));
        entity.setWeekNumber(item.weekNumber());
        entity.setStartDate(item.startDate());
        entity.setEndDate(item.endDate());
        entity.setTopic(item.topic().trim());
        entity.setSubtopic(trim(item.subtopic()));
        entity.setLearningObjectives(trim(item.learningObjectives()));
        entity.setResources(trim(item.resources()));
        entity.setAssessmentTask(trim(item.assessmentTask()));
        entity.setLessonFocus(trim(item.lessonFocus()));
        entity.setNotes(trim(item.notes()));
        entity.setStatus("DRAFT");
        entity.setCreatedBy(actorUserId);
        return entity;
    }

    private ExtractionPayload tryAiExtraction(CurriculumAsset asset, String pdfText) {
        try {
            String prompt = """
                    You are extracting an Annual Teaching Plan from a district ATP PDF.
                    Return ONLY strict JSON. No markdown, no prose, no code fences.

                    Required top-level fields:
                    {
                      "subject": "string",
                      "grade": "string",
                      "phase": "string or null",
                      "academicYear": 2026,
                      "calendarItems": [
                        {
                          "subject": "string or null",
                          "grade": "string or null",
                          "phase": "string or null",
                          "academicYear": 2026,
                          "term": "Term 1",
                          "weekNumber": 1,
                          "startDate": "2026-01-14",
                          "endDate": "2026-01-18",
                          "topic": "string",
                          "subtopic": "string or null",
                          "learningObjectives": "string or null",
                          "resources": "string or null",
                          "assessmentTask": "string or null",
                          "lessonFocus": "string or null",
                          "notes": "string or null"
                        }
                      ]
                    }

                    Use the ATP content. If a field is absent, use null.
                    Preserve week order. Use "Term N" format.

                    Known metadata:
                    Subject: %s
                    Grade: %s
                    Phase: %s
                    Academic year: %s

                    ATP text:
                    %s
                    """.formatted(
                    safe(asset.getSubject()),
                    safe(asset.getGrade()),
                    safe(asset.getCurriculumPhase()),
                    asset.getAcademicYear() == null ? "" : asset.getAcademicYear(),
                    truncate(pdfText, 18000)
            );
            String raw = aiProviderOrchestratorService.generateContent(prompt);
            return objectMapper.readValue(raw, ExtractionPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private ExtractionPayload heuristicExtraction(CurriculumAsset asset, String pdfText) {
        List<ExtractionCalendarItem> items = new ArrayList<>();
        String currentTerm = firstNonBlank(asset.getTerm(), "Term 1");
        WeekBlock currentBlock = null;
        for (String rawLine : pdfText.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            Matcher termMatcher = TERM_PATTERN.matcher(line);
            if (termMatcher.find()) {
                currentTerm = "Term " + termMatcher.group(1);
            }
            Matcher weekMatcher = WEEK_PATTERN.matcher(line);
            if (!weekMatcher.find()) {
                if (currentBlock != null && !looksLikeNoise(line)) {
                    currentBlock.lines().add(line);
                }
                continue;
            }
            if (currentBlock != null) {
                ExtractionCalendarItem extracted = extractWeekBlock(asset, currentBlock);
                if (extracted != null) {
                    items.add(extracted);
                }
            }
            Integer weekNumber = parseInteger(weekMatcher.group(1));
            if (weekNumber == null) {
                currentBlock = null;
                continue;
            }
            currentBlock = new WeekBlock(currentTerm, weekNumber, new ArrayList<>());
            String topicSeed = cleanTopic(weekMatcher.group(2));
            if (!topicSeed.isBlank()) {
                currentBlock.lines().add(topicSeed);
            }
        }
        if (currentBlock != null) {
            ExtractionCalendarItem extracted = extractWeekBlock(asset, currentBlock);
            if (extracted != null) {
                items.add(extracted);
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        return new ExtractionPayload(asset.getSubject(), asset.getGrade(), asset.getCurriculumPhase(), asset.getAcademicYear(), items);
    }

    private ExtractionCalendarItem extractWeekBlock(CurriculumAsset asset, WeekBlock block) {
        String topic = cleanTopic(firstNonBlank(findLabeledValue(block.lines(), "topic"), firstMeaningfulLine(block.lines())));
        if (topic.isBlank() || looksLikeInvalidTopic(topic)) {
            return null;
        }
        String subtopic = cleanOptionalValue(findLabeledValue(block.lines(), "subtopic", "sub-topic", "sub concept", "sub-concept", "concept"));
        if ((subtopic == null || subtopic.isBlank()) && block.lines().size() > 1) {
            String secondLine = cleanOptionalValue(block.lines().get(1));
            if (secondLine != null && !looksLikeLabeledLine(secondLine) && !looksLikeInvalidTopic(secondLine)) {
                subtopic = secondLine;
            }
        }
        String learningObjectives = cleanOptionalValue(findLabeledValue(block.lines(), "objective", "objectives", "learning objective", "learning objectives", "learning outcome", "learning outcomes"));
        String lessonFocus = cleanOptionalValue(findLabeledValue(block.lines(), "activity", "activities", "class activity", "classroom activity", "suggested activity", "suggested activities", "teaching activity", "teaching activities"));
        String assessmentTask = cleanOptionalValue(findLabeledValue(block.lines(), "assessment", "assessment task", "assessment tasks", "assessment activity", "assessment activities"));
        String resources = cleanOptionalValue(findLabeledValue(block.lines(), "resource", "resources", "media", "materials", "resource / media", "resources / media"));
        String notes = cleanOptionalValue(collectResidualNotes(block.lines()));
        List<String> dates = extractDates(String.join(" ", block.lines()));
        return new ExtractionCalendarItem(
                asset.getSubject(),
                asset.getGrade(),
                asset.getCurriculumPhase(),
                asset.getAcademicYear(),
                block.term(),
                block.weekNumber(),
                dates.size() > 0 ? parseDate(dates.get(0)) : null,
                dates.size() > 1 ? parseDate(dates.get(1)) : null,
                topic,
                subtopic,
                learningObjectives,
                resources,
                assessmentTask,
                lessonFocus,
                notes
        );
    }

    private String extractPdfText(CurriculumAsset asset) {
        try {
            byte[] bytes = asset.getPdfBytes() != null && asset.getPdfBytes().length > 0
                    ? asset.getPdfBytes()
                    : Base64.getDecoder().decode(asset.getPdfBase64());
            try (PDDocument document = Loader.loadPDF(new ByteArrayInputStream(bytes).readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return firstNonBlank(stripper.getText(document), "");
            }
        } catch (Exception ex) {
            return "";
        }
    }

    private void markFailed(CurriculumAsset asset, String error) {
        asset.setExtractionStatus("EXTRACTION_FAILED");
        asset.setExtractionError(error);
        asset.setExtractedAt(OffsetDateTime.now());
        curriculumAssetRepository.save(asset);
    }

    private List<String> extractDates(String line) {
        List<String> dates = new ArrayList<>();
        Matcher matcher = DATE_PATTERN.matcher(line);
        while (matcher.find()) {
            dates.add(matcher.group(1));
        }
        return dates;
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private String normalizeTerm(String value) {
        String trimmed = firstNonBlank(value, "Term 1").trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        return digits.isBlank() ? trimmed : "Term " + digits;
    }

    private String normalizeGrade(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.toLowerCase(Locale.ROOT).startsWith("grade") ? trimmed : "Grade " + trimmed;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return safe(value);
        }
        return value.substring(0, max);
    }

    private String cleanTopic(String value) {
        String cleaned = firstNonBlank(value, "").replaceAll("\\s{2,}", " ").trim();
        cleaned = cleaned.replaceAll("(?i)learning objectives.*$", "").trim();
        return cleaned;
    }

    private boolean isUsableCalendarItem(ExtractionCalendarItem item) {
        return item != null
                && item.weekNumber() != null
                && item.term() != null
                && item.topic() != null
                && !item.topic().isBlank()
                && !looksLikeInvalidTopic(item.topic());
    }

    private String cleanOptionalValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s{2,}", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String firstMeaningfulLine(List<String> lines) {
        for (String line : lines) {
            String cleaned = cleanOptionalValue(line);
            if (cleaned == null || looksLikeLabeledLine(cleaned) || looksLikeInvalidTopic(cleaned)) {
                continue;
            }
            return cleaned;
        }
        return null;
    }

    private String findLabeledValue(List<String> lines, String... labels) {
        for (String line : lines) {
            String match = extractLabeledValue(line, labels);
            if (match != null && !match.isBlank()) {
                return match;
            }
        }
        return null;
    }

    private String extractLabeledValue(String line, String... labels) {
        if (line == null || line.isBlank()) {
            return null;
        }
        for (String label : labels) {
            Pattern pattern = Pattern.compile("(?i)^" + Pattern.quote(label) + "\\s*[:\\-]\\s*(.+)$");
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String collectResidualNotes(List<String> lines) {
        List<String> residual = new ArrayList<>();
        for (String line : lines) {
            String cleaned = cleanOptionalValue(line);
            if (cleaned == null || looksLikeLabeledLine(cleaned) || looksLikeInvalidTopic(cleaned)) {
                continue;
            }
            residual.add(cleaned);
        }
        if (residual.size() <= 1) {
            return null;
        }
        return String.join(" | ", residual.subList(1, residual.size()));
    }

    private boolean looksLikeLabeledLine(String line) {
        return extractLabeledValue(line,
                "topic",
                "subtopic",
                "sub-topic",
                "sub concept",
                "sub-concept",
                "concept",
                "objective",
                "objectives",
                "learning objective",
                "learning objectives",
                "learning outcome",
                "learning outcomes",
                "activity",
                "activities",
                "class activity",
                "classroom activity",
                "suggested activity",
                "suggested activities",
                "teaching activity",
                "teaching activities",
                "assessment",
                "assessment task",
                "assessment tasks",
                "assessment activity",
                "assessment activities",
                "resource",
                "resources",
                "media",
                "materials",
                "resource / media",
                "resources / media"
        ) != null;
    }

    private boolean looksLikeNoise(String line) {
        String normalized = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.matches("^page\\s*\\d+.*$")) {
            return true;
        }
        return normalized.matches("^(week\\s*\\d+\\s*){2,}$");
    }

    private boolean looksLikeInvalidTopic(String topic) {
        String normalized = topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.matches("^(week\\s*\\d+\\s*){2,}$")) {
            return true;
        }
        Matcher matcher = Pattern.compile("(?i)\\bweek\\b").matcher(topic);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExtractionPayload(
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            List<ExtractionCalendarItem> calendarItems
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExtractionCalendarItem(
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            LocalDate startDate,
            LocalDate endDate,
            String topic,
            String subtopic,
            String learningObjectives,
            String resources,
            String assessmentTask,
            String lessonFocus,
            String notes
    ) {}

    private record WeekBlock(
            String term,
            Integer weekNumber,
            List<String> lines
    ) {}
}
