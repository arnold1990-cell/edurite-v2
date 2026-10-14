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
        if (documentId.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            try { return parse(readDocx(bytes), documentId); }
            catch (Exception ex) { return failed("The Word document could not be read. Upload a text-based PDF or correct results manually."); }
        }
        try (var pdf = Loader.loadPDF(bytes)) {
            if (pdf.getNumberOfPages() > 30) return failed("The report exceeds 30 pages. Upload a shorter academic report.");
            String text = new PDFTextStripper().getText(pdf);
            if (text.isBlank()) return failed("This scanned report needs OCR or manual review. No marks were changed.");
            return parse(text, documentId);
        } catch (Exception ex) {
            return failed("Automatic extraction supports text-based PDF and DOCX reports. The document is stored; enter marks manually or request review.");
        }
    }
    public Extraction parse(String text, String documentId) {
        Map<String, StudentSubjectAchievementDto> found = new LinkedHashMap<>();
        Set<String> ambiguous = new HashSet<>();
        boolean percentageHeader = Pattern.compile("(?i)(percentage|mark\\s*\\(%\\))").matcher(text).find();
        Set<String> names = new HashSet<>();
        catalogue.subjects().forEach(s -> names.add(s.name()));
        for (String line : text.split("\\R")) {
            // Only accept an unambiguous subject + percentage row. Multiple term columns need review.
            Matcher row = Pattern.compile("^\\s*([\\p{L} &/-]+?)\\s*[:|]?\\s+(100|[0-9]{1,2})\\s*(%)?\\s*$").matcher(line);
            if (!row.matches() || (row.group(3) == null && !percentageHeader)) continue;
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
        String academicYear = year.find() ? year.group(1) : null;
        String curriculum = capture(text, "(?im)^\\s*(?:Curriculum)\\s*:\\s*([^\\r\\n]+)");
        String scale = capture(text, "(?im)^\\s*(?:Grading scale)\\s*:\\s*([^\\r\\n]+)");
        String examination = capture(text, "(?im)^\\s*(?:Examination|Exam|Term)\\s*:\\s*([^\\r\\n]+)");
        List<StudentSubjectAchievementDto> subjects = found.values().stream().map(item -> new StudentSubjectAchievementDto(
                item.subjectName(), item.achievementLevel(), item.markPercentage(), item.source(), false, documentId,
                item.updatedAt(), curriculum, scale, academicYear, examination)).toList();
        String message = found.isEmpty() ? "No unambiguous subject percentages could be extracted. Enter or correct results manually."
                : "Extracted results are pending your review. Choose results, correct them, and save your profile to confirm. Saved marks have not changed.";
        if (!ambiguous.isEmpty()) message += " Conflicting marks found for " + String.join(", ", ambiguous) + "; enter the correct result manually.";
        return new Extraction(found.isEmpty() ? "REVIEW_REQUIRED" : "EXTRACTED", message,
                grade.find() ? "Grade " + grade.group(1) : null, academicYear, subjects);
    }
    private String capture(String text, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
    private String readDocx(byte[] bytes) throws Exception {
        try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) continue;
                byte[] xml = zip.readNBytes(5 * 1024 * 1024 + 1);
                if (xml.length > 5 * 1024 * 1024) throw new IllegalArgumentException("Document too large");
                var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setNamespaceAware(true);
                var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml));
                StringBuilder text = new StringBuilder();
                appendText(document.getDocumentElement(), text);
                return text.toString();
            }
        }
        throw new IllegalArgumentException("Missing document contents");
    }
    private void appendText(org.w3c.dom.Node node, StringBuilder text) {
        if ("t".equals(node.getLocalName())) text.append(node.getTextContent());
        else for (var child = node.getFirstChild(); child != null; child = child.getNextSibling()) appendText(child, text);
        if ("tc".equals(node.getLocalName()) || "tab".equals(node.getLocalName())) text.append(' ');
        if ("tr".equals(node.getLocalName()) || ("p".equals(node.getLocalName()) && !"tc".equals(node.getParentNode().getLocalName()))) text.append('\n');
    }

    private Extraction failed(String message) { return new Extraction("REVIEW_REQUIRED", message, null, null, List.of()); }
}
