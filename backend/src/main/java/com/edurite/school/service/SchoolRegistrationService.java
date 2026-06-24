package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.dto.SchoolRegistrationDtos;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import java.security.Principal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolRegistrationService {

    private final CurrentUserService currentUserService;
    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;

    public SchoolRegistrationService(
            CurrentUserService currentUserService,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository
    ) {
        this.currentUserService = currentUserService;
        this.schoolRegistrationRequestRepository = schoolRegistrationRequestRepository;
    }

    @Transactional(readOnly = true)
    public SchoolRegistrationDtos.SchoolRegistrationStatusResponse myStatus(Principal principal) {
        User user = currentUserService.requireUser(principal);
        SchoolRegistrationRequest request = schoolRegistrationRequestRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("School registration request not found."));
        return toDto(request);
    }

    public SchoolRegistrationDtos.SchoolRegistrationStatusResponse toDto(SchoolRegistrationRequest request) {
        return new SchoolRegistrationDtos.SchoolRegistrationStatusResponse(
                request.getId(),
                request.getSchoolName(),
                request.getEmisNumber(),
                request.getProvince(),
                request.getDistrictName(),
                request.getCircuit(),
                request.getSchoolType(),
                request.getPrincipalName(),
                request.getPrincipalEmail(),
                request.getSchoolEmail(),
                request.getPhoneNumber(),
                request.getPhysicalAddress(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getSubmittedAt(),
                request.getApprovedAt(),
                request.getRejectedAt()
        );
    }
}
