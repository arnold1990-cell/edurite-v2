package com.edurite.ai.university;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityPageClassifierTest {

    private final UniversityPageClassifier classifier = new UniversityPageClassifier();

    @Test
    void classifiesQualificationListAndExtractsKeywordsFromFixture() throws IOException {
        String fixture = readFixture("qualification-list-page.txt");

        UniversityPageType type = classifier.classify("Undergraduate qualifications", fixture);

        assertThat(type).isEqualTo(UniversityPageType.QUALIFICATION_LIST);
        assertThat(classifier.extractKeywords("Undergraduate qualifications", fixture))
                .contains("computer science", "information systems", "accounting");
    }

    @Test
    void qualificationsShouldWinWhenAdmissionsWordingAlsoExists() throws IOException {
        String fixture = readFixture("qualification-list-page.txt");

        UniversityPageType type = classifier.classify("Apply for admission - all qualifications", fixture);

        assertThat(type).isEqualTo(UniversityPageType.QUALIFICATION_LIST);
    }

    @Test
    void genericAdmissionsPageStaysAdmissionsOverview() throws IOException {
        String fixture = readFixture("admissions-overview-page.txt");

        UniversityPageType type = classifier.classify("Admissions", fixture);

        assertThat(type).isEqualTo(UniversityPageType.ADMISSIONS_OVERVIEW);
    }


    @Test
    void deprioritizesNewsAndPrivacyLinksButNotProgrammeLinks() {
        assertThat(classifier.shouldDeprioritizeLink("https://uni.ac.za/news/latest", "Latest News")).isTrue();
        assertThat(classifier.shouldDeprioritizeLink("https://uni.ac.za/privacy", "Privacy policy")).isTrue();
        assertThat(classifier.shouldDeprioritizeLink("https://uni.ac.za/programmes", "Find undergraduate programmes")).isFalse();
    }

    private String readFixture(String name) throws IOException {
        Path path = Path.of("src", "test", "resources", "fixtures", "university", name);
        return Files.readString(path);
    }
}

