package com.edurite.subscription.payment.provider;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.subscription.payment.PayPalPaymentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PayPalPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(PayPalPaymentProvider.class);
    private static final String SANDBOX_BASE_URL = "https://api-m.sandbox.paypal.com";
    private static final String LIVE_BASE_URL = "https://api-m.paypal.com";
    private static final String DEFAULT_PAYPAL_CURRENCY = "USD";
    private static final Set<String> UNSUPPORTED_PAYPAL_CURRENCIES = Set.of("ZAR");
    private static final Pattern ISO_CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final MediaType FORM_MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    private final PayPalPaymentProperties payPalPaymentProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public PayPalPaymentProvider(
            PayPalPaymentProperties payPalPaymentProperties,
            ObjectMapper objectMapper
    ) {
        this.payPalPaymentProperties = payPalPaymentProperties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    @Override
    public String providerCode() {
        return "paypal";
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutContext context) {
        ensureConfigured();
        if (context.amount() == null || context.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentCheckoutResult.failed(providerCode(), "PayPal checkout requires a positive amount.", Map.of());
        }

        String accessToken = requestAccessToken();
        String sourceCurrency = normalizeContextCurrency(context.currency());
        String paypalCurrency = resolvePayPalCurrency();
        BigDecimal providerAmount = resolveProviderAmount(context.amount(), sourceCurrency, paypalCurrency);
        String amountValue = providerAmount.toPlainString();

        if (!paypalCurrency.equals(sourceCurrency)) {
            log.info("PayPal currency override applied: paymentReference={}, sourceCurrency={}, providerCurrency={}, amount={}",
                    context.paymentReference(), sourceCurrency, paypalCurrency, amountValue);
        }
        log.info("Creating PayPal checkout order: paymentReference={}, currency={}, amount={}",
                context.paymentReference(), paypalCurrency, amountValue);

        Map<String, Object> orderPayload = new LinkedHashMap<>();
        orderPayload.put("intent", "CAPTURE");
        orderPayload.put("purchase_units", java.util.List.of(
                Map.of(
                        "reference_id", context.paymentReference(),
                        "custom_id", context.paymentReference(),
                        "description", context.description() == null ? context.planName() : context.description(),
                        "amount", Map.of(
                                "currency_code", paypalCurrency,
                                "value", amountValue
                        )
                )
        ));
        orderPayload.put("application_context", Map.of(
                "brand_name", "EduRite",
                "user_action", "PAY_NOW",
                "return_url", context.successUrl(),
                "cancel_url", context.cancelUrl()
        ));

        JsonNode responseNode = postJson("/v2/checkout/orders", accessToken, orderPayload);
        String orderId = textValue(responseNode, "id");
        String approveUrl = findLinkHref(responseNode.path("links"), "approve");
        if (orderId == null || approveUrl == null) {
            return PaymentCheckoutResult.failed(providerCode(), "PayPal did not return an approval URL.", toMap(responseNode));
        }

        return PaymentCheckoutResult.pending(
                providerCode(),
                orderId,
                null,
                approveUrl,
                toMap(responseNode)
        );
    }

    @Override
    public PaymentConfirmationResult confirmPayment(PaymentConfirmationContext context) {
        ensureConfigured();
        String orderId = firstNonBlank(context.providerOrderId(), context.token(), metadataValue(context, "token"), metadataValue(context, "orderId"));
        if (orderId == null) {
            log.warn("PayPal confirmation rejected: missing order id for paymentReference={}", context.paymentReference());
            return PaymentConfirmationResult.failed(providerCode(), "PayPal order id is required for confirmation.", Map.of());
        }

        String accessToken = requestAccessToken();
        JsonNode orderNode = get("/v2/checkout/orders/" + orderId, accessToken);
        String orderStatus = textValue(orderNode, "status");
        log.info("PayPal confirmation status check: paymentReference={}, orderId={}, orderStatus={}",
                context.paymentReference(), orderId, orderStatus);
        if ("COMPLETED".equalsIgnoreCase(orderStatus)) {
            String captureId = extractCaptureId(orderNode);
            return PaymentConfirmationResult.completed(
                    providerCode(),
                    orderId,
                    null,
                    captureId,
                    null,
                    toMap(orderNode)
            );
        }
        if ("VOIDED".equalsIgnoreCase(orderStatus)) {
            return PaymentConfirmationResult.cancelled(providerCode(), "PayPal order was voided.", toMap(orderNode));
        }
        if ("CREATED".equalsIgnoreCase(orderStatus)) {
            return PaymentConfirmationResult.pending(providerCode(), orderId, null, toMap(orderNode));
        }

        JsonNode captureNode;
        try {
            captureNode = postRaw("/v2/checkout/orders/" + orderId + "/capture", accessToken, "{}");
        } catch (ResourceConflictException ex) {
            JsonNode refreshOrder = get("/v2/checkout/orders/" + orderId, accessToken);
            String refreshedStatus = textValue(refreshOrder, "status");
            if ("COMPLETED".equalsIgnoreCase(refreshedStatus)) {
                return PaymentConfirmationResult.completed(
                        providerCode(),
                        orderId,
                        null,
                        extractCaptureId(refreshOrder),
                        null,
                        toMap(refreshOrder)
                );
            }
            if ("VOIDED".equalsIgnoreCase(refreshedStatus)) {
                return PaymentConfirmationResult.cancelled(providerCode(), "PayPal order was voided.", toMap(refreshOrder));
            }
            if ("APPROVED".equalsIgnoreCase(refreshedStatus) || "CREATED".equalsIgnoreCase(refreshedStatus)) {
                return PaymentConfirmationResult.pending(providerCode(), orderId, null, toMap(refreshOrder));
            }
            return PaymentConfirmationResult.failed(providerCode(), ex.getMessage(), toMap(refreshOrder));
        }

        String captureStatus = textValue(captureNode, "status");
        log.info("PayPal capture response: paymentReference={}, orderId={}, captureStatus={}",
                context.paymentReference(), orderId, captureStatus);
        if ("COMPLETED".equalsIgnoreCase(captureStatus)) {
            return PaymentConfirmationResult.completed(
                    providerCode(),
                    orderId,
                    null,
                    extractCaptureId(captureNode),
                    null,
                    toMap(captureNode)
            );
        }
        if ("PAYER_ACTION_REQUIRED".equalsIgnoreCase(captureStatus) || "APPROVED".equalsIgnoreCase(captureStatus)) {
            return PaymentConfirmationResult.pending(providerCode(), orderId, null, toMap(captureNode));
        }
        if ("VOIDED".equalsIgnoreCase(captureStatus)) {
            return PaymentConfirmationResult.cancelled(providerCode(), "PayPal order was voided.", toMap(captureNode));
        }
        log.warn("PayPal capture did not complete payment: paymentReference={}, orderId={}, captureStatus={}",
                context.paymentReference(), orderId, captureStatus);
        return PaymentConfirmationResult.failed(providerCode(), "PayPal payment is not completed.", toMap(captureNode));
    }

    @Override
    public PaymentWebhookResult handleWebhook(Map<String, String> headers, String rawPayload) {
        JsonNode payloadNode = readJson(rawPayload);
        String eventId = textValue(payloadNode, "id");
        String eventType = textValue(payloadNode, "event_type");
        JsonNode resource = payloadNode.path("resource");
        String paymentReference = firstNonBlank(
                textValue(resource, "custom_id"),
                textValue(resource, "invoice_id")
        );

        String status = "PENDING";
        if (eventType != null) {
            String normalized = eventType.toUpperCase(Locale.ROOT);
            if (normalized.contains("COMPLETED")) {
                status = "COMPLETED";
            } else if (normalized.contains("DENIED") || normalized.contains("FAILED")) {
                status = "FAILED";
            } else if (normalized.contains("CANCELLED") || normalized.contains("VOIDED")) {
                status = "CANCELLED";
            }
        }

        return new PaymentWebhookResult(
                providerCode(),
                eventId,
                eventType == null ? "UNKNOWN" : eventType,
                paymentReference,
                status,
                false,
                toMap(payloadNode)
        );
    }

    private String requestAccessToken() {
        String credentials = payPalPaymentProperties.clientId().trim() + ":" + payPalPaymentProperties.clientSecret().trim();
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(resolveApiBaseUrl() + "/v1/oauth2/token")
                .post(RequestBody.create("grant_type=client_credentials", FORM_MEDIA_TYPE))
                .header("Authorization", "Basic " + basicAuth)
                .header("Accept", "application/json")
                .header("Accept-Language", "en_US")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        JsonNode responseNode = executeJson(request);
        String accessToken = textValue(responseNode, "access_token");
        if (accessToken == null) {
            throw new ResourceConflictException("Could not authenticate with PayPal.");
        }
        return accessToken;
    }

    private JsonNode postJson(String path, String accessToken, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            return postRaw(path, accessToken, body);
        } catch (IOException ex) {
            throw new ResourceConflictException("PayPal request payload could not be created.");
        }
    }

    private JsonNode postRaw(String path, String accessToken, String body) {
        Request request = new Request.Builder()
                .url(resolveApiBaseUrl() + path)
                .post(RequestBody.create(body == null ? "{}" : body, JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();
        return executeJson(request);
    }

    private JsonNode get(String path, String accessToken) {
        Request request = new Request.Builder()
                .url(resolveApiBaseUrl() + path)
                .get()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();
        return executeJson(request);
    }

    private JsonNode executeJson(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String payload = body == null ? "{}" : body.string();
            JsonNode jsonNode = readJson(payload);
            if (!response.isSuccessful()) {
                String message = firstNonBlank(
                        textValue(jsonNode, "error_description"),
                        textValue(jsonNode.path("error"), "message"),
                        formatErrorDetail(jsonNode.path("details").path(0)),
                        textValue(jsonNode, "message"),
                        "PayPal request failed."
                );
                throw new ResourceConflictException(message);
            }
            return jsonNode;
        } catch (IOException ex) {
            throw new ResourceConflictException("PayPal request failed.");
        }
    }

    private JsonNode readJson(String payload) {
        try {
            if (payload == null || payload.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(payload);
        } catch (IOException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }

    private String findLinkHref(JsonNode linksNode, String rel) {
        if (linksNode == null || !linksNode.isArray()) {
            return null;
        }
        for (JsonNode link : linksNode) {
            if (rel.equalsIgnoreCase(textValue(link, "rel"))) {
                return textValue(link, "href");
            }
        }
        return null;
    }

    private String formatErrorDetail(JsonNode detailNode) {
        if (detailNode == null || detailNode.isMissingNode() || detailNode.isNull()) {
            return null;
        }
        String description = textValue(detailNode, "description");
        String issue = textValue(detailNode, "issue");
        if (description == null) {
            return issue;
        }
        if (issue == null) {
            return description;
        }
        return issue + ": " + description;
    }

    private String extractCaptureId(JsonNode node) {
        JsonNode captures = node.path("purchase_units").path(0).path("payments").path("captures");
        if (captures.isArray() && !captures.isEmpty()) {
            return textValue(captures.get(0), "id");
        }
        return null;
    }

    private String metadataValue(PaymentConfirmationContext context, String key) {
        if (context.metadata() == null) {
            return null;
        }
        String value = context.metadata().get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String resolveApiBaseUrl() {
        String configured = payPalPaymentProperties.baseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        String mode = payPalPaymentProperties.mode();
        if (mode != null && mode.trim().equalsIgnoreCase("live")) {
            return LIVE_BASE_URL;
        }
        return SANDBOX_BASE_URL;
    }

    private String normalizeContextCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return DEFAULT_PAYPAL_CURRENCY;
        }
        String normalized = rawCurrency.trim().toUpperCase(Locale.ROOT);
        if (!ISO_CURRENCY_PATTERN.matcher(normalized).matches()) {
            log.warn("Invalid subscription plan currency '{}' detected. Falling back to {} for PayPal checkout.",
                    rawCurrency, DEFAULT_PAYPAL_CURRENCY);
            return DEFAULT_PAYPAL_CURRENCY;
        }
        return normalized;
    }

    private String resolvePayPalCurrency() {
        String configuredCurrency = payPalPaymentProperties.currency() == null || payPalPaymentProperties.currency().isBlank()
                ? DEFAULT_PAYPAL_CURRENCY
                : payPalPaymentProperties.currency().trim().toUpperCase(Locale.ROOT);

        if (!ISO_CURRENCY_PATTERN.matcher(configuredCurrency).matches()) {
            log.error("Invalid PAYPAL currency configuration '{}'. Expected 3-letter ISO code (example: USD).",
                    configuredCurrency);
            throw new ResourceConflictException("PayPal currency configuration is invalid. Set PAYMENT_PAYPAL_CURRENCY to a valid ISO code (for example: USD).");
        }
        if (UNSUPPORTED_PAYPAL_CURRENCIES.contains(configuredCurrency)) {
            log.error("Unsupported PayPal currency configured: {}. Use PAYMENT_PAYPAL_CURRENCY=USD.", configuredCurrency);
            throw new ResourceConflictException("Unsupported PayPal currency configured: " + configuredCurrency + ". Set PAYMENT_PAYPAL_CURRENCY=USD.");
        }
        return configuredCurrency;
    }

    private BigDecimal resolveProviderAmount(BigDecimal amount, String sourceCurrency, String providerCurrency) {
        if (!providerCurrency.equals(sourceCurrency)) {
            // Placeholder for explicit FX conversion once exchange-rate config is introduced.
            log.warn("PayPal checkout is using provider currency {} while plan currency is {}. Amount conversion is not applied.",
                    providerCurrency, sourceCurrency);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureConfigured() {
        if (payPalPaymentProperties.clientId() == null || payPalPaymentProperties.clientId().isBlank()
                || payPalPaymentProperties.clientSecret() == null || payPalPaymentProperties.clientSecret().isBlank()) {
            throw new ResourceConflictException("PayPal is not configured on this server.");
        }
    }
}

