import { apiClient } from '@/services/apiClient';
import type { Bursary, PaginatedResponse } from '@/types';

export interface AggregatedBursary {
  externalId: string;
  title: string;
  provider?: string;
  qualificationLevel?: string;
  region?: string;
  eligibility?: string;
  deadline?: string;
  applicationLink?: string;
  sourceType: 'OFFICIAL_PROVIDER' | 'COMPANY_PROVIDER' | 'INTERNET_FALLBACK' | string;
  relevanceScore: number;
}

export const bursaryService = {
  list: (params?: Record<string, string | number>) => apiClient.get<PaginatedResponse<Bursary> | Bursary[]>('/bursaries', { params }).then((r) => r.data),
  search: (params?: Record<string, string | number>) => apiClient.get<{ items: AggregatedBursary[]; page: number; size: number; total: number }>('/bursaries/search', { params }).then((r) => r.data),
  recommended: () => apiClient.get<AggregatedBursary[]>('/bursaries/recommendations/me').then((r) => r.data),
  details: (id: string) => apiClient.get<Bursary>(`/bursaries/${id}`).then((r) => r.data),
};
