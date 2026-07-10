import { apiClient } from '@/services/apiClient';
import type { LearningResource } from '@/types';

export const learningService = {
  catalogue: () => apiClient.get<LearningResource[]>('/student/learning-centre/catalogue').then((r) => r.data),
  recommended: (outcomes?: string[]) => apiClient.get<LearningResource[]>('/student/learning-centre/recommended', { params: outcomes?.length ? { outcome: outcomes } : undefined }).then((r) => r.data),
  courses: (params?: { search?: string; category?: string; provider?: string; level?: string; subject?: string; freeOnly?: boolean }) =>
    apiClient.get<LearningResource[]>('/student/learning-centre/courses', { params }).then((r) => r.data),
  refreshCourses: () => apiClient.post<{ message: string; total: number; created: number; updated: number; refreshedAt: string }>('/student/learning-centre/courses/refresh').then((r) => r.data),
  books: (query: string) => apiClient.get<LearningResource[]>('/learning-centre/books', { params: { query } }).then((r) => r.data),
  googleBooks: (query: string) => apiClient.get<LearningResource[]>('/learning-centre/google-books', { params: { query } }).then((r) => r.data),
  quizzes: (amount = 10) => apiClient.get<LearningResource[]>('/learning-centre/quizzes', { params: { amount } }).then((r) => r.data),
  videos: (query: string) => apiClient.get<LearningResource[]>('/learning-centre/videos', { params: { query } }).then((r) => r.data),
};
