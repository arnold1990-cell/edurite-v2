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
        return user.getRoles().stream().anyMatch(role -> "STUDENT".equals(role.getName()));
    }
    public PlanType plan(UUID userId) { return access.getCurrentPlan(userId); }
    public boolean hasFeature(UUID userId, Feature feature) { return feature.allowed(plan(userId)); }
    public void requireFeature(User user, Feature feature) {
        if (isStudent(user) && !hasFeature(user.getId(), feature)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Upgrade to " + feature.minimumPlan + " to unlock " + feature.label.toLowerCase() + ".");
        }
    }
    public List<String> entitlements(UUID userId) {
        PlanType plan = plan(userId);
        return Arrays.stream(Feature.values()).filter(f -> f.allowed(plan)).map(Enum::name).toList();
    }
    public static int allowance(PlanType plan) {
        return switch (plan) { case BASIC -> 5; case PREMIUM -> 30; case PRO -> 100; };
    }
}
