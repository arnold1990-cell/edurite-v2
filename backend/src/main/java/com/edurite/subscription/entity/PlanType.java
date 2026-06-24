package com.edurite.subscription.entity;

import java.util.Locale;

public enum PlanType {
    BASIC,
    PREMIUM;

    public static PlanType fromPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return BASIC;
        }

        String normalized = planCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("PLAN_")) {
            normalized = normalized.substring("PLAN_".length());
        }

        if ("PREMIUM".equals(normalized)) {
            return PREMIUM;
        }
        return BASIC;
    }
}

