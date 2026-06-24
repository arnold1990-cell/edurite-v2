package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolRegistrationRequestRepository extends JpaRepository<SchoolRegistrationRequest, UUID> {
    Optional<SchoolRegistrationRequest> findByUserId(UUID userId);

    Optional<SchoolRegistrationRequest> findByEmisNumberIgnoreCase(String emisNumber);

    boolean existsByEmisNumberIgnoreCase(String emisNumber);

    List<SchoolRegistrationRequest> findByDistrictIdOrderBySubmittedAtDesc(UUID districtId);

    List<SchoolRegistrationRequest> findByDistrictIdAndStatusOrderBySubmittedAtDesc(UUID districtId, SchoolStatus status);

    @Query("""
            SELECT request
            FROM SchoolRegistrationRequest request
            WHERE request.districtId = :districtId
              AND (
                    LOWER(request.schoolName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(request.emisNumber) LIKE LOWER(CONCAT('%', :searchText, '%'))
              )
            ORDER BY request.submittedAt DESC
            """)
    List<SchoolRegistrationRequest> searchByDistrict(
            @Param("districtId") UUID districtId,
            @Param("searchText") String searchText
    );

    @Query("""
            SELECT request
            FROM SchoolRegistrationRequest request
            WHERE request.districtId = :districtId
              AND request.status = :status
              AND (
                    LOWER(request.schoolName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(request.emisNumber) LIKE LOWER(CONCAT('%', :searchText, '%'))
              )
            ORDER BY request.submittedAt DESC
            """)
    List<SchoolRegistrationRequest> searchByDistrictAndStatus(
            @Param("districtId") UUID districtId,
            @Param("status") SchoolStatus status,
            @Param("searchText") String searchText
    );
}
