package com.edurite.subscription.service;

import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SubscriptionAccessInterceptor implements HandlerInterceptor {
    private final CurrentUserService users;
    private final EntitlementService entitlements;
    private final AiUsageService usage;
    public SubscriptionAccessInterceptor(CurrentUserService users, EntitlementService entitlements, AiUsageService usage) {
        this.users = users; this.entitlements = entitlements; this.usage = usage;
    }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var rule = StudentAccessPolicy.rule(request.getMethod(), request.getRequestURI());
        if (StudentAccessPolicy.trialProtected(request.getRequestURI()) && request.getUserPrincipal() != null) {
            entitlements.requireFeature(users.requireUser(request.getUserPrincipal()), Feature.CAREER_BASIC);
        }
        if (rule == null) return true;
        // Full assessments cannot be bypassed through the legacy anonymous public alias.
        User user = users.requireUser(request.getUserPrincipal());
        entitlements.requireFeature(user, rule.feature());
        if (entitlements.isStudent(user) && rule.ai()) {
            request.setAttribute("aiReservation", usage.reserve(user.getId()));
            request.setAttribute("aiUser", user.getId());
        }
        return true;
    }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getAttribute("aiReservation") instanceof UUID reservation) {
            usage.finish(reservation, ex == null && response.getStatus() >= 200 && response.getStatus() < 300);
        }
    }
}
