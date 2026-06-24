package com.edurite.bursary.source;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ProviderBursarySource implements BursarySource {

    private final BursaryRepository bursaryRepository;

    public ProviderBursarySource(BursaryRepository bursaryRepository) {
        this.bursaryRepository = bursaryRepository;
    }

    @Override
    public List<BursaryResultDto> fetch(BursarySearchRequest request) {
        return bursaryRepository.searchPublic(
                safe(request.query()),
                safe(request.qualificationLevel()),
                safe(request.region()),
                safe(request.eligibility()),
                PageRequest.of(Math.max(0, request.page()), Math.max(1, request.size()))
        ).getContent().stream().map(this::toDto).toList();
    }

    @Override
    public String sourceType() {
        return "OFFICIAL_PROVIDER";
    }

    private BursaryResultDto toDto(Bursary bursary) {
        String source = bursary.getCompanyId() == null ? sourceType() : "COMPANY_PROVIDER";
        return new BursaryResultDto(
                bursary.getId().toString(),
                bursary.getTitle(),
                bursary.getCompanyId() == null ? "EduRite Provider" : bursary.getCompanyId().toString(),
                bursary.getDescription(),
                bursary.getQualificationLevel(),
                bursary.getLocation(),
                bursary.getEligibility(),
                bursary.getApplicationEndDate(),
                "",
                source,
                relevanceScore(bursary),
                List.of(),
                true,
                false,
                bursary.getApplicationEndDate() == null ? "Deadline not stored in EduRite provider data." : null
        );
    }

    private int relevanceScore(Bursary bursary) {
        int score = 50;
        if (containsStem(bursary.getTitle()) || containsStem(bursary.getFieldOfStudy())) {
            score += 20;
        }
        if (bursary.getApplicationEndDate() != null) {
            score += 10;
        }
        return Math.min(100, score);
    }

    private boolean containsStem(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains("engineering") || lower.contains("science") || lower.contains("technology");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

