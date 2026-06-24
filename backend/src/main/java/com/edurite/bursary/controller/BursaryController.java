package com.edurite.bursary.controller;

import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchResponse;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.bursary.service.BursaryAggregationService;
import com.edurite.bursary.service.BursaryRecommendationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/bursaries", "/api/bursaries"})
public class BursaryController {

    private final BursaryRepository bursaryRepository;
    private final BursaryAggregationService bursaryAggregationService;
    private final BursaryRecommendationService bursaryRecommendationService;

    public BursaryController(
            BursaryRepository bursaryRepository,
            BursaryAggregationService bursaryAggregationService,
            BursaryRecommendationService bursaryRecommendationService
    ) {
        this.bursaryRepository = bursaryRepository;
        this.bursaryAggregationService = bursaryAggregationService;
        this.bursaryRecommendationService = bursaryRecommendationService;
    }

    @GetMapping
    public Page<Bursary> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String eligibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bursaryRepository.findByStatusIgnoreCaseAndDeletedAtIsNullAndTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
                "ACTIVE", q, qualificationLevel, location, eligibility, PageRequest.of(page, size));
    }

    @GetMapping("/search")
    public BursarySearchResponse search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String qualification,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String eligibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bursaryAggregationService.search(new BursarySearchRequest(q, qualification, region, eligibility, page, size));
    }

    @GetMapping("/recommendations/me")
    public List<BursaryResultDto> myRecommendations(Principal principal) {
        return bursaryRecommendationService.recommendForStudent(principal);
    }

    @GetMapping("/{id}")
    public Bursary get(@PathVariable UUID id) {
        Bursary bursary = bursaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bursary not found"));
        if (bursary.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(bursary.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bursary not found");
        }
        return bursary;
    }
}

