import axios, { type AxiosRequestConfig } from 'axios';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApiError, ApprovalStatus, BackendRole, User } from '@/types';

const DEFAULT_API_PATH = '/api';
type ApiRequestConfig = AxiosRequestConfig & { __authBaseRetried?: boolean };
const normalizeBasePath = (value: string) => value.replace(/\/+$/, '');
const extractErrorMessages = (errors: unknown): string[] => {
  if (!errors || typeof errors !== 'object') return [];
  return Object.values(errors as Record<string, unknown>)
    .flatMap((entry) => Array.isArray(entry) ? entry : [entry])
    .filter((entry): entry is string => typeof entry === 'string' && entry.trim().length > 0);
};
const isLikelyHtmlError = (value: string) => /<html|<!doctype html/i.test(value);
const toLegacyAuthBaseUrl = (value: string): string | null => {
  const normalized = normalizeBasePath(value);
  if (normalized.endsWith('/api/v1')) {
    return normalized.replace(/\/api\/v1$/, '/api');
  }
  return null;
};
const isAuthPath = (url?: string) => Boolean(url && /^\/?auth(?:\/|$)/.test(url));
const isRefreshPath = (url?: string) => Boolean(url && /\/auth\/refresh(?:\?.*)?$/.test(url));
const isEmptyResponsePayload = (payload: unknown): boolean => {
  if (payload == null) return true;
  if (typeof payload === 'string') return payload.trim() === '';
  if (typeof payload === 'object') return Object.keys(payload).length === 0;
  return false;
};
const resolveApiErrorMessage = (
  responseStatus: number | undefined,
  responseBody: unknown,
  fallbackErrorCode?: string,
): string => {
  if (responseBody && typeof responseBody === 'object') {
    const body = responseBody as { message?: unknown; error?: unknown; errors?: unknown };
    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message.trim();
    }
    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error.trim();
    }
    const validationMessages = extractErrorMessages(body.errors);
    if (validationMessages.length) {
      return validationMessages.join(', ');
    }
  }

  if (typeof responseBody === 'string' && responseBody.trim() && !isLikelyHtmlError(responseBody)) {
    return responseBody.trim();
  }

  if (fallbackErrorCode === 'ERR_NETWORK') {
    return 'Could not connect to the server.';
  }

  if (fallbackErrorCode === 'ECONNABORTED') {
    return 'Request timed out. Please try again.';
  }

  switch (responseStatus) {
    case 400:
      return 'Invalid request. Please check your input and try again.';
    case 401:
      return 'Invalid email/username or password.';
    case 403:
      return 'You do not have permission to access this resource.';
    case 404:
      return 'The requested service is not available right now.';
    case 422:
      return 'Validation failed. Please check your input and try again.';
    default:
      if ((responseStatus ?? 0) >= 500) {
        return 'Server error. Please try again in a moment.';
      }
      return 'Unexpected error occurred';
  }
};

const deriveApiBaseUrl = () => {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  if (!configuredBaseUrl) {
    return DEFAULT_API_PATH;
  }

  return normalizeBasePath(configuredBaseUrl);
};

const baseURL = deriveApiBaseUrl();
const normalizedBaseURL = normalizeBasePath(baseURL);
const legacyAuthBaseURL = toLegacyAuthBaseUrl(normalizedBaseURL);
let refreshRequest: Promise<string | null> | null = null;

const normalizeApprovalStatus = (status?: string | null): ApprovalStatus | undefined => {
  if (!status) return undefined;
  return ['PENDING', 'APPROVED', 'REJECTED', 'MORE_INFO_REQUIRED', 'SUSPENDED'].includes(status) ? status as ApprovalStatus : undefined;
};

const normalizePlanType = (planType?: string | null): User['planType'] | undefined => {
  if (!planType) return undefined;
  const normalized = planType.trim().toUpperCase();
  if (normalized === 'PREMIUM') return 'PREMIUM';
  if (normalized === 'BASIC') return 'BASIC';
  return undefined;
};

const normalizeRefreshedUser = (payload: unknown): User | null => {
  if (!payload || typeof payload !== 'object') return null;

  const data = payload as { user?: { id?: string; email?: string; username?: string; fullName?: string; companyName?: string; schoolName?: string; roles?: string[]; role?: string; primaryRole?: string; approvalStatus?: string; verified?: boolean; passwordChangeRequired?: boolean; profileCompleted?: boolean; profileCompleteness?: number; planType?: string }; roles?: string[]; role?: string; primaryRole?: string; approvalStatus?: string; mustChangePassword?: boolean };
  const rawRoles = data.user?.roles ?? data.roles ?? (data.user?.role ? [data.user.role] : data.role ? [data.role] : []);
  const roles = Array.from(new Set(rawRoles
    .map((role) => normalizeBackendRole(role))
    .filter((role): role is BackendRole => Boolean(role))));

  if (!roles.length) return null;

  return {
    id: data.user?.id ?? '',
    email: data.user?.email ?? '',
    username: data.user?.username,
    fullName: data.user?.fullName,
    companyName: data.user?.companyName,
    schoolName: data.user?.schoolName,
    roles,
    role: (normalizeBackendRole(data.user?.primaryRole ?? data.primaryRole ?? data.user?.role ?? data.role) ?? roles[0])?.replace('ROLE_', '') as User['role'] | undefined,
    primaryRole: normalizeBackendRole(data.user?.primaryRole ?? data.primaryRole ?? data.user?.role ?? data.role) ?? roles[0],
    approvalStatus: normalizeApprovalStatus(data.user?.approvalStatus ?? data.approvalStatus),
    verified: data.user?.verified,
    planType: normalizePlanType(data.user?.planType),
    passwordChangeRequired: data.user?.passwordChangeRequired ?? data.mustChangePassword,
    profileCompleted: data.user?.profileCompleted,
    profileCompleteness: data.user?.profileCompleteness,
  };
};

export const apiClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const requestConfig = error.config as ApiRequestConfig | undefined;
    const requestMethod = requestConfig?.method?.toUpperCase() ?? 'UNKNOWN';
    const requestUrl = requestConfig?.url ?? 'UNKNOWN_URL';
    const responseStatus = error.response?.status;
    const responseBody = error.response?.data;
    const isAuthEndpoint = isAuthPath(requestConfig?.url);
    const legacyAuthRetryEligible = isAuthEndpoint
      && !requestConfig?.__authBaseRetried
      && Boolean(legacyAuthBaseURL)
      && [401, 404].includes(responseStatus ?? 0)
      && isEmptyResponsePayload(responseBody);

    if (legacyAuthRetryEligible && requestConfig && legacyAuthBaseURL) {
      if (import.meta.env.DEV) {
        console.warn('[api] retrying auth request using legacy /api/auth path', { method: requestMethod, url: requestUrl, fromBaseURL: normalizedBaseURL, toBaseURL: legacyAuthBaseURL });
      }
      return apiClient.request({
        ...requestConfig,
        baseURL: legacyAuthBaseURL,
        __authBaseRetried: true,
      } as ApiRequestConfig);
    }

    if (responseStatus === 401 && authStore.getRefreshToken() && !isRefreshPath(requestConfig?.url)) {
      try {
        if (!refreshRequest) {
          refreshRequest = (async () => {
            const refreshPayload = {
              refreshToken: authStore.getRefreshToken(),
            };
            let response;
            try {
              response = await axios.post(`${normalizedBaseURL}/auth/refresh`, refreshPayload);
            } catch (refreshError) {
              const refreshStatus = (refreshError as { response?: { status?: number } }).response?.status;
              const refreshBody = (refreshError as { response?: { data?: unknown } }).response?.data;
              const shouldTryLegacyRefresh = Boolean(legacyAuthBaseURL)
                && [401, 404].includes(refreshStatus ?? 0)
                && isEmptyResponsePayload(refreshBody);
              if (!shouldTryLegacyRefresh || !legacyAuthBaseURL) {
                throw refreshError;
              }
              response = await axios.post(`${legacyAuthBaseURL}/auth/refresh`, refreshPayload);
            }

            const persistSession = authStore.shouldPersistSession();
            authStore.setTokens(response.data.accessToken, response.data.refreshToken, persistSession);
            const refreshedUser = normalizeRefreshedUser(response.data);
            if (refreshedUser) {
              if (import.meta.env.DEV) {
                console.info('[auth] restored session role', { email: refreshedUser.email, roles: refreshedUser.roles, primaryRole: refreshedUser.primaryRole });
              }
              authStore.setUser(refreshedUser, persistSession);
            }
            return response.data.accessToken as string;
          })().finally(() => {
            refreshRequest = null;
          });
        }

        const refreshedAccessToken = await refreshRequest;
        if (refreshedAccessToken && requestConfig?.headers) {
          requestConfig.headers.Authorization = `Bearer ${refreshedAccessToken}`;
        }
        if (!requestConfig) {
          return Promise.reject(error);
        }
        return apiClient.request(requestConfig);
      } catch {
        authStore.clear();
      }
    }
    if (import.meta.env.DEV) {
      console.error(`[api] ${requestMethod} ${requestUrl} failed`, { status: responseStatus, response: responseBody, error, baseURL });
    }

    const message = resolveApiErrorMessage(responseStatus, responseBody, error.code);

    if (import.meta.env.DEV && message === 'Unexpected error occurred') {
      console.warn('[api] unable to derive useful error message', { status: responseStatus, responseBody, requestMethod, requestUrl });
    }

    const bodyWithMetadata = responseBody as { errors?: Record<string, string[]>; code?: string } | undefined;
    const normalized: ApiError = {
      message,
      status: error.response?.status,
      details: bodyWithMetadata?.errors,
      code: bodyWithMetadata?.code,
    };
    return Promise.reject(Object.assign(new Error(message), normalized));
  },
);
