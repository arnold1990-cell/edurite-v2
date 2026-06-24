import { apiClient } from '@/services/apiClient';
import type { Career, PaginatedResponse } from '@/types';

export const careerService = {
  list: (params?: Record<string, string | number>) => apiClient.get<PaginatedResponse<Career> | Career[]>('/careers', { params }).then((r) => r.data),
  details: (id: string) => apiClient.get<Career>(`/careers/${id}`).then((r) => r.data),
};
