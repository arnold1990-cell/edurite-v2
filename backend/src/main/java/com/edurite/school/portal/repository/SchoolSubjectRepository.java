package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolSubject;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolSubjectRepository extends JpaRepository<SchoolSubject, UUID> {
    List<SchoolSubject> findBySchoolIdAndActiveTrue(UUID schoolId);
    List<SchoolSubject> findBySchoolIdOrderByPhaseAscSubjectNameAsc(UUID schoolId);
}



