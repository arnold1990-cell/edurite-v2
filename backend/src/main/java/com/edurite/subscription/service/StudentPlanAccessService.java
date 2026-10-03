package com.edurite.subscription.service;

import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.User;
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
            "You are on the Basic plan. Upgrade to Premium to unlock deeper analysis and more recommendations.";
    /**
     * Development / owner override.
     * This specific account must always resolve to Premium regardless of subscription state.
     */
    static final String OWNER_PREMIUM_OVERRIDE_EMAIL = "arnoldmadaz@gmail.com";

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public StudentPlanAccessService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    public StudentPlanAccess resolveByUserId(UUID userId) {
        SubscriptionRecord subscription = currentRecord(userId);
        boolean activePro = subscription != null
                && com.edurite.subscription.entity.PlanType.fromPlanCode(subscription.getPlanCode()) == com.edurite.subscription.entity.PlanType.PRO
                && STATUS_ACTIVE.equals(normalizeStatus(subscription.getStatus()))
                && subscription.getEndDate() != null
                && java.time.LocalDate.now(java.time.ZoneOffset.UTC).isBefore(subscription.getEndDate())
                && (subscription.getStartDate() == null || !java.time.LocalDate.now(java.time.ZoneOffset.UTC).isBefore(subscription.getStartDate()));
        if (!activePro && isPermanentPremiumOverride(userId)) {
            return new StudentPlanAccess(
                    PLAN_PREMIUM,
                    STATUS_ACTIVE,
                    true,
                    null,
                    null
            );
        }
        String planCode = normalizePlanCode(subscription == null ? null : subscription.getPlanCode());
        String status = normalizeStatus(subscription == null ? null : subscription.getStatus());
        boolean inPeriod = subscription != null && subscription.getEndDate() != null
                && (subscription.getStartDate() == null || !java.time.LocalDate.now(java.time.ZoneOffset.UTC).isBefore(subscription.getStartDate()))
                && java.time.LocalDate.now(java.time.ZoneOffset.UTC).isBefore(subscription.getEndDate());
        var storedPlan = com.edurite.subscription.entity.PlanType.fromPlanCode(planCode);
        boolean paidPremium = !"PLAN_TRIAL".equals(planCode)
                && storedPlan != com.edurite.subscription.entity.PlanType.BASIC
                && STATUS_ACTIVE.equals(status) && inPeriod;
        boolean trialActive = (PLAN_BASIC.equals(planCode) || "PLAN_TRIAL".equals(planCode))
                && STATUS_ACTIVE.equals(status) && isTrialActive(subscription);
        boolean premium = paidPremium || trialActive;
        String effectivePlanCode = paidPremium ? (storedPlan == com.edurite.subscription.entity.PlanType.PRO ? "PLAN_PRO" : PLAN_PREMIUM)
                : (trialActive ? "PLAN_TRIAL" : PLAN_BASIC);
        if (!premium && !PLAN_BASIC.equals(planCode) && STATUS_ACTIVE.equals(status)) status = "EXPIRED";
        String upgradeMessage = premium
                ? null
                : BASIC_UPGRADE_MESSAGE;

        return new StudentPlanAccess(
                effectivePlanCode,
                status,
                premium,
                premium ? null : BASIC_CAREER_GUIDANCE_LIMIT,
                upgradeMessage
        );
    }

    public SubscriptionRecord currentRecord(UUID userId) {
        return subscriptionRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_ACTIVE)
                .orElseGet(() -> subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null));
    }

    public com.edurite.subscription.entity.PlanType getCurrentPlan(UUID userId) {
        return com.edurite.subscription.entity.PlanType.fromPlanCode(resolveByUserId(userId).planCode());
    }

    public boolean hasPremiumAccess(UUID userId) {
        return resolveByUserId(userId).premium();
    }

    public boolean isPermanentPremiumOverride(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .map(this::isPermanentPremiumOverrideEmail)
                .orElse(false);
    }

    public boolean isPermanentPremiumOverrideEmail(String email) {
        return email != null && OWNER_PREMIUM_OVERRIDE_EMAIL.equalsIgnoreCase(email.trim());
    }

    private boolean isTrialActive(SubscriptionRecord subscription) {
        if (subscription == null || subscription.getTrialEndDate() == null) {
            return false;
        }
        return (subscription.getTrialStartDate() == null || !OffsetDateTime.now().isBefore(subscription.getTrialStartDate()))
                && OffsetDateTime.now().isBefore(subscription.getTrialEndDate());
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
            return STATUS_ACTIVE;
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

