package com.edurite.student;

import com.edurite.student.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class TranscriptExtractionServiceTest {
    private TranscriptExtractionService service() {
        var catalogue = mock(StudentSubjectCatalogue.class);
        when(catalogue.subjects()).thenReturn(List.of(new StudentSubjectCatalogue.Subject("Mathematics", "FET", false, null)));
        when(catalogue.canonicalize(anyString())).thenAnswer(i -> i.getArgument(0));
        return new TranscriptExtractionService(catalogue);
    }
    @Test void extractsOnlyUnambiguousPercentagesWithProvenance() {
        var result = service().parse("Grade 12 2026\nMathematics 68%\nUnknown Subject 90%", "document-1");
        assertThat(result.subjects()).hasSize(1);
        var mark = result.subjects().getFirst();
        assertThat(mark.markPercentage()).isEqualTo(68);
        assertThat(mark.achievementLevel()).isEqualTo(5);
        assertThat(mark.source()).isEqualTo("TRANSCRIPT");
        assertThat(mark.documentId()).isEqualTo("document-1");
        assertThat(mark.verified()).isFalse();
        assertThat(result.grade()).isEqualTo("Grade 12");
    }
    @Test void refusesAmbiguousTermsAndConflictingDuplicateRows() {
        assertThat(service().parse("Mathematics 75%\nMathematics 68%", "doc").subjects()).isEmpty();
        assertThat(service().parse("Mathematics 75 68 70", "doc").subjects()).isEmpty();
    }
    @Test void preservesContextAndReadsPercentagesWithExplicitHeader() {
        var result = service().parse("Grade 12 2026\nCurriculum: CAPS\nGrading scale: NSC percentage\nExamination: Final\nMathematics 72", "doc");
        assertThat(result.subjects()).hasSize(1);
        var row = result.subjects().getFirst();
        assertThat(row.curriculum()).isEqualTo("CAPS");
        assertThat(row.academicYear()).isEqualTo("2026");
        assertThat(row.gradingScale()).isEqualTo("NSC percentage");
        assertThat(row.examination()).isEqualTo("Final");
        assertThat(service().parse("Mathematics 7", "doc").subjects()).isEmpty();
    }
    @Test void readsActualPdfBytesAndDocxTable() throws Exception {
        byte[] pdfBytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new java.io.ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                content.beginText(); content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Mathematics 68%"); content.endText();
            }
            pdf.save(output); pdfBytes = output.toByteArray();
        }
        assertThat(service().extract(pdfBytes, "report.pdf").subjects().getFirst().markPercentage()).isEqualTo(68);
        var output = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(output)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:tbl><w:tr>"
                    + "<w:tc><w:p><w:r><w:t>Mathematics</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>75%</w:t></w:r></w:p></w:tc>"
                    + "</w:tr></w:tbl></w:body></w:document>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        assertThat(service().extract(output.toByteArray(), "report.docx").subjects().getFirst().markPercentage()).isEqualTo(75);
    }
    @Test void unreadableDocumentNeedsReviewWithoutInventedMarks() {
        var result = service().extract(new byte[]{1, 2, 3}, "report.pdf");
        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.subjects()).isEmpty();
    }

}
