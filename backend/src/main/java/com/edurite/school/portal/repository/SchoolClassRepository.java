package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findBySchoolIdAndActiveTrue(UUID schoolId);
}



