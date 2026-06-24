package com.edurite.company.mapper;

import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.entity.CompanyProfile;
import org.springframework.stereotype.Component;

@Component
/**
 * This class named CompanyProfileMapper is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyProfileMapper {

    /**
     * this method handles the "toDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto toDto(CompanyProfile entity) {
        return new CompanyProfileDto(
                entity.getId(),
                entity.getCompanyName(),
                entity.getRegistrationNumber(),
                entity.getIndustry(),
                entity.getOfficialEmail(),
                entity.getMobileNumber(),
                entity.getContactPersonName(),
                entity.getAddress(),
                entity.getWebsite(),
                entity.getDescription(),
                entity.getStatus(),
                entity.isEmailVerified(),
                entity.isMobileVerified(),
                entity.getReviewedAt(),
                entity.getReviewedBy(),
                entity.getReviewNotes()
        );
    }
}

