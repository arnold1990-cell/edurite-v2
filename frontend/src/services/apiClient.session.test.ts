import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient';
import { authStore } from '@/features/auth/authStore';

const originalAdapter = apiClient.defaults.adapter;
afterEach(() => { apiClient.defaults.adapter = originalAdapter; vi.restoreAllMocks(); authStore.clear(); });
const unauthorized = (config: InternalAxiosRequestConfig) => new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null,
  { data: {}, status: 401, statusText: 'Unauthorized', headers: {}, config });

describe('session refresh isolation', () => {
  it('rejects a late refresh result after account switching without clearing the new session', async () => {
    authStore.setTokens('user-a', 'refresh-a', false);
    vi.spyOn(axios, 'post').mockImplementation(async () => {
      authStore.setTokens('user-b', 'refresh-b', false);
      return { data: { accessToken: 'late-user-a', refreshToken: 'late-refresh-a' } };
    });
    apiClient.defaults.adapter = async config => { throw unauthorized(config); };
    await expect(apiClient.get('/student/profile')).rejects.toBeDefined();
    expect(authStore.getAccessToken()).toBe('user-b');
    expect(authStore.getRefreshToken()).toBe('refresh-b');
  });
  it('restores an expired access token on auth/me', async () => {
    authStore.setTokens('expired', 'refresh-a', false);
    const refresh = vi.spyOn(axios, 'post').mockResolvedValue({ data: { accessToken: 'fresh', refreshToken: 'refresh-b' } });
    apiClient.defaults.adapter = async config => {
      if (config.headers.Authorization === 'Bearer expired') throw unauthorized(config);
      return { data: { id: 'a' }, status: 200, statusText: 'OK', headers: {}, config };
    };
    expect((await apiClient.get('/auth/me')).data.id).toBe('a');
    expect(refresh).toHaveBeenCalledTimes(1);
  });
  it('does not retry an old request with a different user session', async () => {
    authStore.setTokens('user-a', 'refresh-a', false);
    const refresh = vi.spyOn(axios, 'post');
    apiClient.defaults.adapter = async config => {
      authStore.setTokens('user-b', 'refresh-b', false);
      throw unauthorized(config);
    };
    await expect(apiClient.get('/student/profile')).rejects.toBeDefined();
    expect(refresh).not.toHaveBeenCalled();
    expect(authStore.getAccessToken()).toBe('user-b');
  });
  it('never refreshes a failed password login', async () => {
    authStore.setTokens('old-user', 'old-refresh', false);
    const refresh = vi.spyOn(axios, 'post');
    apiClient.defaults.adapter = async config => { throw unauthorized(config); };
    await expect(apiClient.post('/auth/login', {})).rejects.toBeDefined();
    expect(refresh).not.toHaveBeenCalled();
  });
});
