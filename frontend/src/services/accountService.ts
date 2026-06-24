import { apiClient } from '@/services/apiClient';
import type { AuthResponseRaw, VerificationStatusResponse } from '@/types';

export const ACCOUNT_ENDPOINTS = {
  deleteMine: '/account/me',
  requestPasswordChangeOtp: '/account/password/change/request-otp',
  changePasswordWithOtp: '/account/password/change/confirm',
  firstLoginChangePassword: '/account/first-login/change-password',
} as const;

export const accountService = {
  deleteMine: (confirmationText: string, reason?: string) => apiClient.delete(ACCOUNT_ENDPOINTS.deleteMine, { data: { confirmationText, reason } }).then((r) => r.data),
  requestPasswordChangeOtp: () => apiClient.post<VerificationStatusResponse>(ACCOUNT_ENDPOINTS.requestPasswordChangeOtp).then((r) => r.data),
  changePasswordWithOtp: (payload: { currentPassword: string; code: string; newPassword: string }) => apiClient.post<AuthResponseRaw>(ACCOUNT_ENDPOINTS.changePasswordWithOtp, payload, { timeout: 15000 }),
  forcePasswordChange: (payload: { currentPassword: string; newPassword: string; confirmNewPassword: string }) => apiClient.post<AuthResponseRaw>(ACCOUNT_ENDPOINTS.firstLoginChangePassword, payload, { timeout: 15000 }),
};
