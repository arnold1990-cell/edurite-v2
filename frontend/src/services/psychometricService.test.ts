import { describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient';
import { psychometricService } from './psychometricService';
vi.mock('./apiClient', () => ({ apiClient: { get: vi.fn() } }));

describe('missing assessment versus unreadable results', () => {
  it('treats the normalized no-submission response as an optional missing input', async () => {
    vi.mocked(apiClient.get).mockRejectedValueOnce(Object.assign(new Error('No psychometric submission found yet.'), { status: 409 }));
    expect(await psychometricService.latestStudent()).toBeNull();
  });
  it.each([409, 403, 500])('keeps unavailable or invalid saved results visible as errors (%s)', async status => {
    const failure = Object.assign(new Error('Stored psychometric scores are invalid.'), { status });
    vi.mocked(apiClient.get).mockRejectedValueOnce(failure);
    await expect(psychometricService.latestStudent()).rejects.toBe(failure);
  });
});
