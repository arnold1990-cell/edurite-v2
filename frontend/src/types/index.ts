export type Role =
  | 'STUDENT'
  | 'COMPANY'
  | 'ADMIN'
  | 'DISTRICT_ADMIN'
  | 'DISTRICT_DIRECTOR'
  | 'CIRCUIT_MANAGER'
  | 'SUBJECT_ADVISOR'
  | 'SCHOOL_ADMIN'
  | 'TEACHER'
  | 'SCHOOL_STUDENT';
export type BackendRole = `ROLE_${Role}`;
export type PlanType = 'BASIC' | 'PREMIUM';

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'MORE_INFO_REQUIRED' | 'PENDING_DISTRICT_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';

export interface User { id: string; email: string; username?: string; fullName?: string; companyName?: string; schoolName?: string; roles: BackendRole[]; role?: Role; primaryRole?: BackendRole; approvalStatus?: ApprovalStatus; verified?: boolean; planType?: PlanType; passwordChangeRequired?: boolean; profileCompleted?: boolean; profileCompleteness?: number; }
export interface AuthResponse { accessToken: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; role?: string; primaryRole?: string; mustChangePassword?: boolean; message?: string; user: User; }
export interface AuthResponseRaw { accessToken?: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; role?: string; primaryRole?: string; approvalStatus?: string; mustChangePassword?: boolean; message?: string; roles?: string[]; user?: Partial<User> & { username?: string; role?: string; primaryRole?: string; approvalStatus?: string; mustChangePassword?: boolean; roles?: string[]; verified?: boolean; planType?: PlanType; profileCompleted?: boolean; profileCompleteness?: number }; }
export interface VerificationStatusResponse { message: string; }
export interface RegistrationResponse { message: string; email: string; verificationRequired: boolean; }

export interface StudentRegisterPayload { fullName?: string; firstName?: string; lastName?: string; email: string; password: string; interests?: string; location?: string; phone: string; dateOfBirth?: string; gender?: string; qualificationLevel?: string; popiaConsentAccepted: boolean; consentVersion: string; }
export interface CompanyRegisterPayload { companyName: string; registrationNumber: string; industry?: string; officialEmail: string; mobileNumber: string; contactPersonName: string; address?: string; website?: string; description?: string; popiaConsentAccepted: boolean; consentVersion: string; password: string; }
export interface SchoolRegisterPayload { schoolName: string; emisNumber: string; districtId: string; circuitId: string; schoolType: string; principalName: string; principalEmail: string; schoolEmail: string; phoneNumber: string; physicalAddress: string; password: string; confirmPassword: string; }
export interface LocationOption { id: string; name: string; code: string; }

export interface StudentProfile {
  id: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
  location?: string;
  bio?: string;
  qualificationLevel?: string;
  selectedGrade?: string;
  subjectAchievements?: StudentSubjectAchievement[];
  qualifications: string[];
  experience: string[];
  skills: string[];
  interests: string[];
  careerGoals?: string;
  cvFileUrl?: string;
  transcriptFileUrl?: string;
  profileCompleted: boolean;
  profileCompleteness: number;
}

export interface StudentSubjectAchievement {
  subjectName: string;
  achievementLevel?: number | null;
}

export type ReadinessStatus = 'GREEN' | 'ORANGE' | 'RED';

export interface StudentReadinessCard {
  score: number;
  status: ReadinessStatus;
  explanation: string;
  strengths: string[];
  gaps: string[];
  nextImprovements: string[];
}

export interface StudentTrackProgress {
  goalSet: boolean;
  goalTitle?: string | null;
  progressScore: number;
  status: ReadinessStatus;
  summary: string;
  strengths: string[];
  gaps: string[];
  nextMilestones: string[];
}

export interface StudentImprovementAction {
  id: string;
  title: string;
  reason: string;
  impactArea: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface StudentCommunicationGuidance {
  title: string;
  description: string;
  tips: string[];
}

export interface StudentPlanSummary {
  type?: PlanType;
  planCode: string;
  tier: PlanType;
  premium: boolean;
  careerSuggestionLimit?: number | null;
  upgradeMessage?: string | null;
}

export interface StudentDashboard {
  profileCompleteness: number;
  savedCareers: number;
  savedBursaries: number;
  savedOpportunities: number;
  activeApplications: number;
  applicationProgress: Array<{ label: string; count: number }>;
  skillGaps: string[];
  recommendedImprovements: string[];
  recommendedImprovementActions: StudentImprovementAction[];
  idealCareerObjective?: string | null;
  idealCareerObjectiveSet: boolean;
  idealCareerObjectiveEmptyStateMessage?: string | null;
  trackProgress: StudentTrackProgress;
  readiness: {
    fieldOfStudy: StudentReadinessCard;
    bursary: StudentReadinessCard;
    job: StudentReadinessCard;
  };
  communicationGuidance: StudentCommunicationGuidance;
  points: number;
  totalPoints: number;
  termCode: string;
  notifications: number;
  planType?: PlanType;
  subscriptionTier: PlanType | string;
  plan?: StudentPlanSummary;
}

export interface StudentSavedProfilePayload {
  firstName?: string;
  lastName?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
  location?: string;
  bio?: string;
  qualificationLevel?: string;
  selectedGrade?: string;
  subjectAchievements?: StudentSubjectAchievement[];
  qualifications?: string[];
  experience?: string[];
  skills?: string[];
  interests?: string[];
  careerGoals?: string;
}

export interface StudentSavedProfileSummary {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface StudentSavedProfile {
  id: string;
  name: string;
  profile: StudentSavedProfilePayload;
  createdAt: string;
  updatedAt: string;
}

export interface Career { id: string; title: string; description?: string; industry?: string; location?: string; qualificationLevel?: string; matchScore?: number; }
export interface Bursary { id: string; title: string; provider?: string; qualificationLevel?: string; region?: string; eligibility?: string; deadline?: string; status: string; }
export type OpportunityType = 'ALL' | 'CAREER' | 'JOB';
export interface UnifiedOpportunity {
  id: string;
  title: string;
  type: Exclude<OpportunityType, 'ALL'>;
  field?: string;
  industry?: string;
  qualification?: string;
  location?: string;
  demand?: string;
  saved: boolean;
  recommended: boolean;
}
export interface JobOpportunity {
  id: string;
  title: string;
  company: string;
  location: string;
  description: string;
  salaryMin?: number | null;
  salaryMax?: number | null;
  contractType?: string;
  category?: string;
  redirectUrl: string;
  created?: string;
  source: string;
}

export interface Course { id: string; name: string; institutionName: string; duration: string; level?: string; matchScore?: number; }
export interface Institution {
  id: string;
  name: string;
  abbreviation?: string;
  institutionType?: string;
  location?: string;
  city?: string;
  province?: string;
  country?: string;
  officialWebsite?: string;
  website?: string;
  applicationUrl?: string;
  logoPath?: string;
  logoUrl?: string;
  category?: string;
  description?: string;
  faculties?: string[] | string;
  programmeCount?: number | null;
  facultyCount?: number | null;
  applicationStatus?: 'OPEN' | 'OPENING_SOON' | 'CLOSED' | string | null;
  applicationOpeningDate?: string | null;
  applicationClosingDate?: string | null;
  qsRanking?: string | null;
  theRanking?: string | null;
  acceptanceIndicator?: string | null;
  featured?: boolean;
  active?: boolean;
}

export interface Application { id: string; status: string; createdAt: string; bursaryId: string; }
export interface RecommendationItem { id: string; title: string; score: number; rationale: string; }
export interface Recommendation {
  suggestedCareers: RecommendationItem[];
  suggestedBursaries: RecommendationItem[];
  suggestedCoursesOrImprovements: RecommendationItem[];
  profileImprovementTips: string[];
  modelVersion: string;
  planCode?: string;
  premiumUnlocked?: boolean;
  careerSuggestionLimit?: number | null;
  careerSuggestionsLimited?: boolean;
  upgradeMessage?: string | null;
}

export interface ProgressScoreCard { key: string; label: string; percentage: number; color: 'green' | 'orange' | 'red' | string; recommendation: string; }
export interface ProgressScoreResponse { overallPercentage: number; overallColor: string; cards: ProgressScoreCard[]; recommendations: string[]; }

export interface StudentCv {
  id?: string | null;
  personalSummary: string;
  education: string;
  skills: string;
  experience: string;
  projects: string;
  certifications: string;
  references: string;
  careerObjective: string;
  readinessPercentage: number;
  updatedAt?: string | null;
}
export interface StudentCvSuggestions { summarySuggestion: string; skillsImprovement: string; coverLetterDraft: string; jobReadinessTips: string; }

export interface ScholarshipApplication {
  id?: string;
  bursaryId?: string | null;
  scholarshipTitle: string;
  provider?: string;
  applicationDeadline?: string;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
  checklist?: string;
  requiredDocuments?: string;
  reminderNotes?: string;
  motivationLetterDraft?: string;
  saved?: boolean;
  notes?: string;
  deadlineSoon?: boolean;
  updatedAt?: string;
}

export interface TutorMessage { id?: string; sender: string; message: string; createdAt?: string; }
export interface TutorSession { id: string; subject: string; title: string; lastMessageAt?: string; messages: TutorMessage[]; }
export interface TutorAskResponse { sessionId: string; subject: string; answer: string; messages: TutorMessage[]; }

export interface UniversityApplication {
  id?: string;
  universityName: string;
  programmeName: string;
  country?: string;
  intakeYear?: number | null;
  applicationDeadline?: string;
  applicationStatus: 'DRAFT' | 'READY' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'WAITLISTED';
  notes?: string;
  documentReferences?: string;
  deadlineSoon?: boolean;
  updatedAt?: string;
}


export interface CareerRoadmap {
  id: string;
  slug: string;
  title: string;
  overview?: string;
  requiredSubjects?: string;
  recommendedSkills?: string;
  studyPath?: string;
  entryLevelJobs?: string;
  longTermGrowth?: string;
  learningResources?: string;
}

export interface ApsSubjectInput {
  subjectName: string;
  markPercentage?: number | null;
  level?: number | null;
  apsPoints?: number | null;
}

export interface ApsSubjectResult {
  subjectName: string;
  markPercentage?: number | null;
  level?: number | null;
  apsPoints?: number | null;
}

export interface ApsCalculationResponse {
  grade?: string;
  province?: string;
  subjects: ApsSubjectResult[];
  totalAps: number;
}

export interface CareerRoadmapOverview {
  description?: string;
  dailyResponsibilities: string[];
  skillsNeeded: string[];
  careerDemand?: string;
  salaryRange?: string;
  professionalBody?: string;
}

export interface CareerRoadmapSubjectRequirement {
  subject: string;
  minimumLevel?: number | null;
  minimumPass?: string;
  suggestedMark?: string;
  required: boolean;
  notes?: string;
}

export interface CareerRoadmapPathwayStep {
  title: string;
  description: string;
}

export interface CareerRoadmapTimelineStage {
  stage?: number | null;
  title: string;
  description: string;
}

export interface UniversityRequirementMatch {
  id?: string | null;
  institutionName: string;
  institutionType?: string;
  province?: string;
  qualificationName: string;
  faculty?: string;
  apsRequired?: number | null;
  mathematicsRequirement?: string;
  mathematicalLiteracyRequirement?: string;
  englishRequirement?: string;
  accountingRequirement?: string;
  physicalSciencesRequirement?: string;
  lifeSciencesRequirement?: string;
  duration?: string;
  nqfLevel?: string;
  applicationUrl?: string;
  notes?: string;
  source?: string;
  verified: boolean;
  verificationBadge: string;
  requirementStatus: 'Eligible' | 'Almost Eligible' | 'Not Yet Eligible' | string;
  apsGap?: number | null;
}

export interface CareerRoadmapReadiness {
  learnerAps: number;
  requiredAps: number;
  apsGap: number;
  readinessScore: number;
  status: 'Eligible' | 'Almost Eligible' | 'Not Yet Eligible' | string;
  bestFitUniversities: number;
}

export interface CareerRoadmapGapAnalysis {
  currentAps: number;
  requiredAps: number;
  apsGap: number;
  missingSubjects: string[];
  subjectsNeedingImprovement: string[];
  bestFitUniversities: string[];
  riskLevel: string;
  improvementSuggestions: string[];
}

export interface CareerRoadmapStudyPlanStep {
  title: string;
  focus: string;
  actions: string[];
}

export interface CareerRoadmapGenerateResponse {
  careerName: string;
  overview: CareerRoadmapOverview;
  requiredSubjects: CareerRoadmapSubjectRequirement[];
  recommendedSubjects: CareerRoadmapSubjectRequirement[];
  universityPathway: CareerRoadmapPathwayStep[];
  professionalPathway: CareerRoadmapPathwayStep[];
  roadmapTimeline: CareerRoadmapTimelineStage[];
  universityRequirements: UniversityRequirementMatch[];
  apsReadiness: CareerRoadmapReadiness;
  gapAnalysis: CareerRoadmapGapAnalysis;
  alternativePathways: string[];
  studyPlan: CareerRoadmapStudyPlanStep[];
}

export interface SavedCareerRoadmap {
  id: string;
  careerName: string;
  roadmap: CareerRoadmapGenerateResponse;
  learnerAps: number;
  requiredAps?: number | null;
  apsGap?: number | null;
  readinessScore: number;
  createdAt: string;
  updatedAt: string;
}

export interface SchoolProfile { id?: string; schoolName: string; country?: string; city?: string; contactPerson?: string; contactEmail?: string; notes?: string; }
export interface SchoolStudentSummary { studentId: string; name: string; qualificationLevel?: string; profileCompleteness: number; }
export interface SchoolSummary {
  schoolId: string;
  linkedStudents: number;
  psychometricCompleted: number;
  completeProfiles: number;
  tutorSessions: number;
  trackedApplications: number;
  students: SchoolStudentSummary[];
}

export interface CareerAdviceRequest {
  qualificationLevel: string;
  interests: string;
  skills: string;
  location: string;
}

export interface CareerAdviceItem {
  name: string;
  matchScore: number;
  reason: string;
  improvements: string[];
}

export interface CareerAdviceResponse {
  recommendedCareers: CareerAdviceItem[];
  planCode?: string;
  premiumUnlocked?: boolean;
  careerSuggestionLimit?: number | null;
  careerSuggestionsLimited?: boolean;
  upgradeMessage?: string | null;
}

export interface UniversitySourcesAnalysisRequest {
  urls?: string[];
  targetProgram?: string;
  careerInterest?: string;
  qualificationLevel?: string;
  maxRecommendations?: number;
}

export interface UniversityRecommendedCareer {
  name: string;
  reason: string;
  requirements: string[];
  relatedProgrammes: string[];
  recommendationReason?: string | null;
  confidenceLevel?: string | null;
  verifiedFacts?: string[];
  inferredInsights?: string[];
  missingData?: string[];
  sourceStatus?: string | null;
  rankingCategory?: string | null;
  nextBestActions?: string[];
}

export interface UniversityRecommendedProgramme {
  name: string;
  university: string;
  admissionRequirements: string[];
  notes: string;
  recommendationReason?: string | null;
  confidenceLevel?: string | null;
  verifiedFacts?: string[];
  inferredInsights?: string[];
  missingData?: string[];
  sourceStatus?: string | null;
  rankingCategory?: string | null;
  nextBestActions?: string[];
}

export interface UniversitySourceDiagnostic {
  sourceUrl: string;
  fetchStatus: string;
  failureReason?: string | null;
  university?: string | null;
  usableProgrammeData?: boolean;
}

export interface UniversitySourceCoverage {
  requestedSourcesCount: number;
  successfulSourcesCount: number;
  failedSourcesCount: number;
  partialSourcesCount: number;
  universitiesWithUsableProgrammeData: string[];
}

export interface UniversitySourcesAnalysisResponse {
  aiLive: boolean;
  fallbackUsed: boolean;
  available?: boolean;
  message?: string | null;
  status?: 'SUCCESS' | 'PARTIAL' | 'ERROR';
  mode?: string;
  warningMessage?: string | null;
  requestedSources?: string[];
  sourceUrls: string[];
  successfullyAnalysedUrls: string[];
  failedUrls: string[];
  totalSourcesUsed: number;
  summary: string;
  recommendedCareers: UniversityRecommendedCareer[];
  recommendedProgrammes: UniversityRecommendedProgramme[];
  recommendedUniversities: string[];
  minimumRequirements: string[];
  keyRequirements: string[];
  skillGaps: string[];
  recommendedNextSteps: string[];
  warnings: string[];
  suitabilityScore: number;
  rawModelUsed: string;
  suitabilityScoreReason?: string | null;
  suitabilitySignalsUsed?: string[];
  suitabilityScoreLimitations?: string[];
  sourceDiagnostics?: UniversitySourceDiagnostic[];
  sourceCoverage?: UniversitySourceCoverage | null;
  planCode?: string;
  premiumUnlocked?: boolean;
  careerSuggestionLimit?: number | null;
  careerSuggestionsLimited?: boolean;
  upgradeMessage?: string | null;
}
export interface Notification { id: string; title: string; message: string; read?: boolean; type?: string; createdAt?: string; isRead?: boolean; priority?: string; }
export type PaymentProviderCode = 'paypal' | 'payfast' | 'mock' | 'internal';

export interface Subscription {
  id: string;
  planCode: string;
  status: string;
  renewalDate: string;
  premiumAccess?: boolean;
  trialActive?: boolean;
  trialStartDate?: string | null;
  trialEndDate?: string | null;
  accessMessage?: string | null;
  provider?: string;
  providerSubscriptionId?: string;
  paymentReference?: string;
}

export interface SubscriptionCheckoutPayload {
  planCode: string;
  provider: PaymentProviderCode;
}

export interface SubscriptionCheckoutResponse {
  paymentReference: string;
  provider: string;
  paymentStatus: string;
  subscriptionStatus: string;
  checkoutUrl?: string | null;
  message: string;
  checkoutPayload?: Record<string, unknown> | null;
}

export interface PayFastInitiatePayload {
  planCode: string;
}

export interface PayFastInitiateResponse {
  paymentReference: string;
  provider: string;
  paymentStatus: string;
  subscriptionStatus: string;
  paymentUrl: string;
  formFields: Record<string, string>;
  message: string;
}

export interface SubscriptionPaymentConfirmPayload {
  paymentReference: string;
  provider?: string;
  sessionId?: string;
  orderId?: string;
  token?: string;
  payerId?: string;
}

export interface SubscriptionPaymentCancelPayload {
  paymentReference: string;
  provider?: string;
  reason?: string;
}

export interface SubscriptionPaymentStatusResponse {
  paymentReference: string;
  provider: string;
  paymentStatus: string;
  subscriptionStatus: string;
  message: string;
}

export interface PricingPlan {
  id?: string;
  planId?: string;
  code: string;
  name: string;
  description?: string;
  currency: string;
  price?: number;
  amount: number;
  billingPeriod?: string;
  billingInterval: string;
  premium: boolean;
  recommended?: boolean;
  features: string[];
}
export interface PsychometricAnswerItem { dimension: string; score: number; }
export interface PsychometricResult { id: string; submissionMode: string; scores: Record<string, number>; strengthAreas: string[]; growthAreas: string[]; interpretation: string; createdAt: string; }
export interface PsychometricAssessment { id: string; code: string; name: string; description?: string; version: string; publicAvailable: boolean; questionCount: number; }
export interface PsychometricQuestion { id: string; questionKey: string; prompt: string; dimensionKey: string; minScore: number; maxScore: number; displayOrder: number; }
export interface PsychometricAttemptResult { attemptId: string; assessmentId: string; scores: Record<string, number>; strengthAreas: string[]; growthAreas: string[]; interpretation: string; submittedAt: string; }
export interface LearningResource {
  id: string;
  title: string;
  description?: string;
  provider?: string;
  category?: string;
  subject?: string;
  grade?: string;
  level?: string;
  resourceType?: string;
  duration?: string;
  thumbnailUrl?: string;
  externalUrl?: string;
  progress?: number;
  instructor?: string;
  lessons?: string[];
  summary?: string;
  url?: string;
  difficulty?: string;
  estimatedMinutes?: number;
  tags?: string[];
  mappedOutcomes?: string[];
  language?: string;
  isFree?: boolean;
  sourceType?: string;
  lastFetchedAt?: string;
}
export interface GamificationSummary {
  totalPoints: number;
  reservedPoints: number;
  availablePoints: number;
  currentTermCode: string;
  recentEvents: Array<{ eventType: string; points: number; awardedAt: string; referenceId?: string }>;
  recentClaims: Array<{ rewardName: string; status: string; claimedPoints: number; claimedAt: string }>;
  activeRules: Array<{ code: string; name: string; description?: string; eventType: string; pointsPerEvent: number; maxPerTerm?: number }>;
}
export interface PaginatedResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface ApiError { message: string; status?: number; details?: Record<string, string[]>; code?: string; }



