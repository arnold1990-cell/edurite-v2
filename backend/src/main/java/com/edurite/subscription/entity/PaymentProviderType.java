package com.edurite.subscription.entity;

import java.util.Locale;

public enum PaymentProviderType {
    PAYPAL("paypal"),
    PAYFAST("payfast");

    private final String code;

    PaymentProviderType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static boolean isPaidProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return false;
        }
        String normalized = providerCode.trim().toLowerCase(Locale.ROOT);
        for (PaymentProviderType value : values()) {
            if (value.code.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}

