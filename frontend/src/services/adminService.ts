import { apiClient } from '@/services/apiClient';

export type AdminMonthlyMetric = { month: string; total: number };
export type AdminStatusCount = { status: string; total: number };
export type AdminApplicationsPerBursary = { bursaryId: string; bursaryTitle: string; totalApplications: number };
export type AdminRecentUser = { id: string; fullName: string; email: string; roles: string[]; createdAt: string };
export type AdminRecentCompany = { id: string; companyName: string; officialEmail: string; status: string; createdAt: string };
export type AdminRecentBursary = { id: string; title: string; companyId: string; status: string; applicationEndDate?: string; createdAt: string };
export type AdminAnalytics = {
  totalUsers: number;
  totalStudents: number;
  totalCompanies: number;
  totalAdmins: number;
  pendingCompanyApprovals: number;
  approvedCompanies: number;
  suspendedCompanies: number;
  totalBursaries: number;
  activeBursaries: number;
  suspendedBursaries: number;
  closedOrExpiredBursaries: number;
  totalApplicationsSubmitted: number;
  applicationsPerBursary: AdminApplicationsPerBursary[];
  recentRegistrations: AdminRecentUser[];
  recentCompanySignups: AdminRecentCompany[];
  recentBursaryPostings: AdminRecentBursary[];
  registrationsByMonth: AdminMonthlyMetric[];
  applicationsByMonth: AdminMonthlyMetric[];
  bursariesByMonth: AdminMonthlyMetric[];
  bursariesByStatus: AdminStatusCount[];
};

export type AdminUser = {
  id: string;
  fullName: string;
  email: string;
  roles: string[];
  primaryRole?: string;
  status: string;
  active: boolean;
  companyApprovalStatus?: string | null;
  createdAt: string;
  deletedAt?: string | null;
};

export type AdminCompany = {
  id: string;
  userId: string;
  companyName: string;
  registrationNumber: string;
  officialEmail: string;
  industry?: string;
  status: string;
  reviewNotes?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
  deletedAt?: string | null;
};

export type AdminBursary = {
  id: string;
  title: string;
  companyId: string;
  companyName?: string | null;
  status: string;
  applicationStartDate?: string | null;
  applicationEndDate?: string | null;
  applicantCount: number;
  createdAt: string;
  deletedAt?: string | null;
};

export type AdminPlatformSettings = {
  id: string;
  companySelfRegistrationEnabled: boolean;
  manualCompanyApprovalRequired: boolean;
  bursaryPostingEnabled: boolean;
  studentRegistrationEnabled: boolean;
  bursaryModerationRequired: boolean;
  aiGuidanceEnabled: boolean;
  maintenanceModeEnabled: boolean;
  supportEmail?: string | null;
  platformContactInfo?: string | null;
  maxCsvBulkUploadRows: number;
  updatedAt: string;
};

export type AdminBulkUploadRowError = { rowNumber: number; message: string };
export type AdminBulkUploadResult = {
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  createdUsers: AdminUser[];
  errors: AdminBulkUploadRowError[];
};

export type AdminDistrictMetric = {
  label: string;
  value: string;
  helperText: string;
};

export type AdminDistrict = {
  id: string;
  districtCode: string;
  districtName: string;
  directorName: string;
  adminName: string;
  adminEmail: string;
  phoneNumber: string;
  address: string;
  status: string;
  active: boolean;
  schoolCount: number;
  pendingRegistrations: number;
  hasAssignedAdmin: boolean;
  warningMessage?: string | null;
  username?: string | null;
  temporaryPassword?: string | null;
  createdAt: string;
};

export type AdminDistrictManagementResponse = {
  metrics: AdminDistrictMetric[];
  items: AdminDistrict[];
};

const toParams = (params?: Record<string, string | number | boolean | undefined | null>) => (
  Object.fromEntries(Object.entries(params ?? {}).filter(([, value]) => value !== undefined && value !== null && value !== ''))
);

export const adminService = {
  getAnalytics: () => apiClient.get<AdminAnalytics>('/admin/analytics').then((r) => r.data),
  getDistricts: () => apiClient.get<AdminDistrictManagementResponse>('/admin/districts').then((r) => r.data),
  createDistrict: (payload: {
    districtName: string;
    districtCode: string;
    directorName: string;
    adminName: string;
    adminEmail: string;
    phoneNumber: string;
    physicalAddress: string;
    status: 'ACTIVE' | 'INACTIVE';
  }) => apiClient.post<AdminDistrict>('/admin/districts', payload).then((r) => r.data),
  updateDistrict: (districtId: string, payload: {
    directorName: string;
    adminName: string;
    adminEmail: string;
    phoneNumber: string;
    status: 'ACTIVE' | 'INACTIVE';
  }) => apiClient.put<AdminDistrict>(`/admin/districts/${districtId}`, payload).then((r) => r.data),
  createDistrictAdmin: (districtId: string) => apiClient.post<AdminDistrict>(`/admin/districts/${districtId}/create-admin`).then((r) => r.data),

  getUsers: (params?: { search?: string; status?: string; accountType?: string; companyStatus?: string; includeDeleted?: boolean }) => apiClient
    .get<AdminUser[]>('/admin/users', { params: toParams(params) })
    .then((r) => r.data),
  updateUserStatus: (id: string, active: boolean) => apiClient.patch<AdminUser>(`/admin/users/${id}/status`, { active }).then((r) => r.data),
  suspendUser: (id: string) => apiClient.patch<AdminUser>(`/admin/users/${id}/suspend`).then((r) => r.data),
  unsuspendUser: (id: string) => apiClient.patch<AdminUser>(`/admin/users/${id}/unsuspend`).then((r) => r.data),
  deleteUser: (id: string, reason?: string) => apiClient.delete<{ message: string }>(`/admin/users/${id}`, { data: { reason } }).then((r) => r.data),

  getRoles: () => apiClient.get('/admin/roles').then((r) => r.data),
  createRole: (payload: Record<string, unknown>) => apiClient.post('/admin/roles', payload).then((r) => r.data),
  updateRole: (id: string, payload: Record<string, unknown>) => apiClient.put(`/admin/roles/${id}`, payload).then((r) => r.data),
  deleteRole: (id: string) => apiClient.delete(`/admin/roles/${id}`).then((r) => r.data),

  listCompanies: (params?: { search?: string; status?: string; includeDeleted?: boolean }) => apiClient
    .get<AdminCompany[]>('/admin/companies', { params: toParams(params) })
    .then((r) => r.data),
  listPendingCompanies: () => apiClient.get<AdminCompany[]>('/admin/companies/pending').then((r) => r.data),
  getCompanyDetail: (id: string) => apiClient.get<AdminCompany>(`/admin/companies/${id}`).then((r) => r.data),
  approveCompany: (id: string, notes?: string) => apiClient.patch<AdminCompany>(`/admin/companies/${id}/approve`, { notes }).then((r) => r.data),
  rejectCompany: (id: string, notes?: string) => apiClient.patch<AdminCompany>(`/admin/companies/${id}/reject`, { notes }).then((r) => r.data),
  requestCompanyMoreInfo: (id: string, notes?: string) => apiClient.patch<AdminCompany>(`/admin/companies/${id}/more-info`, { notes }).then((r) => r.data),
  suspendCompany: (id: string, notes?: string) => apiClient.patch<AdminCompany>(`/admin/companies/${id}/suspend`, { notes }).then((r) => r.data),
  reactivateCompany: (id: string, notes?: string) => apiClient.patch<AdminCompany>(`/admin/companies/${id}/reactivate`, { notes }).then((r) => r.data),
  deleteCompany: (id: string, reason?: string) => apiClient.delete<AdminCompany>(`/admin/companies/${id}`, { data: { reason } }).then((r) => r.data),

  listBursaries: (params?: { status?: string; companyId?: string; fromDate?: string; toDate?: string; includeDeleted?: boolean }) => apiClient
    .get<AdminBursary[]>('/admin/bursaries', { params: toParams(params) })
    .then((r) => r.data),
  getPendingBursaries: () => apiClient.get<AdminBursary[]>('/admin/bursaries/pending').then((r) => r.data),
  reviewBursary: (id: string, decision: 'APPROVED' | 'REJECTED' | 'REQUEST_CHANGES', comment?: string) => apiClient.patch<AdminBursary>(`/admin/bursaries/${id}/review`, { decision, comment }).then((r) => r.data),
  suspendBursary: (id: string, reason?: string) => apiClient.patch<AdminBursary>(`/admin/bursaries/${id}/suspend`, { reason }).then((r) => r.data),
  reactivateBursary: (id: string, reason?: string) => apiClient.patch<AdminBursary>(`/admin/bursaries/${id}/reactivate`, { reason }).then((r) => r.data),
  deleteBursary: (id: string, reason?: string) => apiClient.delete<AdminBursary>(`/admin/bursaries/${id}`, { data: { reason } }).then((r) => r.data),

  getSettings: () => apiClient.get<AdminPlatformSettings>('/admin/settings').then((r) => r.data),
  updateSettings: (payload: Partial<AdminPlatformSettings>) => apiClient.put<AdminPlatformSettings>('/admin/settings', payload).then((r) => r.data),

  getAuditLogs: () => apiClient.get('/admin/audit-logs').then((r) => r.data),
  bulkUploadUsers: (file: File) => {
    const data = new FormData();
    data.append('file', file);
    return apiClient.post<AdminBulkUploadResult>('/admin/users/bulk-upload', data).then((r) => r.data);
  },
  getBulkUploadTemplate: () => apiClient.get('/admin/users/bulk-upload/template', { responseType: 'text' }).then((r) => String(r.data)),
};
