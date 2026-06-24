package com.edurite.subscription.payment.provider;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.subscription.payment.PayFastConfig;
import com.edurite.subscription.payment.PayFastSignatureService;
import com.edurite.subscription.payment.PayFastSignatureService.PayFastSignatureResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PayFastPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(PayFastPaymentProvider.class);
    private static final String PAYFAST_CODE = "payfast";
    private static final String DEFAULT_PROCESS_URL = "https://www.payfast.co.za/eng/process";
    private static final String DEFAULT_SANDBOX_PROCESS_URL = "https://sandbox.payfast.co.za/eng/process";
    private static final String DEFAULT_VALIDATE_URL = "https://www.payfast.co.za/eng/query/validate";
    private static final String DEFAULT_SANDBOX_VALIDATE_URL = "https://sandbox.payfast.co.za/eng/query/validate";
    private static final String NOTIFY_VALIDATE_COMMAND = "cmd=_notify-validate";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final MediaType FORM_MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded");

    private final PayFastConfig payFastConfig;
    private final PayFastSignatureService signatureService;
    private final OkHttpClient httpClient;

    @Autowired
    public PayFastPaymentProvider(PayFastConfig payFastConfig, PayFastSignatureService signatureService) {
        this(payFastConfig, signatureService, new OkHttpClient());
    }

    PayFastPaymentProvider(PayFastConfig payFastConfig, PayFastSignatureService signatureService, OkHttpClient httpClient) {
        this.payFastConfig = payFastConfig;
        this.signatureService = signatureService;
        this.httpClient = httpClient;
    }

    @Override
    public String providerCode() {
        return PAYFAST_CODE;
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutContext context) {
        ensureConfigured();
        if (context.amount() == null || context.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentCheckoutResult.failed(providerCode(), "PayFast checkout requires a positive amount.", Map.of());
        }

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", safeValue(payFastConfig.merchantId(), ""));
        fields.put("merchant_key", safeValue(payFastConfig.merchantKey(), ""));
        fields.put("return_url", safeValue(context.successUrl(), ""));
        fields.put("cancel_url", safeValue(context.cancelUrl(), ""));
        fields.put("notify_url", safeValue(context.notifyUrl(), ""));
        fields.put("name_first", sanitizeText(safeValue(context.customerFirstName(), "EduRite")));
        fields.put("name_last", sanitizeText(safeValue(context.customerLastName(), "Student")));
        fields.put("email_address", safeValue(context.customerEmail(), "noreply@edurite.local"));
        fields.put("m_payment_id", context.paymentReference());
        // Amount is normalized once and reused for posting + signing.
        String normalizedAmount = context.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        fields.put("amount", normalizedAmount);
        fields.put("item_name", sanitizeText(safeValue(context.planName(), "EduRite Premium")));
        fields.put("item_description", sanitizeText(safeValue(context.description(), "EduRite premium subscription upgrade")));
        fields.put("custom_str1", sanitizeText(safeValue(context.planCode(), "")));

        PayFastSignatureResult signatureResult = signatureService.generate(fields, payFastConfig.passphrase());

        // Final posted payload comes from the exact same signed field source.
        LinkedHashMap<String, String> formFields = new LinkedHashMap<>(signatureResult.orderedFields());
        formFields.put("signature", signatureResult.signature());
        logSignatureDebug("checkout", signatureResult, formFields);

        String processUrl = resolveProcessUrl();
        logCheckoutConfiguration(context, processUrl);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentUrl", processUrl);
        payload.put("formFields", formFields);
        payload.put("sandbox", payFastConfig.sandboxEnabled());

        return PaymentCheckoutResult.pending(
                providerCode(),
                context.paymentReference(),
                null,
                processUrl,
                payload
        );
    }

    @Override
    public PaymentConfirmationResult confirmPayment(PaymentConfirmationContext context) {
        String statusHint = normalizeStatus(metadataValue(context, "payment_status"));
        if ("CANCELLED".equals(statusHint)) {
            return PaymentConfirmationResult.cancelled(providerCode(), "Payment was cancelled on PayFast.", context.metadata() == null ? Map.of() : Map.copyOf(context.metadata()));
        }
        if ("FAILED".equals(statusHint)) {
            return PaymentConfirmationResult.failed(providerCode(), "PayFast reported a failed payment.", context.metadata() == null ? Map.of() : Map.copyOf(context.metadata()));
        }
        return PaymentConfirmationResult.pending(
                providerCode(),
                firstNonBlank(context.providerOrderId(), metadataValue(context, "pf_payment_id")),
                context.providerSessionId(),
                Map.of("message", "Awaiting PayFast ITN verification.")
        );
    }

    @Override
    public PaymentWebhookResult handleWebhook(Map<String, String> headers, String rawPayload) {
        Map<String, String> payload = parseFormEncoded(rawPayload);
        String paymentReference = firstNonBlank(payload.get("m_payment_id"), payload.get("paymentReference"), payload.get("reference"));
        String eventId = firstNonBlank(payload.get("pf_payment_id"), paymentReference);
        String paymentStatus = normalizeStatus(payload.get("payment_status"));

        boolean signaturePresent = payload.get("signature") != null && !payload.get("signature").isBlank();
        boolean signatureValid = verifySignature(payload);
        boolean merchantIdValid = validateMerchantId(payload);
        boolean serverValidated = validateWithPayFast(rawPayload);
        // Server-to-server validation plus signature verification are mandatory for trust.
        boolean verified = signaturePresent && signatureValid && merchantIdValid && serverValidated;
        if (!signaturePresent) {
            log.warn("PayFast ITN rejected: missing signature. reference={}, eventId={}", paymentReference, eventId);
        } else if (!signatureValid) {
            log.warn("PayFast ITN rejected: signature mismatch. reference={}, eventId={}", paymentReference, eventId);
        }
        if (!merchantIdValid) {
            log.warn("PayFast ITN rejected: merchant id mismatch. reference={}, eventId={}", paymentReference, eventId);
        }
        if (!serverValidated) {
            log.warn("PayFast ITN rejected: server-to-server validation failed. reference={}, eventId={}", paymentReference, eventId);
        }

        Map<String, Object> resultPayload = new LinkedHashMap<>(payload);
        resultPayload.put("signature_present", signaturePresent);
        resultPayload.put("signature_valid", signatureValid);
        resultPayload.put("merchant_id_valid", merchantIdValid);
        resultPayload.put("server_validated", serverValidated);

        if (log.isDebugEnabled()) {
            log.debug(
                    "PayFast ITN processed: reference={}, eventId={}, status={}, signatureValid={}, merchantIdValid={}, serverValidated={}, verified={}",
                    paymentReference,
                    eventId,
                    paymentStatus,
                    signatureValid,
                    merchantIdValid,
                    serverValidated,
                    verified
            );
        }

        return new PaymentWebhookResult(
                providerCode(),
                eventId,
                "PAYFAST_ITN",
                paymentReference,
                paymentStatus,
                verified,
                resultPayload
        );
    }

    private String normalizeStatus(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "COMPLETE", "COMPLETED", "SUCCESS" -> "COMPLETED";
            case "FAILED" -> "FAILED";
            case "CANCELLED", "CANCELED" -> "CANCELLED";
            default -> "PENDING";
        };
    }

    private boolean verifySignature(Map<String, String> payload) {
        String provided = payload.get("signature");
        if (provided == null || provided.isBlank()) {
            return false;
        }
        PayFastSignatureResult signatureResult = signatureService.generate(payload, payFastConfig.passphrase());
        logSignatureDebug("itn-verify", signatureResult, payload);
        return MessageDigest.isEqual(
                provided.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                signatureResult.signature().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean validateWithPayFast(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return false;
        }
        String validationPayload = rawPayload.contains("cmd=_notify-validate")
                ? rawPayload
                : rawPayload + "&" + NOTIFY_VALIDATE_COMMAND;
        Request request = new Request.Builder()
                .url(resolveValidateUrl())
                .post(RequestBody.create(validationPayload, FORM_MEDIA_TYPE))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String validation = body == null ? "" : body.string();
            return response.isSuccessful() && "VALID".equalsIgnoreCase(validation.trim());
        } catch (IOException ex) {
            log.warn("PayFast ITN server validation request failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean validateMerchantId(Map<String, String> payload) {
        String expected = payFastConfig.merchantId() == null ? "" : payFastConfig.merchantId().trim();
        String received = payload.get("merchant_id") == null ? "" : payload.get("merchant_id").trim();
        return !expected.isBlank() && !received.isBlank() && expected.equals(received);
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
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            if (!key.isBlank()) {
                payload.put(key, value);
            }
        }
        return payload;
    }

    private String resolveProcessUrl() {
        if (payFastConfig.processUrl() != null && !payFastConfig.processUrl().isBlank()) {
            return payFastConfig.processUrl().trim();
        }
        return payFastConfig.sandboxEnabled() ? DEFAULT_SANDBOX_PROCESS_URL : DEFAULT_PROCESS_URL;
    }

    private String resolveValidateUrl() {
        if (payFastConfig.validateUrl() != null && !payFastConfig.validateUrl().isBlank()) {
            return payFastConfig.validateUrl().trim();
        }
        return payFastConfig.sandboxEnabled() ? DEFAULT_SANDBOX_VALIDATE_URL : DEFAULT_VALIDATE_URL;
    }

    private void ensureConfigured() {
        if (payFastConfig.merchantId() == null || payFastConfig.merchantId().isBlank()
                || payFastConfig.merchantKey() == null || payFastConfig.merchantKey().isBlank()) {
            throw new ResourceConflictException("PayFast is not configured on this server.");
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String metadataValue(PaymentConfirmationContext context, String key) {
        if (context.metadata() == null) {
            return null;
        }
        String value = context.metadata().get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\p{Cntrl}", "")
                .replaceAll(WHITESPACE_REGEX, " ")
                .trim();
        return sanitized;
    }

    private void logCheckoutConfiguration(PaymentCheckoutContext context, String processUrl) {
        if (!log.isInfoEnabled()) {
            return;
        }
        log.info(
                "PayFast checkout config: reference={}, sandbox={}, merchantId={}, processUrl={}, returnUrl={}, cancelUrl={}, notifyUrl={}",
                context.paymentReference(),
                payFastConfig.sandboxEnabled(),
                mask(payFastConfig.merchantId()),
                processUrl,
                safeValue(context.successUrl(), ""),
                safeValue(context.cancelUrl(), ""),
                safeValue(context.notifyUrl(), "")
        );
    }

    private void logSignatureDebug(String flow, PayFastSignatureResult signatureResult, Map<String, String> submittedFields) {
        boolean sandboxDebugEnabled = payFastConfig.sandboxEnabled();
        if ((!payFastConfig.debugSignatureEnabled() && !sandboxDebugEnabled) || !log.isDebugEnabled()) {
            return;
        }
        Map<String, String> maskedFields = new LinkedHashMap<>();
        if (submittedFields != null) {
            for (Map.Entry<String, String> entry : submittedFields.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey();
                String value = entry.getValue();
                if ("merchant_key".equalsIgnoreCase(key)) {
                    maskedFields.put(key, mask(value));
                } else {
                    maskedFields.put(key, value);
                }
            }
        }
        String maskedInput = signatureResult.signatureInput().replaceAll("(?i)(^|&)passphrase=[^&]*", "$1passphrase=***");
        log.debug("PayFast signature debug [{}]: orderedFields={}", flow, signatureResult.orderedFields().keySet());
        log.debug("PayFast signature debug [{}]: submittedFields={}", flow, maskedFields);
        log.debug("PayFast signature debug [{}]: signatureInput={}", flow, maskedInput);
        log.debug("PayFast signature debug [{}]: signature={}", flow, signatureResult.signature());
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
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

