import { apiClient } from '@/services/apiClient';
import type {
  OpportunityType,
  StudentDashboard,
  StudentProfile,
  StudentSavedProfile,
  StudentSavedProfilePayload,
  StudentSavedProfileSummary,
  UnifiedOpportunity,
} from '@/types';

export const studentService = {
  getMe: () => apiClient.get<StudentProfile>('/student/profile').then((r) => r.data),
  updateMe: (payload: Partial<StudentProfile>) => apiClient.put<StudentProfile>('/student/profile', payload).then((r) => r.data),
  listSavedProfiles: () => apiClient.get<StudentSavedProfileSummary[]>('/student/profile/saved').then((r) => r.data),
  saveProfileVersion: (name: string, profile: StudentSavedProfilePayload) =>
    apiClient.post<StudentSavedProfile>('/student/profile/saved', { name, profile }).then((r) => r.data),
  getSavedProfile: (savedProfileId: string) => apiClient.get<StudentSavedProfile>(`/student/profile/saved/${savedProfileId}`).then((r) => r.data),
  applySavedProfile: (savedProfileId: string) => apiClient.post<StudentProfile>(`/student/profile/saved/${savedProfileId}/apply`).then((r) => r.data),
  deleteSavedProfile: (savedProfileId: string) => apiClient.delete(`/student/profile/saved/${savedProfileId}`),
  uploadCv: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<StudentProfile>('/student/profile/cv', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  uploadTranscript: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<StudentProfile>('/student/profile/transcript', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  getDashboard: () => apiClient.get<StudentDashboard>('/student/dashboard').then((r) => r.data),
  searchOpportunities: (params?: Record<string, string | number>) => apiClient.get<UnifiedOpportunity[]>('/student/opportunities', { params }).then((r) => r.data),
  saveOpportunity: (type: Exclude<OpportunityType, 'ALL'>, opportunityId: string, title: string) => apiClient.post(`/student/opportunities/${type}/${opportunityId}/save`, { title }),
  unsaveOpportunity: (type: Exclude<OpportunityType, 'ALL'>, opportunityId: string) => apiClient.delete(`/student/opportunities/${type}/${opportunityId}/save`),
  saveCareer: (careerId: string) => apiClient.post(`/student/careers/${careerId}/save`),
  unsaveCareer: (careerId: string) => apiClient.delete(`/student/careers/${careerId}/save`),
  savedCareers: () => apiClient.get<{ items: string[] }>('/student/careers/saved').then((r) => r.data.items),
  saveBursary: (bursaryId: string) => apiClient.post(`/student/bursaries/${bursaryId}/save`),
  unsaveBursary: (bursaryId: string) => apiClient.delete(`/student/bursaries/${bursaryId}/save`),
  savedBursaries: () => apiClient.get<{ items: string[] }>('/student/bursaries/saved').then((r) => r.data.items),
};
