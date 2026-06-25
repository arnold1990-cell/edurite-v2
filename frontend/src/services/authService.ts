import { apiClient } from '@/services/apiClient';
import { authStore } from '@/features/auth/authStore';
import { normalizeBackendRole } from '@/features/auth/roleUtils';
import type { ApprovalStatus, AuthResponse, AuthResponseRaw, BackendRole, CompanyRegisterPayload, RegistrationResponse, SchoolRegisterPayload, StudentRegisterPayload, User, VerificationStatusResponse } from '@/types';

const decodeBase64Url = (value: string): string | null => {
  try {
    const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
    return atob(padded);
  } catch {
    return null;
  }
};

const getTokenPayload = (accessToken?: string): { roles?: unknown; role?: unknown; authorities?: unknown; primaryRole?: unknown; approvalStatus?: unknown; planType?: unknown } | null => {
  if (!accessToken) return null;

  const [, payload] = accessToken.split('.');
  if (!payload) return null;

  const decodedPayload = decodeBase64Url(payload);
  if (!decodedPayload) return null;

  try {
    return JSON.parse(decodedPayload) as { roles?: unknown; role?: unknown; authorities?: unknown; primaryRole?: unknown; approvalStatus?: unknown; planType?: unknown };
  } catch {
    return null;
  }
};

const getRolesFromAccessToken = (accessToken?: string): string[] => {
  const claims = getTokenPayload(accessToken);
  if (!claims) return [];
  if (Array.isArray(claims.roles)) return claims.roles.filter((role): role is string => typeof role === 'string');
  if (Array.isArray(claims.authorities)) return claims.authorities.filter((role): role is string => typeof role === 'string');
  if (typeof claims.role === 'string') return [claims.role];
  return [];
};

const normalizeApprovalStatus = (status?: string | null): ApprovalStatus | undefined => {
  if (!status) return undefined;
  return ['PENDING', 'APPROVED', 'MORE_INFO_REQUIRED', 'PENDING_DISTRICT_APPROVAL', 'ACTIVE', 'REJECTED', 'SUSPENDED'].includes(status) ? status as ApprovalStatus : undefined;
};

const normalizePlanType = (planType?: string | null): User['planType'] | undefined => {
  if (!planType) return undefined;
  const normalized = planType.trim().toUpperCase();
  if (normalized === 'PREMIUM') return 'PREMIUM';
  if (normalized === 'BASIC') return 'BASIC';
  return undefined;
};

const normalizeRoles = (roles: string[]): BackendRole[] => Array.from(
  new Set(roles.map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

const normalizeLoginIdentifier = (value: string): string => {
  const trimmed = value.trim();
  return trimmed.includes('@') ? trimmed.toLowerCase() : trimmed;
};

export const normalizeAuthResponse = (payload: AuthResponseRaw): AuthResponse => {
  const tokenPayload = getTokenPayload(payload.accessToken);
  const responseRoles = payload.user?.roles ?? payload.roles ?? (payload.user?.role ? [payload.user.role] : payload.role ? [payload.role] : []);
  const normalizedRoles = normalizeRoles([...responseRoles, ...getRolesFromAccessToken(payload.accessToken)]);
  const normalizedPrimaryRole = normalizeBackendRole(payload.user?.primaryRole ?? payload.primaryRole ?? payload.user?.role ?? payload.role);
  const approvalStatus = normalizeApprovalStatus(payload.user?.approvalStatus ?? payload.approvalStatus ?? (typeof tokenPayload?.approvalStatus === 'string' ? tokenPayload.approvalStatus : undefined));

  const user: User = {
    id: payload.user?.id ?? '',
    email: payload.user?.email ?? '',
    username: payload.user?.username,
    fullName: payload.user?.fullName,
    companyName: payload.user?.companyName,
    schoolName: payload.user?.schoolName,
    roles: normalizedRoles,
    role: (normalizedPrimaryRole ?? normalizedRoles[0])?.replace('ROLE_', '') as User['role'] | undefined,
    primaryRole: normalizedPrimaryRole ?? normalizedRoles[0],
    approvalStatus,
    verified: payload.user?.verified,
    planType: normalizePlanType(
      payload.user?.planType
      ?? (typeof tokenPayload?.planType === 'string' ? tokenPayload.planType : undefined),
    ),
    passwordChangeRequired: payload.user?.mustChangePassword ?? payload.mustChangePassword,
    profileCompleted: payload.user?.profileCompleted,
    profileCompleteness: payload.user?.profileCompleteness,
  };

  return {
    accessToken: payload.accessToken ?? '',
    refreshToken: payload.refreshToken,
    tokenType: payload.tokenType,
    accessTokenExpiresIn: payload.accessTokenExpiresIn,
    role: user.role,
    primaryRole: user.primaryRole,
    mustChangePassword: payload.mustChangePassword ?? user.passwordChangeRequired,
    message: payload.message,
    user,
  };
};

export const authService = {
  me: () => apiClient.get<AuthResponseRaw>('/auth/me').then((response) => normalizeAuthResponse(response.data)),
  login: (payload: { email?: string; schoolName?: string; emisNumber?: string; password: string }) => {
    authStore.clear();
    return apiClient.post<AuthResponseRaw>('/auth/login', {
      email: payload.email ? normalizeLoginIdentifier(payload.email) : undefined,
      schoolName: payload.schoolName?.trim() || undefined,
      emisNumber: payload.emisNumber?.trim() || undefined,
      password: payload.password,
    }).then((response) => normalizeAuthResponse(response.data));
  },
  googleSignIn: (idToken: string, role: 'STUDENT' | 'COMPANY' = 'STUDENT') => {
    authStore.clear();
    return apiClient.post<AuthResponseRaw>('/auth/google', { idToken, role }).then((response) => normalizeAuthResponse(response.data));
  },
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<RegistrationResponse>('/auth/register/student', payload).then((r) => r.data),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<RegistrationResponse>('/auth/register/company', payload).then((r) => r.data),
  registerSchool: (payload: SchoolRegisterPayload) => apiClient.post<RegistrationResponse>('/auth/register/school', payload).then((r) => r.data),
  verifyOtp: (payload: { phoneNumber: string; code: string }) => apiClient.post<VerificationStatusResponse>('/auth/verify-otp', payload).then((r) => r.data),
  resendVerificationOtp: (payload: { phoneNumber: string }) => apiClient.post<VerificationStatusResponse>('/auth/resend-verification-otp', payload).then((r) => r.data),
  requestForgotPasswordOtp: (payload: { accountIdentifier?: string; phoneNumber?: string }) => apiClient.post<VerificationStatusResponse>('/auth/forgot-password/request-otp', payload).then((r) => r.data),
  resetPasswordWithOtp: (payload: { phoneNumber: string; code: string; newPassword: string }) => apiClient.post<VerificationStatusResponse>('/auth/forgot-password/reset', payload).then((r) => r.data),
  logout: () => apiClient.post('/auth/logout'),
  keepAlive: () => apiClient.post<{ status: string; serverTime: string; message: string }>('/auth/keep-alive').then((r) => r.data),
};
