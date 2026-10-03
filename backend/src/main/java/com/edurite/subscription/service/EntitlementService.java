package com.edurite.subscription.service;

import com.edurite.subscription.entity.PlanType;
import com.edurite.user.entity.User;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EntitlementService {
    private final StudentPlanAccessService access;
    public EntitlementService(StudentPlanAccessService access) { this.access = access; }
    public boolean isStudent(User user) {
        var roles = user.getRoles().stream().map(role -> role.getName().replaceFirst("^ROLE_", "")).toList();
        return roles.contains("STUDENT") && Collections.disjoint(roles,
                Set.of("ADMIN", "SCHOOL_ADMIN", "TEACHER", "DISTRICT", "DISTRICT_ADMIN", "DISTRICT_DIRECTOR", "CIRCUIT_MANAGER", "SUBJECT_ADVISOR"));
    }
    public PlanType plan(UUID userId) { return access.getCurrentPlan(userId); }
    public boolean hasFeature(UUID userId, Feature feature) { return feature == Feature.PROFILE || (access.hasSubscriptionAccess(userId) && feature.allowed(plan(userId))); }
    public void requireFeature(User user, Feature feature) {
        if (feature != Feature.PROFILE && isStudent(user) && !access.hasSubscriptionAccess(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your 14-day free trial has ended. Choose a plan to continue using EduRite.");
        }
        if (isStudent(user) && !hasFeature(user.getId(), feature)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Upgrade to " + feature.minimumPlan + " to unlock " + feature.label.toLowerCase() + ".");
        }
    }
    public List<String> entitlements(UUID userId) {
        var resolved = access.resolveByUserId(userId);
        boolean allowed = resolved.premium() || "TRIAL_ACTIVE".equals(resolved.status());
        PlanType plan = PlanType.fromPlanCode(resolved.planCode());
        return Arrays.stream(Feature.values()).filter(f -> f == Feature.PROFILE || (allowed && f.allowed(plan))).map(Enum::name).toList();
    }
    public static int allowance(PlanType plan) {
        return switch (plan) { case BASIC -> 5; case PREMIUM -> 30; case PRO -> 100; };
    }
}
