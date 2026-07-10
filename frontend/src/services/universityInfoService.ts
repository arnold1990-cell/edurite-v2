import { apiClient } from '@/services/apiClient';

export interface UniversityInfoInstitutionSummary {
  id: string;
  slug: string;
  name: string;
  officialWebsite?: string | null;
  officialProgrammesUrl?: string | null;
  officialAdmissionsUrl?: string | null;
}

export interface UniversityProgrammeInfo {
  id: string;
  name: string;
  qualificationType?: string | null;
  faculty?: string | null;
  department?: string | null;
  duration?: string | null;
  studyMode?: string | null;
  campus?: string | null;
  programmeUrl?: string | null;
  sourceUrl: string;
  sourceLabel?: string | null;
  lastVerifiedAt?: string | null;
  retrievalStatus: string;
  active: boolean;
}

export interface UniversityAdmissionRequirementInfo {
  id: string;
  programmeName?: string | null;
  requirementTitle?: string | null;
  apsMinimum?: number | null;
  requiredSubjects: string[];
  minimumMarks: string[];
  nscRequirement?: string | null;
  languageRequirement?: string | null;
  facultySpecificRequirement?: string | null;
  internationalRequirement?: string | null;
  additionalTests?: string | null;
  sourceUrl: string;
  sourceLabel?: string | null;
  lastVerifiedAt?: string | null;
  retrievalStatus: string;
  active: boolean;
}

export interface UniversityRetrievalLogInfo {
  id: string;
  sourceUrl?: string | null;
  status: string;
  message?: string | null;
  retrievalType?: string | null;
  retrievedAt?: string | null;
}

export interface UniversityProgrammesView {
  institution: UniversityInfoInstitutionSummary;
  retrievalStatus: string;
  message: string;
  lastUpdatedAt?: string | null;
  cachedData: boolean;
  fallbackOnly: boolean;
  officialDomains: string[];
  availableFaculties: string[];
  availableQualificationTypes: string[];
  availableStudyModes: string[];
  programmes: UniversityProgrammeInfo[];
  retrievalLogs: UniversityRetrievalLogInfo[];
}

export interface UniversityAdmissionRequirementsView {
  institution: UniversityInfoInstitutionSummary;
  retrievalStatus: string;
  message: string;
  lastUpdatedAt?: string | null;
  cachedData: boolean;
  fallbackOnly: boolean;
  officialDomains: string[];
  requirements: UniversityAdmissionRequirementInfo[];
  retrievalLogs: UniversityRetrievalLogInfo[];
}

export interface UniversityRefreshResult {
  slug: string;
  status: string;
  message: string;
  refreshedAt: string;
}

export const universityInfoService = {
  programmes: (slug: string) => apiClient.get<UniversityProgrammesView>(`/student/universities/${slug}/programmes`).then((response) => response.data),
  admissionRequirements: (slug: string) => apiClient.get<UniversityAdmissionRequirementsView>(`/student/universities/${slug}/admission-requirements`).then((response) => response.data),
  refresh: (slug: string) => apiClient.post<UniversityRefreshResult>(`/admin/universities/${slug}/refresh`).then((response) => response.data),
};
