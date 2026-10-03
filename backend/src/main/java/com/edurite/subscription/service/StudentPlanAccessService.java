package com.edurite.subscription.service;

import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudentPlanAccessService {

    public static final String PLAN_BASIC = "PLAN_BASIC";
    public static final String PLAN_PREMIUM = "PLAN_PREMIUM";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final int BASIC_CAREER_GUIDANCE_LIMIT = 3;
    public static final String BASIC_UPGRADE_MESSAGE =
            "You are on a Free Trial with Basic features. Upgrade to Premium to unlock deeper analysis and more recommendations.";
    private final SubscriptionRepository subscriptionRepository;

    public StudentPlanAccessService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
    }

    private final java.time.Clock clock = java.time.Clock.systemUTC();

    public StudentPlanAccess resolveByUserId(UUID userId) {
        return resolveAt(userId, OffsetDateTime.now(clock));
    }

    // Explicit server time also makes the exclusive expiry boundary testable.
    public StudentPlanAccess resolveAt(UUID userId, OffsetDateTime now) {
        SubscriptionRecord subscription = currentRecord(userId);
        String code = normalizePlanCode(subscription == null ? null : subscription.getPlanCode());
        String status = normalizeStatus(subscription == null ? null : subscription.getStatus());
        if (isActivePaid(subscription, now.toLocalDate())) {
            return new StudentPlanAccess(code, STATUS_ACTIVE, true, null, null);
        }
        var trial = trialRecord(userId);
        boolean active = trial != null && trial.getTrialStartDate() != null
                && !now.isBefore(trial.getTrialStartDate()) && now.isBefore(trialExpiry(trial));
        String effectiveStatus = active ? "TRIAL_ACTIVE" : "TRIAL_EXPIRED";
        if (!active && subscription != null && !PLAN_BASIC.equals(code) && !"PLAN_TRIAL".equals(code)) {
            effectiveStatus = STATUS_ACTIVE.equals(status) ? "EXPIRED" : status;
        }
        return new StudentPlanAccess(PLAN_BASIC, effectiveStatus, false, BASIC_CAREER_GUIDANCE_LIMIT,
                active ? BASIC_UPGRADE_MESSAGE : "Your 14-day free trial has ended. Choose a plan to continue using EduRite.");
    }

    private boolean isActivePaid(SubscriptionRecord record, java.time.LocalDate today) {
        if (record == null || "PLAN_TRIAL".equals(record.getPlanCode())) return false;
        var plan = com.edurite.subscription.entity.PlanType.fromPlanCode(record.getPlanCode());
        return plan != com.edurite.subscription.entity.PlanType.BASIC
                && STATUS_ACTIVE.equals(normalizeStatus(record.getStatus()))
                && record.getEndDate() != null && today.isBefore(record.getEndDate())
                && (record.getStartDate() == null || !today.isBefore(record.getStartDate()));
    }

    public SubscriptionRecord currentRecord(UUID userId) {
        // A later pending purchase or stale ACTIVE record must not hide valid paid access.
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> isActivePaid(s, java.time.LocalDate.now(clock)))
                .max(java.util.Comparator.comparingInt(s -> com.edurite.subscription.entity.PlanType.fromPlanCode(s.getPlanCode()).ordinal()))
                .orElseGet(() -> subscriptionRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_ACTIVE)
                        .orElseGet(() -> subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null)));
    }

    public SubscriptionRecord trialRecord(UUID userId) {
        var records = new java.util.ArrayList<>(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId));
        var current = currentRecord(userId);
        if (current != null) records.add(current);
        return records.stream().filter(s -> PLAN_BASIC.equals(normalizePlanCode(s.getPlanCode())) || "PLAN_TRIAL".equals(s.getPlanCode()))
                .filter(s -> s.getTrialStartDate() != null && s.getTrialEndDate() != null)
                .min(java.util.Comparator.comparing(SubscriptionRecord::getTrialStartDate)).orElse(null);
    }

    public OffsetDateTime trialExpiry(SubscriptionRecord trial) {
        var maximum = trial.getTrialStartDate().plusDays(14);
        return trial.getTrialEndDate().isBefore(maximum) ? trial.getTrialEndDate() : maximum;
    }

    public boolean hasSubscriptionAccess(UUID userId) {
        var access = resolveByUserId(userId);
        return access.premium() || "TRIAL_ACTIVE".equals(access.status());
    }

    public com.edurite.subscription.entity.PlanType getCurrentPlan(UUID userId) {
        return com.edurite.subscription.entity.PlanType.fromPlanCode(resolveByUserId(userId).planCode());
    }

    public boolean hasPremiumAccess(UUID userId) {
        return resolveByUserId(userId).premium();
    }

    private String normalizePlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return PLAN_BASIC;
        }
        String normalized = planCode.trim().toUpperCase(Locale.ROOT);
        if ("BASIC".equals(normalized)) {
            return PLAN_BASIC;
        }
        if ("PREMIUM".equals(normalized)) {
            return PLAN_PREMIUM;
        }
        if ("PRO".equals(normalized)) return "PLAN_PRO";
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    public record StudentPlanAccess(
            String planCode,
            String status,
            boolean premium,
            Integer careerSuggestionLimit,
            String upgradeMessage
    ) {
    }
}

