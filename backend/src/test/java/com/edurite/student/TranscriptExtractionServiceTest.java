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
}
