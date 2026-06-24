package com.edurite.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.dto.SubscriptionCheckoutRequest;
import com.edurite.subscription.dto.SubscriptionPaymentCancelRequest;
import com.edurite.subscription.dto.SubscriptionPaymentConfirmRequest;
import com.edurite.subscription.entity.PaymentRecord;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.entity.PricingPlan;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.payment.PayFastConfig;
import com.edurite.subscription.payment.PaymentGatewayProperties;
import com.edurite.subscription.payment.provider.PaymentCheckoutResult;
import com.edurite.subscription.payment.provider.PaymentConfirmationResult;
import com.edurite.subscription.payment.provider.PaymentProvider;
import com.edurite.subscription.payment.provider.PaymentProviderFactory;
import com.edurite.subscription.payment.provider.PaymentWebhookResult;
import com.edurite.subscription.repository.PaymentEventRepository;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.PricingPlanRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.subscription.service.SubscriptionService;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionServiceTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentEventRepository paymentEventRepository = mock(PaymentEventRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PricingPlanRepository pricingPlanRepository = mock(PricingPlanRepository.class);
    private final PaymentProviderFactory paymentProviderFactory = mock(PaymentProviderFactory.class);
    private final PaymentProvider paymentProvider = mock(PaymentProvider.class);
    private final StudentPlanAccessService studentPlanAccessService = mock(StudentPlanAccessService.class);

    private SubscriptionService subscriptionService;
    private User user;
    private Principal principal;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                paymentRepository,
                paymentEventRepository,
                currentUserService,
                notificationService,
                userRepository,
                pricingPlanRepository,
                paymentProviderFactory,
                new PaymentGatewayProperties("payfast", "http://localhost:8080/api/payments/callback", "", "", ""),
                new PayFastConfig(
                        "10000100",
                        "46f0cd694581a",
                        "passphrase",
                        true,
                        false,
                        "https://sandbox.payfast.co.za/eng/process",
                        "https://sandbox.payfast.co.za/eng/query/validate",
                        "http://localhost:8080/api/payments/payfast/return",
                        "http://localhost:8080/api/payments/payfast/cancel",
                        "http://localhost:8080/api/payments/payfast/notify"
                ),
                studentPlanAccessService,
                new ObjectMapper(),
                "http://localhost:5173",
                "http://localhost:8080"
        );

        user = new User();
        user.setId(UUID.randomUUID());
        principal = () -> user.getEmail() == null ? "student@example.com" : user.getEmail();
        when(currentUserService.requireUser(any())).thenReturn(user);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentPlanAccessService.resolveByUserId(any())).thenReturn(
                new StudentPlanAccessService.StudentPlanAccess("PLAN_BASIC", "ACTIVE", false, 3, "Upgrade to Premium")
        );

        when(subscriptionRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionRecord subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId(UUID.randomUUID());
            }
            return subscription;
        });

        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            PaymentRecord payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });

        when(pricingPlanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void checkoutThrowsWhenPlanIsUnavailable() {
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_ENTERPRISE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.checkout(principal, new SubscriptionCheckoutRequest("PLAN_ENTERPRISE", "payfast")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Subscription plan is not available");
    }

    @Test
    void checkoutCreatesProviderCheckoutForPaidPlan() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord existing = existingSubscription(user.getId());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(existing));
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.createCheckout(any())).thenReturn(PaymentCheckoutResult.pending(
                "payfast",
                "ORDER-123",
                "SESSION-123",
                "https://sandbox.payfast.co.za/eng/process",
                java.util.Map.of("mode", "subscription")
        ));

        var response = subscriptionService.checkout(principal, new SubscriptionCheckoutRequest("PLAN_PREMIUM", "payfast"));

        assertThat(response.provider()).isEqualTo("payfast");
        assertThat(response.paymentStatus()).isEqualTo("PENDING");
        assertThat(response.subscriptionStatus()).isEqualTo("PENDING");
        assertThat(response.checkoutUrl()).isEqualTo("https://sandbox.payfast.co.za/eng/process");

        verify(paymentProviderFactory).resolve("payfast");
        verify(paymentProvider).createCheckout(any());
        assertThat(existing.getStatus()).isEqualTo("PENDING");
        assertThat(existing.getPlanCode()).isEqualTo("PLAN_PREMIUM");
    }

    @Test
    void confirmCheckoutActivatesSubscriptionWhenProviderReportsCompleted() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-REF-1");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(plan.getAmount());
        payment.setCurrency(plan.getCurrency());

        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-REF-1")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));
        when(paymentProvider.confirmPayment(any())).thenReturn(PaymentConfirmationResult.completed(
                "payfast",
                "ORDER-123",
                "SESSION-123",
                "PAYMENT-123",
                "SUB-123",
                java.util.Map.of("status", "completed")
        ));

        var response = subscriptionService.confirmCheckout(principal, new SubscriptionPaymentConfirmRequest(
                "PAY-REF-1",
                "payfast",
                "SESSION-123",
                "ORDER-123",
                null,
                null
        ));

        assertThat(response.paymentStatus()).isEqualTo("COMPLETED");
        assertThat(response.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        assertThat(subscription.getProvider()).isEqualTo("payfast");
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("SUB-123");
        assertThat(subscription.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(subscription.getEndDate()).isEqualTo(LocalDate.now().plusMonths(1));

        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        assertThat(payment.getProviderOrderId()).isEqualTo("ORDER-123");
        assertThat(payment.getProviderSessionId()).isEqualTo("SESSION-123");
        assertThat(payment.getProviderPaymentId()).isEqualTo("PAYMENT-123");
        assertThat(payment.getProviderSubscriptionId()).isEqualTo("SUB-123");
        assertThat(payment.getConfirmedAt()).isNotNull();
    }

    @Test
    void cancelCheckoutKeepsSubscriptionInactive() {
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-CANCEL-1");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setConfirmedAt(null);

        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-CANCEL-1")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        var response = subscriptionService.cancelCheckout(principal, new SubscriptionPaymentCancelRequest(
                "PAY-CANCEL-1",
                "payfast",
                "User closed checkout"
        ));

        assertThat(response.paymentStatus()).isEqualTo("CANCELLED");
        assertThat(response.subscriptionStatus()).isEqualTo("CANCELLED");
        assertThat(payment.getStatus()).isEqualTo("CANCELLED");
        assertThat(payment.getFailureReason()).isEqualTo("User closed checkout");
        assertThat(payment.getConfirmedAt()).isNotNull();
        assertThat(subscription.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void checkoutFailureMarksPaymentAndSubscriptionAsFailed() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord existing = existingSubscription(user.getId());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(existing));
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.createCheckout(any())).thenThrow(new RuntimeException("PayFast is temporarily unavailable"));

        assertThatThrownBy(() -> subscriptionService.checkout(principal, new SubscriptionCheckoutRequest("PLAN_PREMIUM", "payfast")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("temporarily unavailable");

        ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRepository, atLeast(1)).save(paymentCaptor.capture());
        PaymentRecord last = paymentCaptor.getAllValues().get(paymentCaptor.getAllValues().size() - 1);
        assertThat(last.getStatus()).isEqualTo("FAILED");
        assertThat(last.getFailureReason()).contains("temporarily unavailable");
        assertThat(existing.getStatus()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void plansNormalizesPremiumPriceTo49_99() {
        PricingPlan premium = premiumPlan();
        premium.setAmount(new BigDecimal("35.00"));
        when(pricingPlanRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(premium));

        var plans = subscriptionService.plans();

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).amount()).isEqualByComparingTo("49.99");
        verify(pricingPlanRepository).save(premium);
    }

    @Test
    void handleProviderWebhookCompleteItnActivatesPremium() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-ITN-1");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("ZAR");

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", "10000100");
        payload.put("m_payment_id", "PAY-ITN-1");
        payload.put("pf_payment_id", "PF-123");
        payload.put("payment_status", "COMPLETE");
        payload.put("amount_gross", "49.99");
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-123",
                "PAYFAST_ITN",
                "PAY-ITN-1",
                "COMPLETED",
                true,
                payload
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-123")).thenReturn(Optional.empty());
        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-ITN-1")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("verified", true);
        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getPlanType()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void handleProviderWebhookInvalidSignatureDoesNotActivatePremium() {
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-ITN-2");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("ZAR");

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", "10000100");
        payload.put("m_payment_id", "PAY-ITN-2");
        payload.put("pf_payment_id", "PF-234");
        payload.put("payment_status", "COMPLETE");
        payload.put("amount_gross", "49.99");
        payload.put("signature_valid", false);
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-234",
                "PAYFAST_ITN",
                "PAY-ITN-2",
                "COMPLETED",
                false,
                payload
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-234")).thenReturn(Optional.empty());
        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-ITN-2")).thenReturn(Optional.of(payment));

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("verified", false);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(user.getPlanType()).isNotEqualTo(PlanType.PREMIUM);
    }

    @Test
    void handleProviderWebhookAmountMismatchDoesNotActivatePremium() {
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-ITN-3");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("ZAR");

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", "10000100");
        payload.put("m_payment_id", "PAY-ITN-3");
        payload.put("pf_payment_id", "PF-345");
        payload.put("payment_status", "COMPLETE");
        payload.put("amount_gross", "10.00");
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-345",
                "PAYFAST_ITN",
                "PAY-ITN-3",
                "COMPLETED",
                true,
                payload
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-345")).thenReturn(Optional.empty());
        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-ITN-3")).thenReturn(Optional.of(payment));

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("verified", false);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(user.getPlanType()).isNotEqualTo(PlanType.PREMIUM);
    }

    @Test
    void handleProviderWebhookDuplicateItnIsIgnoredSafely() {
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-456",
                "PAYFAST_ITN",
                "PAY-ITN-4",
                "COMPLETED",
                true,
                Map.of()
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-456"))
                .thenReturn(Optional.of(new com.edurite.subscription.entity.PaymentEventRecord()));

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("duplicate", true);
    }

    @Test
    void handleProviderWebhookNonCompleteStatusDoesNotActivatePremium() {
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-ITN-5");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("ZAR");

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", "10000100");
        payload.put("m_payment_id", "PAY-ITN-5");
        payload.put("pf_payment_id", "PF-567");
        payload.put("payment_status", "PENDING");
        payload.put("amount_gross", "49.99");
        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-567",
                "PAYFAST_ITN",
                "PAY-ITN-5",
                "PENDING",
                true,
                payload
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-567")).thenReturn(Optional.empty());
        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-ITN-5")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("verified", true);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(subscription.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void handleProviderWebhookUnknownReferenceIsRejectedSafely() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchant_id", "10000100");
        payload.put("m_payment_id", "UNKNOWN");
        payload.put("pf_payment_id", "PF-678");
        payload.put("payment_status", "COMPLETE");
        payload.put("amount_gross", "49.99");

        when(paymentProviderFactory.resolve("payfast")).thenReturn(paymentProvider);
        when(paymentProvider.handleWebhook(any(), any())).thenReturn(new PaymentWebhookResult(
                "payfast",
                "PF-678",
                "PAYFAST_ITN",
                "UNKNOWN",
                "COMPLETED",
                true,
                payload
        ));
        when(paymentEventRepository.findByProviderAndEventId("payfast", "PF-678")).thenReturn(Optional.empty());
        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("UNKNOWN")).thenReturn(Optional.empty());

        var response = subscriptionService.handleProviderWebhook("payfast", java.util.Map.of(), "raw");

        assertThat(response).containsEntry("processed", true);
        assertThat(response).containsEntry("verified", true);
    }

    @Test
    void verifyPaymentActivatesFromLatestVerifiedPayFastWebhookEvent() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-MANUAL-ITN-1");
        payment.setProvider("payfast");
        payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("ZAR");

        com.edurite.subscription.entity.PaymentEventRecord event = new com.edurite.subscription.entity.PaymentEventRecord();
        event.setEventId("PF-MANUAL-1");
        event.setProvider("payfast");
        event.setStatus("COMPLETED");
        event.setVerified(true);

        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-MANUAL-ITN-1")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentEventRepository.findTopByPaymentIdAndProviderAndVerifiedTrueOrderByProcessedAtDesc(payment.getId(), "payfast"))
                .thenReturn(Optional.of(event));
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));

        var response = subscriptionService.verifyPayment(principal, "PAY-MANUAL-ITN-1");

        assertThat(response.paymentStatus()).isEqualTo("COMPLETED");
        assertThat(response.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        verify(paymentProviderFactory, never()).resolve("payfast");
    }

    @Test
    void verifyPaymentUsesProviderConfirmationWhenNoVerifiedPayFastEventExists() {
        PricingPlan plan = premiumPlan();
        SubscriptionRecord subscription = existingSubscription(user.getId());
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setStatus("PENDING");

        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setSubscriptionId(subscription.getId());
        payment.setReference("PAY-MANUAL-PP-1");
        payment.setProvider("paypal");
        payment.setProviderOrderId("ORDER-777");
        payment.setStatus("PENDING");
        payment.setAmount(plan.getAmount());
        payment.setCurrency(plan.getCurrency());

        when(paymentRepository.findTopByReferenceOrderByCreatedAtDesc("PAY-MANUAL-PP-1")).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentProviderFactory.resolve("paypal")).thenReturn(paymentProvider);
        when(paymentProvider.confirmPayment(any())).thenReturn(PaymentConfirmationResult.completed(
                "paypal",
                "ORDER-777",
                null,
                "CAPTURE-777",
                null,
                Map.of("status", "COMPLETED")
        ));
        when(pricingPlanRepository.findByCodeAndActiveTrue("PLAN_PREMIUM")).thenReturn(Optional.of(plan));

        var response = subscriptionService.verifyPayment(principal, "PAY-MANUAL-PP-1");

        assertThat(response.paymentStatus()).isEqualTo("COMPLETED");
        assertThat(response.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        assertThat(payment.getProviderPaymentId()).isEqualTo("CAPTURE-777");
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
    }

    private PricingPlan premiumPlan() {
        PricingPlan plan = new PricingPlan();
        plan.setId(UUID.randomUUID());
        plan.setCode("PLAN_PREMIUM");
        plan.setName("Premium");
        plan.setDescription("Advanced guidance");
        plan.setAmount(new BigDecimal("49.99"));
        plan.setCurrency("ZAR");
        plan.setBillingInterval("MONTHLY");
        plan.setFeatures("[]");
        plan.setActive(true);
        plan.setPremium(true);
        return plan;
    }

    private SubscriptionRecord existingSubscription(UUID userId) {
        SubscriptionRecord record = new SubscriptionRecord();
        record.setId(UUID.randomUUID());
        record.setUserId(userId);
        record.setPlanCode("PLAN_BASIC");
        record.setStatus("ACTIVE");
        record.setStartDate(LocalDate.now().minusDays(2));
        record.setEndDate(LocalDate.now().plusMonths(1));
        record.setRenewalDate(LocalDate.now().plusMonths(1));
        record.setLastPaymentAt(OffsetDateTime.now().minusDays(2));
        return record;
    }
}

