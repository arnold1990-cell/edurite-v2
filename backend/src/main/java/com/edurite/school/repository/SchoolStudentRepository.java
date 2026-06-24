package com.edurite.school.repository;

import com.edurite.school.entity.SchoolStudent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolStudentRepository extends JpaRepository<SchoolStudent, UUID> {
    List<SchoolStudent> findBySchoolId(UUID schoolId);
    Optional<SchoolStudent> findBySchoolIdAndStudentId(UUID schoolId, UUID studentId);
}


