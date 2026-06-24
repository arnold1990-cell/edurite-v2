import { createContext, useEffect, useMemo, useState } from 'react';
import { flushSync } from 'react-dom';
import { authStore } from '@/features/auth/authStore';
import { getNormalizedUserRoles, resolvePrimaryRole } from '@/features/auth/roleUtils';
import { authService } from '@/services/authService';
import { companyService } from '@/services/companyService';
import { studentService } from '@/services/studentService';
import type { CompanyRegisterPayload, RegistrationResponse, Role, SchoolRegisterPayload, StudentRegisterPayload, User } from '@/types';

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  isStudentProfileStatusSyncing: boolean;
  login: (payload: { email?: string; schoolName?: string; emisNumber?: string; password: string }, options?: { rememberMe?: boolean }) => Promise<User>;
  loginWithGoogle: (idToken: string, role: 'STUDENT' | 'COMPANY', options?: { rememberMe?: boolean }) => Promise<User>;
  logout: () => Promise<void>;
  registerStudent: (payload: StudentRegisterPayload) => Promise<RegistrationResponse>;
  registerCompany: (payload: CompanyRegisterPayload) => Promise<RegistrationResponse>;
  registerSchool: (payload: SchoolRegisterPayload) => Promise<RegistrationResponse>;
  syncStudentProfileState: (profile: { profileCompleted: boolean; profileCompleteness: number }) => void;
  hasRole: (role: Role) => boolean;
  getPrimaryRole: () => Role | null;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

const parsePlanTypeFromAccessToken = (accessToken?: string | null): User['planType'] | undefined => {
  if (!accessToken) return undefined;
  const [, payload] = accessToken.split('.');
  if (!payload) return undefined;
  try {
    const padded = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=');
    const decoded = atob(padded);
    const claims = JSON.parse(decoded) as { planType?: unknown };
    const normalized = typeof claims.planType === 'string' ? claims.planType.trim().toUpperCase() : '';
    if (normalized === 'PREMIUM') return 'PREMIUM';
    if (normalized === 'BASIC') return 'BASIC';
  } catch {
    return undefined;
  }
  return undefined;
};

const normalizeStoredUser = (user: User | null, accessToken?: string | null): User | null => {
  if (!user) return null;
  const roles = getNormalizedUserRoles(user);
  if (!roles.length) return null;
  const primaryRole = resolvePrimaryRole({ ...user, roles });
  const tokenPlanType = parsePlanTypeFromAccessToken(accessToken);
  return {
    ...user,
    roles,
    primaryRole: primaryRole ? `ROLE_${primaryRole}` : user.primaryRole,
    planType: tokenPlanType ?? user.planType ?? 'BASIC',
  };
};

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isHydrated, setIsHydrated] = useState(false);
  const [isStudentProfileStatusSyncing, setIsStudentProfileStatusSyncing] = useState(false);

  const syncStudentProfileState = (profile: { profileCompleted: boolean; profileCompleteness: number }) => {
    setUser((currentUser) => {
      if (!currentUser) return currentUser;
      const normalizedRoles = getNormalizedUserRoles(currentUser);
      if (!normalizedRoles.includes('ROLE_STUDENT')) {
        return currentUser;
      }
      const next = {
        ...currentUser,
        profileCompleted: profile.profileCompleted,
        profileCompleteness: profile.profileCompleteness,
      };
      const rememberMe = localStorage.getItem('edurite_access_token') !== null;
      authStore.setUser(next, rememberMe);
      return next;
    });
  };

  const syncCompanyApprovalState = (profile: { status?: User['approvalStatus']; companyName?: string }) => {
    setUser((currentUser) => {
      if (!currentUser) return currentUser;
      const normalizedRoles = getNormalizedUserRoles(currentUser);
      if (!normalizedRoles.includes('ROLE_COMPANY')) {
        return currentUser;
      }
      const next = {
        ...currentUser,
        approvalStatus: profile.status ?? currentUser.approvalStatus,
        companyName: profile.companyName ?? currentUser.companyName,
        verified: currentUser.verified,
      };
      const rememberMe = localStorage.getItem('edurite_access_token') !== null;
      authStore.setUser(next, rememberMe);
      return next;
    });
  };

  useEffect(() => {
    let active = true;
    const token = authStore.getAccessToken();
    const storedUser = normalizeStoredUser(authStore.getUser(), token);
    if (!token) {
      authStore.clear();
      setIsHydrated(true);
      return () => {
        active = false;
      };
    }

    if (storedUser) {
      setUser(storedUser);
      const roles = getNormalizedUserRoles(storedUser);
      if (roles.includes('ROLE_STUDENT')) {
        setIsStudentProfileStatusSyncing(true);
        studentService.getMe()
          .then((profile) => {
            if (!active) return;
            syncStudentProfileState({
              profileCompleted: profile.profileCompleted,
              profileCompleteness: profile.profileCompleteness,
            });
          })
          .catch(() => undefined)
          .finally(() => {
            if (!active) return;
            setIsStudentProfileStatusSyncing(false);
          });
      } else if (roles.includes('ROLE_COMPANY')) {
        companyService.getMe()
          .then((profile) => {
            if (!active) return;
            syncCompanyApprovalState({
              status: profile?.status,
              companyName: profile?.companyName,
            });
          })
          .catch(() => undefined);
      } else {
        setIsStudentProfileStatusSyncing(false);
      }
    }
    setIsHydrated(true);
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const onStorageSync = (event: StorageEvent) => {
      if (!event.key || !['edurite_access_token', 'edurite_refresh_token', 'edurite_user', 'edurite_session_event'].includes(event.key)) {
        return;
      }
      if (!authStore.getAccessToken()) {
        setUser(null);
      }
    };
    const onAuthCleared = () => {
      if (!authStore.getAccessToken()) {
        setUser(null);
      }
    };
    const onAuthUpdated = () => {
      const token = authStore.getAccessToken();
      const storedUser = normalizeStoredUser(authStore.getUser(), token);
      setUser(storedUser);
    };
    window.addEventListener('storage', onStorageSync);
    window.addEventListener('edurite-auth-cleared', onAuthCleared as EventListener);
    window.addEventListener('edurite-auth-updated', onAuthUpdated as EventListener);
    return () => {
      window.removeEventListener('storage', onStorageSync);
      window.removeEventListener('edurite-auth-cleared', onAuthCleared as EventListener);
      window.removeEventListener('edurite-auth-updated', onAuthUpdated as EventListener);
    };
  }, []);

  const setSession = (payload: { accessToken: string; refreshToken?: string; user: User }, options?: { rememberMe?: boolean }) => {
    const rememberMe = options?.rememberMe ?? true;
    const normalizedUser = normalizeStoredUser(payload.user, payload.accessToken);
    if (!normalizedUser) {
      throw new Error('Authenticated session did not include a supported role.');
    }
    authStore.setTokens(payload.accessToken, payload.refreshToken, rememberMe);
    authStore.setUser(normalizedUser, rememberMe);
    flushSync(() => {
      setUser(normalizedUser);
    });
    if (import.meta.env.DEV) {
      console.info('[auth] auth context session committed', {
        email: normalizedUser.email,
        roles: normalizedUser.roles,
        primaryRole: normalizedUser.primaryRole,
        approvalStatus: normalizedUser.approvalStatus,
        localStorageAuth: {
          accessToken: localStorage.getItem('edurite_access_token'),
          refreshToken: localStorage.getItem('edurite_refresh_token'),
          user: localStorage.getItem('edurite_user'),
        },
        sessionStorageAuth: {
          accessToken: sessionStorage.getItem('edurite_access_token'),
          refreshToken: sessionStorage.getItem('edurite_refresh_token'),
          user: sessionStorage.getItem('edurite_user'),
        },
      });
    }
    return normalizedUser;
  };

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isHydrated,
      isStudentProfileStatusSyncing,
      isAuthenticated: Boolean(authStore.getAccessToken() && user),
      login: async (payload, options) => setSession(await authService.login(payload), options),
      loginWithGoogle: async (idToken, role, options) => setSession(await authService.googleSignIn(idToken, role), options),
      registerStudent: async (payload) => authService.registerStudent(payload),
      registerCompany: async (payload) => authService.registerCompany(payload),
      registerSchool: async (payload) => authService.registerSchool(payload),
      syncStudentProfileState,
      logout: async () => {
        try {
          await authService.logout();
        } finally {
          authStore.clear();
          setUser(null);
        }
      },
      hasRole: (role) => getNormalizedUserRoles(user).includes(`ROLE_${role}`),
      getPrimaryRole: () => resolvePrimaryRole(user),
    }),
    [isHydrated, isStudentProfileStatusSyncing, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
