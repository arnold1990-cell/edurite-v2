import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient';
import { authService } from './authService';
import { authStore } from '@/features/auth/authStore';
vi.mock('@/features/auth/authStore', () => ({ authStore: { clear: vi.fn() } }));
vi.mock('./apiClient', () => ({ apiClient: { post: vi.fn() } }));

describe('email password recovery API contract', () => {
  beforeEach(() => vi.clearAllMocks());
  it('requests a code through the original endpoint using email only', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { message: 'If the account is eligible...' } });
    await authService.requestEmailPasswordCode('user@example.com');
    expect(apiClient.post).toHaveBeenCalledWith('/auth/forgot-password/request-otp', { email: 'user@example.com' });
  });
  it('verifies the email code before completing a reset with the returned credential', async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { message: 'Verified', resetToken: 'opaque-token' } })
      .mockResolvedValueOnce({ data: { message: 'Password reset complete.' } });
    const verified = await authService.verifyEmailPasswordCode('user@example.com', '123456');
    await authService.resetEmailPassword('user@example.com', verified.resetToken, 'NewStrong@123', 'NewStrong@123');
    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/auth/forgot-password/email/verify-otp', { email: 'user@example.com', code: '123456' });
    expect(authStore.clear).toHaveBeenCalledOnce();
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/auth/forgot-password/email/reset', {
      email: 'user@example.com', resetToken: 'opaque-token', newPassword: 'NewStrong@123', confirmPassword: 'NewStrong@123',
    });
  });
  it('keeps the current client session when a reset is rejected', async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error('Invalid or expired reset session'));
    await expect(authService.resetEmailPassword('user@example.com', 'invalid-token', 'NewStrong@123', 'NewStrong@123')).rejects.toThrow('Invalid or expired');
    expect(authStore.clear).not.toHaveBeenCalled();
  });

});
