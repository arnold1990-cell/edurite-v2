import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { queryKeys } from '@/lib/queryKeys';

export const useStudentDashboard = () =>
  useAppQuery({
    queryKey: queryKeys.student.dashboard,
    queryFn: () => studentService.getDashboard(),
  });
