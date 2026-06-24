import { apiClient } from '@/services/apiClient';
import type { Recommendation } from '@/types';
export const recommendationService = { mine: () => apiClient.get<Recommendation>('/recommendations/me').then((r) => r.data) };
