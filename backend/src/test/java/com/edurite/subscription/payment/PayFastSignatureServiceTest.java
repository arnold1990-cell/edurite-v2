package com.edurite.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class PayFastSignatureServiceTest {

    private final PayFastSignatureService signatureService = new PayFastSignatureService();

    @Test
    void generateExcludesSignatureFieldAndBlankOptionals() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("merchant_key", "46f0cd694581a");
        fields.put("amount", "100.00");
        fields.put("item_name", "Premium Plan");
        fields.put("signature", "SHOULD-BE-IGNORED");
        fields.put("custom_str1", "   ");
        fields.put("return_url", "http://127.0.0.1:5173/payments/payfast/return");

        var result = signatureService.generate(fields, "secret pass");

        String expectedInput = "merchant_id=10000100"
                + "&merchant_key=46f0cd694581a"
                + "&amount=100.00"
                + "&item_name=Premium+Plan"
                + "&return_url=http%3A%2F%2F127.0.0.1%3A5173%2Fpayments%2Fpayfast%2Freturn"
                + "&passphrase=secret+pass";

        assertThat(result.orderedFields().keySet())
                .containsExactly("merchant_id", "merchant_key", "amount", "item_name", "return_url");
        assertThat(result.signatureInput()).isEqualTo(expectedInput);
        assertThat(result.signature()).isEqualTo(md5(expectedInput));
    }

    @Test
    void signatureChangesWhenPassphraseChanges() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("merchant_key", "46f0cd694581a");
        fields.put("amount", "100.00");
        fields.put("item_name", "Premium");

        String withOne = signatureService.generate(fields, "one").signature();
        String withTwo = signatureService.generate(fields, "two").signature();

        assertThat(withOne).isNotEqualTo(withTwo);
    }

    @Test
    void signatureFollowsSubmittedFieldOrder() {
        LinkedHashMap<String, String> orderedA = new LinkedHashMap<>();
        orderedA.put("merchant_id", "10000100");
        orderedA.put("merchant_key", "46f0cd694581a");
        orderedA.put("amount", "100.00");

        LinkedHashMap<String, String> orderedB = new LinkedHashMap<>();
        orderedB.put("amount", "100.00");
        orderedB.put("merchant_key", "46f0cd694581a");
        orderedB.put("merchant_id", "10000100");

        var signatureA = signatureService.generate(orderedA, "pass");
        var signatureB = signatureService.generate(orderedB, "pass");

        assertThat(signatureA.signatureInput()).isEqualTo("merchant_id=10000100&merchant_key=46f0cd694581a&amount=100.00&passphrase=pass");
        assertThat(signatureB.signatureInput()).isEqualTo("amount=100.00&merchant_key=46f0cd694581a&merchant_id=10000100&passphrase=pass");
        assertThat(signatureA.signature()).isNotEqualTo(signatureB.signature());
    }

    @Test
    void signatureIsStableForSamePayload() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("merchant_key", "46f0cd694581a");
        fields.put("amount", "100.00");
        fields.put("item_name", "Premium");

        String first = signatureService.generate(fields, "pass").signature();
        String second = signatureService.generate(fields, "pass").signature();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void amountFormattingAndUrlsArePreservedExactlyAsPosted() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", "100.00");
        fields.put("return_url", "http://localhost:5173/payments/payfast/return");
        fields.put("cancel_url", "http://localhost:5173/payments/payfast/cancel");
        fields.put("notify_url", "http://127.0.0.1:8080/api/payments/payfast/notify");

        var result = signatureService.generate(fields, "pass");

        assertThat(result.orderedFields())
                .containsEntry("amount", "100.00")
                .containsEntry("return_url", "http://localhost:5173/payments/payfast/return")
                .containsEntry("cancel_url", "http://localhost:5173/payments/payfast/cancel")
                .containsEntry("notify_url", "http://127.0.0.1:8080/api/payments/payfast/notify");
        assertThat(result.signatureInput()).contains("amount=100.00");
        assertThat(result.signatureInput())
                .contains("return_url=http%3A%2F%2Flocalhost%3A5173%2Fpayments%2Fpayfast%2Freturn")
                .contains("cancel_url=http%3A%2F%2Flocalhost%3A5173%2Fpayments%2Fpayfast%2Fcancel")
                .contains("notify_url=http%3A%2F%2F127.0.0.1%3A8080%2Fapi%2Fpayments%2Fpayfast%2Fnotify");
    }

    @Test
    void emptyAndNonEmptyPassphraseBehaveDifferently() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("merchant_key", "46f0cd694581a");

        String emptyPassphrase = signatureService.generate(fields, "").signature();
        String nullPassphrase = signatureService.generate(fields, null).signature();
        String withPassphrase = signatureService.generate(fields, "configured-passphrase").signature();

        assertThat(emptyPassphrase).isEqualTo(nullPassphrase);
        assertThat(withPassphrase).isNotEqualTo(emptyPassphrase);
    }

    @Test
    void specialCharactersAreEncodedDeterministically() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("item_name", "Premium Plan + Career & Bursary");

        var result = signatureService.generate(fields, "pass");

        assertThat(result.signatureInput())
                .contains("item_name=Premium+Plan+%2B+Career+%26+Bursary")
                .contains("&passphrase=pass");
    }

    @Test
    void generateMatchesKnownPayFastSamplePayload() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100");
        fields.put("merchant_key", "46f0cd694581a");
        fields.put("return_url", "https://example.com/return");
        fields.put("cancel_url", "https://example.com/cancel");
        fields.put("notify_url", "https://example.com/notify");
        fields.put("name_first", "Test");
        fields.put("name_last", "User");
        fields.put("email_address", "test@example.com");
        fields.put("m_payment_id", "PF-123");
        fields.put("amount", "49.99");
        fields.put("item_name", "Premium Plan");
        fields.put("item_description", "EduRite premium subscription");
        fields.put("custom_str1", "PLAN_PREMIUM");

        String expectedInput = "merchant_id=10000100"
                + "&merchant_key=46f0cd694581a"
                + "&return_url=https%3A%2F%2Fexample.com%2Freturn"
                + "&cancel_url=https%3A%2F%2Fexample.com%2Fcancel"
                + "&notify_url=https%3A%2F%2Fexample.com%2Fnotify"
                + "&name_first=Test"
                + "&name_last=User"
                + "&email_address=test%40example.com"
                + "&m_payment_id=PF-123"
                + "&amount=49.99"
                + "&item_name=Premium+Plan"
                + "&item_description=EduRite+premium+subscription"
                + "&custom_str1=PLAN_PREMIUM"
                + "&passphrase=pass123";

        var result = signatureService.generate(fields, "pass123");

        assertThat(result.signatureInput()).isEqualTo(expectedInput);
        assertThat(result.signature()).isEqualTo(md5(expectedInput));
    }

    @Test
    void trimsFieldValuesAndPassphraseBeforeSigning() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "10000100 ");
        fields.put("merchant_key", " 46f0cd694581a");
        fields.put("return_url", " https://edurite.net/payments/payfast/return  ");

        var result = signatureService.generate(fields, " payfast \n");

        assertThat(result.orderedFields())
                .containsEntry("merchant_id", "10000100")
                .containsEntry("merchant_key", "46f0cd694581a")
                .containsEntry("return_url", "https://edurite.net/payments/payfast/return");
        assertThat(result.signatureInput()).contains("&passphrase=payfast");
    }

    @Test
    void localhostAndLoopbackUrlsSignConsistently() {
        LinkedHashMap<String, String> localhostPayload = new LinkedHashMap<>();
        localhostPayload.put("return_url", "http://localhost:5173/payments/payfast/return");
        localhostPayload.put("notify_url", "http://localhost:8080/api/payments/payfast/notify");

        LinkedHashMap<String, String> loopbackPayload = new LinkedHashMap<>();
        loopbackPayload.put("return_url", "http://127.0.0.1:5173/payments/payfast/return");
        loopbackPayload.put("notify_url", "http://127.0.0.1:8080/api/payments/payfast/notify");

        String localhostSignature = signatureService.generate(localhostPayload, "pass").signature();
        String localhostSignatureAgain = signatureService.generate(localhostPayload, "pass").signature();
        String loopbackSignature = signatureService.generate(loopbackPayload, "pass").signature();

        assertThat(localhostSignature).isEqualTo(localhostSignatureAgain);
        assertThat(localhostSignature).isNotEqualTo(loopbackSignature);
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

