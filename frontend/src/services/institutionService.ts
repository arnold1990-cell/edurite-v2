import { apiClient } from '@/services/apiClient';
export const institutionService = { list: (params?: Record<string, string | number>) => apiClient.get('/institutions', { params }).then((r) => r.data), details: (id: string) => apiClient.get(`/institutions/${id}`).then((r) => r.data) };
