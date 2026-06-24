import { apiClient } from '@/services/apiClient';
import type { GamificationSummary } from '@/types';

export const gamificationService = {
  summary: () => apiClient.get<GamificationSummary>('/student/gamification/summary').then((r) => r.data),
  completeTask: (referenceId?: string) => apiClient.post('/student/gamification/tasks/complete', undefined, { params: { referenceId } }).then((r) => r.data),
  claimReward: (rewardName: string, rewardDescription?: string) => apiClient.post('/student/gamification/claims', { rewardName, rewardDescription }).then((r) => r.data),
};
