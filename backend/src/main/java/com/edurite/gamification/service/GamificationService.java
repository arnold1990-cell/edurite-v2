package com.edurite.gamification.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.gamification.dto.GamificationSummaryDto;
import com.edurite.gamification.dto.RewardClaimRequest;
import com.edurite.gamification.entity.RewardClaim;
import com.edurite.gamification.entity.RewardRule;
import com.edurite.gamification.entity.StudentPointsLedger;
import com.edurite.gamification.repository.RewardClaimRepository;
import com.edurite.gamification.repository.RewardRuleRepository;
import com.edurite.gamification.repository.StudentPointsLedgerRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationService {

    public static final String EVENT_LOGIN_DAILY = "LOGIN_DAILY";
    public static final String EVENT_TASK_COMPLETED = "TASK_COMPLETED";
    private static final int CLAIM_COST = 500;

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentPointsLedgerRepository studentPointsLedgerRepository;
    private final RewardRuleRepository rewardRuleRepository;
    private final RewardClaimRepository rewardClaimRepository;

    public GamificationService(
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            StudentPointsLedgerRepository studentPointsLedgerRepository,
            RewardRuleRepository rewardRuleRepository,
            RewardClaimRepository rewardClaimRepository
    ) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.studentPointsLedgerRepository = studentPointsLedgerRepository;
        this.rewardRuleRepository = rewardRuleRepository;
        this.rewardClaimRepository = rewardClaimRepository;
    }

    @Transactional
    public void awardLoginPoints(User user) {
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return;
        }
        String referenceId = LocalDate.now(ZoneOffset.UTC).toString();
        if (studentPointsLedgerRepository.findFirstByStudentIdAndEventTypeAndReferenceId(profile.getId(), EVENT_LOGIN_DAILY, referenceId).isPresent()) {
            return;
        }
        String termCode = currentTermCode();
        if (isAwardBlocked(profile.getId(), EVENT_LOGIN_DAILY, termCode)) {
            return;
        }
        int points = resolvePointsForEvent(EVENT_LOGIN_DAILY, 5);
        createLedgerEntry(profile, EVENT_LOGIN_DAILY, points, referenceId);
    }

    @Transactional
    public void awardTaskCompletion(Principal principal, String referenceId) {
        StudentProfile profile = requireStudentProfile(principal);
        String safeReference = (referenceId == null || referenceId.isBlank())
                ? "TASK-" + System.currentTimeMillis()
                : referenceId.trim();
        if (studentPointsLedgerRepository.findFirstByStudentIdAndEventTypeAndReferenceId(profile.getId(), EVENT_TASK_COMPLETED, safeReference).isPresent()) {
            return;
        }
        String termCode = currentTermCode();
        if (isAwardBlocked(profile.getId(), EVENT_TASK_COMPLETED, termCode)) {
            return;
        }
        int points = resolvePointsForEvent(EVENT_TASK_COMPLETED, 15);
        createLedgerEntry(profile, EVENT_TASK_COMPLETED, points, safeReference);
    }

    @Transactional(readOnly = true)
    public GamificationSummaryDto getSummary(Principal principal) {
        StudentProfile profile = requireStudentProfile(principal);
        long totalPoints = studentPointsLedgerRepository.sumPointsByStudentId(profile.getId());
        long reservedPoints = rewardClaimRepository.sumReservedPointsByStudentId(profile.getId());
        long availablePoints = Math.max(totalPoints - reservedPoints, 0);

        List<GamificationSummaryDto.RecentPointEventDto> events = studentPointsLedgerRepository
                .findTop20ByStudentIdOrderByAwardedAtDesc(profile.getId())
                .stream()
                .map(item -> new GamificationSummaryDto.RecentPointEventDto(
                        userFacingEventLabel(item.getEventType()),
                        item.getPoints(),
                        item.getAwardedAt().toString(),
                        item.getReferenceId()))
                .toList();

        List<GamificationSummaryDto.RewardClaimDto> claims = rewardClaimRepository
                .findTop20ByStudentIdOrderByClaimedAtDesc(profile.getId())
                .stream()
                .map(item -> new GamificationSummaryDto.RewardClaimDto(
                        normalizeDisplayText(item.getRewardName()),
                        userFacingClaimStatus(item.getStatus()),
                        item.getClaimedPoints(),
                        item.getClaimedAt().toString()))
                .toList();

        List<GamificationSummaryDto.RewardRuleDto> activeRules = rewardRuleRepository.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(rule -> new GamificationSummaryDto.RewardRuleDto(
                        rule.getCode(),
                        rule.getName(),
                        rule.getDescription(),
                        rule.getEventType(),
                        rule.getPointsPerEvent(),
                        rule.getMaxPerTerm()))
                .toList();

        return new GamificationSummaryDto(
                totalPoints,
                reservedPoints,
                availablePoints,
                currentTermCode(),
                events,
                claims,
                activeRules
        );
    }

    @Transactional
    public RewardClaim claimReward(Principal principal, RewardClaimRequest request) {
        StudentProfile profile = requireStudentProfileForUpdate(principal);
        String rewardName = normalizeDisplayText(request.rewardName()).trim();
        String termCode = currentTermCode();
        if (rewardClaimRepository.existsActiveClaimForReward(profile.getId(), termCode, rewardName)) {
            throw new ResourceConflictException("This reward has already been claimed for the current term.");
        }
        long totalPoints = studentPointsLedgerRepository.sumPointsByStudentId(profile.getId());
        long reservedPoints = rewardClaimRepository.sumReservedPointsByStudentId(profile.getId());
        long availablePoints = Math.max(totalPoints - reservedPoints, 0);
        if (availablePoints < CLAIM_COST) {
            throw new ResourceConflictException("Not enough points to claim a reward yet.");
        }

        RewardClaim claim = new RewardClaim();
        claim.setStudentId(profile.getId());
        claim.setTermCode(termCode);
        claim.setRewardName(rewardName);
        claim.setRewardDescription(normalizeDisplayText(request.rewardDescription()));
        claim.setStatus("PENDING");
        claim.setClaimedPoints(CLAIM_COST);
        claim.setClaimedAt(OffsetDateTime.now());
        return rewardClaimRepository.save(claim);
    }

    private void createLedgerEntry(StudentProfile profile, String eventType, int points, String referenceId) {
        StudentPointsLedger ledger = new StudentPointsLedger();
        ledger.setStudentId(profile.getId());
        ledger.setEventType(eventType);
        ledger.setPoints(points);
        ledger.setReferenceId(referenceId);
        ledger.setAwardedAt(OffsetDateTime.now());
        ledger.setTermCode(currentTermCode());
        studentPointsLedgerRepository.save(ledger);
    }

    private int resolvePointsForEvent(String eventType, int fallbackPoints) {
        return rewardRuleRepository.findByCode(eventType)
                .filter(this::isRuleApplicableToday)
                .map(RewardRule::getPointsPerEvent)
                .orElse(fallbackPoints);
    }

    private boolean isAwardBlocked(UUID studentId, String eventType, String termCode) {
        return rewardRuleRepository.findByCode(eventType)
                .filter(this::isRuleApplicableToday)
                .map(rule -> {
                    Integer maxPerTerm = rule.getMaxPerTerm();
                    if (maxPerTerm == null || maxPerTerm <= 0) {
                        return false;
                    }
                    long awardedCount = studentPointsLedgerRepository.countByStudentIdAndEventTypeAndTermCode(studentId, eventType, termCode);
                    return awardedCount >= maxPerTerm;
                })
                .orElse(false);
    }

    private boolean isRuleApplicableToday(RewardRule rule) {
        if (!rule.isActive()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (rule.getStartDate() != null && today.isBefore(rule.getStartDate())) {
            return false;
        }
        if (rule.getEndDate() != null && today.isAfter(rule.getEndDate())) {
            return false;
        }
        return true;
    }

    private StudentProfile requireStudentProfile(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found for this account."));
    }

    private StudentProfile requireStudentProfileForUpdate(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return studentProfileRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found for this account."));
    }

    private String currentTermCode() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int quarter = ((today.getMonthValue() - 1) / 3) + 1;
        return today.getYear() + "-T" + quarter;
    }

    private String userFacingEventLabel(String eventType) {
        String normalized = normalizeDisplayText(eventType).trim();
        if (normalized.isBlank()) {
            return "Activity recorded";
        }
        return switch (normalized) {
            case EVENT_LOGIN_DAILY -> "Daily login";
            case EVENT_TASK_COMPLETED -> "Task completed";
            default -> titleCaseUnderscoreLabel(normalized);
        };
    }

    private String userFacingClaimStatus(String status) {
        String normalized = normalizeDisplayText(status).trim();
        if (normalized.isBlank()) {
            return "Pending";
        }
        return titleCaseUnderscoreLabel(normalized);
    }

    private String titleCaseUnderscoreLabel(String value) {
        String[] parts = value.toLowerCase().split("[_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? value : builder.toString();
    }

    private String normalizeDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value.trim();
        }
        return value
                .replace("â€¢", "•")
                .replace("â€“", "–")
                .replace("â€”", "—")
                .replace("â€™", "'")
                .replace("ï¿½", "�")
                .replace("\uFFFD", "•")
                .trim();
    }
}

