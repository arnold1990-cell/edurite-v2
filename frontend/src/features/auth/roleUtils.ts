import type { ApprovalStatus, BackendRole, Role, User } from '@/types';

const ROLE_PRIORITY: Role[] = ['ADMIN', 'DISTRICT_ADMIN', 'DISTRICT_DIRECTOR', 'CIRCUIT_MANAGER', 'SUBJECT_ADVISOR', 'SCHOOL_ADMIN', 'TEACHER', 'COMPANY', 'SCHOOL_STUDENT', 'STUDENT'];
const DASHBOARD_PATHS: Record<Role, string> = {
  ADMIN: '/admin/dashboard',
  DISTRICT_ADMIN: '/district/dashboard',
  DISTRICT_DIRECTOR: '/district/dashboard',
  CIRCUIT_MANAGER: '/district/circuit/dashboard',
  SUBJECT_ADVISOR: '/district/advisor/dashboard',
  SCHOOL_ADMIN: '/school/dashboard',
  TEACHER: '/teacher/dashboard',
  COMPANY: '/company/dashboard',
  SCHOOL_STUDENT: '/school-student/dashboard',
  STUDENT: '/student/dashboard',
};

const ROLE_PATH_PREFIXES: Record<Role, string> = {
  ADMIN: '/admin/',
  DISTRICT_ADMIN: '/district/',
  DISTRICT_DIRECTOR: '/district/',
  CIRCUIT_MANAGER: '/district/',
  SUBJECT_ADVISOR: '/district/',
  SCHOOL_ADMIN: '/school/',
  TEACHER: '/teacher/',
  COMPANY: '/company/',
  SCHOOL_STUDENT: '/school-student/',
  STUDENT: '/student/',
};

export const normalizeBackendRole = (role?: string | null): BackendRole | null => {
  if (!role) return null;
  const normalized = role.trim().toUpperCase();
  const prefixed = normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`;
  return ['ROLE_STUDENT', 'ROLE_COMPANY', 'ROLE_ADMIN', 'ROLE_DISTRICT_ADMIN', 'ROLE_DISTRICT_DIRECTOR', 'ROLE_CIRCUIT_MANAGER', 'ROLE_SUBJECT_ADVISOR', 'ROLE_SCHOOL_ADMIN', 'ROLE_TEACHER', 'ROLE_SCHOOL_STUDENT'].includes(prefixed) ? (prefixed as BackendRole) : null;
};

export const getNormalizedUserRoles = (user: Pick<User, 'roles'> | null | undefined): BackendRole[] => Array.from(
  new Set((user?.roles ?? []).map((role) => normalizeBackendRole(role)).filter((role): role is BackendRole => Boolean(role))),
);

export const resolvePrimaryRole = (user: Pick<User, 'roles'> | null | undefined): Role | null => {
  const roles = new Set(getNormalizedUserRoles(user));
  const resolved = ROLE_PRIORITY.find((role) => roles.has(`ROLE_${role}` as BackendRole));
  return resolved ?? null;
};

export const getDashboardPathForRole = (role: Role | null): string | null => role ? DASHBOARD_PATHS[role] : null;

export const getCompanyPathForApprovalStatus = (approvalStatus?: ApprovalStatus | null): string => {
  switch (approvalStatus) {
    case 'APPROVED':
      return '/company/dashboard';
    case 'SUSPENDED':
    case 'REJECTED':
      return '/company/rejected';
    case 'MORE_INFO_REQUIRED':
    case 'PENDING':
    default:
      return '/company/pending-approval';
  }
};

export const getSchoolPathForApprovalStatus = (approvalStatus?: ApprovalStatus | null): string => {
  switch (approvalStatus) {
    case 'ACTIVE':
    case 'APPROVED':
      return '/school/dashboard';
    case 'REJECTED':
    case 'SUSPENDED':
      return '/school/registration-status';
    case 'PENDING_DISTRICT_APPROVAL':
    default:
      return '/school/pending-approval';
  }
};

export const getDashboardPathForUser = (user: Pick<User, 'roles' | 'approvalStatus' | 'profileCompleted'> | null | undefined): string | null => {
  const primaryRole = resolvePrimaryRole(user);
  if (primaryRole === 'COMPANY') {
    return getCompanyPathForApprovalStatus(user?.approvalStatus);
  }
  if (primaryRole === 'STUDENT') {
    return '/student/dashboard';
  }
  if (primaryRole === 'SCHOOL_ADMIN') {
    return getSchoolPathForApprovalStatus(user?.approvalStatus);
  }
  if (primaryRole === 'DISTRICT_ADMIN') {
    return '/district/dashboard';
  }
  if (primaryRole === 'DISTRICT_DIRECTOR') {
    return '/district/dashboard';
  }
  if (primaryRole === 'CIRCUIT_MANAGER') {
    return '/district/circuit/dashboard';
  }
  if (primaryRole === 'SUBJECT_ADVISOR') {
    return '/district/advisor/dashboard';
  }
  if (primaryRole === 'TEACHER') {
    return '/teacher/dashboard';
  }
  if (primaryRole === 'SCHOOL_STUDENT') {
    return '/school-student/dashboard';
  }
  return getDashboardPathForRole(primaryRole);
};

export const isAuthorizedPathForRole = (pathname: string | null | undefined, role: Role | null): boolean => {
  if (!pathname || !role) return false;
  return pathname === DASHBOARD_PATHS[role] || pathname.startsWith(ROLE_PATH_PREFIXES[role]);
};
