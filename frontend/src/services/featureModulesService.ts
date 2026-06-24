import { apiClient } from '@/services/apiClient';
import type {
  ApsCalculationResponse,
  ApsSubjectInput,
  CareerRoadmap,
  CareerRoadmapGenerateResponse,
  ProgressScoreResponse,
  SavedCareerRoadmap,
  ScholarshipApplication,
  SchoolProfile,
  SchoolSummary,
  StudentCv,
  StudentCvSuggestions,
  TutorAskResponse,
  TutorSession,
  UniversityApplication,
} from '@/types';

export const featureModulesService = {
  progressScore: () => apiClient.get<ProgressScoreResponse>('/student/progress-score').then((r) => r.data),

  getCv: () => apiClient.get<StudentCv>('/student/cv').then((r) => r.data),
  saveCv: (payload: Partial<StudentCv>) => apiClient.put<StudentCv>('/student/cv', payload).then((r) => r.data),
  cvSuggestions: () => apiClient.get<StudentCvSuggestions>('/student/cv/ai-suggestions').then((r) => r.data),

  scholarships: () => apiClient.get<ScholarshipApplication[]>('/student/scholarship-applications').then((r) => r.data),
  upcomingScholarships: () => apiClient.get<ScholarshipApplication[]>('/student/scholarship-applications/upcoming').then((r) => r.data),
  createScholarship: (payload: Partial<ScholarshipApplication>) => apiClient.post<ScholarshipApplication>('/student/scholarship-applications', payload).then((r) => r.data),
  updateScholarship: (id: string, payload: Partial<ScholarshipApplication>) => apiClient.put<ScholarshipApplication>(`/student/scholarship-applications/${id}`, payload).then((r) => r.data),
  deleteScholarship: (id: string) => apiClient.delete(`/student/scholarship-applications/${id}`),
  motivationLetter: (id: string) => apiClient.post<{ motivationLetterDraft: string }>(`/student/scholarship-applications/${id}/motivation-letter`).then((r) => r.data),

  tutorSessions: () => apiClient.get<TutorSession[]>('/student/tutor/sessions').then((r) => r.data),
  tutorSession: (id: string) => apiClient.get<TutorSession>(`/student/tutor/sessions/${id}`).then((r) => r.data),
  askTutor: (payload: { sessionId?: string; subject: string; question: string }) => apiClient.post<TutorAskResponse>('/student/tutor/ask', payload).then((r) => r.data),

  universityApplications: () => apiClient.get<UniversityApplication[]>('/student/university-applications').then((r) => r.data),
  createUniversityApplication: (payload: Partial<UniversityApplication>) => apiClient.post<UniversityApplication>('/student/university-applications', payload).then((r) => r.data),
  updateUniversityApplication: (id: string, payload: Partial<UniversityApplication>) => apiClient.put<UniversityApplication>(`/student/university-applications/${id}`, payload).then((r) => r.data),
  deleteUniversityApplication: (id: string) => apiClient.delete(`/student/university-applications/${id}`),

  careerRoadmaps: () => apiClient.get<CareerRoadmap[]>('/student/career-roadmaps').then((r) => r.data),
  careerRoadmap: (slug: string) => apiClient.get<CareerRoadmap>(`/student/career-roadmaps/${slug}`).then((r) => r.data),
  generateCareerRoadmap: (payload: { careerName: string; grade?: string; province?: string; subjects: ApsSubjectInput[] }) =>
    apiClient.post<CareerRoadmapGenerateResponse>('/student/career-roadmaps/generate', payload).then((r) => r.data),
  savedCareerRoadmaps: () => apiClient.get<SavedCareerRoadmap[]>('/student/career-roadmaps/saved').then((r) => r.data),
  saveCareerRoadmap: (payload: { careerName: string; roadmap: CareerRoadmapGenerateResponse; learnerAps: number; requiredAps?: number; apsGap?: number; readinessScore: number }) =>
    apiClient.post<SavedCareerRoadmap>('/student/career-roadmaps/save', payload).then((r) => r.data),
  careerRequirementMatches: (career: string) =>
    apiClient.get<CareerRoadmapGenerateResponse['universityRequirements']>('/student/career-roadmaps/requirements', { params: { career } }).then((r) => r.data),
  calculateAps: (payload: { grade?: string; province?: string; subjects: ApsSubjectInput[] }) =>
    apiClient.post<ApsCalculationResponse>('/student/aps/calculate', payload).then((r) => r.data),
  apsProfile: () => apiClient.get<ApsCalculationResponse>('/student/aps/profile').then((r) => r.data),

  adminSchools: () => apiClient.get<SchoolProfile[]>('/admin/schools').then((r) => r.data),
  createSchool: (payload: SchoolProfile) => apiClient.post<SchoolProfile>('/admin/schools', payload).then((r) => r.data),
  updateSchool: (id: string, payload: SchoolProfile) => apiClient.put<SchoolProfile>(`/admin/schools/${id}`, payload).then((r) => r.data),
  schoolSummary: (id: string) => apiClient.get<SchoolSummary>(`/admin/schools/${id}/summary`).then((r) => r.data),
  linkSchoolStudent: (id: string, studentId: string) => apiClient.post<SchoolSummary>(`/admin/schools/${id}/students`, { studentId }).then((r) => r.data),
};
