package com.edurite.school.repository;

import com.edurite.school.entity.RegisteredSchool;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegisteredSchoolRepository extends JpaRepository<RegisteredSchool, UUID> {

    Optional<RegisteredSchool> findByIdAndActiveTrue(UUID id);

    @Query("""
            SELECT school
            FROM RegisteredSchool school
            WHERE school.active = true
              AND school.provinceId = :provinceId
              AND school.districtId = :districtId
              AND (
                    :searchText IS NULL
                    OR LOWER(school.schoolName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(school.emisNumber) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(COALESCE(school.schoolCode, '')) LIKE LOWER(CONCAT('%', :searchText, '%'))
              )
            ORDER BY school.schoolName ASC
            """)
    List<RegisteredSchool> searchActive(
            @Param("provinceId") UUID provinceId,
            @Param("districtId") UUID districtId,
            @Param("searchText") String searchText,
            Pageable pageable
    );
}
