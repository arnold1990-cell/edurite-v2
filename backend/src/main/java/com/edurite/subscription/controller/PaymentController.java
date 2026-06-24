package com.edurite.subscription.controller;

import com.edurite.subscription.service.SubscriptionService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/payments", "/api/payments"})
public class PaymentController {

    private final SubscriptionService subscriptionService;

    public PaymentController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam Map<String, String> query
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (body != null) {
            payload.putAll(body);
        }
        if (query != null && !query.isEmpty()) {
            payload.putAll(query);
        }
        return ResponseEntity.ok(subscriptionService.handlePaymentCallback(payload));
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callbackGet(@RequestParam Map<String, String> query) {
        return ResponseEntity.ok(subscriptionService.handlePaymentCallback(query));
    }

    @PostMapping("/callbacks/{provider}")
    public ResponseEntity<Map<String, Object>> providerCallbackPost(
            @PathVariable String provider,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam Map<String, String> query
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (body != null) {
            payload.putAll(body);
        }
        if (query != null && !query.isEmpty()) {
            payload.putAll(query);
        }
        return ResponseEntity.ok(subscriptionService.handleProviderCallback(provider, payload));
    }

    @GetMapping("/callbacks/{provider}")
    public ResponseEntity<Map<String, Object>> providerCallbackGet(
            @PathVariable String provider,
            @RequestParam Map<String, String> query
    ) {
        return ResponseEntity.ok(subscriptionService.handleProviderCallback(provider, query));
    }

    @PostMapping("/webhooks/{provider}")
    public ResponseEntity<Map<String, Object>> providerWebhook(
            @PathVariable String provider,
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String rawPayload
    ) {
        return ResponseEntity.ok(subscriptionService.handleProviderWebhook(provider, headers, rawPayload));
    }
}

