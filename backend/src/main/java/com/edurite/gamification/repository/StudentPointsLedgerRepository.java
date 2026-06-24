package com.edurite.gamification.repository;

import com.edurite.gamification.entity.StudentPointsLedger;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentPointsLedgerRepository extends JpaRepository<StudentPointsLedger, UUID> {
    List<StudentPointsLedger> findTop20ByStudentIdOrderByAwardedAtDesc(UUID studentId);

    Optional<StudentPointsLedger> findFirstByStudentIdAndEventTypeAndReferenceId(UUID studentId, String eventType, String referenceId);

    long countByStudentIdAndEventTypeAndTermCode(UUID studentId, String eventType, String termCode);

    @Query("select coalesce(sum(l.points), 0) from StudentPointsLedger l where l.studentId = :studentId")
    long sumPointsByStudentId(@Param("studentId") UUID studentId);

    @Query("""
            select count(l) from StudentPointsLedger l
            where l.studentId = :studentId
              and l.eventType = :eventType
              and l.awardedAt >= :startInclusive
              and l.awardedAt < :endExclusive
            """)
    long countByStudentAndEventBetween(
            @Param("studentId") UUID studentId,
            @Param("eventType") String eventType,
            @Param("startInclusive") OffsetDateTime startInclusive,
            @Param("endExclusive") OffsetDateTime endExclusive
    );
}

