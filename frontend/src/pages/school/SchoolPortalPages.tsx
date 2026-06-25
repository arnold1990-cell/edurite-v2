import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { BookOpen, FileBarChart2, GraduationCap, MessageSquare, Sparkles, Users } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { DashboardLogo } from '@/components/common/DashboardLogo';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ErrorState, LoadingState } from '@/components/feedback/States';
import { DashboardKpiCard, DashboardSectionCard, DashboardShell, EmptyStateCompact, ProgressBar } from '@/components/dashboard/DashboardPrimitives';
import {
  schoolService,
  type AcademicInsightsResponse,
  type BursaryReadinessResponse,
  type CareerReadinessResponse,
  type InterventionReportItem,
  type LearnerListItem,
  type PortalSettingsResponse,
  type SchoolPortalDashboard,
  type TeacherCurriculumWidgets,
  type TeacherLessonPlanResponse,
} from '@/services/schoolService';
import { TeacherCurriculumPage } from '@/pages/school/SchoolCurriculumPage';

type PortalMode = 'school' | 'teacher';
type Section = 'dashboard' | 'learners' | 'curriculum' | 'academic-insights' | 'career-readiness' | 'courses' | 'bursaries' | 'interventions' | 'reports' | 'settings';

const sectionFromPath = (pathname: string, mode: PortalMode): Section => {
  const base = mode === 'school' ? '/school/' : '/teacher/';
  const suffix = pathname.startsWith(base) ? pathname.slice(base.length) : 'dashboard';
  if (suffix.startsWith('learners')) return 'learners';
  if (suffix.startsWith('curriculum')) return 'curriculum';
  if (suffix.startsWith('academic-insights')) return 'academic-insights';
  if (suffix.startsWith('career-readiness')) return 'career-readiness';
  if (suffix.startsWith('courses')) return 'courses';
  if (suffix.startsWith('bursaries')) return 'bursaries';
  if (suffix.startsWith('interventions')) return 'interventions';
  if (suffix.startsWith('reports')) return 'reports';
  if (suffix.startsWith('settings')) return 'settings';
  return 'dashboard';
};

const reportTypes = [
  { key: 'whole-school-readiness', label: 'Whole-school readiness report' },
  { key: 'grade-readiness', label: 'Grade readiness report' },
  { key: 'subject-gap', label: 'Subject gap report' },
  { key: 'career-interest', label: 'Career interest report' },
  { key: 'bursary-readiness', label: 'Bursary readiness report' },
  { key: 'at-risk-learner', label: 'At-risk learner report' },
] as const;

const kpiIcons = [Users, GraduationCap, FileBarChart2, MessageSquare, Sparkles, BookOpen];

const PortalCards = ({ dashboard }: { dashboard: SchoolPortalDashboard }) => (
  <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
    {dashboard.metrics.map((metric, index) => (
      <DashboardKpiCard
        key={metric.label}
        icon={kpiIcons[index % kpiIcons.length]}
        label={metric.label}
        value={String(metric.value)}
        helperText={metric.trendLabel}
        tone={metric.tone}
        actionLabel="View"
      />
    ))}
  </div>
);

const BreakdownPanel = ({ title, items }: { title: string; items: Array<{ label: string; value: number }> }) => (
  <DashboardSectionCard title={title}>
    <div className="space-y-3">
      {items.length ? items.map((item) => (
        <div key={`${title}-${item.label}`} className="space-y-1">
          <div className="flex items-center justify-between gap-2 text-sm">
            <span className="text-slate-700">{item.label}</span>
            <span className="font-semibold text-slate-900">{item.value}</span>
          </div>
          <ProgressBar value={Math.min(100, item.value * 10)} />
        </div>
      )) : <EmptyStateCompact title={title} message="No data yet." />}
    </div>
  </DashboardSectionCard>
);

const downloadExport = (fileName: string, contentType: string, base64Content: string) => {
  const binary = window.atob(base64Content);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  const blob = new Blob([bytes], { type: contentType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
};

const formatDate = (value?: string | null) => value ? new Date(value).toLocaleDateString() : 'N/A';

const EmptyListState = ({ message }: { message: string }) => (
  <EmptyStateCompact title="Nothing yet" message={message} />
);

const SchoolPortalContent = ({ mode }: { mode: PortalMode }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const section = sectionFromPath(location.pathname, mode);
  const [search, setSearch] = useState('');
  const [gradeFilter, setGradeFilter] = useState('');
  const [classFilter, setClassFilter] = useState('');
  const [selectedLearnerId, setSelectedLearnerId] = useState<string | null>(null);
  const [learnerForm, setLearnerForm] = useState({ firstName: '', lastName: '', email: '', password: '', selectedGrade: 'Grade 12', careerGoal: '', popiaConsentAccepted: true, consentVersion: 'v1.0' });
  const [assignmentForm, setAssignmentForm] = useState({ learnerUserId: '', classId: '', subjectId: '', teacherUserId: '' });
  const [interventionForm, setInterventionForm] = useState({ learnerUserId: '', supportType: 'Academic Support', priority: 'HIGH', notes: '', followUpDate: '' });
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [generatedLessonPlan, setGeneratedLessonPlan] = useState<TeacherLessonPlanResponse | null>(null);

  const dashboardQuery = useAppQuery({
    queryKey: ['portal', mode, 'dashboard'],
    queryFn: () => mode === 'school' ? schoolService.schoolPortalDashboard() : schoolService.teacherPortalDashboard(),
  });
  const learnersQuery = useAppQuery({
    queryKey: ['portal', mode, 'learners', search, gradeFilter, classFilter],
    queryFn: () => mode === 'school'
      ? schoolService.schoolPortalLearners({ search, grade: gradeFilter, className: classFilter })
      : schoolService.teacherPortalLearners({ search, grade: gradeFilter, className: classFilter }),
    enabled: ['dashboard', 'learners', 'academic-insights', 'career-readiness', 'courses', 'bursaries', 'interventions', 'reports'].includes(section),
  });
  const learnerProfileQuery = useAppQuery({
    queryKey: ['portal', mode, 'learner-profile', selectedLearnerId],
    queryFn: () => mode === 'school' ? schoolService.schoolPortalLearnerProfile(selectedLearnerId as string) : schoolService.teacherPortalLearnerProfile(selectedLearnerId as string),
    enabled: Boolean(selectedLearnerId),
  });
  const insightsQuery = useAppQuery<AcademicInsightsResponse>({
    queryKey: ['portal', mode, 'academic-insights'],
    queryFn: () => mode === 'school' ? schoolService.schoolAcademicInsights() : schoolService.teacherAcademicInsights(),
    enabled: section === 'academic-insights',
  });
  const careerQuery = useAppQuery<CareerReadinessResponse>({
    queryKey: ['portal', mode, 'career-readiness'],
    queryFn: () => mode === 'school' ? schoolService.schoolCareerReadiness() : schoolService.teacherCareerReadiness(),
    enabled: ['career-readiness', 'courses'].includes(section),
  });
  const bursaryQuery = useAppQuery<BursaryReadinessResponse>({
    queryKey: ['portal', mode, 'bursary-readiness'],
    queryFn: () => mode === 'school' ? schoolService.schoolBursaryReadiness() : schoolService.teacherBursaryReadiness(),
    enabled: ['bursaries', 'reports'].includes(section),
  });
  const interventionsQuery = useAppQuery<InterventionReportItem[]>({
    queryKey: ['portal', mode, 'interventions'],
    queryFn: () => mode === 'school' ? schoolService.schoolInterventions() : schoolService.teacherInterventions(),
    enabled: ['dashboard', 'interventions', 'learners'].includes(section),
  });
  const settingsQuery = useAppQuery<PortalSettingsResponse>({
    queryKey: ['portal', mode, 'settings'],
    queryFn: () => mode === 'school' ? schoolService.schoolPortalSettings() : schoolService.teacherPortalSettings(),
    enabled: section === 'settings',
  });
  const classesQuery = useAppQuery({ queryKey: ['portal', 'school', 'classes'], queryFn: schoolService.listClasses, enabled: mode === 'school' && section === 'learners' });
  const subjectsQuery = useAppQuery({ queryKey: ['portal', 'school', 'subjects'], queryFn: schoolService.listSubjects, enabled: mode === 'school' && section === 'learners' });
  const teachersQuery = useAppQuery({ queryKey: ['portal', 'school', 'teachers'], queryFn: schoolService.listTeachers, enabled: mode === 'school' && section === 'learners' });
  const curriculumWidgetsQuery = useAppQuery<TeacherCurriculumWidgets>({
    queryKey: ['teacher-curriculum-widgets'],
    queryFn: schoolService.teacherCurriculumWidgets,
    enabled: mode === 'teacher' && section === 'dashboard',
  });

  const learners = learnersQuery.data?.items ?? [];
  const activeLearnerProfile = learnerProfileQuery.data;

  const refreshPortal = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['portal', mode] }),
      queryClient.invalidateQueries({ queryKey: ['portal', 'school', 'classes'] }),
      queryClient.invalidateQueries({ queryKey: ['portal', 'school', 'subjects'] }),
      queryClient.invalidateQueries({ queryKey: ['portal', 'school', 'teachers'] }),
      queryClient.invalidateQueries({ queryKey: ['teacher-curriculum-widgets'] }),
    ]);
  };

  const createLearner = useMutation({
    mutationFn: () => schoolService.createSchoolUser({ ...learnerForm, roleName: 'ROLE_SCHOOL_STUDENT' }),
    onSuccess: async () => {
      setLearnerForm({ firstName: '', lastName: '', email: '', password: '', selectedGrade: 'Grade 12', careerGoal: '', popiaConsentAccepted: true, consentVersion: 'v1.0' });
      await refreshPortal();
    },
  });
  const bulkUpload = useMutation({
    mutationFn: () => schoolService.bulkUploadLearners(csvFile as File),
    onSuccess: async () => {
      setCsvFile(null);
      await refreshPortal();
    },
  });
  const enrollLearner = useMutation({
    mutationFn: async () => {
      await schoolService.enrollLearner({ learnerUserId: assignmentForm.learnerUserId, classId: assignmentForm.classId, subjectId: assignmentForm.subjectId });
      if (assignmentForm.teacherUserId) {
        await schoolService.assignTeacher({ teacherUserId: assignmentForm.teacherUserId, classId: assignmentForm.classId, subjectId: assignmentForm.subjectId });
      }
    },
    onSuccess: async () => {
      setAssignmentForm({ learnerUserId: '', classId: '', subjectId: '', teacherUserId: '' });
      await refreshPortal();
    },
  });
  const createIntervention = useMutation({
    mutationFn: () => mode === 'school'
      ? schoolService.createSchoolIntervention(interventionForm)
      : schoolService.createTeacherIntervention(interventionForm),
    onSuccess: async () => {
      setInterventionForm({ learnerUserId: '', supportType: 'Academic Support', priority: 'HIGH', notes: '', followUpDate: '' });
      await refreshPortal();
    },
  });
  const updateIntervention = useMutation({
    mutationFn: ({ interventionId, status, notes, followUpDate }: { interventionId: string; status: string; notes: string; followUpDate?: string }) =>
      mode === 'school'
        ? schoolService.updateSchoolIntervention(interventionId, { status, notes, followUpDate })
        : schoolService.updateTeacherIntervention(interventionId, { status, notes, followUpDate }),
    onSuccess: refreshPortal,
  });
  const exportReport = useMutation({
    mutationFn: ({ type, format }: { type: string; format: 'csv' | 'pdf' }) => mode === 'school'
      ? schoolService.exportSchoolReport(type, format)
      : schoolService.exportTeacherReport(type, format),
    onSuccess: (data) => downloadExport(data.fileName, data.contentType, data.base64Content),
  });
  const updateCurriculumProgress = useMutation({
    mutationFn: ({ weekPlanId, status, completionPercent }: { weekPlanId: string; status: string; completionPercent: number }) =>
      schoolService.updateTeacherCurriculumProgress(weekPlanId, { status, completionPercent }),
    onSuccess: refreshPortal,
  });
  const generateLessonPlan = useMutation({
    mutationFn: (weekPlanId: string) => schoolService.generateTeacherLessonPlan(weekPlanId),
    onSuccess: (data) => setGeneratedLessonPlan(data),
  });

  const learnerCards = useMemo(() => {
    const list = learners.length ? learners : [];
    return {
      atRisk: list.filter((item) => item.needsIntervention).length,
      bursaryReady: list.filter((item) => item.bursaryEligible).length,
      profileComplete: list.filter((item) => item.profileComplete).length,
    };
  }, [learners]);

  if (dashboardQuery.isLoading) return <LoadingState message="Loading school portal..." />;
  if (dashboardQuery.isError || !dashboardQuery.data) return <ErrorState message="Unable to load the school portal dashboard." />;

  const renderLearnerTable = (items: LearnerListItem[]) => (
    <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div className="hidden grid-cols-[1.4fr_0.8fr_0.8fr_0.9fr_0.7fr] gap-3 border-b border-slate-200 bg-slate-50 px-5 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 md:grid">
        <span>Learner</span>
        <span>Class</span>
        <span>Teacher</span>
        <span>APS</span>
        <span>Status</span>
      </div>
      <div className="divide-y divide-slate-100">
        {items.map((learner) => (
          <button
            key={learner.learnerUserId}
            type="button"
            onClick={() => setSelectedLearnerId(learner.learnerUserId)}
            className="w-full px-4 py-4 text-left transition hover:bg-slate-50 md:px-5"
          >
            <div className="grid gap-3 md:grid-cols-[1.4fr_0.8fr_0.8fr_0.9fr_0.7fr] md:items-center">
              <span>
                <span className="block font-semibold text-slate-900">{learner.learnerName}</span>
                <span className="block text-xs text-slate-500">{learner.email}</span>
              </span>
              <span className="text-sm text-slate-700"><span className="mr-2 inline-block text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400 md:hidden">Class</span>{learner.grade || 'N/A'} {learner.className || ''}</span>
              <span className="text-sm text-slate-700"><span className="mr-2 inline-block text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400 md:hidden">Teacher</span>{learner.teacherName || 'Unassigned'}</span>
              <span className="text-sm font-semibold text-slate-900"><span className="mr-2 inline-block text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400 md:hidden">APS</span>{learner.apsPoints}</span>
              <span className={`inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-medium ${learner.needsIntervention ? 'bg-amber-50 text-amber-700' : learner.profileComplete ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'}`}>
                {learner.needsIntervention ? 'Support' : learner.profileComplete ? 'Ready' : 'Incomplete'}
              </span>
            </div>
          </button>
        ))}
        {!items.length ? <div className="px-5 py-8 text-sm text-slate-500">No learners match the current filters.</div> : null}
      </div>
    </div>
  );

  return (
    <DashboardShell>
      <div className="rounded-2xl border border-slate-200 bg-[radial-gradient(circle_at_top_left,_#e8f1ff,_#ffffff_55%)] p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex min-w-0 items-start gap-3">
            <DashboardLogo className="block h-12 w-auto shrink-0 object-contain" />
            <div>
            <h1 className="mt-1.5 text-2xl font-semibold text-slate-900">{mode === 'school' ? 'School readiness command centre' : 'Teacher learner readiness workspace'}</h1>
            <p className="mt-2 max-w-3xl text-[13px] leading-6 text-slate-600">
              {mode === 'school'
                ? 'Track learner readiness, academic risk, career alignment, bursary eligibility, and intervention progress from one school-scoped portal.'
                : 'Monitor only your assigned learners, surface readiness gaps early, and log evidence-backed interventions without leaving the teacher workflow.'}
            </p>
            </div>
          </div>
          <div className="grid gap-2 rounded-2xl border border-white/70 bg-white/85 p-3 backdrop-blur sm:grid-cols-3">
            <div><p className="text-xs uppercase tracking-[0.16em] text-slate-500">Profiles complete</p><p className="mt-1 text-xl font-semibold text-slate-900">{learnerCards.profileComplete}</p></div>
            <div><p className="text-xs uppercase tracking-[0.16em] text-slate-500">Need support</p><p className="mt-1 text-xl font-semibold text-slate-900">{learnerCards.atRisk}</p></div>
            <div><p className="text-xs uppercase tracking-[0.16em] text-slate-500">Bursary ready</p><p className="mt-1 text-xl font-semibold text-slate-900">{learnerCards.bursaryReady}</p></div>
          </div>
        </div>
      </div>

      {(createLearner.isError || bulkUpload.isError || enrollLearner.isError || createIntervention.isError || exportReport.isError || updateIntervention.isError)
        ? <ErrorState message={(createLearner.error as Error | null)?.message ?? (bulkUpload.error as Error | null)?.message ?? (enrollLearner.error as Error | null)?.message ?? (createIntervention.error as Error | null)?.message ?? (updateIntervention.error as Error | null)?.message ?? (exportReport.error as Error | null)?.message ?? 'An action failed.'} />
        : null}

      {section === 'dashboard' ? (
        <>
          <PortalCards dashboard={dashboardQuery.data} />
          {mode === 'teacher' && curriculumWidgetsQuery.data ? (
            <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
              <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-slate-900">This Week&apos;s ATP Coverage</h3>
                    <p className="mt-1 text-sm text-slate-600">This week&apos;s curriculum, coverage status, and lesson-plan generation.</p>
                  </div>
                  {curriculumWidgetsQuery.data.thisWeeksCoverage ? (
                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">{curriculumWidgetsQuery.data.thisWeeksCoverage.term} · Week {curriculumWidgetsQuery.data.thisWeeksCoverage.weekNumber}</span>
                  ) : null}
                </div>
                {curriculumWidgetsQuery.data.thisWeeksCoverage ? (
                  <div className="mt-4 space-y-4">
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                      <p className="text-sm font-semibold text-slate-900">{curriculumWidgetsQuery.data.thisWeeksCoverage.subject} · {curriculumWidgetsQuery.data.thisWeeksCoverage.grade}</p>
                      <p className="mt-2 text-xl font-semibold text-slate-900">{curriculumWidgetsQuery.data.thisWeeksCoverage.topic}</p>
                      <p className="mt-1 text-sm text-slate-600">{curriculumWidgetsQuery.data.thisWeeksCoverage.subtopic || 'District ATP guidance available for this week.'}</p>
                      <p className="mt-3 text-sm text-slate-700"><span className="font-semibold text-slate-900">Objectives:</span> {curriculumWidgetsQuery.data.thisWeeksCoverage.learningObjectives || 'No structured objectives mapped yet.'}</p>
                      <p className="mt-2 text-sm text-slate-700"><span className="font-semibold text-slate-900">Activities:</span> {curriculumWidgetsQuery.data.thisWeeksCoverage.lessonFocus || 'No suggested activities mapped yet.'}</p>
                      <p className="mt-2 text-sm text-slate-700"><span className="font-semibold text-slate-900">Assessment:</span> {curriculumWidgetsQuery.data.thisWeeksCoverage.assessmentTask || 'No assessment task mapped yet.'}</p>
                      <p className="mt-2 text-sm text-slate-700"><span className="font-semibold text-slate-900">Resources:</span> {curriculumWidgetsQuery.data.thisWeeksCoverage.resources || 'No resources mapped yet.'}</p>
                      <div className="mt-4 h-2 rounded-full bg-slate-100">
                        <div className="h-2 rounded-full bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF]" style={{ width: `${curriculumWidgetsQuery.data.thisWeeksCoverage.progressPercent}%` }} />
                      </div>
                      <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-500">{curriculumWidgetsQuery.data.thisWeeksCoverage.status.split('_').join(' ')} · {curriculumWidgetsQuery.data.thisWeeksCoverage.progressPercent}%</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" onClick={() => updateCurriculumProgress.mutate({ weekPlanId: curriculumWidgetsQuery.data.thisWeeksCoverage!.weekPlanId, status: 'NOT_STARTED', completionPercent: 0 })} className="rounded-2xl bg-slate-200 px-4 py-2 text-slate-900 hover:bg-slate-300">Not Started</Button>
                      <Button type="button" onClick={() => updateCurriculumProgress.mutate({ weekPlanId: curriculumWidgetsQuery.data.thisWeeksCoverage!.weekPlanId, status: 'IN_PROGRESS', completionPercent: 60 })} className="rounded-2xl bg-amber-500 px-4 py-2 hover:bg-amber-600">In Progress</Button>
                      <Button type="button" onClick={() => updateCurriculumProgress.mutate({ weekPlanId: curriculumWidgetsQuery.data.thisWeeksCoverage!.weekPlanId, status: 'COMPLETED', completionPercent: 100 })} className="rounded-2xl bg-emerald-600 px-4 py-2 hover:bg-emerald-700">Completed</Button>
                      <Button type="button" onClick={() => generateLessonPlan.mutate(curriculumWidgetsQuery.data.thisWeeksCoverage!.weekPlanId)} className="rounded-2xl bg-slate-900 px-4 py-2 hover:bg-slate-800">Generate Lesson Plan</Button>
                    </div>
                    {generatedLessonPlan ? (
                      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
                        <p className="font-semibold text-slate-900">{generatedLessonPlan.title}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Objectives:</span> {generatedLessonPlan.learningObjectives}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Introduction:</span> {generatedLessonPlan.introduction}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Activities:</span> {generatedLessonPlan.activities}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Resources:</span> {generatedLessonPlan.resources}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Assessment:</span> {generatedLessonPlan.assessment}</p>
                        <p className="mt-2"><span className="font-semibold text-slate-900">Homework:</span> {generatedLessonPlan.homework}</p>
                      </div>
                    ) : null}
                  </div>
                ) : curriculumWidgetsQuery.data.visibleTopics?.length ? (
                  <div className="mt-4 space-y-3">
                    <p className="text-sm text-amber-700">No ATP item is mapped to this week yet, but published ATP calendar items are available below.</p>
                    {curriculumWidgetsQuery.data.visibleTopics.slice(0, 4).map((item) => (
                      <div key={item.weekPlanId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900">{item.subject} · {item.grade}</p>
                        <p className="mt-1 text-xs uppercase tracking-[0.14em] text-slate-500">{item.term} · Week {item.weekNumber}</p>
                        <p className="mt-2 text-slate-900">{item.topic}</p>
                        <p className="mt-1 text-sm text-slate-600">{item.subtopic || item.learningObjectives || item.lessonFocus || 'Published ATP topic available.'}</p>
                      </div>
                    ))}
                  </div>
                ) : <p className="mt-4 text-sm text-slate-500">No weekly ATP topic has been mapped yet.</p>}
              </section>
              <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-slate-900">District Resources</h3>
                    <p className="mt-1 text-sm text-slate-600">Latest district-approved resources relevant to this teacher.</p>
                  </div>
                  <Button type="button" className="rounded-2xl bg-slate-900 px-4 py-2 hover:bg-slate-800" onClick={() => navigate('/teacher/curriculum')}>
                    View Curriculum Resources
                  </Button>
                </div>
                <div className="mt-4 space-y-3">
                  {curriculumWidgetsQuery.data.districtResources.slice(0, 5).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-xs text-slate-500">{item.subject} · {item.grade} · {item.badge}</p>
                    </div>
                  ))}
                  {!curriculumWidgetsQuery.data.districtResources.length ? <p className="text-sm text-slate-500">No curriculum resources are available yet.</p> : null}
                </div>
                <div className="mt-5">
                  <h4 className="text-sm font-semibold text-slate-900">Reminders</h4>
                  <div className="mt-3 space-y-3">
                    {(curriculumWidgetsQuery.data.reminders ?? []).map((item) => (
                      <div key={`${item.reminderType}-${item.title}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                        <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                        <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                      </div>
                    ))}
                    {!(curriculumWidgetsQuery.data.reminders ?? []).length ? <p className="text-sm text-slate-500">No curriculum reminders for today.</p> : null}
                  </div>
                </div>
              </section>
            </div>
          ) : null}
          <div className="grid gap-4 xl:grid-cols-2">
            <BreakdownPanel title="Top Career Interests" items={dashboardQuery.data.topCareerInterests} />
            <BreakdownPanel title="Top Subject Risk Areas" items={dashboardQuery.data.topSubjectRiskAreas} />
          </div>
          <div className="grid gap-4 xl:grid-cols-[1.4fr_0.6fr]">
            {renderLearnerTable(learners.slice(0, 8))}
            <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="text-base font-semibold text-slate-900">Active interventions</h3>
              <div className="mt-4 space-y-3">
                {(interventionsQuery.data ?? []).slice(0, 6).map((item) => (
                  <div key={item.interventionId} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-slate-900">{item.learnerName}</p>
                      <span className="rounded-full bg-amber-50 px-2 py-1 text-[11px] font-medium text-amber-700">{item.status}</span>
                    </div>
                    <p className="mt-1 text-xs text-slate-600">{item.supportType} · {item.priority}</p>
                    <p className="mt-2 text-sm text-slate-700">{item.notes}</p>
                  </div>
                ))}
                {!(interventionsQuery.data ?? []).length ? <p className="text-sm text-slate-500">No interventions logged yet.</p> : null}
              </div>
            </section>
          </div>
        </>
      ) : null}

      {section === 'learners' ? (
        <div className="grid gap-4 xl:grid-cols-[1.25fr_0.75fr]">
          <div className="space-y-4">
            <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-center gap-3">
                <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search learners" className="h-11 flex-1 rounded-2xl" />
                <Input value={gradeFilter} onChange={(event) => setGradeFilter(event.target.value)} placeholder="Filter grade" className="h-11 w-36 rounded-2xl" />
                <Input value={classFilter} onChange={(event) => setClassFilter(event.target.value)} placeholder="Filter class" className="h-11 w-36 rounded-2xl" />
              </div>
            </section>
            {renderLearnerTable(learners)}
            {activeLearnerProfile ? (
              <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{activeLearnerProfile.learnerName}</h3>
                    <p className="text-sm text-slate-500">{activeLearnerProfile.grade || 'N/A'} {activeLearnerProfile.className || ''} · {activeLearnerProfile.teacherName || 'Unassigned'}</p>
                  </div>
                  <div className="flex gap-2">
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">APS {activeLearnerProfile.apsPoints}</span>
                    <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">{activeLearnerProfile.popiaStatus}</span>
                  </div>
                </div>
                <div className="mt-5 grid gap-4 lg:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Student overview</h4>
                    <p className="mt-2 text-sm text-slate-700">Career goal: {activeLearnerProfile.careerGoal || 'Not set'}</p>
                    <p className="mt-1 text-sm text-slate-700">Qualification level: {activeLearnerProfile.qualificationLevel || 'Not set'}</p>
                    <p className="mt-1 text-sm text-slate-700">Interests: {activeLearnerProfile.interests || 'Not set'}</p>
                    <p className="mt-1 text-sm text-slate-700">Skills: {activeLearnerProfile.skills || 'Not set'}</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Subjects and marks</h4>
                    <div className="mt-2 space-y-2">
                      {activeLearnerProfile.subjects.map((subject) => (
                        <div key={subject.subjectName} className="flex items-center justify-between gap-3 text-sm">
                          <span className="text-slate-700">{subject.subjectName}</span>
                          <span className={`font-semibold ${subject.risk ? 'text-amber-700' : 'text-slate-900'}`}>{Math.round(subject.markPercent)}%</span>
                        </div>
                      ))}
                      {!activeLearnerProfile.subjects.length ? <EmptyListState message="No subject marks uploaded yet." /> : null}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Course eligibility</h4>
                    <div className="mt-2 space-y-2">
                      {activeLearnerProfile.courseEligibility.map((course) => (
                        <div key={course.name} className="rounded-xl border border-slate-200 bg-white p-3">
                          <div className="flex items-center justify-between gap-2">
                            <p className="text-sm font-semibold text-slate-900">{course.name}</p>
                            <span className={`rounded-full px-2 py-1 text-[11px] font-medium ${course.eligible ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>{course.eligible ? 'Eligible' : 'Gap'}</span>
                          </div>
                          <p className="mt-1 text-xs text-slate-500">{course.level || 'Pathway'} · {course.reason}</p>
                        </div>
                      ))}
                      {!activeLearnerProfile.courseEligibility.length ? <EmptyListState message="No course pathways calculated yet." /> : null}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Bursary matches</h4>
                    <div className="mt-2 space-y-2">
                      {activeLearnerProfile.bursaryMatches.map((match) => (
                        <div key={`${match.title}-${match.provider || 'provider'}`} className="rounded-xl border border-slate-200 bg-white p-3">
                          <p className="text-sm font-semibold text-slate-900">{match.title}</p>
                          <p className="mt-1 text-xs text-slate-500">{match.provider || 'Funding provider'} · Deadline {formatDate(match.deadline)}</p>
                          <p className="mt-1 text-xs text-slate-600">{match.missingRequirements || 'No major missing requirement flagged.'}</p>
                        </div>
                      ))}
                      {!activeLearnerProfile.bursaryMatches.length ? <EmptyListState message="No bursary matches available for this learner yet." /> : null}
                    </div>
                  </div>
                </div>
                <div className="mt-5 grid gap-4 lg:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Intervention plan</h4>
                    <div className="mt-2 space-y-2">
                      {activeLearnerProfile.interventions.map((item) => (
                        <div key={item.interventionId} className="rounded-xl border border-slate-200 bg-white p-3">
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-sm font-semibold text-slate-900">{item.supportType}</span>
                            <span className="rounded-full bg-amber-50 px-2 py-1 text-[11px] font-medium text-amber-700">{item.status}</span>
                          </div>
                          <p className="mt-1 text-xs text-slate-600">{item.notes}</p>
                        </div>
                      ))}
                      {!activeLearnerProfile.interventions.length ? <EmptyListState message="No intervention plan has been logged yet." /> : null}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Activity timeline</h4>
                    <div className="mt-2 space-y-2">
                      {activeLearnerProfile.activityTimeline.map((item) => (
                        <div key={`${item.title}-${item.occurredAt}`} className="rounded-xl border border-slate-200 bg-white p-3">
                          <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                          <p className="mt-1 text-xs text-slate-600">{item.detail}</p>
                          <p className="mt-1 text-[11px] uppercase tracking-[0.12em] text-slate-400">{new Date(item.occurredAt).toLocaleString()}</p>
                        </div>
                      ))}
                      {!activeLearnerProfile.activityTimeline.length ? <EmptyListState message="No learner activity has been recorded yet." /> : null}
                    </div>
                  </div>
                </div>
              </section>
            ) : null}
          </div>

          <div className="space-y-4">
            {mode === 'school' ? (
              <>
                <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                  <h3 className="text-base font-semibold text-slate-900">Add learner manually</h3>
                  <div className="mt-4 grid gap-3">
                    <Input value={learnerForm.firstName} onChange={(event) => setLearnerForm((state) => ({ ...state, firstName: event.target.value }))} placeholder="First name" className="h-11 rounded-2xl" />
                    <Input value={learnerForm.lastName} onChange={(event) => setLearnerForm((state) => ({ ...state, lastName: event.target.value }))} placeholder="Last name" className="h-11 rounded-2xl" />
                    <Input value={learnerForm.email} onChange={(event) => setLearnerForm((state) => ({ ...state, email: event.target.value }))} placeholder="Email" className="h-11 rounded-2xl" />
                    <Input value={learnerForm.password} onChange={(event) => setLearnerForm((state) => ({ ...state, password: event.target.value }))} placeholder="Temporary password" className="h-11 rounded-2xl" />
                    <Input value={learnerForm.selectedGrade} onChange={(event) => setLearnerForm((state) => ({ ...state, selectedGrade: event.target.value }))} placeholder="Selected grade" className="h-11 rounded-2xl" />
                    <Input value={learnerForm.careerGoal} onChange={(event) => setLearnerForm((state) => ({ ...state, careerGoal: event.target.value }))} placeholder="Career goal" className="h-11 rounded-2xl" />
                    <label className="flex items-center gap-2 text-sm text-slate-600">
                      <input type="checkbox" checked={learnerForm.popiaConsentAccepted} onChange={(event) => setLearnerForm((state) => ({ ...state, popiaConsentAccepted: event.target.checked }))} />
                      POPIA consent captured
                    </label>
                    <Button type="button" disabled={createLearner.isPending} onClick={() => createLearner.mutate()} className="h-11 rounded-2xl bg-[#0B5BFF] hover:bg-[#0849cb]">
                      {createLearner.isPending ? 'Saving...' : 'Create learner'}
                    </Button>
                  </div>
                </section>

                <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                  <h3 className="text-base font-semibold text-slate-900">Bulk upload learners by CSV</h3>
                  <input type="file" accept=".csv" onChange={(event) => setCsvFile(event.target.files?.[0] ?? null)} className="mt-4 block w-full text-sm text-slate-600" />
                  <Button type="button" disabled={!csvFile || bulkUpload.isPending} onClick={() => bulkUpload.mutate()} className="mt-4 h-11 rounded-2xl bg-slate-900 hover:bg-slate-800">
                    {bulkUpload.isPending ? 'Uploading...' : 'Upload CSV'}
                  </Button>
                </section>

                <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                  <h3 className="text-base font-semibold text-slate-900">Assign learner to grade, class, and teacher</h3>
                  <div className="mt-4 grid gap-3">
                    <select value={assignmentForm.learnerUserId} onChange={(event) => setAssignmentForm((state) => ({ ...state, learnerUserId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                      <option value="">Select learner</option>
                      {learners.map((learner) => <option key={learner.learnerUserId} value={learner.learnerUserId}>{learner.learnerName}</option>)}
                    </select>
                    <select value={assignmentForm.classId} onChange={(event) => setAssignmentForm((state) => ({ ...state, classId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                      <option value="">Select class</option>
                      {(classesQuery.data ?? []).map((item) => <option key={item.id} value={item.id}>{item.grade} {item.className}</option>)}
                    </select>
                    <select value={assignmentForm.subjectId} onChange={(event) => setAssignmentForm((state) => ({ ...state, subjectId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                      <option value="">Select subject</option>
                      {(subjectsQuery.data ?? []).map((item) => <option key={item.id} value={item.id}>{item.subjectName}</option>)}
                    </select>
                    <select value={assignmentForm.teacherUserId} onChange={(event) => setAssignmentForm((state) => ({ ...state, teacherUserId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                      <option value="">Select teacher</option>
                      {(teachersQuery.data ?? []).map((item) => <option key={item.userId} value={item.userId}>{item.fullName}</option>)}
                    </select>
                    <Button type="button" disabled={enrollLearner.isPending || !assignmentForm.learnerUserId || !assignmentForm.classId || !assignmentForm.subjectId} onClick={() => enrollLearner.mutate()} className="h-11 rounded-2xl bg-emerald-600 hover:bg-emerald-700">
                      {enrollLearner.isPending ? 'Linking...' : 'Link learner'}
                    </Button>
                  </div>
                </section>
              </>
            ) : (
              <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <h3 className="text-base font-semibold text-slate-900">Teacher scope</h3>
                <p className="mt-3 text-sm text-slate-600">You can only see learners assigned to your classes and subjects. The portal data on this account is automatically scoped by teacher assignment.</p>
              </section>
            )}
          </div>
        </div>
      ) : null}

      {section === 'academic-insights' ? (
        <div className="space-y-4">
          {insightsQuery.isLoading ? <LoadingState message="Loading academic insights..." /> : null}
          {insightsQuery.data ? (
            <>
              <div className="grid gap-4 lg:grid-cols-3">
                <BreakdownPanel title="Grade performance" items={insightsQuery.data.gradePerformance.map((item) => ({ label: item.label, value: Math.round(item.value) }))} />
                <BreakdownPanel title="Subject performance" items={insightsQuery.data.subjectPerformance.map((item) => ({ label: item.label, value: Math.round(item.value) }))} />
                <BreakdownPanel title="Class performance" items={insightsQuery.data.classPerformance.map((item) => ({ label: item.label, value: Math.round(item.value) }))} />
              </div>
              <div className="grid gap-4 xl:grid-cols-2">
                {renderLearnerTable(insightsQuery.data.atRiskLearners)}
                <BreakdownPanel title="Subjects affecting career eligibility" items={insightsQuery.data.subjectsAffectingCareerEligibility} />
              </div>
            </>
          ) : null}
        </div>
      ) : null}

      {section === 'career-readiness' ? (
        <div className="grid gap-4 xl:grid-cols-[1.4fr_0.6fr]">
          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="grid grid-cols-[1.1fr_1fr_0.7fr_1fr_1fr] gap-3 border-b border-slate-200 bg-slate-50 px-5 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
              <span>Learner</span>
              <span>Career</span>
              <span>APS</span>
              <span>Gap</span>
              <span>Alternative pathway</span>
            </div>
            <div className="divide-y divide-slate-100">
              {(careerQuery.data?.learners ?? []).map((item) => (
                <div key={item.learnerUserId} className="grid grid-cols-[1.1fr_1fr_0.7fr_1fr_1fr] gap-3 px-5 py-3 text-sm">
                  <span className="font-semibold text-slate-900">{item.learnerName}</span>
                  <span className="text-slate-700">{item.careerGoal || 'Not selected'}</span>
                  <span className="text-slate-700">{item.apsPoints}</span>
                  <span className={`${item.aligned ? 'text-emerald-700' : 'text-amber-700'}`}>{item.readinessGap}</span>
                  <span className="text-slate-700">{item.alternativePathway}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="space-y-4">
            <BreakdownPanel title="Top career interests" items={careerQuery.data?.topCareerInterests ?? []} />
            <BreakdownPanel title="Readiness gaps" items={careerQuery.data?.readinessGaps ?? []} />
            <BreakdownPanel title="Alternative pathways" items={careerQuery.data?.alternativePathways ?? []} />
          </div>
        </div>
      ) : null}

      {section === 'courses' ? (
        <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Learner course eligibility</h3>
            <p className="mt-2 text-sm text-slate-600">Open a learner profile from the learner list to inspect subject-level course eligibility. This view highlights pathway readiness by learner.</p>
            <div className="mt-4 space-y-3">
              {learners.slice(0, 8).map((learner) => (
                <button key={learner.learnerUserId} type="button" onClick={() => setSelectedLearnerId(learner.learnerUserId)} className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left hover:bg-slate-100">
                  <span>
                    <span className="block text-sm font-semibold text-slate-900">{learner.learnerName}</span>
                    <span className="block text-xs text-slate-500">{learner.careerGoal || 'No career goal set'}</span>
                  </span>
                  <span className="rounded-full bg-slate-900 px-3 py-1 text-xs font-medium text-white">APS {learner.apsPoints}</span>
                </button>
              ))}
            </div>
          </section>
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Pathway suggestions</h3>
            <div className="mt-4 space-y-3">
              {(careerQuery.data?.learners ?? []).slice(0, 10).map((item) => (
                <div key={item.learnerUserId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900">{item.learnerName}</p>
                  <p className="mt-1 text-xs text-slate-600">{item.careerGoal || 'Career not selected'}</p>
                  <p className="mt-2 text-sm text-slate-700">{item.alternativePathway}</p>
                </div>
              ))}
            </div>
          </section>
        </div>
      ) : null}

      {section === 'bursaries' ? (
        <div className="grid gap-4 xl:grid-cols-[1.35fr_0.65fr]">
          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="grid grid-cols-[1fr_1fr_0.8fr_0.9fr_1fr] gap-3 border-b border-slate-200 bg-slate-50 px-5 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
              <span>Learner</span>
              <span>Bursary</span>
              <span>Deadline</span>
              <span>Missing requirements</span>
              <span>Checklist</span>
            </div>
            <div className="divide-y divide-slate-100">
              {(bursaryQuery.data?.matches ?? []).map((item) => (
                <div key={`${item.learnerUserId}-${item.bursaryTitle}`} className="grid grid-cols-[1fr_1fr_0.8fr_0.9fr_1fr] gap-3 px-5 py-3 text-sm">
                  <span className="font-semibold text-slate-900">{item.learnerName}</span>
                  <span className="text-slate-700">{item.bursaryTitle}</span>
                  <span className="text-slate-700">{formatDate(item.deadline)}</span>
                  <span className="text-amber-700">{item.missingRequirements || 'None flagged'}</span>
                  <span className="text-slate-700">{item.checklist}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="space-y-4">
            <BreakdownPanel title="Missing requirements" items={bursaryQuery.data?.missingRequirements ?? []} />
            <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="text-base font-semibold text-slate-900">Deadline alerts</h3>
              <div className="mt-4 space-y-3">
                {(bursaryQuery.data?.deadlineAlerts ?? []).map((item) => (
                  <div key={`${item.learnerUserId}-${item.bursaryTitle}-deadline`} className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
                    <p className="text-sm font-semibold text-slate-900">{item.learnerName}</p>
                    <p className="mt-1 text-xs text-slate-600">{item.bursaryTitle}</p>
                    <p className="mt-2 text-sm text-amber-800">Deadline {formatDate(item.deadline)}</p>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </div>
      ) : null}

      {section === 'interventions' ? (
        <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
          <div className="space-y-4">
            <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="text-base font-semibold text-slate-900">Intervention register</h3>
              <div className="mt-4 space-y-3">
                {(interventionsQuery.data ?? []).map((item) => (
                  <div key={item.interventionId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">{item.learnerName}</p>
                        <p className="mt-1 text-xs text-slate-500">{item.supportType} · {item.priority} · Assigned by {item.assignedBy}</p>
                      </div>
                      <div className="flex gap-2">
                        {mode === 'school' ? (
                          <>
                            <Button type="button" className="h-9 rounded-xl bg-emerald-600 px-3 text-xs hover:bg-emerald-700" onClick={() => updateIntervention.mutate({ interventionId: item.interventionId, status: 'IN_PROGRESS', notes: item.notes, followUpDate: item.followUpDate ?? undefined })}>In Progress</Button>
                            <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-xs hover:bg-slate-800" onClick={() => updateIntervention.mutate({ interventionId: item.interventionId, status: 'CLOSED', notes: item.notes, followUpDate: item.followUpDate ?? undefined })}>Close</Button>
                          </>
                        ) : null}
                        <span className="rounded-full bg-amber-50 px-2.5 py-1 text-[11px] font-medium text-amber-700">{item.status}</span>
                      </div>
                    </div>
                    <p className="mt-3 text-sm text-slate-700">{item.notes}</p>
                    <p className="mt-2 text-xs text-slate-500">Follow-up: {formatDate(item.followUpDate)} · Updated {new Date(item.updatedAt).toLocaleString()}</p>
                  </div>
                ))}
              </div>
            </section>
          </div>
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Flag learner for support</h3>
            <div className="mt-4 grid gap-3">
              <select value={interventionForm.learnerUserId} onChange={(event) => setInterventionForm((state) => ({ ...state, learnerUserId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select learner</option>
                {learners.map((learner) => <option key={learner.learnerUserId} value={learner.learnerUserId}>{learner.learnerName}</option>)}
              </select>
              <Input value={interventionForm.supportType} onChange={(event) => setInterventionForm((state) => ({ ...state, supportType: event.target.value }))} placeholder="Support type" className="h-11 rounded-2xl" />
              <select value={interventionForm.priority} onChange={(event) => setInterventionForm((state) => ({ ...state, priority: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
              <Input value={interventionForm.followUpDate} onChange={(event) => setInterventionForm((state) => ({ ...state, followUpDate: event.target.value }))} type="date" className="h-11 rounded-2xl" />
              <textarea value={interventionForm.notes} onChange={(event) => setInterventionForm((state) => ({ ...state, notes: event.target.value }))} rows={5} className="w-full rounded-2xl border border-slate-200 px-3 py-3 text-sm text-slate-800 outline-none focus:border-primary-300 focus:ring-4 focus:ring-primary-100" placeholder="Teacher notes and intervention plan" />
              <Button type="button" disabled={createIntervention.isPending || !interventionForm.learnerUserId || !interventionForm.notes.trim()} onClick={() => createIntervention.mutate()} className="h-11 rounded-2xl bg-[#0B5BFF] hover:bg-[#0849cb]">
                {createIntervention.isPending ? 'Saving...' : 'Log intervention'}
              </Button>
            </div>
          </section>
        </div>
      ) : null}

      {section === 'curriculum' && mode === 'teacher' ? (
        <TeacherCurriculumPage latestResources={curriculumWidgetsQuery.data?.districtResources ?? []} />
      ) : null}

      {section === 'reports' ? (
        <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Export reports</h3>
            <div className="mt-4 space-y-3">
              {reportTypes.map((report) => (
                <div key={report.key} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <span className="text-sm font-medium text-slate-800">{report.label}</span>
                  <div className="flex gap-2">
                    <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-xs hover:bg-slate-800" onClick={() => exportReport.mutate({ type: report.key, format: 'csv' })}>CSV</Button>
                    <Button type="button" className="h-9 rounded-xl bg-[#0B5BFF] px-3 text-xs hover:bg-[#0849cb]" onClick={() => exportReport.mutate({ type: report.key, format: 'pdf' })}>PDF</Button>
                  </div>
                </div>
              ))}
            </div>
          </section>
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Report coverage</h3>
            <ul className="mt-4 space-y-3 text-sm text-slate-700">
              <li>Whole-school readiness report</li>
              <li>Grade readiness report</li>
              <li>Subject gap report</li>
              <li>Career interest report</li>
              <li>Bursary readiness report</li>
              <li>At-risk learner report</li>
            </ul>
            <p className="mt-4 text-sm text-slate-500">Exports respect role scope: school users get school-wide data; teachers get assigned-learner data only.</p>
          </section>
        </div>
      ) : null}

      {section === 'settings' ? (
        <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Portal settings</h3>
            <div className="mt-4 space-y-3 text-sm text-slate-700">
              <p>School: {settingsQuery.data?.schoolName || user?.fullName || 'EduRite School'}</p>
              <p>District: {settingsQuery.data?.district || 'Not set'}</p>
              <p>Province: {settingsQuery.data?.province || 'Not set'}</p>
              <p>Roles active: {(settingsQuery.data?.activeRoles ?? []).join(', ') || 'No roles mapped'}</p>
              <p>POPIA guard: learner creation and imports require recorded consent confirmation.</p>
              <p>District / Department phase 2: reserved for aggregated-only reporting.</p>
            </div>
          </section>
          <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Recent audit logs</h3>
            <div className="mt-4 space-y-3">
              {(settingsQuery.data?.recentAuditLogs ?? []).map((log) => (
                <div key={`${log.action}-${log.createdAt}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900">{log.action}</p>
                  <p className="mt-1 text-xs text-slate-500">{new Date(log.createdAt).toLocaleString()}</p>
                </div>
              ))}
              {!(settingsQuery.data?.recentAuditLogs ?? []).length ? <p className="text-sm text-slate-500">No audit events available yet.</p> : null}
            </div>
          </section>
        </div>
      ) : null}
    </DashboardShell>
  );
};

export const SchoolAdminPortalPage = () => <SchoolPortalContent mode="school" />;
export const TeacherPortalPage = () => <SchoolPortalContent mode="teacher" />;
