package com.edurite.ai.university;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "crawled_university_pages", indexes = {
        @Index(name = "idx_crawled_pages_university", columnList = "university_name"),
        @Index(name = "idx_crawled_pages_status", columnList = "crawl_status"),
        @Index(name = "idx_crawled_pages_last_crawled", columnList = "last_crawled_at")
})
public class CrawledUniversityPage extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String universityName;

    @Column(nullable = false, unique = true, length = 1200)
    private String sourceUrl;

    @Column(length = 500)
    private String pageTitle;

    @Column(length = 80)
    private String pageType;

    @Column(length = 80)
    private String qualificationLevel;

    @Column(length = 180)
    private String facultyName;

    @Column(length = 180)
    private String campusName;

    @ElementCollection
    @CollectionTable(name = "crawled_university_page_keywords", joinColumns = @JoinColumn(name = "page_id"))
    @Column(name = "keyword", length = 120)
    private Set<String> extractedKeywords = new LinkedHashSet<>();

    @Column(columnDefinition = "TEXT")
    private String cleanedContent;

    @Column(length = 128)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlStatus crawlStatus = CrawlStatus.SKIPPED;

    private OffsetDateTime lastCrawledAt;
    private OffsetDateTime lastSuccessfulCrawledAt;
    private OffsetDateTime lastFailureAt;

    @Column(length = 100)
    private String errorType;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String summaryExcerpt;

}

