package com.edurite.career.controller;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/careers", "/api/careers"})
public class CareerController {

    private final CareerRepository careerRepository;

    public CareerController(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    @GetMapping
    public Page<Career> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String field,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String demand,
            @RequestParam(defaultValue = "") String salaryRange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String industryFilter = !industry.isBlank() ? industry : field;
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Career> careers = careerRepository.search(
                normalize(q),
                industryFilter,
                normalize(qualificationLevel),
                normalize(location),
                normalize(demand),
                normalize(salaryRange),
                pageRequest);
        var ranked = careers.getContent().stream()
                .peek(career -> career.setMatchScore(computeMatchScore(
                        career,
                        q,
                        industryFilter,
                        qualificationLevel,
                        location,
                        demand,
                        salaryRange
                )))
                .sorted(Comparator.comparing(Career::getMatchScore).reversed())
                .toList();
        return new PageImpl<>(ranked, pageRequest, careers.getTotalElements());
    }

    @GetMapping("/{id}")
    public Career get(@PathVariable UUID id) { return careerRepository.findById(id).orElseThrow(); }

    private int computeMatchScore(
            Career career,
            String q,
            String industry,
            String qualificationLevel,
            String location,
            String demand,
            String salaryRange
    ) {
        int score = 35;
        score += scoreForQueryMatch(q, career.getTitle(), career.getDescription(), career.getIndustry());
        score += scoreForFilter(industry, career.getIndustry(), 20);
        score += scoreForFilter(qualificationLevel, career.getQualificationLevel(), 15);
        score += scoreForFilter(location, career.getLocation(), 15);
        score += scoreForFilter(demand, career.getDemandLevel(), 10);
        score += scoreForFilter(salaryRange, career.getSalaryRange(), 5);
        return Math.clamp(score, 0, 100);
    }

    private int scoreForQueryMatch(String query, String... values) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return 0;
        }
        for (String value : values) {
            if (containsNormalized(value, normalizedQuery)) {
                return 25;
            }
        }
        return 0;
    }

    private int scoreForFilter(String filter, String target, int score) {
        String normalizedFilter = normalize(filter);
        if (normalizedFilter.isBlank()) {
            return 0;
        }
        return containsNormalized(target, normalizedFilter) ? score : 0;
    }

    private boolean containsNormalized(String value, String normalizedNeedle) {
        String normalizedValue = normalize(value);
        if (normalizedNeedle.isBlank() || normalizedValue.isBlank()) {
            return false;
        }
        String compactNeedle = normalizedNeedle.replace(" ", "");
        return normalizedValue.contains(normalizedNeedle)
                || normalizedValue.replace(" ", "").contains(compactNeedle);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

