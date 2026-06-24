package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolUserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolUserProfileRepository extends JpaRepository<SchoolUserProfile, UUID> {
    Optional<SchoolUserProfile> findByUserIdAndDeletedFalse(UUID userId);
    List<SchoolUserProfile> findBySchoolIdAndRoleNameAndDeletedFalse(UUID schoolId, String roleName);
    List<SchoolUserProfile> findBySchoolIdInAndRoleNameAndDeletedFalse(List<UUID> schoolIds, String roleName);
    List<SchoolUserProfile> findBySchoolIdAndDeletedFalse(UUID schoolId);
    List<SchoolUserProfile> findBySchoolIdInAndDeletedFalse(List<UUID> schoolIds);
    Optional<SchoolUserProfile> findBySchoolIdAndUserIdAndDeletedFalse(UUID schoolId, UUID userId);
    boolean existsByPortalUsernameIgnoreCaseAndDeletedFalse(String portalUsername);
}



