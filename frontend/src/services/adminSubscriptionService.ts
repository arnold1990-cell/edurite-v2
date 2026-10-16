import { apiClient } from './apiClient';
import type { PaginatedResponse } from '@/types';

export type ManagedPlan = 'BASIC' | 'PREMIUM' | 'PRO';
export interface ManagedSubscription {
  userId: string; fullName: string; email: string; username?: string;
  currentPlan: ManagedPlan; source: string; status: string;
  subscription?: { id: string; provider: string; startDate?: string; endDate?: string; renewalDate?: string };
}
export interface SubscriptionChange {
  id: string; administratorEmail: string; previousPlan: string; newPlan: string; reason: string; createdAt: string;
}
export const adminSubscriptionService = {
  list: (search: string, page: number) => apiClient.get<PaginatedResponse<ManagedSubscription>>('/admin/subscriptions', { params: { search, page, size: 25 } }).then(r => r.data),
  detail: (userId: string) => apiClient.get<ManagedSubscription>(`/admin/subscriptions/${userId}`).then(r => r.data),
  history: (userId: string, page: number) => apiClient.get<PaginatedResponse<SubscriptionChange>>(`/admin/subscriptions/${userId}/history`, { params: { page, size: 25 } }).then(r => r.data),
  change: (userId: string, plan: ManagedPlan, reason: string) => apiClient.post<ManagedSubscription>(`/admin/subscriptions/${userId}/override`, { plan, reason }).then(r => r.data),
};
