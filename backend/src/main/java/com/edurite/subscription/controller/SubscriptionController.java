package com.edurite.subscription.controller;

import com.edurite.subscription.dto.PricingPlanDto;
import com.edurite.subscription.dto.SubscriptionCheckoutRequest;
import com.edurite.subscription.dto.SubscriptionCheckoutResponse;
import com.edurite.subscription.dto.SubscriptionPaymentCancelRequest;
import com.edurite.subscription.dto.SubscriptionPaymentConfirmRequest;
import com.edurite.subscription.dto.SubscriptionPaymentStatusResponse;
import com.edurite.subscription.dto.SubscriptionPaymentVerifyRequest;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/subscriptions", "/api/subscriptions"})
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public SubscriptionRecord current(Principal principal) {
        return subscriptionService.current(principal);
    }

    @GetMapping("/plans")
    public java.util.List<PricingPlanDto> plans() {
        return subscriptionService.plans();
    }

    @PostMapping("/checkout")
    public SubscriptionCheckoutResponse checkout(
            Principal principal,
            @Valid @RequestBody SubscriptionCheckoutRequest request
    ) {
        return subscriptionService.checkout(principal, request);
    }

    @PostMapping("/confirm")
    public SubscriptionPaymentStatusResponse confirm(
            Principal principal,
            @Valid @RequestBody SubscriptionPaymentConfirmRequest request
    ) {
        return subscriptionService.confirmCheckout(principal, request);
    }

    @PostMapping("/cancel")
    public SubscriptionPaymentStatusResponse cancel(
            Principal principal,
            @Valid @RequestBody SubscriptionPaymentCancelRequest request
    ) {
        return subscriptionService.cancelCheckout(principal, request);
    }

    @PostMapping("/verify")
    public SubscriptionPaymentStatusResponse verify(
            Principal principal,
            @Valid @RequestBody SubscriptionPaymentVerifyRequest request
    ) {
        return subscriptionService.verifyPayment(principal, request.paymentReference());
    }

    @PostMapping("/purchase")
    public Map<String, Object> purchase(Principal principal, @RequestBody Map<String, String> payload) {
        return subscriptionService.purchase(principal, payload.getOrDefault("plan", planFallback(payload)));
    }

    private String planFallback(Map<String, String> payload) {
        String planCode = payload.get("planCode");
        if (planCode != null && !planCode.isBlank()) {
            return planCode;
        }
        return "BASIC";
    }
}

