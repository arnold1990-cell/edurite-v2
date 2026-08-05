package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolSubject;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolSubjectRepository extends JpaRepository<SchoolSubject, UUID> {
    List<SchoolSubject> findBySchoolIdAndActiveTrue(UUID schoolId);

    List<SchoolSubject> findByIdIn(Collection<UUID> ids);

    @Query("""
            select subject
            from SchoolSubject subject
            left join fetch subject.subjectCatalogue
            where subject.schoolId = :schoolId
            order by subject.phase asc, subject.subjectName asc
            """)
    List<SchoolSubject> findBySchoolIdOrderByPhaseAscSubjectNameAsc(@Param("schoolId") UUID schoolId);

    @Query("""
            select subject
            from SchoolSubject subject
            left join fetch subject.subjectCatalogue
            where subject.id = :id
            """)
    java.util.Optional<SchoolSubject> findByIdWithSubjectCatalogue(@Param("id") UUID id);
}



