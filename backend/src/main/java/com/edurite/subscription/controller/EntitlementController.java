package com.edurite.subscription.controller;

import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.service.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/subscriptions", "/api/v1/subscriptions"})
public class EntitlementController {
    private final CurrentUserService users;
    private final StudentPlanAccessService access;
    private final EntitlementService entitlements;
    private final AiUsageService usage;
    public EntitlementController(CurrentUserService users, StudentPlanAccessService access, EntitlementService entitlements, AiUsageService usage) {
        this.users=users; this.access=access; this.entitlements=entitlements; this.usage=usage;
    }
    @GetMapping("/entitlements")
    public Map<String,Object> current(Principal principal) {
        UUID id=users.requireUser(principal).getId();
        var plan=access.resolveByUserId(id);
        var subscription=access.currentRecord(id);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("plan", entitlements.plan(id));
        result.put("status", plan.status());
        result.put("trialActive", "PLAN_TRIAL".equals(plan.planCode()));
        result.put("entitlements", entitlements.entitlements(id));
        result.put("features", Arrays.stream(Feature.values()).map(f -> Map.of("id",f.name(),"label",f.label,"minimumPlan",f.minimumPlan)).toList());
        result.put("routeRules", StudentAccessPolicy.ROUTES);
        result.put("aiUsage", usage.current(id));
        result.put("billingInterval", subscription == null ? null : subscription.getPlanCode() != null && subscription.getPlanCode().endsWith("_YEARLY") ? "YEARLY" : "MONTHLY");
        result.put("expiresAt", subscription == null ? null : subscription.getEndDate());
        result.put("renewalDate", subscription == null ? null : subscription.getRenewalDate());
        result.put("cancelAtPeriodEnd", subscription != null && subscription.isCancelAtPeriodEnd());
        result.put("trialEndDate", subscription == null ? null : subscription.getTrialEndDate());
        return result;
    }
}
