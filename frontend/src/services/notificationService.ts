import { apiClient } from '@/services/apiClient';
import { authStore } from '@/features/auth/authStore';
import type { PaginatedResponse } from '@/types';

export type NotificationType = 'INFO' | 'WARNING' | 'SUCCESS' | 'PAYMENT' | 'SYSTEM' | 'ANNOUNCEMENT';
export type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type NotificationTargetAudience = 'ALL' | 'FILTERED' | 'SELECTED';

export type UserNotification = {
  id: string;
  notificationId: string;
  userId: string;
  isRead: boolean;
  readAt?: string | null;
  deliveredAt?: string | null;
  createdAt: string;
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  status: string;
  notificationCreatedAt: string;
  expiresAt?: string | null;
};

export type AdminNotification = {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  targetAudience: NotificationTargetAudience;
  status: string;
  createdAt: string;
  expiresAt?: string | null;
  recipients: number;
};

export type NotificationFilterPreview = { totalMatchedUsers: number; userIds: string[] };

export type SendNotificationPayload = {
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  targetAudience: NotificationTargetAudience;
  expiresAt?: string;
  filters?: {
    role?: string;
    status?: string;
    subscriptionPlan?: string;
    grade?: string;
    schoolId?: string;
    className?: string;
    search?: string;
    activeOnly?: boolean;
  };
  selectedUserIds?: string[];
};

export const notificationService = {
  mine: (params?: { page?: number; size?: number; status?: string; type?: string }) =>
    apiClient.get<PaginatedResponse<UserNotification>>('/notifications', { params }).then((r) => r.data),
  unreadCount: () => apiClient.get<{ unreadCount: number }>('/notifications/unread-count').then((r) => r.data),
  markRead: (id: string) => apiClient.patch(`/notifications/${id}/read`),
  markAllRead: () => apiClient.patch('/notifications/read-all'),
  deleteMine: (id: string) => apiClient.delete(`/notifications/${id}`),

  sendAdmin: (payload: SendNotificationPayload) => apiClient.post<AdminNotification>('/admin/notifications/send', payload).then((r) => r.data),
  listAdmin: (params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<PaginatedResponse<AdminNotification>>('/admin/notifications', { params }).then((r) => r.data),
  filterUsers: (params?: { role?: string; status?: string; subscriptionPlan?: string; grade?: string; school?: string; className?: string; search?: string; activeOnly?: boolean; page?: number; size?: number }) =>
    apiClient.get<NotificationFilterPreview>('/admin/notifications/users/filter', { params }).then((r) => r.data),
  deleteAdmin: (id: string) => apiClient.delete(`/admin/notifications/${id}`),
  streamUrl: () => {
    const token = authStore.getAccessToken();
    if (!token) return null;
    const base = (apiClient.defaults.baseURL ?? '/api').replace(/\/+$/, '');
    return `${base}/notifications/stream?access_token=${encodeURIComponent(token)}`;
  },
};
