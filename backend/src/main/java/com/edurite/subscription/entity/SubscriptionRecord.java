package com.edurite.subscription.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "subscriptions")
@Getter
@Setter
/**
 * This class named SubscriptionRecord is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SubscriptionRecord extends BaseEntity {
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID userId;
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String planCode;
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String status;
    private LocalDate renewalDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentReference;
    private String provider;
    private String providerSubscriptionId;
    private OffsetDateTime lastPaymentAt;
    private boolean cancelAtPeriodEnd;

    @Column(name = "trial_start_date")
    private OffsetDateTime trialStartDate;

    @Column(name = "trial_end_date")
    private OffsetDateTime trialEndDate;

    @Column(name = "premium_until")
    private OffsetDateTime premiumUntil;

    @Column(name = "trial_used", nullable = false)
    private boolean trialUsed;

    @Transient
    private Boolean premiumAccess;

    @Transient
    private Boolean trialActive;

    @Transient
    private String accessMessage;
}

