package com.edurite.district.service;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.district.entity.DistrictAdminProfile;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DistrictAccessService {

    public static final String ROLE_DISTRICT_ADMIN = "ROLE_DISTRICT_ADMIN";
    public static final String ROLE_DISTRICT_DIRECTOR = "ROLE_DISTRICT_DIRECTOR";
    public static final String ROLE_CIRCUIT_MANAGER = "ROLE_CIRCUIT_MANAGER";
    public static final String ROLE_SUBJECT_ADVISOR = "ROLE_SUBJECT_ADVISOR";

    private final CurrentUserService currentUserService;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;

    public DistrictAccessService(
            CurrentUserService currentUserService,
            DistrictAdminProfileRepository districtAdminProfileRepository
    ) {
        this.currentUserService = currentUserService;
        this.districtAdminProfileRepository = districtAdminProfileRepository;
    }

    public AccessContext requireDistrictContext(Principal principal, Set<String> acceptedRoles) {
        User user = currentUserService.requireUser(principal);
        DistrictAdminProfile profile = districtAdminProfileRepository.findByUserIdAndDeletedFalse(user.getId())
                .orElseThrow(() -> new InvalidCredentialsException("District access is not configured for this account."));
        if (!profile.isActive()) {
            throw new InvalidCredentialsException("District access is inactive.");
        }
        Set<String> actualRoles = user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase())
                .collect(java.util.stream.Collectors.toSet());
        String grantedRole = acceptedRoles.stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase())
                .filter(actualRoles::contains)
                .findFirst()
                .orElse(null);
        if (grantedRole == null) {
            throw new ResourceConflictException("Access denied for this district resource.");
        }
        return new AccessContext(user.getId(), profile.getDistrictId(), grantedRole);
    }

    public record AccessContext(UUID userId, UUID districtId, String roleName) {}
}
