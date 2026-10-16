package com.edurite.subscription.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "admin_subscription_changes")
@Immutable
@Getter
@Setter
public class AdminSubscriptionChange extends BaseEntity {
    private UUID userId;
    private UUID administratorId;
    private String administratorEmail;
    private UUID subscriptionId;
    private String previousPlan;
    private String newPlan;
    @jakarta.persistence.Column(length = 1000)
    private String reason;
}
