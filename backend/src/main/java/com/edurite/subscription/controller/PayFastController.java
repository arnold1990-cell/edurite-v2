package com.edurite.subscription.controller;

import com.edurite.subscription.dto.PayFastInitiateRequest;
import com.edurite.subscription.dto.PayFastInitiateResponse;
import com.edurite.subscription.dto.SubscriptionPaymentStatusResponse;
import com.edurite.subscription.service.PayFastService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.stream.Collectors;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/payments/payfast", "/api/payments/payfast"})
public class PayFastController {

    private final PayFastService payFastService;

    public PayFastController(PayFastService payFastService) {
        this.payFastService = payFastService;
    }

    @PostMapping("/initiate")
    public PayFastInitiateResponse initiate(
            Principal principal,
            @Valid @RequestBody PayFastInitiateRequest request
    ) {
        return payFastService.initiate(principal, request);
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notify(
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) throws IOException {
        String rawPayload = request.getReader().lines().collect(Collectors.joining(""));
        if (rawPayload == null || rawPayload.isBlank()) {
            String query = request.getQueryString();
            rawPayload = query == null ? "" : query.trim();
        }
        payFastService.handleNotify(headers, rawPayload);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/return")
    public void onReturn(
            @RequestParam Map<String, String> query,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(payFastService.handleReturn(query));
    }

    @GetMapping("/cancel")
    public void onCancel(
            @RequestParam Map<String, String> query,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(payFastService.handleCancel(query));
    }

    @GetMapping("/status")
    public SubscriptionPaymentStatusResponse status(
            Principal principal,
            @RequestParam String paymentReference
    ) {
        return payFastService.paymentStatus(principal, paymentReference);
    }
}

