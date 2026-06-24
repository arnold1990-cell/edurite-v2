package com.edurite.institution.controller;

import com.edurite.institution.dto.InstitutionDto;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/institutions", "/api/institutions"})
public class InstitutionController {

    private final InstitutionRepository institutionRepository;

    public InstitutionController(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @GetMapping
    public List<InstitutionDto> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String province,
            @RequestParam(defaultValue = "") String type
    ) {
        String search = q.trim().toLowerCase();
        String provinceFilter = province.trim().toLowerCase();
        String typeFilter = type.trim().toLowerCase();
        return institutionRepository.findByActiveTrueOrderByFeaturedDescNameAsc().stream()
                .filter(institution -> search.isEmpty()
                        || institution.getName().toLowerCase().contains(search)
                        || (institution.getLocation() != null && institution.getLocation().toLowerCase().contains(search))
                        || (institution.getProvince() != null && institution.getProvince().toLowerCase().contains(search))
                        || (institution.getCategory() != null && institution.getCategory().toLowerCase().contains(search)))
                .filter(institution -> provinceFilter.isEmpty()
                        || (institution.getProvince() != null && institution.getProvince().toLowerCase().contains(provinceFilter)))
                .filter(institution -> {
                    if (typeFilter.isEmpty()) return true;
                    String normalizedType = "tvet".equals(typeFilter) ? "TVET" : "COLLEGE";
                    String institutionType = "TVET".equalsIgnoreCase(institution.getCategory()) ? "TVET" : "COLLEGE";
                    return institutionType.equals(normalizedType);
                })
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InstitutionDto details(@PathVariable UUID id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institution not found"));
        return toDto(institution);
    }

    private InstitutionDto toDto(Institution institution) {
        return new InstitutionDto(
                institution.getId(),
                institution.getName(),
                institution.getLocation(),
                institution.getCity(),
                institution.getProvince(),
                institution.getCountry(),
                institution.getWebsite(),
                institution.getLogoUrl(),
                institution.getCategory(),
                Boolean.TRUE.equals(institution.getFeatured()),
                !Boolean.FALSE.equals(institution.getActive()));
    }
}

