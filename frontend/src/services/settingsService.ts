import { apiClient } from '@/services/apiClient';

export interface StudentSettings {
  inAppNotificationsEnabled: boolean;
  emailNotificationsEnabled: boolean;
  smsNotificationsEnabled: boolean;
}

export const settingsService = {
  get: () => apiClient.get<StudentSettings>('/student/settings').then((r) => r.data),
  update: (payload: StudentSettings) => apiClient.put<StudentSettings>('/student/settings', payload).then((r) => r.data),
};
