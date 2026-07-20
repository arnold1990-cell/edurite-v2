import { describe, expect, it } from 'vitest';
import { formatRewardClaimSummary, formatRewardEventSummary, getRewardClaimState, normalizeRewardText } from '@/pages/student/rewards.utils';

describe('rewards.utils', () => {
  it('normalizes corrupted reward text', () => {
    expect(normalizeRewardText('APS 3 â€¢ Score 0%')).toBe('APS 3 • Score 0%');
    expect(normalizeRewardText('â€™')).toBe("'");
  });

  it('formats recent activity with user-facing labels and points suffix', () => {
    expect(formatRewardEventSummary({
      eventType: 'LOGIN_DAILY',
      points: 5,
      awardedAt: '2026-07-20T08:00:00Z',
      referenceId: '2026-07-20',
    })).toBe('• Daily login (+5 points)');
  });

  it('formats recent claims cleanly', () => {
    expect(formatRewardClaimSummary({
      rewardName: 'End of Term Reward',
      status: 'PENDING',
      claimedPoints: 500,
      claimedAt: '2026-07-20T08:00:00Z',
    })).toBe('• End of Term Reward (Pending, 500 points)');
  });

  it('disables reward claims when points are insufficient', () => {
    expect(getRewardClaimState(120, 500)).toEqual({
      canClaim: false,
      remainingPoints: 380,
      buttonLabel: '380 more points required',
      helperMessage: 'You need 500 available points to claim this reward.',
    });
  });

  it('enables reward claims when points meet the threshold', () => {
    expect(getRewardClaimState(500, 500)).toEqual({
      canClaim: true,
      remainingPoints: 0,
      buttonLabel: 'Claim Reward',
      helperMessage: 'You have enough available points to claim this reward.',
    });
  });
});
