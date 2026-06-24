package com.edurite.subscription.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.payment.payfast")
public record PayFastConfig(
        String merchantId,
        String merchantKey,
        String passphrase,
        Boolean sandbox,
        Boolean debugSignature,
        String processUrl,
        String validateUrl,
        String returnUrl,
        String cancelUrl,
        String notifyUrl
) {
    public boolean sandboxEnabled() {
        return Boolean.TRUE.equals(sandbox);
    }

    public boolean debugSignatureEnabled() {
        return Boolean.TRUE.equals(debugSignature);
    }
}

