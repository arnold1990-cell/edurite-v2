import { adminService } from '@/services/adminService';

export const analyticsService = {
  adminOverview: () => adminService.getAnalytics(),
};
