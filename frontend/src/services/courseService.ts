import { apiClient } from '@/services/apiClient';
import type { Course, PaginatedResponse } from '@/types';

export const courseService = {
  list: (params?: Record<string, string | number>) => apiClient.get<PaginatedResponse<Course> | Course[]>('/courses', { params }).then((r) => r.data),
  details: (id: string) => apiClient.get<Course>(`/courses/${id}`).then((r) => r.data),
};
