package com.edurite.student.service;

import com.edurite.school.portal.entity.SubjectCatalogue;
import com.edurite.school.portal.repository.SubjectCatalogueRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class StudentSubjectCatalogue {
    private final SubjectCatalogueRepository repository;
    public StudentSubjectCatalogue(SubjectCatalogueRepository repository) { this.repository = repository; }
    public record Subject(String name, String phase, boolean language, String languageLevel) {}
    public List<Subject> subjects() {
        return repository.findByActiveTrueOrderByPhaseAscNameAsc().stream()
            .filter(s -> "FET".equalsIgnoreCase(s.getPhase()) || "Senior".equalsIgnoreCase(s.getPhase()))
            .map(s -> new Subject(display(s), s.getPhase(), s.isLanguage(), s.getLanguageLevel())).distinct().toList();
    }
    private String display(SubjectCatalogue s) {
        return s.isLanguage() && s.getLanguageLevel() != null && !s.getName().endsWith(s.getLanguageLevel())
                ? s.getName() + " " + s.getLanguageLevel() : s.getName();
    }
    public String canonicalize(String value) {
        String normalized = value.trim().replaceAll("(?i)\\bHL$", "Home Language")
                .replaceAll("(?i)\\bFAL$", "First Additional Language").replaceAll("(?i)\\bSAL$", "Second Additional Language");
        normalized = switch (normalized.toLowerCase(Locale.ROOT)) {
            case "maths" -> "Mathematics";
            case "math lit" -> "Mathematical Literacy";
            case "physical science" -> "Physical Sciences";
            case "life science" -> "Life Sciences";
            case "cat" -> "Computer Applications Technology";
            default -> normalized;
        };
        String name = normalized;
        return subjects().stream().map(Subject::name).filter(s -> s.equalsIgnoreCase(name)).findFirst().orElse(name);
    }
}
