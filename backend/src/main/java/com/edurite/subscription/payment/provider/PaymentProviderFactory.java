package com.edurite.subscription.payment.provider;

import com.edurite.common.exception.ResourceConflictException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderFactory {

    private final Map<String, PaymentProvider> providers;

    public PaymentProviderFactory(List<PaymentProvider> providerImplementations) {
        this.providers = providerImplementations.stream()
                .collect(Collectors.toMap(
                        provider -> normalize(provider.providerCode()),
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    public PaymentProvider resolve(String providerCode) {
        String normalized = normalize(providerCode);
        PaymentProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new ResourceConflictException("Unsupported payment provider: " + providerCode);
        }
        return provider;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

