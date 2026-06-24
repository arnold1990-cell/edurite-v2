package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityPageRetrievalServiceTest {

    @Mock
    private CrawledUniversityPageRepository repository;

    @InjectMocks
    private UniversityPageRetrievalService service;

    @Test
    void ranksRelevantPagesAndReturnsTopSubset() {
        StudentProfile profile = new StudentProfile();
        profile.setInterests("software engineering");
        profile.setSkills("java");
        profile.setQualificationLevel("Undergraduate");

        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software Engineer", "Undergraduate", 10);

        when(repository.findByActiveTrueAndCrawlStatus(CrawlStatus.SUCCESS)).thenReturn(List.of(
                page("University A", "Software Engineering", "PROGRAMME_DETAIL", "Undergraduate", "software engineering java"),
                page("University B", "History", "QUALIFICATION_LIST", "Undergraduate", "history and arts"),
                page("University C", "Computer Science", "PROGRAMME_DETAIL", "Undergraduate", "computer science software systems")
        ));

        List<UniversityPageSummary> top = service.retrieveTopRelevantPages(profile, request, 2);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).pageTitle()).containsAnyOf("Software Engineering", "Computer Science");
    }

    private CrawledUniversityPage page(String university, String title, String pageType, String qualificationLevel, String content) {
        CrawledUniversityPage page = new CrawledUniversityPage();
        page.setUniversityName(university);
        page.setSourceUrl("https://" + university.toLowerCase().replace(" ", "-") + ".ac.za/" + title.toLowerCase().replace(" ", "-"));
        page.setPageTitle(title);
        page.setPageType(pageType);
        page.setQualificationLevel(qualificationLevel);
        page.setCleanedContent(content);
        page.setSummaryExcerpt(content);
        page.setCrawlStatus(CrawlStatus.SUCCESS);
        page.setActive(true);
        return page;
    }
}

