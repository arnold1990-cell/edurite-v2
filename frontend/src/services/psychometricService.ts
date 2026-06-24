import axios from 'axios';
import { apiClient } from '@/services/apiClient';
import type { PsychometricAnswerItem, PsychometricAssessment, PsychometricAttemptResult, PsychometricQuestion, PsychometricResult } from '@/types';

export const psychometricService = {
  assessments: () => apiClient.get<PsychometricAssessment[]>('/student/psychometric/assessments').then((r) => r.data),
  assessmentQuestions: (assessmentId: string) => apiClient.get<PsychometricQuestion[]>(`/student/psychometric/assessments/${assessmentId}/questions`).then((r) => r.data),
  assessmentHistory: (assessmentId: string) => apiClient.get<PsychometricAttemptResult[]>(`/student/psychometric/assessments/${assessmentId}/attempts`).then((r) => r.data),
  submitAssessmentAttempt: (assessmentId: string, answers: Array<{ questionId: string; score: number }>) =>
    apiClient.post<PsychometricAttemptResult>(`/student/psychometric/assessments/${assessmentId}/attempts`, { answers }).then((r) => r.data),
  submitStudent: (answers: PsychometricAnswerItem[]) => apiClient.post<PsychometricResult>('/student/psychometric/submit', { answers }).then((r) => r.data),
  latestStudent: async () => {
    try {
      const response = await apiClient.get<PsychometricResult>('/student/psychometric/latest');
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && (error.response?.status === 404 || error.response?.status === 409)) {
        return null;
      }
      throw error;
    }
  },
  submitPublic: (answers: PsychometricAnswerItem[], sessionId?: string) => apiClient.post<PsychometricResult>('/public/psychometric/submit', { answers }, { params: { sessionId } }).then((r) => r.data),
};
