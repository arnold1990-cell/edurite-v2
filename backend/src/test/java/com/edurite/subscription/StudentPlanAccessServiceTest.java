package com.edurite.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.service.StudentPlanAccessService;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudentPlanAccessServiceTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudentPlanAccessService service = new StudentPlanAccessService(subscriptionRepository, userRepository);

    @Test
    void existingOwnerBenefitDoesNotOverrideVerifiedProSubscription() {
        UUID id = UUID.randomUUID();
        User owner = new User(); owner.setEmail("arnoldmadaz@gmail.com");
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_PRO_YEARLY"); subscription.setStatus("ACTIVE");
        subscription.setEndDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1));
        when(userRepository.findById(id)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(id)).thenReturn(Optional.of(subscription));
        assertThat(service.getCurrentPlan(id)).isEqualTo(com.edurite.subscription.entity.PlanType.PRO);
    }

    @Test
    void trialUserHasOnlyBasicAccessDuringTrialWindow() {
        UUID userId = UUID.randomUUID();
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_BASIC");
        subscription.setStatus("ACTIVE");
        subscription.setTrialStartDate(OffsetDateTime.now().minusDays(2));
        subscription.setTrialEndDate(OffsetDateTime.now().plusDays(20));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));

        var access = service.resolveByUserId(userId);
        assertThat(access.premium()).isFalse();
        assertThat(access.planCode()).isEqualTo("PLAN_BASIC");
        assertThat(access.status()).isEqualTo("TRIAL_ACTIVE");
    }

    @Test
    void userLosesPremiumAccessAfterTrialExpiryWithoutPaidPremium() {
        UUID userId = UUID.randomUUID();
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_BASIC");
        subscription.setStatus("ACTIVE");
        subscription.setTrialStartDate(OffsetDateTime.now().minusMonths(2));
        subscription.setTrialEndDate(OffsetDateTime.now().minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));

        var access = service.resolveByUserId(userId);
        assertThat(access.premium()).isFalse();
        assertThat(access.planCode()).isEqualTo("PLAN_BASIC");
    }

    @Test
    void paidPremiumOverridesExpiredTrial() {
        UUID userId = UUID.randomUUID();
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_PREMIUM");
        subscription.setEndDate(java.time.LocalDate.now().plusMonths(1));
        subscription.setStatus("ACTIVE");
        subscription.setTrialStartDate(OffsetDateTime.now().minusMonths(3));
        subscription.setTrialEndDate(OffsetDateTime.now().minusMonths(2));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));

        var access = service.resolveByUserId(userId);
        assertThat(access.premium()).isTrue();
        assertThat(access.planCode()).isEqualTo("PLAN_PREMIUM");
    }

    @Test
    void emailAloneCannotGrantPremium() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("arnoldmadaz@gmail.com");

        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_BASIC");
        subscription.setStatus("CANCELLED");
        subscription.setTrialEndDate(OffsetDateTime.now().minusDays(30));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));

        var access = service.resolveByUserId(userId);
        assertThat(access.premium()).isFalse();
        assertThat(access.status()).isEqualTo("TRIAL_EXPIRED");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"13,true", "14,false", "15,false"})
    void trialBoundaryIsExclusive(int day, boolean active) {
        UUID id = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T12:34:56Z");
        SubscriptionRecord trial = new SubscriptionRecord();
        trial.setPlanCode("PLAN_BASIC"); trial.setStatus("ACTIVE");
        trial.setTrialStartDate(start); trial.setTrialEndDate(start.plusDays(14));
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(id)).thenReturn(Optional.of(trial));
        assertThat(service.resolveAt(id, start.plusDays(day)).status()).isEqualTo(active ? "TRIAL_ACTIVE" : "TRIAL_EXPIRED");
        assertThat(service.resolveAt(id, start.plusDays(14).minusNanos(1)).status()).isEqualTo("TRIAL_ACTIVE");
        trial.setTrialEndDate(start.plusYears(1));
        assertThat(service.resolveAt(id, start.plusDays(14)).status()).isEqualTo("TRIAL_EXPIRED");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"PLAN_PREMIUM", "PLAN_PRO"})
    void validPaidRecordWinsOverExpiredTrialAndPendingPurchase(String code) {
        UUID id = UUID.randomUUID();
        SubscriptionRecord paid = new SubscriptionRecord(); paid.setPlanCode(code); paid.setStatus("ACTIVE");
        paid.setEndDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(10));
        SubscriptionRecord trial = new SubscriptionRecord(); trial.setPlanCode("PLAN_BASIC"); trial.setStatus("ACTIVE");
        trial.setTrialStartDate(OffsetDateTime.now().minusDays(30)); trial.setTrialEndDate(trial.getTrialStartDate().plusDays(14));
        SubscriptionRecord pending = new SubscriptionRecord(); pending.setPlanCode("PLAN_PRO"); pending.setStatus("PENDING");
        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(id)).thenReturn(java.util.List.of(pending, trial, paid));
        assertThat(service.resolveByUserId(id).planCode()).isEqualTo(code);
        assertThat(service.hasSubscriptionAccess(id)).isTrue();
        for (String status : java.util.List.of("CANCELLED", "EXPIRED", "PENDING", "PAYMENT_FAILED")) {
            paid.setStatus(status);
            assertThat(service.hasSubscriptionAccess(id)).isFalse();
        }
    }
}
