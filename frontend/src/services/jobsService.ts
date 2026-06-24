import { apiClient } from '@/services/apiClient';
import type { JobOpportunity } from '@/types';

export const jobsService = {
  search: (params: { query: string; location: string; category?: string; experience?: string }) =>
    apiClient.get<JobOpportunity[]>('/jobs/search', { params }).then((r) => r.data),
};

