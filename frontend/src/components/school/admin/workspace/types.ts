export type Feedback = { type: 'success' | 'error'; message: string } | null;

export type SchoolAdminSummary = {
  totalClasses: number;
  totalSubjects: number;
  totalTeachers: number;
  totalLearners: number;
  pendingTasks: number;
  totalNotes: number;
  totalSubmissions: number;
  totalReports: number;
};
