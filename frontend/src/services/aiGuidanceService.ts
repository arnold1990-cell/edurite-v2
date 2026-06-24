import { apiClient } from '@/services/apiClient';
import type {
  CareerAdviceRequest,
  CareerAdviceResponse,
  Recommendation,
  UniversitySourcesAnalysisRequest,
  UniversitySourcesAnalysisResponse,
} from '@/types';

const demoModeEnabled = false;
export const AI_ERROR_MESSAGE = 'Our AI could not process your request at this time. Please try again.';
const FRIENDLY_AI_UNAVAILABLE_MESSAGE = 'AI guidance is temporarily unavailable. Please try again later.';

const normalizeWarningMessage = (warning?: string | null): string | null | undefined => {
  if (!warning) return warning;
  const normalized = warning.toLowerCase();
  if (
    normalized.includes('invalid provider response')
    || normalized.includes('gemini_model')
    || normalized.includes('gemini_api_key')
    || normalized.includes('live ai guidance')
    || normalized.includes('provider rejected')
    || normalized.includes('provider configuration')
  ) {
    return FRIENDLY_AI_UNAVAILABLE_MESSAGE;
  }
  return warning;
};

const normalizeUniversityResponse = (payload: UniversitySourcesAnalysisResponse): UniversitySourcesAnalysisResponse => {
  const requestedSources = payload.requestedSources ?? payload.sourceUrls ?? [];
  const sourceUrls = payload.sourceUrls ?? requestedSources;
  const sourceCoverage = payload.sourceCoverage ?? null;
  const warningMessage = normalizeWarningMessage(payload.warningMessage);
  const warnings = Array.from(new Set((payload.warnings ?? []).map((warning) => normalizeWarningMessage(warning) ?? '').filter(Boolean)));

  return {
    ...payload,
    available: payload.available ?? payload.aiLive ?? false,
    message: payload.message ?? warningMessage ?? null,
    status: payload.status ?? (payload.mode === 'PARTIAL' ? 'PARTIAL' : payload.mode === 'UNAVAILABLE' ? 'ERROR' : payload.fallbackUsed ? 'PARTIAL' : 'SUCCESS'),
    mode: payload.mode ?? (payload.fallbackUsed ? 'FALLBACK' : payload.aiLive ? 'LIVE' : 'UNAVAILABLE'),
    requestedSources,
    sourceUrls,
    successfullyAnalysedUrls: payload.successfullyAnalysedUrls ?? [],
    failedUrls: payload.failedUrls ?? [],
    recommendedCareers: payload.recommendedCareers ?? [],
    recommendedProgrammes: payload.recommendedProgrammes ?? [],
    recommendedUniversities: payload.recommendedUniversities ?? [],
    minimumRequirements: payload.minimumRequirements ?? [],
    keyRequirements: payload.keyRequirements ?? [],
    skillGaps: payload.skillGaps ?? [],
    recommendedNextSteps: payload.recommendedNextSteps ?? [],
    warningMessage,
    warnings,
    suitabilitySignalsUsed: payload.suitabilitySignalsUsed ?? [],
    suitabilityScoreLimitations: payload.suitabilityScoreLimitations ?? [],
    sourceDiagnostics: payload.sourceDiagnostics ?? [],
    sourceCoverage,
    totalSourcesUsed: payload.totalSourcesUsed ?? payload.successfullyAnalysedUrls?.length ?? sourceCoverage?.successfulSourcesCount ?? 0,
    planCode: payload.planCode ?? 'PLAN_BASIC',
    premiumUnlocked: payload.premiumUnlocked ?? false,
    careerSuggestionLimit: payload.careerSuggestionLimit ?? null,
    careerSuggestionsLimited: payload.careerSuggestionsLimited ?? false,
    upgradeMessage: payload.upgradeMessage ?? null,
  };
};

export const aiGuidanceService = {
  demoModeEnabled,
  getCareerAdvice: (payload: CareerAdviceRequest) =>
    apiClient.post<CareerAdviceResponse>('/ai/career-advice', payload).then((r) => r.data),
  analyseUniversitySources: (payload: UniversitySourcesAnalysisRequest) =>
    apiClient.post<UniversitySourcesAnalysisResponse>('/ai/analyse-university-sources', payload).then((r) => normalizeUniversityResponse(r.data)),
  getDefaultUniversitySources: () =>
    apiClient.get<string[]>('/ai/default-university-sources').then((r) => r.data),
  getDemoGuidance: () => apiClient.get<Recommendation>('/recommendations/me').then((r) => r.data),
};
