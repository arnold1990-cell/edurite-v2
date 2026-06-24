import { Navigate, Outlet } from 'react-router-dom';
import { getDashboardPathForRole, getDashboardPathForUser } from '@/features/auth/roleUtils';
import type { Role } from '@/types';
import { useAuth } from '@/hooks/useAuth';

export const RequireRole = ({ role, roles }: { role?: Role; roles?: Role[] }) => {
  const { isAuthenticated, hasRole, getPrimaryRole, user } = useAuth();
  const requestedRoles = roles ?? (role ? [role] : []);

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }

  if (requestedRoles.some((item) => hasRole(item))) {
    return <Outlet />;
  }

  const primaryRole = getPrimaryRole();
  if (import.meta.env.DEV) {
    console.info('[auth] protected route denied', { requestedRoles, primaryRole, approvalStatus: user?.approvalStatus, redirectPath: getDashboardPathForUser(user) ?? getDashboardPathForRole(primaryRole) ?? '/auth/login' });
  }
  return <Navigate to={getDashboardPathForUser(user) ?? getDashboardPathForRole(primaryRole) ?? '/auth/login'} replace />;
};
