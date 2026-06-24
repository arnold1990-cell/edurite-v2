package com.edurite.gamification.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reward_claims")
@Getter
@Setter
public class RewardClaim extends BaseEntity {

    @Column(nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private String termCode;

    @Column(nullable = false)
    private String rewardName;

    @Column(columnDefinition = "TEXT")
    private String rewardDescription;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private int claimedPoints;

    @Column(nullable = false)
    private OffsetDateTime claimedAt = OffsetDateTime.now();

    private OffsetDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

