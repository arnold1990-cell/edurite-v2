package com.edurite.institution.universityinfo.service;

import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UniversitySlugService {

    private final InstitutionRepository institutionRepository;

    public UniversitySlugService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public Institution requireBySlug(String slug) {
        return institutionRepository.findByActiveTrueOrderByFeaturedDescNameAsc().stream()
                .filter(institution -> slugify(institution.getName()).equals(slugify(slug)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found"));
    }

    public String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replace('&', '-');
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized;
    }
}
