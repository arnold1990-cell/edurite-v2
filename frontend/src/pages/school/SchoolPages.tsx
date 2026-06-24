import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { EduRiteLogo } from '@/components/common/EduRiteLogo';
import { Button } from '@/components/ui/Button';
import { LoadingState, ErrorState } from '@/components/feedback/States';
import { schoolService } from '@/services/schoolService';
import { SchoolAdminWorkspace } from '@/components/school/admin/SchoolAdminWorkspace';
import { TeacherWorkspace } from '@/components/school/teacher/TeacherWorkspace';
import { SchoolStudentWorkspace } from '@/components/school/student/SchoolStudentWorkspace';
import { useAuth } from '@/hooks/useAuth';
import type { ApiError } from '@/types';

const SCHOOL_SIGN_IN_PATH = '/school/login';
const PUBLIC_HOME_PATH = '/';

export const SchoolPendingApprovalPage = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const statusQuery = useAppQuery({
    queryKey: ['school', 'registration-status', 'page'],
    queryFn: schoolService.schoolRegistrationStatus,
    refetchInterval: 15000,
  });

  if (statusQuery.isLoading) return <LoadingState />;
  if (statusQuery.isError || !statusQuery.data) return <ErrorState message="Unable to load school registration status." />;

  const status = statusQuery.data.status;
  const hasSession = Boolean(user);
  const title = status === 'ACTIVE'
    ? 'School Registration Approved'
    : status === 'REJECTED'
      ? 'School Registration Rejected'
      : status === 'SUSPENDED'
        ? 'School Registration Suspended'
        : 'Registration Submitted';

  const summary = status === 'ACTIVE'
    ? 'Your school has been linked to the selected district and full School Admin access is available.'
    : status === 'REJECTED'
      ? 'Your district rejected this registration request. Review the reason below and contact the district office.'
      : status === 'SUSPENDED'
        ? 'This school account is suspended. Contact the district office for assistance.'
        : 'Your registration was submitted successfully. Your school account is waiting for district approval. You may sign out and return later to check your approval status.';

  const handleNavigateHome = () => {
    navigate(PUBLIC_HOME_PATH, { replace: true });
  };

  const handleSessionExit = async (target: string) => {
    await logout();
    navigate(target, { replace: true });
  };

  return (
    <section className="min-h-screen bg-[linear-gradient(180deg,#eff6ff_0%,#f8fafc_50%,#ffffff_100%)] text-slate-900">
      <header className="border-b border-slate-200/80 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <Link to={PUBLIC_HOME_PATH} className="flex h-12 w-12 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" aria-label="EduRite home">
              <EduRiteLogo className="h-9 w-9" />
            </Link>
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">EduRite</p>
              <h1 className="truncate text-lg font-semibold text-slate-900 sm:text-xl">{title}</h1>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button type="button" className="bg-slate-800 hover:bg-slate-700" onClick={() => void handleSessionExit(SCHOOL_SIGN_IN_PATH)}>
              Sign In
            </Button>
            {hasSession ? (
              <Button type="button" className="bg-rose-600 hover:bg-rose-500" onClick={() => void handleSessionExit(SCHOOL_SIGN_IN_PATH)}>
                Logout
              </Button>
            ) : null}
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-[28px] border border-sky-100 bg-white shadow-[0_32px_80px_-48px_rgba(15,23,42,0.35)]">
          <div className="bg-[linear-gradient(135deg,#0f3b8c_0%,#2563eb_55%,#7dd3fc_100%)] px-6 py-8 text-white sm:px-8">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-100">District Approval Workflow</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight">{title}</h2>
            <p className="mt-4 max-w-3xl text-sm leading-7 text-sky-50 sm:text-base">{summary}</p>
          </div>

          <div className="grid gap-6 px-6 py-6 sm:px-8 lg:grid-cols-[minmax(0,1.3fr)_minmax(280px,0.7fr)]">
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">School</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{statusQuery.data.schoolName}</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">EMIS Number</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{statusQuery.data.emisNumber}</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Selected District</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{statusQuery.data.district}</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Province</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{statusQuery.data.province}</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Circuit</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{statusQuery.data.circuit}</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Submitted</p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">{new Date(statusQuery.data.submittedAt).toLocaleString()}</p>
                </div>
              </div>
              {statusQuery.data.rejectionReason ? (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                  <p className="font-semibold">District feedback</p>
                  <p className="mt-1">{statusQuery.data.rejectionReason}</p>
                </div>
              ) : null}
              <p className="text-sm leading-6 text-slate-600">
                Contact the district office or your assigned district administrator if you need help with this request.
              </p>
            </div>

            <aside className="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
              <p className="text-sm font-semibold text-slate-900">Available actions</p>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Pending schools can review this approval page, change their password, return to sign in, or sign out until district approval is complete.
              </p>
              <div className="mt-5 flex flex-col gap-3">
                <Button type="button" onClick={() => void handleSessionExit(SCHOOL_SIGN_IN_PATH)}>
                  Back to Sign In
                </Button>
                <Button type="button" className="bg-slate-800 hover:bg-slate-700" onClick={handleNavigateHome}>
                  Go to Home
                </Button>
                <Button type="button" className="bg-rose-600 hover:bg-rose-500" onClick={() => void handleSessionExit(SCHOOL_SIGN_IN_PATH)}>
                  Logout
                </Button>
                {status === 'ACTIVE' ? <Link to="/school/dashboard"><Button>Open School Admin Portal</Button></Link> : null}
                <Link to="/account/change-password"><Button className="bg-white text-slate-900 ring-1 ring-slate-200 hover:bg-slate-100">Change Password</Button></Link>
              </div>
            </aside>
          </div>
        </div>
      </div>
    </section>
  );
};

export const SchoolAdminDashboardPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const dashboard = useAppQuery({ queryKey: ['school', 'admin', 'dashboard'], queryFn: schoolService.schoolAdminDashboard });
  const { user, logout } = useAuth();
  const [grade, setGrade] = useState('Grade 8');
  const [className, setClassName] = useState('A');
  const [academicYear, setAcademicYear] = useState(new Date().getFullYear());
  const [subjectName, setSubjectName] = useState('Mathematics');
  const [phase, setPhase] = useState('FET');
  const resolveSectionFromPath = (pathname: string) => {
    if (pathname.endsWith('/teachers')) return 'Teachers';
    if (pathname.endsWith('/learners')) return 'Learners';
    if (pathname.endsWith('/classes')) return 'Classes';
    if (pathname.endsWith('/subjects')) return 'Subjects';
    if (pathname.endsWith('/assignments')) return 'Assignments';
    if (pathname.endsWith('/assessments')) return 'Assessments';
    if (pathname.endsWith('/results')) return 'Results';
    if (pathname.endsWith('/reports')) return 'Reports';
    if (pathname.endsWith('/notifications')) return 'Notifications';
    if (pathname.endsWith('/settings')) return 'Settings';
    return 'Dashboard';
  };
  const [activeSection, setActiveSection] = useState(resolveSectionFromPath(location.pathname));
  const sectionRouteMap: Record<string, string> = {
    Dashboard: '/school/dashboard',
    Teachers: '/school/teachers',
    Learners: '/school/learners',
    Classes: '/school/classes',
    Subjects: '/school/subjects',
    Assignments: '/school/assignments',
    Assessments: '/school/assessments',
    Results: '/school/results',
    Reports: '/school/reports',
    Notifications: '/school/notifications',
    Settings: '/school/settings',
    'Change Password': '/account/change-password',
  };
  const setActiveSectionAndRoute = (section: string) => {
    setActiveSection(section);
    const target = sectionRouteMap[section];
    if (target && target !== location.pathname) {
      navigate(target);
    }
  };
  useEffect(() => {
    setActiveSection(resolveSectionFromPath(location.pathname));
  }, [location.pathname]);
  const [search, setSearch] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const createClass = useMutation({ mutationFn: schoolService.createClass });
  const createSubject = useMutation({ mutationFn: schoolService.createSubject });

  if (dashboard.isLoading) return <LoadingState />;
  if (dashboard.isError || !dashboard.data) return <ErrorState message="Unable to load school dashboard." />;

  const safeTasks = Math.max(0, dashboard.data.totalTasks);
  const safeNotes = Math.max(0, dashboard.data.totalNotes);
  const safeSubmissions = Math.max(0, dashboard.data.totalSubmissions);
  const pendingTasks = Math.max(0, safeTasks - safeSubmissions);
  const totalReports = Math.max(0, dashboard.data.totalClasses + dashboard.data.totalSubjects);

  const createClassAction = async () => {
    setFeedback(null);
    try {
      await createClass.mutateAsync({ grade: grade.trim(), className: className.trim(), academicYear });
      setFeedback({ type: 'success', message: `Class ${grade} ${className} created successfully.` });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Could not create class.';
      setFeedback({ type: 'error', message });
    }
  };

  const createSubjectAction = async () => {
    setFeedback(null);
    try {
      await createSubject.mutateAsync({ subjectName: subjectName.trim(), phase: phase.trim() });
      setFeedback({ type: 'success', message: `${subjectName} created successfully.` });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Could not create subject.';
      setFeedback({ type: 'error', message });
    }
  };

  return (
    <SchoolAdminWorkspace
      fullName={user?.fullName}
      activeSection={activeSection}
      setActiveSection={setActiveSectionAndRoute}
      search={search}
      setSearch={setSearch}
      feedback={feedback}
      summary={{
        totalClasses: dashboard.data.totalClasses,
        totalSubjects: dashboard.data.totalSubjects,
        totalTeachers: 0,
        totalLearners: 0,
        pendingTasks,
        totalNotes: safeNotes,
        totalSubmissions: safeSubmissions,
        totalReports,
      }}
      grade={grade}
      className={className}
      academicYear={academicYear}
      subjectName={subjectName}
      phase={phase}
      setGrade={setGrade}
      setClassName={setClassName}
      setAcademicYear={setAcademicYear}
      setSubjectName={setSubjectName}
      setPhase={setPhase}
      onCreateClass={createClassAction}
      onCreateSubject={createSubjectAction}
      creatingClass={createClass.isPending}
      creatingSubject={createSubject.isPending}
      onLogout={logout}
    />
  );
};

export const TeacherDashboardPage = () => {
  const { logout } = useAuth();
  const queryClient = useQueryClient();
  const [activeSection, setActiveSection] = useState<
    'Dashboard' | 'My Classes' | 'Subjects' | 'Assignments & SBA' | 'Notes & Resources' | 'Exams & Assessments' | 'Learner Progress' | 'Analytics' | 'Calendar' | 'Settings' | 'Logout'
  >('Dashboard');
  const loadTasks = ['Assignments & SBA', 'Learner Progress', 'Exams & Assessments'].includes(activeSection);
  const loadAnalytics = ['Dashboard', 'Analytics'].includes(activeSection);
  const loadAssessments = activeSection === 'Exams & Assessments';
  const loadSubmissions = activeSection === 'Learner Progress';
  const loadCalendar = activeSection === 'Calendar';
  const widgetQueryConfig = { staleTime: 60000, retry: 1, refetchOnWindowFocus: false } as const;
  const dashboard = useAppQuery({ queryKey: ['school', 'teacher', 'dashboard'], queryFn: schoolService.teacherDashboard });
  const tasks = useAppQuery({ queryKey: ['school', 'teacher', 'tasks'], queryFn: schoolService.teacherTasks, enabled: loadTasks, ...widgetQueryConfig });
  const classes = useAppQuery({ queryKey: ['school', 'teacher', 'classes'], queryFn: schoolService.teacherClasses, ...widgetQueryConfig });
  const subjects = useAppQuery({ queryKey: ['school', 'teacher', 'subjects'], queryFn: schoolService.teacherSubjects, ...widgetQueryConfig });
  const assessments = useAppQuery({ queryKey: ['school', 'teacher', 'assessments'], queryFn: schoolService.teacherAssessments, enabled: loadAssessments, ...widgetQueryConfig });
  const allSubmissions = useAppQuery({ queryKey: ['school', 'teacher', 'submissions-all'], queryFn: schoolService.teacherSubmissions, enabled: loadSubmissions, ...widgetQueryConfig });
  const analytics = useAppQuery({ queryKey: ['school', 'teacher', 'analytics'], queryFn: schoolService.teacherAnalytics, enabled: loadAnalytics, ...widgetQueryConfig });
  const calendar = useAppQuery({ queryKey: ['school', 'teacher', 'calendar'], queryFn: schoolService.teacherCalendar, enabled: loadCalendar, ...widgetQueryConfig });
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [title, setTitle] = useState('SBA Task 1');
  const [instructions, setInstructions] = useState('Complete and submit before due date.');
  const [dueAt, setDueAt] = useState('');
  const [selectedTaskId, setSelectedTaskId] = useState('');
  const submissions = useAppQuery({ queryKey: ['school', 'teacher', 'submissions', selectedTaskId], queryFn: () => schoolService.taskSubmissions(selectedTaskId), enabled: loadSubmissions && Boolean(selectedTaskId), ...widgetQueryConfig });
  const createTask = useMutation({ mutationFn: schoolService.createTask });
  const createNote = useMutation({ mutationFn: schoolService.createNote });
  const markSubmission = useMutation({
    mutationFn: ({ submissionId, payload }: { submissionId: string; payload: { marksAwarded: number; comments?: string; released: boolean } }) =>
      schoolService.markSubmission(submissionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher', 'submissions-all'] });
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher', 'submissions'] });
      queryClient.invalidateQueries({ queryKey: ['school', 'teacher', 'analytics'] });
    },
  });

  if (dashboard.isLoading) return <LoadingState />;
  if (dashboard.isError || !dashboard.data) return <ErrorState message="Unable to load teacher dashboard." />;

  const optionalFailures: string[] = [];
  if (tasks.isError) optionalFailures.push('Tasks unavailable');
  if (classes.isError) optionalFailures.push('Classes unavailable');
  if (subjects.isError) optionalFailures.push('Subjects unavailable');
  if (assessments.isError) optionalFailures.push('Assessments unavailable');
  if (allSubmissions.isError) optionalFailures.push('Submissions unavailable');
  if (analytics.isError) optionalFailures.push('Analytics unavailable');
  if (calendar.isError) optionalFailures.push('Calendar unavailable');
  if (submissions.isError) optionalFailures.push('Task submissions unavailable');

  return (
    <section className="space-y-3">
      {optionalFailures.length ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          Some sections could not be loaded: {optionalFailures.join(' | ')}
        </div>
      ) : null}
      {(createTask.isError || createNote.isError || markSubmission.isError) ? (
        <ErrorState
          message={
            (createTask.error as Error | null)?.message
            ?? (createNote.error as Error | null)?.message
            ?? (markSubmission.error as Error | null)?.message
            ?? 'An action failed.'
          }
        />
      ) : null}
      <TeacherWorkspace
        activeSection={activeSection}
        setActiveSection={setActiveSection}
        dashboard={dashboard.data}
        analytics={analytics.data ?? null}
        tasks={tasks.data ?? []}
        submissions={selectedTaskId ? (submissions.data ?? []) : (allSubmissions.data ?? [])}
        classes={classes.data ?? []}
        subjects={subjects.data ?? []}
        assessments={assessments.data ?? []}
        calendarItems={calendar.data ?? []}
        selectedTaskId={selectedTaskId}
        setSelectedTaskId={setSelectedTaskId}
        classId={classId}
        setClassId={setClassId}
        subjectId={subjectId}
        setSubjectId={setSubjectId}
        title={title}
        setTitle={setTitle}
        instructions={instructions}
        setInstructions={setInstructions}
        dueAt={dueAt}
        setDueAt={setDueAt}
        onCreateTask={() => {
          if (!dueAt) return;
          createTask.mutate({
            classId,
            subjectId,
            taskType: 'SBA',
            title,
            instructions,
            dueAt: new Date(dueAt).toISOString(),
            maxMarks: 100,
            term: 'Term 1',
          });
        }}
        onCreateNote={(payload) => createNote.mutate({ classId, subjectId, title: payload.title, noteText: payload.noteText })}
        onMarkSubmission={(submissionId, payload) => markSubmission.mutate({ submissionId, payload })}
        onLogout={logout}
        creatingTask={createTask.isPending}
        creatingNote={createNote.isPending}
        markingSubmissionId={markSubmission.variables?.submissionId}
      />
    </section>
  );
};

export const SchoolStudentDashboardPage = () => {
  const formatQueryError = (label: string, error: unknown) => {
    if (!error || typeof error !== 'object') return `${label}: unknown error`;
    const apiError = error as ApiError;
    const statusPart = apiError.status ? `HTTP ${apiError.status}` : 'HTTP unknown';
    const messagePart = apiError.message || 'Unexpected error';
    return `${label} failed (${statusPart}): ${messagePart}`;
  };

  const queryClient = useQueryClient();
  const location = useLocation();
  const initialLearnerSection = useMemo(() => {
    const hash = location.hash.toLowerCase();
    if (hash === '#assignments') return 'Assignments & SBA' as const;
    if (hash === '#marks') return 'Marks & Feedback' as const;
    if (hash === '#announcements') return 'Notes & Resources' as const;
    return 'Dashboard' as const;
  }, [location.hash]);
  const [activeSection, setActiveSection] = useState<
    'Dashboard' | 'My Subjects' | 'Assignments & SBA' | 'Notes & Resources' | 'Exams & Quizzes' | 'My Submissions' | 'Marks & Feedback' | 'Progress' | 'Logout'
  >(initialLearnerSection);
  const loadSubjects = ['Dashboard', 'My Subjects'].includes(activeSection);
  const loadTasks = ['Dashboard', 'Assignments & SBA', 'My Submissions'].includes(activeSection);
  const loadNotes = ['Dashboard', 'Notes & Resources'].includes(activeSection);
  const loadAssessments = ['Dashboard', 'Exams & Quizzes'].includes(activeSection);
  const loadSubmissions = ['Dashboard', 'My Submissions', 'Marks & Feedback', 'Progress'].includes(activeSection);
  const loadMarks = ['Dashboard', 'Marks & Feedback', 'Progress'].includes(activeSection);
  const loadProgress = ['Dashboard', 'Progress'].includes(activeSection);
  const widgetQueryConfig = { staleTime: 60000, retry: 1, refetchOnWindowFocus: false } as const;
  const dashboard = useAppQuery({ queryKey: ['school', 'learner', 'dashboard'], queryFn: schoolService.learnerDashboard });
  const subjects = useAppQuery({ queryKey: ['school', 'learner', 'subjects'], queryFn: schoolService.learnerSubjects, enabled: loadSubjects, ...widgetQueryConfig });
  const notes = useAppQuery<Array<{ id?: string; title?: string; noteText?: string; pdfUrl?: string }>>({ queryKey: ['school', 'learner', 'notes'], queryFn: schoolService.learnerNotes, enabled: loadNotes, ...widgetQueryConfig });
  const tasks = useAppQuery({ queryKey: ['school', 'learner', 'tasks'], queryFn: schoolService.learnerTasks, enabled: loadTasks, ...widgetQueryConfig });
  const assessments = useAppQuery({ queryKey: ['school', 'learner', 'assessments'], queryFn: schoolService.learnerAssessments, enabled: loadAssessments, ...widgetQueryConfig });
  const submissions = useAppQuery({ queryKey: ['school', 'learner', 'submissions'], queryFn: schoolService.learnerSubmissions, enabled: loadSubmissions, ...widgetQueryConfig });
  const marks = useAppQuery({ queryKey: ['school', 'learner', 'marks'], queryFn: schoolService.learnerMarks, enabled: loadMarks, ...widgetQueryConfig });
  const progress = useAppQuery({ queryKey: ['school', 'learner', 'progress'], queryFn: schoolService.learnerProgress, enabled: loadProgress, ...widgetQueryConfig });
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const submitTask = useMutation({
    mutationFn: schoolService.submitTask,
    onSuccess: async () => {
      setSuccessMessage('Submission uploaded successfully.');
      setErrorMessage(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['school', 'learner', 'submissions'] }),
        queryClient.invalidateQueries({ queryKey: ['school', 'learner', 'marks'] }),
        queryClient.invalidateQueries({ queryKey: ['school', 'learner', 'tasks'] }),
        queryClient.invalidateQueries({ queryKey: ['school', 'learner', 'progress'] }),
        queryClient.invalidateQueries({ queryKey: ['school', 'learner', 'dashboard'] }),
      ]);
    },
    onError: (error) => {
      setSuccessMessage(null);
      setErrorMessage(error instanceof Error ? error.message : 'Unable to submit task.');
    },
  });
  const { logout } = useAuth();

  useEffect(() => {
    setActiveSection(initialLearnerSection);
  }, [initialLearnerSection]);

  const criticalLoading = dashboard.isLoading;
  const criticalFailure = dashboard.isError || !dashboard.data;
  const optionalFailures: string[] = [];
  if (subjects.isError) optionalFailures.push(formatQueryError('Subjects', subjects.error));
  if (tasks.isError) optionalFailures.push(formatQueryError('Tasks', tasks.error));
  if (notes.isError) optionalFailures.push(formatQueryError('Notes', notes.error));
  if (assessments.isError) optionalFailures.push(formatQueryError('Assessments', assessments.error));
  if (submissions.isError) optionalFailures.push(formatQueryError('Submissions', submissions.error));
  if (marks.isError) optionalFailures.push(formatQueryError('Marks', marks.error));
  if (progress.isError) optionalFailures.push(formatQueryError('Progress', progress.error));

  if (criticalLoading) {
    return <LoadingState message="Loading learner workspace..." />;
  }
  if (criticalFailure) {
    const criticalErrors: string[] = [];
    if (dashboard.isError) criticalErrors.push(formatQueryError('Dashboard', dashboard.error));
    const reason = criticalErrors.join(' | ') || 'Missing critical learner data';
    return <ErrorState message={`Unable to load learner dashboard. ${reason}`} />;
  }

  return (
    <section className="space-y-3">
      {errorMessage ? <ErrorState message={errorMessage} /> : null}
      {optionalFailures.length ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          Some sections could not be loaded: {optionalFailures.join(' | ')}
        </div>
      ) : null}
      <SchoolStudentWorkspace
        initialSection={initialLearnerSection}
        subjects={subjects.data ?? []}
        tasks={tasks.data ?? []}
        assessments={assessments.data ?? []}
        notes={notes.data ?? []}
        submissions={submissions.data ?? []}
        marks={marks.data ?? []}
        progress={progress.data ?? { totalTasks: 0, submitted: 0, missing: 0, late: 0 }}
        submitting={submitTask.isPending}
        successMessage={successMessage}
        onSubmitTask={async (payload) => submitTask.mutateAsync(payload)}
        onLogout={logout}
        onSectionChange={setActiveSection}
      />
    </section>
  );
};
