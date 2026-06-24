import { apiClient } from '@/services/apiClient';

export interface PublicDiscoveryInsight {
  summary: string;
  aiUsed: boolean;
  highlights: string[];
  resultCount: number;
}

export const publicDiscoveryService = {
  careersInsight: (params?: Record<string, string | number>) => apiClient.get<PublicDiscoveryInsight>('/public/discovery/careers/insight', { params }).then((r) => r.data),
  coursesInsight: (params?: Record<string, string | number>) => apiClient.get<PublicDiscoveryInsight>('/public/discovery/courses/insight', { params }).then((r) => r.data),
  bursariesInsight: (params?: Record<string, string | number>) => apiClient.get<PublicDiscoveryInsight>('/public/discovery/bursaries/insight', { params }).then((r) => r.data),
};
