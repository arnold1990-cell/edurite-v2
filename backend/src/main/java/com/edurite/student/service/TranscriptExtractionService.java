package com.edurite.student.service;

import com.edurite.student.dto.StudentSubjectAchievementDto;
import java.util.*;
import java.util.regex.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class TranscriptExtractionService {
    private final StudentSubjectCatalogue catalogue;
    public TranscriptExtractionService(StudentSubjectCatalogue catalogue) { this.catalogue = catalogue; }
    public record Extraction(String status, String message, String grade, String year, List<StudentSubjectAchievementDto> subjects) {}
    public Extraction extract(byte[] bytes, String documentId) {
        try (var pdf = Loader.loadPDF(bytes)) {
            if (pdf.getNumberOfPages() > 30) return failed("The report exceeds 30 pages. Upload a shorter academic report.");
            String text = new PDFTextStripper().getText(pdf);
            if (text.isBlank()) return failed("This scanned report needs OCR or manual review. No marks were changed.");
            return parse(text, documentId);
        } catch (Exception ex) {
            return failed("Automatic extraction supports text-based PDF reports. The document is stored; enter marks manually or request review.");
        }
    }
    public Extraction parse(String text, String documentId) {
        Map<String, StudentSubjectAchievementDto> found = new LinkedHashMap<>();
        Set<String> ambiguous = new HashSet<>();
        Set<String> names = new HashSet<>();
        catalogue.subjects().forEach(s -> names.add(s.name()));
        for (String line : text.split("\\R")) {
            // Only accept an unambiguous subject + percentage row. Multiple term columns need review.
            Matcher row = Pattern.compile("^\\s*([\\p{L} &/-]+?)\\s*[:|]?\\s+(100|[0-9]{1,2})\\s*%\\s*$").matcher(line);
            if (!row.matches()) continue;
            String name = catalogue.canonicalize(row.group(1));
            if (!names.contains(name)) continue;
            int mark = Integer.parseInt(row.group(2));
            int level = mark >= 80 ? 7 : mark >= 70 ? 6 : mark >= 60 ? 5 : mark >= 50 ? 4 : mark >= 40 ? 3 : mark >= 30 ? 2 : 1;
            if (found.containsKey(name) && found.get(name).markPercentage() != mark) ambiguous.add(name);
            found.put(name, new StudentSubjectAchievementDto(name, level, mark, "TRANSCRIPT", false, documentId, java.time.OffsetDateTime.now().toString()));
        }
        ambiguous.forEach(found::remove);
        Matcher grade = Pattern.compile("(?i)\\bGrade\\s+(8|9|10|11|12)\\b").matcher(text);
        Matcher year = Pattern.compile("\\b(20[0-9]{2})\\b").matcher(text);
        return new Extraction(found.isEmpty() ? "REVIEW_REQUIRED" : "EXTRACTED",
            found.isEmpty() ? "No unambiguous subject percentages could be extracted. Your document is stored; review your academic profile."
                : "Extracted marks have populated your academic profile. Check them below. They are not verified until reviewed by an authorised administrator.",
            grade.find() ? "Grade " + grade.group(1) : null, year.find() ? year.group(1) : null, new ArrayList<>(found.values()));
    }
    private Extraction failed(String message) { return new Extraction("REVIEW_REQUIRED", message, null, null, List.of()); }
}
