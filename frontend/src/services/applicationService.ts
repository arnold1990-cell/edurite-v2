import { apiClient } from '@/services/apiClient';
import type { Application } from '@/types';
export const applicationService = {
  listMine: () => apiClient.get<Application[]>('/applications/me').then((r) => r.data),
  submit: (bursaryId: string) => apiClient.post(`/bursaries/${bursaryId}/applications`),
};
