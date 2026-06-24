package com.edurite.subscription.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pricing_plans")
@Getter
@Setter
public class PricingPlan extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private java.math.BigDecimal amount;

    @Column(nullable = false)
    private String billingInterval;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String features = "[]";

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean premium = false;

    @Column(nullable = false)
    private int displayOrder = 0;
}

