import { apiClient } from '@/services/apiClient';

export type DistrictMetricCard = { label: string; value: string; helperText: string; tone: string };
export type DistrictTrendPoint = { label: string; value: number; tone: string };
export type DistrictInsightItem = { title: string; detail: string; severity: string };
export type DistrictDistributionItem = { label: string; value: number; tone: string };

export type DistrictAnnouncement = {
  id: string;
  audience: string;
  title: string;
  message: string;
  deliveryScope: string;
  schoolId?: string | null;
  schoolName?: string | null;
  status: string;
  sentAt?: string | null;
  createdAt: string;
};

export type DistrictSupportRequest = {
  id: string;
  schoolId: string;
  schoolName: string;
  category: string;
  title: string;
  message: string;
  status: string;
  priority: string;
  createdAt: string;
};

export type SchoolRegistrationRequestItem = {
  requestId: string;
  userId: string;
  schoolId?: string | null;
  schoolName: string;
  emisNumber: string;
  province: string;
  district: string;
  circuit: string;
  schoolType: string;
  principalName: string;
  principalEmail: string;
  schoolEmail: string;
  phoneNumber: string;
  physicalAddress: string;
  status: 'PENDING_DISTRICT_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';
  rejectionReason?: string | null;
  submittedAt: string;
  approvedAt?: string | null;
  rejectedAt?: string | null;
};

export type SchoolRegistrationRequestStatusFilter = 'PENDING' | 'APPROVED' | 'REJECTED';

export type SchoolRegistrationRequestsResponse = {
  metrics: DistrictMetricCard[];
  items: SchoolRegistrationRequestItem[];
  total: number;
};

export type DistrictDashboardResponse = {
  districtName: string;
  districtCode?: string | null;
  province?: string | null;
  licensingStatus: string;
  summaryHeadline: string;
  metrics: DistrictMetricCard[];
  performanceTrends: DistrictTrendPoint[];
  learnerRiskDistribution: DistrictDistributionItem[];
  reportUploadProgress: DistrictDistributionItem[];
  schoolRanking: DistrictInsightItem[];
  schoolsNeedingIntervention: DistrictInsightItem[];
  aiHighlights: DistrictInsightItem[];
  recentSupportRequests: DistrictSupportRequest[];
  recentAnnouncements: DistrictAnnouncement[];
};

export type DistrictSchoolSummary = {
  schoolId: string;
  schoolName: string;
  registrationNumber?: string | null;
  district?: string | null;
  province?: string | null;
  learnerCount: number;
  teacherCount: number;
  activeClasses: number;
  reportUploadStatus: string;
  averageApsScore: number;
  performanceSummary: string;
  apsReadiness: string;
  riskLevel: string;
  complianceStatus: string;
};

export type DistrictSchoolsResponse = {
  metrics: DistrictMetricCard[];
  items: DistrictSchoolSummary[];
  total: number;
};

export type DistrictSchoolDetailResponse = {
  schoolId: string;
  schoolName: string;
  registrationNumber?: string | null;
  district?: string | null;
  province?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  metrics: DistrictMetricCard[];
  subjectPerformance: DistrictTrendPoint[];
  apsBandDistribution: DistrictDistributionItem[];
  reportUploads: DistrictDistributionItem[];
  teacherActivity: DistrictInsightItem[];
  aiRecommendations: DistrictInsightItem[];
  exportReportType: string;
};

export type DistrictSchoolAnalyticsResponse = {
  schoolId: string;
  schoolName: string;
  performanceTrends: DistrictTrendPoint[];
  subjectPerformance: DistrictTrendPoint[];
  gradePerformanceComparison: DistrictDistributionItem[];
  careerPathwayDistribution: DistrictDistributionItem[];
  urgentInterventions: DistrictInsightItem[];
};

export type DistrictAnalyticsResponse = {
  districtPerformanceTrends: DistrictTrendPoint[];
  schoolRankingComparison: DistrictInsightItem[];
  subjectPerformanceBySchool: DistrictTrendPoint[];
  apsBandDistribution: DistrictDistributionItem[];
  careerPathwayDistribution: DistrictDistributionItem[];
  learnerRiskDistribution: DistrictDistributionItem[];
  reportUploadCompletionProgress: DistrictDistributionItem[];
  gradeClassPerformanceComparison: DistrictDistributionItem[];
  urgentInterventions: DistrictInsightItem[];
};

export type DistrictAiInsightsResponse = {
  dataAvailable: boolean;
  emptyStateMessage?: string | null;
  schoolsAtRisk: DistrictInsightItem[];
  learnersAtRiskBySchool: DistrictInsightItem[];
  weakSubjectsAcrossDistrict: DistrictInsightItem[];
  apsReadinessWarnings: DistrictInsightItem[];
  careerPathwayGaps: DistrictInsightItem[];
  bursaryFundingIndicators: DistrictInsightItem[];
  teacherActivityAlerts: DistrictInsightItem[];
  recommendedDistrictInterventions: DistrictInsightItem[];
};

export type DistrictReportItem = { key: string; title: string; description: string; pdfSupported: boolean; excelSupported: boolean };
export type DistrictReportExportResponse = { fileName: string; contentType: string; base64Content: string };

export type DistrictInterventionItem = {
  id: string;
  title: string;
  category: string;
  priority: string;
  status: string;
  targetScope: string;
  schoolId?: string | null;
  schoolName?: string | null;
  notes: string;
  followUpDate?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type DistrictInterventionsResponse = {
  headline: string;
  metrics: DistrictMetricCard[];
  interventionTypes: DistrictInsightItem[];
  items: DistrictInterventionItem[];
};

export type DistrictSettingsResponse = {
  districtName: string;
  districtCode?: string | null;
  province?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  licensingStatus: string;
  activeRoles: string[];
  linkedSchoolSettings: Array<{ schoolName: string; district?: string | null; province?: string | null; activeRoles: string[]; recentAuditLogs: Array<{ action: string; entityType: string; entityId?: string | null; createdAt: string }> }>;
  supportRequests: DistrictSupportRequest[];
  announcements: DistrictAnnouncement[];
  recentAuditLogs: Array<{ action: string; entityType: string; entityId?: string | null; createdAt: string }>;
};

export type CurriculumFilePayload = {
  fileName?: string;
  contentType?: string;
  base64Content?: string;
};

export type DistrictCurriculumAsset = {
  id: string;
  repositoryType: string;
  contentSource: string;
  source?: string;
  visibility?: string;
  status: string;
  extractionStatus?: string;
  extractionError?: string | null;
  badge: string;
  title: string;
  subject: string;
  grade: string;
  curriculumPhase?: string | null;
  academicYear?: number | null;
  province?: string | null;
  versionNumber?: string | null;
  description?: string | null;
  term?: string | null;
  weekNumber?: number | null;
  uploadedBy: string;
  uploadDate: string;
  extractedAt?: string | null;
  archived: boolean;
  active?: boolean;
  deleted?: boolean;
  pdfAvailable: boolean;
  docxAvailable: boolean;
  excelAvailable: boolean;
};

export type DistrictCurriculumCalendarItem = {
  id: string;
  curriculumResourceId: string;
  subject: string;
  grade: string;
  phase?: string | null;
  academicYear?: number | null;
  province?: string | null;
  term: string;
  weekNumber: number;
  startDate?: string | null;
  endDate?: string | null;
  topic: string;
  subtopic?: string | null;
  learningObjectives?: string | null;
  resources?: string | null;
  assessmentTask?: string | null;
  lessonFocus?: string | null;
  notes?: string | null;
  status: string;
  sourceTitle: string;
  extractionStatus?: string | null;
  publishable: boolean;
  tone: string;
};

export type DistrictCurriculumCalendarStats = {
  atpsProcessed: number;
  calendarItemsGenerated: number;
  publishedTopics: number;
  draftTopics: number;
  teacherRemindersCreated: number;
  extractionErrors: number;
};

export type DistrictCurriculumCalendarResponse = {
  stats: DistrictCurriculumCalendarStats;
  items: DistrictCurriculumCalendarItem[];
  extractionErrors: DistrictCurriculumAsset[];
};

export type DistrictCurriculumPublishRepairResponse = {
  publishedItems: number;
  weekPlansSynced: number;
  schoolRemindersQueued: number;
  teacherRemindersQueued: number;
  message: string;
};

export type DistrictCurriculumHeatMapItem = {
  schoolName: string;
  subject: string;
  status: string;
  tone: string;
  compliancePercent: number;
};

export type DistrictCurriculumComplianceSchool = {
  schoolId: string;
  schoolName: string;
  compliancePercent: number;
  status: string;
  teachersBehind: number;
  subjectsBehind: number;
};

export type DistrictCurriculumRiskAlert = {
  id: string;
  schoolId: string;
  schoolName: string;
  teacherUserId: string;
  teacherName: string;
  subject: string;
  grade: string;
  title: string;
  detail: string;
  severity: string;
  createdAt: string;
};

export type DistrictCurriculumComplianceResponse = {
  metrics: DistrictMetricCard[];
  schools: DistrictCurriculumComplianceSchool[];
  heatMap: DistrictCurriculumHeatMapItem[];
  subjectsBehindSchedule: DistrictInsightItem[];
  teachersBehindSchedule: DistrictInsightItem[];
  riskAlerts: DistrictCurriculumRiskAlert[];
};

export type DistrictCurriculumAssetUpsert = {
  repositoryType: string;
  title: string;
  subject: string;
  grade: string;
  curriculumPhase?: string;
  academicYear?: number;
  province?: string;
  versionNumber?: string;
  description?: string;
  term?: string;
  weekNumber?: number;
  pdf?: CurriculumFilePayload;
  docx?: CurriculumFilePayload;
  excel?: CurriculumFilePayload;
};

export type DistrictCurriculumCalendarItemUpsert = {
  curriculumResourceId: string;
  subject?: string;
  grade?: string;
  phase?: string;
  academicYear?: number;
  term: string;
  weekNumber: number;
  startDate?: string;
  endDate?: string;
  topic: string;
  subtopic?: string;
  learningObjectives?: string;
  resources?: string;
  assessmentTask?: string;
  lessonFocus?: string;
  notes?: string;
};

export type DistrictCurriculumAssetDownload = {
  fileName: string;
  contentType: string;
  base64Content: string;
};
export type DistrictCurriculumAssetFile = {
  blob: Blob;
  fileName: string;
  contentType: string;
};

export type DistrictEducationRoleDashboard = {
  title: string;
  subtitle: string;
  metrics: DistrictMetricCard[];
};

export type CircuitSchoolRow = {
  schoolId: string;
  schoolName: string;
  principal: string;
  learners: number;
  teachers: number;
  passRate: number;
  atpCompliance: number;
  attendance: number;
  riskStatus: string;
};

export type CircuitSchoolsResponse = {
  metrics: DistrictMetricCard[];
  items: CircuitSchoolRow[];
};

export type CircuitCurriculumRow = {
  schoolId: string;
  schoolName: string;
  subject: string;
  grade: string;
  term: string;
  expectedWeek: number;
  actualWeek: number;
  topicExpected?: string | null;
  topicCompleted?: string | null;
  weeksBehind: number;
  status: string;
  riskTone: string;
};

const parseContentDispositionFileName = (header?: string) => {
  if (!header) return 'curriculum-resource';
  const utf8Match = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }
  const match = header.match(/filename="?([^"]+)"?/i);
  return match?.[1] ?? 'curriculum-resource';
};

export type SchoolVisitItem = {
  id: string;
  schoolId: string;
  schoolName: string;
  visitDate: string;
  purpose: string;
  status: string;
  notes?: string | null;
  outcome?: string | null;
  createdAt: string;
};

export type DistrictSupportWorkflowItem = {
  id: string;
  schoolId: string;
  schoolName: string;
  requestType: string;
  subject?: string | null;
  grade?: string | null;
  description: string;
  status: string;
  assignedTo?: string | null;
  assignedToName?: string | null;
  createdAt: string;
};

export type DistrictWorkflowIntervention = {
  id: string;
  title: string;
  description?: string | null;
  interventionType: string;
  priority: string;
  status: string;
  schoolId?: string | null;
  schoolName?: string | null;
  teacherId?: string | null;
  teacherName?: string | null;
  subject?: string | null;
  grade?: string | null;
  assignedTo?: string | null;
  assignedToName?: string | null;
  dueDate?: string | null;
  supportPlan?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdvisorTeacherRow = {
  teacherUserId: string;
  teacherName: string;
  schoolId: string;
  schoolName: string;
  subject: string;
  grade: string;
  classes: number;
  atpWeek: number;
  expectedWeek: number;
  status: string;
  averageMark: number;
};

export type AdvisorTeachersResponse = {
  metrics: DistrictMetricCard[];
  items: AdvisorTeacherRow[];
};

export type AdvisorTeacherProfile = {
  teacherUserId: string;
  teacherName: string;
  schoolName: string;
  assignedSubjects: string[];
  assignedClasses: string[];
  currentCoverage?: DistrictTeacherCoverage | null;
  topicsBehind: DistrictTeacherCoverage[];
  attendanceSubmissionRate: number;
  averageMark: number;
  interventions: DistrictWorkflowIntervention[];
};

export type DistrictTeacherCoverage = {
  weekPlanId: string;
  subject: string;
  grade: string;
  term: string;
  weekNumber: number;
  topic: string;
  subtopic?: string | null;
  status: string;
  progressPercent: number;
  completionLabel: string;
};

export type CommonAssessmentItem = {
  id: string;
  title: string;
  subject: string;
  grade: string;
  term: string;
  date?: string | null;
  totalMarks?: number | null;
  dueDate?: string | null;
  badge: string;
  asset: DistrictCurriculumAsset;
};

export const districtService = {
  dashboard: () => apiClient.get<DistrictDashboardResponse>('/district/dashboard').then((r) => r.data),
  schools: (params?: { search?: string; riskLevel?: string; complianceStatus?: string }) => apiClient.get<DistrictSchoolsResponse>('/district/schools', { params }).then((r) => r.data),
  schoolRegistrationRequests: (params?: { search?: string; status?: SchoolRegistrationRequestStatusFilter }) => apiClient.get<SchoolRegistrationRequestsResponse>('/district/school-registration-requests', { params }).then((r) => r.data),
  approveSchoolRegistrationRequest: (requestId: string) => apiClient.post<SchoolRegistrationRequestItem>(`/district/school-registration-requests/${requestId}/approve`).then((r) => r.data),
  rejectSchoolRegistrationRequest: (requestId: string, payload: { rejectionReason: string }) => apiClient.post<SchoolRegistrationRequestItem>(`/district/school-registration-requests/${requestId}/reject`, payload).then((r) => r.data),
  decideSchoolRegistrationRequest: (requestId: string, payload: { decision: 'APPROVE' | 'REJECT'; rejectionReason?: string }) => apiClient.post<SchoolRegistrationRequestItem>(`/district/school-registration-requests/${requestId}/decision`, payload).then((r) => r.data),
  schoolDetail: (schoolId: string) => apiClient.get<DistrictSchoolDetailResponse>(`/district/schools/${schoolId}`).then((r) => r.data),
  schoolAnalytics: (schoolId: string) => apiClient.get<DistrictSchoolAnalyticsResponse>(`/district/schools/${schoolId}/analytics`).then((r) => r.data),
  analytics: () => apiClient.get<DistrictAnalyticsResponse>('/district/analytics').then((r) => r.data),
  aiInsights: () => apiClient.get<DistrictAiInsightsResponse>('/district/ai-insights').then((r) => r.data),
  reports: () => apiClient.get<DistrictReportItem[]>('/district/reports').then((r) => r.data),
  exportReport: (type: string, format: 'pdf' | 'xlsx') => apiClient.post<DistrictReportExportResponse>('/district/reports/export', { type, format }).then((r) => r.data),
  createAnnouncement: (payload: { audience: string; title: string; message: string; deliveryScope?: string; schoolId?: string }) => apiClient.post<DistrictAnnouncement>('/district/announcements', payload).then((r) => r.data),
  interventions: () => apiClient.get<DistrictInterventionsResponse>('/district/interventions').then((r) => r.data),
  createIntervention: (payload: { title: string; category: string; priority: string; notes: string; targetScope?: string; schoolId?: string; followUpDate?: string }) => apiClient.post<DistrictInterventionItem>('/district/interventions', payload).then((r) => r.data),
  settings: () => apiClient.get<DistrictSettingsResponse>('/district/settings').then((r) => r.data),
  curriculumAssets: (repositoryType?: string) => apiClient.get<DistrictCurriculumAsset[]>('/district/curriculum/assets', { params: { repositoryType } }).then((r) => r.data),
  createCurriculumAsset: (payload: DistrictCurriculumAssetUpsert) => apiClient.post<DistrictCurriculumAsset>('/district/curriculum/assets', payload).then((r) => r.data),
  updateCurriculumAsset: (assetId: string, payload: DistrictCurriculumAssetUpsert) => apiClient.put<DistrictCurriculumAsset>(`/district/curriculum/assets/${assetId}`, payload).then((r) => r.data),
  extractCurriculumAtp: (assetId: string) => apiClient.post<DistrictCurriculumAsset>(`/district/curriculum/assets/${assetId}/extract`).then((r) => r.data),
  archiveCurriculumAsset: (assetId: string) => apiClient.post<DistrictCurriculumAsset>(`/district/curriculum/assets/${assetId}/archive`).then((r) => r.data),
  deleteCurriculumAsset: (assetId: string) => apiClient.delete(`/district/curriculum/assets/${assetId}`).then((r) => r.data),
  downloadCurriculumAsset: (assetId: string, format: 'PDF' | 'DOCX' | 'EXCEL') =>
    apiClient.get<Blob>(`/district/curriculum/assets/${assetId}/download`, { params: { format }, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      fileName: parseContentDispositionFileName(r.headers['content-disposition']),
      contentType: r.headers['content-type'] || r.data.type || 'application/octet-stream',
    })),
  curriculumCalendar: () => apiClient.get<DistrictCurriculumCalendarResponse>('/district/curriculum/calendar').then((r) => r.data),
  createCurriculumCalendarItem: (payload: DistrictCurriculumCalendarItemUpsert) => apiClient.post<DistrictCurriculumCalendarItem>('/district/curriculum/calendar/items', payload).then((r) => r.data),
  updateCurriculumCalendarItem: (itemId: string, payload: DistrictCurriculumCalendarItemUpsert) => apiClient.put<DistrictCurriculumCalendarItem>(`/district/curriculum/calendar/items/${itemId}`, payload).then((r) => r.data),
  publishCurriculumCalendarItem: (itemId: string) => apiClient.post<DistrictCurriculumCalendarItem>(`/district/curriculum/calendar/items/${itemId}/publish`).then((r) => r.data),
  archiveCurriculumCalendarItem: (itemId: string) => apiClient.post<DistrictCurriculumCalendarItem>(`/district/curriculum/calendar/items/${itemId}/archive`).then((r) => r.data),
  syncPublishedCurriculumCalendar: () => apiClient.post<DistrictCurriculumPublishRepairResponse>('/district/curriculum/calendar/sync').then((r) => r.data),
  curriculumCompliance: () => apiClient.get<DistrictCurriculumComplianceResponse>('/district/curriculum/compliance').then((r) => r.data),
  districtDirectorDashboard: () => apiClient.get<DistrictEducationRoleDashboard>('/district/education/director/dashboard').then((r) => r.data),
  circuitDashboard: () => apiClient.get<DistrictEducationRoleDashboard>('/district/education/circuit/dashboard').then((r) => r.data),
  circuitSchools: () => apiClient.get<CircuitSchoolsResponse>('/district/education/circuit/schools').then((r) => r.data),
  circuitCurriculum: (params?: { school?: string; subject?: string; grade?: string; term?: string; week?: number }) => apiClient.get<{ items: CircuitCurriculumRow[] }>('/district/education/circuit/curriculum', { params }).then((r) => r.data),
  circuitVisits: () => apiClient.get<SchoolVisitItem[]>('/district/education/circuit/visits').then((r) => r.data),
  createCircuitVisit: (payload: { schoolId: string; visitDate: string; purpose: string; notes?: string; status?: string; outcome?: string }) => apiClient.post<SchoolVisitItem>('/district/education/circuit/visits', payload).then((r) => r.data),
  circuitSupportRequests: () => apiClient.get<DistrictSupportWorkflowItem[]>('/district/education/circuit/support-requests').then((r) => r.data),
  updateCircuitSupportRequest: (id: string, payload: { status?: string; assignedTo?: string }) => apiClient.put<DistrictSupportWorkflowItem>(`/district/education/circuit/support-requests/${id}`, payload).then((r) => r.data),
  circuitInterventions: () => apiClient.get<DistrictWorkflowIntervention[]>('/district/education/circuit/interventions').then((r) => r.data),
  createDistrictWorkflowIntervention: (payload: { title: string; description?: string; interventionType?: string; priority?: string; status?: string; schoolId?: string; teacherId?: string; subject?: string; grade?: string; assignedTo?: string; dueDate?: string; notes?: string }) => apiClient.post<DistrictWorkflowIntervention>('/district/education/interventions', payload).then((r) => r.data),
  generateAiSupportPlan: (id: string) => apiClient.post<{ interventionId: string; supportPlan: string }>(`/district/education/interventions/${id}/ai-support-plan`).then((r) => r.data),
  advisorDashboard: () => apiClient.get<DistrictEducationRoleDashboard>('/district/education/advisor/dashboard').then((r) => r.data),
  advisorTeachers: () => apiClient.get<AdvisorTeachersResponse>('/district/education/advisor/teachers').then((r) => r.data),
  advisorTeacherProfile: (teacherId: string) => apiClient.get<AdvisorTeacherProfile>(`/district/education/advisor/teachers/${teacherId}`).then((r) => r.data),
  advisorAtpMonitoring: () => apiClient.get<AdvisorTeachersResponse>('/district/education/advisor/atp-monitoring').then((r) => r.data),
  advisorAssessments: () => apiClient.get<CommonAssessmentItem[]>('/district/education/advisor/assessments').then((r) => r.data),
  createAdvisorAssessment: (payload: { title: string; subject: string; grade: string; term: string; date?: string; totalMarks?: number; dueDate?: string; asset: DistrictCurriculumAssetUpsert }) => apiClient.post<CommonAssessmentItem>('/district/education/advisor/assessments', payload).then((r) => r.data),
};
