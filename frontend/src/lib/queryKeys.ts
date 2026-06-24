export const queryKeys = {
  auth: ['auth'] as const,
  student: {
    me: ['student', 'me'] as const,
    dashboard: ['student', 'dashboard'] as const,
    recommendations: ['student', 'recommendations'] as const,
  },
  company: {
    me: ['company', 'me'] as const,
    bursaries: ['company', 'bursaries'] as const,
  },
  admin: {
    users: ['admin', 'users'] as const,
    analytics: ['admin', 'analytics'] as const,
  },
};
