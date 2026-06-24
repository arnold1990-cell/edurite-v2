package com.edurite.subscription.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PayFastSignatureService {

    private static final Pattern LOWERCASE_PERCENT_ENCODING = Pattern.compile("%[0-9a-f]{2}");

    public PayFastSignatureResult generate(Map<String, String> fields, String passphrase) {
        LinkedHashMap<String, String> orderedFields = sanitize(fields);
        String signatureInput = buildSignatureInput(orderedFields, passphrase);
        return new PayFastSignatureResult(
                Collections.unmodifiableMap(new LinkedHashMap<>(orderedFields)),
                signatureInput,
                md5Hex(signatureInput)
        );
    }

    private LinkedHashMap<String, String> sanitize(Map<String, String> fields) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        if (fields == null || fields.isEmpty()) {
            return ordered;
        }

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (key.isBlank() || "signature".equalsIgnoreCase(key)) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.isBlank()) {
                continue;
            }
            ordered.put(key, value);
        }
        return ordered;
    }

    private String buildSignatureInput(LinkedHashMap<String, String> fields, String passphrase) {
        String joined = fields.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        if (passphrase != null && !passphrase.isBlank()) {
            String delimiter = joined.isEmpty() ? "" : "&";
            return joined + delimiter + "passphrase=" + encode(passphrase.trim());
        }
        return joined;
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate PayFast MD5 signature.", ex);
        }
    }

    private String encode(String value) {
        String encoded = java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
        // PayFast expects uppercase URL-encoded hex pairs, and '+' for spaces.
        Matcher matcher = LOWERCASE_PERCENT_ENCODING.matcher(encoded);
        StringBuilder sb = new StringBuilder(encoded.length());
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().toUpperCase(Locale.ROOT));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public record PayFastSignatureResult(
            Map<String, String> orderedFields,
            String signatureInput,
            String signature
    ) {
    }
}

