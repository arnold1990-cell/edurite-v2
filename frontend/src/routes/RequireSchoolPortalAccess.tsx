import { Navigate, Outlet } from 'react-router-dom';
import { LoadingState } from '@/components/feedback/States';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { schoolService } from '@/services/schoolService';

export const RequireSchoolPortalAccess = () => {
  const { isAuthenticated, hasRole } = useAuth();
  const hasSchoolStudentRole = hasRole('SCHOOL_STUDENT');
  const hasStudentRole = hasRole('STUDENT');
  const statusQuery = useAppQuery({
    queryKey: ['student', 'my-school', 'status', 'guard'],
    queryFn: schoolService.getMySchoolStatus,
    enabled: isAuthenticated && !hasSchoolStudentRole && hasStudentRole,
    staleTime: 60_000,
  });

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" replace />;
  }

  if (hasSchoolStudentRole) {
    return <Outlet />;
  }

  if (!hasStudentRole) {
    return <Navigate to="/" replace />;
  }

  if (statusQuery.isLoading) {
    return <LoadingState message="Loading your school portal..." />;
  }

  if (statusQuery.data?.status === 'APPROVED') {
    return <Outlet />;
  }

  return <Navigate to="/student/dashboard" replace />;
};
