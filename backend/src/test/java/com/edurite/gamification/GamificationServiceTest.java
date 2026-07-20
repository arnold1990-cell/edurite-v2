package com.edurite.gamification;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.gamification.dto.GamificationSummaryDto;
import com.edurite.gamification.dto.RewardClaimRequest;
import com.edurite.gamification.entity.RewardClaim;
import com.edurite.gamification.entity.StudentPointsLedger;
import com.edurite.gamification.repository.RewardClaimRepository;
import com.edurite.gamification.repository.RewardRuleRepository;
import com.edurite.gamification.repository.StudentPointsLedgerRepository;
import com.edurite.gamification.service.GamificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GamificationServiceTest {

    @Test
    void awardLoginPointsAwardsOncePerDay() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());

        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        String today = LocalDate.now(java.time.ZoneOffset.UTC).toString();
        when(studentPointsLedgerRepository.findFirstByStudentIdAndEventTypeAndReferenceId(profile.getId(), GamificationService.EVENT_LOGIN_DAILY, today))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new StudentPointsLedger()));
        when(studentPointsLedgerRepository.save(any(StudentPointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.awardLoginPoints(user);
        service.awardLoginPoints(user);

        verify(studentPointsLedgerRepository).save(any(StudentPointsLedger.class));
    }

    @Test
    void taskCompletionAwardsPointsForUniqueReference() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> user.getEmail();

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(studentPointsLedgerRepository.findFirstByStudentIdAndEventTypeAndReferenceId(profile.getId(), GamificationService.EVENT_TASK_COMPLETED, "PROFILE_UPDATE"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new StudentPointsLedger()));
        when(studentPointsLedgerRepository.save(any(StudentPointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.awardTaskCompletion(principal, "PROFILE_UPDATE");
        service.awardTaskCompletion(principal, "PROFILE_UPDATE");

        verify(studentPointsLedgerRepository).save(any(StudentPointsLedger.class));
    }

    @Test
    void claimRewardRejectsInsufficientBalance() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> "student@example.com";

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(profile));
        when(studentPointsLedgerRepository.sumPointsByStudentId(profile.getId())).thenReturn(120L);
        when(rewardClaimRepository.sumReservedPointsByStudentId(profile.getId())).thenReturn(0L);
        when(rewardClaimRepository.existsActiveClaimForReward(profile.getId(), LocalDate.now(java.time.ZoneOffset.UTC).getYear() + "-T3", "End of Term Reward")).thenReturn(false);

        ResourceConflictException error = assertThrows(ResourceConflictException.class,
                () -> service.claimReward(principal, new RewardClaimRequest("End of Term Reward", "Reward claim")));

        assertThat(error.getMessage()).isEqualTo("Not enough points to claim a reward yet.");
        verify(rewardClaimRepository, never()).save(any(RewardClaim.class));
    }

    @Test
    void claimRewardCreatesPendingClaimWhenEnoughPoints() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> "student@example.com";
        String currentTermCode = LocalDate.now(java.time.ZoneOffset.UTC).getYear() + "-T3";

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(profile));
        when(studentPointsLedgerRepository.sumPointsByStudentId(profile.getId())).thenReturn(650L);
        when(rewardClaimRepository.sumReservedPointsByStudentId(profile.getId())).thenReturn(100L);
        when(rewardClaimRepository.existsActiveClaimForReward(profile.getId(), currentTermCode, "End of Term Reward")).thenReturn(false);
        when(rewardClaimRepository.save(any(RewardClaim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardClaim claim = service.claimReward(principal, new RewardClaimRequest("End of Term Reward", "Reward claim"));

        assertThat(claim.getStudentId()).isEqualTo(profile.getId());
        assertThat(claim.getTermCode()).isEqualTo(currentTermCode);
        assertThat(claim.getRewardName()).isEqualTo("End of Term Reward");
        assertThat(claim.getClaimedPoints()).isEqualTo(500);
        assertThat(claim.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void claimRewardRejectsDuplicateActiveClaimForCurrentTerm() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> "student@example.com";
        String currentTermCode = LocalDate.now(java.time.ZoneOffset.UTC).getYear() + "-T3";

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(profile));
        when(rewardClaimRepository.existsActiveClaimForReward(profile.getId(), currentTermCode, "End of Term Reward")).thenReturn(true);

        ResourceConflictException error = assertThrows(ResourceConflictException.class,
                () -> service.claimReward(principal, new RewardClaimRequest("End of Term Reward", "Reward claim")));

        assertThat(error.getMessage()).isEqualTo("This reward has already been claimed for the current term.");
    }

    @Test
    void getSummaryReturnsUserFacingEventLabels() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        StudentPointsLedgerRepository studentPointsLedgerRepository = mock(StudentPointsLedgerRepository.class);
        RewardRuleRepository rewardRuleRepository = mock(RewardRuleRepository.class);
        RewardClaimRepository rewardClaimRepository = mock(RewardClaimRepository.class);
        GamificationService service = new GamificationService(
                currentUserService,
                studentProfileRepository,
                studentPointsLedgerRepository,
                rewardRuleRepository,
                rewardClaimRepository
        );

        User user = new User();
        user.setId(UUID.randomUUID());
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        Principal principal = () -> "student@example.com";

        StudentPointsLedger ledger = new StudentPointsLedger();
        ledger.setStudentId(profile.getId());
        ledger.setEventType(GamificationService.EVENT_LOGIN_DAILY);
        ledger.setPoints(5);
        ledger.setReferenceId(LocalDate.now(java.time.ZoneOffset.UTC).toString());
        ledger.setAwardedAt(java.time.OffsetDateTime.now());

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(studentPointsLedgerRepository.sumPointsByStudentId(profile.getId())).thenReturn(5L);
        when(rewardClaimRepository.sumReservedPointsByStudentId(profile.getId())).thenReturn(0L);
        when(studentPointsLedgerRepository.findTop20ByStudentIdOrderByAwardedAtDesc(profile.getId())).thenReturn(java.util.List.of(ledger));
        when(rewardClaimRepository.findTop20ByStudentIdOrderByClaimedAtDesc(profile.getId())).thenReturn(java.util.List.of());
        when(rewardRuleRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(java.util.List.of());

        GamificationSummaryDto summary = service.getSummary(principal);

        assertThat(summary.availablePoints()).isEqualTo(5L);
        assertThat(summary.recentEvents()).singleElement().extracting(GamificationSummaryDto.RecentPointEventDto::eventType).isEqualTo("Daily login");
    }
}

