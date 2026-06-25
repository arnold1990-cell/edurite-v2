package com.edurite.subscription.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.dto.PricingPlanDto;
import com.edurite.subscription.dto.SubscriptionCheckoutRequest;
import com.edurite.subscription.dto.SubscriptionCheckoutResponse;
import com.edurite.subscription.dto.SubscriptionPaymentCancelRequest;
import com.edurite.subscription.dto.SubscriptionPaymentConfirmRequest;
import com.edurite.subscription.dto.SubscriptionPaymentStatusResponse;
import com.edurite.subscription.entity.PaymentEventRecord;
import com.edurite.subscription.entity.PaymentProviderType;
import com.edurite.subscription.entity.PaymentRecord;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.entity.PricingPlan;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.payment.PayFastConfig;
import com.edurite.subscription.payment.PaymentGatewayProperties;
import com.edurite.subscription.payment.provider.PaymentCheckoutContext;
import com.edurite.subscription.payment.provider.PaymentCheckoutResult;
import com.edurite.subscription.payment.provider.PaymentConfirmationContext;
import com.edurite.subscription.payment.provider.PaymentConfirmationResult;
import com.edurite.subscription.payment.provider.PaymentProvider;
import com.edurite.subscription.payment.provider.PaymentProviderFactory;
import com.edurite.subscription.payment.provider.PaymentWebhookResult;
import com.edurite.subscription.repository.PaymentEventRepository;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.PricingPlanRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final String PLAN_BASIC = "PLAN_BASIC";
    private static final String PLAN_PREMIUM = "PLAN_PREMIUM";
    private static final BigDecimal PREMIUM_MONTHLY_PRICE = new BigDecimal("49.99");
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAYMENT_FAILED = "PAYMENT_FAILED";
    private static final String PROVIDER_PAYPAL = "paypal";
    private static final String PROVIDER_PAYFAST = "payfast";
    private static final String PROVIDER_INTERNAL = "internal";
    private static final String PROVIDER_MOCK = "mock";
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:5173";
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final PaymentProviderFactory paymentProviderFactory;
    private final PaymentGatewayProperties paymentGatewayProperties;
    private final PayFastConfig payFastConfig;
    private final ObjectMapper objectMapper;
    private final String frontendUrl;
    private final String backendBaseUrl;
    private final StudentPlanAccessService studentPlanAccessService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            UserRepository userRepository,
            PricingPlanRepository pricingPlanRepository,
            PaymentProviderFactory paymentProviderFactory,
            PaymentGatewayProperties paymentGatewayProperties,
            PayFastConfig payFastConfig,
            StudentPlanAccessService studentPlanAccessService,
            ObjectMapper objectMapper,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.backend-base-url:http://localhost:8080}") String backendBaseUrl
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.paymentProviderFactory = paymentProviderFactory;
        this.paymentGatewayProperties = paymentGatewayProperties;
        this.payFastConfig = payFastConfig;
        this.studentPlanAccessService = studentPlanAccessService;
        this.objectMapper = objectMapper;
        this.frontendUrl = frontendUrl;
        this.backendBaseUrl = backendBaseUrl;
    }

    public SubscriptionRecord current(Principal principal) {
        User user = currentUserService.requireUser(principal);
        SubscriptionRecord subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseGet(() -> createDefaultSubscription(user.getId()));
        subscription = ensurePermanentPremiumOverride(subscription);
        return decorateSubscriptionAccess(subscription);
    }

    @Transactional
    public SubscriptionRecord initializeStudentTrialIfAbsent(UUID userId) {
        SubscriptionRecord existing = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        if (existing != null) {
            return ensurePermanentPremiumOverride(existing);
        }

        if (studentPlanAccessService.isPermanentPremiumOverride(userId)) {
            return createDefaultSubscription(userId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEnd = now.plusMonths(1);

        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setUserId(userId);
        subscription.setPlanCode(PLAN_BASIC);
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setProvider(PROVIDER_INTERNAL);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setRenewalDate(subscription.getEndDate());
        subscription.setTrialStartDate(now);
        subscription.setTrialEndDate(trialEnd);
        subscription.setPremiumUntil(trialEnd);
        subscription.setTrialUsed(true);
        SubscriptionRecord saved = subscriptionRepository.save(subscription);
        log.info("Initialized one-time student premium trial: userId={}, trialStart={}, trialEnd={}",
                userId, saved.getTrialStartDate(), saved.getTrialEndDate());
        return saved;
    }

    public List<PricingPlanDto> plans() {
        return pricingPlanRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::normalizePlanAmountIfNeeded)
                .map(plan -> new PricingPlanDto(
                        plan.getId(),
                        plan.getCode(),
                        plan.getCode(),
                        plan.getName(),
                        plan.getDescription(),
                        plan.getCurrency(),
                        plan.getAmount(),
                        plan.getAmount(),
                        plan.getBillingInterval(),
                        plan.getBillingInterval(),
                        plan.isPremium(),
                        plan.isPremium(),
                        parseFeatures(plan.getFeatures())
                ))
                .toList();
    }

    @Transactional
    public SubscriptionCheckoutResponse checkout(Principal principal, SubscriptionCheckoutRequest request) {
        return checkout(principal, request.planCode(), request.provider());
    }

    @Transactional
    public SubscriptionCheckoutResponse checkout(Principal principal, String rawPlanCode, String rawProviderCode) {
        User user = currentUserService.requireUser(principal);
        PricingPlan plan = resolvePlan(rawPlanCode);
        String providerCode = normalizeProviderCode(rawProviderCode);
        if (plan.getAmount() != null
                && plan.getAmount().compareTo(BigDecimal.ZERO) > 0
                && !PaymentProviderType.isPaidProvider(providerCode)) {
            throw new ResourceConflictException("Paid plans require either PayPal or PayFast.");
        }

        SubscriptionRecord subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseGet(() -> createDefaultSubscription(user.getId()));
        subscription.setPlanCode(plan.getCode());
        subscription.setProvider(providerCode);
        subscription.setStatus(STATUS_PENDING);
        subscriptionRepository.save(subscription);

        if (plan.getAmount() == null || plan.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            PaymentRecord payment = createPaymentRecord(subscription, plan, "internal");
            payment.setStatus(STATUS_COMPLETED);
            payment.setProviderPaymentId("INTERNAL-" + UUID.randomUUID());
            payment.setConfirmedAt(OffsetDateTime.now());
            payment.setPaidAt(OffsetDateTime.now());
            payment.setMetadata(serializeMetadata(Map.of("event", "FREE_PLAN_ACTIVATED", "provider", "internal")));
            paymentRepository.save(payment);

            activateSubscription(subscription, payment, plan.getBillingInterval(), null);
            subscriptionRepository.save(subscription);
            notifySubscriptionStatus(subscription, STATUS_COMPLETED);

            return new SubscriptionCheckoutResponse(
                    payment.getReference(),
                    PROVIDER_INTERNAL,
                    payment.getStatus(),
                    subscription.getStatus(),
                    null,
                    "Subscription updated successfully.",
                    Map.of()
            );
        }

        PaymentProvider provider = paymentProviderFactory.resolve(providerCode);
        PaymentRecord payment = createPaymentRecord(subscription, plan, provider.providerCode());
        paymentRepository.save(payment);
        log.info(
                "Subscription checkout initiated: userId={}, planCode={}, provider={}, paymentReference={}, amount={}, currency={}",
                user.getId(),
                plan.getCode(),
                provider.providerCode(),
                payment.getReference(),
                plan.getAmount(),
                normalizeCurrency(plan.getCurrency())
        );

        PaymentCheckoutResult checkoutResult;
        try {
            checkoutResult = provider.createCheckout(new PaymentCheckoutContext(
                    user.getId(),
                    payment.getReference(),
                    plan.getCode(),
                    plan.getName(),
                    plan.getBillingInterval(),
                    plan.getAmount(),
                    normalizeCurrency(plan.getCurrency()),
                    "EduRite " + plan.getName() + " subscription",
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    buildSuccessUrl(payment.getReference(), provider.providerCode()),
                    buildCancelUrl(payment.getReference(), provider.providerCode()),
                    buildNotifyUrl(payment.getReference(), provider.providerCode())
            ));
        } catch (RuntimeException ex) {
            payment.setStatus(STATUS_FAILED);
            payment.setFailureReason(ex.getMessage());
            payment.setConfirmedAt(OffsetDateTime.now());
            payment.setMetadata(serializeMetadata(Map.of("event", "CHECKOUT_FAILED", "reason", safe(ex.getMessage()))));
            paymentRepository.save(payment);

            subscription.setPaymentReference(payment.getReference());
            subscription.setStatus(toSubscriptionStatus(payment.getStatus()));
            subscriptionRepository.save(subscription);
            notifySubscriptionStatus(subscription, payment.getStatus());
            throw ex;
        }

        applyCheckoutResult(subscription, payment, plan, checkoutResult);
        log.info(
                "Subscription checkout response: paymentReference={}, provider={}, paymentStatus={}, subscriptionStatus={}",
                payment.getReference(),
                payment.getProvider(),
                payment.getStatus(),
                subscription.getStatus()
        );
        paymentRepository.save(payment);
        subscriptionRepository.save(subscription);
        notifySubscriptionStatus(subscription, payment.getStatus());

        return new SubscriptionCheckoutResponse(
                payment.getReference(),
                payment.getProvider(),
                payment.getStatus(),
                subscription.getStatus(),
                payment.getCheckoutUrl(),
                checkoutMessage(payment),
                checkoutResult == null || checkoutResult.rawResponse() == null ? Map.of() : checkoutResult.rawResponse()
        );
    }

    @Transactional
    public SubscriptionPaymentStatusResponse confirmCheckout(Principal principal, SubscriptionPaymentConfirmRequest request) {
        User user = currentUserService.requireUser(principal);
        PaymentSubscriptionContext context = findPaymentByReferenceForUser(request.paymentReference(), user.getId());
        PaymentRecord payment = context.payment();
        SubscriptionRecord subscription = context.subscription();

        if (isTerminal(payment.getStatus()) && payment.getConfirmedAt() != null) {
            return toPaymentStatusResponse(payment, subscription, "Payment was already processed.");
        }

        String providerCode = normalizeProviderCode(firstNonBlank(request.provider(), payment.getProvider()));
        PaymentProvider provider = paymentProviderFactory.resolve(providerCode);

        PaymentConfirmationResult confirmationResult = provider.confirmPayment(new PaymentConfirmationContext(
                payment.getReference(),
                firstNonBlank(request.orderId(), payment.getProviderOrderId()),
                firstNonBlank(request.sessionId(), payment.getProviderSessionId()),
                payment.getProviderPaymentId(),
                request.token(),
                request.payerId(),
                Map.of(
                        "paymentReference", payment.getReference(),
                        "orderId", safe(request.orderId()),
                        "session_id", safe(request.sessionId()),
                        "token", safe(request.token()),
                        "payerId", safe(request.payerId())
                )
        ));

        log.info(
                "Interactive payment confirmation received: paymentReference={}, provider={}, providerOrderId={}, providerSessionId={}",
                payment.getReference(),
                providerCode,
                firstNonBlank(request.orderId(), payment.getProviderOrderId()),
                firstNonBlank(request.sessionId(), payment.getProviderSessionId())
        );
        applyConfirmationResult(subscription, payment, confirmationResult, true);
        paymentRepository.save(payment);
        subscriptionRepository.save(subscription);
        notifySubscriptionStatus(subscription, payment.getStatus());

        return toPaymentStatusResponse(payment, subscription, confirmationMessage(payment));
    }

    @Transactional
    public SubscriptionPaymentStatusResponse cancelCheckout(Principal principal, SubscriptionPaymentCancelRequest request) {
        User user = currentUserService.requireUser(principal);
        PaymentSubscriptionContext context = findPaymentByReferenceForUser(request.paymentReference(), user.getId());
        PaymentRecord payment = context.payment();
        SubscriptionRecord subscription = context.subscription();

        if (STATUS_COMPLETED.equals(payment.getStatus())) {
            return toPaymentStatusResponse(payment, subscription, "Payment already completed. Cancellation ignored.");
        }

        payment.setStatus(STATUS_CANCELLED);
        payment.setFailureReason(firstNonBlank(request.reason(), "Checkout cancelled by user."));
        payment.setConfirmedAt(OffsetDateTime.now());
        payment.setCallbackReceivedAt(OffsetDateTime.now());
        payment.setMetadata(serializeMetadata(Map.of(
                "event", "CHECKOUT_CANCELLED",
                "reason", payment.getFailureReason()
        )));

        subscription.setStatus(toSubscriptionStatus(STATUS_CANCELLED));
        subscription.setPaymentReference(payment.getReference());

        paymentRepository.save(payment);
        subscriptionRepository.save(subscription);
        notifySubscriptionStatus(subscription, payment.getStatus());
        return toPaymentStatusResponse(payment, subscription, "Checkout cancelled.");
    }

    @Transactional
    public SubscriptionPaymentStatusResponse paymentStatus(Principal principal, String paymentReference) {
        User user = currentUserService.requireUser(principal);
        PaymentSubscriptionContext context = findPaymentByReferenceForUser(paymentReference, user.getId());
        PaymentRecord payment = context.payment();
        SubscriptionRecord subscription = context.subscription();

        if (STATUS_PENDING.equals(payment.getStatus()) && PROVIDER_PAYFAST.equals(normalizeProviderCode(payment.getProvider()))) {
            // Reconcile on read in case a verified ITN was persisted but status transition was missed.
            if (applyLatestVerifiedWebhookStatusIfApplicable(subscription, payment, false)) {
                paymentRepository.save(payment);
                subscriptionRepository.save(subscription);
                notifySubscriptionStatus(subscription, payment.getStatus());
            }
        }

        return toPaymentStatusResponse(payment, subscription, confirmationMessage(payment));
    }

    @Transactional
    public SubscriptionPaymentStatusResponse verifyPayment(Principal principal, String paymentReference) {
        User user = currentUserService.requireUser(principal);
        PaymentSubscriptionContext context = findPaymentByReferenceForUser(paymentReference, user.getId());
        PaymentRecord payment = context.payment();
        SubscriptionRecord subscription = context.subscription();

        if (STATUS_COMPLETED.equals(payment.getStatus()) && STATUS_ACTIVE.equals(subscription.getStatus())) {
            return toPaymentStatusResponse(payment, subscription, "Payment already verified. Subscription is active.");
        }

        if (applyLatestVerifiedWebhookStatusIfApplicable(subscription, payment, true)) {
            paymentRepository.save(payment);
            subscriptionRepository.save(subscription);
            notifySubscriptionStatus(subscription, payment.getStatus());
            return toPaymentStatusResponse(payment, subscription, confirmationMessage(payment));
        }

        String providerCode = normalizeProviderCode(payment.getProvider());
        PaymentProvider provider = paymentProviderFactory.resolve(providerCode);
        PaymentConfirmationResult confirmationResult = provider.confirmPayment(new PaymentConfirmationContext(
                payment.getReference(),
                payment.getProviderOrderId(),
                payment.getProviderSessionId(),
                payment.getProviderPaymentId(),
                null,
                null,
                Map.of("paymentReference", payment.getReference())
        ));

        log.info(
                "Manual payment verification executed: paymentReference={}, provider={}, currentStatus={}, providerStatus={}",
                payment.getReference(),
                providerCode,
                payment.getStatus(),
                confirmationResult.status()
        );
        applyConfirmationResult(subscription, payment, confirmationResult, false);
        payment.setCallbackReceivedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
        subscriptionRepository.save(subscription);
        notifySubscriptionStatus(subscription, payment.getStatus());

        if (!STATUS_COMPLETED.equals(payment.getStatus())) {
            log.warn(
                    "Manual payment verification did not activate subscription: paymentReference={}, provider={}, paymentStatus={}, subscriptionStatus={}",
                    payment.getReference(),
                    providerCode,
                    payment.getStatus(),
                    subscription.getStatus()
            );
        }

        return toPaymentStatusResponse(payment, subscription, confirmationMessage(payment));
    }

    // Backward-compatible endpoint flow used by older clients.
    @Transactional
    public Map<String, Object> purchase(Principal principal, String planCode) {
        SubscriptionCheckoutResponse response = checkout(
                principal,
                resolvePlanCode(planCode),
                configuredProvider()
        );
        SubscriptionRecord subscription = current(principal);
        PaymentRecord payment = paymentRepository.findTopByReferenceOrderByCreatedAtDesc(response.paymentReference()).orElse(null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscription", subscription);
        payload.put("payment", payment);
        payload.put("paymentGateway", response.provider());
        payload.put("callbackUrl", resolveCallbackUrl());
        payload.put("checkoutUrl", response.checkoutUrl());
        payload.put("paymentStatus", response.paymentStatus());
        return payload;
    }

    @Transactional
    public Map<String, Object> handlePaymentCallback(Map<String, String> callbackPayload) {
        String providerCode = normalizeProviderCode(firstNonBlank(
                callbackPayload.get("provider"),
                callbackPayload.get("gateway"),
                configuredProvider()
        ));
        return handleProviderCallback(providerCode, callbackPayload);
    }

    @Transactional
    public Map<String, Object> handleProviderCallback(String providerCode, Map<String, String> payload) {
        String normalizedProvider = normalizeProviderCode(providerCode);
        log.info(
                "Provider callback received: provider={}, keys={}",
                normalizedProvider,
                payload == null ? List.of() : payload.keySet()
        );
        PaymentProvider provider = paymentProviderFactory.resolve(normalizedProvider);
        PaymentSubscriptionContext context = findPaymentByProviderPayload(normalizedProvider, payload);
        if (context == null) {
            log.warn("Provider callback ignored: no matching payment found. provider={}, payload={}", normalizedProvider, payload);
            return Map.of(
                    "acknowledged", true,
                    "processed", false,
                    "message", "No matching payment record for callback."
            );
        }

        PaymentRecord payment = context.payment();
        SubscriptionRecord subscription = context.subscription();
        if (isTerminal(payment.getStatus()) && payment.getConfirmedAt() != null) {
            return Map.of(
                    "acknowledged", true,
                    "processed", true,
                    "duplicate", true,
                    "paymentReference", payment.getReference(),
                    "paymentStatus", payment.getStatus(),
                    "subscriptionStatus", subscription.getStatus()
            );
        }

        String statusHint = normalizePaymentStatus(payload.get("status"));
        if (STATUS_CANCELLED.equals(statusHint)) {
            payment.setStatus(STATUS_CANCELLED);
            payment.setFailureReason(firstNonBlank(payload.get("reason"), "Checkout cancelled."));
            payment.setConfirmedAt(OffsetDateTime.now());
            payment.setCallbackReceivedAt(OffsetDateTime.now());
            payment.setMetadata(serializeMetadata(Map.of("event", "CALLBACK_CANCELLED", "payload", payload)));
            subscription.setStatus(toSubscriptionStatus(payment.getStatus()));
            subscription.setPaymentReference(payment.getReference());
            paymentRepository.save(payment);
            subscriptionRepository.save(subscription);
            notifySubscriptionStatus(subscription, payment.getStatus());
            return Map.of(
                    "acknowledged", true,
                    "processed", true,
                    "paymentReference", payment.getReference(),
                    "paymentStatus", payment.getStatus(),
                    "subscriptionStatus", subscription.getStatus()
            );
        }

        PaymentConfirmationResult confirmationResult = provider.confirmPayment(new PaymentConfirmationContext(
                payment.getReference(),
                firstNonBlank(payload.get("orderId"), payload.get("token"), payment.getProviderOrderId()),
                firstNonBlank(payload.get("session_id"), payload.get("sessionId"), payment.getProviderSessionId()),
                payment.getProviderPaymentId(),
                payload.get("token"),
                firstNonBlank(payload.get("PayerID"), payload.get("payerId")),
                payload
        ));

        applyConfirmationResult(subscription, payment, confirmationResult, false);
        payment.setCallbackReceivedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
        subscriptionRepository.save(subscription);
        notifySubscriptionStatus(subscription, payment.getStatus());
        if (!STATUS_COMPLETED.equals(payment.getStatus())) {
            log.warn(
                    "Provider callback did not complete payment: provider={}, paymentReference={}, paymentStatus={}, subscriptionStatus={}",
                    normalizedProvider,
                    payment.getReference(),
                    payment.getStatus(),
                    subscription.getStatus()
            );
        }

        return Map.of(
                "acknowledged", true,
                "processed", true,
                "paymentReference", payment.getReference(),
                "paymentStatus", payment.getStatus(),
                "subscriptionStatus", subscription.getStatus()
        );
    }

    @Transactional
    public Map<String, Object> handleProviderWebhook(String providerCode, Map<String, String> headers, String rawPayload) {
        String normalizedProvider = normalizeProviderCode(providerCode);
        PaymentProvider provider = paymentProviderFactory.resolve(normalizedProvider);
        PaymentWebhookResult webhookResult = provider.handleWebhook(headers, rawPayload);
        String eventId = firstNonBlank(webhookResult.eventId(), UUID.randomUUID().toString());

        if (paymentEventRepository.findByProviderAndEventId(normalizedProvider, eventId).isPresent()) {
            log.info("Duplicate webhook event ignored: provider={}, eventId={}", normalizedProvider, eventId);
            return Map.of(
                    "acknowledged", true,
                    "processed", false,
                    "duplicate", true,
                    "eventId", eventId
            );
        }

        PaymentRecord payment = webhookResult.paymentReference() == null
                ? null
                : paymentRepository.findTopByReferenceOrderByCreatedAtDesc(webhookResult.paymentReference()).orElse(null);
        boolean webhookVerified = webhookResult.verified();
        if (webhookVerified && PROVIDER_PAYFAST.equals(normalizedProvider) && payment != null) {
            webhookVerified = validatePayFastWebhookPayment(payment, webhookResult.payload());
        }
        log.info(
                "Payment webhook received: provider={}, eventId={}, reference={}, status={}, verifiedBeforeDomain={}, verifiedAfterDomain={}",
                normalizedProvider,
                eventId,
                webhookResult.paymentReference(),
                webhookResult.status(),
                webhookResult.verified(),
                webhookVerified
        );
        if (!webhookVerified) {
            log.warn(
                    "Payment webhook verification failed: provider={}, eventId={}, reference={}, payload={}",
                    normalizedProvider,
                    eventId,
                    webhookResult.paymentReference(),
                    webhookResult.payload()
            );
        }

        PaymentEventRecord paymentEvent = new PaymentEventRecord();
        paymentEvent.setPaymentId(payment == null ? null : payment.getId());
        paymentEvent.setProvider(normalizedProvider);
        paymentEvent.setEventId(eventId);
        paymentEvent.setEventType(firstNonBlank(webhookResult.eventType(), "UNKNOWN"));
        paymentEvent.setStatus(normalizePaymentStatus(webhookResult.status()));
        paymentEvent.setVerified(webhookVerified);
        paymentEvent.setPayload(serializeMetadata(Map.of(
                "headers", headers == null ? Map.of() : headers,
                "payload", webhookResult.payload() == null ? Map.of() : webhookResult.payload()
        )));
        paymentEvent.setProcessedAt(OffsetDateTime.now());
        paymentEventRepository.save(paymentEvent);

        if (webhookVerified && payment != null) {
            SubscriptionRecord subscription = subscriptionRepository.findById(payment.getSubscriptionId()).orElse(null);
            String webhookStatus = normalizePaymentStatus(webhookResult.status());
            if (subscription != null && shouldApplyWebhookTransition(payment, webhookStatus)) {
                applyWebhookStatus(subscription, payment, normalizePaymentStatus(webhookResult.status()));
                paymentRepository.save(payment);
                subscriptionRepository.save(subscription);
                notifySubscriptionStatus(subscription, payment.getStatus());
                log.info(
                        "Webhook status applied: provider={}, paymentReference={}, paymentStatus={}, subscriptionStatus={}",
                        normalizedProvider,
                        payment.getReference(),
                        payment.getStatus(),
                        subscription.getStatus()
                );
            }
        }

        return Map.of(
                "acknowledged", true,
                "processed", true,
                "eventId", eventId,
                "verified", webhookVerified
        );
    }

    public boolean hasPremiumAccess(Principal principal) {
        SubscriptionRecord subscription = current(principal);
        return PLAN_PREMIUM.equals(subscription.getPlanCode()) && STATUS_ACTIVE.equals(subscription.getStatus());
    }

    public String configuredProvider() {
        return normalizeProviderCode(firstNonBlank(paymentGatewayProperties.provider(), PROVIDER_PAYPAL));
    }

    private void applyCheckoutResult(
            SubscriptionRecord subscription,
            PaymentRecord payment,
            PricingPlan plan,
            PaymentCheckoutResult result
    ) {
        payment.setProvider(normalizeProviderCode(firstNonBlank(result.provider(), payment.getProvider())));
        payment.setProviderOrderId(firstNonBlank(result.externalOrderId(), payment.getProviderOrderId()));
        payment.setProviderSessionId(firstNonBlank(result.externalSessionId(), payment.getProviderSessionId()));
        payment.setProviderPaymentId(firstNonBlank(result.externalPaymentId(), payment.getProviderPaymentId()));
        payment.setProviderSubscriptionId(firstNonBlank(result.externalSubscriptionId(), payment.getProviderSubscriptionId()));
        payment.setCheckoutUrl(firstNonBlank(result.checkoutUrl(), payment.getCheckoutUrl()));
        payment.setFailureReason(result.failureReason());
        payment.setMetadata(serializeMetadata(Map.of(
                "event", "CHECKOUT_CREATED",
                "provider", payment.getProvider(),
                "response", result.rawResponse() == null ? Map.of() : result.rawResponse()
        )));

        String paymentStatus = normalizePaymentStatus(result.status());
        payment.setStatus(paymentStatus);
        subscription.setPaymentReference(payment.getReference());
        subscription.setProvider(payment.getProvider());
        subscription.setProviderSubscriptionId(firstNonBlank(result.externalSubscriptionId(), subscription.getProviderSubscriptionId()));
        subscription.setStatus(toSubscriptionStatus(paymentStatus));

        if (STATUS_COMPLETED.equals(paymentStatus)) {
            payment.setConfirmedAt(OffsetDateTime.now());
            payment.setPaidAt(OffsetDateTime.now());
            activateSubscription(subscription, payment, plan.getBillingInterval(), result.externalSubscriptionId());
        }
        if (STATUS_FAILED.equals(paymentStatus) || STATUS_CANCELLED.equals(paymentStatus)) {
            payment.setConfirmedAt(OffsetDateTime.now());
        }
    }

    private void applyConfirmationResult(
            SubscriptionRecord subscription,
            PaymentRecord payment,
            PaymentConfirmationResult result,
            boolean interactiveConfirm
    ) {
        String paymentStatus = normalizePaymentStatus(result.status());
        payment.setStatus(paymentStatus);
        payment.setProvider(normalizeProviderCode(firstNonBlank(result.provider(), payment.getProvider())));
        payment.setProviderOrderId(firstNonBlank(result.externalOrderId(), payment.getProviderOrderId()));
        payment.setProviderSessionId(firstNonBlank(result.externalSessionId(), payment.getProviderSessionId()));
        payment.setProviderPaymentId(firstNonBlank(result.externalPaymentId(), payment.getProviderPaymentId()));
        payment.setProviderSubscriptionId(firstNonBlank(result.externalSubscriptionId(), payment.getProviderSubscriptionId()));
        payment.setFailureReason(result.failureReason());
        payment.setConfirmedAt(OffsetDateTime.now());
        payment.setMetadata(serializeMetadata(Map.of(
                "event", interactiveConfirm ? "CHECKOUT_CONFIRMED" : "CALLBACK_CONFIRMED",
                "provider", payment.getProvider(),
                "response", result.rawResponse() == null ? Map.of() : result.rawResponse()
        )));

        subscription.setPaymentReference(payment.getReference());
        subscription.setProvider(payment.getProvider());
        subscription.setProviderSubscriptionId(firstNonBlank(result.externalSubscriptionId(), subscription.getProviderSubscriptionId()));
        subscription.setStatus(toSubscriptionStatus(paymentStatus));

        if (STATUS_COMPLETED.equals(paymentStatus)) {
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(OffsetDateTime.now());
            }
            PricingPlan plan = resolvePlan(subscription.getPlanCode());
            activateSubscription(subscription, payment, plan.getBillingInterval(), result.externalSubscriptionId());
            return;
        }

        if (STATUS_PENDING.equals(paymentStatus)) {
            log.info(
                    "Payment still pending verification: paymentReference={}, provider={}, interactiveConfirm={}",
                    payment.getReference(),
                    payment.getProvider(),
                    interactiveConfirm
            );
            return;
        }

        log.warn(
                "Payment confirmation failed to activate: paymentReference={}, provider={}, paymentStatus={}, reason={}",
                payment.getReference(),
                payment.getProvider(),
                paymentStatus,
                payment.getFailureReason()
        );
    }

    private void applyWebhookStatus(SubscriptionRecord subscription, PaymentRecord payment, String webhookStatus) {
        payment.setStatus(webhookStatus);
        payment.setConfirmedAt(OffsetDateTime.now());
        if (STATUS_COMPLETED.equals(webhookStatus) && payment.getPaidAt() == null) {
            payment.setPaidAt(OffsetDateTime.now());
        }
        subscription.setStatus(toSubscriptionStatus(webhookStatus));
        if (STATUS_COMPLETED.equals(webhookStatus)) {
            PricingPlan plan = resolvePlan(subscription.getPlanCode());
            activateSubscription(subscription, payment, plan.getBillingInterval(), payment.getProviderSubscriptionId());
        }
    }

    private SubscriptionRecord createDefaultSubscription(UUID userId) {
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setUserId(userId);
        if (studentPlanAccessService.isPermanentPremiumOverride(userId)) {
            applyPermanentPremiumOverride(subscription);
        } else {
            subscription.setPlanCode(PLAN_BASIC);
            subscription.setStatus(STATUS_ACTIVE);
            subscription.setProvider(PROVIDER_INTERNAL);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(LocalDate.now().plusMonths(1));
            subscription.setRenewalDate(subscription.getEndDate());
            subscription.setTrialUsed(true);
        }
        return subscriptionRepository.save(subscription);
    }

    private SubscriptionRecord decorateSubscriptionAccess(SubscriptionRecord subscription) {
        subscription = ensurePermanentPremiumOverride(subscription);
        StudentPlanAccessService.StudentPlanAccess access = studentPlanAccessService.resolveByUserId(subscription.getUserId());
        boolean trialActive = subscription.getTrialEndDate() != null && OffsetDateTime.now().isBefore(subscription.getTrialEndDate());

        subscription.setPremiumAccess(access.premium());
        subscription.setTrialActive(trialActive);
        if (trialActive && subscription.getTrialEndDate() != null) {
            subscription.setAccessMessage("You are on a free Premium trial. Trial ends on " + subscription.getTrialEndDate().toLocalDate() + ".");
        } else if (!access.premium()) {
            subscription.setAccessMessage("Your free Premium trial has ended. You are now on Basic. Subscribe to unlock Premium features.");
        } else {
            subscription.setAccessMessage(null);
        }
        return subscription;
    }

    private PaymentRecord createPaymentRecord(SubscriptionRecord subscription, PricingPlan plan, String providerCode) {
        PaymentRecord payment = new PaymentRecord();
        payment.setSubscriptionId(subscription.getId());
        payment.setAmount(plan.getAmount());
        payment.setCurrency(normalizeCurrency(plan.getCurrency()));
        payment.setStatus(STATUS_PENDING);
        payment.setProvider(normalizeProviderCode(providerCode));
        payment.setReference(generatePaymentReference());
        payment.setMetadata(serializeMetadata(Map.of(
                "event", "PAYMENT_CREATED",
                "planCode", plan.getCode(),
                "provider", normalizeProviderCode(providerCode)
        )));
        return payment;
    }

    private void activateSubscription(
            SubscriptionRecord subscription,
            PaymentRecord payment,
            String billingInterval,
            String providerSubscriptionId
    ) {
        if (studentPlanAccessService.isPermanentPremiumOverride(subscription.getUserId())) {
            applyPermanentPremiumOverride(subscription);
            subscription.setPaymentReference(payment.getReference());
            subscription.setProvider(firstNonBlank(payment.getProvider(), PROVIDER_INTERNAL));
            subscription.setProviderSubscriptionId(firstNonBlank(providerSubscriptionId, payment.getProviderSubscriptionId(), subscription.getProviderSubscriptionId()));
            subscription.setLastPaymentAt(OffsetDateTime.now());
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = calculateEndDate(today, billingInterval);
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setPaymentReference(payment.getReference());
        subscription.setProvider(payment.getProvider());
        subscription.setProviderSubscriptionId(firstNonBlank(providerSubscriptionId, payment.getProviderSubscriptionId(), subscription.getProviderSubscriptionId()));
        subscription.setStartDate(today);
        subscription.setEndDate(endDate);
        subscription.setRenewalDate(endDate);
        subscription.setLastPaymentAt(OffsetDateTime.now());
        subscription.setCancelAtPeriodEnd(false);
        log.info(
                "Subscription activated: userId={}, paymentReference={}, provider={}, planCode={}, startDate={}, endDate={}",
                subscription.getUserId(),
                payment.getReference(),
                payment.getProvider(),
                subscription.getPlanCode(),
                subscription.getStartDate(),
                subscription.getEndDate()
        );
    }

    private LocalDate calculateEndDate(LocalDate startDate, String billingInterval) {
        String normalized = billingInterval == null ? "MONTHLY" : billingInterval.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "YEARLY", "ANNUAL" -> startDate.plusYears(1);
            case "WEEKLY" -> startDate.plusWeeks(1);
            case "DAILY" -> startDate.plusDays(1);
            default -> startDate.plusMonths(1);
        };
    }

    private SubscriptionPaymentStatusResponse toPaymentStatusResponse(
            PaymentRecord payment,
            SubscriptionRecord subscription,
            String message
    ) {
        return new SubscriptionPaymentStatusResponse(
                payment.getReference(),
                payment.getProvider(),
                payment.getStatus(),
                subscription.getStatus(),
                message
        );
    }

    private String checkoutMessage(PaymentRecord payment) {
        return switch (payment.getStatus()) {
            case STATUS_PENDING -> "Checkout session created. Redirect to provider to complete payment.";
            case STATUS_COMPLETED -> "Payment completed and subscription is active.";
            case STATUS_CANCELLED -> "Checkout was cancelled.";
            case STATUS_FAILED -> firstNonBlank(payment.getFailureReason(), "Could not initialize checkout.");
            default -> "Checkout initialized.";
        };
    }

    private String confirmationMessage(PaymentRecord payment) {
        return switch (payment.getStatus()) {
            case STATUS_COMPLETED -> "Payment verified. Subscription activated.";
            case STATUS_PENDING -> "Payment is still pending.";
            case STATUS_CANCELLED -> "Payment was cancelled.";
            default -> "Payment could not be confirmed.";
        };
    }

    private PricingPlan resolvePlan(String rawPlanCode) {
        String normalizedCode = resolvePlanCode(rawPlanCode);
        return pricingPlanRepository.findByCodeAndActiveTrue(normalizedCode)
                .map(this::normalizePlanAmountIfNeeded)
                .orElseThrow(() -> new ResourceConflictException("Subscription plan is not available: " + normalizedCode));
    }

    private PricingPlan normalizePlanAmountIfNeeded(PricingPlan plan) {
        if (plan == null || plan.getCode() == null) {
            return plan;
        }
        if (!PLAN_PREMIUM.equalsIgnoreCase(plan.getCode().trim())) {
            return plan;
        }
        if (plan.getAmount() != null && plan.getAmount().compareTo(PREMIUM_MONTHLY_PRICE) == 0) {
            return plan;
        }
        plan.setAmount(PREMIUM_MONTHLY_PRICE);
        return pricingPlanRepository.save(plan);
    }

    private String resolvePlanCode(String rawPlanCode) {
        if (rawPlanCode == null || rawPlanCode.isBlank()) {
            return PLAN_BASIC;
        }
        String normalized = rawPlanCode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BASIC", PLAN_BASIC -> PLAN_BASIC;
            case "PREMIUM", PLAN_PREMIUM -> PLAN_PREMIUM;
            default -> normalized;
        };
    }

    private PaymentSubscriptionContext findPaymentByReferenceForUser(String paymentReference, UUID userId) {
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new ResourceConflictException("paymentReference is required.");
        }
        PaymentRecord payment = paymentRepository.findTopByReferenceOrderByCreatedAtDesc(paymentReference.trim())
                .orElseThrow(() -> new ResourceConflictException("Payment not found."));
        SubscriptionRecord subscription = subscriptionRepository.findById(payment.getSubscriptionId())
                .orElseThrow(() -> new ResourceConflictException("Subscription not found for payment."));
        if (!subscription.getUserId().equals(userId)) {
            throw new ResourceConflictException("Payment does not belong to the current user.");
        }
        return new PaymentSubscriptionContext(payment, subscription);
    }

    private PaymentSubscriptionContext findPaymentByProviderPayload(String providerCode, Map<String, String> payload) {
        String reference = firstNonBlank(payload.get("paymentReference"), payload.get("reference"), payload.get("m_payment_id"));
        if (reference != null) {
            PaymentRecord payment = paymentRepository.findTopByReferenceOrderByCreatedAtDesc(reference).orElse(null);
            if (payment != null) {
                SubscriptionRecord subscription = subscriptionRepository.findById(payment.getSubscriptionId()).orElse(null);
                return subscription == null ? null : new PaymentSubscriptionContext(payment, subscription);
            }
        }

        String orderId = firstNonBlank(payload.get("orderId"), payload.get("token"), payload.get("pf_payment_id"));
        if (orderId != null) {
            PaymentRecord payment = paymentRepository.findTopByProviderAndProviderOrderIdOrderByCreatedAtDesc(providerCode, orderId).orElse(null);
            if (payment != null) {
                SubscriptionRecord subscription = subscriptionRepository.findById(payment.getSubscriptionId()).orElse(null);
                return subscription == null ? null : new PaymentSubscriptionContext(payment, subscription);
            }
        }

        String sessionId = firstNonBlank(payload.get("session_id"), payload.get("sessionId"));
        if (sessionId != null) {
            PaymentRecord payment = paymentRepository.findTopByProviderAndProviderSessionIdOrderByCreatedAtDesc(providerCode, sessionId).orElse(null);
            if (payment != null) {
                SubscriptionRecord subscription = subscriptionRepository.findById(payment.getSubscriptionId()).orElse(null);
                return subscription == null ? null : new PaymentSubscriptionContext(payment, subscription);
            }
        }
        return null;
    }

    private String normalizeCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return "USD";
        }
        return rawCurrency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeProviderCode(String rawProviderCode) {
        if (rawProviderCode == null || rawProviderCode.isBlank()) {
            return configuredProvider();
        }
        String normalized = rawProviderCode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pay_pal", "paypal", "pp" -> PROVIDER_PAYPAL;
            case "pay_fast", "payfast", "pf" -> PROVIDER_PAYFAST;
            case "internal", "basic" -> PROVIDER_INTERNAL;
            case "mock" -> PROVIDER_MOCK;
            default -> normalized;
        };
    }

    private String normalizePaymentStatus(String rawStatus) {
        String normalized = rawStatus == null ? STATUS_FAILED : rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUCCESS", "COMPLETE", STATUS_COMPLETED -> STATUS_COMPLETED;
            case STATUS_PENDING, "PROCESSING" -> STATUS_PENDING;
            case STATUS_CANCELLED, "CANCELED" -> STATUS_CANCELLED;
            default -> STATUS_FAILED;
        };
    }

    private boolean shouldApplyWebhookTransition(PaymentRecord payment, String webhookStatus) {
        if (STATUS_COMPLETED.equals(webhookStatus)) {
            return !STATUS_COMPLETED.equals(payment.getStatus());
        }
        if (isTerminal(payment.getStatus())) {
            return false;
        }
        return !webhookStatus.equals(payment.getStatus());
    }

    private boolean applyLatestVerifiedWebhookStatusIfApplicable(
            SubscriptionRecord subscription,
            PaymentRecord payment,
            boolean logOnMiss
    ) {
        String provider = normalizeProviderCode(payment.getProvider());
        if (!PROVIDER_PAYFAST.equals(provider) || payment.getId() == null) {
            return false;
        }

        var event = paymentEventRepository.findTopByPaymentIdAndProviderAndVerifiedTrueOrderByProcessedAtDesc(payment.getId(), provider);
        if (event.isEmpty()) {
            if (logOnMiss) {
                log.warn("No verified PayFast webhook event found for paymentReference={}", payment.getReference());
            }
            return false;
        }

        String eventStatus = normalizePaymentStatus(event.get().getStatus());
        if (!shouldApplyWebhookTransition(payment, eventStatus)) {
            return false;
        }

        log.info(
                "Applying latest verified webhook event: paymentReference={}, provider={}, eventId={}, eventStatus={}",
                payment.getReference(),
                provider,
                event.get().getEventId(),
                eventStatus
        );
        applyWebhookStatus(subscription, payment, eventStatus);
        return true;
    }

    private String toSubscriptionStatus(String paymentStatus) {
        return switch (paymentStatus) {
            case STATUS_COMPLETED -> STATUS_ACTIVE;
            case STATUS_PENDING -> STATUS_PENDING;
            case STATUS_CANCELLED -> STATUS_CANCELLED;
            default -> STATUS_PAYMENT_FAILED;
        };
    }

    private String buildSuccessUrl(String paymentReference, String providerCode) {
        if (PROVIDER_PAYFAST.equals(providerCode)) {
            String configured = payFastConfig.returnUrl();
            if (configured != null && !configured.isBlank()) {
                return appendQueryParams(configured.trim(), Map.of(
                        "paymentReference", paymentReference,
                        "provider", "payfast"
                ));
            }
            String base = normalizeBackendBaseUrl();
            return base + "/api/payments/payfast/return?paymentReference=" + paymentReference + "&provider=payfast";
        }
        String base = normalizeFrontendUrl();
        String delimiter = base.contains("?") ? "&" : "?";
        return base + delimiter + "checkoutResult=success&provider=" + providerCode + "&paymentReference=" + paymentReference;
    }

    private String buildCancelUrl(String paymentReference, String providerCode) {
        if (PROVIDER_PAYFAST.equals(providerCode)) {
            String configured = payFastConfig.cancelUrl();
            if (configured != null && !configured.isBlank()) {
                return appendQueryParams(configured.trim(), Map.of(
                        "paymentReference", paymentReference,
                        "provider", "payfast"
                ));
            }
            String base = normalizeBackendBaseUrl();
            return base + "/api/payments/payfast/cancel?paymentReference=" + paymentReference + "&provider=payfast";
        }
        String base = normalizeFrontendUrl();
        String delimiter = base.contains("?") ? "&" : "?";
        return base + delimiter + "checkoutResult=cancel&provider=" + providerCode + "&paymentReference=" + paymentReference;
    }

    private String buildNotifyUrl(String paymentReference, String providerCode) {
        if (PROVIDER_PAYFAST.equals(providerCode)) {
            String configured = payFastConfig.notifyUrl();
            if (configured != null && !configured.isBlank()) {
                return configured.trim();
            }
            String base = normalizeBackendBaseUrl();
            return base + "/api/payments/payfast/notify";
        }
        return resolveCallbackUrl();
    }

    private String normalizeFrontendUrl() {
        String configured = frontendUrl == null || frontendUrl.isBlank() ? DEFAULT_FRONTEND_URL : frontendUrl.trim();
        String normalized = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        return normalized + "/student/subscription";
    }

    private String normalizeBackendBaseUrl() {
        String configured = backendBaseUrl == null || backendBaseUrl.isBlank() ? DEFAULT_BACKEND_URL : backendBaseUrl.trim();
        return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    private boolean validatePayFastWebhookPayment(PaymentRecord payment, Map<String, Object> payload) {
        if (payment == null || payload == null || payload.isEmpty()) {
            return false;
        }
        String merchantId = stringValue(payload.get("merchant_id"));
        String expectedMerchantId = payFastConfig.merchantId() == null ? null : payFastConfig.merchantId().trim();
        if (expectedMerchantId == null || expectedMerchantId.isBlank() || merchantId == null || !expectedMerchantId.equals(merchantId)) {
            return false;
        }
        String paymentReference = stringValue(payload.get("m_payment_id"));
        if (paymentReference == null || !paymentReference.equals(payment.getReference())) {
            return false;
        }
        String payFastPaymentId = stringValue(payload.get("pf_payment_id"));
        if (payFastPaymentId == null || payFastPaymentId.isBlank()) {
            return false;
        }
        String status = stringValue(payload.get("payment_status"));
        if (status == null || status.isBlank()) {
            return false;
        }
        BigDecimal expectedAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal paidAmount = parseAmount(payload.get("amount_gross"));
        if (paidAmount == null) {
            return false;
        }
        return expectedAmount.compareTo(paidAmount) == 0;
    }

    private String appendQueryParams(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            url.append(delimiter)
                    .append(entry.getKey())
                    .append("=")
                    .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
            delimiter = "&";
        }
        return url.toString();
    }

    private BigDecimal parseAmount(Object value) {
        String raw = stringValue(value);
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String resolveCallbackUrl() {
        String configured = paymentGatewayProperties.callbackUrl();
        if (configured == null || configured.isBlank()) {
            return "http://localhost:8080/api/payments/callback";
        }
        return configured.trim();
    }

    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID();
    }

    private List<String> parseFeatures(String rawFeatures) {
        if (rawFeatures == null || rawFeatures.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawFeatures, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of(rawFeatures);
        }
    }

    private void notifySubscriptionStatus(SubscriptionRecord subscription, String paymentStatus) {
        syncUserPlanType(subscription);
        String planName = subscription.getPlanCode() == null ? "plan" : subscription.getPlanCode().replace("PLAN_", "");
        if (STATUS_COMPLETED.equals(paymentStatus)) {
            notificationService.createInApp(
                    subscription.getUserId(),
                    "SUBSCRIPTION",
                    "Subscription updated",
                    "You are now on " + planName + " plan."
            );
            return;
        }

        if (STATUS_PENDING.equals(paymentStatus)) {
            notificationService.createInApp(
                    subscription.getUserId(),
                    "SUBSCRIPTION",
                    "Payment pending",
                    "Your payment for " + planName + " is pending confirmation."
            );
            return;
        }

        String reason = STATUS_CANCELLED.equals(paymentStatus)
                ? "The checkout was cancelled for your " + planName + " plan."
                : "We could not activate your " + planName + " plan. Please try again.";
        notificationService.createInApp(
                subscription.getUserId(),
                "SUBSCRIPTION",
                "Subscription payment issue",
                reason
        );
    }

    private void syncUserPlanType(SubscriptionRecord subscription) {
        userRepository.findById(subscription.getUserId()).ifPresent(user -> {
            PlanType resolvedPlanType = studentPlanAccessService.isPermanentPremiumOverride(user.getId())
                    ? PlanType.PREMIUM
                    : resolvePlanType(subscription);
            if (user.getPlanType() != resolvedPlanType) {
                user.setPlanType(resolvedPlanType);
                userRepository.save(user);
            }
        });
    }

    private PlanType resolvePlanType(SubscriptionRecord subscription) {
        if (subscription == null) {
            return PlanType.BASIC;
        }
        if (studentPlanAccessService.isPermanentPremiumOverride(subscription.getUserId())) {
            return PlanType.PREMIUM;
        }
        String planCode = subscription.getPlanCode() == null ? "" : subscription.getPlanCode().trim().toUpperCase(Locale.ROOT);
        String status = subscription.getStatus() == null ? "" : subscription.getStatus().trim().toUpperCase(Locale.ROOT);
        boolean premiumActive = (PLAN_PREMIUM.equals(planCode) || "PREMIUM".equals(planCode)) && STATUS_ACTIVE.equals(status);
        return premiumActive ? PlanType.PREMIUM : PlanType.BASIC;
    }

    private SubscriptionRecord ensurePermanentPremiumOverride(SubscriptionRecord subscription) {
        if (subscription == null || !studentPlanAccessService.isPermanentPremiumOverride(subscription.getUserId())) {
            return subscription;
        }
        applyPermanentPremiumOverride(subscription);
        return subscriptionRepository.save(subscription);
    }

    private void applyPermanentPremiumOverride(SubscriptionRecord subscription) {
        subscription.setPlanCode(PLAN_PREMIUM);
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setProvider(PROVIDER_INTERNAL);
        subscription.setStartDate(subscription.getStartDate() != null ? subscription.getStartDate() : LocalDate.now());
        subscription.setEndDate(null);
        subscription.setRenewalDate(null);
        subscription.setPremiumUntil(null);
        subscription.setTrialStartDate(null);
        subscription.setTrialEndDate(null);
        subscription.setTrialUsed(true);
        subscription.setCancelAtPeriodEnd(false);
    }

    private String serializeMetadata(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private boolean isTerminal(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status) || STATUS_FAILED.equals(status);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record PaymentSubscriptionContext(PaymentRecord payment, SubscriptionRecord subscription) {
    }
}

