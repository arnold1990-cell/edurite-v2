package com.edurite.course.controller;

import com.edurite.course.dto.CourseDto;
import com.edurite.course.entity.Course;
import com.edurite.course.repository.CourseRepository;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/courses", "/api/courses"})
public class CourseController {

    private final CourseRepository courseRepository;
    private final InstitutionRepository institutionRepository;

    public CourseController(CourseRepository courseRepository, InstitutionRepository institutionRepository) {
        this.courseRepository = courseRepository;
        this.institutionRepository = institutionRepository;
    }

    @GetMapping
    public Page<CourseDto> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String level,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Course> courses = courseRepository.search(normalize(q), normalize(level), normalize(location), pageRequest);
        Map<UUID, String> institutionNames = resolveInstitutionNames(courses.getContent());
        Map<UUID, String> institutionLocations = resolveInstitutionLocations(courses.getContent());
        List<CourseDto> ranked = courses.getContent().stream()
                .map(course -> toDto(course, institutionNames, institutionLocations, q, level, location))
                .sorted(Comparator.comparing(CourseDto::matchScore).reversed())
                .toList();
        return new PageImpl<>(ranked, pageRequest, courses.getTotalElements());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseDto details(@PathVariable UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        Map<UUID, String> institutionNames = resolveInstitutionNames(List.of(course));
        Map<UUID, String> institutionLocations = resolveInstitutionLocations(List.of(course));
        return toDto(course, institutionNames, institutionLocations, "", "", "");
    }

    private Map<UUID, String> resolveInstitutionNames(List<Course> courses) {
        List<UUID> institutionIds = courses.stream()
                .map(Course::getInstitutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (institutionIds.isEmpty()) {
            return Map.of();
        }
        return institutionRepository.findAllById(institutionIds).stream()
                .collect(Collectors.toMap(Institution::getId, Institution::getName, (existing, replacement) -> existing));
    }

    private Map<UUID, String> resolveInstitutionLocations(List<Course> courses) {
        List<UUID> institutionIds = courses.stream()
                .map(Course::getInstitutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (institutionIds.isEmpty()) {
            return Map.of();
        }
        return institutionRepository.findAllById(institutionIds).stream()
                .collect(Collectors.toMap(Institution::getId, institution -> institution.getLocation() == null ? "" : institution.getLocation(), (existing, replacement) -> existing));
    }

    private CourseDto toDto(
            Course course,
            Map<UUID, String> institutionNames,
            Map<UUID, String> institutionLocations,
            String q,
            String level,
            String location
    ) {
        String institutionName = institutionNames.getOrDefault(course.getInstitutionId(), "");
        String institutionLocation = institutionLocations.getOrDefault(course.getInstitutionId(), "");
        return new CourseDto(
                course.getId(),
                course.getName(),
                institutionName,
                formatDuration(course.getDurationMonths()),
                course.getLevel(),
                computeMatchScore(course, institutionName, institutionLocation, q, level, location)
        );
    }

    private String formatDuration(Integer durationMonths) {
        if (durationMonths == null || durationMonths <= 0) {
            return "Duration not specified";
        }
        if (durationMonths % 12 == 0) {
            int years = durationMonths / 12;
            return years + (years == 1 ? " year" : " years");
        }
        return durationMonths + (durationMonths == 1 ? " month" : " months");
    }

    private int computeMatchScore(
            Course course,
            String institutionName,
            String institutionLocation,
            String q,
            String level,
            String location
    ) {
        int score = 35;
        score += scoreForQuery(q, course.getName(), course.getLevel(), institutionName, institutionLocation);
        score += scoreForFilter(level, course.getLevel(), 25);
        score += scoreForFilter(location, institutionLocation, 20);
        if (!normalize(location).isBlank() && scoreForFilter(location, institutionName, 10) > 0) {
            score += 10;
        }
        return Math.clamp(score, 0, 100);
    }

    private int scoreForQuery(String query, String... values) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return 0;
        }
        for (String value : values) {
            if (containsNormalized(value, normalizedQuery)) {
                return 20;
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

    private boolean containsNormalized(String value, String needle) {
        String normalizedValue = normalize(value);
        if (needle.isBlank() || normalizedValue.isBlank()) {
            return false;
        }
        String compactNeedle = needle.replace(" ", "");
        return normalizedValue.contains(needle) || normalizedValue.replace(" ", "").contains(compactNeedle);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

