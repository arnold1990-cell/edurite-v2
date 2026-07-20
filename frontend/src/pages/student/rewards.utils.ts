import type { GamificationSummary } from '@/types';

export const REWARD_CLAIM_COST = 500;

const TEXT_REPLACEMENTS: Array<[string, string]> = [
  ['â€¢', '•'],
  ['â€“', '–'],
  ['â€”', '—'],
  ['â€™', "'"],
  ['ï¿½', '�'],
];

const FRIENDLY_EVENT_LABELS: Record<string, string> = {
  LOGIN_DAILY: 'Daily login',
  TASK_COMPLETED: 'Task completed',
};

export const normalizeRewardText = (value?: string | null) => {
  if (!value) return '';
  const normalized = TEXT_REPLACEMENTS.reduce((result, [from, to]) => result.split(from).join(to), value).replace(/\uFFFD/g, '•');
  return normalized.replace(/\*\*|###|##|#/g, '').trim();
};

const titleCaseUnderscoreLabel = (value: string) => value
  .toLowerCase()
  .split(/[_\s]+/)
  .filter(Boolean)
  .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
  .join(' ');

export const formatRewardEventType = (eventType?: string | null) => {
  const normalized = normalizeRewardText(eventType);
  if (!normalized) return 'Activity recorded';
  if (!/^[A-Z0-9_]+$/.test(normalized)) {
    return normalized;
  }
  return FRIENDLY_EVENT_LABELS[normalized] ?? titleCaseUnderscoreLabel(normalized);
};

export const formatRewardClaimStatus = (status?: string | null) => {
  const normalized = normalizeRewardText(status);
  if (!normalized) return 'Pending';
  return /^[A-Z0-9_]+$/.test(normalized) ? titleCaseUnderscoreLabel(normalized) : normalized;
};

export const formatRewardEventSummary = (event: GamificationSummary['recentEvents'][number]) =>
  `• ${formatRewardEventType(event.eventType)} (+${event.points} points)`;

export const formatRewardClaimSummary = (claim: GamificationSummary['recentClaims'][number]) =>
  `• ${normalizeRewardText(claim.rewardName)} (${formatRewardClaimStatus(claim.status)}, ${claim.claimedPoints} points)`;

export const getRewardClaimState = (availablePoints: number, requiredPoints: number) => {
  const remainingPoints = Math.max(requiredPoints - availablePoints, 0);
  if (remainingPoints > 0) {
    return {
      canClaim: false,
      remainingPoints,
      buttonLabel: `${remainingPoints} more points required`,
      helperMessage: `You need ${requiredPoints} available points to claim this reward.`,
    };
  }
  return {
    canClaim: true,
    remainingPoints: 0,
    buttonLabel: 'Claim Reward',
    helperMessage: `You have enough available points to claim this reward.`,
  };
};
