import { apiClient } from '@/services/apiClient';

export const companyService = {
  getMe: () => apiClient.get('/companies/me').then((r) => r.data),
  getDashboard: () => apiClient.get('/companies/dashboard').then((r) => r.data),
  updateMe: (payload: Record<string, unknown>) => apiClient.put('/companies/me', payload).then((r) => r.data),
  uploadDocument: (file: File, documentType: string) => {
    const data = new FormData();
    data.append('file', file);
    data.append('documentType', documentType);
    return apiClient.post('/companies/me/documents', data).then((r) => r.data);
  },
  getDocuments: () => apiClient.get('/companies/me/documents').then((r) => r.data),
  getBursaries: () => apiClient.get('/companies/bursaries').then((r) => r.data),
  getBursary: (id: string) => apiClient.get(`/companies/bursaries/${id}`).then((r) => r.data),
  createBursary: (payload: Record<string, unknown>) => apiClient.post('/companies/bursaries', payload).then((r) => r.data),
  updateBursary: (id: string, payload: Record<string, unknown>) => apiClient.put(`/companies/bursaries/${id}`, payload).then((r) => r.data),
  closeBursary: (id: string) => apiClient.patch(`/companies/bursaries/${id}/close`).then((r) => r.data),
  unpublishBursary: (id: string) => apiClient.patch(`/companies/bursaries/${id}/unpublish`).then((r) => r.data),
  reopenBursary: (id: string) => apiClient.patch(`/companies/bursaries/${id}/reopen`).then((r) => r.data),
  searchStudents: (params?: Record<string, string>) => apiClient.get('/companies/students/search', { params }).then((r) => r.data),
  bookmarkStudent: (studentId: string, payload?: Record<string, string>) => apiClient.post(`/companies/students/${studentId}/bookmarks`, payload ?? {}).then((r) => r.data),
  getBookmarks: () => apiClient.get('/companies/students/bookmarks').then((r) => r.data),
  shortlistStudent: (studentId: string, payload?: Record<string, string>) => apiClient.post(`/companies/students/${studentId}/shortlists`, payload ?? {}).then((r) => r.data),
  getShortlists: () => apiClient.get('/companies/students/shortlists').then((r) => r.data),
  sendMessage: (studentId: string, payload: Record<string, string>) => apiClient.post(`/companies/students/${studentId}/messages`, payload).then((r) => r.data),
  getMessages: () => apiClient.get('/companies/messages').then((r) => r.data),
  sendInvitation: (studentId: string, payload: Record<string, string>) => apiClient.post(`/companies/students/${studentId}/invitations`, payload).then((r) => r.data),
  getInvitations: () => apiClient.get('/companies/invitations').then((r) => r.data),
};
