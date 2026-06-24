package com.edurite.subscription.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurite.subscription.payment.PayFastConfig;
import com.edurite.subscription.payment.PayFastSignatureService;
import com.edurite.subscription.payment.PayFastSignatureService.PayFastSignatureResult;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayFastPaymentProviderTest {

    private final PayFastSignatureService signatureService = new PayFastSignatureService();

    @Test
    void createCheckoutSignsExactFormPayloadThatWillBeSubmitted() {
        PayFastPaymentProvider provider = new PayFastPaymentProvider(
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "pass123",
                        true,
                        false,
                        "https://sandbox.payfast.co.za/eng/process",
                        "https://sandbox.payfast.co.za/eng/query/validate",
                        "http://127.0.0.1:5173/payments/payfast/return",
                        "http://127.0.0.1:5173/payments/payfast/cancel",
                        "http://127.0.0.1:8080/api/payments/payfast/notify"
                ),
                signatureService
        );

        PaymentCheckoutResult result = provider.createCheckout(checkoutContext(
                "PAY-123",
                "PLAN_PREMIUM",
                "Premium Plan",
                "EduRite premium subscription",
                new BigDecimal("100")
        ));

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.checkoutUrl()).isEqualTo("https://sandbox.payfast.co.za/eng/process");
        assertThat(result.rawResponse()).containsEntry("paymentUrl", "https://sandbox.payfast.co.za/eng/process");

        Map<String, String> formFields = extractFormFields(result);
        String signature = formFields.get("signature");
        assertThat(signature).isNotBlank();

        LinkedHashMap<String, String> signedPayload = new LinkedHashMap<>(formFields);
        signedPayload.remove("signature");

        PayFastSignatureResult regenerated = signatureService.generate(signedPayload, "pass123");
        String expectedInput = "merchant_id=10000100"
                + "&merchant_key=46f0cd694581a"
                + "&return_url=http%3A%2F%2F127.0.0.1%3A5173%2Fpayments%2Fpayfast%2Freturn"
                + "&cancel_url=http%3A%2F%2F127.0.0.1%3A5173%2Fpayments%2Fpayfast%2Fcancel"
                + "&notify_url=http%3A%2F%2F127.0.0.1%3A8080%2Fapi%2Fpayments%2Fpayfast%2Fnotify"
                + "&name_first=Arnold"
                + "&name_last=Madaz"
                + "&email_address=arnold%40example.com"
                + "&m_payment_id=PAY-123"
                + "&amount=100.00"
                + "&item_name=Premium+Plan"
                + "&item_description=EduRite+premium+subscription"
                + "&custom_str1=PLAN_PREMIUM"
                + "&passphrase=pass123";

        assertThat(regenerated.signatureInput()).isEqualTo(expectedInput);
        assertThat(signature).isEqualTo(regenerated.signature());
        assertThat(formFields)
                .containsEntry("amount", "100.00")
                .containsEntry("return_url", "http://127.0.0.1:5173/payments/payfast/return")
                .containsEntry("cancel_url", "http://127.0.0.1:5173/payments/payfast/cancel")
                .containsEntry("notify_url", "http://127.0.0.1:8080/api/payments/payfast/notify");
    }

    @Test
    void changingPostedFieldAfterSigningCreatesInvalidSignatureRegressionGuard() {
        PayFastPaymentProvider provider = new PayFastPaymentProvider(
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "pass123",
                        true,
                        false,
                        "https://sandbox.payfast.co.za/eng/process",
                        "https://sandbox.payfast.co.za/eng/query/validate",
                        "http://127.0.0.1:5173/payments/payfast/return",
                        "http://127.0.0.1:5173/payments/payfast/cancel",
                        "http://127.0.0.1:8080/api/payments/payfast/notify"
                ),
                signatureService
        );

        PaymentCheckoutResult result = provider.createCheckout(checkoutContext(
                "PAY-456",
                "PLAN_PREMIUM",
                "Premium Plan",
                "EduRite premium subscription",
                new BigDecimal("49.99")
        ));

        Map<String, String> formFields = extractFormFields(result);
        String originalSignature = formFields.get("signature");

        LinkedHashMap<String, String> tamperedPayload = new LinkedHashMap<>(formFields);
        tamperedPayload.remove("signature");
        tamperedPayload.put("amount", "99.00");
        String tamperedSignature = signatureService.generate(tamperedPayload, "pass123").signature();

        assertThat(tamperedSignature).isNotEqualTo(originalSignature);
    }

    @Test
    void createCheckoutFormatsAmountAndHandlesBlankOptionalFieldsConsistently() {
        PayFastPaymentProvider provider = new PayFastPaymentProvider(
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "pass123",
                        false,
                        false,
                        "https://www.payfast.co.za/eng/process",
                        "https://www.payfast.co.za/eng/query/validate",
                        "http://localhost:5173/payments/payfast/return",
                        "http://localhost:5173/payments/payfast/cancel",
                        "http://localhost:8080/api/payments/payfast/notify"
                ),
                signatureService
        );

        PaymentCheckoutResult result = provider.createCheckout(new PaymentCheckoutContext(
                UUID.randomUUID(),
                "PAY-789",
                "",
                "Premium Plan",
                "MONTHLY",
                new BigDecimal("49.99"),
                "ZAR",
                "",
                "",
                "",
                "",
                "http://localhost:5173/payments/payfast/return",
                "http://localhost:5173/payments/payfast/cancel",
                "http://localhost:8080/api/payments/payfast/notify"
        ));

        assertThat(result.checkoutUrl()).isEqualTo("https://www.payfast.co.za/eng/process");
        Map<String, String> formFields = extractFormFields(result);
        assertThat(formFields)
                .containsEntry("amount", "49.99")
                .containsEntry("name_first", "EduRite")
                .containsEntry("name_last", "Student")
                .containsEntry("email_address", "noreply@edurite.local")
                .doesNotContainKey("custom_str1");
    }

    @Test
    void createCheckoutSanitizesPostedTextFieldsBeforeSigning() {
        PayFastPaymentProvider provider = new PayFastPaymentProvider(
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "pass123",
                        false,
                        false,
                        "https://www.payfast.co.za/eng/process",
                        "https://www.payfast.co.za/eng/query/validate",
                        "http://localhost:5173/payments/payfast/return",
                        "http://localhost:5173/payments/payfast/cancel",
                        "http://localhost:8080/api/payments/payfast/notify"
                ),
                signatureService
        );

        PaymentCheckoutResult result = provider.createCheckout(new PaymentCheckoutContext(
                UUID.randomUUID(),
                "PAY-321",
                "PLAN_PREMIUM",
                "Premium\tPlan\n",
                "EduRite premium\r\nsubscription",
                new BigDecimal("49.99"),
                "ZAR",
                "EduRite premium\r\nsubscription",
                "arnold@example.com",
                "Arnold\t",
                "Madaz\n",
                "http://localhost:5173/payments/payfast/return",
                "http://localhost:5173/payments/payfast/cancel",
                "http://localhost:8080/api/payments/payfast/notify"
        ));

        Map<String, String> formFields = extractFormFields(result);

        assertThat(formFields)
                .containsEntry("item_name", "Premium Plan")
                .containsEntry("item_description", "EduRite premium subscription")
                .containsEntry("name_first", "Arnold")
                .containsEntry("name_last", "Madaz");
    }

    @Test
    void createCheckoutFailsForNonPositiveAmount() {
        PayFastPaymentProvider provider = new PayFastPaymentProvider(
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "pass123",
                        true,
                        false,
                        "https://sandbox.payfast.co.za/eng/process",
                        "https://sandbox.payfast.co.za/eng/query/validate",
                        "http://127.0.0.1:5173/payments/payfast/return",
                        "http://127.0.0.1:5173/payments/payfast/cancel",
                        "http://127.0.0.1:8080/api/payments/payfast/notify"
                ),
                signatureService
        );

        PaymentCheckoutResult result = provider.createCheckout(checkoutContext(
                "PAY-000",
                BigDecimal.ZERO
        ));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failureReason()).contains("positive amount");
    }

    private PaymentCheckoutContext checkoutContext(
            String paymentReference,
            String planCode,
            String planName,
            String description,
            BigDecimal amount
    ) {
        return new PaymentCheckoutContext(
                UUID.randomUUID(),
                paymentReference,
                planCode,
                planName,
                "MONTHLY",
                amount,
                "ZAR",
                description,
                "arnold@example.com",
                "Arnold",
                "Madaz",
                "http://127.0.0.1:5173/payments/payfast/return",
                "http://127.0.0.1:5173/payments/payfast/cancel",
                "http://127.0.0.1:8080/api/payments/payfast/notify"
        );
    }

    private PaymentCheckoutContext checkoutContext(
            String paymentReference,
            BigDecimal amount
    ) {
        return new PaymentCheckoutContext(
                UUID.randomUUID(),
                paymentReference,
                "PLAN_PREMIUM",
                "Premium Plan",
                "MONTHLY",
                amount,
                "ZAR",
                "EduRite premium subscription",
                "arnold@example.com",
                "Arnold",
                "Madaz",
                "http://127.0.0.1:5173/payments/payfast/return",
                "http://127.0.0.1:5173/payments/payfast/cancel",
                "http://127.0.0.1:8080/api/payments/payfast/notify"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractFormFields(PaymentCheckoutResult result) {
        Object fields = result.rawResponse().get("formFields");
        assertThat(fields).isInstanceOf(Map.class);
        return (Map<String, String>) fields;
    }
}

