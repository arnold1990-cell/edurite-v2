import { apiClient } from './apiClient';
export type StudyPlan = { id: string; careerName: string; createdAt: string; steps: { title: string; focus: string; actions: string[] }[]; completedSteps: number[] };
export const studyPlanService = {
  history: () => apiClient.get<StudyPlan[]>('/student/career-roadmaps/study-plans').then(r => r.data),
  generate: (request: { requestKey: string; careerName: string; programmeId?: string }) => apiClient.post<StudyPlan>('/student/career-roadmaps/study-plans', request, { timeout: 240000 }).then(r => r.data),
  progress: (id: string, completedSteps: number[]) => apiClient.put<StudyPlan>(`/student/career-roadmaps/study-plans/${id}/progress`, { completedSteps }).then(r => r.data),
};
