package com.edurite.gamification.repository;

import com.edurite.gamification.entity.RewardClaim;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, UUID> {
    List<RewardClaim> findTop20ByStudentIdOrderByClaimedAtDesc(UUID studentId);

    @Query("""
            select (count(c) > 0) from RewardClaim c
            where c.studentId = :studentId
              and c.termCode = :termCode
              and lower(c.rewardName) = lower(:rewardName)
              and c.status in ('PENDING', 'APPROVED', 'FULFILLED')
            """)
    boolean existsActiveClaimForReward(
            @Param("studentId") UUID studentId,
            @Param("termCode") String termCode,
            @Param("rewardName") String rewardName
    );

    @Query("""
            select coalesce(sum(c.claimedPoints), 0) from RewardClaim c
            where c.studentId = :studentId and c.status in ('PENDING', 'APPROVED', 'FULFILLED')
            """)
    long sumReservedPointsByStudentId(@Param("studentId") UUID studentId);
}

