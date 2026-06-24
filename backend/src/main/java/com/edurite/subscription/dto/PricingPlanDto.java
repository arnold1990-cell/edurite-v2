package com.edurite.subscription.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PricingPlanDto(
        UUID id,
        String planId,
        String code,
        String name,
        String description,
        String currency,
        BigDecimal price,
        BigDecimal amount,
        String billingPeriod,
        String billingInterval,
        boolean premium,
        boolean recommended,
        List<String> features
) {
}

