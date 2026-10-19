import type { StudentSubjectAchievement } from "./index";
export interface AcademicAnalysis {
  progression?: {
    subject: string;
    curriculum: string;
    previousYear: string;
    currentYear: string;
    previousMark: number;
    currentMark: number;
    percentagePointChange: number;
  }[];
  grade?: string;
  percentageAverage: number | null;
  subjects: StudentSubjectAchievement[];
  relativeStrengths: StudentSubjectAchievement[];
  improvementPriorities: StudentSubjectAchievement[];
  curricula: string[];
  interests: string[];
  skills: string[];
  goals?: string;
  limitations: string[];
}
export interface ProgrammeEligibility {
  status: string;
  category?: string;
  studentAps: number | null;
  requiredAps: number | null;
  apsRule?: string;
  subjects: {
    subject: string;
    studentResult: string;
    requirement: string;
    status: string;
    reviewed: boolean;
  }[];
  gaps: string[];
  uncheckedConditions: string[];
}
export interface ResearchProgramme {
  id: string;
  institution: string;
  country?: string;
  name: string;
  qualification?: string;
  duration?: string;
  campus?: string;
  studyMode?: string;
  programmeUrl?: string;
  applicationUrl?: string;
  requirements?: string;
  sourceUrl?: string;
  verifiedAt?: string;
  verificationStatus: string;
  eligibility: ProgrammeEligibility;
  careerIds: string[];
  publishedEvidence?: {
    minimumAps?: number;
    apsRule?: string;
    subjectRequirements: Record<string, string>;
    intakeYear?: number;
    publishedRequirements?: string;
    applicationOpening?: string;
    applicationClosing?: string;
    tuitionFees?: string;
    campus?: string;
    faculty?: string;
    applicationUrl?: string;
    fundingLinks: Record<string, string>;
    evidenceUrl: string;
    limitations: string[];
  } | null;
}
export interface ResearchCareer {
  id: string;
  title: string;
  category: string;
  description: string;
  rankingWeight: number;
  suitability: string;
  explanation: string;
  relevantSubjects: string[];
  strengths: string[];
  improvementAreas: string[];
  qualifications: string[];
  programmeIds: string[];
}
export interface CareerResearchReport {
  schemaVersion: string;
  generatedAt: string;
  academic: AcademicAnalysis;
  careers: ResearchCareer[];
  programmes: ResearchProgramme[];
  alternativePathways: ResearchProgramme[];
  funding: {
    externalId: string;
    title: string;
    provider?: string;
    description?: string;
    eligibility?: string;
    deadline?: string;
    applicationLink?: string;
    dataFreshnessNote?: string;
  }[];
  interpretation: {
    assessment: string;
    researchSummary: string;
    nextSteps: string[];
    sourceUrls: string[];
  };
  sources: {
    url: string;
    institution: string;
    verifiedAt?: string;
    status: string;
  }[];
  inputsUsed: string[];
  limitations: string[];
  rankingMethod: string;
}
export interface CareerResearchJob {
  jobId: string | null;
  status: "IDLE" | "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "STALE";
  stage?: string;
  updatedAt?: string;
  error?: string;
  report: CareerResearchReport | null;
}
