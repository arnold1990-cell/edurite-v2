package com.edurite.gamification;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
}

