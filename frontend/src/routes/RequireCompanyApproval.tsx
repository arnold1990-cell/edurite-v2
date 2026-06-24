import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getCompanyPathForApprovalStatus } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { companyService } from '@/services/companyService';

type CompanyProfileStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_REQUIRED' | 'SUSPENDED';

type CompanyProfile = {
  status: CompanyProfileStatus;
};

const allowedPendingRoutes = ['/company/pending', '/company/pending-approval', '/company/rejected', '/company/profile', '/company/verification-docs', '/company/settings'];

export const RequireCompanyApproval = () => {
  const location = useLocation();
  const { user } = useAuth();
  const profile = useAppQuery<CompanyProfile>({
    queryKey: ['company', 'approval-status'],
    queryFn: () => companyService.getMe(),
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    staleTime: 60000,
    refetchInterval: 60000,
    retry: 1,
  });
  const resolvedStatus = profile.data?.status ?? user?.approvalStatus;
  const isPendingStatusRoute = location.pathname.startsWith('/company/pending') || location.pathname.startsWith('/company/rejected');

  if (profile.isLoading && !resolvedStatus) {
    return <div className="p-6 text-sm text-slate-500">Loading company approval status...</div>;
  }

  if (profile.error) {
    if (import.meta.env.DEV) {
      console.error('[auth] company approval guard failed to refresh company profile', {
        pathname: location.pathname,
        cachedApprovalStatus: user?.approvalStatus,
        error: profile.error,
      });
    }
    if (resolvedStatus === 'APPROVED') {
      return <div className="p-6 text-sm text-red-600">Unable to verify current company access. Please refresh and sign in again.</div>;
    }
    if (resolvedStatus) {
      const fallbackPath = getCompanyPathForApprovalStatus(resolvedStatus);
      if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route)) || fallbackPath === location.pathname) {
        return <Outlet />;
      }
      return <Navigate to={fallbackPath} replace />;
    }
    return <div className="p-6 text-sm text-red-600">Unable to load company approval status. Please sign in again or contact support if the issue persists.</div>;
  }

  if (import.meta.env.DEV) {
    console.info('[auth] company approval guard', { pathname: location.pathname, approvalStatus: resolvedStatus });
  }

  if (resolvedStatus === 'APPROVED') {
    if (isPendingStatusRoute) {
      return <Navigate to="/company/dashboard" replace />;
    }
    return <Outlet />;
  }

  if (allowedPendingRoutes.some((route) => location.pathname.startsWith(route))) {
    return <Outlet />;
  }

  if (resolvedStatus === 'REJECTED' || resolvedStatus === 'SUSPENDED') {
    return <Navigate to="/company/rejected" replace />;
  }

  return <Navigate to="/company/pending-approval" replace />;
};
