package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.School;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    java.util.List<School> findByDistrictIdOrderBySchoolNameAsc(UUID districtId);
    java.util.List<School> findByStatusIgnoreCaseOrderBySchoolNameAsc(String status);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    java.util.Optional<School> findByRegistrationNumberIgnoreCase(String registrationNumber);
}



