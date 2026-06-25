package com.edurite.subscription.service;

import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.security.Principal;
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
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public StudentPlanAccessService(
            SubscriptionRepository subscriptionRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    public StudentPlanAccess resolve(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return resolveByUserId(user.getId());
    }

    public StudentPlanAccess resolveByUserId(UUID userId) {
        if (isPermanentPremiumOverride(userId)) {
            return new StudentPlanAccess(
                    PLAN_PREMIUM,
                    STATUS_ACTIVE,
                    true,
                    null,
                    null
            );
        }
        SubscriptionRecord subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        String planCode = normalizePlanCode(subscription == null ? null : subscription.getPlanCode());
        String status = normalizeStatus(subscription == null ? null : subscription.getStatus());
        boolean paidPremium = PLAN_PREMIUM.equals(planCode) && STATUS_ACTIVE.equals(status);
        boolean trialActive = isTrialActive(subscription);
        boolean premium = paidPremium || trialActive;
        String effectivePlanCode = paidPremium ? PLAN_PREMIUM : (trialActive ? "PLAN_TRIAL" : PLAN_BASIC);
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
        return OffsetDateTime.now().isBefore(subscription.getTrialEndDate());
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

