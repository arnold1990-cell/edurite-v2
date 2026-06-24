package com.edurite.school.service;

import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SchoolAccessService {

    public static final String ROLE_SCHOOL_ADMIN = "ROLE_SCHOOL_ADMIN";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_SCHOOL_STUDENT = "ROLE_SCHOOL_STUDENT";

    private final CurrentUserService currentUserService;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final MySchoolService mySchoolService;

    public SchoolAccessService(CurrentUserService currentUserService, SchoolUserProfileRepository schoolUserProfileRepository, MySchoolService mySchoolService) {
        this.currentUserService = currentUserService;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.mySchoolService = mySchoolService;
    }

    public AccessContext requireSchoolContext(Principal principal, Set<String> acceptedRoles) {
        User user = currentUserService.requireUser(principal);
        SchoolUserProfile profile = schoolUserProfileRepository.findByUserIdAndDeletedFalse(user.getId()).orElse(null);
        if (profile != null) {
            if (!profile.isActive()) {
                throw new InvalidCredentialsException("School access is inactive.");
            }
            if (acceptedRoles.contains(profile.getRoleName())) {
                return new AccessContext(user.getId(), profile.getSchoolId(), profile.getRoleName());
            }
        }
        if (acceptedRoles.contains(ROLE_SCHOOL_STUDENT)) {
            return mySchoolService.resolveApprovedStudentSchoolContext(user.getId());
        }
        throw new ResourceConflictException("Access denied for this school resource.");
    }

    public record AccessContext(UUID userId, UUID schoolId, String roleName) {}
}


