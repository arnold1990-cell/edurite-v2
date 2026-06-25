import { useMemo, useRef, useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { DashboardLogo } from '@/components/common/DashboardLogo';
import {
  Activity,
  BarChart3,
  Bell,
  BookOpen,
  Bot,
  Briefcase,
  ChevronRight,
  ClipboardList,
  FileBarChart2,
  FileSpreadsheet,
  GraduationCap,
  HandHeart,
  LayoutDashboard,
  LifeBuoy,
  Loader2,
  LogOut,
  Menu,
  Plus,
  Search,
  Send,
  Settings,
  ShieldCheck,
  Sparkles,
  Users,
  UserSquare2,
  WandSparkles,
  X,
} from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { AssignmentManagementPanel } from '@/components/school/admin/AssignmentManagementPanel';
import {
  AdminActionButton,
  AdminBadge,
  AdminCard,
  AdminDataTable,
  AdminMetricCard,
  AdminPageHeader,
  AdminPageLayout,
  AdminRightPanel,
} from '@/components/school/admin/AdminUi';
import { SubjectManagementPanel } from '@/components/school/admin/SubjectManagementPanel';
import { TeacherManagementPanel } from '@/components/school/admin/TeacherManagementPanel';
import { DashboardKpiCard, DashboardSectionCard, DashboardShell, EmptyStateCompact, QuickActionButton, RiskBadge } from '@/components/dashboard/DashboardPrimitives';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ErrorState } from '@/components/feedback/States';
import { SchoolCurriculumPage } from '@/pages/school/SchoolCurriculumPage';
import { schoolService } from '@/services/schoolService';

type CommandSection =
  | 'dashboard'
  | 'analytics'
  | 'ai-insights'
  | 'learners'
  | 'my-school-requests'
  | 'teachers'
  | 'classes'
  | 'subjects'
  | 'curriculum'
  | 'assignments'
  | 'assessments'
  | 'results'
  | 'career-readiness'
  | 'courses'
  | 'bursaries'
  | 'interventions'
  | 'reports'
  | 'announcements'
  | 'notifications'
  | 'support-requests'
  | 'school-settings';

type NavItem = { label: string; section?: CommandSection; href?: string; icon: typeof LayoutDashboard };
type NavGroup = { title: string; items: NavItem[] };

const NAV_GROUPS: NavGroup[] = [
  {
    title: 'Overview',
    items: [
      { label: 'Dashboard', section: 'dashboard', icon: LayoutDashboard },
      { label: 'Analytics / Academic Insights', section: 'analytics', icon: BarChart3 },
      { label: 'AI Insights', section: 'ai-insights', icon: Bot },
    ],
  },
  {
    title: 'Academics',
    items: [
      { label: 'Learners', section: 'learners', icon: Users },
      { label: 'Learner Join Requests', section: 'my-school-requests', icon: Users },
      { label: 'Teachers', section: 'teachers', icon: UserSquare2 },
      { label: 'Classes', section: 'classes', icon: GraduationCap },
      { label: 'Subjects', section: 'subjects', icon: BookOpen },
      { label: 'Curriculum', section: 'curriculum', icon: FileSpreadsheet },
    ],
  },
  {
    title: 'Career & Readiness',
    items: [
      { label: 'Career Readiness', section: 'career-readiness', icon: Briefcase },
      { label: 'Courses', section: 'courses', icon: ClipboardList },
      { label: 'Bursaries', section: 'bursaries', icon: HandHeart },
      { label: 'Interventions', section: 'interventions', icon: WandSparkles },
    ],
  },
  {
    title: 'Delivery',
    items: [
      { label: 'Assignments', section: 'assignments', icon: ClipboardList },
      { label: 'Assessments', section: 'assessments', icon: FileBarChart2 },
      { label: 'Results', section: 'results', icon: Activity },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Reports', section: 'reports', icon: FileSpreadsheet },
      { label: 'Announcements', section: 'announcements', icon: Send },
      { label: 'Notifications', section: 'notifications', icon: Bell },
      { label: 'Support Requests', section: 'support-requests', icon: LifeBuoy },
    ],
  },
  {
    title: 'Settings',
    items: [
      { label: 'School Settings', section: 'school-settings', icon: Settings },
      { label: 'Security / Change Password', href: '/account/change-password', icon: ShieldCheck },
      { label: 'Logout', href: '/logout', icon: LogOut },
    ],
  },
];

const sectionFromPath = (pathname: string): CommandSection => {
  const suffix = pathname.startsWith('/school/') ? pathname.slice('/school/'.length) : 'dashboard';
  if (suffix.startsWith('analytics') || suffix.startsWith('academic-insights')) return 'analytics';
  if (suffix.startsWith('ai-insights')) return 'ai-insights';
  if (suffix.startsWith('learners')) return 'learners';
  if (suffix.startsWith('my-school-requests')) return 'my-school-requests';
  if (suffix.startsWith('teachers')) return 'teachers';
  if (suffix.startsWith('classes')) return 'classes';
  if (suffix.startsWith('subjects')) return 'subjects';
  if (suffix.startsWith('curriculum')) return 'curriculum';
  if (suffix.startsWith('assignments')) return 'assignments';
  if (suffix.startsWith('assessments')) return 'assessments';
  if (suffix.startsWith('results')) return 'results';
  if (suffix.startsWith('career-readiness')) return 'career-readiness';
  if (suffix.startsWith('courses')) return 'courses';
  if (suffix.startsWith('bursaries')) return 'bursaries';
  if (suffix.startsWith('interventions')) return 'interventions';
  if (suffix.startsWith('reports')) return 'reports';
  if (suffix.startsWith('announcements')) return 'announcements';
  if (suffix.startsWith('notifications')) return 'notifications';
  if (suffix.startsWith('support-requests')) return 'support-requests';
  if (suffix.startsWith('school-settings') || suffix.startsWith('settings')) return 'school-settings';
  return 'dashboard';
};

const sectionPath = (section: CommandSection) => `/school/${section}`;

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

const SectionShell = ({ title, subtitle, actions, children }: { title: string; subtitle: string; actions?: ReactNode; children: ReactNode }) => (
  <AdminPageLayout>
    <AdminPageHeader title={title} subtitle={subtitle} actions={actions} />
    {children}
  </AdminPageLayout>
);

const MetricCard = ({ label, value, helperText, tone }: { label: string; value: string; helperText: string; tone: string }) => (
  <AdminMetricCard label={label} value={value} helperText={helperText} tone={tone} trendLabel={tone.replace('-', ' ')} />
);

const ChartPanel = ({ title, points }: { title: string; points: Array<{ label: string; value: number; tone: string }> }) => (
  <section className="rounded-[18px] border border-slate-200 bg-white p-[20px] shadow-sm">
    <h3 className="text-[17px] font-semibold leading-snug text-slate-950">{title}</h3>
    <div className="mt-4 space-y-3">
      {points.length ? points.map((point) => (
        <div key={`${title}-${point.label}`} className="space-y-1.5">
          <div className="flex items-center justify-between gap-3 text-[15px] leading-6">
            <span className="truncate text-slate-700">{point.label}</span>
            <span className="font-semibold text-slate-950">{point.value}</span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
            <div
              className={`h-full rounded-full ${point.tone === 'critical' ? 'bg-rose-500' : point.tone === 'warning' ? 'bg-amber-500' : 'bg-gradient-to-r from-blue-600 to-cyan-500'}`}
              style={{ width: `${Math.max(8, Math.min(100, point.value))}%` }}
            />
          </div>
        </div>
      )) : <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-5 text-[15px] text-slate-500">No data available yet.</div>}
    </div>
  </section>
);

const InsightList = ({ title, items, emptyMessage }: { title: string; items: Array<{ title: string; detail: string; severity: string }>; emptyMessage: string }) => (
  <AdminCard className="p-[18px]">
    <h3 className="text-[17px] font-semibold leading-snug text-slate-950">{title}</h3>
    <div className="mt-4 space-y-3">
      {items.length ? items.map((item) => (
        <div key={`${title}-${item.title}-${item.detail}`} className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <p className="text-[15px] font-semibold leading-6 text-slate-900">{item.title}</p>
            <AdminBadge label={item.severity} tone={item.severity} />
          </div>
          <p className="mt-2 text-[15px] leading-6 text-slate-600">{item.detail}</p>
        </div>
      )) : <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-5 text-[15px] text-slate-500">{emptyMessage}</div>}
    </div>
  </AdminCard>
);

const TableShell = ({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) => (
  <AdminDataTable title={title} action={action}>{children}</AdminDataTable>
);

const LoadingGrid = () => (
  <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
    {Array.from({ length: 8 }).map((_, index) => <div key={index} className="h-32 animate-pulse rounded-[18px] bg-white/70" />)}
  </div>
);

const formatDate = (value?: string | null) => {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const formatDateShort = (value?: string | null) => {
  if (!value) return 'Not set';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
};

const QuickChip = ({ label, tone = 'neutral' }: { label: string; tone?: string }) => (
  <AdminBadge label={label} tone={tone} className="text-[11px]" />
);

const CompactStatCard = ({ label, value, helper }: { label: string; value: string; helper: string }) => (
  <article className="rounded-[18px] border border-slate-200 bg-white p-4 shadow-sm">
    <p className="text-[15px] font-medium text-slate-600">{label}</p>
    <p className="mt-2 text-[28px] font-semibold leading-none text-slate-950">{value}</p>
    <p className="mt-2 text-[14px] leading-6 text-slate-500">{helper}</p>
  </article>
);

const SelectField = ({
  value,
  onChange,
  options,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  options: string[];
  placeholder: string;
}) => (
  <select value={value} onChange={(event) => onChange(event.target.value)} className="h-11 w-full rounded-2xl border border-slate-200 bg-white px-3 text-[15px] text-slate-700 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100">
    <option value="">{placeholder}</option>
    {options.map((option) => <option key={`${placeholder}-${option}`} value={option}>{option}</option>)}
  </select>
);

const reportIsComplete = (status?: string | null) => (status || '').toLowerCase().includes('complete');
const hasCareerGoal = (careerGoal?: string | null) => Boolean(careerGoal?.trim());
const readinessTone = (status: string, needsIntervention: boolean, profileComplete = true) => {
  if (needsIntervention) return 'critical';
  if (status.toLowerCase().includes('support')) return 'warning';
  return profileComplete ? 'positive' : 'warning';
};
const alertSeverityForLearner = (learner: {
  apsPoints: number;
  needsIntervention: boolean;
  reportUploadStatus?: string | null;
  careerGoal?: string | null;
  readinessStatus: string;
}) => {
  const reportPending = !reportIsComplete(learner.reportUploadStatus);
  const missingGoal = !hasCareerGoal(learner.careerGoal);
  if (learner.apsPoints <= 0 || (learner.needsIntervention && reportPending && missingGoal)) return 'High';
  if (learner.needsIntervention || reportPending || missingGoal || learner.readinessStatus.toLowerCase().includes('support')) return 'Medium';
  return 'Low';
};
const alertSeverityTone = (severity: string) => severity === 'High' ? 'critical' : severity === 'Medium' ? 'warning' : 'neutral';

export const SchoolAdminPortalPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { user, logout } = useAuth();
  const activeSection = sectionFromPath(location.pathname);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [gradeFilter, setGradeFilter] = useState('');
  const [classFilter, setClassFilter] = useState('');
  const [learnerModalOpen, setLearnerModalOpen] = useState(false);
  const [announcementOpen, setAnnouncementOpen] = useState(false);
  const [supportOpen, setSupportOpen] = useState(false);
  const [interventionOpen, setInterventionOpen] = useState(false);
  const [learnerAssignmentOpen, setLearnerAssignmentOpen] = useState(false);
  const [selectedLearnerId, setSelectedLearnerId] = useState<string | null>(null);
  const [selectedTeacherId, setSelectedTeacherId] = useState<string | null>(null);
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [learnerPage, setLearnerPage] = useState(1);
  const [learnerForm, setLearnerForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    username: '',
    password: '',
    grade: '',
    className: '',
    careerGoal: '',
    parentGuardianName: '',
    parentGuardianPhone: '',
    parentGuardianEmail: '',
    popiaConsentAccepted: true,
    consentVersion: 'v1.0',
  });
  const [announcementForm, setAnnouncementForm] = useState({ audience: 'ALL', title: '', message: '' });
  const [supportForm, setSupportForm] = useState({ category: 'Technical', title: '', message: '', priority: 'MEDIUM' });
  const [interventionForm, setInterventionForm] = useState({
    learnerUserId: '',
    supportType: 'Academic support',
    priority: 'HIGH',
    notes: '',
    followUpDate: '',
    status: 'OPEN',
  });
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const importInputRef = useRef<HTMLInputElement | null>(null);
  const [classEditOpen, setClassEditOpen] = useState(false);
  const [classAssignmentOpen, setClassAssignmentOpen] = useState(false);
  const [readinessFilter, setReadinessFilter] = useState('');
  const [careerGoalFilter, setCareerGoalFilter] = useState('');
  const [classForm, setClassForm] = useState({ id: '', grade: '', className: '', academicYear: new Date().getFullYear(), term: '', active: true });
  const [assignmentForm, setAssignmentForm] = useState({ teacherUserId: '', classId: '', subjectId: '' });
  const [learnerAssignmentForm, setLearnerAssignmentForm] = useState({ learnerUserId: '', classId: '', subjectId: '' });

  const dashboardQuery = useAppQuery({ queryKey: ['school-admin', 'dashboard'], queryFn: schoolService.schoolAdminCommandDashboard });
  const analyticsQuery = useAppQuery({ queryKey: ['school-admin', 'analytics'], queryFn: schoolService.schoolAdminAnalytics });
  const aiQuery = useAppQuery({ queryKey: ['school-admin', 'ai-insights'], queryFn: schoolService.schoolAdminAiInsights });
  const learnersQuery = useAppQuery({
    queryKey: ['school-admin', 'learners', search, gradeFilter, classFilter],
    queryFn: () => schoolService.schoolAdminLearners({ search, grade: gradeFilter, className: classFilter }),
  });
  const learnerJoinRequestsQuery = useAppQuery({
    queryKey: ['school-admin', 'learner-join-requests', 'PENDING'],
    queryFn: () => schoolService.schoolAdminLearnerJoinRequests('PENDING'),
  });
  const learnerProfileQuery = useAppQuery({
    queryKey: ['school-admin', 'learner-profile', selectedLearnerId],
    queryFn: () => schoolService.schoolAdminLearnerProfile(selectedLearnerId as string),
    enabled: Boolean(selectedLearnerId),
  });
  const teachersQuery = useAppQuery({ queryKey: ['school-admin', 'teachers'], queryFn: schoolService.schoolAdminTeachers });
  const teacherDetailQuery = useAppQuery({
    queryKey: ['school-admin', 'teacher-detail', selectedTeacherId],
    queryFn: () => schoolService.schoolAdminTeacherDetail(selectedTeacherId as string),
    enabled: Boolean(selectedTeacherId),
  });
  const teacherAnalyticsQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-analytics'], queryFn: schoolService.schoolAdminTeacherAnalytics });
  const teacherEngagementQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-engagement'], queryFn: schoolService.schoolAdminTeacherEngagement });
  const teacherAiInsightsQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-ai-insights'], queryFn: schoolService.schoolAdminTeacherAiInsights });
  const teacherWorkloadQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-workload'], queryFn: schoolService.schoolAdminTeacherWorkload });
  const teacherInterventionsQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-interventions'], queryFn: schoolService.schoolAdminTeacherInterventions });
  const teacherResourcesQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-resources'], queryFn: schoolService.schoolAdminTeacherResources });
  const teacherTrainingQuery = useAppQuery({ queryKey: ['school-admin', 'teacher-training'], queryFn: schoolService.schoolAdminTeacherTraining });
  const careerReadinessQuery = useAppQuery({ queryKey: ['school-admin', 'career-readiness'], queryFn: schoolService.schoolAdminCareerReadiness });
  const coursesQuery = useAppQuery({ queryKey: ['school-admin', 'courses'], queryFn: schoolService.schoolAdminCourses });
  const bursariesQuery = useAppQuery({ queryKey: ['school-admin', 'bursaries'], queryFn: schoolService.schoolAdminBursaries });
  const interventionsQuery = useAppQuery({ queryKey: ['school-admin', 'interventions'], queryFn: schoolService.schoolAdminInterventions });
  const reportsQuery = useAppQuery({ queryKey: ['school-admin', 'reports'], queryFn: schoolService.schoolAdminReports });
  const announcementsQuery = useAppQuery({ queryKey: ['school-admin', 'announcements'], queryFn: schoolService.schoolAnnouncements });
  const notificationsQuery = useAppQuery({ queryKey: ['school-admin', 'notifications'], queryFn: () => schoolService.schoolAdminNotifications({ page: 0, size: 20 }) });
  const supportQuery = useAppQuery({ queryKey: ['school-admin', 'support-requests'], queryFn: schoolService.schoolSupportRequests });
  const settingsQuery = useAppQuery({ queryKey: ['school-admin', 'settings'], queryFn: schoolService.schoolAdminSettings });
  const classesQuery = useAppQuery({ queryKey: ['school-admin', 'classes'], queryFn: schoolService.schoolAdminClasses });
  const classProfileQuery = useAppQuery({
    queryKey: ['school-admin', 'class-profile', selectedClassId],
    queryFn: () => schoolService.schoolAdminClassProfile(selectedClassId as string),
    enabled: Boolean(selectedClassId),
  });
  const classAnalyticsQuery = useAppQuery({
    queryKey: ['school-admin', 'class-analytics', selectedClassId],
    queryFn: () => schoolService.schoolAdminClassAnalytics(selectedClassId as string),
    enabled: Boolean(selectedClassId),
  });
  const classCareerQuery = useAppQuery({
    queryKey: ['school-admin', 'class-career', selectedClassId],
    queryFn: () => schoolService.schoolAdminClassCareerReadiness(selectedClassId as string),
    enabled: Boolean(selectedClassId),
  });
  const classBursaryQuery = useAppQuery({
    queryKey: ['school-admin', 'class-bursaries', selectedClassId],
    queryFn: () => schoolService.schoolAdminClassBursaries(selectedClassId as string),
    enabled: Boolean(selectedClassId),
  });
  const classAiQuery = useAppQuery({
    queryKey: ['school-admin', 'class-ai', selectedClassId],
    queryFn: () => schoolService.schoolAdminClassAiInsights(selectedClassId as string),
    enabled: Boolean(selectedClassId),
  });
  const subjectsQuery = useAppQuery({ queryKey: ['school-admin', 'subjects'], queryFn: schoolService.listSubjects });
  const assessmentsQuery = useAppQuery({ queryKey: ['school-admin', 'assessments'], queryFn: schoolService.schoolAssessments });
  const resultsQuery = useAppQuery({ queryKey: ['school-admin', 'results'], queryFn: schoolService.schoolResults });

  const refreshAdmin = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['school-admin'] }),
      queryClient.invalidateQueries({ queryKey: ['portal', 'school'] }),
    ]);
  };

  const createLearner = useMutation({
    mutationFn: () => schoolService.createSchoolAdminLearner(learnerForm),
    onSuccess: async () => {
      setLearnerModalOpen(false);
      setLearnerForm({
        firstName: '',
        lastName: '',
        email: '',
        username: '',
        password: '',
        grade: '',
        className: '',
        careerGoal: '',
        parentGuardianName: '',
        parentGuardianPhone: '',
        parentGuardianEmail: '',
        popiaConsentAccepted: true,
        consentVersion: 'v1.0',
      });
      await refreshAdmin();
    },
  });
  const importLearners = useMutation({
    mutationFn: () => schoolService.importSchoolAdminLearners(csvFile as File),
    onSuccess: refreshAdmin,
  });
  const createAnnouncement = useMutation({
    mutationFn: () => schoolService.createSchoolAnnouncement(announcementForm),
    onSuccess: async () => {
      setAnnouncementOpen(false);
      setAnnouncementForm({ audience: 'ALL', title: '', message: '' });
      await refreshAdmin();
    },
  });
  const createSupportRequest = useMutation({
    mutationFn: () => schoolService.createSchoolSupportRequest(supportForm),
    onSuccess: async () => {
      setSupportOpen(false);
      setSupportForm({ category: 'Technical', title: '', message: '', priority: 'MEDIUM' });
      await refreshAdmin();
    },
  });
  const createIntervention = useMutation({
    mutationFn: () => schoolService.createSchoolAdminIntervention(interventionForm),
    onSuccess: async () => {
      setInterventionOpen(false);
      setInterventionForm({ learnerUserId: '', supportType: 'Academic support', priority: 'HIGH', notes: '', followUpDate: '', status: 'OPEN' });
      await refreshAdmin();
    },
  });
  const exportCredentials = useMutation({
    mutationFn: schoolService.exportLearnerCredentials,
    onSuccess: (data) => downloadExport(data.fileName, data.contentType, data.base64Content),
  });
  const exportReport = useMutation({
    mutationFn: ({ type, format }: { type: string; format: 'pdf' | 'xlsx' }) => schoolService.exportSchoolAdminReport(type, format),
    onSuccess: (data) => downloadExport(data.fileName, data.contentType, data.base64Content),
  });
  const moderateTeacher = useMutation({
    mutationFn: ({ teacherId, action }: { teacherId: string; action: 'approve' | 'reject' | 'suspend' | 'reactivate' | 'delete' }) => {
      if (action === 'approve') return schoolService.approveTeacher(teacherId);
      if (action === 'reject') return schoolService.rejectTeacher(teacherId);
      if (action === 'suspend') return schoolService.suspendTeacherAdmin(teacherId);
      if (action === 'reactivate') return schoolService.reactivateTeacherAdmin(teacherId);
      return schoolService.deleteTeacherAdmin(teacherId);
    },
    onSuccess: refreshAdmin,
  });
  const moderateSchoolJoinRequest = useMutation({
    mutationFn: ({ requestId, action }: { requestId: string; action: 'approve' | 'reject' }) =>
      action === 'approve' ? schoolService.approveLearnerJoinRequest(requestId) : schoolService.rejectLearnerJoinRequest(requestId),
    onSuccess: refreshAdmin,
  });
  const updateClassMutation = useMutation({
    mutationFn: () => schoolService.updateClass(classForm.id, classForm),
    onSuccess: async () => {
      setClassEditOpen(false);
      await refreshAdmin();
    },
  });
  const assignTeacherMutation = useMutation({
    mutationFn: () => schoolService.assignTeacher(assignmentForm),
    onSuccess: async () => {
      setClassAssignmentOpen(false);
      setAssignmentForm({ teacherUserId: '', classId: '', subjectId: '' });
      await refreshAdmin();
    },
  });
  const assignLearnerToClassMutation = useMutation({
    mutationFn: () => schoolService.enrollLearner(learnerAssignmentForm),
    onSuccess: async () => {
      setLearnerAssignmentOpen(false);
      setLearnerAssignmentForm({ learnerUserId: '', classId: '', subjectId: '' });
      await refreshAdmin();
    },
  });

  const learnerItems = learnersQuery.data?.items ?? [];
  const gradeOptions = useMemo(() => [...new Set(learnerItems.map((item) => item.grade).filter(Boolean) as string[])].sort((left, right) => left.localeCompare(right, undefined, { numeric: true })), [learnerItems]);
  const classOptions = useMemo(() => {
    const fromClasses = (classesQuery.data?.items ?? []).map((item) => `${item.grade} ${item.className}`.trim());
    const fromLearners = learnerItems.map((item) => item.className).filter(Boolean) as string[];
    return [...new Set([...fromClasses, ...fromLearners])].sort((left, right) => left.localeCompare(right, undefined, { numeric: true }));
  }, [classesQuery.data?.items, learnerItems]);
  const readinessOptions = useMemo(() => [...new Set(learnerItems.map((item) => item.readinessStatus).filter(Boolean))].sort((left, right) => left.localeCompare(right)), [learnerItems]);
  const filteredLearners = useMemo(() => {
    const query = search.trim().toLowerCase();
    return learnerItems.filter((learner) => {
      const matchesSearch = !query || [learner.learnerName, learner.email, learner.username].some((value) => value.toLowerCase().includes(query));
      const matchesGrade = !gradeFilter || learner.grade === gradeFilter;
      const learnerClass = learner.className ? `${learner.grade || ''} ${learner.className}`.trim() : '';
      const matchesClass = !classFilter || learner.className === classFilter || learnerClass === classFilter;
      const matchesReadiness = !readinessFilter || learner.readinessStatus === readinessFilter;
      const matchesCareerGoal = !careerGoalFilter
        || (careerGoalFilter === 'set' && hasCareerGoal(learner.careerGoal))
        || (careerGoalFilter === 'missing' && !hasCareerGoal(learner.careerGoal));
      return matchesSearch && matchesGrade && matchesClass && matchesReadiness && matchesCareerGoal;
    });
  }, [careerGoalFilter, classFilter, gradeFilter, learnerItems, readinessFilter, search]);
  const learnerSummary = useMemo(() => ({
    total: filteredLearners.length,
    assigned: filteredLearners.filter((learner) => Boolean(learner.className)).length,
    pendingAssignment: filteredLearners.filter((learner) => !learner.className).length,
    interventionRequired: filteredLearners.filter((learner) => learner.needsIntervention).length,
    careerGoalMissing: filteredLearners.filter((learner) => !hasCareerGoal(learner.careerGoal)).length,
    reportPending: filteredLearners.filter((learner) => !reportIsComplete(learner.reportUploadStatus)).length,
  }), [filteredLearners]);
  const learnerAlerts = useMemo(() => (
    filteredLearners
      .filter((learner) => learner.needsIntervention || !hasCareerGoal(learner.careerGoal) || !reportIsComplete(learner.reportUploadStatus) || learner.apsPoints <= 0)
      .slice(0, 6)
      .map((learner) => {
        const reasons = [
          learner.apsPoints <= 0 ? 'APS 0' : learner.needsIntervention ? `APS ${learner.apsPoints}` : null,
          !hasCareerGoal(learner.careerGoal) ? 'Career goal pending' : null,
          !reportIsComplete(learner.reportUploadStatus) ? 'Report pending' : null,
          learner.readinessStatus.toLowerCase().includes('support') ? learner.readinessStatus : null,
        ].filter(Boolean) as string[];
        const severity = alertSeverityForLearner(learner);
        const alertType = severity === 'High'
          ? 'High Risk'
          : learner.needsIntervention
            ? 'Intervention Required'
            : !reportIsComplete(learner.reportUploadStatus)
              ? 'Report Pending'
              : 'Career Goal Missing';
        return {
          learner,
          severity,
          alertType,
          reason: (reasons.length ? reasons : [`APS ${learner.apsPoints}`]).slice(0, 3).join(' | '),
        };
      })
  ), [filteredLearners]);

  const pagedLearners = useMemo(() => {
    const items = filteredLearners;
    const pageSize = 10;
    const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
    const currentPage = Math.min(learnerPage, totalPages);
    const start = (currentPage - 1) * pageSize;
    return {
      totalPages,
      currentPage,
      items: items.slice(start, start + pageSize),
    };
  }, [filteredLearners, learnerPage]);

  const commandErrors = [
    dashboardQuery.error,
    analyticsQuery.error,
    aiQuery.error,
    learnersQuery.error,
    learnerProfileQuery.error,
    teachersQuery.error,
    teacherDetailQuery.error,
    classesQuery.error,
    classProfileQuery.error,
    classAnalyticsQuery.error,
    classCareerQuery.error,
    classBursaryQuery.error,
    classAiQuery.error,
    teacherAnalyticsQuery.error,
    teacherEngagementQuery.error,
    teacherAiInsightsQuery.error,
    teacherWorkloadQuery.error,
    teacherInterventionsQuery.error,
    teacherResourcesQuery.error,
    teacherTrainingQuery.error,
    careerReadinessQuery.error,
    coursesQuery.error,
    bursariesQuery.error,
    interventionsQuery.error,
    reportsQuery.error,
    announcementsQuery.error,
    notificationsQuery.error,
    supportQuery.error,
    settingsQuery.error,
    createLearner.error,
    importLearners.error,
    createAnnouncement.error,
    createSupportRequest.error,
    createIntervention.error,
    moderateTeacher.error,
    updateClassMutation.error,
    assignTeacherMutation.error,
    assignLearnerToClassMutation.error,
    exportReport.error,
    exportCredentials.error,
  ].filter(Boolean) as Error[];

  const primaryLoading = dashboardQuery.isLoading || analyticsQuery.isLoading || aiQuery.isLoading;

  const handleNavigate = (section: CommandSection) => {
    setMobileNavOpen(false);
    navigate(sectionPath(section));
  };

  const openLearnerProfile = (learnerUserId: string) => {
    setSelectedLearnerId(learnerUserId);
    if (interventionForm.learnerUserId !== learnerUserId) {
      setInterventionForm((state) => ({ ...state, learnerUserId }));
    }
  };

  const openLearnerAssignment = (learnerUserId: string) => {
    setLearnerAssignmentForm((state) => ({ ...state, learnerUserId }));
    setLearnerAssignmentOpen(true);
  };

  const openClassProfile = (classId: string) => setSelectedClassId(classId);

  const renderClasses = () => (
    <SectionShell title="Classes" subtitle="Class teacher coverage, learner totals, subject allocation, readiness posture, and class-level actions.">
      {!classesQuery.data ? <LoadingGrid /> : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4 2xl:grid-cols-6">
            {classesQuery.data.metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}
          </div>
          <div className="grid gap-4 xl:grid-cols-4">
            <InsightList title="Top Performing Classes" items={classesQuery.data.topPerformingClasses} emptyMessage="No class rankings yet." />
            <InsightList title="Most Improved Classes" items={classesQuery.data.mostImprovedClasses} emptyMessage="No improvement signals yet." />
            <InsightList title="At-Risk Classes" items={classesQuery.data.atRiskClasses} emptyMessage="No at-risk classes flagged." />
            <InsightList title="Highest Career Readiness" items={classesQuery.data.highestCareerReadinessClasses} emptyMessage="No class readiness leaders yet." />
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {classesQuery.data.items.map((item) => (
              <article key={item.classId} className="rounded-[16px] border border-[#e5edf7] bg-white p-4 shadow-sm shadow-slate-200/40">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-[13px] font-semibold text-slate-500">{item.grade}</p>
                    <h3 className="mt-1 text-[18px] font-semibold text-slate-950">{item.className}</h3>
                    <p className="mt-1 text-[13px] text-slate-500">Academic year {item.academicYear} | {item.classTeacher}</p>
                  </div>
                  <QuickChip label={item.active ? 'Active' : 'Inactive'} tone={item.active ? 'positive' : 'warning'} />
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-[13px] text-slate-600">
                  <div><p className="text-[12px] text-slate-500">Learners</p><p className="mt-1 font-semibold text-slate-950">{item.learnerCount}</p></div>
                  <div><p className="text-[12px] text-slate-500">Subjects</p><p className="mt-1 font-semibold text-slate-950">{item.subjectCount}</p></div>
                  <div><p className="text-[12px] text-slate-500">Average APS</p><p className="mt-1 font-semibold text-slate-950">{item.averageAps}</p></div>
                  <div><p className="text-[12px] text-slate-500">Interventions</p><p className="mt-1 font-semibold text-slate-950">{item.interventionCount}</p></div>
                </div>
                <div className="mt-4 space-y-2">
                  <div className="flex items-center justify-between text-xs text-slate-500"><span>Career readiness</span><span>{item.careerReadinessPercent}%</span></div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-blue-600" style={{ width: `${Math.max(6, Math.min(100, item.careerReadinessPercent))}%` }} /></div>
                  <div className="flex items-center justify-between text-xs text-slate-500"><span>Bursary readiness</span><span>{item.bursaryReadinessPercent}%</span></div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${Math.max(6, Math.min(100, item.bursaryReadinessPercent))}%` }} /></div>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <QuickChip label={`Attendance ${item.attendanceRate}%`} tone={item.attendanceRate >= 70 ? 'positive' : 'warning'} />
                  <QuickChip label={`Assignments ${item.assignmentCompletionRate}%`} tone={item.assignmentCompletionRate >= 70 ? 'positive' : 'warning'} />
                  <QuickChip label={`Reports ${item.reportUploadCompletion}%`} tone={item.reportUploadCompletion >= 60 ? 'positive' : 'warning'} />
                </div>
                <div className="mt-5 flex flex-wrap gap-2">
                  <AdminActionButton variant="secondary" onClick={() => openClassProfile(item.classId)}>View</AdminActionButton>
                  <AdminActionButton variant="ghost" onClick={() => { setClassForm({ id: item.classId, grade: item.grade, className: item.className, academicYear: item.academicYear, term: item.term || '', active: item.active }); setClassEditOpen(true); }}>Edit</AdminActionButton>
                  <AdminActionButton variant="primary" onClick={() => { setAssignmentForm((state) => ({ ...state, classId: item.classId })); setClassAssignmentOpen(true); }}>Assign Teacher</AdminActionButton>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
    </SectionShell>
  );

  const renderOverview = () => (
    <SectionShell
      title="Dashboard"
      subtitle="School health, learner progress, approvals, reports, and intervention visibility from one school admin workspace."
      actions={
        <>
          <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => setLearnerModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Learner
          </Button>
          <Button type="button" className="h-11 rounded-2xl bg-white text-slate-900 border border-slate-200 hover:bg-slate-50" onClick={() => navigate('/school/reports')}>
            <FileSpreadsheet className="mr-2 h-4 w-4" />
            Reports
          </Button>
          <Button type="button" className="h-11 rounded-2xl bg-slate-900 hover:bg-slate-800" onClick={() => navigate('/school/career-readiness')}>
            <Sparkles className="mr-2 h-4 w-4" />
            Open Readiness
          </Button>
        </>
      }
    >
      {primaryLoading || !dashboardQuery.data ? <LoadingGrid /> : (
        <DashboardShell>
          <DashboardSectionCard title="School Snapshot" subtitle={dashboardQuery.data.summaryHeadline}>
            <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
              <DashboardKpiCard label="Learners" value={String(learnersQuery.data?.total ?? learnerSummary.total)} helperText="Learners linked to this school." tone="positive" actionLabel="View" />
              <DashboardKpiCard label="Teachers" value={String(teachersQuery.data?.items?.length ?? 0)} helperText="Teachers available in the portal." tone="info" actionLabel="View" />
              <DashboardKpiCard label="Classes" value={String(classesQuery.data?.items?.length ?? 0)} helperText="Active classes and timetables." tone="info" actionLabel="Open" />
              <DashboardKpiCard label="Attendance" value={`${Math.round((dashboardQuery.data.reportUploadProgress[0]?.value ?? 0))}%`} helperText="Latest reporting completion signal." tone="neutral" actionLabel="Review" />
              <DashboardKpiCard label="ATP Compliance" value={`${Math.round((dashboardQuery.data.subjectPerformance[0]?.value ?? 0))}%`} helperText="Current curriculum execution signal." tone="warning" actionLabel="Track" />
              <DashboardKpiCard label="Open Alerts" value={String((learnerJoinRequestsQuery.data?.length ?? 0) + (interventionsQuery.data?.items?.length ?? 0))} helperText="Join requests and active interventions." tone={(learnerJoinRequestsQuery.data?.length ?? 0) || (interventionsQuery.data?.items?.length ?? 0) ? 'warning' : 'positive'} actionLabel="Respond" />
            </div>
          </DashboardSectionCard>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr_1fr]">
            <div className="space-y-4 xl:col-span-2">
              <div className="grid gap-4 xl:grid-cols-3">
                <ChartPanel title="School Performance" points={dashboardQuery.data.schoolPerformanceTrends} />
                <ChartPanel title="Teacher Activity" points={dashboardQuery.data.subjectPerformance} />
                <ChartPanel title="Curriculum Resources" points={dashboardQuery.data.reportUploadProgress} />
              </div>
              <div className="grid gap-4 xl:grid-cols-[1.4fr_1fr]">
                <InsightList title="Learner Risk Table" items={dashboardQuery.data.topSubjectRiskAreas} emptyMessage="No subject risk areas detected yet." />
                <InsightList title="Recent Reports" items={dashboardQuery.data.districtReportingSummary} emptyMessage="District-ready summary will appear once school data is available." />
              </div>
            </div>
            <div className="space-y-4">
              <DashboardSectionCard title="Announcements" subtitle="Latest school and district messaging.">
                <div className="space-y-3">
                  {dashboardQuery.data.teacherActivitySummary.slice(0, 3).map((item) => (
                    <div key={`dash-announce-${item.title}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-[12px] text-slate-500">{item.detail}</p>
                    </div>
                  ))}
                  {!dashboardQuery.data.teacherActivitySummary.length ? <EmptyStateCompact title="Announcements" message="No school announcements are active." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Support Requests" subtitle={(learnerJoinRequestsQuery.data?.length ?? 0) ? 'Learners are waiting for school approval.' : 'No learner join requests are waiting for approval.'}>
                <div className="space-y-3">
                  <RiskBadge label={`${learnerJoinRequestsQuery.data?.length ?? 0} pending`} />
                  <AdminActionButton className="w-full justify-center" variant="primary" onClick={() => navigate('/school/my-school-requests')} disabled={learnerJoinRequestsQuery.isLoading || learnerJoinRequestsQuery.isError}>Review Requests</AdminActionButton>
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Quick Actions" subtitle="Most-used school admin actions.">
                <div className="grid gap-3">
                  <QuickActionButton label="Add Learner" helperText="Register a learner and capture consent." icon={Plus} onClick={() => setLearnerModalOpen(true)} />
                  <QuickActionButton label="Open Reports" helperText="Export readiness and academic reports." icon={FileSpreadsheet} onClick={() => navigate('/school/reports')} />
                  <QuickActionButton label="Create Support Request" helperText="Escalate training or intervention needs." icon={LifeBuoy} onClick={() => navigate('/school/support-requests')} />
                </div>
              </DashboardSectionCard>
            </div>
          </div>
        </DashboardShell>
      )}
    </SectionShell>
  );

  const renderAnalytics = () => (
    <SectionShell title="Academic Intelligence" subtitle="Grade, class, subject, APS, and learner-improvement intelligence using live school data.">
      {!analyticsQuery.data ? <LoadingGrid /> : (
        <>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Grade Performance" points={analyticsQuery.data.schoolPerformanceTrends} />
            <ChartPanel title="Subject Performance" points={analyticsQuery.data.subjectPerformance} />
          </div>
          <div className="grid gap-4 xl:grid-cols-3">
            <ChartPanel title="APS by Band" points={analyticsQuery.data.apsBandDistribution} />
            <ChartPanel title="Class Comparison" points={analyticsQuery.data.gradePerformanceComparison} />
            <InsightList title="District Summary" items={analyticsQuery.data.districtReadyReportingSummary} emptyMessage="No district summary yet." />
          </div>
          <div className="grid gap-4 xl:grid-cols-2">
            <InsightList title="Learner Improvement Recommendations" items={analyticsQuery.data.learnerImprovementRecommendations} emptyMessage="No learner improvement actions flagged." />
            <InsightList title="Career Readiness Overview" items={analyticsQuery.data.careerReadinessOverview} emptyMessage="Career readiness data is still building." />
          </div>
        </>
      )}
    </SectionShell>
  );

  const renderAiInsights = () => (
    <SectionShell title="AI Insights" subtitle="Learner risk, APS interpretation, pathway gaps, bursary indicators, and recommended interventions.">
      {!aiQuery.data ? <LoadingGrid /> : !aiQuery.data.dataAvailable ? (
        <div className="rounded-[32px] border border-dashed border-blue-200 bg-white px-8 py-16 text-center shadow-sm">
          <Bot className="mx-auto h-12 w-12 text-blue-600" />
          <h3 className="mt-4 text-xl font-semibold text-slate-950">AI Insights Unavailable</h3>
          <p className="mx-auto mt-3 max-w-2xl text-sm text-slate-600">{aiQuery.data.emptyStateMessage || 'AI insights will appear once learner reports and academic data are available.'}</p>
        </div>
      ) : (
        <div className="grid gap-4 xl:grid-cols-2">
          <InsightList title="Learners at Risk" items={aiQuery.data.learnersAtRisk} emptyMessage="No learners at risk." />
          <InsightList title="Subject Weakness Trends" items={aiQuery.data.subjectWeaknessTrends} emptyMessage="No subject weakness trends found." />
          <InsightList title="APS Readiness Warnings" items={aiQuery.data.apsReadinessWarnings} emptyMessage="No APS warnings found." />
          <InsightList title="Career Pathway Gaps" items={aiQuery.data.careerPathwayGaps} emptyMessage="No pathway gaps found." />
          <InsightList title="Bursary Need Indicators" items={aiQuery.data.bursaryNeedIndicators} emptyMessage="No bursary indicators found." />
          <InsightList title="Teacher Activity Alerts" items={aiQuery.data.teacherActivityAlerts} emptyMessage="No teacher alerts found." />
          <div className="xl:col-span-2">
            <InsightList title="Recommended Interventions" items={aiQuery.data.recommendedInterventions} emptyMessage="No interventions recommended yet." />
          </div>
        </div>
      )}
    </SectionShell>
  );

  const renderLearners = () => (
    <SectionShell
      title="Learner Management"
      subtitle="Manage learner records, school usernames, readiness, APS, reports, and intervention actions."
      actions={
        <div className="flex flex-wrap gap-2">
          <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => setLearnerModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Learner
          </Button>
          <Button type="button" className="h-11 rounded-2xl bg-emerald-600 hover:bg-emerald-700" onClick={() => importInputRef.current?.click()}>
            <FileSpreadsheet className="mr-2 h-4 w-4" />
            Import Learners
          </Button>
          <Button type="button" className="h-11 rounded-2xl bg-slate-900 hover:bg-slate-800" onClick={() => exportCredentials.mutate()} disabled={exportCredentials.isPending}>
            {exportCredentials.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <FileSpreadsheet className="mr-2 h-4 w-4" />}
            Export
          </Button>
        </div>
      }
    >
      <input ref={importInputRef} type="file" accept=".csv" onChange={(event) => setCsvFile(event.target.files?.[0] ?? null)} className="hidden" />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <CompactStatCard label="Total Learners" value={String(learnerSummary.total)} helper="Learners currently visible in this workspace." />
        <CompactStatCard label="Assigned to Classes" value={String(learnerSummary.assigned)} helper="Learners already linked to a class." />
        <CompactStatCard label="Pending Class Assignment" value={String(learnerSummary.pendingAssignment)} helper="Learners still waiting for class placement." />
        <CompactStatCard label="Intervention Required" value={String(learnerSummary.interventionRequired)} helper="Learners with active support flags." />
        <CompactStatCard label="Career Goal Missing" value={String(learnerSummary.careerGoalMissing)} helper="Learners without a selected pathway." />
        <CompactStatCard label="Report Pending" value={String(learnerSummary.reportPending)} helper="Learners still missing a completed report." />
      </div>
      <div className="grid min-w-0 max-w-full gap-4 xl:grid-cols-[minmax(0,7fr)_minmax(19rem,3fr)]">
        <div className="min-w-0 space-y-4">
          <section className="rounded-[18px] border border-slate-200 bg-white p-4 shadow-sm">
            <div className="grid gap-3 lg:grid-cols-2 2xl:grid-cols-[minmax(0,1.4fr)_repeat(4,minmax(0,1fr))_auto]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input value={search} onChange={(event) => { setSearch(event.target.value); setLearnerPage(1); }} placeholder="Search by name, email, or username" className="h-11 rounded-2xl pl-10 text-[15px]" />
              </div>
              <SelectField value={gradeFilter} onChange={(value) => { setGradeFilter(value); setLearnerPage(1); }} options={gradeOptions} placeholder="All grades" />
              <SelectField value={classFilter} onChange={(value) => { setClassFilter(value); setLearnerPage(1); }} options={classOptions} placeholder="All classes" />
              <SelectField value={readinessFilter} onChange={(value) => { setReadinessFilter(value); setLearnerPage(1); }} options={readinessOptions} placeholder="All readiness" />
              <select value={careerGoalFilter} onChange={(event) => { setCareerGoalFilter(event.target.value); setLearnerPage(1); }} className="h-11 rounded-2xl border border-slate-200 bg-white px-3 text-[15px] text-slate-700 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100">
                <option value="">All career goals</option>
                <option value="set">Career goal set</option>
                <option value="missing">Career goal missing</option>
              </select>
              <Button
                type="button"
                className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200"
                onClick={() => {
                  setSearch('');
                  setGradeFilter('');
                  setClassFilter('');
                  setReadinessFilter('');
                  setCareerGoalFilter('');
                  setLearnerPage(1);
                }}
              >
                Clear Filters
              </Button>
            </div>
          </section>
          <TableShell title={`Learners (${filteredLearners.length})`} action={<span className="text-[14px] text-slate-500">Page {pagedLearners.currentPage} of {pagedLearners.totalPages}</span>}>
            <div className="max-w-full">
              <table className="hidden w-full table-fixed divide-y divide-slate-100 text-[14px] lg:table xl:text-[15px]">
                <colgroup>
                  <col className="w-[28%]" />
                  <col className="w-[16%]" />
                  <col className="w-[8%]" />
                  <col className="w-[16%]" />
                  <col className="w-[12%]" />
                  <col className="w-[10%]" />
                  <col className="w-[20%]" />
                </colgroup>
                <thead className="bg-slate-50 text-left text-[13px] font-semibold text-slate-600">
                  <tr>
                    <th className="px-4 py-3.5">Learner</th>
                    <th className="px-3 py-3.5">Grade / Class</th>
                    <th className="px-3 py-3.5">APS</th>
                    <th className="px-4 py-3.5">Career Goal</th>
                    <th className="px-3 py-3.5">Readiness</th>
                    <th className="px-3 py-3.5">Report</th>
                    <th className="px-4 py-3.5">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pagedLearners.items.length ? pagedLearners.items.map((learner) => (
                    <tr key={learner.learnerUserId} className="align-top">
                      <td className="px-4 py-4">
                        <p className="text-[15px] font-semibold text-slate-950">{learner.learnerName}</p>
                        <p className="mt-1 break-words text-[14px] text-slate-600">{learner.email.endsWith('@school.local') ? 'No email address' : learner.email}</p>
                        <p className="mt-1 text-[14px] text-slate-500">{learner.username}</p>
                      </td>
                      <td className="px-3 py-4 text-slate-600">
                        <p className="text-[14px] font-medium text-slate-900">{learner.grade || 'Unassigned'}</p>
                        <p className="mt-1 text-[14px] text-slate-500">{learner.className || 'Class pending'}</p>
                      </td>
                      <td className="px-3 py-4 text-[15px] font-semibold text-slate-900">{learner.apsPoints}</td>
                      <td className="px-4 py-4 text-slate-600">
                        <p className="break-words text-[14px]">{learner.careerGoal || 'Not selected'}</p>
                      </td>
                      <td className="px-3 py-4"><QuickChip label={learner.readinessStatus} tone={readinessTone(learner.readinessStatus, learner.needsIntervention, learner.profileComplete)} /></td>
                      <td className="px-3 py-4"><QuickChip label={learner.reportUploadStatus || 'Pending'} tone={reportIsComplete(learner.reportUploadStatus) ? 'positive' : 'warning'} /></td>
                      <td className="px-4 py-4">
                        <div className="flex flex-wrap gap-2">
                          <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-[13px] hover:bg-slate-800" onClick={() => openLearnerProfile(learner.learnerUserId)}>View Profile</Button>
                          <Button type="button" className="h-9 rounded-xl bg-blue-600 px-3 text-[13px] hover:bg-blue-700" onClick={() => openLearnerAssignment(learner.learnerUserId)}>Assign Class</Button>
                          <Button type="button" className="h-9 rounded-xl bg-slate-100 px-3 text-[13px] text-slate-900 hover:bg-slate-200" onClick={() => navigate('/school/results')}>Results</Button>
                          <Button type="button" className="h-9 rounded-xl bg-amber-500 px-3 text-[13px] hover:bg-amber-600" onClick={() => openLearnerProfile(learner.learnerUserId)}>Readiness</Button>
                        </div>
                      </td>
                    </tr>
                  )) : (
                    <tr>
                      <td colSpan={7} className="px-5 py-12">
                        <div className="rounded-[18px] border border-dashed border-slate-200 px-4 py-8 text-center">
                          <p className="text-[16px] font-medium text-slate-900">No learners registered yet.</p>
                          <p className="mt-2 text-[15px] text-slate-500">Add learners manually or import a CSV file to begin.</p>
                        </div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
              <div className="space-y-3 p-4 lg:hidden">
                {pagedLearners.items.length ? pagedLearners.items.map((learner) => (
                  <article key={learner.learnerUserId} className="rounded-[18px] border border-slate-200 bg-white p-4 shadow-sm">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="text-[16px] font-semibold text-slate-950">{learner.learnerName}</p>
                        <p className="mt-1 break-words text-[14px] text-slate-600">{learner.email.endsWith('@school.local') ? 'No email address' : learner.email}</p>
                        <p className="mt-1 text-[14px] text-slate-500">{learner.username}</p>
                      </div>
                      <QuickChip label={`APS ${learner.apsPoints}`} tone="neutral" />
                    </div>
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      <div>
                        <p className="text-[14px] font-medium text-slate-500">Grade / Class</p>
                        <p className="mt-1 text-[15px] text-slate-700">{learner.grade || 'Unassigned'}{learner.className ? ` · ${learner.className}` : ''}</p>
                      </div>
                      <div>
                        <p className="text-[14px] font-medium text-slate-500">Career Goal</p>
                        <p className="mt-1 text-[15px] text-slate-700">{learner.careerGoal || 'Not selected'}</p>
                      </div>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                      <QuickChip label={learner.readinessStatus} tone={readinessTone(learner.readinessStatus, learner.needsIntervention, learner.profileComplete)} />
                      <QuickChip label={learner.reportUploadStatus || 'Pending'} tone={reportIsComplete(learner.reportUploadStatus) ? 'positive' : 'warning'} />
                      <QuickChip label={learner.needsIntervention ? 'Intervention required' : 'Clear'} tone={learner.needsIntervention ? 'critical' : 'positive'} />
                    </div>
                    <div className="mt-4 grid gap-2 sm:grid-cols-2">
                      <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2">
                        <p className="text-[14px] font-medium text-slate-500">Readiness</p>
                        <p className="mt-1 text-[15px] font-semibold text-slate-900">{learner.readinessStatus}</p>
                      </div>
                      <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2">
                        <p className="text-[14px] font-medium text-slate-500">Report</p>
                        <p className="mt-1 text-[15px] font-semibold text-slate-900">{learner.reportUploadStatus || 'Pending'}</p>
                      </div>
                    </div>
                    <div className="mt-4 grid gap-2 sm:grid-cols-2">
                      <Button type="button" className="h-10 w-full rounded-xl bg-slate-900 px-3 text-[13px] hover:bg-slate-800" onClick={() => openLearnerProfile(learner.learnerUserId)}>View Profile</Button>
                      <Button type="button" className="h-10 w-full rounded-xl bg-blue-600 px-3 text-[13px] hover:bg-blue-700" onClick={() => openLearnerAssignment(learner.learnerUserId)}>Assign Class</Button>
                      <Button type="button" className="h-10 w-full rounded-xl bg-slate-100 px-3 text-[13px] text-slate-900 hover:bg-slate-200" onClick={() => navigate('/school/results')}>Results</Button>
                      <Button type="button" className="h-10 w-full rounded-xl bg-amber-500 px-3 text-[13px] hover:bg-amber-600" onClick={() => openLearnerProfile(learner.learnerUserId)}>Readiness</Button>
                    </div>
                  </article>
                )) : <div className="rounded-[18px] border border-dashed border-slate-200 px-4 py-8 text-center text-[15px] text-slate-500"><p className="text-[16px] font-medium text-slate-900">No learners registered yet.</p><p className="mt-2">Add learners manually or import a CSV file to begin.</p></div>}
              </div>
            </div>
            <div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-[14px] text-slate-500">Search, filter, and open a learner profile for readiness intelligence.</p>
              <div className="flex gap-2 self-start sm:self-auto">
                <Button type="button" className="h-9 rounded-xl bg-slate-100 px-3 text-[13px] text-slate-900 hover:bg-slate-200" disabled={pagedLearners.currentPage <= 1} onClick={() => setLearnerPage((page) => Math.max(1, page - 1))}>Previous</Button>
                <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-[13px] hover:bg-slate-800" disabled={pagedLearners.currentPage >= pagedLearners.totalPages} onClick={() => setLearnerPage((page) => Math.min(pagedLearners.totalPages, page + 1))}>Next</Button>
              </div>
            </div>
          </TableShell>
        </div>
        <div className="min-w-0 space-y-4 self-start xl:max-w-full">
          <section className="min-w-0 rounded-[18px] border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-[18px] font-semibold text-slate-950">Import Learners</h3>
            <p className="mt-2 text-[15px] leading-6 text-slate-600">Bulk import learners, including records without email addresses. EduRite will generate usernames and passwords where needed.</p>
            <input type="file" accept=".csv" onChange={(event) => setCsvFile(event.target.files?.[0] ?? null)} className="mt-4 block w-full text-[15px] text-slate-600" />
            <Button type="button" className="mt-4 h-11 w-full rounded-2xl bg-slate-900 hover:bg-slate-800" disabled={!csvFile || importLearners.isPending} onClick={() => importLearners.mutate()}>
              {importLearners.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
              Import Learners
            </Button>
          </section>
          <section className="rounded-[18px] border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-[18px] font-semibold text-slate-950">AI Learner Alerts</h3>
              <QuickChip label={String(learnerAlerts.length)} tone={learnerAlerts.length ? 'warning' : 'positive'} />
            </div>
            <div className="mt-4 space-y-3">
              {learnerAlerts.length ? learnerAlerts.map(({ learner, severity, alertType, reason }) => (
                <article key={`alert-${learner.learnerUserId}`} className="rounded-[16px] border border-slate-200 bg-slate-50/80 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-[15px] font-semibold text-slate-950">{learner.learnerName}</p>
                      <p className="mt-1 text-[14px] font-medium text-slate-700">{alertType}</p>
                    </div>
                    <QuickChip label={severity} tone={alertSeverityTone(severity)} />
                  </div>
                  <p className="mt-2 text-[14px] leading-6 text-slate-600">{reason}</p>
                  <Button type="button" className="mt-3 h-9 rounded-xl bg-slate-900 px-3 text-[13px] hover:bg-slate-800" onClick={() => openLearnerProfile(learner.learnerUserId)}>
                    View
                  </Button>
                </article>
              )) : (
                <div className="rounded-[16px] border border-dashed border-slate-200 px-4 py-8 text-center">
                  <p className="text-[15px] text-slate-500">No learner readiness alerts at the moment.</p>
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </SectionShell>
  );

  const renderMySchoolRequests = () => (
    <SectionShell title="Learner Join Requests" subtitle="Review learner requests coming from the main EduRite student dashboard and approve them into this school portal.">
      {learnerJoinRequestsQuery.isError ? <ErrorState message="Could not load learner join requests." /> : null}
      <TableShell title={`Requests (${learnerJoinRequestsQuery.data?.length ?? 0})`} action={learnerJoinRequestsQuery.isLoading ? <span className="text-xs text-slate-500">Loading pending requests...</span> : undefined}>
        <div className="hidden min-w-0 overflow-x-auto lg:block">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
              <tr>
                <th className="px-5 py-3">Learner</th>
                <th className="px-5 py-3">Email / Phone</th>
                <th className="px-5 py-3">Requested School</th>
                <th className="px-5 py-3">Requested</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Generated Username</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(learnerJoinRequestsQuery.data ?? []).map((request) => (
                <tr key={request.requestId}>
                  <td className="px-5 py-4">
                    <p className="font-semibold text-slate-950">{request.learnerFullName}</p>
                  </td>
                  <td className="px-5 py-4 text-slate-600">
                    <p>{request.learnerEmail || 'No email available'}</p>
                    <p className="mt-1 text-xs text-slate-500">{request.learnerPhone || 'No phone available'}</p>
                  </td>
                  <td className="px-5 py-4 text-slate-600">{request.schoolName}</td>
                  <td className="px-5 py-4 text-slate-600">{formatDate(request.requestedAt)}</td>
                  <td className="px-5 py-4">
                    <QuickChip label={request.status} tone={request.status === 'APPROVED' ? 'positive' : request.status === 'REJECTED' ? 'critical' : 'warning'} />
                  </td>
                  <td className="px-5 py-4 text-slate-600">{request.generatedUsername || 'Pending approval'}</td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-2">
                      <Button
                        type="button"
                        className="h-9 rounded-xl bg-emerald-600 px-3 text-xs hover:bg-emerald-700"
                        disabled={moderateSchoolJoinRequest.isPending}
                        onClick={() => moderateSchoolJoinRequest.mutate({ requestId: request.requestId, action: 'approve' })}
                      >
                        Approve
                      </Button>
                      <Button
                        type="button"
                        className="h-9 rounded-xl bg-rose-600 px-3 text-xs hover:bg-rose-700"
                        disabled={moderateSchoolJoinRequest.isPending}
                        onClick={() => moderateSchoolJoinRequest.mutate({ requestId: request.requestId, action: 'reject' })}
                      >
                        Reject
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {learnerJoinRequestsQuery.isLoading ? (
                <tr>
                  <td colSpan={7} className="px-5 py-10 text-center text-sm text-slate-500">Loading pending learner join requests...</td>
                </tr>
              ) : null}
              {!learnerJoinRequestsQuery.isLoading && !learnerJoinRequestsQuery.data?.length ? (
                <tr>
                  <td colSpan={7} className="px-5 py-10 text-center text-sm text-slate-500">No learner join requests are waiting for approval.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
        <div className="space-y-3 p-4 lg:hidden">
          {(learnerJoinRequestsQuery.data ?? []).map((request) => (
            <article key={request.requestId} className="rounded-3xl border border-slate-200 bg-slate-50/80 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-950">{request.learnerFullName}</p>
                  <p className="text-sm text-slate-600">{request.learnerEmail || 'No email available'}</p>
                  <p className="text-xs text-slate-500">{request.learnerPhone || 'No phone available'}</p>
                </div>
                <QuickChip label={request.status} tone={request.status === 'APPROVED' ? 'positive' : request.status === 'REJECTED' ? 'critical' : 'warning'} />
              </div>
              <div className="mt-3 space-y-1 text-sm text-slate-600">
                <p><span className="font-medium text-slate-900">Requested school:</span> {request.schoolName}</p>
                <p><span className="font-medium text-slate-900">Requested:</span> {formatDate(request.requestedAt)}</p>
                <p><span className="font-medium text-slate-900">Generated username:</span> {request.generatedUsername || 'Pending approval'}</p>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button
                  type="button"
                  className="h-9 rounded-xl bg-emerald-600 px-3 text-xs hover:bg-emerald-700"
                  disabled={moderateSchoolJoinRequest.isPending}
                  onClick={() => moderateSchoolJoinRequest.mutate({ requestId: request.requestId, action: 'approve' })}
                >
                  Approve
                </Button>
                <Button
                  type="button"
                  className="h-9 rounded-xl bg-rose-600 px-3 text-xs hover:bg-rose-700"
                  disabled={moderateSchoolJoinRequest.isPending}
                  onClick={() => moderateSchoolJoinRequest.mutate({ requestId: request.requestId, action: 'reject' })}
                >
                  Reject
                </Button>
              </div>
            </article>
          ))}
          {learnerJoinRequestsQuery.isLoading ? <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-8 text-sm text-slate-500">Loading pending learner join requests...</div> : null}
          {!learnerJoinRequestsQuery.isLoading && !learnerJoinRequestsQuery.data?.length ? <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-8 text-sm text-slate-500">No learner join requests are waiting for approval.</div> : null}
        </div>
      </TableShell>
    </SectionShell>
  );

  const renderCareerReadiness = () => (
    <SectionShell title="Career Readiness" subtitle="Track selected pathways, alignment to requirements, readiness gaps, and alternative pathway recommendations.">
      {!careerReadinessQuery.data ? <LoadingGrid /> : (
        <>
          <div className="rounded-[30px] border border-blue-100 bg-gradient-to-r from-blue-50 via-white to-emerald-50 p-5 shadow-sm">
            <p className="text-sm font-semibold text-slate-950">{careerReadinessQuery.data.headline}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {careerReadinessQuery.data.metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}
          </div>
          <div className="grid gap-4 xl:grid-cols-2">
            <InsightList title="Top Career Interests" items={careerReadinessQuery.data.topCareerInterests} emptyMessage="No career interests yet." />
            <InsightList title="Common Readiness Gaps" items={careerReadinessQuery.data.commonReadinessGaps} emptyMessage="No readiness gaps detected." />
            <InsightList title="Alignment Warnings" items={careerReadinessQuery.data.alignmentWarnings} emptyMessage="No alignment warnings." />
            <InsightList title="Alternative Pathways" items={careerReadinessQuery.data.alternativePathwayRecommendations} emptyMessage="No alternative pathways suggested." />
          </div>
        </>
      )}
    </SectionShell>
  );

  const renderCourses = () => (
    <SectionShell title="Courses & Study Pathways" subtitle="University, TVET, and alternative-pathway intelligence based on current learner readiness data.">
      {!coursesQuery.data ? <LoadingGrid /> : (
        <>
          <div className="rounded-[30px] border border-blue-100 bg-gradient-to-r from-blue-50 via-white to-emerald-50 p-5 shadow-sm">
            <p className="text-sm font-semibold text-slate-950">{coursesQuery.data.headline}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {coursesQuery.data.metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}
          </div>
          <div className="grid gap-4 xl:grid-cols-3">
            <InsightList title="Institution Options" items={coursesQuery.data.institutionOptions} emptyMessage="No institution options yet." />
            <InsightList title="Most Matched Courses" items={coursesQuery.data.mostMatchedCourses} emptyMessage="No course matches yet." />
            <InsightList title="Qualification Gaps" items={coursesQuery.data.qualificationGaps} emptyMessage="No qualification gaps found." />
          </div>
          <TableShell title="Learner Course Matches">
            <div className="divide-y divide-slate-100">
              {coursesQuery.data.learnerMatches.length ? coursesQuery.data.learnerMatches.map((item) => (
                <div key={item.learnerUserId} className="grid gap-4 px-5 py-4 xl:grid-cols-[0.8fr_1fr_1fr]">
                  <div>
                    <p className="font-semibold text-slate-950">{item.learnerName}</p>
                    <p className="text-sm text-slate-500">{item.careerGoal || 'No selected career goal'} | APS {item.apsPoints}</p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Qualified Now</p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {item.qualifiedCourses.length ? item.qualifiedCourses.map((course) => <QuickChip key={`${item.learnerUserId}-${course.name}-qualified`} label={course.name} tone="positive" />) : <span className="text-sm text-slate-500">No current matches</span>}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Close To Qualifying</p>
                    <div className="mt-2 space-y-2">
                      {item.closeCourses.length ? item.closeCourses.map((course) => (
                        <div key={`${item.learnerUserId}-${course.name}-close`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                          <p className="font-semibold text-slate-900">{course.name}</p>
                          <p className="text-xs text-slate-500">{course.reason}</p>
                        </div>
                      )) : <span className="text-sm text-slate-500">No near-term gaps flagged</span>}
                    </div>
                  </div>
                </div>
              )) : <div className="px-5 py-10 text-sm text-slate-500">No learner course matches available.</div>}
            </div>
          </TableShell>
        </>
      )}
    </SectionShell>
  );

  const renderBursaries = () => (
    <SectionShell title="Bursary Readiness" subtitle="Funding matches, missing bursary requirements, deadline alerts, and application-readiness support.">
      {!bursariesQuery.data ? <LoadingGrid /> : (
        <>
          <div className="rounded-[30px] border border-blue-100 bg-gradient-to-r from-blue-50 via-white to-emerald-50 p-5 shadow-sm">
            <p className="text-sm font-semibold text-slate-950">{bursariesQuery.data.headline}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {bursariesQuery.data.metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}
          </div>
          <div className="grid gap-4 xl:grid-cols-3">
            <InsightList title="Funding Interest By Career Field" items={bursariesQuery.data.fundingInterestByCareerField} emptyMessage="No funding signals yet." />
            <InsightList title="Missing Bursary Requirements" items={bursariesQuery.data.missingRequirements} emptyMessage="No missing bursary requirements." />
            <InsightList title="Deadline Alerts" items={bursariesQuery.data.deadlineAlerts} emptyMessage="No bursary deadlines approaching." />
          </div>
          <TableShell title="Saved / Matched Bursaries">
            <div className="divide-y divide-slate-100">
              {bursariesQuery.data.savedBursaries.length ? bursariesQuery.data.savedBursaries.map((bursary) => (
                <div key={`${bursary.title}-${bursary.provider}`} className="flex flex-wrap items-start justify-between gap-3 px-5 py-4">
                  <div>
                    <p className="font-semibold text-slate-950">{bursary.title}</p>
                    <p className="text-sm text-slate-500">{bursary.provider || 'Provider not set'}{bursary.deadline ? ` | Deadline ${bursary.deadline}` : ''}</p>
                    <p className="mt-2 text-sm text-slate-600">{bursary.missingRequirements}</p>
                  </div>
                  <QuickChip label={bursary.eligible ? 'Matched' : 'Review'} tone={bursary.eligible ? 'positive' : 'warning'} />
                </div>
              )) : <div className="px-5 py-10 text-sm text-slate-500">No bursaries available.</div>}
            </div>
          </TableShell>
        </>
      )}
    </SectionShell>
  );

  const renderInterventions = () => (
    <SectionShell
      title="Interventions"
      subtitle="Academic support, career guidance, bursary support, psychosocial referrals, follow-up tasks, and export-ready intervention tracking."
      actions={<Button type="button" className="h-11 rounded-2xl bg-amber-500 hover:bg-amber-600" onClick={() => setInterventionOpen(true)}><Plus className="mr-2 h-4 w-4" />Create intervention</Button>}
    >
      {!interventionsQuery.data ? <LoadingGrid /> : (
        <>
          <div className="rounded-[30px] border border-blue-100 bg-gradient-to-r from-blue-50 via-white to-emerald-50 p-5 shadow-sm">
            <p className="text-sm font-semibold text-slate-950">{interventionsQuery.data.headline}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {interventionsQuery.data.metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}
          </div>
          <div className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
            <InsightList title="Intervention Types" items={interventionsQuery.data.interventionTypes} emptyMessage="No intervention categories recorded." />
            <TableShell title="Intervention Tracking">
              <div className="divide-y divide-slate-100">
                {interventionsQuery.data.items.length ? interventionsQuery.data.items.map((item) => (
                  <div key={item.interventionId} className="grid gap-3 px-5 py-4 xl:grid-cols-[0.9fr_1fr_0.8fr]">
                    <div>
                      <p className="font-semibold text-slate-950">{item.learnerName}</p>
                      <p className="text-xs text-slate-500">{item.assignedBy}</p>
                    </div>
                    <div>
                      <div className="flex flex-wrap gap-2">
                        <QuickChip label={item.supportType} tone="neutral" />
                        <QuickChip label={item.priority} tone={item.priority === 'HIGH' ? 'warning' : 'neutral'} />
                        <QuickChip label={item.status} tone={item.status === 'COMPLETED' ? 'positive' : 'warning'} />
                      </div>
                      <p className="mt-2 text-sm text-slate-600">{item.notes}</p>
                    </div>
                    <div className="text-sm text-slate-600">
                      <p>Follow-up: {formatDateShort(item.followUpDate)}</p>
                      <p className="mt-1 text-xs text-slate-500">Updated {formatDate(item.updatedAt)}</p>
                    </div>
                  </div>
                )) : <div className="px-5 py-10 text-sm text-slate-500">No interventions logged yet.</div>}
              </div>
            </TableShell>
          </div>
        </>
      )}
    </SectionShell>
  );

  const renderLegacySection = (title: string, subtitle: string, rows: ReactNode) => (
    <SectionShell title={title} subtitle={subtitle}>
      <TableShell title={title}>{rows}</TableShell>
    </SectionShell>
  );

  const renderReports = () => (
    <SectionShell title="Reports" subtitle="Whole-school, readiness, subject-gap, bursary, intervention, and district-ready exports with PDF and Excel-friendly output.">
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <TableShell title="Available reports">
          <div className="divide-y divide-slate-100">
            {(reportsQuery.data ?? []).map((report) => (
              <div key={report.key} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
                <div>
                  <p className="font-semibold text-slate-950">{report.title}</p>
                  <p className="text-sm text-slate-500">{report.description}</p>
                </div>
                <div className="flex gap-2">
                  <Button type="button" className="h-9 rounded-xl bg-slate-900 px-3 text-xs hover:bg-slate-800" onClick={() => exportReport.mutate({ type: report.key, format: 'xlsx' })}>Excel / CSV</Button>
                  <Button type="button" className="h-9 rounded-xl bg-blue-600 px-3 text-xs hover:bg-blue-700" onClick={() => exportReport.mutate({ type: report.key, format: 'pdf' })}>PDF</Button>
                </div>
              </div>
            ))}
          </div>
        </TableShell>
        <AdminRightPanel className="xl:w-auto">
          <InsightList title="District-Ready Deliverables" items={(analyticsQuery.data?.districtReadyReportingSummary ?? []).map((item) => ({ ...item, severity: 'neutral' }))} emptyMessage="District-ready export notes will appear here." />
        </AdminRightPanel>
      </div>
    </SectionShell>
  );

  const renderAnnouncements = () => (
    <SectionShell
      title="Announcements"
      subtitle="Broadcast announcements to learners, teachers, or both, with a visible history and future parent communication placeholder."
      actions={<Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => setAnnouncementOpen(true)}><Send className="mr-2 h-4 w-4" />Send announcement</Button>}
    >
      <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <TableShell title="Announcement history">
          <div className="divide-y divide-slate-100">
            {(announcementsQuery.data ?? []).length ? announcementsQuery.data?.map((announcement) => (
              <div key={announcement.id} className="px-5 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950">{announcement.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{announcement.message}</p>
                  </div>
                  <div className="text-right text-xs text-slate-500">
                    <p>{announcement.audience}</p>
                    <p>{formatDate(announcement.createdAt)}</p>
                  </div>
                </div>
              </div>
            )) : <div className="px-5 py-10 text-sm text-slate-500">No announcements have been sent yet.</div>}
          </div>
        </TableShell>
        <InsightList
          title="Communication Roadmap"
          items={[
            { title: 'Learner nudges', detail: 'Use notifications for report uploads, pathway changes, course qualification, and bursary deadlines.', severity: 'neutral' },
            { title: 'Parent communication placeholder', detail: 'Reserved for future guardian email, SMS, and WhatsApp delivery in a later rollout.', severity: 'neutral' },
          ]}
          emptyMessage="Communication roadmap will appear here."
        />
      </div>
    </SectionShell>
  );

  const renderNotifications = () => (
    <SectionShell title="Notifications" subtitle="Recent school-admin notifications, alerts, learner events, and message history for the current account.">
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <TableShell title="Notification stream">
          <div className="divide-y divide-slate-100">
            {(notificationsQuery.data?.content ?? []).length ? notificationsQuery.data?.content.map((notification) => (
              <div key={notification.id} className="px-5 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950">{notification.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{notification.message}</p>
                  </div>
                  <div className="text-right text-xs text-slate-500">
                    <p>{notification.priority}</p>
                    <p>{formatDate(notification.createdAt)}</p>
                  </div>
                </div>
              </div>
            )) : <div className="px-5 py-10 text-sm text-slate-500">No notifications available.</div>}
          </div>
        </TableShell>
        <AdminRightPanel className="xl:w-auto">
          <AdminCard className="p-4">
            <h3 className="text-[18px] font-semibold text-slate-950">Recent Activity</h3>
            <div className="mt-4 space-y-3">
              {(notificationsQuery.data?.content ?? []).slice(0, 4).map((notification) => (
                <div key={`recent-${notification.id}`} className="rounded-[14px] border border-slate-200 bg-slate-50/80 p-3">
                  <p className="text-[14px] font-semibold text-slate-900">{notification.title}</p>
                  <p className="mt-1 text-[12px] text-slate-500">{formatDate(notification.createdAt)}</p>
                </div>
              ))}
              {!(notificationsQuery.data?.content ?? []).length ? <p className="text-[14px] text-slate-500">No recent notification activity.</p> : null}
            </div>
          </AdminCard>
        </AdminRightPanel>
      </div>
    </SectionShell>
  );

  const renderSupportRequests = () => (
    <SectionShell
      title="Support Requests"
      subtitle="Capture school operations, technical, data, onboarding, and reporting support needs with clear priority."
      actions={<Button type="button" className="h-11 rounded-2xl bg-slate-900 hover:bg-slate-800" onClick={() => setSupportOpen(true)}><LifeBuoy className="mr-2 h-4 w-4" />Create request</Button>}
    >
      <TableShell title="Support request history">
        <div className="divide-y divide-slate-100">
          {(supportQuery.data ?? []).length ? supportQuery.data?.map((request) => (
            <div key={request.id} className="px-5 py-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-950">{request.title}</p>
                  <p className="mt-1 text-sm text-slate-600">{request.message}</p>
                </div>
                <div className="flex flex-col items-end gap-1 text-xs">
                  <QuickChip label={request.priority} tone={request.priority === 'HIGH' ? 'warning' : 'neutral'} />
                  <span className="text-slate-500">{request.category}</span>
                </div>
              </div>
            </div>
          )) : <div className="px-5 py-10 text-sm text-slate-500">No support requests logged yet.</div>}
        </div>
      </TableShell>
    </SectionShell>
  );

  const renderSettings = () => (
    <SectionShell title="School Settings" subtitle="School profile, roles, audit visibility, and security handoff for the current school context.">
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <div className="grid gap-4">
        <section className="rounded-[16px] border border-[#e5edf7] bg-white p-4 shadow-sm">
          <h3 className="text-base font-semibold text-slate-950">School Profile</h3>
          <dl className="mt-4 space-y-3 text-sm text-slate-600">
            <div><dt className="font-medium text-slate-900">School</dt><dd>{settingsQuery.data?.schoolName || dashboardQuery.data?.schoolName || 'EduRite School'}</dd></div>
            <div><dt className="font-medium text-slate-900">Registration</dt><dd>{settingsQuery.data?.registrationNumber || 'Not set'}</dd></div>
            <div><dt className="font-medium text-slate-900">District</dt><dd>{settingsQuery.data?.district || 'Not set'}</dd></div>
            <div><dt className="font-medium text-slate-900">Province</dt><dd>{settingsQuery.data?.province || 'Not set'}</dd></div>
            <div><dt className="font-medium text-slate-900">Contact</dt><dd>{settingsQuery.data?.contactEmail || 'Not set'} {settingsQuery.data?.contactPhone ? `| ${settingsQuery.data.contactPhone}` : ''}</dd></div>
            <div><dt className="font-medium text-slate-900">Address</dt><dd>{settingsQuery.data?.address || 'Not set'}</dd></div>
            <div><dt className="font-medium text-slate-900">Roles</dt><dd>{settingsQuery.data?.activeRoles.join(', ') || 'No roles mapped'}</dd></div>
          </dl>
          <Link to="/account/change-password" className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-blue-700">
            Go to security / change password
            <ChevronRight className="h-4 w-4" />
          </Link>
        </section>
        <TableShell title="Recent audit logs">
          <div className="divide-y divide-slate-100">
            {(settingsQuery.data?.recentAuditLogs ?? []).length ? settingsQuery.data?.recentAuditLogs.map((log, index) => (
              <div key={`${log.action}-${index}`} className="px-5 py-4">
                <p className="font-semibold text-slate-950">{log.action}</p>
                <p className="mt-1 text-sm text-slate-600">{log.entityType}{log.entityId ? ` | ${log.entityId}` : ''}</p>
                <p className="mt-1 text-xs text-slate-500">{formatDate(log.createdAt)}</p>
              </div>
            )) : <div className="px-5 py-10 text-sm text-slate-500">No audit logs available.</div>}
          </div>
        </TableShell>
        </div>
        <AdminRightPanel className="xl:w-auto">
          <AdminCard className="p-4">
            <h3 className="text-[18px] font-semibold text-slate-950">Quick Actions</h3>
            <div className="mt-4 space-y-2">
              <AdminActionButton variant="primary" className="w-full justify-center" onClick={() => navigate('/school/reports')}>Open Reports</AdminActionButton>
              <AdminActionButton variant="secondary" className="w-full justify-center" onClick={() => navigate('/account/change-password')}>Security / Change Password</AdminActionButton>
            </div>
          </AdminCard>
        </AdminRightPanel>
      </div>
    </SectionShell>
  );

  const content = (() => {
    if (activeSection === 'dashboard') return renderOverview();
    if (activeSection === 'analytics') return renderAnalytics();
    if (activeSection === 'ai-insights') return renderAiInsights();
    if (activeSection === 'learners') return renderLearners();
    if (activeSection === 'my-school-requests') return renderMySchoolRequests();
    if (activeSection === 'teachers') return <TeacherManagementPanel />;
    if (activeSection === 'career-readiness') return renderCareerReadiness();
    if (activeSection === 'courses') return renderCourses();
    if (activeSection === 'bursaries') return renderBursaries();
    if (activeSection === 'interventions') return renderInterventions();
    if (activeSection === 'classes') return renderClasses();
    if (activeSection === 'subjects') return <SubjectManagementPanel />;
    if (activeSection === 'curriculum') return <SchoolCurriculumPage />;
    if (activeSection === 'assignments') return <AssignmentManagementPanel />;
    if (activeSection === 'assessments') return renderLegacySection('Assessments', 'Exams, tests, quizzes, and SBA assessments requiring completion tracking.', (
      <div className="divide-y divide-slate-100">
        {(assessmentsQuery.data ?? []).map((item) => <div key={item.id} className="px-5 py-4"><p className="font-semibold text-slate-950">{item.title}</p><p className="text-sm text-slate-500">{item.taskType} | Due {new Date(item.dueAt).toLocaleDateString()} | {item.maxMarks} marks</p></div>)}
      </div>
    ));
    if (activeSection === 'results') return renderLegacySection('Results', 'Released results and marked submissions from the current school workflow.', (
      <div className="divide-y divide-slate-100">
        {(resultsQuery.data ?? []).map((item) => <div key={item.submissionId} className="px-5 py-4"><p className="font-semibold text-slate-950">{item.learnerName}</p><p className="text-sm text-slate-500">{item.marks ?? 'Pending'} marks | {item.feedback || 'No feedback yet'}</p></div>)}
      </div>
    ));
    if (activeSection === 'reports') return renderReports();
    if (activeSection === 'announcements') return renderAnnouncements();
    if (activeSection === 'notifications') return renderNotifications();
    if (activeSection === 'support-requests') return renderSupportRequests();
    return renderSettings();
  })();

  const sectionBadges: Partial<Record<CommandSection, number>> = {
    'my-school-requests': learnerJoinRequestsQuery.data?.length ?? 0,
    notifications: notificationsQuery.data?.content?.length ?? 0,
    'support-requests': supportQuery.data?.filter((item) => item.priority === 'HIGH').length ?? 0,
  };

  return (
    <div className="school-admin-command min-h-screen max-w-full overflow-x-hidden bg-[radial-gradient(circle_at_top_left,_rgba(37,99,235,0.08),_transparent_20%),linear-gradient(180deg,_#f8fbff,_#f3f7fd)] p-3 lg:p-4">
      {commandErrors.length ? <ErrorState message={commandErrors[0].message} /> : null}
      <div className="relative w-full overflow-x-hidden">
        <aside className="fixed left-3 top-3 z-20 hidden h-[calc(100vh-1.5rem)] max-h-[calc(100vh-1.5rem)] w-[260px] flex-col overflow-hidden rounded-[18px] border border-slate-900/20 bg-[linear-gradient(180deg,#081224_0%,#0d1730_48%,#101b38_100%)] text-white shadow-2xl lg:flex">
          <div className="shrink-0 border-b border-white/10 px-5 py-5">
            <div className="flex items-center gap-3">
              <DashboardLogo className="block h-10 w-auto shrink-0 object-contain" />
              <div>
                <p className="text-[15px] font-semibold leading-6 text-white">{dashboardQuery.data?.schoolName || user?.fullName || 'School Admin Portal'}</p>
              </div>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <div className="rounded-[14px] border border-white/10 bg-white/5 p-3">
                <p className="text-[12px] uppercase tracking-[0.12em] text-slate-400">Role</p>
                <p className="mt-1 text-[15px] font-semibold leading-5">{dashboardQuery.data?.role || 'School Admin / Principal'}</p>
              </div>
              <div className="rounded-[14px] border border-cyan-400/15 bg-cyan-400/8 p-3">
                <p className="text-[12px] uppercase tracking-[0.12em] text-cyan-100">Status</p>
                <p className="mt-1 text-[15px] font-semibold leading-5 text-white">{dashboardQuery.data?.systemStatus || 'Online'}</p>
              </div>
            </div>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto px-4 py-4 pb-5">
            {NAV_GROUPS.map((group) => (
              <div key={group.title} className="mb-4">
                <p className="mb-2 px-3 text-[12px] font-semibold uppercase tracking-[0.16em] text-slate-500">{group.title}</p>
                <div className="space-y-1">
                  {group.items.filter((item) => item.label !== 'Logout').map((item) => {
                    const Icon = item.icon;
                    const active = item.section === activeSection;
                    const badge = item.section ? sectionBadges[item.section] : 0;
                    if (item.href) {
                      return (
                        <Link key={item.label} to={item.href} className="flex min-h-[44px] items-center gap-3 rounded-[14px] px-3 py-2.5 text-[14px] text-slate-200 hover:bg-white/10">
                          <Icon className="h-4 w-4" />
                          {item.label}
                        </Link>
                      );
                    }
                    return (
                      <button key={item.label} type="button" onClick={() => handleNavigate(item.section!)} className={`flex min-h-[44px] w-full items-center gap-3 rounded-[14px] px-3 py-2.5 text-left text-[14px] transition ${active ? 'bg-[linear-gradient(135deg,#2563eb,#1d4ed8)] text-white shadow-lg shadow-blue-950/30' : 'text-slate-200 hover:bg-white/10'}`}>
                        <Icon className="h-4 w-4" />
                        <span className="flex-1">{item.label}</span>
                        {badge ? <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-orange-500 px-1.5 text-[11px] font-semibold text-white">{badge}</span> : null}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
          <div className="shrink-0 border-t border-white/10 px-4 py-4">
            <button type="button" onClick={logout} className="flex min-h-[44px] w-full items-center gap-3 rounded-[14px] px-3 py-2.5 text-left text-[14px] text-slate-200 transition hover:bg-white/10">
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </div>
        </aside>

        {mobileNavOpen ? (
          <>
            <button type="button" className="fixed inset-0 z-40 bg-slate-950/50 lg:hidden" onClick={() => setMobileNavOpen(false)} aria-label="Close navigation overlay" />
            <aside className="fixed inset-y-2 left-0 z-50 flex h-[calc(100vh-1rem)] max-h-[calc(100vh-1rem)] w-[88vw] max-w-[320px] flex-col overflow-hidden rounded-r-[20px] border-r border-slate-200 bg-[linear-gradient(180deg,#081224_0%,#0d1730_48%,#101b38_100%)] p-4 text-white shadow-2xl lg:hidden">
              <div className="shrink-0 flex items-center justify-between rounded-[18px] border border-white/10 bg-white/5 px-4 py-3">
                <div className="flex items-center gap-3">
                  <DashboardLogo className="block h-10 w-auto shrink-0 object-contain" />
                  <div>
                    <p className="text-[15px] font-semibold">{dashboardQuery.data?.schoolName || 'School Admin Portal'}</p>
                  </div>
                </div>
                <button type="button" onClick={() => setMobileNavOpen(false)} className="rounded-xl border border-white/10 p-2 text-white">
                  <X className="h-4 w-4" />
                </button>
              </div>
              <div className="mt-4 min-h-0 flex-1 overflow-y-auto pb-6">
                {NAV_GROUPS.map((group) => (
                  <div key={group.title} className="mb-4">
                    <p className="mb-2 px-2 text-[12px] font-semibold uppercase tracking-[0.14em] text-slate-400">{group.title}</p>
                    <div className="space-y-1">
                      {group.items.filter((item) => item.label !== 'Logout').map((item) => {
                        const Icon = item.icon;
                        if (item.href) {
                          return <Link key={item.label} to={item.href} className="flex min-h-[46px] items-center gap-3 rounded-[16px] px-3 py-2.5 text-[15px] text-slate-200 hover:bg-white/10"><Icon className="h-4 w-4" />{item.label}</Link>;
                        }
                        return <button key={item.label} type="button" onClick={() => handleNavigate(item.section!)} className={`flex min-h-[46px] w-full items-center gap-3 rounded-[16px] px-3 py-2.5 text-left text-[15px] ${item.section === activeSection ? 'bg-white text-slate-950' : 'text-slate-200 hover:bg-white/10'}`}><Icon className="h-4 w-4" />{item.label}</button>;
                      })}
                    </div>
                  </div>
                ))}
              </div>
              <div className="shrink-0 border-t border-white/10 pt-4">
                <button type="button" onClick={logout} className="flex min-h-[46px] w-full items-center gap-3 rounded-[16px] px-3 py-2.5 text-left text-[15px] text-slate-200 transition hover:bg-white/10">
                  <LogOut className="h-4 w-4" />
                  Logout
                </button>
              </div>
            </aside>
          </>
        ) : null}

        <main className="min-w-0 max-w-full overflow-x-hidden lg:pl-[284px]">
          <div className="min-w-0 max-w-full overflow-x-hidden rounded-[18px] border border-[#e5edf7] bg-white/92 p-4 shadow-sm shadow-slate-200/60 backdrop-blur sm:p-5">
            <div className="flex flex-wrap items-center gap-3">
              <button type="button" className="inline-flex h-11 w-11 items-center justify-center rounded-[16px] border border-slate-200 text-slate-700 lg:hidden" onClick={() => setMobileNavOpen(true)} aria-label="Open school admin navigation">
                <Menu className="h-5 w-5" />
              </button>
              <div className="min-w-0 flex-1 sm:min-w-[240px]">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search learners, teachers, reports, support requests..." className="h-11 rounded-[16px] border-slate-200 bg-slate-50 pl-10 text-[15px]" />
                </div>
              </div>
              <div className="rounded-[16px] border border-slate-200 bg-slate-50 px-4 py-2.5">
                <p className="text-[12px] font-semibold uppercase tracking-[0.14em] text-slate-500">Licensing</p>
                <p className="text-[15px] font-semibold text-slate-900">{dashboardQuery.data?.subscriptionStatus || 'Not linked'}</p>
              </div>
              <div className="rounded-[16px] border border-slate-200 bg-slate-50 px-4 py-2.5">
                <p className="text-[12px] font-semibold uppercase tracking-[0.14em] text-slate-500">User</p>
                <p className="text-[15px] font-semibold text-slate-900">{user?.fullName || 'School Admin'}</p>
              </div>
            </div>
            <div className="mt-4">{content}</div>
          </div>
        </main>
      </div>

      <div className="fixed bottom-4 left-1/2 z-30 w-[calc(100%-1.5rem)] max-w-xl -translate-x-1/2 rounded-3xl border border-slate-200 bg-white/95 p-2 shadow-2xl backdrop-blur lg:hidden">
        <div className="grid grid-cols-3 gap-2">
          <Button type="button" className="h-11 rounded-2xl bg-blue-600 text-xs hover:bg-blue-700" onClick={() => setLearnerModalOpen(true)}>Add learner</Button>
          <Button type="button" className="h-11 rounded-2xl bg-slate-900 text-xs hover:bg-slate-800" onClick={() => navigate('/school/announcements')}>Announce</Button>
          <Button type="button" className="h-11 rounded-2xl bg-emerald-600 text-xs hover:bg-emerald-700" onClick={() => navigate('/school/reports')}>Report</Button>
        </div>
      </div>

      <button type="button" onClick={() => setLearnerModalOpen(true)} className="fixed bottom-24 right-4 z-30 hidden h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-white shadow-2xl hover:bg-blue-700 lg:flex" aria-label="Open quick actions">
        <Plus className="h-6 w-6" />
      </button>

      {selectedLearnerId ? (
        <div className="fixed inset-0 z-50 bg-slate-950/55">
          <button type="button" className="absolute inset-0" onClick={() => setSelectedLearnerId(null)} aria-label="Close learner profile" />
          <aside className="absolute inset-y-0 right-0 w-full max-w-2xl overflow-y-auto bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-700">Learner Intelligence Profile</p>
                <h3 className="mt-2 text-2xl font-semibold text-slate-950">{learnerProfileQuery.data?.learnerName || 'Loading learner'}</h3>
              </div>
              <button type="button" onClick={() => setSelectedLearnerId(null)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            {!learnerProfileQuery.data ? (
              <div className="mt-6 space-y-3">{Array.from({ length: 6 }).map((_, index) => <div key={index} className="h-20 animate-pulse rounded-[24px] bg-slate-100" />)}</div>
            ) : (
              <div className="mt-6 space-y-4">
                <div className="rounded-[28px] border border-slate-200 bg-slate-50 p-5">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Overview</p>
                      <p className="mt-2 text-sm text-slate-600">{learnerProfileQuery.data.grade || 'Grade pending'} {learnerProfileQuery.data.className || ''}</p>
                      <p className="mt-1 text-sm text-slate-600">{learnerProfileQuery.data.teacherName || 'Teacher not linked'}</p>
                      <p className="mt-1 text-sm text-slate-600">{learnerProfileQuery.data.email}</p>
                    </div>
                    <div>
                      <div className="flex flex-wrap gap-2">
                        <QuickChip label={`APS ${learnerProfileQuery.data.apsPoints}`} tone="positive" />
                        <QuickChip label={learnerProfileQuery.data.readinessLevel} tone={learnerProfileQuery.data.readinessLevel.toLowerCase().includes('support') ? 'warning' : 'positive'} />
                        <QuickChip label={learnerProfileQuery.data.reportUploadStatus || 'Report pending'} tone={(learnerProfileQuery.data.reportUploadStatus || '').toLowerCase().includes('complete') ? 'positive' : 'warning'} />
                      </div>
                      <p className="mt-3 text-sm text-slate-600">Career goal: {learnerProfileQuery.data.careerGoal || 'Not selected'}</p>
                      <p className="mt-1 text-sm text-slate-600">POPIA consent: {learnerProfileQuery.data.consentStatus || 'Pending'}</p>
                      <p className="mt-1 text-sm text-slate-600">Follow-up date: {learnerProfileQuery.data.followUpDate || 'Not set'}</p>
                    </div>
                  </div>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Required Subjects" items={learnerProfileQuery.data.requiredSubjects.map((item) => ({ title: item.label, detail: item.value, severity: item.tone }))} emptyMessage="No required subjects available." />
                  <InsightList title="Required Marks" items={learnerProfileQuery.data.requiredMarks.map((item) => ({ title: item.label, detail: item.value, severity: item.tone }))} emptyMessage="No subject marks available." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Courses Qualified For Now" items={learnerProfileQuery.data.coursesQualifiedNow.map((item) => ({ title: item.name, detail: item.reason, severity: 'positive' }))} emptyMessage="No current course matches." />
                  <InsightList title="Courses Close To Qualifying For" items={learnerProfileQuery.data.coursesCloseToQualifyingFor.map((item) => ({ title: item.name, detail: item.reason, severity: 'warning' }))} emptyMessage="No near-term course matches." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Alternative Pathways" items={learnerProfileQuery.data.alternativePathways} emptyMessage="No alternative pathways suggested." />
                  <InsightList title="Missing Requirements" items={learnerProfileQuery.data.missingRequirements.map((item) => ({ title: item.label, detail: item.value, severity: item.tone }))} emptyMessage="No missing requirements captured." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Bursary Matches" items={learnerProfileQuery.data.bursaryMatches.map((item) => ({ title: item.title, detail: `${item.provider || 'Provider not set'} | ${item.missingRequirements}`, severity: item.eligible ? 'positive' : 'warning' }))} emptyMessage="No bursary matches." />
                  <InsightList title="Bursary Deadlines" items={learnerProfileQuery.data.bursaryDeadlines.map((item) => ({ title: item.label, detail: item.value, severity: item.tone }))} emptyMessage="No bursary deadlines recorded." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Intervention Plan" items={learnerProfileQuery.data.interventionPlan.map((item) => ({ title: item.supportType, detail: `${item.status} | ${item.notes}`, severity: item.status === 'COMPLETED' ? 'positive' : 'warning' }))} emptyMessage="No intervention plan recorded." />
                  <InsightList title="Teacher Notes" items={learnerProfileQuery.data.teacherNotes.map((item) => ({ title: item.title, detail: `${item.author} | ${item.detail}`, severity: 'neutral' }))} emptyMessage="No teacher notes available." />
                </div>
                <InsightList title="Activity Timeline" items={learnerProfileQuery.data.activityTimeline.map((item) => ({ title: item.title, detail: `${item.detail} | ${formatDate(item.occurredAt)}`, severity: 'neutral' }))} emptyMessage="No activity timeline yet." />
                <div className="flex flex-wrap gap-2">
                  <Button type="button" className="h-11 rounded-2xl bg-amber-500 hover:bg-amber-600" onClick={() => { setInterventionForm((state) => ({ ...state, learnerUserId: learnerProfileQuery.data.learnerUserId })); setInterventionOpen(true); }}>Create intervention</Button>
                  <Button type="button" className="h-11 rounded-2xl bg-slate-900 hover:bg-slate-800" onClick={() => navigate('/school/reports')}>Generate report</Button>
                </div>
              </div>
            )}
          </aside>
        </div>
      ) : null}

      {selectedTeacherId ? (
        <div className="fixed inset-0 z-50 bg-slate-950/55">
          <button type="button" className="absolute inset-0" onClick={() => setSelectedTeacherId(null)} aria-label="Close teacher profile" />
          <aside className="absolute inset-y-0 right-0 w-full max-w-2xl overflow-y-auto bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-700">Teacher Intelligence Profile</p>
                <h3 className="mt-2 text-2xl font-semibold text-slate-950">{teacherDetailQuery.data?.summary.fullName || 'Loading teacher'}</h3>
              </div>
              <button type="button" onClick={() => setSelectedTeacherId(null)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            {!teacherDetailQuery.data ? (
              <div className="mt-6 space-y-3">{Array.from({ length: 6 }).map((_, index) => <div key={index} className="h-20 animate-pulse rounded-[24px] bg-slate-100" />)}</div>
            ) : (
              <div className="mt-6 space-y-4">
                <div className="rounded-[28px] border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-wrap items-center gap-2">
                    <QuickChip label={teacherDetailQuery.data.summary.status} tone={teacherDetailQuery.data.summary.status === 'ACTIVE' ? 'positive' : teacherDetailQuery.data.summary.status === 'PENDING' ? 'warning' : 'critical'} />
                    <QuickChip label={`Engagement ${teacherDetailQuery.data.summary.engagementScore}`} tone="neutral" />
                    <QuickChip label={`Learners ${teacherDetailQuery.data.summary.learnerCount}`} tone="neutral" />
                    <QuickChip label={`Interventions ${teacherDetailQuery.data.summary.interventionCount}`} tone={teacherDetailQuery.data.summary.interventionCount > 0 ? 'warning' : 'positive'} />
                  </div>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Profile" items={teacherDetailQuery.data.profile.map((item) => ({ title: item.label, detail: item.value, severity: 'neutral' }))} emptyMessage="No profile data." />
                  <InsightList title="Approval Center" items={teacherDetailQuery.data.approvalDetails.map((item) => ({ title: item.label, detail: item.value, severity: 'neutral' }))} emptyMessage="No approval details." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Academics" items={[
                    { title: 'Subjects', detail: teacherDetailQuery.data.subjects.join(', ') || 'No subjects assigned', severity: 'neutral' },
                    { title: 'Classes', detail: teacherDetailQuery.data.classes.join(', ') || 'No classes assigned', severity: 'neutral' },
                    { title: 'Learners', detail: String(teacherDetailQuery.data.summary.learnerCount), severity: 'neutral' },
                  ]} emptyMessage="No academic assignment data." />
                  <InsightList title="Performance" items={teacherDetailQuery.data.performance.map((item) => ({ title: item.label, detail: item.value, severity: 'neutral' }))} emptyMessage="No performance data." />
                </div>
                <InsightList title="Career Readiness Impact" items={teacherDetailQuery.data.readinessImpact.map((item) => ({ title: item.label, detail: item.value, severity: 'neutral' }))} emptyMessage="No readiness impact data." />
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Notes" items={teacherDetailQuery.data.notes} emptyMessage="No teacher notes." />
                  <InsightList title="Documents" items={teacherDetailQuery.data.documents.map((item) => ({ title: item.title, detail: item.status, severity: 'neutral' }))} emptyMessage="No uploaded documents recorded." />
                </div>
                <InsightList title="Interventions" items={teacherDetailQuery.data.interventions.map((item) => ({ title: item.learnerName, detail: `${item.supportType} | ${item.status} | ${item.notes}`, severity: item.status === 'COMPLETED' ? 'positive' : 'warning' }))} emptyMessage="No interventions recorded." />
                <InsightList title="Activity Timeline" items={teacherDetailQuery.data.activityTimeline.map((item) => ({ title: item.title, detail: `${item.detail} | ${formatDate(item.occurredAt)}`, severity: 'neutral' }))} emptyMessage="No teacher activity timeline yet." />
                <InsightList title="Approval History" items={teacherDetailQuery.data.approvalHistory.map((item) => ({ title: item.action, detail: formatDate(item.createdAt), severity: 'neutral' }))} emptyMessage="No approval history recorded." />
                <div className="flex flex-wrap gap-2">
                  <Button type="button" className="h-11 rounded-2xl bg-emerald-600 hover:bg-emerald-700" onClick={() => moderateTeacher.mutate({ teacherId: teacherDetailQuery.data.summary.teacherUserId, action: 'approve' })}>Approve</Button>
                  <Button type="button" className="h-11 rounded-2xl bg-amber-500 hover:bg-amber-600" onClick={() => moderateTeacher.mutate({ teacherId: teacherDetailQuery.data.summary.teacherUserId, action: teacherDetailQuery.data.summary.status === 'SUSPENDED' ? 'reactivate' : 'suspend' })}>{teacherDetailQuery.data.summary.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}</Button>
                  <Button type="button" className="h-11 rounded-2xl bg-rose-600 hover:bg-rose-700" onClick={() => moderateTeacher.mutate({ teacherId: teacherDetailQuery.data.summary.teacherUserId, action: 'reject' })}>Reject</Button>
                </div>
              </div>
            )}
          </aside>
        </div>
      ) : null}

      {selectedClassId ? (
        <div className="fixed inset-0 z-50 bg-slate-950/55">
          <button type="button" className="absolute inset-0" onClick={() => setSelectedClassId(null)} aria-label="Close class profile" />
          <aside className="absolute inset-y-0 right-0 w-full max-w-2xl overflow-y-auto bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-700">Class Intelligence Profile</p>
                <h3 className="mt-2 text-2xl font-semibold text-slate-950">{classProfileQuery.data?.summary.grade} {classProfileQuery.data?.summary.className || 'Loading class'}</h3>
              </div>
              <button type="button" onClick={() => setSelectedClassId(null)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            {!classProfileQuery.data ? (
              <div className="mt-6 space-y-3">{Array.from({ length: 6 }).map((_, index) => <div key={index} className="h-20 animate-pulse rounded-[24px] bg-slate-100" />)}</div>
            ) : (
              <div className="mt-6 space-y-4">
                <div className="rounded-[28px] border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-wrap gap-2">
                    <QuickChip label={`APS ${classProfileQuery.data.summary.averageAps}`} tone="positive" />
                    <QuickChip label={`Career Ready ${classProfileQuery.data.summary.careerReadinessPercent}%`} tone={classProfileQuery.data.summary.careerReadinessPercent >= 60 ? 'positive' : 'warning'} />
                    <QuickChip label={`Bursary Ready ${classProfileQuery.data.summary.bursaryReadinessPercent}%`} tone={classProfileQuery.data.summary.bursaryReadinessPercent >= 40 ? 'positive' : 'warning'} />
                    <QuickChip label={`Interventions ${classProfileQuery.data.summary.interventionCount}`} tone={classProfileQuery.data.summary.interventionCount > 0 ? 'warning' : 'positive'} />
                  </div>
                  <p className="mt-3 text-sm text-slate-600">{classProfileQuery.data.summary.classTeacher} | {classProfileQuery.data.summary.learnerCount} learners | {classProfileQuery.data.summary.subjectCount} subjects</p>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Overview" items={[
                    { title: 'Grade', detail: classProfileQuery.data.summary.grade, severity: 'neutral' },
                    { title: 'Teacher', detail: classProfileQuery.data.summary.classTeacher, severity: 'neutral' },
                    { title: 'Learner count', detail: String(classProfileQuery.data.summary.learnerCount), severity: 'neutral' },
                  ]} emptyMessage="No overview data." />
                  <InsightList title="Academics" items={classProfileQuery.data.academics} emptyMessage="No academic data." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Career Readiness" items={classProfileQuery.data.careerReadiness} emptyMessage="No readiness data." />
                  <InsightList title="Bursaries" items={classProfileQuery.data.bursaries} emptyMessage="No bursary data." />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <InsightList title="Interventions" items={classProfileQuery.data.interventions} emptyMessage="No interventions." />
                  {!classAiQuery.data || !classAiQuery.data.dataAvailable ? (
                    <div className="rounded-[30px] border border-dashed border-blue-200 bg-white px-6 py-10 text-center shadow-sm">
                      <Bot className="mx-auto h-10 w-10 text-blue-600" />
                      <p className="mt-3 text-sm text-slate-600">{classAiQuery.data?.emptyStateMessage || 'AI insights will appear as learner reports become available.'}</p>
                    </div>
                  ) : <InsightList title="AI Insights" items={classAiQuery.data.items} emptyMessage="No AI insights." />}
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <ChartPanel title="APS Distribution" points={classAnalyticsQuery.data?.apsDistribution ?? []} />
                  <ChartPanel title="Subject Performance Trends" points={classAnalyticsQuery.data?.subjectPerformanceTrends ?? []} />
                  <ChartPanel title="Readiness Trends" points={classAnalyticsQuery.data?.readinessTrends ?? []} />
                  <ChartPanel title="Intervention Trends" points={classAnalyticsQuery.data?.interventionTrends ?? []} />
                </div>
                <InsightList title="Subject Teachers" items={classProfileQuery.data.subjectTeachers} emptyMessage="No subject teachers assigned." />
                <TableShell title="Class Learner Breakdown">
                  <div className="overflow-x-auto">
                    <table className="min-w-[920px] divide-y divide-slate-100 text-sm">
                      <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                        <tr>
                          <th className="px-5 py-3">Name</th>
                          <th className="px-5 py-3">APS</th>
                          <th className="px-5 py-3">Career Goal</th>
                          <th className="px-5 py-3">Readiness</th>
                          <th className="px-5 py-3">Bursary</th>
                          <th className="px-5 py-3">Report</th>
                          <th className="px-5 py-3">Intervention</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {classProfileQuery.data.learners.map((learner) => (
                          <tr key={learner.learnerUserId}>
                            <td className="px-5 py-4 font-semibold text-slate-950">{learner.learnerName}</td>
                            <td className="px-5 py-4 text-slate-600">{learner.apsPoints}</td>
                            <td className="px-5 py-4 text-slate-600">{learner.careerGoal || 'Not selected'}</td>
                            <td className="px-5 py-4"><QuickChip label={learner.readinessStatus} tone={learner.needsIntervention ? 'warning' : 'positive'} /></td>
                            <td className="px-5 py-4 text-slate-600">{learner.bursaryMatchCount}</td>
                            <td className="px-5 py-4"><QuickChip label={learner.reportUploadStatus || 'Pending'} tone={(learner.reportUploadStatus || '').toLowerCase().includes('complete') ? 'positive' : 'warning'} /></td>
                            <td className="px-5 py-4"><QuickChip label={learner.needsIntervention ? 'Open' : 'Clear'} tone={learner.needsIntervention ? 'warning' : 'positive'} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </TableShell>
                <InsightList title="Activity Timeline" items={classProfileQuery.data.activityTimeline.map((item) => ({ title: item.title, detail: `${item.detail} | ${formatDate(item.occurredAt)}`, severity: 'neutral' }))} emptyMessage="No class activity timeline yet." />
              </div>
            )}
          </aside>
        </div>
      ) : null}

      {learnerModalOpen ? (
        <div className="fixed inset-0 z-50 flex items-start justify-center bg-slate-950/60 p-4">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-semibold text-slate-950">Add Learner</h3>
                <p className="mt-1 text-sm text-slate-600">Create a learner account, guardian contact details, and class-ready credentials.</p>
              </div>
              <button type="button" onClick={() => setLearnerModalOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-6 grid gap-3 md:grid-cols-2">
              <Input value={learnerForm.firstName} onChange={(event) => setLearnerForm((state) => ({ ...state, firstName: event.target.value }))} placeholder="First name" className="h-11 rounded-2xl" />
              <Input value={learnerForm.lastName} onChange={(event) => setLearnerForm((state) => ({ ...state, lastName: event.target.value }))} placeholder="Last name" className="h-11 rounded-2xl" />
              <Input value={learnerForm.email} onChange={(event) => setLearnerForm((state) => ({ ...state, email: event.target.value }))} placeholder="Email (optional)" className="h-11 rounded-2xl" />
              <Input value={learnerForm.username} onChange={(event) => setLearnerForm((state) => ({ ...state, username: event.target.value }))} placeholder="Username (optional)" className="h-11 rounded-2xl" />
              <Input value={learnerForm.password} onChange={(event) => setLearnerForm((state) => ({ ...state, password: event.target.value }))} placeholder="Password (optional)" className="h-11 rounded-2xl" />
              <Input value={learnerForm.grade} onChange={(event) => setLearnerForm((state) => ({ ...state, grade: event.target.value }))} placeholder="Grade" className="h-11 rounded-2xl" />
              <Input value={learnerForm.className} onChange={(event) => setLearnerForm((state) => ({ ...state, className: event.target.value }))} placeholder="Class" className="h-11 rounded-2xl" />
              <Input value={learnerForm.careerGoal} onChange={(event) => setLearnerForm((state) => ({ ...state, careerGoal: event.target.value }))} placeholder="Career goal" className="h-11 rounded-2xl" />
              <Input value={learnerForm.parentGuardianName} onChange={(event) => setLearnerForm((state) => ({ ...state, parentGuardianName: event.target.value }))} placeholder="Parent / guardian name" className="h-11 rounded-2xl" />
              <Input value={learnerForm.parentGuardianPhone} onChange={(event) => setLearnerForm((state) => ({ ...state, parentGuardianPhone: event.target.value }))} placeholder="Parent / guardian phone" className="h-11 rounded-2xl" />
              <Input value={learnerForm.parentGuardianEmail} onChange={(event) => setLearnerForm((state) => ({ ...state, parentGuardianEmail: event.target.value }))} placeholder="Parent / guardian email" className="h-11 rounded-2xl md:col-span-2" />
            </div>
            <label className="mt-4 flex items-center gap-3 text-sm text-slate-700">
              <input type="checkbox" checked={learnerForm.popiaConsentAccepted} onChange={(event) => setLearnerForm((state) => ({ ...state, popiaConsentAccepted: event.target.checked }))} />
              POPIA consent confirmed for learner onboarding
            </label>
            <div className="mt-6 flex flex-wrap justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setLearnerModalOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => createLearner.mutate()} disabled={createLearner.isPending || !learnerForm.firstName.trim() || !learnerForm.lastName.trim()}>
                {createLearner.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Create learner
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {learnerAssignmentOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-semibold text-slate-950">Assign Class</h3>
                <p className="mt-1 text-sm text-slate-600">Link this learner to a class and subject without changing the existing learner record.</p>
              </div>
              <button type="button" onClick={() => setLearnerAssignmentOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3">
              <select value={learnerAssignmentForm.learnerUserId} onChange={(event) => setLearnerAssignmentForm((state) => ({ ...state, learnerUserId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select learner</option>
                {learnerItems.map((learner) => <option key={learner.learnerUserId} value={learner.learnerUserId}>{learner.learnerName}</option>)}
              </select>
              <select value={learnerAssignmentForm.classId} onChange={(event) => setLearnerAssignmentForm((state) => ({ ...state, classId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select class</option>
                {(classesQuery.data?.items ?? []).map((item) => <option key={item.classId} value={item.classId}>{item.grade} {item.className}</option>)}
              </select>
              <select value={learnerAssignmentForm.subjectId} onChange={(event) => setLearnerAssignmentForm((state) => ({ ...state, subjectId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select subject</option>
                {(subjectsQuery.data ?? []).map((subject) => <option key={subject.id} value={subject.id}>{subject.subjectName}</option>)}
              </select>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setLearnerAssignmentOpen(false)}>Cancel</Button>
              <Button
                type="button"
                className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700"
                onClick={() => assignLearnerToClassMutation.mutate()}
                disabled={assignLearnerToClassMutation.isPending || !learnerAssignmentForm.learnerUserId || !learnerAssignmentForm.classId || !learnerAssignmentForm.subjectId}
              >
                {assignLearnerToClassMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Save Assignment
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {classEditOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-xl font-semibold text-slate-950">Edit Class</h3>
              <button type="button" onClick={() => setClassEditOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <Input value={classForm.grade} onChange={(event) => setClassForm((state) => ({ ...state, grade: event.target.value }))} placeholder="Grade" className="h-11 rounded-2xl" />
              <Input value={classForm.className} onChange={(event) => setClassForm((state) => ({ ...state, className: event.target.value }))} placeholder="Class name" className="h-11 rounded-2xl" />
              <Input type="number" value={String(classForm.academicYear)} onChange={(event) => setClassForm((state) => ({ ...state, academicYear: Number(event.target.value) || new Date().getFullYear() }))} placeholder="Academic year" className="h-11 rounded-2xl" />
              <Input value={classForm.term} onChange={(event) => setClassForm((state) => ({ ...state, term: event.target.value }))} placeholder="Term" className="h-11 rounded-2xl" />
            </div>
            <label className="mt-4 flex items-center gap-3 text-sm text-slate-700">
              <input type="checkbox" checked={classForm.active} onChange={(event) => setClassForm((state) => ({ ...state, active: event.target.checked }))} />
              Active class
            </label>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setClassEditOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => updateClassMutation.mutate()} disabled={updateClassMutation.isPending || !classForm.id}>
                {updateClassMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Save class
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {classAssignmentOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-xl font-semibold text-slate-950">Assign Teacher To Class</h3>
              <button type="button" onClick={() => setClassAssignmentOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3">
              <select value={assignmentForm.teacherUserId} onChange={(event) => setAssignmentForm((state) => ({ ...state, teacherUserId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select teacher</option>
                {(teachersQuery.data?.items ?? []).map((teacher) => <option key={teacher.teacherUserId} value={teacher.teacherUserId}>{teacher.fullName}</option>)}
              </select>
              <select value={assignmentForm.subjectId} onChange={(event) => setAssignmentForm((state) => ({ ...state, subjectId: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="">Select subject</option>
                {(subjectsQuery.data ?? []).map((subject) => <option key={subject.id} value={subject.id}>{subject.subjectName}</option>)}
              </select>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setClassAssignmentOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => assignTeacherMutation.mutate()} disabled={assignTeacherMutation.isPending || !assignmentForm.classId || !assignmentForm.subjectId || !assignmentForm.teacherUserId}>
                {assignTeacherMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Assign teacher
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {announcementOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-xl font-semibold text-slate-950">Send Announcement</h3>
              <button type="button" onClick={() => setAnnouncementOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3">
              <select value={announcementForm.audience} onChange={(event) => setAnnouncementForm((state) => ({ ...state, audience: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="ALL">Learners and teachers</option>
                <option value="LEARNERS">Learners</option>
                <option value="TEACHERS">Teachers</option>
              </select>
              <Input value={announcementForm.title} onChange={(event) => setAnnouncementForm((state) => ({ ...state, title: event.target.value }))} placeholder="Announcement title" className="h-11 rounded-2xl" />
              <textarea value={announcementForm.message} onChange={(event) => setAnnouncementForm((state) => ({ ...state, message: event.target.value }))} rows={6} className="w-full rounded-2xl border border-slate-200 px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100" placeholder="Write the announcement message" />
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setAnnouncementOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-blue-600 hover:bg-blue-700" onClick={() => createAnnouncement.mutate()} disabled={createAnnouncement.isPending || !announcementForm.title.trim() || !announcementForm.message.trim()}>
                {createAnnouncement.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Send announcement
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {supportOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-xl font-semibold text-slate-950">Create Support Request</h3>
              <button type="button" onClick={() => setSupportOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3">
              <select value={supportForm.category} onChange={(event) => setSupportForm((state) => ({ ...state, category: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="Technical">Technical</option>
                <option value="Training">Training</option>
                <option value="Reporting">Reporting</option>
                <option value="Data">Data</option>
              </select>
              <select value={supportForm.priority} onChange={(event) => setSupportForm((state) => ({ ...state, priority: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
              <Input value={supportForm.title} onChange={(event) => setSupportForm((state) => ({ ...state, title: event.target.value }))} placeholder="Support request title" className="h-11 rounded-2xl" />
              <textarea value={supportForm.message} onChange={(event) => setSupportForm((state) => ({ ...state, message: event.target.value }))} rows={6} className="w-full rounded-2xl border border-slate-200 px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100" placeholder="Describe the support need" />
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setSupportOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-slate-900 hover:bg-slate-800" onClick={() => createSupportRequest.mutate()} disabled={createSupportRequest.isPending || !supportForm.title.trim() || !supportForm.message.trim()}>
                {createSupportRequest.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Submit request
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {interventionOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-2xl">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-xl font-semibold text-slate-950">Create Intervention</h3>
              <button type="button" onClick={() => setInterventionOpen(false)} className="rounded-xl border border-slate-200 p-2 text-slate-600"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <Input value={interventionForm.learnerUserId} onChange={(event) => setInterventionForm((state) => ({ ...state, learnerUserId: event.target.value }))} placeholder="Learner user ID" className="h-11 rounded-2xl md:col-span-2" />
              <select value={interventionForm.supportType} onChange={(event) => setInterventionForm((state) => ({ ...state, supportType: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="Academic support">Academic support</option>
                <option value="Career guidance">Career guidance</option>
                <option value="Subject choice guidance">Subject choice guidance</option>
                <option value="Bursary support">Bursary support</option>
                <option value="Parent meeting">Parent meeting</option>
                <option value="Psychosocial referral">Psychosocial referral</option>
                <option value="Vocational pathway discussion">Vocational pathway discussion</option>
              </select>
              <select value={interventionForm.priority} onChange={(event) => setInterventionForm((state) => ({ ...state, priority: event.target.value }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
              <Input value={interventionForm.followUpDate} onChange={(event) => setInterventionForm((state) => ({ ...state, followUpDate: event.target.value }))} type="date" className="h-11 rounded-2xl md:col-span-2" />
              <textarea value={interventionForm.notes} onChange={(event) => setInterventionForm((state) => ({ ...state, notes: event.target.value }))} rows={6} className="md:col-span-2 w-full rounded-2xl border border-slate-200 px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-100" placeholder="Intervention notes and actions" />
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200" onClick={() => setInterventionOpen(false)}>Cancel</Button>
              <Button type="button" className="h-11 rounded-2xl bg-amber-500 hover:bg-amber-600" onClick={() => createIntervention.mutate()} disabled={createIntervention.isPending || !interventionForm.learnerUserId.trim() || !interventionForm.notes.trim()}>
                {createIntervention.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Save intervention
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

