package com.edurite.gamification.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reward_rules")
@Getter
@Setter
public class RewardRule extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private int pointsPerEvent;

    private Integer maxPerTerm;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDate startDate;
    private LocalDate endDate;
}

