package com.edurite.subscription.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "payments")
@Getter
@Setter
/**
 * This class named PaymentRecord is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class PaymentRecord extends BaseEntity {
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID subscriptionId;
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private BigDecimal amount;
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String currency;
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private String status;

    private String provider;
    private String reference;
    private String providerOrderId;
    private String providerSessionId;
    private String providerPaymentId;
    private String providerSubscriptionId;

    @Column(columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private OffsetDateTime paidAt;
    private OffsetDateTime confirmedAt;

    private OffsetDateTime callbackReceivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";
}

