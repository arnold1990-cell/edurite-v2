import { beforeEach, describe, expect, it, vi } from 'vitest';
import { studyPlanService } from './studyPlanService';
import { apiClient } from './apiClient';
vi.mock('./apiClient', () => ({ apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
describe('shared study plan API', () => {
  beforeEach(() => vi.clearAllMocks());
  it('preserves the request key across retries and sends no invented learner context', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { id: 'plan' } });
    const request = { requestKey: 'retry-key', careerName: 'Nurse' };
    await studyPlanService.generate(request); await studyPlanService.generate(request);
    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/student/career-roadmaps/study-plans', request, { timeout: 240000 });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/student/career-roadmaps/study-plans', request, { timeout: 240000 });
  });
  it('loads persisted history and saves completion through the same resource', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [{ id: 'saved' }] });
    vi.mocked(apiClient.put).mockResolvedValue({ data: { id: 'saved', completedSteps: [0] } });
    expect(await studyPlanService.history()).toEqual([{ id: 'saved' }]);
    await studyPlanService.progress('saved', [0]);
    expect(apiClient.put).toHaveBeenCalledWith('/student/career-roadmaps/study-plans/saved/progress', { completedSteps: [0] });
  });
});
