package com.edurite.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.subscription.payment.provider.PaymentCheckoutContext;
import com.edurite.subscription.payment.provider.PaymentCheckoutResult;
import com.edurite.subscription.payment.provider.PaymentConfirmationContext;
import com.edurite.subscription.payment.provider.PaymentConfirmationResult;
import com.edurite.subscription.payment.provider.PaymentProvider;
import com.edurite.subscription.payment.provider.PaymentProviderFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentProviderFactoryTest {

    @Test
    void resolveIsCaseInsensitive() {
        PaymentProviderFactory factory = new PaymentProviderFactory(List.of(new StubProvider("payfast"), new StubProvider("mock")));

        PaymentProvider payFast = factory.resolve("PAYFAST");
        PaymentProvider mock = factory.resolve("Mock");

        assertThat(payFast.providerCode()).isEqualTo("payfast");
        assertThat(mock.providerCode()).isEqualTo("mock");
    }

    @Test
    void resolveThrowsForUnsupportedProvider() {
        PaymentProviderFactory factory = new PaymentProviderFactory(List.of(new StubProvider("mock")));

        assertThatThrownBy(() -> factory.resolve("bank"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Unsupported payment provider");
    }

    private record StubProvider(String providerCode) implements PaymentProvider {
        @Override
        public PaymentCheckoutResult createCheckout(PaymentCheckoutContext context) {
            return PaymentCheckoutResult.pending(providerCode, "ORDER", "SESSION", "https://checkout.local", Map.of());
        }

        @Override
        public PaymentConfirmationResult confirmPayment(PaymentConfirmationContext context) {
            return PaymentConfirmationResult.completed(providerCode, "ORDER", "SESSION", "PAYMENT", "SUB", Map.of());
        }
    }
}

