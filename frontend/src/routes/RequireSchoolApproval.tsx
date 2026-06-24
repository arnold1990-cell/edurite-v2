import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getSchoolPathForApprovalStatus } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { schoolService } from '@/services/schoolService';

type SchoolRegistrationStatus = {
  status: 'PENDING_DISTRICT_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';
};

const allowedPendingRoutes = ['/school/pending-approval', '/school/registration-status'];

export const RequireSchoolApproval = () => {
  const location = useLocation();
  const { user } = useAuth();
  const profile = useAppQuery<SchoolRegistrationStatus>({
    queryKey: ['school', 'registration-status'],
    queryFn: () => schoolService.schoolRegistrationStatus(),
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    staleTime: 60000,
    refetchInterval: 60000,
    retry: 1,
  });
  const resolvedStatus = profile.data?.status ?? user?.approvalStatus;

  if (profile.isLoading && !resolvedStatus) {
    return <div className="p-6 text-sm text-slate-500">Loading school approval status...</div>;
  }

  if (profile.error) {
    if (resolvedStatus === 'ACTIVE' || resolvedStatus === 'APPROVED') {
      return <div className="p-6 text-sm text-red-600">Unable to verify current school access. Please refresh and sign in again.</div>;
    }
    if (resolvedStatus) {
      const fallbackPath = getSchoolPathForApprovalStatus(resolvedStatus);
      if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route)) || fallbackPath === location.pathname) {
        return <Outlet />;
      }
      return <Navigate to={fallbackPath} replace />;
    }
    return <div className="p-6 text-sm text-red-600">Unable to load school approval status. Please sign in again or contact your district office.</div>;
  }

  if (resolvedStatus === 'ACTIVE' || resolvedStatus === 'APPROVED') {
    if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route))) {
      return <Navigate to="/school/dashboard" replace />;
    }
    return <Outlet />;
  }

  if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route))) {
    return <Outlet />;
  }

  return <Navigate to={getSchoolPathForApprovalStatus(resolvedStatus)} replace />;
};
