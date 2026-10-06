package com.edurite.student;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentProfileCompletionService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ProfileCompletenessTest {
    @Test void transcriptAloneSatisfiesDocumentWeightAndCvCannotSubstitute() {
        var p = new StudentProfile();
        p.setFirstName("Test"); p.setLastName("Student"); p.setPhone("0123456789");
        p.setDateOfBirth(java.time.LocalDate.of(2005,1,1)); p.setQualificationLevel("High School");
        p.setInterests("Technology"); p.setSkills("Problem solving");
        var service = new StudentProfileCompletionService();
        assertThat(service.calculateCompleteness(p)).isEqualTo(75);
        p.setCvFileUrl("cv"); assertThat(service.calculateCompleteness(p)).isEqualTo(75);
        p.setTranscriptFileUrl("transcript"); assertThat(service.calculateCompleteness(p)).isEqualTo(100);
        p.setCvFileUrl(null); assertThat(service.calculateCompleteness(p)).isEqualTo(100);
    }
}
