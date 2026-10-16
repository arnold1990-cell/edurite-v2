import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminSubscriptionService } from './adminSubscriptionService';
import { apiClient } from './apiClient';
vi.mock('./apiClient', () => ({ apiClient: { get: vi.fn(), post: vi.fn() } }));
describe('administrator subscription API', () => {
  beforeEach(() => vi.clearAllMocks());
  it('uses authenticated admin endpoints for search, details and paginated history', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [] } });
    await adminSubscriptionService.list('person@example.test', 2);
    await adminSubscriptionService.detail('user-id');
    await adminSubscriptionService.history('user-id', 1);
    expect(apiClient.get).toHaveBeenCalledWith('/admin/subscriptions', { params: { search: 'person@example.test', page: 2, size: 25 } });
    expect(apiClient.get).toHaveBeenCalledWith('/admin/subscriptions/user-id');
    expect(apiClient.get).toHaveBeenCalledWith('/admin/subscriptions/user-id/history', { params: { page: 1, size: 25 } });
  });
  it.each(['BASIC', 'PREMIUM', 'PRO'] as const)('assigns %s without checkout or payment claims', async plan => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { currentPlan: plan, source: 'ADMIN_OVERRIDE' } });
    expect(await adminSubscriptionService.change('user-id', plan, 'Reviewed access')).toEqual({ currentPlan: plan, source: 'ADMIN_OVERRIDE' });
    expect(apiClient.post).toHaveBeenCalledExactlyOnceWith('/admin/subscriptions/user-id/override', { plan, reason: 'Reviewed access' });
  });
  it('propagates authorization and write failures to the UI', async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error('Administrator permission required'));
    await expect(adminSubscriptionService.change('user-id', 'PRO', 'Review')).rejects.toThrow('Administrator permission required');
  });
});
