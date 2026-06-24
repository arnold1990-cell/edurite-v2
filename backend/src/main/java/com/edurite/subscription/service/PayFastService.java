package com.edurite.subscription.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.subscription.dto.PayFastInitiateRequest;
import com.edurite.subscription.dto.PayFastInitiateResponse;
import com.edurite.subscription.dto.SubscriptionCheckoutResponse;
import com.edurite.subscription.dto.SubscriptionPaymentStatusResponse;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PayFastService {

    private static final Logger log = LoggerFactory.getLogger(PayFastService.class);
    private static final String PROVIDER_PAYFAST = "payfast";
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:5173";

    private final SubscriptionService subscriptionService;
    private final String frontendUrl;

    public PayFastService(
            SubscriptionService subscriptionService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.subscriptionService = subscriptionService;
        this.frontendUrl = frontendUrl;
    }

    public PayFastInitiateResponse initiate(Principal principal, PayFastInitiateRequest request) {
        SubscriptionCheckoutResponse checkout = subscriptionService.checkout(principal, request.planCode(), PROVIDER_PAYFAST);
        String paymentUrl = checkout.checkoutUrl();
        Map<String, String> formFields = new LinkedHashMap<>();

        if (checkout.checkoutPayload() != null) {
            Object paymentUrlValue = checkout.checkoutPayload().get("paymentUrl");
            if (paymentUrlValue instanceof String value && !value.isBlank()) {
                paymentUrl = value.trim();
            }
            Object fields = checkout.checkoutPayload().get("formFields");
            if (fields instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    formFields.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }

        if (paymentUrl == null || paymentUrl.isBlank() || formFields.isEmpty()) {
            log.warn("PayFast initiation failed: missing checkout URL or form fields for paymentReference={}", checkout.paymentReference());
            throw new ResourceConflictException("Could not initialize PayFast checkout.");
        }

        log.info(
                "PayFast initiation prepared: paymentReference={}, paymentStatus={}, checkoutUrl={}",
                checkout.paymentReference(),
                checkout.paymentStatus(),
                paymentUrl
        );

        return new PayFastInitiateResponse(
                checkout.paymentReference(),
                checkout.provider(),
                checkout.paymentStatus(),
                checkout.subscriptionStatus(),
                paymentUrl,
                formFields,
                checkout.message()
        );
    }

    public Map<String, Object> handleNotify(Map<String, String> headers, String rawPayload) {
        String normalizedRawPayload = rawPayload == null ? "" : rawPayload.trim();
        Map<String, String> payload = parseFormEncoded(normalizedRawPayload);
        String paymentReference = firstNonBlank(payload.get("m_payment_id"), payload.get("paymentReference"), payload.get("reference"));
        if (payload.isEmpty()) {
            log.warn("PayFast ITN received with empty payload. headersPresent={}, rawPayloadLength={}",
                    headers == null ? 0 : headers.size(),
                    normalizedRawPayload.length());
        } else {
            log.info(
                    "PayFast ITN received: reference={}, status={}, hasSignature={}",
                    paymentReference,
                    payload.get("payment_status"),
                    payload.get("signature") != null && !payload.get("signature").isBlank()
            );
        }

        Map<String, Object> result = subscriptionService.handleProviderWebhook(PROVIDER_PAYFAST, headers, normalizedRawPayload);
        Object verified = result.get("verified");
        if (Boolean.FALSE.equals(verified)) {
            log.warn("PayFast ITN could not be fully verified: reference={}, result={}", paymentReference, result);
        }
        return result;
    }

    public String handleReturn(Map<String, String> query) {
        String paymentReference = firstNonBlank(
                query.get("paymentReference"),
                query.get("m_payment_id"),
                query.get("reference")
        );
        return buildFrontendRedirect("processing", paymentReference, null);
    }

    public String handleCancel(Map<String, String> query) {
        String paymentReference = firstNonBlank(
                query.get("paymentReference"),
                query.get("m_payment_id"),
                query.get("reference")
        );
        if (paymentReference != null) {
            Map<String, String> callbackPayload = new LinkedHashMap<>(query);
            callbackPayload.put("paymentReference", paymentReference);
            callbackPayload.put("status", "CANCELLED");
            callbackPayload.putIfAbsent("reason", "Checkout cancelled on PayFast.");
            subscriptionService.handleProviderCallback(PROVIDER_PAYFAST, callbackPayload);
        }
        return buildFrontendRedirect("cancel", paymentReference, "Checkout cancelled on PayFast.");
    }

    public SubscriptionPaymentStatusResponse paymentStatus(Principal principal, String paymentReference) {
        return subscriptionService.paymentStatus(principal, paymentReference);
    }

    private String buildFrontendRedirect(String result, String paymentReference, String message) {
        String base = normalizeFrontendSubscriptionUrl();
        StringBuilder url = new StringBuilder(base);
        url.append(base.contains("?") ? "&" : "?");
        url.append("checkoutResult=").append(urlEncode(result));
        if (paymentReference != null && !paymentReference.isBlank()) {
            url.append("&paymentReference=").append(urlEncode(paymentReference));
        }
        url.append("&provider=payfast");
        if (message != null && !message.isBlank()) {
            url.append("&message=").append(urlEncode(message));
        }
        return url.toString();
    }

    private String normalizeFrontendSubscriptionUrl() {
        String configured = frontendUrl == null || frontendUrl.isBlank() ? DEFAULT_FRONTEND_URL : frontendUrl.trim();
        String normalized = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        return normalized + "/student/subscription";
    }

    private String toFormEncoded(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (!encoded.isEmpty()) {
                encoded.append('&');
            }
            encoded.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue() == null ? "" : entry.getValue()));
        }
        return encoded.toString();
    }

    private Map<String, String> parseFormEncoded(String rawPayload) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        if (rawPayload == null || rawPayload.isBlank()) {
            return payload;
        }
        for (String pair : rawPayload.split("&")) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = urlDecode(keyValue[0]);
            String value = keyValue.length > 1 ? urlDecode(keyValue[1]) : "";
            if (!key.isBlank()) {
                payload.put(key, value);
            }
        }
        return payload;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}

