import { apiClient } from '@/services/apiClient';
import type { UserNotification } from '@/services/notificationService';
import type { PaginatedResponse } from '@/types';

export type SchoolDashboard = { role: string; schoolId: string; totalClasses: number; totalSubjects: number; totalTasks: number; totalNotes: number; totalSubmissions: number };
export type SchoolClass = { id: string; grade: string; className: string; academicYear: number; term?: string; active: boolean };
export type SchoolSubject = {
  id: string;
  subjectName: string;
  phase: string;
  grade?: string;
  gradeRange?: string;
  languageLevel?: string;
  subjectType?: string;
  isLanguage?: boolean;
  isCompulsory?: boolean;
  hodUserId?: string | null;
  capsAligned?: boolean;
  subjectCatalogueId?: string | null;
  active: boolean;
};
export type SchoolTask = {
  id: string;
  classId: string;
  subjectId: string;
  teacherUserId: string;
  taskType: string;
  title: string;
  instructions?: string;
  dueAt: string;
  maxMarks: number;
  term?: string;
  atpTopicId?: string | null;
  academicYear?: number | null;
  phase?: string | null;
  grade?: string | null;
  assessmentType?: string | null;
  weekNumber?: number | null;
  resourcesMaterials?: string | null;
  cognitiveLevel?: string | null;
  assessmentCategory?: string | null;
};
export type SubmissionView = { submissionId: string; taskId: string; learnerUserId: string; learnerName: string; submissionText?: string; fileUrl?: string; late: boolean; status: string; similarity: number; plagiarismFlag: boolean; marks?: number; feedback?: string; released: boolean };
export type TeacherClassView = { classId: string; grade: string; className: string; academicYear: number; subjectName: string; learnerCount: number };
export type TeacherSubjectView = { subjectId: string; subjectName: string; phase: string; grade?: string; classCount: number };
export type TeacherAnalytics = { pendingMarking: number; sbaTasksDue: number; learnerSubmissions: number; averageClassPerformance: number; attendanceRate: number; upcomingAssessments: number };
export type TeacherActivityItem = { type: string; title: string; detail: string; occurredAt: string; priority: string };
export type TeacherCalendarItem = { title: string; category: string; dueAt: string };
export type LearnerSubjectView = { subjectId: string; subjectName: string; phase: string; grade?: string; teacherName: string; progress: number; latestTaskTitle?: string; latestNoteTitle?: string };
export type LearnerTaskView = { taskId: string; taskType: string; title: string; instructions?: string; dueAt: string; maxMarks: number; term?: string };
export type LearnerAssessmentView = { taskId: string; taskType: string; title: string; instructions?: string; dueAt: string; maxMarks: number; term?: string };
export type LearnerProgressSummary = { totalTasks: number; submitted: number; missing: number; late: number };
export type PublicSchool = { id: string; name: string; schoolCode: string };
export type SchoolRegistrationStatus = {
  requestId: string;
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
export type StudentMySchoolStatus = {
  status: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';
  school?: PublicSchool | null;
  generatedUsername?: string | null;
  learnerGrade?: string | null;
  learnerClassName?: string | null;
  message?: string | null;
  requestedAt?: string | null;
  approvedAt?: string | null;
  rejectedAt?: string | null;
};
export type SchoolJoinRequest = {
  requestId: string;
  studentId: string;
  learnerFullName: string;
  learnerEmail: string;
  learnerPhone?: string | null;
  schoolId: string;
  schoolName: string;
  schoolCode: string;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  generatedUsername?: string | null;
};
export type SchoolAdminUser = { userId: string; fullName: string; email: string; roleName: string; status: string; active: boolean; phoneNumber?: string | null };
export type TeacherAssignment = { id: string; teacherUserId: string; classId: string; subjectId: string; phase?: string; grade?: string; isClassTeacher?: boolean; active: boolean };
export type SubjectCatalogueItem = { id: string; name: string; phase: string; gradeRange: string; subjectType?: string; languageLevel?: string; isLanguage: boolean; isCompulsory: boolean; active: boolean };
export type AtpTopic = { id: string; phase: string; grade: string; subjectCatalogueId?: string | null; subjectName: string; academicYear: number; term: string; weekNumber?: number | null; topic: string; subtopic?: string | null; recommendedActivities?: string | null; assessmentGuidance?: string | null; capsReference?: string | null; active: boolean };
export type SchoolSubjectManagementView = SchoolSubject & { assignedTeacherCount: number; learnerCount: number; hasLinkedRecords: boolean };
export type SubjectSummaryMetric = { label: string; value: number; helperText: string };
export type SubjectManagementSummary = { metrics: SubjectSummaryMetric[] };
export type LearnerEnrollment = { id: string; learnerUserId: string; classId: string; subjectId: string; active: boolean };
export type PortalMetric = { label: string; value: number; trendLabel: string; tone: string };
export type TopBreakdownItem = { label: string; value: number };
export type SchoolPortalDashboard = { role: string; schoolId: string; metrics: PortalMetric[]; topCareerInterests: TopBreakdownItem[]; topSubjectRiskAreas: TopBreakdownItem[] };
export type LearnerListItem = {
  learnerUserId: string;
  studentProfileId?: string | null;
  learnerName: string;
  email: string;
  grade?: string | null;
  className?: string | null;
  teacherName?: string | null;
  active: boolean;
  profileComplete: boolean;
  apsPoints: number;
  careerGoal?: string | null;
  needsIntervention: boolean;
  bursaryEligible: boolean;
  popiaStatus: string;
};
export type LearnerListResponse = { items: LearnerListItem[]; total: number };
export type SubjectMarkView = { subjectName: string; achievementLevel?: number | null; markPercent: number; risk: boolean };
export type MatchedCourseView = { name: string; level?: string | null; eligible: boolean; reason: string };
export type MatchedBursaryView = { title: string; provider?: string | null; deadline?: string | null; eligible: boolean; missingRequirements: string };
export type InterventionSummaryView = { interventionId: string; status: string; priority: string; supportType: string; notes: string; followUpDate?: string | null; updatedAt: string };
export type TimelineItem = { title: string; detail: string; occurredAt: string; type: string };
export type LearnerProfileResponse = {
  learnerUserId: string;
  studentProfileId?: string | null;
  learnerName: string;
  email: string;
  grade?: string | null;
  className?: string | null;
  teacherName?: string | null;
  profileComplete: boolean;
  apsPoints: number;
  careerGoal?: string | null;
  qualificationLevel?: string | null;
  interests?: string | null;
  skills?: string | null;
  popiaStatus: string;
  subjects: SubjectMarkView[];
  courseEligibility: MatchedCourseView[];
  bursaryMatches: MatchedBursaryView[];
  interventions: InterventionSummaryView[];
  activityTimeline: TimelineItem[];
};
export type PerformanceBandItem = { label: string; value: number; tone: string };
export type AcademicInsightsResponse = {
  gradePerformance: PerformanceBandItem[];
  subjectPerformance: PerformanceBandItem[];
  classPerformance: PerformanceBandItem[];
  atRiskLearners: LearnerListItem[];
  subjectsAffectingCareerEligibility: TopBreakdownItem[];
};
export type CareerReadinessLearnerView = { learnerUserId: string; learnerName: string; careerGoal?: string | null; aligned: boolean; apsPoints: number; readinessGap: string; alternativePathway: string };
export type CareerReadinessResponse = { learners: CareerReadinessLearnerView[]; topCareerInterests: TopBreakdownItem[]; readinessGaps: TopBreakdownItem[]; alternativePathways: TopBreakdownItem[] };
export type BursaryReadinessItem = { learnerUserId: string; learnerName: string; bursaryTitle: string; provider?: string | null; deadline?: string | null; missingRequirements: string; checklist: string };
export type BursaryReadinessResponse = { matches: BursaryReadinessItem[]; deadlineAlerts: BursaryReadinessItem[]; missingRequirements: TopBreakdownItem[] };
export type InterventionReportItem = { interventionId: string; learnerUserId: string; learnerName: string; assignedBy: string; supportType: string; priority: string; status: string; notes: string; followUpDate?: string | null; updatedAt: string };
export type ReportExportResponse = { fileName: string; contentType: string; base64Content: string };
export type PortalSettingsResponse = { schoolName: string; district?: string | null; province?: string | null; activeRoles: string[]; recentAuditLogs: Array<{ action: string; entityType: string; entityId?: string | null; createdAt: string }> };
export type CurriculumFilePayload = { fileName?: string; contentType?: string; base64Content?: string };
export type CurriculumAsset = {
  id: string;
  repositoryType: string;
  contentSource: string;
  source?: string;
  visibility?: string;
  status?: string;
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
  archived: boolean;
  active?: boolean;
  deleted?: boolean;
  pdfAvailable: boolean;
  docxAvailable: boolean;
  excelAvailable: boolean;
};
export type TeacherReminder = { reminderType: string; title: string; message: string; tone: string; reminderDate?: string | null; status?: string };
export type TeacherCoverageItem = {
  weekPlanId: string;
  curriculumResourceId: string;
  atpCalendarItemId?: string | null;
  subject: string;
  grade: string;
  phase?: string | null;
  academicYear?: number | null;
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
  sourceTitle?: string | null;
  status: string;
  progressPercent: number;
  completionLabel: string;
};
export type TeacherCurriculumWidgets = {
  currentTopic?: TeacherCoverageItem | null;
  thisWeeksCoverage?: TeacherCoverageItem | null;
  topicsBehindSchedule: TeacherCoverageItem[];
  visibleTopics: TeacherCoverageItem[];
  upcomingTopics: TeacherCoverageItem[];
  reminders: TeacherReminder[];
  districtResources: CurriculumAsset[];
  officialSyllabuses: CurriculumAsset[];
  lessonPlans: CurriculumAsset[];
  totalPublishedAtpCalendarItems: number;
  currentTeacherAtpMatches: number;
  currentWeekAtpItem?: TeacherCoverageItem | null;
};
export type TeacherLessonPlanResponse = {
  lessonPlanAssetId?: string | null;
  sourceCalendarItemId?: string | null;
  alreadyExisted: boolean;
  pdfAvailable: boolean;
  docxAvailable: boolean;
  title: string;
  weekEnding: string;
  subtopic: string;
  sourceAtpTitle: string;
  learningObjectives: string;
  introduction: string;
  activities: string;
  learnerActivities: string;
  resources: string;
  assessment: string;
  homework: string;
  differentiation: string;
  reflection: string;
  days: Array<{
    day: string;
    topicContent: string;
    objectives: string;
    sourceOfMatter: string;
    media: string;
    lessonActivities: string;
    evaluation: string;
  }>;
};
export type SchoolCurriculumCalendarResponse = {
  thisWeeksCoverage?: TeacherCoverageItem | null;
  upcomingTopics: TeacherCoverageItem[];
  visibleTopics: TeacherCoverageItem[];
  subjectsBehindSchedule: TeacherCoverageItem[];
  reminders: TeacherReminder[];
  districtResources: CurriculumAsset[];
  totalPublishedAtpCalendarItems: number;
  currentWeekAtpItem?: TeacherCoverageItem | null;
  currentSchoolAtpMatches: number;
  itemsVisibleToSchool: number;
  itemsMatchingCurrentWeek: number;
  itemsWithNullDates: number;
  districtWideItems: number;
  remindersQueuedForSchool: number;
};
export type CurriculumAssetDownload = { fileName: string; contentType: string; base64Content: string };
export type CurriculumResourceFilters = {
  type?: string;
  subject?: string;
  grade?: string;
  phase?: string;
  term?: string;
  week?: number;
  academicYear?: number;
};
export type CurriculumResourceFile = {
  blob: Blob;
  fileName: string;
  contentType: string;
};
export type SchoolProfileUpsert = {
  schoolName: string;
  registrationNumber?: string;
  district?: string;
  province?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
};
export type AdminMetricCard = { label: string; value: string; helperText: string; tone: string };
export type AdminTrendPoint = { label: string; value: number; tone: string };
export type AdminInsightItem = { title: string; detail: string; severity: string };
export type AdminDistributionItem = { label: string; value: number; tone: string };
export type SchoolAdminDashboardResponse = {
  schoolName: string;
  role: string;
  systemStatus: string;
  subscriptionStatus: string;
  summaryHeadline: string;
  metrics: AdminMetricCard[];
  topCareerInterests: AdminInsightItem[];
  topSubjectRiskAreas: AdminInsightItem[];
  teacherActivitySummary: AdminInsightItem[];
  schoolPerformanceTrends: AdminTrendPoint[];
  subjectPerformance: AdminTrendPoint[];
  apsBandDistribution: AdminDistributionItem[];
  gradePerformanceComparison: AdminDistributionItem[];
  reportUploadProgress: AdminDistributionItem[];
  districtReportingSummary: AdminInsightItem[];
};

export type SchoolJoinRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type SchoolAdminAnalyticsResponse = {
  schoolPerformanceTrends: AdminTrendPoint[];
  subjectPerformance: AdminTrendPoint[];
  apsBandDistribution: AdminDistributionItem[];
  gradePerformanceComparison: AdminDistributionItem[];
  learnerImprovementRecommendations: AdminInsightItem[];
  careerReadinessOverview: AdminInsightItem[];
  districtReadyReportingSummary: AdminInsightItem[];
};
export type SchoolAdminAiInsightsResponse = {
  dataAvailable: boolean;
  emptyStateMessage?: string | null;
  learnersAtRisk: AdminInsightItem[];
  subjectWeaknessTrends: AdminInsightItem[];
  apsReadinessWarnings: AdminInsightItem[];
  careerPathwayGaps: AdminInsightItem[];
  bursaryNeedIndicators: AdminInsightItem[];
  teacherActivityAlerts: AdminInsightItem[];
  recommendedInterventions: AdminInsightItem[];
};
export type SchoolAdminLearner = {
  learnerUserId: string;
  learnerName: string;
  username: string;
  email: string;
  generatedPassword?: string | null;
  grade?: string | null;
  className?: string | null;
  teacherName?: string | null;
  apsPoints: number;
  profileComplete: boolean;
  needsIntervention: boolean;
  bursaryEligible: boolean;
  readinessStatus: string;
  bursaryMatchCount: number;
  parentGuardianName?: string | null;
  parentGuardianPhone?: string | null;
  parentGuardianEmail?: string | null;
  consentStatus?: string | null;
  reportUploadStatus?: string | null;
  careerGoal?: string | null;
  lastActiveAt?: string | null;
};
export type SchoolAdminLearnerListResponse = { items: SchoolAdminLearner[]; total: number };
export type SchoolAdminLearnerRequirement = { label: string; value: string; tone: string };
export type SchoolAdminCourseItem = { name: string; level?: string | null; eligible: boolean; reason: string };
export type SchoolAdminBursaryItem = { title: string; provider?: string | null; deadline?: string | null; eligible: boolean; missingRequirements: string };
export type SchoolAdminInterventionItem = { interventionId: string; status: string; priority: string; supportType: string; notes: string; followUpDate?: string | null; updatedAt?: string | null };
export type SchoolAdminNoteItem = { title: string; detail: string; author: string; createdAt?: string | null };
export type SchoolAdminTimelineItem = { title: string; detail: string; occurredAt?: string | null; type: string };
export type SchoolAdminLearnerProfileResponse = {
  learnerUserId: string;
  learnerName: string;
  email: string;
  grade?: string | null;
  className?: string | null;
  teacherName?: string | null;
  profileComplete: boolean;
  apsPoints: number;
  careerGoal?: string | null;
  readinessLevel: string;
  reportUploadStatus?: string | null;
  consentStatus?: string | null;
  qualificationLevel?: string | null;
  interests?: string | null;
  skills?: string | null;
  requiredSubjects: SchoolAdminLearnerRequirement[];
  requiredMarks: SchoolAdminLearnerRequirement[];
  coursesQualifiedNow: SchoolAdminCourseItem[];
  coursesCloseToQualifyingFor: SchoolAdminCourseItem[];
  alternativePathways: AdminInsightItem[];
  bursaryMatches: SchoolAdminBursaryItem[];
  missingRequirements: SchoolAdminLearnerRequirement[];
  bursaryDeadlines: SchoolAdminLearnerRequirement[];
  interventionPlan: SchoolAdminInterventionItem[];
  teacherNotes: SchoolAdminNoteItem[];
  followUpDate?: string | null;
  activityTimeline: SchoolAdminTimelineItem[];
};
export type SchoolAdminTeacher = {
  teacherUserId: string;
  fullName: string;
  email: string;
  phoneNumber?: string | null;
  employeeNumber?: string | null;
  profilePhotoUrl?: string | null;
  status: string;
  active: boolean;
  assignedClasses: number;
  assignedSubjects: number;
  learnerCount: number;
  createdAssignments: number;
  createdAssessments: number;
  uploadedNotes: number;
  resourcesUploaded: number;
  learnerSubmissions: number;
  submissionRate: number;
  averageLearnerPerformance: number;
  apsImpact: number;
  interventionCount: number;
  careerGuidanceSessions: number;
  learnersSupported: number;
  careerMappedLearners: number;
  careerReadyLearners: number;
  bursaryReadyLearners: number;
  learnersAtRisk: number;
  learnersMeetingApsRequirements: number;
  activeInterventions: number;
  engagementScore: number;
};
export type SchoolAdminTeacherListResponse = { items: SchoolAdminTeacher[]; total: number; pendingApprovals: number };
export type TeacherAdminDashboardResponse = {
  metrics: AdminMetricCard[];
  workloadAlerts: AdminInsightItem[];
  topContributors: AdminInsightItem[];
  approvalAlerts: AdminInsightItem[];
};
export type SchoolAdminTeacherActivityItem = { teacherUserId: string; teacherName: string; title: string; detail: string; category: string; occurredAt?: string | null };
export type TeacherActivityResponse = { today: SchoolAdminTeacherActivityItem[]; thisWeek: SchoolAdminTeacherActivityItem[]; thisMonth: SchoolAdminTeacherActivityItem[] };
export type TeacherAiInsightResponse = { dataAvailable: boolean; emptyStateMessage?: string | null; items: AdminInsightItem[] };
export type TeacherWorkloadItem = { teacherUserId: string; teacherName: string; classesAssigned: number; subjectsAssigned: number; learnersAssigned: number; assessmentsAssigned: number; assignmentsAssigned: number; workloadBand: string };
export type TeacherWorkloadResponse = { items: TeacherWorkloadItem[]; alerts: AdminInsightItem[] };
export type TeacherResourceCategory = { category: string; uploadCount: number; downloadCount: number };
export type TeacherResourceItem = { teacherUserId: string; teacherName: string; uploadCount: number; downloadCount: number; categories: TeacherResourceCategory[] };
export type TeacherResourceResponse = { teachers: TeacherResourceItem[]; mostUsedResources: AdminInsightItem[]; topContributors: AdminInsightItem[] };
export type TeacherTrainingItem = {
  teacherUserId: string;
  teacherName: string;
  trainingCompleted: number;
  eduriteCertifications: number;
  careerGuidanceTraining: number;
  aiToolUsage: number;
  cpdHours: number;
  lastTrainingDate?: string | null;
  certificationStatus: string;
  developmentProgress: string;
};
export type TeacherTrainingResponse = { items: TeacherTrainingItem[]; recommendations: AdminInsightItem[] };
export type TeacherApprovalHistory = { action: string; createdAt?: string | null };
export type TeacherProfileField = { label: string; value: string };
export type TeacherDocument = { title: string; status: string; url?: string | null };
export type TeacherDetailResponse = {
  summary: SchoolAdminTeacher;
  classes: string[];
  subjects: string[];
  profile: TeacherProfileField[];
  approvalDetails: TeacherProfileField[];
  performance: TeacherProfileField[];
  readinessImpact: TeacherProfileField[];
  activityTimeline: SchoolAdminTeacherActivityItem[];
  documents: TeacherDocument[];
  notes: AdminInsightItem[];
  interventions: SchoolAdminInterventionReport[];
  approvalHistory: TeacherApprovalHistory[];
};
export type SchoolAdminReportItem = { key: string; title: string; description: string; pdfSupported: boolean; excelSupported: boolean };
export type SchoolAnnouncement = { id: string; audience: string; title: string; message: string; status: string; sentAt?: string | null; createdAt: string };
export type SchoolSupportRequest = { id: string; category: string; title: string; message: string; status: string; priority: string; createdAt: string };
export type SchoolAdminCareerReadinessResponse = {
  headline: string;
  metrics: AdminMetricCard[];
  topCareerInterests: AdminInsightItem[];
  commonReadinessGaps: AdminInsightItem[];
  alignmentWarnings: AdminInsightItem[];
  alternativePathwayRecommendations: AdminInsightItem[];
};
export type SchoolAdminCourseMatch = {
  learnerUserId: string;
  learnerName: string;
  careerGoal?: string | null;
  apsPoints: number;
  qualifiedCourses: SchoolAdminCourseItem[];
  closeCourses: SchoolAdminCourseItem[];
};
export type SchoolAdminCoursesResponse = {
  headline: string;
  metrics: AdminMetricCard[];
  institutionOptions: AdminInsightItem[];
  mostMatchedCourses: AdminInsightItem[];
  qualificationGaps: AdminInsightItem[];
  learnerMatches: SchoolAdminCourseMatch[];
};
export type SchoolAdminBursaryReadinessResponse = {
  headline: string;
  metrics: AdminMetricCard[];
  fundingInterestByCareerField: AdminInsightItem[];
  missingRequirements: AdminInsightItem[];
  deadlineAlerts: AdminInsightItem[];
  savedBursaries: SchoolAdminBursaryItem[];
};
export type SchoolAdminInterventionReport = {
  interventionId: string;
  learnerUserId: string;
  learnerName: string;
  assignedBy: string;
  supportType: string;
  priority: string;
  status: string;
  notes: string;
  followUpDate?: string | null;
  updatedAt?: string | null;
};
export type SchoolAdminInterventionsResponse = {
  headline: string;
  metrics: AdminMetricCard[];
  interventionTypes: AdminInsightItem[];
  items: SchoolAdminInterventionReport[];
};
export type SchoolAdminSettingsResponse = {
  schoolName: string;
  registrationNumber?: string | null;
  district?: string | null;
  province?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  activeRoles: string[];
  recentAuditLogs: Array<{ action: string; entityType: string; entityId?: string | null; createdAt: string }>;
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
export type LearnerCredentialsExportResponse = {
  fileName: string;
  contentType: string;
  base64Content: string;
  items: Array<{ learnerName: string; username: string; password: string; grade: string; className: string }>;
};
export type SchoolAdminClass = {
  classId: string;
  className: string;
  grade: string;
  academicYear: number;
  term?: string | null;
  classTeacher: string;
  learnerCount: number;
  subjectCount: number;
  averageAps: number;
  careerReadinessPercent: number;
  bursaryReadinessPercent: number;
  interventionCount: number;
  active: boolean;
  attendanceRate: number;
  assignmentCompletionRate: number;
  reportUploadCompletion: number;
};
export type SchoolAdminClassListResponse = {
  metrics: AdminMetricCard[];
  items: SchoolAdminClass[];
  topPerformingClasses: AdminInsightItem[];
  mostImprovedClasses: AdminInsightItem[];
  atRiskClasses: AdminInsightItem[];
  highestCareerReadinessClasses: AdminInsightItem[];
};
export type SchoolAdminClassAnalyticsResponse = {
  classId: string;
  classLabel: string;
  metrics: AdminMetricCard[];
  apsDistribution: AdminTrendPoint[];
  subjectPerformanceTrends: AdminTrendPoint[];
  readinessTrends: AdminTrendPoint[];
  interventionTrends: AdminTrendPoint[];
};
export type SchoolAdminClassCareerReadinessResponse = {
  classId: string;
  classLabel: string;
  careerReadinessScore: number;
  careerInterests: AdminInsightItem[];
  careerAlignment: AdminInsightItem[];
  careerGaps: AdminInsightItem[];
};
export type SchoolAdminClassBursaryReadinessResponse = {
  classId: string;
  classLabel: string;
  metrics: AdminMetricCard[];
  upcomingDeadlines: AdminInsightItem[];
  applicationReadiness: AdminInsightItem[];
};
export type SchoolAdminClassAiInsightsResponse = { dataAvailable: boolean; emptyStateMessage?: string | null; items: AdminInsightItem[] };
export type SchoolAdminClassProfileResponse = {
  summary: SchoolAdminClass;
  academics: AdminInsightItem[];
  careerReadiness: AdminInsightItem[];
  bursaries: AdminInsightItem[];
  interventions: AdminInsightItem[];
  aiInsights: AdminInsightItem[];
  activityTimeline: SchoolAdminTimelineItem[];
  learners: SchoolAdminLearner[];
  subjectTeachers: AdminInsightItem[];
};

export const schoolService = {
  listPublicSchools: () => apiClient.get<PublicSchool[]>('/public/schools').then((r) => r.data),
  getMySchoolStatus: () => apiClient.get<StudentMySchoolStatus>('/student/my-school/status').then((r) => r.data),
  requestMySchoolJoin: (schoolId: string) => apiClient.post<StudentMySchoolStatus>('/student/my-school/request', { schoolId }).then((r) => r.data),
  schoolAdminLearnerJoinRequests: (status: SchoolJoinRequestStatus = 'PENDING') =>
    apiClient.get<SchoolJoinRequest[]>('/school-admin/learner-join-requests', { params: { status } }).then((r) => r.data),
  approveLearnerJoinRequest: (requestId: string) => apiClient.post<SchoolJoinRequest>(`/school-admin/learner-join-requests/${requestId}/approve`).then((r) => r.data),
  rejectLearnerJoinRequest: (requestId: string) => apiClient.post<SchoolJoinRequest>(`/school-admin/learner-join-requests/${requestId}/reject`).then((r) => r.data),
  schoolAdminMySchoolRequests: () => apiClient.get<SchoolJoinRequest[]>('/school-admin/my-school-requests', { params: { status: 'PENDING' } }).then((r) => r.data),
  approveMySchoolRequest: (requestId: string) => apiClient.post<SchoolJoinRequest>(`/school-admin/my-school-requests/${requestId}/approve`).then((r) => r.data),
  rejectMySchoolRequest: (requestId: string) => apiClient.post<SchoolJoinRequest>(`/school-admin/my-school-requests/${requestId}/reject`).then((r) => r.data),
  schoolAdminDashboard: () => apiClient.get<SchoolDashboard>('/school/dashboard').then((r) => r.data),
  teacherDashboard: () => apiClient.get<SchoolDashboard>('/teacher/dashboard').then((r) => r.data),
  learnerDashboard: () => apiClient.get<SchoolDashboard>('/school-student/dashboard').then((r) => r.data),
  upsertSchoolProfile: (payload: SchoolProfileUpsert) => apiClient.put('/school/profile', payload).then((r) => r.data),

  createClass: (payload: { grade: string; className: string; academicYear: number; term?: string }) => apiClient.post('/school/classes', payload).then((r) => r.data),
  listClasses: () => apiClient.get<SchoolClass[]>('/school/classes').then((r) => r.data),
  updateClass: (classId: string, payload: { grade: string; className: string; academicYear: number; term?: string; active: boolean }) => apiClient.put(`/school/classes/${classId}`, payload).then((r) => r.data),
  deactivateClass: (classId: string) => apiClient.patch(`/school/classes/${classId}/deactivate`).then((r) => r.data),
  createSubject: (payload: { subjectCatalogueId?: string; subjectName: string; phase: string; grade?: string; gradeRange?: string; languageLevel?: string; subjectType?: string; isLanguage?: boolean; isCompulsory?: boolean; hodUserId?: string | null; capsAligned?: boolean; active?: boolean }) => apiClient.post('/school/subjects', payload).then((r) => r.data),
  listSubjects: () => apiClient.get<SchoolSubject[]>('/school/subjects').then((r) => r.data),
  listSubjectManagement: (includeInactive = true) => apiClient.get<SchoolSubjectManagementView[]>('/school/subjects', { params: { view: 'management', includeInactive } }).then((r) => r.data),
  listSubjectCatalogue: () => apiClient.get<SubjectCatalogueItem[]>('/school/subjects/catalogue').then((r) => r.data),
  subjectSummary: () => apiClient.get<SubjectManagementSummary>('/school/subjects/summary').then((r) => r.data),
  listAtpTopics: (params?: { phase?: string; grade?: string; subjectId?: string; term?: string }) => apiClient.get<AtpTopic[]>('/school/atp-topics', { params }).then((r) => r.data),
  updateSubject: (subjectId: string, payload: { subjectCatalogueId?: string; subjectName: string; phase: string; grade?: string; gradeRange?: string; languageLevel?: string; subjectType?: string; isLanguage?: boolean; isCompulsory?: boolean; hodUserId?: string | null; capsAligned?: boolean; active: boolean }) => apiClient.put(`/school/subjects/${subjectId}`, payload).then((r) => r.data),
  deactivateSubject: (subjectId: string) => apiClient.patch(`/school/subjects/${subjectId}/deactivate`).then((r) => r.data),
  assignTeacher: (payload: { teacherUserId: string; classId: string; subjectId: string; phase?: string; grade?: string; isClassTeacher?: boolean }) => apiClient.post('/school/teacher-assignments', payload).then((r) => r.data),
  listTeacherAssignments: () => apiClient.get<TeacherAssignment[]>('/school/teacher-assignments').then((r) => r.data),
  listTeacherAssignmentsForTeacher: (teacherUserId: string) => apiClient.get<TeacherAssignment[]>('/school/teacher-assignments', { params: { teacherUserId } }).then((r) => r.data),
  replaceTeacherAssignments: (teacherUserId: string, assignments: Array<{ teacherUserId: string; classId: string; subjectId: string; phase?: string; grade?: string; isClassTeacher?: boolean }>) =>
    apiClient.put<TeacherAssignment[]>(`/school/teachers/${teacherUserId}/assignments`, { assignments }).then((r) => r.data),
  enrollLearner: (payload: { learnerUserId: string; classId: string; subjectId: string }) => apiClient.post('/school/learner-enrollments', payload).then((r) => r.data),
  listLearnerEnrollments: () => apiClient.get<LearnerEnrollment[]>('/school/learner-enrollments').then((r) => r.data),
  listTeachers: () => apiClient.get<SchoolAdminUser[]>('/school/teachers').then((r) => r.data),
  listLearners: () => apiClient.get<SchoolAdminUser[]>('/school/learners').then((r) => r.data),
  createSchoolUser: (payload: { email: string; password: string; firstName: string; lastName: string; roleName: string; phoneNumber?: string; status?: string; selectedGrade?: string; careerGoal?: string; popiaConsentAccepted?: boolean; consentVersion?: string }) => apiClient.post('/school/users', payload).then((r) => r.data),
  bulkUploadLearners: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/school/learners/bulk-upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  updateSchoolUser: (userId: string, payload: { firstName: string; lastName: string; email: string }) => apiClient.put(`/school/users/${userId}`, payload).then((r) => r.data),
  deactivateSchoolUser: (userId: string) => apiClient.delete(`/school/users/${userId}`).then((r) => r.data),
  schoolTasks: () => apiClient.get<SchoolTask[]>('/school/tasks').then((r) => r.data),
  createSchoolTask: (payload: { teacherUserId?: string; classId: string; subjectId: string; atpTopicId?: string; taskType: string; title: string; academicYear?: number; phase?: string; grade?: string; instructions?: string; assessmentType?: string; weekNumber?: number; dueAt: string; term?: string; maxMarks: number; rubric?: string; resources?: string; cognitiveLevel?: string; assessmentCategory?: string }) => apiClient.post('/school/tasks', payload).then((r) => r.data),
  schoolAssessments: () => apiClient.get<SchoolTask[]>('/school/assessments').then((r) => r.data),
  schoolSubmissions: () => apiClient.get<{ items: SubmissionView[] }>('/school/submissions').then((r) => r.data.items),
  schoolResults: () => apiClient.get<{ items: SubmissionView[] }>('/school/results').then((r) => r.data.items),
  schoolNotes: () => apiClient.get('/school/notes').then((r) => r.data),

  createNote: (payload: { classId: string; subjectId: string; title: string; noteText?: string; pdfUrl?: string }) => apiClient.post('/teacher/notes', payload).then((r) => r.data),
  teacherAtpTopics: (params?: { phase?: string; grade?: string; subjectId?: string; term?: string }) => apiClient.get<AtpTopic[]>('/teacher/atp-topics', { params }).then((r) => r.data),
  createTask: (payload: { teacherUserId?: string; classId: string; subjectId: string; atpTopicId?: string; taskType: string; title: string; academicYear?: number; phase?: string; grade?: string; instructions?: string; assessmentType?: string; weekNumber?: number; dueAt: string; term?: string; maxMarks: number; rubric?: string; resources?: string; cognitiveLevel?: string; assessmentCategory?: string }) => apiClient.post('/teacher/tasks', payload).then((r) => r.data),
  teacherTasks: () => apiClient.get<SchoolTask[]>('/teacher/tasks').then((r) => r.data),
  teacherAssignments: () => apiClient.get<SchoolTask[]>('/teacher/assignments').then((r) => r.data),
  teacherClasses: () => apiClient.get<TeacherClassView[]>('/teacher/classes').then((r) => r.data),
  teacherSubjects: () => apiClient.get<TeacherSubjectView[]>('/teacher/subjects').then((r) => r.data),
  teacherAssessments: () => apiClient.get<SchoolTask[]>('/teacher/assessments').then((r) => r.data),
  teacherSubmissions: () => apiClient.get<{ items: SubmissionView[] }>('/teacher/submissions').then((r) => r.data.items),
  teacherAnalytics: () => apiClient.get<TeacherAnalytics>('/teacher/analytics').then((r) => r.data),
  teacherActivity: () => apiClient.get<{ items: TeacherActivityItem[] }>('/teacher/activity').then((r) => r.data.items),
  teacherCalendar: () => apiClient.get<{ items: TeacherCalendarItem[] }>('/teacher/calendar').then((r) => r.data.items),
  teacherSearch: (q: string) => apiClient.get<{ items: string[] }>('/teacher/search', { params: { q } }).then((r) => r.data.items),
  taskSubmissions: (taskId: string) => apiClient.get<{ items: SubmissionView[] }>(`/teacher/tasks/${taskId}/submissions`).then((r) => r.data.items),
  markSubmission: (submissionId: string, payload: { marksAwarded: number; comments?: string; rubricScoring?: string; released: boolean }) => apiClient.post(`/teacher/submissions/${submissionId}/mark`, payload).then((r) => r.data),

  learnerNotes: () => apiClient.get('/school-student/notes').then((r) => r.data),
  learnerSubjects: () => apiClient.get<LearnerSubjectView[]>('/school-student/subjects').then((r) => r.data),
  learnerTasks: () => apiClient.get<LearnerTaskView[]>('/school-student/tasks').then((r) => r.data),
  learnerAssessments: () => apiClient.get<LearnerAssessmentView[]>('/school-student/assessments').then((r) => r.data),
  submitTask: (payload: { taskId: string; submissionText?: string; fileUrl?: string }) => apiClient.post('/school-student/submissions', payload).then((r) => r.data),
  learnerSubmissions: () => apiClient.get<{ items: SubmissionView[] }>('/school-student/submissions').then((r) => r.data.items),
  learnerProgress: () => apiClient.get<LearnerProgressSummary>('/school-student/progress').then((r) => r.data),
  learnerMarks: () => apiClient.get<{ items: SubmissionView[] }>('/school-student/marks').then((r) => r.data.items),

  schoolPortalDashboard: () => apiClient.get<SchoolPortalDashboard>('/school/portal/dashboard').then((r) => r.data),
  teacherPortalDashboard: () => apiClient.get<SchoolPortalDashboard>('/teacher/portal/dashboard').then((r) => r.data),
  schoolPortalLearners: (params?: { search?: string; grade?: string; className?: string }) => apiClient.get<LearnerListResponse>('/school/portal/learners', { params }).then((r) => r.data),
  teacherPortalLearners: (params?: { search?: string; grade?: string; className?: string }) => apiClient.get<LearnerListResponse>('/teacher/portal/learners', { params }).then((r) => r.data),
  schoolPortalLearnerProfile: (learnerUserId: string) => apiClient.get<LearnerProfileResponse>(`/school/portal/learners/${learnerUserId}`).then((r) => r.data),
  teacherPortalLearnerProfile: (learnerUserId: string) => apiClient.get<LearnerProfileResponse>(`/teacher/portal/learners/${learnerUserId}`).then((r) => r.data),
  schoolAcademicInsights: () => apiClient.get<AcademicInsightsResponse>('/school/portal/academic-insights').then((r) => r.data),
  teacherAcademicInsights: () => apiClient.get<AcademicInsightsResponse>('/teacher/portal/academic-insights').then((r) => r.data),
  schoolCareerReadiness: () => apiClient.get<CareerReadinessResponse>('/school/portal/career-readiness').then((r) => r.data),
  teacherCareerReadiness: () => apiClient.get<CareerReadinessResponse>('/teacher/portal/career-readiness').then((r) => r.data),
  schoolBursaryReadiness: () => apiClient.get<BursaryReadinessResponse>('/school/portal/bursary-readiness').then((r) => r.data),
  teacherBursaryReadiness: () => apiClient.get<BursaryReadinessResponse>('/teacher/portal/bursary-readiness').then((r) => r.data),
  schoolInterventions: () => apiClient.get<{ items: InterventionReportItem[] }>('/school/portal/interventions').then((r) => r.data.items),
  teacherInterventions: () => apiClient.get<{ items: InterventionReportItem[] }>('/teacher/portal/interventions').then((r) => r.data.items),
  createSchoolIntervention: (payload: { learnerUserId: string; supportType: string; priority: string; notes: string; followUpDate?: string; status?: string }) => apiClient.post<InterventionReportItem>('/school/portal/interventions', payload).then((r) => r.data),
  createTeacherIntervention: (payload: { learnerUserId: string; supportType: string; priority: string; notes: string; followUpDate?: string; status?: string }) => apiClient.post<InterventionReportItem>('/teacher/portal/interventions', payload).then((r) => r.data),
  updateSchoolIntervention: (interventionId: string, payload: { status: string; notes: string; followUpDate?: string }) => apiClient.patch<InterventionReportItem>(`/school/portal/interventions/${interventionId}`, payload).then((r) => r.data),
  updateTeacherIntervention: (interventionId: string, payload: { status: string; notes: string; followUpDate?: string }) => apiClient.patch<InterventionReportItem>(`/teacher/portal/interventions/${interventionId}`, payload).then((r) => r.data),
  exportSchoolReport: (type: string, format: 'csv' | 'pdf') => apiClient.get<ReportExportResponse>('/school/portal/reports/export', { params: { type, format } }).then((r) => r.data),
  exportTeacherReport: (type: string, format: 'csv' | 'pdf') => apiClient.get<ReportExportResponse>('/teacher/portal/reports/export', { params: { type, format } }).then((r) => r.data),
  schoolPortalSettings: () => apiClient.get<PortalSettingsResponse>('/school/portal/settings').then((r) => r.data),
  teacherPortalSettings: () => apiClient.get<PortalSettingsResponse>('/teacher/portal/settings').then((r) => r.data),
  teacherCurriculumWidgets: () => apiClient.get<TeacherCurriculumWidgets>('/teacher/curriculum/calendar').then((r) => r.data),
  teacherCurriculumReminders: () => apiClient.get<TeacherReminder[]>('/teacher/portal/curriculum/reminders').then((r) => r.data),
  schoolCurriculumCalendar: () => apiClient.get<SchoolCurriculumCalendarResponse>('/school/curriculum/calendar').then((r) => r.data),
  schoolCurriculumResources: (params?: CurriculumResourceFilters) =>
    apiClient.get<CurriculumAsset[]>('/school/curriculum/resources', { params }).then((r) => r.data),
  teacherCurriculumResources: (params?: CurriculumResourceFilters) =>
    apiClient.get<CurriculumAsset[]>('/teacher/curriculum/resources', { params }).then((r) => r.data),
  createTeacherLessonPlanFromCalendarItem: (calendarItemId: string, payload?: { regenerate?: boolean }) =>
    apiClient.post<TeacherLessonPlanResponse>(`/teacher/curriculum/calendar/${calendarItemId}/lesson-plan`, payload ?? {}).then((r) => r.data),
  downloadCurriculumResourceFile: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') =>
    apiClient.get<Blob>(`/curriculum/resources/${assetId}/download`, { params: format ? { format } : undefined, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      fileName: parseContentDispositionFileName(r.headers['content-disposition']),
      contentType: r.headers['content-type'] || r.data.type || 'application/octet-stream',
    })),
  viewCurriculumResourceFile: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') =>
    apiClient.get<Blob>(`/curriculum/resources/${assetId}/view`, { params: format ? { format } : undefined, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      fileName: parseContentDispositionFileName(r.headers['content-disposition']),
      contentType: r.headers['content-type'] || r.data.type || 'application/octet-stream',
    })),
  downloadTeacherCurriculumAsset: (assetId: string, format: 'PDF' | 'DOCX' | 'EXCEL') =>
    apiClient.get<Blob>(`/teacher/portal/curriculum/assets/${assetId}/download`, { params: { format }, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      fileName: parseContentDispositionFileName(r.headers['content-disposition']),
      contentType: r.headers['content-type'] || r.data.type || 'application/octet-stream',
    })),
  updateTeacherCurriculumProgress: (weekPlanId: string, payload: { status: string; completionPercent?: number; notes?: string }) =>
    apiClient.patch<TeacherCoverageItem>(`/teacher/portal/curriculum/weeks/${weekPlanId}`, payload).then((r) => r.data),
  generateTeacherLessonPlan: (weekPlanId: string) => apiClient.post<TeacherLessonPlanResponse>('/teacher/portal/curriculum/lesson-plan/generate', { weekPlanId }).then((r) => r.data),
  schoolAdminCurriculumAssets: (repositoryType?: string) => apiClient.get<CurriculumAsset[]>('/school-admin/curriculum/assets', { params: { repositoryType } }).then((r) => r.data),
  createSchoolAdminCurriculumAsset: (payload: {
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
  }) => apiClient.post<CurriculumAsset>('/school-admin/curriculum/assets', payload).then((r) => r.data),
  downloadSchoolAdminCurriculumAsset: (assetId: string, format: 'PDF' | 'DOCX' | 'EXCEL') =>
    apiClient.get<Blob>(`/school-admin/curriculum/assets/${assetId}/download`, { params: { format }, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      fileName: parseContentDispositionFileName(r.headers['content-disposition']),
      contentType: r.headers['content-type'] || r.data.type || 'application/octet-stream',
    })),

  schoolAdminCommandDashboard: () => apiClient.get<SchoolAdminDashboardResponse>('/school-admin/dashboard').then((r) => r.data),
  schoolAdminAnalytics: () => apiClient.get<SchoolAdminAnalyticsResponse>('/school-admin/analytics').then((r) => r.data),
  schoolAdminAiInsights: () => apiClient.get<SchoolAdminAiInsightsResponse>('/school-admin/ai-insights').then((r) => r.data),
  schoolAdminLearners: (params?: { search?: string; grade?: string; className?: string }) => apiClient.get<SchoolAdminLearnerListResponse>('/school-admin/learners', { params }).then((r) => r.data),
  schoolAdminLearnerProfile: (learnerUserId: string) => apiClient.get<SchoolAdminLearnerProfileResponse>(`/school-admin/learners/${learnerUserId}`).then((r) => r.data),
  createSchoolAdminLearner: (payload: {
    email?: string;
    username?: string;
    password?: string;
    firstName: string;
    lastName: string;
    grade?: string;
    className?: string;
    careerGoal?: string;
    parentGuardianName?: string;
    parentGuardianPhone?: string;
    parentGuardianEmail?: string;
    popiaConsentAccepted?: boolean;
    consentVersion?: string;
  }) => apiClient.post<SchoolAdminLearner>('/school-admin/learners', payload).then((r) => r.data),
  importSchoolAdminLearners: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/school-admin/learners/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  exportLearnerCredentials: () => apiClient.get<LearnerCredentialsExportResponse>('/school-admin/learners/credentials').then((r) => r.data),
  schoolAdminTeachers: () => apiClient.get<SchoolAdminTeacherListResponse>('/school-admin/teachers').then((r) => r.data),
  schoolAdminTeacherDetail: (teacherId: string) => apiClient.get<TeacherDetailResponse>(`/school-admin/teachers/${teacherId}`).then((r) => r.data),
  schoolAdminTeacherAnalytics: () => apiClient.get<TeacherAdminDashboardResponse>('/school-admin/teachers/analytics').then((r) => r.data),
  schoolAdminTeacherEngagement: () => apiClient.get<TeacherActivityResponse>('/school-admin/teachers/engagement').then((r) => r.data),
  schoolAdminTeacherAiInsights: () => apiClient.get<TeacherAiInsightResponse>('/school-admin/teachers/ai-insights').then((r) => r.data),
  schoolAdminTeacherWorkload: () => apiClient.get<TeacherWorkloadResponse>('/school-admin/teachers/workload').then((r) => r.data),
  schoolAdminTeacherInterventions: () => apiClient.get<SchoolAdminInterventionReport[]>('/school-admin/teachers/interventions').then((r) => r.data),
  schoolAdminTeacherResources: () => apiClient.get<TeacherResourceResponse>('/school-admin/teachers/resources').then((r) => r.data),
  schoolAdminTeacherTraining: () => apiClient.get<TeacherTrainingResponse>('/school-admin/teachers/training').then((r) => r.data),
  schoolAdminClasses: () => apiClient.get<SchoolAdminClassListResponse>('/school-admin/classes').then((r) => r.data),
  schoolAdminClassProfile: (classId: string) => apiClient.get<SchoolAdminClassProfileResponse>(`/school-admin/classes/${classId}`).then((r) => r.data),
  schoolAdminClassAnalytics: (classId: string) => apiClient.get<SchoolAdminClassAnalyticsResponse>(`/school-admin/classes/${classId}/analytics`).then((r) => r.data),
  schoolAdminClassLearners: (classId: string) => apiClient.get<SchoolAdminLearner[]>(`/school-admin/classes/${classId}/learners`).then((r) => r.data),
  schoolAdminClassCareerReadiness: (classId: string) => apiClient.get<SchoolAdminClassCareerReadinessResponse>(`/school-admin/classes/${classId}/career-readiness`).then((r) => r.data),
  schoolAdminClassBursaries: (classId: string) => apiClient.get<SchoolAdminClassBursaryReadinessResponse>(`/school-admin/classes/${classId}/bursaries`).then((r) => r.data),
  schoolAdminClassInterventions: (classId: string) => apiClient.get<SchoolAdminInterventionReport[]>(`/school-admin/classes/${classId}/interventions`).then((r) => r.data),
  schoolAdminClassAiInsights: (classId: string) => apiClient.get<SchoolAdminClassAiInsightsResponse>(`/school-admin/classes/${classId}/ai-insights`).then((r) => r.data),
  schoolAdminCareerReadiness: () => apiClient.get<SchoolAdminCareerReadinessResponse>('/school-admin/career-readiness').then((r) => r.data),
  schoolAdminCourses: () => apiClient.get<SchoolAdminCoursesResponse>('/school-admin/courses').then((r) => r.data),
  schoolAdminBursaries: () => apiClient.get<SchoolAdminBursaryReadinessResponse>('/school-admin/bursaries').then((r) => r.data),
  schoolAdminInterventions: () => apiClient.get<SchoolAdminInterventionsResponse>('/school-admin/interventions').then((r) => r.data),
  createSchoolAdminIntervention: (payload: { learnerUserId: string; supportType: string; priority: string; notes: string; followUpDate?: string; status?: string }) =>
    apiClient.post<SchoolAdminInterventionReport>('/school-admin/interventions', payload).then((r) => r.data),
  updateSchoolAdminIntervention: (interventionId: string, payload: { status: string; notes: string; followUpDate?: string }) =>
    apiClient.patch<SchoolAdminInterventionReport>(`/school-admin/interventions/${interventionId}`, payload).then((r) => r.data),
  approveTeacher: (teacherId: string) => apiClient.post<SchoolAdminTeacher>(`/school-admin/teachers/${teacherId}/approve`).then((r) => r.data),
  rejectTeacher: (teacherId: string) => apiClient.post<SchoolAdminTeacher>(`/school-admin/teachers/${teacherId}/reject`).then((r) => r.data),
  suspendTeacherAdmin: (teacherId: string) => apiClient.post<SchoolAdminTeacher>(`/school-admin/teachers/${teacherId}/suspend`).then((r) => r.data),
  reactivateTeacherAdmin: (teacherId: string) => apiClient.post<SchoolAdminTeacher>(`/school-admin/teachers/${teacherId}/reactivate`).then((r) => r.data),
  deleteTeacherAdmin: (teacherId: string) => apiClient.delete(`/school-admin/teachers/${teacherId}`).then((r) => r.data),
  schoolAdminReports: () => apiClient.get<SchoolAdminReportItem[]>('/school-admin/reports').then((r) => r.data),
  exportSchoolAdminReport: (type: string, format: 'pdf' | 'xlsx') => apiClient.post<ReportExportResponse>('/school-admin/reports/export', { type, format }).then((r) => r.data),
  schoolAnnouncements: () => apiClient.get<SchoolAnnouncement[]>('/school-admin/announcements').then((r) => r.data),
  createSchoolAnnouncement: (payload: { audience: string; title: string; message: string }) => apiClient.post<SchoolAnnouncement>('/school-admin/announcements', payload).then((r) => r.data),
  schoolAdminNotifications: (params?: { page?: number; size?: number; status?: string; type?: string }) => apiClient.get<PaginatedResponse<UserNotification>>('/school-admin/notifications', { params }).then((r) => r.data),
  schoolSupportRequests: () => apiClient.get<SchoolSupportRequest[]>('/school-admin/support-requests').then((r) => r.data),
  createSchoolSupportRequest: (payload: { category: string; title: string; message: string; priority?: string }) => apiClient.post<SchoolSupportRequest>('/school-admin/support-requests', payload).then((r) => r.data),
  schoolAdminSettings: () => apiClient.get<SchoolAdminSettingsResponse>('/school-admin/settings').then((r) => r.data),
  schoolRegistrationStatus: () => apiClient.get<SchoolRegistrationStatus>('/school-registration/me').then((r) => r.data),
};
