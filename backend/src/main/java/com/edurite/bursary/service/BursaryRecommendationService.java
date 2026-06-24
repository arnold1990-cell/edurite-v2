package com.edurite.bursary.service;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BursaryRecommendationService {

    private final StudentService studentService;
    private final BursaryAggregationService bursaryAggregationService;

    public BursaryRecommendationService(StudentService studentService, BursaryAggregationService bursaryAggregationService) {
        this.studentService = studentService;
        this.bursaryAggregationService = bursaryAggregationService;
    }

    public List<BursaryResultDto> recommendForStudent(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        BursarySearchRequest request = new BursarySearchRequest(
                profile.getInterests(),
                profile.getQualificationLevel(),
                profile.getLocation(),
                profile.getSkills(),
                0,
                20
        );

        return bursaryAggregationService.search(request).items().stream()
                .map(item -> new BursaryResultDto(
                        item.externalId(),
                        item.title(),
                        item.provider(),
                        item.description(),
                        item.qualificationLevel(),
                        item.region(),
                        item.eligibility(),
                        item.deadline(),
                        item.applicationLink(),
                        item.sourceType(),
                        calculateRelevance(item, profile),
                        item.sourceUrls(),
                        item.officialSource(),
                        item.incomplete(),
                        item.dataFreshnessNote()
                ))
                .sorted(Comparator.comparing(BursaryResultDto::officialSource).reversed()
                        .thenComparing(Comparator.comparingInt(BursaryResultDto::relevanceScore).reversed()))
                .toList();
    }

    private int calculateRelevance(BursaryResultDto item, StudentProfile profile) {
        int score = item.relevanceScore();
        score += contains(item.qualificationLevel(), profile.getQualificationLevel()) ? 12 : 0;
        score += contains(item.region(), profile.getLocation()) ? 10 : 0;
        score += contains(item.eligibility(), profile.getSkills()) ? 8 : 0;
        score += contains(item.description(), profile.getCareerGoals()) ? 6 : 0;
        return Math.min(100, score);
    }

    private boolean contains(String left, String right) {
        String a = left == null ? "" : left.toLowerCase(Locale.ROOT);
        String b = right == null ? "" : right.toLowerCase(Locale.ROOT);
        return !a.isBlank() && !b.isBlank() && (a.contains(b) || b.contains(a));
    }
}

