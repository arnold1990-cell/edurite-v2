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
    void trialUserHasPremiumAccessDuringTrialWindow() {
        UUID userId = UUID.randomUUID();
        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode("PLAN_BASIC");
        subscription.setStatus("ACTIVE");
        subscription.setTrialStartDate(OffsetDateTime.now().minusDays(2));
        subscription.setTrialEndDate(OffsetDateTime.now().plusDays(20));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(subscription));

        var access = service.resolveByUserId(userId);
        assertThat(access.premium()).isTrue();
        assertThat(access.planCode()).isEqualTo("PLAN_TRIAL");
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
    void ownerOverrideAlwaysResolvesAsPremiumWithoutLimits() {
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
        assertThat(access.premium()).isTrue();
        assertThat(access.planCode()).isEqualTo("PLAN_PREMIUM");
        assertThat(access.status()).isEqualTo("ACTIVE");
        assertThat(access.careerSuggestionLimit()).isNull();
        assertThat(access.upgradeMessage()).isNull();
    }
}

