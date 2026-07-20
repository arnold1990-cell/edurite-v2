import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { formatPlanPrice } from '@/lib/pricing';
import { studentService } from '@/services/studentService';
import { keepPreviousData } from '@tanstack/react-query';
import { recommendationService } from '@/services/recommendationService';
import { AI_ERROR_MESSAGE, aiGuidanceService } from '@/services/aiGuidanceService';
import { bursaryService } from '@/services/bursaryService';
import { notificationService } from '@/services/notificationService';
import { applicationService } from '@/services/applicationService';
import { subscriptionService } from '@/services/subscriptionService';
import { settingsService } from '@/services/settingsService';
import { careerService } from '@/services/careerService';
import { psychometricService } from '@/services/psychometricService';
import { learningService } from '@/services/learningService';
import { gamificationService } from '@/services/gamificationService';
import { accountService } from '@/services/accountService';
import { featureModulesService } from '@/services/featureModulesService';
import { jobsService } from '@/services/jobsService';
import { schoolService } from '@/services/schoolService';
import { formatRewardClaimSummary, formatRewardEventSummary, getRewardClaimState, normalizeRewardText, REWARD_CLAIM_COST } from '@/pages/student/rewards.utils';
import { AlertTriangle, Bell, BookOpen, Brain, BriefcaseBusiness, CheckCheck, CheckCircle2, CircleDollarSign, Clock3, FileText, FileUp, GraduationCap, PlayCircle, Settings, Sparkles, Target, UserCircle2, Video, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import type {
  ApiError,
  Notification,
  OpportunityType,
  PaymentProviderCode,
  PlanType,
  ReadinessStatus,
  StudentDashboard,
  StudentProfile,
  StudentSavedProfilePayload,
  StudentSubjectAchievement,
  UnifiedOpportunity,
  UniversityRecommendedCareer,
  UniversityRecommendedProgramme,
  UniversitySourcesAnalysisResponse,
  LearningResource,
  JobOpportunity,
} from '@/types';

const Section = ({ title, children, description }: { title: string; children: React.ReactNode; description?: string }) => (
  <section className="student-page-section">
    <header>
      <h1 className="student-page-title">{title}</h1>
      {description ? <p className="student-page-subtitle">{description}</p> : null}
    </header>
    <div className="space-y-5">{children}</div>
  </section>
);
const Card = ({ label, value }: { label: string; value: string | number }) => (
  <div className="student-metric-card">
    <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
    <p className="mt-2 text-3xl font-bold tracking-normal text-slate-950">{value}</p>
  </div>
);
const schoolStatusBadgeColor = (status?: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED') => {
  if (status === 'APPROVED') return 'emerald' as const;
  if (status === 'PENDING') return 'amber' as const;
  if (status === 'REJECTED') return 'slate' as const;
  return 'slate' as const;
};
const schoolStatusLabel = (status?: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED') => {
  if (status === 'APPROVED') return 'Connected / Approved';
  if (status === 'PENDING') return 'Pending approval';
  if (status === 'REJECTED') return 'Rejected';
  return 'Not linked';
};
const schoolGradeClassLabel = (status?: { learnerGrade?: string | null; learnerClassName?: string | null }) => {
  const parts = [status?.learnerGrade, status?.learnerClassName].filter(Boolean);
  return parts.length ? parts.join(' | ') : 'Assigned after school approval';
};
const readinessPalette: Record<ReadinessStatus, { badge: 'emerald' | 'amber' | 'slate'; panel: string; label: string }> = {
  GREEN: { badge: 'emerald', panel: 'border-emerald-200 bg-emerald-50/70', label: 'Ready' },
  ORANGE: { badge: 'amber', panel: 'border-amber-200 bg-amber-50/70', label: 'Almost there' },
  RED: { badge: 'slate', panel: 'border-rose-200 bg-rose-50/70', label: 'Not ready yet' },
};

const STUDENT_GRADES = ['Grade 8', 'Grade 9', 'Grade 10', 'Grade 11', 'Grade 12'] as const;
const SENIOR_PHASE_SUBJECT_OPTIONS = [
  'Home Language',
  'First Additional Language',
  'Mathematics',
  'Natural Sciences',
  'Social Sciences',
  'Technology',
  'Economic and Management Sciences',
  'Life Orientation',
  'Creative Arts',
] as const;
const FET_SUBJECT_OPTIONS = [
  'Accounting',
  'Agricultural Management Practices',
  'Agricultural Sciences',
  'Agricultural Technology',
  'Business Studies',
  'Civil Technology',
  'Computer Applications Technology',
  'Consumer Studies',
  'Dance Studies',
  'Design',
  'Dramatic Arts',
  'Economics',
  'Electrical Technology',
  'Engineering Graphics and Design',
  'Geography',
  'History',
  'Hospitality Studies',
  'Information Technology',
  'Life Orientation',
  'Life Sciences',
  'Mathematical Literacy',
  'Mathematics',
  'Mechanical Technology',
  'Music',
  'Physical Sciences',
  'Religion Studies',
  'Tourism',
  'Visual Arts',
  'Home Language',
  'First Additional Language',
  'Second Additional Language',
] as const;
const ALL_SUBJECT_OPTIONS = [...SENIOR_PHASE_SUBJECT_OPTIONS, ...FET_SUBJECT_OPTIONS] as const;
const isSeniorPhaseGrade = (grade: string) => grade === 'Grade 8' || grade === 'Grade 9';
const isFetGrade = (grade: string) => grade === 'Grade 10' || grade === 'Grade 11' || grade === 'Grade 12';
const ACHIEVEMENT_LEVELS = [
  { value: 1, label: 'Level 1 (0-29%)' },
  { value: 2, label: 'Level 2 (30-39%)' },
  { value: 3, label: 'Level 3 (40-49%)' },
  { value: 4, label: 'Level 4 (50-59%)' },
  { value: 5, label: 'Level 5 (60-69%)' },
  { value: 6, label: 'Level 6 (70-79%)' },
  { value: 7, label: 'Level 7 (80-100%)' },
] as const;

type ProgrammeRequirementCheck = { subjectName: string; minLevel: number };
type ProgrammeQualificationEvaluation = {
  programmeName: string;
  university: string;
  admissionRequirements: string[];
  parsedChecks: ProgrammeRequirementCheck[];
  qualifies: boolean | null;
  qualificationMessage: string;
  missingRequirements: string[];
  weakSubjects: string[];
  improvementSuggestions: string[];
  alternatives: string[];
};

const normalizeText = (value: string) => value.toLowerCase().replace(/[^a-z0-9 ]/gi, ' ').replace(/\s+/g, ' ').trim();
const normalizeSubject = (value: string) => normalizeText(value).replace(/\b(home|first|additional|language)\b/g, '').replace(/\s+/g, ' ').trim();
const getInitials = (firstName?: string, lastName?: string) => `${(firstName?.[0] ?? '').toUpperCase()}${(lastName?.[0] ?? '').toUpperCase()}` || 'ST';
const toSubjectRow = (item?: StudentSubjectAchievement | null): StudentSubjectAchievement => ({
  subjectName: item?.subjectName ?? '',
  achievementLevel: item?.achievementLevel ?? null,
});

const parseProgrammeChecks = (requirements: string[] = []): ProgrammeRequirementCheck[] => {
  const subjectsByLength = [...ALL_SUBJECT_OPTIONS].sort((a, b) => b.length - a.length);
  const checks: ProgrammeRequirementCheck[] = [];
  const seen = new Set<string>();

  requirements.forEach((requirement) => {
    const reqText = normalizeText(requirement);
    const levelMatch = requirement.match(/(?:level|lvl)\s*([1-7])/i) ?? requirement.match(/([1-7])\s*(?:or above|minimum|\+)/i);
    if (!levelMatch) return;
    const minLevel = Number(levelMatch[1]);
    if (!Number.isFinite(minLevel)) return;
    subjectsByLength.forEach((subjectOption) => {
      const normalizedOption = normalizeSubject(subjectOption);
      if (!normalizedOption || !reqText.includes(normalizedOption)) return;
      const key = `${subjectOption}:${minLevel}`;
      if (seen.has(key)) return;
      seen.add(key);
      checks.push({ subjectName: subjectOption, minLevel });
    });
  });

  return checks;
};

const evaluateProgrammeQualification = (
  programme: UniversitySourcesAnalysisResponse['recommendedProgrammes'][number],
  selectedSubjects: StudentSubjectAchievement[],
  alternatives: string[],
): ProgrammeQualificationEvaluation => {
  const studentMap = new Map(
    selectedSubjects
      .filter((item) => item.subjectName)
      .map((item) => [normalizeSubject(item.subjectName), item.achievementLevel ?? null]),
  );
  const parsedChecks = parseProgrammeChecks(programme.admissionRequirements ?? []);
  const missingRequirements: string[] = [];
  const weakSubjects: string[] = [];
  const improvementSuggestions: string[] = [];

  parsedChecks.forEach((check) => {
    const level = studentMap.get(normalizeSubject(check.subjectName));
    if (!level) {
      missingRequirements.push(`${check.subjectName} requires Level ${check.minLevel}, but it is not selected in your report-card subjects.`);
      improvementSuggestions.push(`Add ${check.subjectName} and target at least Level ${check.minLevel}.`);
      return;
    }
    if (level < check.minLevel) {
      weakSubjects.push(`${check.subjectName}: Level ${level} (required Level ${check.minLevel})`);
      improvementSuggestions.push(`Improve ${check.subjectName} from Level ${level} to Level ${check.minLevel} or higher.`);
    }
  });

  const dedupedImprovements = Array.from(new Set(improvementSuggestions));
  const qualifies = parsedChecks.length ? (missingRequirements.length === 0 && weakSubjects.length === 0) : null;
  let qualificationMessage = 'Requirement details were not explicit enough for strict subject-level matching.';
  if (qualifies === true) {
    qualificationMessage = 'You currently meet the parsed subject-level requirements from available admission criteria.';
  } else if (qualifies === false) {
    qualificationMessage = `You do not currently qualify based on your selected subjects and levels: ${[...missingRequirements, ...weakSubjects].join(' ')}`;
  }

  return {
    programmeName: programme.name,
    university: programme.university,
    admissionRequirements: programme.admissionRequirements ?? [],
    parsedChecks,
    qualifies,
    qualificationMessage,
    missingRequirements,
    weakSubjects,
    improvementSuggestions: dedupedImprovements,
    alternatives,
  };
};

type PremiumDashboardTab = 'OVERVIEW' | 'IMPROVEMENTS';
type DashboardRecommendationItem = { id: string; title: string };

const resolveDashboardPlanType = (dashboard: StudentDashboard): PlanType => {
  const planHints = [
    dashboard.planType,
    dashboard.plan?.type,
    dashboard.plan?.tier,
    dashboard.subscriptionTier,
    dashboard.plan?.planCode,
  ];
  for (const hint of planHints) {
    if (!hint) continue;
    const normalized = hint.trim().toUpperCase();
    const withoutPrefix = normalized.startsWith('PLAN_') ? normalized.slice('PLAN_'.length) : normalized;
    if (withoutPrefix === 'PREMIUM') return 'PREMIUM';
    if (withoutPrefix === 'BASIC') return 'BASIC';
  }
  return 'BASIC';
};

const schoolPortalQuickLinks = [
  { label: 'School Portal', to: '/school-student/dashboard' },
  { label: 'Assignments', to: '/school-student/dashboard#assignments' },
  { label: 'Attendance', to: '/school-student/dashboard#dashboard' },
  { label: 'Results', to: '/school-student/dashboard#marks' },
  { label: 'Announcements', to: '/school-student/dashboard#announcements' },
  { label: 'Teacher Messages', to: '/school-student/dashboard#announcements' },
] as const;

const SchoolPortalQuickLinks = () => (
  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
    {schoolPortalQuickLinks.map((link) => (
      <Link
        key={link.label}
        to={link.to}
        className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-primary-200 hover:text-primary-700"
      >
        {link.label}
      </Link>
    ))}
  </div>
);

const BasicStudentDashboard = ({
  dashboard,
  careers,
  bursaries,
}: {
  dashboard: StudentDashboard;
  careers: DashboardRecommendationItem[];
  bursaries: DashboardRecommendationItem[];
}) => {
  const improvementTitles = dashboard.recommendedImprovements?.length
    ? dashboard.recommendedImprovements
    : (dashboard.recommendedImprovementActions ?? []).map((action) => action.title);

  return <>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      <Card label="Profile completeness" value={`${dashboard.profileCompleteness}%`} />
      <Card label="Saved careers" value={dashboard.savedCareers} />
      <Card label="Saved bursaries" value={dashboard.savedBursaries} />
      <Card label="Applications in progress" value={dashboard.activeApplications} />
      <Card label="Reward points" value={dashboard.points} />
    </div>

    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2">
      <article className="rounded border p-4">
        <h3 className="mb-2 font-semibold">Skill gaps</h3>
        {dashboard.skillGaps?.length
          ? <ul className="space-y-1 text-sm text-slate-700">{dashboard.skillGaps.map((gap) => <li key={gap}>- {gap}</li>)}</ul>
          : <p className="text-sm text-slate-500">No skill gaps identified yet.</p>}
      </article>
      <article className="rounded border p-4">
        <h3 className="mb-2 font-semibold">Recommended improvements</h3>
        {improvementTitles.length
          ? <ul className="space-y-1 text-sm text-slate-700">{improvementTitles.slice(0, 6).map((item) => <li key={item}>- {item}</li>)}</ul>
          : <p className="text-sm text-slate-500">No recommended improvements yet.</p>}
      </article>
      <article className="rounded border p-4">
        <h3 className="mb-2 font-semibold">Recommended careers</h3>
        {careers.length
          ? <ul className="space-y-1 text-sm text-slate-700">{careers.map((item) => <li key={item.id}>- {item.title}</li>)}</ul>
          : <p className="text-sm text-slate-500">No career recommendations yet.</p>}
      </article>
      <article className="rounded border p-4">
        <h3 className="mb-2 font-semibold">Recommended bursaries</h3>
        {bursaries.length
          ? <ul className="space-y-1 text-sm text-slate-700">{bursaries.map((item) => <li key={item.id}>- {item.title}</li>)}</ul>
          : <p className="text-sm text-slate-500">No bursary recommendations yet.</p>}
      </article>
    </div>
  </>;
};

const PremiumStudentDashboard = ({
  dashboard,
  careers,
  bursaries,
  activeTab,
  onTabChange,
}: {
  dashboard: StudentDashboard;
  careers: DashboardRecommendationItem[];
  bursaries: DashboardRecommendationItem[];
  activeTab: PremiumDashboardTab;
  onTabChange: (tab: PremiumDashboardTab) => void;
}) => {
  const d = dashboard;
  const objectiveSet = d.idealCareerObjectiveSet && Boolean(d.idealCareerObjective?.trim());
  const readinessCards = [
    { key: 'fieldOfStudy', label: 'Field of study readiness', data: d.readiness.fieldOfStudy },
    { key: 'bursary', label: 'Bursary readiness', data: d.readiness.bursary },
    { key: 'job', label: 'Job opportunity readiness', data: d.readiness.job },
  ];

  return <>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      <Card label="Profile completeness" value={`${d.profileCompleteness}%`} />
      <Card label="Saved careers" value={d.savedCareers} />
      <Card label="Saved bursaries" value={d.savedBursaries} />
      <Card label="Applications in progress" value={d.activeApplications} />
      <Card label="Reward points" value={d.points} />
    </div>
    <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-1">
      <button
        type="button"
        onClick={() => onTabChange('OVERVIEW')}
        className={`rounded-md px-3 py-1.5 text-sm ${activeTab === 'OVERVIEW' ? 'bg-primary-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
      >
        Overview
      </button>
      <button
        type="button"
        onClick={() => onTabChange('IMPROVEMENTS')}
        className={`rounded-md px-3 py-1.5 text-sm ${activeTab === 'IMPROVEMENTS' ? 'bg-primary-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
      >
        Recommended Improvements
      </button>
    </div>

    {activeTab === 'OVERVIEW' ? <>
      <div className="rounded border p-4">
        <h3 className="font-semibold">Ideal Career Objective</h3>
        {objectiveSet ? (
          <p className="mt-2 text-sm text-slate-700">{d.idealCareerObjective}</p>
        ) : (
          <div className="mt-2 rounded-lg border border-dashed border-slate-300 bg-slate-50 p-3 text-sm text-slate-600">
            <p>{d.idealCareerObjectiveEmptyStateMessage ?? 'Set your ideal career objective in your profile to track your progress.'}</p>
            <Link to="/student/profile" className="mt-2 inline-block font-semibold text-primary-600 hover:text-primary-500">Update profile objective</Link>
          </div>
        )}
      </div>

      <div className="rounded border p-4 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="font-semibold">Track Progress</h3>
          <Badge color={readinessPalette[d.trackProgress.status].badge}>
            {readinessPalette[d.trackProgress.status].label}
          </Badge>
        </div>
        <p className="text-sm text-slate-600">{d.trackProgress.summary}</p>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <Card label="Progress score" value={`${d.trackProgress.progressScore}%`} />
          <Card label="Saved opportunities" value={d.savedOpportunities} />
          <Card label="Unread alerts" value={d.notifications} />
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div className="rounded border p-3">
            <p className="text-xs uppercase tracking-wide text-slate-500">Top strengths</p>
            <ul className="mt-2 space-y-1 text-sm text-slate-700">{d.trackProgress.strengths.map((item) => <li key={item}>- {item}</li>)}</ul>
          </div>
          <div className="rounded border p-3">
            <p className="text-xs uppercase tracking-wide text-slate-500">Gaps to close</p>
            <ul className="mt-2 space-y-1 text-sm text-slate-700">{d.trackProgress.gaps.map((item) => <li key={item}>- {item}</li>)}</ul>
          </div>
          <div className="rounded border p-3">
            <p className="text-xs uppercase tracking-wide text-slate-500">Next milestones</p>
            <ul className="mt-2 space-y-1 text-sm text-slate-700">{d.trackProgress.nextMilestones.map((item) => <li key={item}>- {item}</li>)}</ul>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        <h3 className="font-semibold">Readiness Scorecards</h3>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {readinessCards.map((entry) => {
            const palette = readinessPalette[entry.data.status];
            return <article key={entry.key} className={`rounded border p-4 space-y-3 ${palette.panel}`}>
              <div className="flex items-center justify-between gap-2">
                <h4 className="text-sm font-semibold">{entry.label}</h4>
                <Badge color={palette.badge}>{palette.label}</Badge>
              </div>
              <p className="text-sm font-semibold">{entry.data.score}%</p>
              <p className="text-xs text-slate-700">{entry.data.explanation}</p>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-500">Top strengths</p>
                <ul className="mt-1 space-y-1 text-xs text-slate-700">{entry.data.strengths.map((item) => <li key={item}>- {item}</li>)}</ul>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-500">Missing requirements / gaps</p>
                <ul className="mt-1 space-y-1 text-xs text-slate-700">{entry.data.gaps.map((item) => <li key={item}>- {item}</li>)}</ul>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-500">Next recommended improvements</p>
                <ul className="mt-1 space-y-1 text-xs text-slate-700">{entry.data.nextImprovements.map((item) => <li key={item}>- {item}</li>)}</ul>
              </div>
            </article>;
          })}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2">
        <div className="rounded border p-3">
          <h3 className="mb-2 font-semibold">Recommended careers</h3>
          {careers.length ? <ul className="space-y-1 text-sm text-slate-700">{careers.map((item) => <li key={item.id}>- {item.title}</li>)}</ul> : <p className="text-sm text-slate-500">No career recommendations yet.</p>}
        </div>
        <div className="rounded border p-3">
          <h3 className="mb-2 font-semibold">Recommended bursaries</h3>
          {bursaries.length ? <ul className="space-y-1 text-sm text-slate-700">{bursaries.map((item) => <li key={item.id}>- {item.title}</li>)}</ul> : <p className="text-sm text-slate-500">No bursary recommendations yet.</p>}
        </div>
      </div>

      <div className="rounded border p-4 space-y-2">
        <h3 className="font-semibold">{d.communicationGuidance.title}</h3>
        <p className="text-sm text-slate-600">{d.communicationGuidance.description}</p>
        <ul className="space-y-1 text-sm text-slate-700">{d.communicationGuidance.tips.map((tip) => <li key={tip}>- {tip}</li>)}</ul>
      </div>
    </> : (
      <div className="space-y-3">
        <p className="text-sm text-slate-600">These actions are personalised to the current readiness gaps in your profile.</p>
        {!d.recommendedImprovementActions?.length ? (
          <p className="text-sm text-slate-500">No improvement actions available right now.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
            {d.recommendedImprovementActions.map((action) => <article key={action.id} className="rounded border p-4 bg-white space-y-2">
              <div className="flex items-center justify-between gap-2">
                <h4 className="font-semibold">{action.title}</h4>
                <Badge color={action.priority === 'HIGH' ? 'amber' : action.priority === 'MEDIUM' ? 'blue' : 'slate'}>{action.priority}</Badge>
              </div>
              <p className="text-sm text-slate-600">{action.reason}</p>
              <p className="text-xs text-slate-500">Impact area: {action.impactArea}</p>
            </article>)}
          </div>
        )}
      </div>
    )}

  </>;
};

export const StudentDashboardPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const dashboard = useAppQuery<StudentDashboard>({ queryKey: ['dash'], queryFn: studentService.getDashboard });
  const recs = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  const progress = useAppQuery({ queryKey: ['progress-score'], queryFn: featureModulesService.progressScore });
  const mySchoolStatus = useAppQuery({ queryKey: ['student', 'my-school', 'status'], queryFn: schoolService.getMySchoolStatus, staleTime: 60_000 });
  const [activeTab, setActiveTab] = useState<PremiumDashboardTab>('OVERVIEW');

  if (dashboard.isLoading) return <LoadingState />;
  if (dashboard.isError) return <ErrorState message="Could not load dashboard. Please refresh and try again." />;
  const d = dashboard.data;
  if (!d) return <ErrorState message="Could not load dashboard details. Please refresh and try again." />;

  const careers = (recs.data?.suggestedCareers?.slice(0, 3) ?? []).map((item) => ({ id: item.id, title: item.title }));
  const bursaries = (recs.data?.suggestedBursaries?.slice(0, 3) ?? []).map((item) => ({ id: item.id, title: item.title }));
  const planType = resolveDashboardPlanType(d);
  const firstName = user?.fullName?.split(/\s+/)[0] || 'Student';
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const progressValue = Math.min(100, Math.max(0, d.profileCompleteness));
  const streak = Number((d as StudentDashboard & { currentStreak?: number; streak?: number }).currentStreak ?? (d as StudentDashboard & { streak?: number }).streak ?? 0);
  const schoolStatus = mySchoolStatus.data?.status ?? 'NONE';
  const pendingDeadlines = [
    `${d.activeApplications} active applications`,
    `${d.notifications} unread notifications`,
  ];
  const profileRecommendations = d.recommendedImprovements?.length
    ? d.recommendedImprovements.slice(0, 4)
    : (d.recommendedImprovementActions ?? []).slice(0, 4).map((item) => item.title);

  return <Section title="Student Dashboard" description="Track your profile, recommendations, applications, and rewards from one workspace.">
    <div className="student-hero-card">
      <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="pointer-events-none absolute -right-8 -top-10 hidden h-44 w-44 rounded-full bg-gradient-to-br from-blue-100 to-orange-100 blur-2xl md:block" />
        <div className="relative min-w-0">
          <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-primary-100 bg-primary-50 px-3 py-1 text-xs font-semibold text-primary-700">
            <Sparkles size={14} />
            {planType} workspace
          </div>
          <h2 className="text-2xl font-bold tracking-normal text-slate-950 md:text-3xl">{greeting}, {firstName}</h2>
          <p className="text-xl font-semibold text-slate-900 md:text-2xl">Welcome back!</p>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Your profile is {d.profileCompleteness}% complete with {d.points} reward points and {d.notifications} unread alerts.</p>
          <div className="mt-4 flex flex-wrap gap-2">
            <Link to="/student/profile" className="rounded-2xl bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:brightness-110">Continue Your Journey</Link>
            <Link to="/student/recommendations/careers" className="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50">Ask AI Assistant</Link>
          </div>
        </div>
        <div className="relative grid min-w-0 gap-3 sm:grid-cols-3 lg:w-[560px]">
          <div className="flex items-center gap-3 rounded-[24px] border border-blue-100 bg-gradient-to-br from-blue-50 to-white p-4">
            <div className="relative grid h-16 w-16 place-items-center rounded-full bg-white shadow-sm">
              <div className="absolute inset-0 rounded-full" style={{ background: `conic-gradient(#1E8BFF ${progressValue}%, #E2E8F0 0)` }} />
              <div className="absolute inset-[5px] rounded-full bg-white" />
              <span className="relative text-xs font-bold text-slate-900">{progressValue}%</span>
            </div>
            <div>
              <p className="text-xs font-medium uppercase text-slate-500">Profile completion</p>
              <p className="text-base font-semibold text-slate-900">Progress</p>
            </div>
          </div>
          <div className="rounded-[24px] border border-orange-100 bg-gradient-to-br from-orange-50 to-white p-4">
            <p className="text-xs font-medium uppercase text-slate-500">Reward points</p>
            <p className="mt-2 text-2xl font-bold text-slate-950">{d.points}</p>
            <p className="mt-1 text-xs text-slate-500">Keep learning to earn more</p>
          </div>
          <div className="rounded-[24px] border border-slate-200 bg-gradient-to-br from-slate-50 to-white p-4">
            <p className="text-xs font-medium uppercase text-slate-500">Current streak</p>
            <p className="mt-2 text-2xl font-bold text-slate-950">{streak} days</p>
            <p className="mt-1 text-xs text-slate-500">Stay consistent</p>
          </div>
        </div>
      </div>
    </div>
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div className="student-soft-card bg-gradient-to-br from-blue-50 to-white">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Profile Completeness</p>
        <div className="mt-2 flex items-center justify-between"><p className="text-3xl font-bold text-slate-950">{d.profileCompleteness}%</p><UserCircle2 size={20} className="text-blue-500" /></div>
      </div>
      <div className="student-soft-card bg-gradient-to-br from-indigo-50 to-white">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Applications</p>
        <div className="mt-2 flex items-center justify-between"><p className="text-3xl font-bold text-slate-950">{d.savedBursaries}</p><GraduationCap size={20} className="text-indigo-500" /></div>
      </div>
      <div className="student-soft-card bg-gradient-to-br from-cyan-50 to-white">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Applications in Progress</p>
        <div className="mt-2 flex items-center justify-between"><p className="text-3xl font-bold text-slate-950">{d.activeApplications}</p><Clock3 size={20} className="text-cyan-600" /></div>
      </div>
      <div className="student-soft-card bg-gradient-to-br from-orange-50 to-white">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Reward Points</p>
        <div className="mt-2 flex items-center justify-between"><p className="text-3xl font-bold text-slate-950">{d.points}</p><CircleDollarSign size={20} className="text-orange-500" /></div>
      </div>
    </div>
    <article id="my-school-section" className="student-soft-card scroll-mt-28">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900">My School</h3>
          <p className="mt-1 text-sm text-slate-600">View your school link status and open the full My School workspace without leaving your student dashboard.</p>
        </div>
        <Badge color={schoolStatusBadgeColor(schoolStatus)}>{schoolStatusLabel(schoolStatus)}</Badge>
      </div>
      <div className="mt-4 space-y-3 text-sm text-slate-600">
        {mySchoolStatus.isLoading ? <p>Checking school link status...</p> : null}
        {!mySchoolStatus.isLoading && schoolStatus === 'NONE' ? <p>No school is linked to your AI Career Guidance account yet.</p> : null}
        {schoolStatus === 'PENDING' ? (
          <>
            <p>{mySchoolStatus.data?.message ?? 'Your school request is pending approval.'}</p>
            <p className="text-xs text-slate-500">Requested school: {mySchoolStatus.data?.school?.name ?? 'Selected school'}</p>
          </>
        ) : null}
        {schoolStatus === 'APPROVED' ? (
          <>
            <p className="text-slate-700">{mySchoolStatus.data?.message ?? 'Your school link is active.'}</p>
            <div className="grid gap-3 md:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">School</p>
                <p className="mt-1 font-semibold text-slate-900">{mySchoolStatus.data?.school?.name ?? 'Linked school'}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">School Username</p>
                <p className="mt-1 font-semibold text-slate-900">{mySchoolStatus.data?.generatedUsername ?? 'Pending generation'}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 md:col-span-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Grade / Class</p>
                <p className="mt-1 font-semibold text-slate-900">{schoolGradeClassLabel(mySchoolStatus.data)}</p>
              </div>
            </div>
          </>
        ) : null}
        {schoolStatus === 'REJECTED' ? <p>{mySchoolStatus.data?.message ?? 'Your previous school request was rejected.'}</p> : null}
        <div className="flex flex-wrap gap-2">
          <Button type="button" className="h-10 rounded-2xl bg-[#0B5BFF] px-4 hover:bg-[#0849cb]" onClick={() => navigate('/student/my-school')}>
            Open My School
          </Button>
          {schoolStatus === 'APPROVED' ? (
            <Button type="button" className="h-10 rounded-2xl bg-slate-900 px-4 hover:bg-slate-800" onClick={() => navigate('/school-student/dashboard')}>
              School Portal
            </Button>
          ) : null}
        </div>
      </div>
    </article>
    <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">AI Recommendations for You</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          {profileRecommendations.length ? profileRecommendations.map((item) => <p key={item}>- {item}</p>) : <p>No recommendations yet. Complete more profile details for personalized AI suggestions.</p>}
        </div>
      </article>
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">Top Career Matches</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          {careers.length ? careers.map((item) => <p key={item.id}>- {item.title}</p>) : <p>No career matches available yet.</p>}
        </div>
      </article>
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">Upcoming Deadlines</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          {pendingDeadlines.map((item) => <p key={item}>- {item}</p>)}
        </div>
      </article>
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">Recent AI Tutor Sessions</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          <p>- Continue tutoring to build your study streak.</p>
          <Link to="/student/ai-tutor" className="inline-flex font-semibold text-primary-700 hover:text-primary-600">Open AI Tutor</Link>
        </div>
      </article>
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">Trending Bursaries</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          {bursaries.length ? bursaries.map((item) => <p key={item.id}>- {item.title}</p>) : <p>No trending bursaries available yet.</p>}
        </div>
      </article>
      <article className="student-soft-card">
        <h3 className="text-base font-semibold text-slate-900">Learning Progress</h3>
        <div className="mt-3 space-y-2 text-sm text-slate-600">
          <p>- Overall score: {progress.data?.overallPercentage ?? 0}%</p>
          <p>- Saved opportunities: {d.savedOpportunities}</p>
          <p>- Notifications: {d.notifications}</p>
        </div>
      </article>
    </div>
    {planType === 'PREMIUM' ? (
      <PremiumStudentDashboard
        dashboard={d}
        careers={careers}
        bursaries={bursaries}
        activeTab={activeTab}
        onTabChange={setActiveTab}
      />
    ) : (
      <BasicStudentDashboard
        dashboard={d}
        careers={careers}
        bursaries={bursaries}
      />
    )}
    {d.plan ? (
      <div className="rounded border border-primary-200 bg-primary-50 p-4 text-sm text-primary-900">
        <p className="font-semibold">Current plan: {d.plan.tier}</p>
        <p className="mt-1">{d.plan.premium ? 'Premium features are unlocked for your account.' : d.plan.upgradeMessage}</p>
      </div>
    ) : null}
    {progress.data ? (
      <div className="space-y-3 rounded border bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="font-semibold">Platform Progress Score</h3>
          <Badge color={progress.data.overallColor === 'green' ? 'emerald' : progress.data.overallColor === 'orange' ? 'amber' : 'slate'}>{progress.data.overallPercentage}%</Badge>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {progress.data.cards.map((card) => (
            <div key={card.key} className="rounded border p-3">
              <p className="text-xs text-slate-500">{card.label}</p>
              <p className="text-2xl font-semibold">{card.percentage}%</p>
              <Badge color={card.color === 'green' ? 'emerald' : card.color === 'orange' ? 'amber' : 'slate'}>{card.color}</Badge>
            </div>
          ))}
        </div>
        <div className="text-sm text-slate-600">
          {progress.data.recommendations.map((item) => <p key={item}>- {item}</p>)}
        </div>
      </div>
    ) : null}
  </Section>;
};

export const StudentMySchoolPage = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSchoolId, setSelectedSchoolId] = useState('');
  const mySchoolStatus = useAppQuery({ queryKey: ['student', 'my-school', 'status'], queryFn: schoolService.getMySchoolStatus, staleTime: 60_000 });
  const publicSchools = useAppQuery({
    queryKey: ['public', 'schools'],
    queryFn: schoolService.listPublicSchools,
    staleTime: 5 * 60_000,
  });
  const requestMySchoolJoin = useMutation({
    mutationFn: (schoolId: string) => schoolService.requestMySchoolJoin(schoolId),
    onSuccess: async () => {
      setSelectedSchoolId('');
      setSearchTerm('');
      await queryClient.invalidateQueries({ queryKey: ['student', 'my-school'] });
    },
  });

  const schoolStatus = mySchoolStatus.data?.status ?? 'NONE';
  const schools = publicSchools.data ?? [];
  const filteredSchools = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) return schools;
    return schools.filter((school) => `${school.name} ${school.schoolCode}`.toLowerCase().includes(query));
  }, [schools, searchTerm]);
  const selectedSchool = filteredSchools.find((school) => school.id === selectedSchoolId)
    ?? schools.find((school) => school.id === selectedSchoolId)
    ?? null;
  const canSubmitJoinRequest = schoolStatus === 'NONE' || schoolStatus === 'REJECTED';

  return (
    <Section
      title="My School"
      description="Link your EduRite learner account to your registered school and access your school resources."
    >
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
        <article className="student-soft-card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">School Link Status</p>
              <h2 className="mt-2 text-2xl font-bold tracking-normal text-slate-950">My School</h2>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
                Manage your school connection from one place and open school resources without leaving your main EduRite learner account.
              </p>
            </div>
            <Badge color={schoolStatusBadgeColor(schoolStatus)}>{schoolStatusLabel(schoolStatus)}</Badge>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">School</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{mySchoolStatus.data?.school?.name ?? 'No school linked'}</p>
            </div>
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">School Code</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{mySchoolStatus.data?.school?.schoolCode ?? 'Not assigned'}</p>
            </div>
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Grade / Class</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{schoolGradeClassLabel(mySchoolStatus.data)}</p>
            </div>
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Admin Approval</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{schoolStatus === 'APPROVED' ? 'Connected / Approved' : schoolStatus === 'PENDING' ? 'Pending admin approval' : schoolStatus === 'REJECTED' ? 'Rejected' : 'Awaiting request'}</p>
            </div>
          </div>

          <div className="mt-6 space-y-3 rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-sm text-slate-600">
            {mySchoolStatus.isLoading ? <p>Checking your school link status...</p> : null}
            {!mySchoolStatus.isLoading && schoolStatus === 'NONE' ? <p>No school linked. Select your school below to request access.</p> : null}
            {schoolStatus === 'PENDING' ? (
              <p>{mySchoolStatus.data?.message ?? 'Your request has been sent to the school admin. You will be notified once approved.'}</p>
            ) : null}
            {schoolStatus === 'APPROVED' ? (
              <>
                <p>{mySchoolStatus.data?.message ?? 'Your school access is active and ready to use.'}</p>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">School Username</p>
                    <p className="mt-1 font-semibold text-slate-900">{mySchoolStatus.data?.generatedUsername ?? 'Pending generation'}</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Grade / Class</p>
                    <p className="mt-1 font-semibold text-slate-900">{schoolGradeClassLabel(mySchoolStatus.data)}</p>
                  </div>
                </div>
              </>
            ) : null}
            {schoolStatus === 'REJECTED' ? (
              <p>{mySchoolStatus.data?.message ?? 'Your request was rejected. You can select another school and submit a new request.'}</p>
            ) : null}
          </div>
        </article>

        <article className="student-soft-card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Join School</p>
              <h3 className="mt-2 text-xl font-semibold text-slate-950">Join My School</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Search for a registered school, select it from the list, and send a join request to the school admin.
              </p>
            </div>
            {canSubmitJoinRequest ? null : (
              <Badge color={schoolStatusBadgeColor(schoolStatus)}>{schoolStatusLabel(schoolStatus)}</Badge>
            )}
          </div>

          <div className="mt-6 space-y-4">
            <label className="block text-sm font-medium text-slate-700">
              Search School
              <Input
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                className="mt-2 h-11 rounded-2xl"
                placeholder="Search by school name or code"
                disabled={!canSubmitJoinRequest}
              />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Registered School
              <select
                value={selectedSchoolId}
                onChange={(event) => setSelectedSchoolId(event.target.value)}
                className="mt-2 h-11 w-full rounded-2xl border border-slate-200 bg-white px-3 text-sm text-slate-800"
                disabled={!canSubmitJoinRequest || publicSchools.isLoading}
              >
                <option value="">Select your school</option>
                {filteredSchools.map((school) => (
                  <option key={school.id} value={school.id}>
                    {school.name}
                  </option>
                ))}
              </select>
            </label>

            {selectedSchool ? (
              <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
                <p className="font-semibold text-slate-900">{selectedSchool.name}</p>
                <p className="mt-1">School code: {selectedSchool.schoolCode}</p>
              </div>
            ) : null}

            {publicSchools.isLoading ? <p className="text-sm text-slate-500">Loading registered schools...</p> : null}
            {publicSchools.isError ? <p className="text-sm text-rose-600">Could not load registered schools right now.</p> : null}
            {requestMySchoolJoin.isError ? <p className="text-sm text-rose-600">{requestMySchoolJoin.error.message}</p> : null}

            {schoolStatus === 'PENDING' ? (
              <div className="rounded-[24px] border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900">
                Your request has been sent to the school admin. You will be notified once approved.
              </div>
            ) : null}

            <div className="flex flex-wrap justify-end gap-2">
              <Button
                type="button"
                className="h-11 rounded-2xl bg-slate-100 text-slate-900 hover:bg-slate-200"
                disabled={!canSubmitJoinRequest}
                onClick={() => {
                  setSelectedSchoolId('');
                  setSearchTerm('');
                }}
              >
                Reset
              </Button>
              <Button
                type="button"
                className="h-11 rounded-2xl bg-[#0B5BFF] px-4 hover:bg-[#0849cb]"
                disabled={!canSubmitJoinRequest || !selectedSchoolId || requestMySchoolJoin.isPending}
                onClick={() => requestMySchoolJoin.mutate(selectedSchoolId)}
              >
                {requestMySchoolJoin.isPending ? 'Sending...' : 'Ask to Join'}
              </Button>
            </div>
          </div>
        </article>
      </div>

      {schoolStatus === 'APPROVED' ? (
        <article className="student-soft-card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="text-xl font-semibold text-slate-950">My School Portal</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Open school-linked resources from your current EduRite learner session.
              </p>
            </div>
            <Button type="button" className="h-11 rounded-2xl bg-slate-900 px-4 hover:bg-slate-800" onClick={() => navigate('/school-student/dashboard')}>
              Open School Portal
            </Button>
          </div>
          <div className="mt-5">
            <SchoolPortalQuickLinks />
          </div>
        </article>
      ) : null}
    </Section>
  );
};
export const StudentProfilePage = () => {
  const qc = useQueryClient();
  const { user, syncStudentProfileState } = useAuth();
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const recommendations = useAppQuery({
    queryKey: ['profile-ai-recommendations-fallback'],
    queryFn: recommendationService.mine,
    enabled: Boolean(user?.id),
    staleTime: 5 * 60_000,
  });
  const savedProfiles = useAppQuery({ queryKey: ['me', 'saved-profiles'], queryFn: studentService.listSavedProfiles });
  const [form, setForm] = useState<Record<string, string>>({});
  const [subjectRows, setSubjectRows] = useState<StudentSubjectAchievement[]>([{ subjectName: '', achievementLevel: null }]);
  const [profileVersionName, setProfileVersionName] = useState('');
  const [selectedCvName, setSelectedCvName] = useState('');
  const [selectedTranscriptName, setSelectedTranscriptName] = useState('');
  const [guidanceRefresh, setGuidanceRefresh] = useState(0);

  const invalidateProfileDerivedQueries = () => {
    qc.invalidateQueries({ queryKey: ['me'] });
    qc.invalidateQueries({ queryKey: ['dash'] });
    qc.invalidateQueries({ queryKey: ['student-aps-profile'] });
    qc.invalidateQueries({ queryKey: ['student-career-roadmaps-saved'] });
    qc.invalidateQueries({ queryKey: ['profile-ai-readiness'] });
    qc.invalidateQueries({ queryKey: ['ai-guidance-university-sources'] });
    qc.invalidateQueries({ queryKey: ['career-roadmap-career-match'] });
  };

  const toFormState = (source?: StudentProfile | null) => ({
    firstName: source?.firstName ?? '',
    lastName: source?.lastName ?? '',
    phone: source?.phone ?? '',
    dateOfBirth: source?.dateOfBirth ?? '',
    gender: source?.gender ?? '',
    location: source?.location ?? '',
    bio: source?.bio ?? '',
    qualificationLevel: source?.qualificationLevel ?? '',
    selectedGrade: source?.selectedGrade ?? '',
    careerGoals: source?.careerGoals ?? '',
    qualifications: (source?.qualifications ?? []).join(', '),
    experience: (source?.experience ?? []).join(', '),
    skills: (source?.skills ?? []).join(', '),
    interests: (source?.interests ?? []).join(', '),
  });

  const toSubjectRows = (source?: StudentProfile | null) => {
    const rows = (source?.subjectAchievements ?? []).map((item) => toSubjectRow(item));
    return rows.length ? rows : [{ subjectName: '', achievementLevel: null }];
  };

  useEffect(() => {
    if (!profile.data) return;
    setForm(toFormState(profile.data));
    setSubjectRows(toSubjectRows(profile.data));
  }, [profile.data]);

  const value = (key: string) => form[key] ?? '';
  const parseList = (input: string) => input.split(',').map((item) => item.trim()).filter(Boolean);
  const normalizedSubjects = useMemo(
    () => subjectRows
      .filter((item) => item.subjectName.trim())
      .map((item) => ({ subjectName: item.subjectName.trim(), achievementLevel: item.achievementLevel ?? null })),
    [subjectRows],
  );

  const buildSavedPayload = (): StudentSavedProfilePayload => ({
    firstName: value('firstName') || undefined,
    lastName: value('lastName') || undefined,
    phone: value('phone') || undefined,
    dateOfBirth: value('dateOfBirth') || undefined,
    gender: value('gender') || undefined,
    location: value('location') || undefined,
    bio: value('bio') || undefined,
    qualificationLevel: value('qualificationLevel') || undefined,
    selectedGrade: value('selectedGrade') || undefined,
    subjectAchievements: normalizedSubjects,
    qualifications: parseList(value('qualifications')),
    experience: parseList(value('experience')),
    skills: parseList(value('skills')),
    interests: parseList(value('interests')),
    careerGoals: value('careerGoals') || undefined,
  });

  const update = useMutation({
    mutationFn: () => studentService.updateMe({
      ...form,
      dateOfBirth: form.dateOfBirth || undefined,
      selectedGrade: form.selectedGrade || undefined,
      subjectAchievements: normalizedSubjects,
      qualifications: parseList(form.qualifications ?? ''),
      experience: parseList(form.experience ?? ''),
      skills: parseList(form.skills ?? ''),
      interests: parseList(form.interests ?? ''),
    }),
    onSuccess: (updatedProfile) => {
      setForm(toFormState(updatedProfile));
      setSubjectRows(toSubjectRows(updatedProfile));
      syncStudentProfileState({
        profileCompleted: updatedProfile.profileCompleted,
        profileCompleteness: updatedProfile.profileCompleteness,
      });
      invalidateProfileDerivedQueries();
      gamificationService.completeTask('PROFILE_UPDATE').catch(() => undefined);
    },
  });

  const cvUpload = useMutation({
    mutationFn: (file: File) => studentService.uploadCv(file),
    onSuccess: (updatedProfile) => {
      syncStudentProfileState({
        profileCompleted: updatedProfile.profileCompleted,
        profileCompleteness: updatedProfile.profileCompleteness,
      });
      invalidateProfileDerivedQueries();
    },
  });

  const transcriptUpload = useMutation({
    mutationFn: (file: File) => studentService.uploadTranscript(file),
    onSuccess: (updatedProfile) => {
      syncStudentProfileState({
        profileCompleted: updatedProfile.profileCompleted,
        profileCompleteness: updatedProfile.profileCompleteness,
      });
      invalidateProfileDerivedQueries();
    },
  });

  const saveProfileVersion = useMutation({
    mutationFn: () => studentService.saveProfileVersion(profileVersionName.trim(), buildSavedPayload()),
    onSuccess: () => {
      setProfileVersionName('');
      qc.invalidateQueries({ queryKey: ['me', 'saved-profiles'] });
    },
  });

  const applySavedProfile = useMutation({
    mutationFn: (savedProfileId: string) => studentService.applySavedProfile(savedProfileId),
    onSuccess: (updatedProfile) => {
      setForm(toFormState(updatedProfile));
      setSubjectRows(toSubjectRows(updatedProfile));
      syncStudentProfileState({
        profileCompleted: updatedProfile.profileCompleted,
        profileCompleteness: updatedProfile.profileCompleteness,
      });
      invalidateProfileDerivedQueries();
    },
  });

  const deleteSavedProfile = useMutation({
    mutationFn: (savedProfileId: string) => studentService.deleteSavedProfile(savedProfileId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me', 'saved-profiles'] }),
  });

  const savedProfileDetails = useAppQuery({
    queryKey: ['me', 'saved-profiles', 'details', (savedProfiles.data ?? []).map((item) => item.id).join(',')],
    enabled: Boolean(savedProfiles.data?.length),
    queryFn: async () => Promise.all((savedProfiles.data ?? []).map((saved) => studentService.getSavedProfile(saved.id))),
  });

  const selectedGrade = value('selectedGrade');
  const currentSubjectOptions = useMemo(
    () => (isSeniorPhaseGrade(selectedGrade) ? SENIOR_PHASE_SUBJECT_OPTIONS : FET_SUBJECT_OPTIONS),
    [selectedGrade],
  );
  const subjectSelectionHeading = isFetGrade(selectedGrade) ? 'FET Subject Selection' : 'Subject Selection';
  const subjectPlaceholder = isFetGrade(selectedGrade) ? 'Select FET subject' : 'Select subject';
  const interests = parseList(value('interests')).join(', ');
  const targetProgram = value('careerGoals').trim() || interests;
  const qualificationLevel = value('qualificationLevel').trim();

  const profileReadinessMessage = !selectedGrade || normalizedSubjects.length === 0
    ? 'Select your grade and at least one report-card subject with achievement level to generate qualification readiness.'
    : !qualificationLevel || !targetProgram
      ? 'Complete qualification level and career goals/interests to generate AI guidance.'
      : null;

  const isPremiumStudent = (user?.planType ?? 'BASIC').toUpperCase() === 'PREMIUM';

  useEffect(() => {
    if (!selectedGrade) return;
    const allowedSubjects = new Set<string>(currentSubjectOptions);
    setSubjectRows((rows) => {
      const normalizedRows = rows.map((row) => (allowedSubjects.has(row.subjectName) || !row.subjectName
        ? row
        : { ...row, subjectName: '', achievementLevel: null }));
      return normalizedRows.length ? normalizedRows : [{ subjectName: '', achievementLevel: null }];
    });
  }, [selectedGrade, currentSubjectOptions]);

  useEffect(() => {
    if (isPremiumStudent && !profileReadinessMessage && guidanceRefresh === 0 && !profile.isLoading) {
      setGuidanceRefresh(1);
    }
  }, [isPremiumStudent, profileReadinessMessage, guidanceRefresh, profile.isLoading]);

  const aiPreview = useAppQuery({
    queryKey: ['profile-ai-readiness', guidanceRefresh, profile.data?.id, selectedGrade, qualificationLevel, targetProgram, interests, normalizedSubjects.map((item) => `${item.subjectName}:${item.achievementLevel ?? ''}`).join('|')],
    enabled: isPremiumStudent && guidanceRefresh > 0 && !profileReadinessMessage,
    queryFn: () => aiGuidanceService.analyseUniversitySources({
      urls: [],
      targetProgram,
      careerInterest: interests,
      qualificationLevel: `${qualificationLevel} | ${selectedGrade}`,
      maxRecommendations: 6,
    }),
    retry: false,
    staleTime: 10 * 60_000,
    gcTime: 20 * 60_000,
  });

  const qualificationEvaluations = useMemo<ProgrammeQualificationEvaluation[]>(() => {
    if (!aiPreview.data?.recommendedProgrammes?.length) return [];
    const alternatives = (aiPreview.data.recommendedCareers ?? []).map((career) => career.name).filter(Boolean).slice(0, 5);
    return aiPreview.data.recommendedProgrammes.slice(0, 6).map((programme) => evaluateProgrammeQualification(programme, normalizedSubjects, alternatives));
  }, [aiPreview.data, normalizedSubjects]);
  const fallbackCareerRecommendations = recommendations.data?.suggestedCareers?.slice(0, 5) ?? [];
  const aiStatus = aiPreview.data?.status;
  const aiWarningMessage = aiPreview.data?.warningMessage ?? null;

  const evaluatedProgrammes = qualificationEvaluations.filter((item) => item.qualifies !== null);
  const qualifiesCount = evaluatedProgrammes.filter((item) => item.qualifies).length;
  const missingRequirementsCount = qualificationEvaluations.reduce((count, item) => count + item.missingRequirements.length + item.weakSubjects.length, 0);

  const profileCompleteness = profile.data?.profileCompleteness ?? 0;
  const averageAchievementLevel = normalizedSubjects.length
    ? normalizedSubjects.reduce((sum, item) => sum + (item.achievementLevel ?? 0), 0) / normalizedSubjects.length
    : 0;
  const academicReadinessScore = Math.min(
    100,
    (selectedGrade ? 25 : 0)
      + Math.min(35, normalizedSubjects.length * 5)
      + Math.round((averageAchievementLevel / 7) * 40),
  );
  const qualificationReadinessScore = evaluatedProgrammes.length
    ? Math.round((qualifiesCount / evaluatedProgrammes.length) * 100)
    : 0;
  const documentCompletenessScore = Math.round(((profile.data?.cvFileUrl ? 1 : 0) + (profile.data?.transcriptFileUrl ? 1 : 0)) * 50);

  const savedProfileDetailById = useMemo(() => new Map((savedProfileDetails.data ?? []).map((item) => [item.id, item])), [savedProfileDetails.data]);
  const setSubjectField = (index: number, patch: Partial<StudentSubjectAchievement>) => {
    setSubjectRows((current) => current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };
  const addSubjectRow = () => setSubjectRows((current) => [...current, { subjectName: '', achievementLevel: null }]);
  const removeSubjectRow = (index: number) => {
    setSubjectRows((current) => {
      const next = current.filter((_, rowIndex) => rowIndex !== index);
      return next.length ? next : [{ subjectName: '', achievementLevel: null }];
    });
  };

  const p = profile.data;
  if (profile.isLoading) return <LoadingState />;
  if (profile.isError) return <ErrorState message="Could not load your profile. Please refresh and try again." />;

  const initials = getInitials(value('firstName'), value('lastName'));

  return <Section title="My Profile">
    <article className="overflow-hidden rounded-2xl border border-primary-100 bg-gradient-to-r from-white via-primary-50/40 to-blue-50/50 shadow-sm transition">
      <div className="flex flex-col gap-4 p-5 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary-100 text-xl font-bold text-primary-700">{initials}</div>
          <div className="space-y-1">
            <p className="text-xs font-semibold uppercase tracking-wide text-primary-700">Student profile summary</p>
            <h2 className="text-2xl font-semibold text-slate-900">{`${value('firstName')} ${value('lastName')}`.trim() || 'Your Profile'}</h2>
            <p className="text-sm text-slate-600">{value('selectedGrade') || 'Grade not selected yet'}{value('qualificationLevel') ? ` â€¢ ${value('qualificationLevel')}` : ''}</p>
          </div>
        </div>
        <div className="rounded-xl border border-primary-100 bg-white/90 p-3 text-sm shadow-sm">
          <p className="font-semibold text-slate-800">Profile completion</p>
          <p className="text-xs text-slate-500">{profileCompleteness}% complete</p>
          <div className="mt-2 h-2 w-56 overflow-hidden rounded-full bg-slate-200">
            <div className="h-full rounded-full bg-primary-600 transition-all duration-500" style={{ width: `${profileCompleteness}%` }} />
          </div>
        </div>
      </div>
    </article>

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <div className="mb-4 flex items-center gap-2">
        <UserCircle2 className="h-5 w-5 text-primary-600" />
        <h3 className="text-lg font-semibold text-slate-900">Personal Information</h3>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
        <label className="space-y-1 text-sm font-medium text-slate-700">First name<Input className="transition focus:shadow-sm" placeholder="First name" value={value('firstName')} onChange={(e) => setForm((s) => ({ ...s, firstName: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Last name<Input className="transition focus:shadow-sm" placeholder="Last name" value={value('lastName')} onChange={(e) => setForm((s) => ({ ...s, lastName: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Phone<Input className="transition focus:shadow-sm" placeholder="Phone" value={value('phone')} onChange={(e) => setForm((s) => ({ ...s, phone: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Date of birth<Input className="transition focus:shadow-sm" type="date" placeholder="Date of birth" value={value('dateOfBirth')} onChange={(e) => setForm((s) => ({ ...s, dateOfBirth: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Gender<Input className="transition focus:shadow-sm" placeholder="Gender" value={value('gender')} onChange={(e) => setForm((s) => ({ ...s, gender: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Location<Input className="transition focus:shadow-sm" placeholder="Location" value={value('location')} onChange={(e) => setForm((s) => ({ ...s, location: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700 sm:col-span-2">Bio<Input className="transition focus:shadow-sm" placeholder="Bio" value={value('bio')} onChange={(e) => setForm((s) => ({ ...s, bio: e.target.value }))} /></label>
      </div>
    </article>

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <div className="mb-4 flex items-center gap-2">
        <GraduationCap className="h-5 w-5 text-primary-600" />
        <h3 className="text-lg font-semibold text-slate-900">Academic Information</h3>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
        <label className="space-y-1 text-sm font-medium text-slate-700">Grade
          <select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm transition outline-none focus:ring-2 focus:ring-primary-500" value={value('selectedGrade')} onChange={(e) => setForm((s) => ({ ...s, selectedGrade: e.target.value }))}>
            <option value="">Select grade</option>
            {STUDENT_GRADES.map((grade) => <option key={grade} value={grade}>{grade}</option>)}
          </select>
        </label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Qualification level<Input className="transition focus:shadow-sm" placeholder="Qualification level" value={value('qualificationLevel')} onChange={(e) => setForm((s) => ({ ...s, qualificationLevel: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700 sm:col-span-2">Career goals<Input className="transition focus:shadow-sm" placeholder="Career goals" value={value('careerGoals')} onChange={(e) => setForm((s) => ({ ...s, careerGoals: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Qualifications (comma separated)<Input className="transition focus:shadow-sm" placeholder="Qualifications (comma separated)" value={value('qualifications')} onChange={(e) => setForm((s) => ({ ...s, qualifications: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Experience (comma separated)<Input className="transition focus:shadow-sm" placeholder="Experience (comma separated)" value={value('experience')} onChange={(e) => setForm((s) => ({ ...s, experience: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Skills (comma separated)<Input className="transition focus:shadow-sm" placeholder="Skills (comma separated)" value={value('skills')} onChange={(e) => setForm((s) => ({ ...s, skills: e.target.value }))} /></label>
        <label className="space-y-1 text-sm font-medium text-slate-700">Interests (comma separated)<Input className="transition focus:shadow-sm" placeholder="Interests (comma separated)" value={value('interests')} onChange={(e) => setForm((s) => ({ ...s, interests: e.target.value }))} /></label>
      </div>
    </article>

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <div className="mb-3 flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">{subjectSelectionHeading}</h3>
          <p className="text-sm text-slate-600">Select subjects from your report card and capture achievement levels.</p>
        </div>
        <Button type="button" className="bg-slate-800 hover:bg-slate-700" onClick={addSubjectRow}>Add subject</Button>
      </div>
      <div className="space-y-3">
        {subjectRows.map((row, index) => {
          const selectedElsewhere = new Set(subjectRows.filter((_, rowIndex) => rowIndex !== index).map((item) => item.subjectName));
          return <div key={`${index}-${row.subjectName || 'empty'}`} className="grid gap-2 rounded-xl border border-slate-200 bg-slate-50/70 p-3 md:grid-cols-[1fr_220px_auto] md:items-end">
            <label className="space-y-1 text-sm font-medium text-slate-700">Subject
              <select className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:ring-2 focus:ring-primary-500" value={row.subjectName} onChange={(e) => setSubjectField(index, { subjectName: e.target.value })}>
                <option value="">{subjectPlaceholder}</option>
                {currentSubjectOptions.map((subject) => <option key={subject} value={subject} disabled={selectedElsewhere.has(subject)}>{subject}</option>)}
              </select>
            </label>
            <label className="space-y-1 text-sm font-medium text-slate-700">Achievement level
              <select className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition focus:ring-2 focus:ring-primary-500" value={row.achievementLevel ?? ''} onChange={(e) => setSubjectField(index, { achievementLevel: e.target.value ? Number(e.target.value) : null })}>
                <option value="">Select level</option>
                {ACHIEVEMENT_LEVELS.map((level) => <option key={level.value} value={level.value}>{level.label}</option>)}
              </select>
            </label>
            <Button type="button" className="bg-rose-600 hover:bg-rose-500" onClick={() => removeSubjectRow(index)} disabled={subjectRows.length === 1}>Remove</Button>
          </div>;
        })}
      </div>
      <p className="mt-2 text-xs text-slate-500">Achievement levels follow the NSC scale from Level 1 to Level 7.</p>
    </article>

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <h3 className="text-lg font-semibold text-slate-900">Profile Strength / Readiness Breakdown</h3>
      <div className={`mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-2 ${isPremiumStudent ? 'xl:grid-cols-4 2xl:grid-cols-5' : ''}`}>
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs uppercase tracking-wide text-slate-500">Profile completeness</p><p className="mt-1 text-xl font-semibold text-slate-900">{profileCompleteness}%</p></div>
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs uppercase tracking-wide text-slate-500">Missing requirements</p><p className="mt-1 text-xl font-semibold text-slate-900">{missingRequirementsCount}</p></div>
        {isPremiumStudent ? <div className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs uppercase tracking-wide text-slate-500">Academic readiness</p><p className="mt-1 text-xl font-semibold text-slate-900">{academicReadinessScore}%</p></div> : null}
        {isPremiumStudent ? <div className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs uppercase tracking-wide text-slate-500">Qualification readiness</p><p className="mt-1 text-xl font-semibold text-slate-900">{qualificationReadinessScore}%</p></div> : null}
        {isPremiumStudent ? <div className="rounded-xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs uppercase tracking-wide text-slate-500">Document completeness</p><p className="mt-1 text-xl font-semibold text-slate-900">{documentCompletenessScore}%</p></div> : null}
      </div>
    </article>

    {isPremiumStudent ? (
      <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Brain className="h-5 w-5 text-primary-600" />
            <h3 className="text-lg font-semibold text-slate-900">AI Guidance Preview / Qualification Readiness</h3>
          </div>
          <Button type="button" onClick={() => setGuidanceRefresh((count) => count + 1)} disabled={Boolean(profileReadinessMessage) || aiPreview.isFetching}>{aiPreview.isFetching ? 'Analyzing...' : 'Refresh guidance'}</Button>
        </div>

        {profileReadinessMessage ? <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{profileReadinessMessage}</div> : null}
        {!profileReadinessMessage && aiPreview.isLoading ? <LoadingState message="Analyzing your qualification readiness..." detail="Matching your grade, subjects, and levels against available admission requirements." /> : null}
        {!profileReadinessMessage && aiPreview.isError ? <ErrorState message="Could not generate qualification readiness right now. Please try again shortly." /> : null}

        {!profileReadinessMessage && !aiPreview.isLoading && !aiPreview.isError ? (
          <div className="space-y-3">
            {aiStatus === 'ERROR' && aiWarningMessage ? <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{aiWarningMessage}</div> : null}
            {qualificationEvaluations.length ? qualificationEvaluations.map((evaluation) => (
              <article key={`${evaluation.university}-${evaluation.programmeName}`} className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h4 className="font-semibold text-slate-900">{evaluation.programmeName}</h4>
                    <p className="text-xs text-slate-500">{evaluation.university || 'Institution pending'}</p>
                  </div>
                  {evaluation.qualifies === true ? <Badge color="emerald">Qualifies</Badge> : evaluation.qualifies === false ? <Badge color="amber">Does Not Qualify Yet</Badge> : <Badge color="slate">Needs Manual Verification</Badge>}
                </div>
                <p className={`mt-2 text-sm ${evaluation.qualifies === false ? 'text-rose-700' : 'text-slate-700'}`}>{evaluation.qualificationMessage}</p>
                {evaluation.admissionRequirements.length ? <div className="mt-2"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Minimum requirements</p><ul className="ml-5 list-disc text-sm text-slate-700">{evaluation.admissionRequirements.map((requirement) => <li key={`${evaluation.programmeName}-${requirement}`}>{requirement}</li>)}</ul></div> : null}
                {evaluation.parsedChecks.length ? <div className="mt-2"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Parsed pass-level checks</p><ul className="ml-5 list-disc text-sm text-slate-700">{evaluation.parsedChecks.map((check) => <li key={`${evaluation.programmeName}-${check.subjectName}-${check.minLevel}`}>{check.subjectName}: minimum Level {check.minLevel}</li>)}</ul></div> : null}
                {evaluation.missingRequirements.length ? <div className="mt-2 rounded-lg border border-rose-200 bg-rose-50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-rose-700">Missing requirements</p><ul className="ml-5 list-disc text-sm text-rose-700">{evaluation.missingRequirements.map((gap) => <li key={`${evaluation.programmeName}-${gap}`}>{gap}</li>)}</ul></div> : null}
                {evaluation.weakSubjects.length ? <div className="mt-2 rounded-lg border border-amber-200 bg-amber-50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-amber-700">Weak subject areas</p><ul className="ml-5 list-disc text-sm text-amber-700">{evaluation.weakSubjects.map((weakness) => <li key={`${evaluation.programmeName}-${weakness}`}>{weakness}</li>)}</ul></div> : null}
                {evaluation.improvementSuggestions.length ? <div className="mt-2"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Improvement suggestions</p><ul className="ml-5 list-disc text-sm text-slate-700">{evaluation.improvementSuggestions.map((suggestion) => <li key={`${evaluation.programmeName}-${suggestion}`}>{suggestion}</li>)}</ul></div> : null}
                {evaluation.alternatives.length ? <div className="mt-2"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Suggested alternatives</p><div className="mt-1 flex flex-wrap gap-1.5">{evaluation.alternatives.map((alternative) => <span key={`${evaluation.programmeName}-${alternative}`} className="rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-700">{alternative}</span>)}</div></div> : null}
              </article>
            )) : (
              <article className="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
                <p className="text-sm text-slate-600">No qualification programmes were returned yet. Refresh guidance after updating your profile details.</p>
                {fallbackCareerRecommendations.length ? (
                  <div className="mt-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Fallback career recommendations</p>
                    <ul className="mt-2 ml-5 list-disc text-sm text-slate-700">
                      {fallbackCareerRecommendations.map((career) => <li key={career.id}>{career.title}</li>)}
                    </ul>
                  </div>
                ) : null}
              </article>
            )}
          </div>
        ) : null}
      </article>
    ) : (
      <article className="rounded-2xl border border-primary-200 bg-primary-50 p-5 shadow-sm">
        <p className="font-semibold text-primary-900">Upgrade to Premium to unlock insights</p>
        <p className="mt-1 text-sm text-primary-800">Unlock academic, qualification, and AI guidance insights with Premium.</p>
        <Link to="/student/subscription" className="mt-2 inline-block text-sm font-semibold text-primary-700 hover:text-primary-600">View plans</Link>
      </article>
    )}

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <h3 className="text-lg font-semibold text-slate-900">Documents</h3>
      <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
        <label className="block cursor-pointer rounded-xl border border-dashed border-slate-300 bg-slate-50/70 p-4 transition hover:border-primary-300 hover:bg-primary-50/40">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-medium text-slate-800">CV Upload</p>
              <p className="text-xs text-slate-500">{selectedCvName || 'Choose PDF/DOC/DOCX'}</p>
              <p className={`mt-2 text-xs font-semibold ${p?.cvFileUrl ? 'text-emerald-700' : 'text-slate-500'}`}>{p?.cvFileUrl ? 'Uploaded' : 'Not uploaded yet'}</p>
            </div>
            {p?.cvFileUrl ? <CheckCircle2 className="h-5 w-5 text-emerald-600" /> : <FileUp className="h-5 w-5 text-slate-500" />}
          </div>
          <input
            type="file"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              setSelectedCvName(file.name);
              cvUpload.mutate(file);
            }}
          />
        </label>
        <label className="block cursor-pointer rounded-xl border border-dashed border-slate-300 bg-slate-50/70 p-4 transition hover:border-primary-300 hover:bg-primary-50/40">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-medium text-slate-800">Transcript Upload</p>
              <p className="text-xs text-slate-500">{selectedTranscriptName || 'Choose PDF/DOC/DOCX'}</p>
              <p className={`mt-2 text-xs font-semibold ${p?.transcriptFileUrl ? 'text-emerald-700' : 'text-slate-500'}`}>{p?.transcriptFileUrl ? 'Uploaded' : 'Not uploaded yet'}</p>
            </div>
            {p?.transcriptFileUrl ? <CheckCircle2 className="h-5 w-5 text-emerald-600" /> : <FileUp className="h-5 w-5 text-slate-500" />}
          </div>
          <input
            type="file"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              setSelectedTranscriptName(file.name);
              transcriptUpload.mutate(file);
            }}
          />
        </label>
      </div>
      {(cvUpload.isError || transcriptUpload.isError) ? <p className="mt-2 text-sm text-red-600">Could not upload one of your documents. Please try again.</p> : null}
    </article>

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <h3 className="text-lg font-semibold text-slate-900">Saved Profile Versions</h3>
      <div className="mt-3 grid gap-2 md:grid-cols-[1fr_auto]">
        <Input placeholder="Profile name (example: Engineering Track)" value={profileVersionName} onChange={(e) => setProfileVersionName(e.target.value)} />
        <Button onClick={() => saveProfileVersion.mutate()} disabled={saveProfileVersion.isPending || !profileVersionName.trim()}>
          {saveProfileVersion.isPending ? 'Saving...' : 'Save As New Profile'}
        </Button>
      </div>
      {saveProfileVersion.isError ? <p className="mt-2 text-sm text-red-600">Could not save this profile version right now.</p> : null}
      {saveProfileVersion.isSuccess ? <p className="mt-2 text-sm text-emerald-700">Profile version saved for future use.</p> : null}
      {savedProfiles.isLoading ? <p className="mt-2 text-sm text-slate-500">Loading saved profiles...</p> : null}
      {savedProfiles.isError ? <p className="mt-2 text-sm text-red-600">Could not load saved profile versions.</p> : null}
      {!!savedProfiles.data?.length && <div className="mt-3 space-y-2">
        {savedProfiles.data.map((saved) => {
          const details = savedProfileDetailById.get(saved.id);
          const gradeLabel = details?.profile?.selectedGrade;
          const subjectSummary = (details?.profile?.subjectAchievements ?? [])
            .filter((item) => item.subjectName)
            .slice(0, 4)
            .map((item) => `${item.subjectName} (L${item.achievementLevel ?? '-'})`);
          return <div key={saved.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 transition hover:bg-white">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-medium text-slate-900">{saved.name}</p>
                <p className="text-xs text-slate-500">Updated {new Date(saved.updatedAt).toLocaleString()}</p>
                {gradeLabel ? <p className="mt-1 text-xs font-semibold text-primary-700">{gradeLabel}</p> : null}
                {subjectSummary.length ? <p className="mt-1 text-xs text-slate-600">{subjectSummary.join(' â€¢ ')}</p> : null}
              </div>
              <div className="flex gap-2">
                <Button onClick={() => applySavedProfile.mutate(saved.id)} disabled={applySavedProfile.isPending || deleteSavedProfile.isPending}>
                  {applySavedProfile.isPending ? 'Applying...' : 'Use This Profile'}
                </Button>
                <Button onClick={() => deleteSavedProfile.mutate(saved.id)} disabled={applySavedProfile.isPending || deleteSavedProfile.isPending} className="bg-slate-700 hover:bg-slate-600">
                  {deleteSavedProfile.isPending ? 'Deleting...' : 'Delete'}
                </Button>
              </div>
            </div>
          </div>;
        })}
      </div>}
      {!savedProfiles.isLoading && !savedProfiles.data?.length ? <p className="mt-2 text-sm text-slate-500">No saved profiles yet. Save this one so you can reuse it later.</p> : null}
      {applySavedProfile.isError ? <p className="mt-2 text-sm text-red-600">Could not apply saved profile right now.</p> : null}
      {deleteSavedProfile.isError ? <p className="mt-2 text-sm text-red-600">Could not delete saved profile right now.</p> : null}
    </article>

    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-2 text-sm text-slate-600">
        {(update.isSuccess || cvUpload.isSuccess || transcriptUpload.isSuccess) ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <AlertTriangle className="h-4 w-4 text-slate-400" />}
        <span>{update.isSuccess ? 'Profile saved successfully.' : 'Remember to save profile changes after editing.'}</span>
      </div>
      <Button onClick={() => update.mutate()} disabled={update.isPending}>{update.isPending ? 'Saving profile...' : 'Save profile'}</Button>
    </div>
  </Section>;
};

export const StudentAcademicProfilePage = StudentProfilePage;
export const StudentDocumentsPage = StudentProfilePage;
export const StudentQualificationsPage = StudentProfilePage;
export const StudentExperiencePage = StudentProfilePage;

export const StudentCareerRecommendationsPage = () => {
  const isDemoMode = aiGuidanceService.demoModeEnabled;
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const [guidanceMode, setGuidanceMode] = useState<'FAST' | 'DEEP'>('FAST');
  const subscription = useAppQuery({ queryKey: ['sub'], queryFn: subscriptionService.current });
  const isPremium = Boolean(subscription.data?.premiumAccess) || (subscription.data?.planCode === 'PLAN_PREMIUM' && subscription.data?.status === 'ACTIVE');

  useEffect(() => {
    if (guidanceMode === 'DEEP' && !isPremium) {
      setGuidanceMode('FAST');
    }
  }, [guidanceMode, isPremium]);

  const currentProfile = profile.data;
  const qualificationLevel = currentProfile?.qualificationLevel?.trim() ?? '';
  const careerInterest = (currentProfile?.interests ?? []).map((item) => item.trim()).filter(Boolean).join(', ');
  const targetProgram = currentProfile?.careerGoals?.trim() || careerInterest;
  const guidanceMaxRecommendations = guidanceMode === 'FAST' ? 3 : 10;
  const profileReadinessMessage = !currentProfile
    ? 'Student profile is required before requesting AI guidance.'
    : !qualificationLevel || !careerInterest
      ? 'Please complete your profile (qualification level and interests) before generating AI guidance.'
      : null;

  const defaultSources = useAppQuery({
    queryKey: ['default-university-sources'],
    enabled: !aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDefaultUniversitySources,
    staleTime: 30 * 60_000,
    gcTime: 60 * 60_000,
    refetchOnMount: false,
  });

  const aiAdvice = useAppQuery({
    queryKey: ['ai-guidance-university-sources', guidanceMode, currentProfile?.id, currentProfile?.profileCompleteness, qualificationLevel, careerInterest, targetProgram],
    enabled: Boolean(currentProfile) && !aiGuidanceService.demoModeEnabled && !profileReadinessMessage,
    queryFn: async () => aiGuidanceService.analyseUniversitySources({
      urls: [], // Empty list triggers backend default-source mode.
      targetProgram,
      careerInterest,
      qualificationLevel,
      maxRecommendations: guidanceMaxRecommendations,
    }),
    retry: false,
    staleTime: 15 * 60_000,
    gcTime: 30 * 60_000,
    refetchOnMount: false,
    placeholderData: keepPreviousData,
  });

  const demoAdvice = useAppQuery({
    queryKey: ['ai-guidance-demo'],
    enabled: aiGuidanceService.demoModeEnabled,
    queryFn: aiGuidanceService.getDemoGuidance,
  });

  if (profile.isLoading || demoAdvice.isLoading) return <LoadingState message="Generating AI guidance..." detail="We are matching your profile with the best-fit careers and programmes." />;
  if (profile.isError) return <ErrorState message="Could not load your profile. Please refresh and try again." />;

  const isSearching = !isDemoMode && (aiAdvice.isLoading || aiAdvice.isFetching || defaultSources.isLoading || defaultSources.isFetching);
  if (!isDemoMode && profileReadinessMessage) return <ErrorState message={profileReadinessMessage} />;

  const hasAiRequestError = !isDemoMode && aiAdvice.isError;

  const demoRecommendations = (demoAdvice.data?.suggestedCareers ?? []).map((item) => item.title);
  const careers = aiAdvice.data?.recommendedCareers ?? [];
  const programmes = aiAdvice.data?.recommendedProgrammes ?? [];
  const universities = aiAdvice.data?.recommendedUniversities ?? [];
  const minimumRequirements = aiAdvice.data?.minimumRequirements ?? [];
  const skillGaps = aiAdvice.data?.skillGaps ?? [];
  const nextSteps = aiAdvice.data?.recommendedNextSteps ?? [];
  const warnings = aiAdvice.data?.warnings ?? [];
  const sourceDiagnostics = aiAdvice.data?.sourceDiagnostics ?? [];
  const sourceCoverage = aiAdvice.data?.sourceCoverage;
  const responseStatus = hasAiRequestError ? 'ERROR' : (aiAdvice.data?.status ?? 'ERROR');
  const aiAvailable = hasAiRequestError ? false : (aiAdvice.data?.available ?? (aiAdvice.data?.aiLive ?? false));
  const aiUnavailable = !isDemoMode && (hasAiRequestError || !aiAvailable || responseStatus === 'ERROR');
  const backendMode = aiAdvice.data?.mode ?? (aiAdvice.data?.fallbackUsed ? 'FALLBACK' : aiAdvice.data?.aiLive ? 'LIVE' : 'UNAVAILABLE');
  const requestedSources = aiAdvice.data?.sourceCoverage?.requestedSourcesCount ?? aiAdvice.data?.requestedSources?.length ?? aiAdvice.data?.sourceUrls?.length ?? 0;
  const analysedSources = aiAdvice.data?.totalSourcesUsed ?? 0;
  const aiPlanCode = aiAdvice.data?.planCode ?? (isPremium ? 'PLAN_PREMIUM' : 'PLAN_BASIC');
  const aiPremiumUnlocked = isDemoMode ? isPremium : (aiAdvice.data?.premiumUnlocked ?? isPremium);
  const aiCareerLimit = aiPremiumUnlocked ? null : (aiAdvice.data?.careerSuggestionLimit ?? 3);
  const visibleCareers = aiPremiumUnlocked ? careers : careers.slice(0, aiCareerLimit ?? 3);
  const visibleDemoRecommendations = isPremium ? demoRecommendations.slice(0, 10) : demoRecommendations.slice(0, 3);
  const basicPlanLockMessage = aiAdvice.data?.upgradeMessage ?? 'Upgrade to Premium to unlock deeper AI analysis and more recommendations.';

  const renderSimpleList = (items: string[], emptyText: string) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">{emptyText}</p>;
    }
    return <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-2">{items.map((item) => <div key={item} className="border p-2 rounded text-sm">{item}</div>)}</div>;
  };

  const renderCareerCards = (items: UniversityRecommendedCareer[]) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">No career recommendations yet.</p>;
    }
    return <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">{items.slice(0, 10).map((career) => <article key={career.name} className="rounded border p-3 space-y-2 bg-white">
      <h4 className="font-semibold">{career.name}</h4>
      <p className="text-sm text-slate-600">{career.reason}</p>
      {career.rankingCategory && <p className="text-xs font-medium text-emerald-700">{career.rankingCategory}</p>}
      {career.recommendationReason && <p className="text-sm text-slate-700"><span className="font-medium">Why this was recommended:</span> {career.recommendationReason}</p>}
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Requirements</p>
        <ul className="list-disc ml-5 text-sm">{career.requirements.map((requirement) => <li key={`${career.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Related programmes</p>
        <ul className="list-disc ml-5 text-sm">{career.relatedProgrammes.map((programme) => <li key={`${career.name}-${programme}`}>{programme}</li>)}</ul>
      </div>
      {career.verifiedFacts?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Verified facts</p><ul className="list-disc ml-5 text-sm">{career.verifiedFacts.map((fact) => <li key={`${career.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {career.nextBestActions?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Next best actions</p><ul className="list-disc ml-5 text-sm">{career.nextBestActions.map((action) => <li key={`${career.name}-${action}`}>{action}</li>)}</ul></div> : null}
    </article>)}</div>;
  };

  const renderProgrammeCards = (items: UniversityRecommendedProgramme[]) => {
    if (items.length === 0) {
      return <p className="text-sm text-slate-500">No programme recommendations yet.</p>;
    }
    return <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">{items.slice(0, 10).map((programme) => <article key={`${programme.name}-${programme.university}`} className="rounded border p-3 space-y-2 bg-white">
      <h4 className="font-semibold">{programme.name}</h4>
      <p className="text-sm text-slate-600">{programme.university}</p>
      <div className="flex gap-2 text-xs">{programme.rankingCategory && <span className="rounded bg-emerald-50 px-2 py-1 text-emerald-700">{programme.rankingCategory}</span>}{programme.confidenceLevel && <span className="rounded bg-slate-100 px-2 py-1 text-slate-700">Confidence: {programme.confidenceLevel}</span>}{programme.sourceStatus && <span className="rounded bg-blue-50 px-2 py-1 text-blue-700">Source: {programme.sourceStatus}</span>}</div>
      {programme.recommendationReason && <p className="text-sm text-slate-700"><span className="font-medium">Why this was recommended:</span> {programme.recommendationReason}</p>}
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Admission requirements</p>
        <ul className="list-disc ml-5 text-sm">{programme.admissionRequirements.map((requirement) => <li key={`${programme.name}-${requirement}`}>{requirement}</li>)}</ul>
      </div>
      <div className="text-sm">
        <p><span className="font-medium">Notes:</span> {programme.notes}</p>
      </div>
      {programme.verifiedFacts?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Verified facts</p><ul className="list-disc ml-5 text-sm">{programme.verifiedFacts.map((fact) => <li key={`${programme.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {programme.missingData?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Missing information</p><ul className="list-disc ml-5 text-sm">{programme.missingData.map((fact) => <li key={`${programme.name}-${fact}`}>{fact}</li>)}</ul></div> : null}
      {programme.nextBestActions?.length ? <div><p className="text-xs uppercase tracking-wide text-slate-500">Next best actions</p><ul className="list-disc ml-5 text-sm">{programme.nextBestActions.map((action) => <li key={`${programme.name}-${action}`}>{action}</li>)}</ul></div> : null}
    </article>)}</div>;
  };

  return <Section title="AI Guidance">
    <div className="relative overflow-hidden rounded-[28px] border border-blue-900/30 bg-[#081739] p-5 text-white shadow-xl shadow-slate-900/20 md:p-6">
      <div className="pointer-events-none absolute -right-8 -top-12 h-44 w-44 rounded-full bg-gradient-to-br from-blue-500/35 to-orange-400/30 blur-2xl" />
      <div className="relative flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm text-blue-100/90">Mode: {isDemoMode ? 'demo (seeded)' : `${backendMode} (${guidanceMode.toLowerCase()})`}</p>
          <p className="mt-2 text-lg font-semibold">AI-powered university and career guidance workspace</p>
        </div>
        {!isDemoMode && (
          <div className="rounded-2xl border border-white/20 bg-white/10 px-4 py-3 text-sm">
            <p><span className="font-semibold">Current plan:</span> {aiPlanCode.replace('PLAN_', '')}</p>
            <p className="mt-1 text-blue-100/90">
              {aiPremiumUnlocked
                ? 'Premium unlocks full guidance, expanded analysis, and uncapped career suggestions.'
                : `Basic plan shows up to ${aiCareerLimit ?? 3} career suggestions with limited detail.`}
            </p>
            {!aiPremiumUnlocked ? <p className="mt-1 text-amber-200">{basicPlanLockMessage}</p> : null}
            {!aiPremiumUnlocked ? <Link to="/student/subscription" className="mt-2 inline-block font-semibold text-white underline underline-offset-2 hover:text-blue-200">Upgrade to Premium</Link> : null}
          </div>
        )}
      </div>
    </div>
    {!isDemoMode && <div className="rounded-[22px] border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-slate-800">Search mode</p>
          <p className="text-xs text-slate-500">Fast returns quicker results. Deep checks more sources for richer guidance and requires an active Premium plan.</p>
        </div>
        <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-1">
          <button
            type="button"
            className={`rounded-md px-3 py-1 text-sm ${guidanceMode === 'FAST' ? 'bg-primary-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
            onClick={() => setGuidanceMode('FAST')}
          >
            Fast
          </button>
          <button
            type="button"
            className={`rounded-md px-3 py-1 text-sm ${guidanceMode === 'DEEP' ? 'bg-primary-600 text-white' : 'text-slate-600 hover:bg-slate-100'} ${!isPremium ? 'cursor-not-allowed opacity-50' : ''}`}
            onClick={() => setGuidanceMode('DEEP')}
            disabled={!isPremium}
            title={!isPremium ? 'Upgrade to Premium to use Deep mode.' : undefined}
          >
            Deep
          </button>
        </div>
      </div>
    </div>}
    {!aiPremiumUnlocked ? <p className="text-xs text-amber-700">Deep AI mode and expanded insights are available on Premium subscriptions.</p> : null}
    {isSearching ? <div className="rounded-[22px] border border-blue-200 bg-blue-50/70 p-4"><LoadingState message="Searching for guidance results..." detail={guidanceMode === 'FAST' ? 'Fast mode is prioritising quick source retrieval.' : 'Deep mode is checking more sources for broader coverage.'} /></div> : null}
    {!isDemoMode && <>
      {aiUnavailable && <div className="rounded-[22px] border border-rose-300 bg-rose-50 p-4 text-sm text-rose-900 shadow-sm">
        <span className="font-semibold">{AI_ERROR_MESSAGE}</span>
      </div>}
      {!aiUnavailable && responseStatus === 'PARTIAL' && <div className="rounded border border-blue-300 bg-blue-50 p-3 text-sm text-blue-900">
        EduRite returned partial guidance using the university sources that completed successfully.
      </div>}
      {!aiUnavailable && <>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <Card label="Sources used" value={analysedSources} />
        <Card label="Requested sources" value={sourceCoverage?.requestedSourcesCount ?? requestedSources} />
        <Card label="Suitability score" value={`${aiAdvice.data?.suitabilityScore ?? 0}%`} />
      </div>
      {aiAdvice.data?.warningMessage && <div className="rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
        <span className="font-semibold">Warning:</span> {aiAdvice.data.warningMessage}
      </div>}
      {analysedSources === 0 && <div className="rounded border border-rose-300 bg-rose-50 p-3 text-sm text-rose-900">
        No public university sources were analysed for this response. Please verify every recommendation on official university pages.
      </div>}
      {aiAdvice.data?.suitabilityScoreReason && <div className="rounded border p-3 bg-slate-50 text-sm">
        <p><span className="font-semibold">Suitability explanation:</span> {aiAdvice.data.suitabilityScoreReason}</p>
        {!!aiAdvice.data.suitabilitySignalsUsed?.length && <p className="mt-2"><span className="font-semibold">Signals used:</span> {aiAdvice.data.suitabilitySignalsUsed.join(', ')}</p>}
        {!!aiAdvice.data.suitabilityScoreLimitations?.length && <p className="mt-2"><span className="font-semibold">What lowered the score:</span> {aiAdvice.data.suitabilityScoreLimitations.join(', ')}</p>}
      </div>}
      {sourceCoverage && <div className="rounded border p-3 bg-white text-sm">
        <h3 className="font-semibold mb-1">Source coverage</h3>
        <p>Successful: {sourceCoverage.successfulSourcesCount} • Failed: {sourceCoverage.failedSourcesCount} • Partial: {sourceCoverage.partialSourcesCount}</p>
        {!!sourceCoverage.universitiesWithUsableProgrammeData?.length && <p className="mt-1">Universities with usable programme data: {sourceCoverage.universitiesWithUsableProgrammeData.join(', ')}</p>}
      </div>}
    </>}

    {!aiUnavailable && <div className="space-y-2">
      <h3 className="font-semibold">Recommended careers</h3>
      {isDemoMode ? renderSimpleList(visibleDemoRecommendations, 'No career recommendations yet.') : renderCareerCards(visibleCareers)}
      {!isDemoMode && !aiPremiumUnlocked ? <p className="text-xs text-amber-700">Showing {aiCareerLimit ?? 3} career suggestions on Basic. Upgrade for full recommendations.</p> : null}
    </div>}

    {!aiUnavailable && !isDemoMode && aiPremiumUnlocked && <>
      <div className="space-y-2">
        <h3 className="font-semibold">Recommended programmes</h3>
        {renderProgrammeCards(programmes)}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Recommended universities</h3>
        {renderSimpleList(universities, 'No university recommendations yet.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Minimum requirements</h3>
        {renderSimpleList(minimumRequirements, 'Minimum requirements are currently unavailable.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Skill gaps</h3>
        {renderSimpleList(skillGaps, 'No skill gaps identified.')}
      </div>

      <div className="space-y-2">
        <h3 className="font-semibold">Recommended next steps</h3>
        {renderSimpleList(nextSteps, 'No next steps provided.')}
      </div>

      {warnings.length > 0 && <div className="space-y-2">
        <h3 className="font-semibold">Warnings</h3>
        {renderSimpleList(warnings, 'No warnings.')}
      </div>}

      {sourceDiagnostics.length > 0 && <div className="space-y-2">
        <h3 className="font-semibold">Source availability</h3>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-2">
          {sourceDiagnostics.map((item) => (
            <div key={item.sourceUrl} className="rounded border p-3 text-sm">
              <p className="font-medium">{item.university ?? 'University source'}</p>
              <p className="mt-1"><span className="font-medium">Availability:</span> {item.fetchStatus}</p>
              {item.failureReason && <p className="mt-1 text-slate-600">{item.failureReason}</p>}
            </div>
          ))}
        </div>
      </div>}

      {aiAdvice.data?.summary && <div className="rounded border p-3 bg-slate-50">
        <h3 className="font-semibold mb-1">Summary</h3>
        <p className="text-sm">{aiAdvice.data.summary}</p>
      </div>}
      </>}
    </>}
    {!aiUnavailable && !isDemoMode && !aiPremiumUnlocked ? (
      <div className="rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
        <p className="font-semibold">Premium detail is locked on Basic.</p>
        <p className="mt-1">Upgrade to unlock full programme analysis, source diagnostics, and expanded next-step guidance.</p>
      </div>
    ) : null}
  </Section>;
};
export const StudentBursaryRecommendationsPage = StudentCareerRecommendationsPage;

export const StudentSavedPage = () => {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', field: '', industry: '', qualification: '', location: '', demand: '', opportunityType: 'ALL' as OpportunityType });
  const opportunities = useAppQuery({ queryKey: ['student-opportunities', filters], queryFn: () => studentService.searchOpportunities(filters) });
  const [jobSearch, setJobSearch] = useState({ query: 'software developer', location: 'South Africa', category: '', experience: '' });
  const liveJobs = useAppQuery<JobOpportunity[]>({
    queryKey: ['live-jobs', jobSearch.query, jobSearch.location, jobSearch.category, jobSearch.experience],
    queryFn: () => jobsService.search(jobSearch),
    retry: false,
  });
  const toggle = useMutation({
    mutationFn: ({ item }: { item: UnifiedOpportunity }) => item.saved
      ? studentService.unsaveOpportunity(item.type, item.id)
      : studentService.saveOpportunity(item.type, item.id, item.title),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['student-opportunities'] }),
  });
  const items = opportunities.data ?? [];
  const badgeColor = (type: UnifiedOpportunity['type']): 'blue' | 'emerald' | 'amber' => type === 'CAREER' ? 'blue' : type === 'JOB' ? 'emerald' : 'amber';
  const normalize = (value?: string | null) => value?.trim().toLowerCase() ?? '';
  const jobLike = (job: JobOpportunity) => job as JobOpportunity & { logoUrl?: string; companyLogoUrl?: string; posted?: string; jobType?: string; workMode?: string };
  const companyLogoUrl = (job: JobOpportunity) => jobLike(job).logoUrl ?? jobLike(job).companyLogoUrl;
  const companyInitials = (company: string) => company.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? '').join('').slice(0, 2) || 'J';
  const jobTypeLabel = (job: JobOpportunity) => job.contractType?.trim() || jobLike(job).jobType?.trim() || (normalize(job.category).includes('intern') ? 'Internship' : normalize(job.category).includes('graduate') ? 'Graduate' : 'Full Time');
  const workModeLabel = (job: JobOpportunity) => {
    const blob = `${job.location} ${job.description} ${job.category} ${job.contractType}`.toLowerCase();
    if (blob.includes('remote') || blob.includes('work from home') || blob.includes('virtual')) return 'Remote';
    if (blob.includes('hybrid')) return 'Hybrid';
    if (blob.includes('intern')) return 'Internship';
    if (blob.includes('graduate')) return 'Graduate';
    return 'In Office';
  };
  const postedLabel = (value?: string) => {
    if (!value) return 'Recently';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return value;
    const now = new Date();
    const diffDays = Math.max(0, Math.floor((now.getTime() - parsed.getTime()) / 86400000));
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return '1 day ago';
    return `${diffDays} days ago`;
  };
  return <Section title="Explore Careers & Jobs">
    <p className="text-sm text-slate-500">Explore careers and jobs tailored to your skills, interests, and academic background.</p>
    <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      <Input placeholder="Search" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Field" value={filters.field} onChange={(e) => setFilters((s) => ({ ...s, field: e.target.value }))} />
      <Input placeholder="Industry" value={filters.industry} onChange={(e) => setFilters((s) => ({ ...s, industry: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualification} onChange={(e) => setFilters((s) => ({ ...s, qualification: e.target.value }))} />
      <Input placeholder="Location" value={filters.location} onChange={(e) => setFilters((s) => ({ ...s, location: e.target.value }))} />
      <Input placeholder="Demand" value={filters.demand} onChange={(e) => setFilters((s) => ({ ...s, demand: e.target.value }))} />
      <select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary-500" value={filters.opportunityType} onChange={(e) => setFilters((s) => ({ ...s, opportunityType: e.target.value as OpportunityType }))}>
        <option value="ALL">All opportunity types</option>
        <option value="CAREER">Career</option>
        <option value="JOB">Job</option>
      </select>
    </div>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
      {items.map((item) => <article key={`${item.type}-${item.id}`} className="rounded border border-slate-200 bg-white p-4 shadow-sm space-y-3">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-base font-semibold text-slate-900">{item.title}</h3>
              <Badge color={badgeColor(item.type)}>{item.type}</Badge>
              {item.recommended ? <span className="rounded bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">Recommended</span> : null}
            </div>
            <p className="text-sm text-slate-600">{item.industry ?? item.field ?? 'General opportunities'}{item.location ? ` • ${item.location}` : ''}</p>
          </div>
          <Button onClick={() => toggle.mutate({ item })} disabled={toggle.isPending}>{item.saved ? 'Saved' : 'Save'}</Button>
        </div>
        <div className="grid gap-2 text-sm text-slate-600 sm:grid-cols-2">
          <p><span className="font-medium text-slate-800">Field:</span> {item.field ?? '-'}</p>
          <p><span className="font-medium text-slate-800">Industry:</span> {item.industry ?? '-'}</p>
          <p><span className="font-medium text-slate-800">Qualification:</span> {item.qualification ?? '-'}</p>
          <p><span className="font-medium text-slate-800">Demand:</span> {item.demand ?? '-'}</p>
        </div>
        <div>
          <Button
            className="bg-slate-700 hover:bg-slate-600"
            onClick={() => navigate(`/student/careers/${item.id}`, { state: { opportunityType: item.type } })}
          >
            View details
          </Button>
        </div>
      </article>)}
    </div>
    <div className="mt-4 space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-slate-900">Live Job Opportunities</h3>
        <span className="text-xs text-slate-500">Adzuna</span>
      </div>
      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        <Input value={jobSearch.query} onChange={(e) => setJobSearch((s) => ({ ...s, query: e.target.value }))} placeholder="Job title" />
        <Input value={jobSearch.location} onChange={(e) => setJobSearch((s) => ({ ...s, location: e.target.value }))} placeholder="Location" />
        <Input value={jobSearch.category} onChange={(e) => setJobSearch((s) => ({ ...s, category: e.target.value }))} placeholder="Category" />
        <select value={jobSearch.experience} onChange={(e) => setJobSearch((s) => ({ ...s, experience: e.target.value }))} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
          <option value="">All levels</option>
          <option value="internship">Internship</option>
          <option value="graduate">Graduate</option>
          <option value="entry">Entry level</option>
        </select>
      </div>
      {liveJobs.isLoading || liveJobs.isFetching ? <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800">Loading job opportunities...</div> : null}
      {liveJobs.isError ? <div className="rounded-lg border border-rose-300 bg-rose-50 p-3 text-sm text-rose-900">Job opportunities are temporarily unavailable. Please try again.</div> : null}
      {!liveJobs.isLoading && !liveJobs.isError && !(liveJobs.data ?? []).length ? <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">No jobs found for this search.</div> : null}
      {!liveJobs.isError && (liveJobs.data ?? []).length > 0 ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 items-stretch">
        {(liveJobs.data ?? []).slice(0, 9).map((job) => <article key={job.id} className="h-full rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_6px_18px_rgba(15,23,42,0.06)] transition-all duration-200 hover:-translate-y-1 hover:shadow-lg">
          <div className="flex h-full flex-col">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h4 className="line-clamp-2 text-sm font-bold text-[#1E293B]">{job.title}</h4>
                <div className="mt-2 flex items-center gap-2">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-100 ring-1 ring-slate-200">
                    {companyLogoUrl(job) ? <img src={companyLogoUrl(job)} alt={`${job.company} logo`} className="h-full w-full object-cover" /> : <span className="text-[11px] font-bold text-[#2563EB]">{companyInitials(job.company)}</span>}
                  </div>
                  <p className="text-sm font-semibold text-[#2563EB]">{job.company}</p>
                </div>
              </div>
              <div className="flex flex-col items-end gap-2">
                <Badge color="blue">{jobTypeLabel(job)}</Badge>
                <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700">{workModeLabel(job)}</span>
              </div>
            </div>
            <div className="mt-3 flex flex-wrap gap-2 text-[11px] font-medium">
              {job.category ? <span className="rounded-full bg-blue-50 px-2.5 py-1 text-blue-700">{job.category}</span> : null}
              {normalize(job.location).includes('remote') ? <span className="rounded-full bg-cyan-50 px-2.5 py-1 text-cyan-700">Remote</span> : null}
              {normalize(job.location).includes('hybrid') ? <span className="rounded-full bg-amber-50 px-2.5 py-1 text-amber-700">Hybrid</span> : null}
              {job.contractType ? <span className="rounded-full bg-slate-100 px-2.5 py-1 text-slate-600">{job.contractType}</span> : null}
            </div>
            <p className="mt-3 line-clamp-3 text-sm leading-6 text-[#475569]">{job.description}</p>
            <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-[#F8FAFF] p-3 text-sm text-[#64748B]">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Location</p>
                <p className="mt-1 line-clamp-1 text-sm font-medium text-slate-700">{job.location}</p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Company</p>
                <p className="mt-1 line-clamp-1 text-sm font-medium text-slate-700">{job.company}</p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Job Type</p>
                <p className="mt-1 line-clamp-1 text-sm font-medium text-slate-700">{jobTypeLabel(job)}</p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Posted</p>
                <p className="mt-1 line-clamp-1 text-sm font-medium text-slate-700">{postedLabel(job.created)}</p>
              </div>
            </div>
            {(job.salaryMin || job.salaryMax) ? <p className="mt-3 text-sm text-[#64748B]"><span className="font-medium text-slate-700">Salary:</span> {job.salaryMin ?? '-'} - {job.salaryMax ?? '-'}</p> : null}
            <div className="mt-auto pt-4">
              <a href={job.redirectUrl} target="_blank" rel="noopener noreferrer" className="inline-flex w-full items-center justify-center rounded-[10px] bg-[#2563EB] px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#1D4ED8]">View Job</a>
            </div>
          </div>
        </article>)}
      </div> : null}
    </div>
    {!items.length && !opportunities.isLoading ? <EmptyState title="No opportunities found" message="Try broadening your filters to explore more careers and jobs." /> : null}
  </Section>;
};
export const StudentCareerDetailsPage = () => {
  const { id = '' } = useParams();
  const location = useLocation();
  const routeState = location.state as { opportunityType?: UnifiedOpportunity['type'] } | null;
  const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id);
  const isCatalogOpportunity = routeState?.opportunityType === 'JOB' || !isUuid;

  const career = useAppQuery({
    queryKey: ['student', 'career', id],
    queryFn: () => careerService.details(id),
    enabled: Boolean(id) && !isCatalogOpportunity,
  });
  const catalogOpportunity = useAppQuery({
    queryKey: ['student', 'opportunity', 'details', id],
    queryFn: async () => {
      const opportunities = await studentService.searchOpportunities({ opportunityType: 'ALL' });
      return opportunities.find((item) => item.id === id) ?? null;
    },
    enabled: Boolean(id) && isCatalogOpportunity,
  });

  if (!id) return <ErrorState message="No opportunity selected." />;

  if (!isCatalogOpportunity) {
    if (career.isLoading) return <LoadingState message="Loading career details..." />;
    if (career.isError || !career.data) return <ErrorState message="Could not load this career right now. Please return to search results and try again." />;

    return <Section title={career.data.title}>
      <p className="text-sm text-slate-500">Detailed information for the selected career path.</p>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
        <div className="rounded border p-4 bg-white">
          <p className="text-xs uppercase tracking-wide text-slate-500">Industry</p>
          <p className="mt-1 text-sm text-slate-700">{career.data.industry || 'Not specified'}</p>
        </div>
        <div className="rounded border p-4 bg-white">
          <p className="text-xs uppercase tracking-wide text-slate-500">Location</p>
          <p className="mt-1 text-sm text-slate-700">{career.data.location || 'Not specified'}</p>
        </div>
        <div className="rounded border p-4 bg-white">
          <p className="text-xs uppercase tracking-wide text-slate-500">Qualification level</p>
          <p className="mt-1 text-sm text-slate-700">{career.data.qualificationLevel || 'Not specified'}</p>
        </div>
        <div className="rounded border p-4 bg-white">
          <p className="text-xs uppercase tracking-wide text-slate-500">Career ID</p>
          <p className="mt-1 break-all text-sm text-slate-700">{career.data.id}</p>
        </div>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Overview</p>
        <p className="mt-2 text-sm text-slate-700">{career.data.description || 'Detailed overview is not available for this career yet.'}</p>
      </div>
    </Section>;
  }

  if (catalogOpportunity.isLoading) return <LoadingState message="Loading opportunity details..." />;
  if (catalogOpportunity.isError || !catalogOpportunity.data) return <ErrorState message="Could not load this opportunity right now. Please return to search results and try again." />;

  const opportunity = catalogOpportunity.data;

  return <Section title={opportunity.title}>
    <div className="flex items-center gap-2">
      <Badge color={opportunity.type === 'JOB' ? 'emerald' : 'amber'}>{opportunity.type}</Badge>
      {opportunity.recommended ? <span className="rounded bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">Recommended</span> : null}
      {opportunity.saved ? <span className="rounded bg-primary-50 px-2 py-1 text-xs font-medium text-primary-700">Saved</span> : null}
    </div>
    <p className="text-sm text-slate-500">Detailed information for the selected opportunity.</p>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Field</p>
        <p className="mt-1 text-sm text-slate-700">{opportunity.field || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Industry</p>
        <p className="mt-1 text-sm text-slate-700">{opportunity.industry || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Location</p>
        <p className="mt-1 text-sm text-slate-700">{opportunity.location || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Qualification level</p>
        <p className="mt-1 text-sm text-slate-700">{opportunity.qualification || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white">
        <p className="text-xs uppercase tracking-wide text-slate-500">Demand</p>
        <p className="mt-1 text-sm text-slate-700">{opportunity.demand || 'Not specified'}</p>
      </div>
      <div className="rounded border p-4 bg-white sm:col-span-2">
        <p className="text-xs uppercase tracking-wide text-slate-500">Opportunity ID</p>
        <p className="mt-1 break-all text-sm text-slate-700">{opportunity.id}</p>
      </div>
    </div>
  </Section>;
};

export const StudentApplicationsPage = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ q: '', qualificationLevel: '', region: '', eligibility: '' });
  const apps = useAppQuery({ queryKey: ['apps'], queryFn: applicationService.listMine });
  const bursaries = useAppQuery({ queryKey: ['burs-search', filters], queryFn: () => bursaryService.search({ q: filters.q, qualification: filters.qualificationLevel, region: filters.region, eligibility: filters.eligibility }) });
  const saved = useAppQuery({ queryKey: ['saved-bursary-ids'], queryFn: studentService.savedBursaries });
  const recommendations = useAppQuery({ queryKey: ['recs-bursary-finder'], queryFn: bursaryService.recommended });
  const toggle = useMutation({ mutationFn: ({ id, exists }: { id: string; exists: boolean }) => exists ? studentService.unsaveBursary(id) : studentService.saveBursary(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['saved-bursary-ids'] }) });
  return <Section title="Bursary Finder">
    <div className="rounded border p-3">
      <h3 className="font-semibold mb-2">AI Recommended Bursaries</h3>
      {(recommendations.data ?? []).slice(0, 3).map((item) => <p key={item.externalId}>â€¢ {item.title} ({item.relevanceScore}%)</p>)}
      {!((recommendations.data ?? []).length) && <p className="text-sm text-slate-500">No AI bursary suggestions yet. Complete your profile for better matches.</p>}
    </div>
    <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-2">
      <Input placeholder="Search bursaries" value={filters.q} onChange={(e) => setFilters((s) => ({ ...s, q: e.target.value }))} />
      <Input placeholder="Qualification" value={filters.qualificationLevel} onChange={(e) => setFilters((s) => ({ ...s, qualificationLevel: e.target.value }))} />
      <Input placeholder="Region" value={filters.region} onChange={(e) => setFilters((s) => ({ ...s, region: e.target.value }))} />
      <Input placeholder="Eligibility" value={filters.eligibility} onChange={(e) => setFilters((s) => ({ ...s, eligibility: e.target.value }))} />
    </div>
    {((bursaries.data?.items ?? []) as Array<any>).map((b) => {
      const exists = (saved.data ?? []).includes(b.id);
      return <div key={b.id} className="flex justify-between border p-2 rounded"><span>{b.title} - {b.region}</span><div className="space-x-2"><Button onClick={() => toggle.mutate({ id: b.id, exists })}>{exists ? 'Saved' : 'Bookmark'}</Button><Button onClick={() => applicationService.submit(b.id)}>Apply</Button></div></div>;
    })}
    <p className="font-medium">My applications: {(apps.data ?? []).length}</p>
  </Section>;
};

const fallbackPsychometricDimensions = [
  { key: 'analytical', label: 'Analytical thinking' },
  { key: 'communication', label: 'Communication' },
  { key: 'creativity', label: 'Creativity' },
  { key: 'leadership', label: 'Leadership' },
  { key: 'technical', label: 'Technical confidence' },
];

export const StudentPsychometricPage = () => {
  const qc = useQueryClient();
  const assessments = useAppQuery({ queryKey: ['psychometric', 'assessments'], queryFn: psychometricService.assessments });
  const latest = useAppQuery({ queryKey: ['psychometric', 'latest'], queryFn: psychometricService.latestStudent, retry: false });
  const [selectedAssessmentId, setSelectedAssessmentId] = useState<string>('');
  useEffect(() => {
    if (!selectedAssessmentId && assessments.data?.length) {
      setSelectedAssessmentId(assessments.data[0].id);
    }
  }, [assessments.data, selectedAssessmentId]);
  const questions = useAppQuery({
    queryKey: ['psychometric', 'questions', selectedAssessmentId],
    queryFn: () => psychometricService.assessmentQuestions(selectedAssessmentId),
    enabled: Boolean(selectedAssessmentId),
  });
  const history = useAppQuery({
    queryKey: ['psychometric', 'history', selectedAssessmentId],
    queryFn: () => psychometricService.assessmentHistory(selectedAssessmentId),
    enabled: Boolean(selectedAssessmentId),
  });
  const [scores, setScores] = useState<Record<string, number>>({
    analytical: 3,
    communication: 3,
    creativity: 3,
    leadership: 3,
    technical: 3,
  });
  const [questionScores, setQuestionScores] = useState<Record<string, number>>({});
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);
  const assessmentQuestions = (questions.data ?? []).slice(0, 25);
  useEffect(() => {
    setQuestionScores({});
    setActiveQuestionIndex(0);
  }, [selectedAssessmentId]);
  const toAttemptResultFromLegacy = (legacyResult: NonNullable<Awaited<ReturnType<typeof psychometricService.latestStudent>>>) => ({
    attemptId: legacyResult.id,
    assessmentId: selectedAssessmentId || 'LEGACY',
    scores: legacyResult.scores,
    strengthAreas: legacyResult.strengthAreas,
    growthAreas: legacyResult.growthAreas,
    interpretation: legacyResult.interpretation,
    submittedAt: legacyResult.createdAt,
  });
  const submitLegacy = (answers: Array<{ dimension: string; score: number }>) =>
    psychometricService.submitStudent(answers).then((legacyResult) => toAttemptResultFromLegacy(legacyResult));
  const submit = useMutation({
    mutationFn: async () => {
      if (selectedAssessmentId && assessmentQuestions.length) {
        const attemptAnswers = assessmentQuestions.map((question) => ({
          questionId: question.id,
          score: questionScores[question.id] ?? Math.round((question.minScore + question.maxScore) / 2),
        }));
        try {
          return await psychometricService.submitAssessmentAttempt(selectedAssessmentId, attemptAnswers);
        } catch {
          return submitLegacy(assessmentQuestions.map((question) => ({
            dimension: question.dimensionKey,
            score: questionScores[question.id] ?? Math.round((question.minScore + question.maxScore) / 2),
          })));
        }
      }
      return submitLegacy(Object.entries(scores).map(([dimension, score]) => ({ dimension, score })));
    },
    onSuccess: () => {
      if (selectedAssessmentId) {
        qc.invalidateQueries({ queryKey: ['psychometric', 'history', selectedAssessmentId] });
      }
      qc.invalidateQueries({ queryKey: ['psychometric', 'latest'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
    },
  });
  useEffect(() => {
    if (!assessmentQuestions.length) return;
    if (activeQuestionIndex >= assessmentQuestions.length) {
      setActiveQuestionIndex(assessmentQuestions.length - 1);
    }
  }, [activeQuestionIndex, assessmentQuestions.length]);

  const totalQuestionCount = assessmentQuestions.length || fallbackPsychometricDimensions.length;
  const answeredQuestionCount = assessmentQuestions.length
    ? assessmentQuestions.filter((question) => questionScores[question.id] != null).length
    : fallbackPsychometricDimensions.filter((dimension) => scores[dimension.key] != null).length;
  const progressPercent = Math.round((answeredQuestionCount / Math.max(totalQuestionCount, 1)) * 100);
  const currentQuestion = assessmentQuestions.length ? assessmentQuestions[Math.min(activeQuestionIndex, assessmentQuestions.length - 1)] : null;
  const canSubmitAssessment = assessmentQuestions.length ? answeredQuestionCount === assessmentQuestions.length : true;

  return <Section title="Psychometric Test">
    <p className="text-sm text-slate-600">Complete this quick assessment to improve guidance and learning recommendations.</p>
    {!!assessments.data?.length && (
      <div className="rounded border p-3">
        <p className="text-sm font-medium">Assessment</p>
        <select
          value={selectedAssessmentId}
          onChange={(event) => setSelectedAssessmentId(event.target.value)}
          className="mt-2 w-full rounded border border-slate-300 px-3 py-2 text-sm"
        >
          {assessments.data.map((assessment) => (
            <option key={assessment.id} value={assessment.id}>{assessment.name}</option>
          ))}
        </select>
      </div>
    )}
    <div className="rounded border p-4 space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium">Progress</p>
        <p className="text-xs text-slate-600">{answeredQuestionCount}/{totalQuestionCount} answered ({progressPercent}%)</p>
      </div>
      <div className="h-2 w-full rounded-full bg-slate-200">
        <div className="h-full rounded-full bg-primary-600 transition-all" style={{ width: `${progressPercent}%` }} />
      </div>
      <p className="text-xs text-slate-500">This assessment is capped at 25 questions for concise and accurate guidance.</p>
    </div>
    {assessmentQuestions.length ? (
      <div className="space-y-3">
        {currentQuestion ? (
          <div className="rounded border p-4 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium">Question {activeQuestionIndex + 1} of {assessmentQuestions.length}</p>
              <Badge color="blue">{currentQuestion.dimensionKey}</Badge>
            </div>
            <p className="text-sm text-slate-800">{currentQuestion.prompt}</p>
            <input
              type="range"
              min={currentQuestion.minScore}
              max={currentQuestion.maxScore}
              value={questionScores[currentQuestion.id] ?? Math.round((currentQuestion.minScore + currentQuestion.maxScore) / 2)}
              onChange={(event) => setQuestionScores((state) => ({ ...state, [currentQuestion.id]: Number(event.target.value) }))}
              className="w-full"
            />
            <p className="text-xs text-slate-500">
              Score: {questionScores[currentQuestion.id] ?? Math.round((currentQuestion.minScore + currentQuestion.maxScore) / 2)} / {currentQuestion.maxScore}
            </p>
            <div className="flex flex-wrap justify-between gap-2">
              <Button
                onClick={() => setActiveQuestionIndex((index) => Math.max(index - 1, 0))}
                disabled={activeQuestionIndex === 0}
                className="bg-slate-700 hover:bg-slate-600"
              >
                Previous
              </Button>
              <Button
                onClick={() => setActiveQuestionIndex((index) => Math.min(index + 1, assessmentQuestions.length - 1))}
                disabled={activeQuestionIndex >= assessmentQuestions.length - 1}
              >
                Next
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    ) : (
      <div className="space-y-3">
        {fallbackPsychometricDimensions.map((dimension) => ({
          key: dimension.key,
          label: dimension.label,
          dimension: dimension.key,
          min: 1,
          max: 5,
          value: scores[dimension.key] ?? 3,
          onChange: (value: number) => setScores((state) => ({ ...state, [dimension.key]: value })),
        })).map((item) => <div key={item.key} className="rounded border p-3">
          <p className="text-sm font-medium">{item.label}</p>
          <p className="text-xs text-slate-500">{item.dimension}</p>
          <input
            type="range"
            min={item.min}
            max={item.max}
            value={item.value}
            onChange={(event) => item.onChange(Number(event.target.value))}
            className="mt-2 w-full"
          />
          <p className="text-xs text-slate-500">Score: {item.value} / {item.max}</p>
        </div>)}
      </div>
    )}
    {!canSubmitAssessment ? <p className="text-xs text-amber-700">Answer every question before submitting your assessment.</p> : null}
    <Button onClick={() => submit.mutate()} disabled={submit.isPending || !canSubmitAssessment}>{submit.isPending ? 'Submitting...' : 'Submit Psychometric Test'}</Button>
    {submit.isError ? <p className="text-sm text-red-600">Could not submit the test right now. Please retry.</p> : null}
    {history.data?.[0] ? <div className="rounded border p-4 bg-slate-50">
      <p className="font-medium">Latest assessment result</p>
      <p className="mt-1 text-sm text-slate-700">{history.data[0].interpretation}</p>
      <p className="mt-3 text-sm"><span className="font-medium">Strength areas:</span> {(history.data[0].strengthAreas ?? []).join(', ') || 'None yet'}</p>
      <p className="text-sm"><span className="font-medium">Growth areas:</span> {(history.data[0].growthAreas ?? []).join(', ') || 'None yet'}</p>
    </div> : null}
    {latest.data ? <div className="rounded border p-4 bg-slate-50">
      <p className="font-medium">Latest interpretation</p>
      <p className="mt-1 text-sm text-slate-700">{latest.data.interpretation}</p>
      <p className="mt-3 text-sm"><span className="font-medium">Strength areas:</span> {(latest.data.strengthAreas ?? []).join(', ') || 'None yet'}</p>
      <p className="text-sm"><span className="font-medium">Growth areas:</span> {(latest.data.growthAreas ?? []).join(', ') || 'None yet'}</p>
    </div> : null}
  </Section>;
};

export const StudentLearningCentrePage = () => {
  type LearningTab = 'Books' | 'Study Materials' | 'Video Lessons' | 'Quizzes' | 'Coding Tutorials' | 'Past Papers';
  type LearningCentreResource = {
    id: string;
    title: string;
    description: string;
    category: string;
    subject: string;
    grade: string;
    level: 'Beginner' | 'Intermediate' | 'Advanced';
    resourceType: 'Course' | 'Past Paper' | 'Revision Guide' | 'Worksheet' | 'Video Tutorial' | 'PDF Notes' | 'Book' | 'Study Guide' | 'Quiz';
    duration: string;
    progress: number;
    instructor: string;
    lessons: string[];
    provider: string;
    isFree: boolean;
    sourceUrl?: string;
  };
  type YouTubeVideo = {
    id: string;
    title: string;
    description: string;
    channelTitle: string;
    thumbnailUrl: string;
    subject: string;
    grade: string;
    phase: string;
    term: string;
    topic: string;
  };

  const qc = useQueryClient();
  const { user } = useAuth();
  const recommended = useAppQuery<LearningResource[]>({ queryKey: ['learning', 'recommended'], queryFn: () => learningService.recommended() });
  const catalogue = useAppQuery<LearningResource[]>({ queryKey: ['learning', 'catalogue'], queryFn: learningService.catalogue });
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('All Categories');
  const [subject, setSubject] = useState('All Subjects');
  const [providerFilter, setProviderFilter] = useState('All Providers');
  const [grade, setGrade] = useState('All Grades');
  const [skillLevel, setSkillLevel] = useState('All Levels');
  const [resourceType, setResourceType] = useState('All Resource Types');
  const [freeOnly, setFreeOnly] = useState(true);
  const [selectedResource, setSelectedResource] = useState<LearningCentreResource | null>(null);
  const [isResourceModalOpen, setIsResourceModalOpen] = useState(false);
  const resourceModalTriggerRef = useRef<HTMLButtonElement | null>(null);
  const [activeTab, setActiveTab] = useState<LearningTab>('Study Materials');
  const [youtubeKeyword, setYoutubeKeyword] = useState('');
  const [youtubePhase, setYoutubePhase] = useState('All Phases');
  const [youtubeSubject, setYoutubeSubject] = useState('All Subjects');
  const [youtubeGrade, setYoutubeGrade] = useState('All Grades');
  const [youtubeTerm, setYoutubeTerm] = useState('All Terms');
  const [youtubeTopic, setYoutubeTopic] = useState('');
  const [youtubeSearchToken, setYoutubeSearchToken] = useState(1);
  const [youtubeVideos, setYoutubeVideos] = useState<YouTubeVideo[]>([]);
  const [youtubeLoading, setYoutubeLoading] = useState(false);
  const [youtubeError, setYoutubeError] = useState<string | null>(null);
  const externalQuery = search.trim() || (subject !== 'All Subjects' ? subject : categoryFilter !== 'All Categories' ? categoryFilter : 'study materials');
  const booksQuery = useAppQuery<LearningResource[]>({
    queryKey: ['learning', 'books', externalQuery],
    enabled: activeTab === 'Books' || activeTab === 'Study Materials',
    queryFn: () => learningService.books(externalQuery),
    retry: false,
  });
  const googleBooksQuery = useAppQuery<LearningResource[]>({
    queryKey: ['learning', 'google-books', externalQuery],
    enabled: activeTab === 'Study Materials',
    queryFn: () => learningService.googleBooks(externalQuery),
    retry: false,
  });
  const videosQuery = useAppQuery<LearningResource[]>({
    queryKey: ['learning', 'videos', externalQuery],
    enabled: activeTab === 'Video Lessons' || activeTab === 'Coding Tutorials',
    queryFn: () => learningService.videos(activeTab === 'Coding Tutorials' ? `${externalQuery} coding tutorial` : externalQuery),
    retry: false,
  });
  const quizzesQuery = useAppQuery<LearningResource[]>({
    queryKey: ['learning', 'quizzes', 10],
    enabled: activeTab === 'Quizzes',
    queryFn: () => learningService.quizzes(10),
    retry: false,
  });
  const isAdmin = user?.roles?.includes('ROLE_ADMIN') ?? false;
  const refreshCourses = useMutation({
    mutationFn: learningService.refreshCourses,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['learning'] }),
  });


  const normalize = (v: string) => v.trim().toLowerCase();
  const mapApiType = (value?: string) => {
    const clean = normalize(value ?? '');
    if (clean.includes('quiz')) return 'Quiz';
    if (clean.includes('book')) return 'Book';
    if (clean.includes('study')) return 'Study Guide';
    if (clean.includes('course')) return 'Course';
    if (clean.includes('past') || clean.includes('paper')) return 'Past Paper';
    if (clean.includes('video')) return 'Video Tutorial';
    if (clean.includes('worksheet')) return 'Worksheet';
    if (clean.includes('guide')) return 'Revision Guide';
    if (clean.includes('note')) return 'PDF Notes';
    return 'Course';
  };
  const mapApiLevel = (value?: string) => {
    const clean = normalize(value ?? '');
    if (clean.includes('advanced')) return 'Advanced';
    if (clean.includes('beginner')) return 'Beginner';
    return 'Intermediate';
  };

  const fromApi: LearningCentreResource[] = (catalogue.data ?? []).map((resource, index) => ({
    id: resource.id || `api-${index}`,
    title: resource.title,
    description: resource.description ?? resource.summary ?? 'Learning resource',
    category: resource.category ?? 'Career Guidance',
    subject: resource.subject ?? resource.category ?? 'Career Guidance',
    grade: resource.grade ?? 'All Grades',
    level: mapApiLevel(resource.level ?? resource.difficulty),
    resourceType: mapApiType(resource.resourceType),
    duration: resource.duration ?? `${resource.estimatedMinutes ?? 60}m`,
    progress: resource.progress ?? 0,
    instructor: resource.instructor ?? resource.provider ?? 'EduRite Learning Team',
    lessons: resource.lessons?.length ? resource.lessons : ['Overview', 'Core concepts', 'Practice set'],
    provider: resource.provider ?? 'EduRite',
    isFree: resource.isFree ?? true,
    sourceUrl: resource.externalUrl ?? resource.url,
  }));
  const recommendedIds = new Set((recommended.data ?? []).map((resource) => resource.id));
  const externalResourcesRaw = [
    ...(booksQuery.data ?? []),
    ...(googleBooksQuery.data ?? []),
    ...(videosQuery.data ?? []),
    ...(quizzesQuery.data ?? []),
  ];
  const externalResources: LearningCentreResource[] = externalResourcesRaw.map((resource, index) => ({
    id: resource.id || `ext-${index}`,
    title: resource.title,
    description: resource.description ?? resource.summary ?? 'Learning resource',
    category: resource.category ?? 'Study Materials',
    subject: resource.subject ?? resource.category ?? 'Study Materials',
    grade: resource.grade ?? 'All Grades',
    level: mapApiLevel(resource.level ?? resource.difficulty),
    resourceType: mapApiType(resource.resourceType),
    duration: resource.duration ?? `${resource.estimatedMinutes ?? 30}m`,
    progress: resource.progress ?? 0,
    instructor: resource.instructor ?? resource.provider ?? 'External Provider',
    lessons: resource.lessons?.length ? resource.lessons : ['Open resource', 'Study key topics', 'Practice'],
    provider: resource.provider ?? 'External Provider',
    isFree: resource.isFree ?? true,
    sourceUrl: resource.externalUrl ?? resource.url,
  }));

  const externalLoading = booksQuery.isFetching || googleBooksQuery.isFetching || videosQuery.isFetching || quizzesQuery.isFetching;
  const externalFailed = booksQuery.isError && googleBooksQuery.isError && videosQuery.isError && quizzesQuery.isError;
  const googleBooksErrorStatus = (() => {
    const err = googleBooksQuery.error as { status?: number; response?: { status?: number } } | null;
    return err?.status ?? err?.response?.status ?? null;
  })();
  const showGoogleBooks502 = (activeTab === 'Books' || activeTab === 'Study Materials')
    && googleBooksQuery.isError
    && googleBooksErrorStatus === 502;
  const studyMaterialsFallback: LearningCentreResource[] = [
    {
      id: 'fallback-study-1',
      title: 'No courses found yet',
      description: 'Try another category or refresh courses.',
      category: 'Study Materials',
      subject: 'Exam Preparation',
      grade: 'All Grades',
      level: 'Beginner',
      resourceType: 'Study Guide',
      duration: '10m',
      progress: 0,
      instructor: 'EduRite Learning Team',
      lessons: ['Refresh courses', 'Switch category', 'Try a broader search'],
      provider: 'EduRite',
      isFree: true,
    },
  ];

  const tabFilteredExternal = externalResources.filter((item) => {
    if (activeTab === 'Books') return item.category === 'Books' || item.resourceType === 'Book';
    if (activeTab === 'Study Materials') return item.category === 'Study Materials' || item.resourceType === 'Study Guide' || item.resourceType === 'Book';
    if (activeTab === 'Video Lessons') return item.resourceType === 'Video Tutorial';
    if (activeTab === 'Quizzes') return item.resourceType === 'Quiz';
    if (activeTab === 'Coding Tutorials') return item.resourceType === 'Video Tutorial' || item.category === 'Video Lessons';
    if (activeTab === 'Past Papers') return item.resourceType === 'Past Paper';
    return true;
  });

  const preferExternalTab = activeTab !== 'Past Papers';
  const resources = preferExternalTab && tabFilteredExternal.length > 0
    ? [...fromApi, ...tabFilteredExternal]
    : preferExternalTab && activeTab === 'Study Materials' && fromApi.length === 0
      ? [...studyMaterialsFallback]
      : [...fromApi];

  const filteredResources = resources.filter((resource) => {
    const query = search.trim().toLowerCase();
    const matchesSearch = !query
      || resource.title.toLowerCase().includes(query)
      || resource.description.toLowerCase().includes(query)
      || resource.category.toLowerCase().includes(query)
      || resource.provider.toLowerCase().includes(query)
      || resource.subject.toLowerCase().includes(query);
    const matchesCategory = categoryFilter === 'All Categories' || resource.category === categoryFilter;
    const matchesSubject = subject === 'All Subjects' || resource.subject === subject;
    const matchesGrade = grade === 'All Grades' || resource.grade === grade;
    const matchesLevel = skillLevel === 'All Levels' || resource.level === skillLevel;
    const matchesProvider = providerFilter === 'All Providers' || resource.provider === providerFilter;
    const matchesType = resourceType === 'All Resource Types' || resource.resourceType === resourceType;
    const matchesFree = !freeOnly || resource.isFree;
    return matchesSearch && matchesCategory && matchesSubject && matchesGrade && matchesLevel && matchesProvider && matchesType && matchesFree;
  });

  const featuredCourses = filteredResources
    .filter((resource) => resource.resourceType === 'Course')
    .sort((left, right) => Number(recommendedIds.has(right.id)) - Number(recommendedIds.has(left.id)))
    .slice(0, 8);
  const continueLearning = featuredCourses.slice(0, 8);
  const featuredResources = filteredResources.slice(0, 12);

  const categoryDefinitions = [
    { label: 'Mathematics', helper: 'Revision packs, formulas, and worked examples.' },
    { label: 'Science', helper: 'Physics, chemistry, and life sciences support.' },
    { label: 'Coding', helper: 'Programming tutorials, practice, and debugging.' },
    { label: 'Business', helper: 'Entrepreneurship, economics, and commercial studies.' },
    { label: 'Engineering', helper: 'Design, mechanics, and technical problem-solving.' },
    { label: 'AI & Technology', helper: 'Digital skills, AI foundations, and systems thinking.' },
    { label: 'Accounting', helper: 'Financial statements, costing, and exam drills.' },
    { label: 'Languages', helper: 'Comprehension, writing, and communication practice.' },
    { label: 'Exam Preparation', helper: 'Study plans, past papers, and revision strategy.' },
    { label: 'Career Guidance', helper: 'Career planning, pathways, and learner support.' },
  ] as const;
  const liveCategoryCounts = fromApi.filter((resource) => resource.resourceType === 'Course').reduce<Record<string, number>>((acc, resource) => {
    acc[resource.category] = (acc[resource.category] ?? 0) + 1;
    return acc;
  }, {});
  const categoryOptions = categoryDefinitions.map(({ label, helper }) => ({
    label,
    helper,
    count: liveCategoryCounts[label] ?? 0,
  }));
  const selectedCategoryCount = categoryFilter === 'All Categories' ? fromApi.length : (liveCategoryCounts[categoryFilter] ?? 0);
  const aiTools = [
    { title: 'AI Tutor', description: 'Get instant support for study questions.', to: '/student/ai-tutor', icon: Sparkles },
    { title: 'Career Path Coach', description: 'Map learning to career outcomes.', to: '/student/recommendations/careers', icon: Target },
    { title: 'CV Builder', description: 'Turn skills into a job-ready CV.', to: '/student/cv-builder', icon: FileText },
    { title: 'Interview Prep', description: 'Practice common interview questions.', to: '/student/interview-prep', icon: Brain },
  ];
  const progressRings = [
    { label: 'Courses', value: Math.round(resources.filter((item) => item.resourceType === 'Course').reduce((acc, item) => acc + item.progress, 0) / Math.max(1, resources.filter((item) => item.resourceType === 'Course').length)), color: '#2563eb' },
    { label: 'Papers', value: Math.round(resources.filter((item) => item.resourceType === 'Past Paper').reduce((acc, item) => acc + item.progress, 0) / Math.max(1, resources.filter((item) => item.resourceType === 'Past Paper').length)), color: '#0ea5e9' },
    { label: 'Overall', value: Math.round(resources.reduce((acc, item) => acc + item.progress, 0) / Math.max(1, resources.length)), color: '#14b8a6' },
  ];
  const streakDays = 9;

  const clearFilters = () => {
    setSearch('');
    setCategoryFilter('All Categories');
    setSubject('All Subjects');
    setProviderFilter('All Providers');
    setGrade('All Grades');
    setSkillLevel('All Levels');
    setResourceType('All Resource Types');
    setFreeOnly(true);
  };

  const closeResourceModal = () => {
    setIsResourceModalOpen(false);
    setSelectedResource(null);
    window.requestAnimationFrame(() => resourceModalTriggerRef.current?.focus());
  };
  const openDetails = (resource: LearningCentreResource, trigger?: HTMLButtonElement | null) => {
    resourceModalTriggerRef.current = trigger ?? null;
    setSelectedResource(resource);
    setIsResourceModalOpen(true);
  };
  const applyQuickFilter = (type?: LearningCentreResource['resourceType'], category?: string) => {
    setSearch('');
    setCategoryFilter(category ?? 'All Categories');
    setSubject('All Subjects');
    setProviderFilter('All Providers');
    setGrade('All Grades');
    setSkillLevel('All Levels');
    setResourceType(type ?? 'All Resource Types');
    setFreeOnly(true);
  };
  const activeFilters = [
    categoryFilter !== 'All Categories' ? `Category: ${categoryFilter}` : null,
    subject !== 'All Subjects' ? `Subject: ${subject}` : null,
    providerFilter !== 'All Providers' ? `Provider: ${providerFilter}` : null,
    grade !== 'All Grades' ? `Grade: ${grade}` : null,
    skillLevel !== 'All Levels' ? `Level: ${skillLevel}` : null,
    resourceType !== 'All Resource Types' ? `Type: ${resourceType}` : null,
    freeOnly ? 'Free only' : null,
    search.trim() ? `Search: "${search.trim()}"` : null,
  ].filter(Boolean) as string[];
  const youtubeApiKey = import.meta.env.VITE_YOUTUBE_API_KEY;
  const learnerYoutubeMessage = 'Video lessons are currently being updated. Please check back soon.';
  const youtubeRestrictedMessage = 'YouTube access is currently restricted. Please contact support.';
  const youtubeConfigurationMessage = 'YouTube is not configured correctly right now. Please contact support.';

  useEffect(() => {
    if (!isResourceModalOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsResourceModalOpen(false);
        setSelectedResource(null);
        window.requestAnimationFrame(() => resourceModalTriggerRef.current?.focus());
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isResourceModalOpen]);
  const youtubeNoResultsMessage = 'No videos found for these filters. Try changing the subject, grade, or topic.';
  const capsSeniorSubjects = [
    'Home Language',
    'First Additional Language',
    'Second Additional Language',
    'Mathematics',
    'Natural Sciences',
    'Technology',
    'Social Sciences',
    'Economic and Management Sciences',
    'Life Orientation',
    'Creative Arts',
    'Coding and Robotics',
  ];
  const capsFetSubjects = [
    'Home Language',
    'First Additional Language',
    'Second Additional Language',
    'Mathematics',
    'Mathematical Literacy',
    'Physical Sciences',
    'Life Sciences',
    'Accounting',
    'Business Studies',
    'Economics',
    'Geography',
    'History',
    'Life Orientation',
    'Agricultural Sciences',
    'Computer Applications Technology',
    'Information Technology',
    'Engineering Graphics and Design',
    'Civil Technology',
    'Electrical Technology',
    'Mechanical Technology',
    'Tourism',
    'Consumer Studies',
    'Hospitality Studies',
    'Visual Arts',
    'Dramatic Arts',
    'Music',
    'Dance Studies',
    'Design',
    'Religion Studies',
  ];
  const youtubeSubjects = Array.from(new Set([...capsSeniorSubjects, ...capsFetSubjects]));
  const subjectsForPhase = youtubePhase === 'Senior Phase'
    ? capsSeniorSubjects
    : youtubePhase === 'FET Phase'
      ? capsFetSubjects
      : youtubeSubjects;

  useEffect(() => {
    const controller = new AbortController();
    const inferTerm = (text: string) => {
      const match = text.match(/\bterm\s*([1-4])\b/i);
      return match ? `Term ${match[1]}` : (youtubeTerm === 'All Terms' ? 'Term not specified' : youtubeTerm);
    };
    const inferGrade = (text: string) => {
      const match = text.match(/\bgrade\s*(7|8|9|10|11|12)\b/i);
      return match ? `Grade ${match[1]}` : (youtubeGrade === 'All Grades' ? 'Grade not specified' : youtubeGrade);
    };
    const inferPhase = (gradeValue: string) => {
      if (gradeValue.includes('7') || gradeValue.includes('8') || gradeValue.includes('9')) return 'Senior Phase';
      if (gradeValue.includes('10') || gradeValue.includes('11') || gradeValue.includes('12')) return 'FET Phase';
      return youtubePhase === 'All Phases' ? 'Phase not specified' : youtubePhase;
    };
    const inferSubject = (text: string) => {
      const found = youtubeSubjects.find((value) => text.toLowerCase().includes(value.toLowerCase()));
      if (found) return found;
      return youtubeSubject === 'All Subjects' ? 'General CAPS' : youtubeSubject;
    };
    const buildTopic = (text: string) => {
      if (youtubeTopic.trim()) return youtubeTopic.trim();
      const compact = text.replace(/\s+/g, ' ').trim();
      return compact.length > 70 ? `${compact.slice(0, 67)}...` : compact || 'CAPS topic';
    };
    const matchesFilters = (video: YouTubeVideo) => {
      if (youtubePhase !== 'All Phases' && video.phase !== youtubePhase) return false;
      if (youtubeGrade !== 'All Grades' && video.grade !== youtubeGrade) return false;
      if (youtubeSubject !== 'All Subjects' && video.subject !== youtubeSubject) return false;
      if (youtubeTerm !== 'All Terms' && video.term !== youtubeTerm) return false;
      if (youtubeTopic.trim() && !`${video.title} ${video.description} ${video.topic}`.toLowerCase().includes(youtubeTopic.trim().toLowerCase())) return false;
      return !(youtubeKeyword.trim() && !`${video.title} ${video.description} ${video.channelTitle}`.toLowerCase().includes(youtubeKeyword.trim().toLowerCase()));
    };
    const run = async () => {
      if (!youtubeApiKey) {
        console.warn('Learning Centre: VITE_YOUTUBE_API_KEY is missing during frontend runtime.');
        setYoutubeError(learnerYoutubeMessage);
        setYoutubeVideos([]);
        return;
      }

      const resolvedPhase = youtubePhase !== 'All Phases'
        ? youtubePhase
        : (youtubeGrade.startsWith('Grade 7') || youtubeGrade.startsWith('Grade 8') || youtubeGrade.startsWith('Grade 9')
          ? 'Senior Phase'
          : youtubeGrade !== 'All Grades'
            ? 'FET Phase'
            : 'CAPS');
      const resolvedGrade = youtubeGrade !== 'All Grades' ? youtubeGrade : 'Grade 12';
      const resolvedSubject = youtubeSubject !== 'All Subjects' ? youtubeSubject : 'Mathematics';
      const resolvedTerm = youtubeTerm !== 'All Terms' ? youtubeTerm : '';
      const resolvedTopic = youtubeTopic.trim();
      const resolvedKeyword = youtubeKeyword.trim();
      const youtubeQuery = [
        'CAPS',
        resolvedPhase,
        resolvedGrade,
        resolvedSubject,
        resolvedTerm,
        resolvedTopic,
        resolvedKeyword,
        'South Africa lesson',
      ]
        .filter(Boolean)
        .join(' ')
        .replace(/\s+/g, ' ')
        .trim();

      setYoutubeLoading(true);
      setYoutubeError(null);
      try {
        const params = new URLSearchParams({
          part: 'snippet',
          type: 'video',
          maxResults: '12',
          q: youtubeQuery,
          key: youtubeApiKey,
          safeSearch: 'strict',
          videoEmbeddable: 'true',
          relevanceLanguage: 'en',
          regionCode: 'ZA',
        });
        const response = await fetch(`https://www.googleapis.com/youtube/v3/search?${params.toString()}`, { signal: controller.signal });
        let errorPayload: { error?: { message?: string; errors?: Array<{ reason?: string; message?: string }> } } | null = null;
        if (!response.ok) {
          try {
            errorPayload = await response.json();
          } catch {
            errorPayload = null;
          }
        }
        const errorReason = errorPayload?.error?.errors?.[0]?.reason ?? '';
        const errorMessage = errorPayload?.error?.message ?? '';
        if (response.status === 400 || response.status === 401) {
          console.error('Learning Centre YouTube configuration error.', { query: youtubeQuery, status: response.status, errorReason, errorMessage });
          setYoutubeError(youtubeConfigurationMessage);
          setYoutubeVideos([]);
          return;
        }
        if (response.status === 403) {
          console.error('Learning Centre YouTube access restricted.', { query: youtubeQuery, status: response.status, errorReason, errorMessage });
          setYoutubeError(youtubeRestrictedMessage);
          setYoutubeVideos([]);
          return;
        }
        if (!response.ok) {
          console.error('Learning Centre YouTube request failed.', { query: youtubeQuery, status: response.status, errorReason, errorMessage });
          setYoutubeError(learnerYoutubeMessage);
          setYoutubeVideos([]);
          return;
        }
        const payload = await response.json() as {
          items?: Array<{
            id?: { videoId?: string };
            snippet?: {
              title?: string;
              description?: string;
              channelTitle?: string;
              thumbnails?: { high?: { url?: string }; medium?: { url?: string }; default?: { url?: string } };
            };
          }>;
        };
        const videos = (payload.items ?? [])
          .map((item) => {
            const videoId = item.id?.videoId;
            if (!videoId || !item.snippet) return null;
            const title = item.snippet.title ?? 'Untitled video';
            const description = item.snippet.description ?? 'No description available.';
            const merged = `${title} ${description}`;
            const inferredGrade = inferGrade(merged);
            const inferredTerm = inferTerm(merged);
            return {
              id: videoId,
              title,
              description,
              channelTitle: item.snippet.channelTitle ?? 'Unknown channel',
              thumbnailUrl: item.snippet.thumbnails?.high?.url
                ?? item.snippet.thumbnails?.medium?.url
                ?? item.snippet.thumbnails?.default?.url
                ?? '',
              subject: youtubeSubject !== 'All Subjects' ? youtubeSubject : inferSubject(merged),
              grade: youtubeGrade !== 'All Grades' ? youtubeGrade : inferredGrade,
              phase: youtubePhase !== 'All Phases' ? youtubePhase : inferPhase(inferredGrade),
              term: youtubeTerm !== 'All Terms' ? youtubeTerm : inferredTerm,
              topic: resolvedTopic || buildTopic(title),
            } satisfies YouTubeVideo;
          })
          .filter((item): item is YouTubeVideo => Boolean(item));
        const unique = Array.from(new Map(videos.map((video) => [video.id, video])).values());
        const filteredVideos = unique.filter(matchesFilters);
        if (filteredVideos.length === 0) {
          setYoutubeError(youtubeNoResultsMessage);
          setYoutubeVideos([]);
          return;
        }
        setYoutubeVideos(filteredVideos);
      } catch (error) {
        if ((error as Error).name === 'AbortError') return;
        console.error('Learning Centre YouTube fetch failed.', error);
        setYoutubeError(youtubeRestrictedMessage);
        setYoutubeVideos([]);
      } finally {
        setYoutubeLoading(false);
      }
    };
    run();
    return () => controller.abort();
  }, [learnerYoutubeMessage, youtubeApiKey, youtubeConfigurationMessage, youtubeGrade, youtubeKeyword, youtubeNoResultsMessage, youtubePhase, youtubeRestrictedMessage, youtubeSearchToken, youtubeSubject, youtubeTerm, youtubeTopic]);

  if (recommended.isLoading || catalogue.isLoading) return <LoadingState message="Loading your premium learning hub..." />;
  if (recommended.isError && catalogue.isError) return <ErrorState message="Could not load learning centre resources right now." />;

  return <section className="student-page-section">
    <div className="rounded-2xl bg-gradient-to-r from-[#081739] via-[#0d2a63] to-[#13408f] p-5 text-white shadow-2xl shadow-blue-950/30 sm:p-7">
      <div className="space-y-4">
        <div>
          <h1 className="text-2xl font-bold sm:text-3xl lg:text-4xl">Learning Centre</h1>
          <p className="mt-2 max-w-3xl text-sm text-blue-100 sm:text-base">Personalized learning resources powered by EduRite AI.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button className="w-full cursor-pointer rounded-2xl bg-white/15 text-white backdrop-blur hover:bg-white/25 sm:w-auto" onClick={() => applyQuickFilter('Course')}>Browse Courses</Button>
          <Link to="/student/ai-tutor" className="w-full sm:w-auto">
            <Button className="w-full cursor-pointer rounded-2xl bg-white/10 text-white hover:bg-white/20">AI Study Assistant</Button>
          </Link>
          <Button className="w-full cursor-pointer rounded-2xl bg-white/10 text-white hover:bg-white/20 sm:w-auto" onClick={() => { setActiveTab('Past Papers'); applyQuickFilter('Past Paper'); }}>Past Papers</Button>
          <Button className="w-full cursor-pointer rounded-2xl bg-white/10 text-white hover:bg-white/20 sm:w-auto" onClick={() => { setActiveTab('Coding Tutorials'); applyQuickFilter(undefined, 'Coding'); }}>Coding Tutorials</Button>
        </div>
      </div>
    </div>

    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <div className="flex flex-wrap gap-2">
        {(['Books', 'Study Materials', 'Video Lessons', 'Quizzes', 'Coding Tutorials', 'Past Papers'] as LearningTab[]).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={`rounded-xl px-3 py-2 text-sm font-semibold transition ${activeTab === tab ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
          >
            {tab}
          </button>
        ))}
      </div>
      {externalLoading ? <p className="mt-3 text-sm text-slate-500">Loading live learning content...</p> : null}
      {externalFailed ? <p className="mt-3 text-sm text-amber-700">Live providers are unavailable right now. Showing EduRite fallback resources.</p> : null}
      {!externalLoading && preferExternalTab && tabFilteredExternal.length === 0 ? <p className="mt-3 text-sm text-slate-500">No live results found for this tab. Showing fallback resources.</p> : null}
    </div>

    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search courses, resources, and learning paths..." />
      <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <select value={subject} onChange={(event) => setSubject(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm">
          <option>All Subjects</option><option>Mathematics</option><option>Science</option><option>Coding</option><option>Business</option><option>Engineering</option><option>AI & Technology</option><option>Accounting</option><option>Languages</option><option>Exam Preparation</option>
        </select>
        <select value={grade} onChange={(event) => setGrade(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm">
          <option>All Grades</option><option>Grade 8</option><option>Grade 9</option><option>Grade 10</option><option>Grade 11</option><option>Grade 12</option>
        </select>
        <select value={skillLevel} onChange={(event) => setSkillLevel(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm">
          <option>All Levels</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option>
        </select>
        <select value={resourceType} onChange={(event) => setResourceType(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm">
          <option>All Resource Types</option><option>Course</option><option>Past Paper</option><option>Revision Guide</option><option>Video Tutorial</option><option>Worksheet</option><option>PDF Notes</option>
        </select>
      </div>
      <div className="mt-3 flex">
        <Button className="w-full cursor-pointer rounded-xl bg-slate-700 hover:bg-slate-600 sm:w-auto" onClick={clearFilters}>Clear Filters</Button>
      </div>
    </div>

    <div className="rounded-2xl bg-gradient-to-br from-[#0A0E2B] via-[#10235A] to-[#13408f] p-4 text-white shadow-xl shadow-slate-950/30 sm:p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold sm:text-xl">YouTube Learning Videos</h2>
          <p className="mt-1 text-sm text-blue-100">Browse CAPS-aligned videos by phase, grade, subject, term, and topic.</p>
        </div>
      </div>
      <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
        <select value={youtubePhase} onChange={(event) => setYoutubePhase(event.target.value)} className="w-full rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white">
          <option className="text-slate-900">All Phases</option>
          <option className="text-slate-900">Senior Phase</option>
          <option className="text-slate-900">FET Phase</option>
        </select>
        <select value={youtubeGrade} onChange={(event) => setYoutubeGrade(event.target.value)} className="w-full rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white">
          <option className="text-slate-900">All Grades</option><option className="text-slate-900">Grade 7</option><option className="text-slate-900">Grade 8</option><option className="text-slate-900">Grade 9</option><option className="text-slate-900">Grade 10</option><option className="text-slate-900">Grade 11</option><option className="text-slate-900">Grade 12</option>
        </select>
        <select value={youtubeSubject} onChange={(event) => setYoutubeSubject(event.target.value)} className="w-full rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white">
          <option className="text-slate-900">All Subjects</option>
          {subjectsForPhase.map((item) => <option key={item} className="text-slate-900">{item}</option>)}
        </select>
        <select value={youtubeTerm} onChange={(event) => setYoutubeTerm(event.target.value)} className="w-full rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white">
          <option className="text-slate-900">All Terms</option><option className="text-slate-900">Term 1</option><option className="text-slate-900">Term 2</option><option className="text-slate-900">Term 3</option><option className="text-slate-900">Term 4</option>
        </select>
        <Input
          value={youtubeKeyword}
          onChange={(event) => setYoutubeKeyword(event.target.value)}
          placeholder="Search CAPS videos"
          className="border-white/20 bg-white/10 text-white placeholder:text-blue-200"
        />
        <Input
          value={youtubeTopic}
          onChange={(event) => setYoutubeTopic(event.target.value)}
          placeholder="Topic (e.g. functions, photosynthesis)"
          className="border-white/20 bg-white/10 text-white placeholder:text-blue-200"
        />
        <Button className="cursor-pointer rounded-xl bg-white/20 text-white hover:bg-white/30" onClick={() => setYoutubeSearchToken((value) => value + 1)}>
          Search Videos
        </Button>
      </div>
      {youtubeLoading ? <p className="mt-4 text-sm text-blue-100">Loading YouTube videos...</p> : null}
      {youtubeError ? <p className="mt-4 text-sm text-amber-200">{youtubeError}</p> : null}
      {!youtubeLoading && !youtubeError && youtubeVideos.length === 0 ? (
        <div className="mt-4 rounded-xl border border-white/20 bg-white/10 p-4 text-sm text-blue-100">
          No CAPS-aligned videos found for this subject yet.
        </div>
      ) : null}
      {!youtubeLoading && !youtubeError && youtubeVideos.length > 0 ? (
        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {youtubeVideos.map((video) => (
            <article key={video.id} className="rounded-2xl border border-white/20 bg-white/10 p-3 transition hover:-translate-y-0.5 hover:bg-white/20">
              {video.thumbnailUrl ? <img src={video.thumbnailUrl} alt={video.title} className="h-40 w-full rounded-xl object-cover" /> : <div className="h-40 w-full rounded-xl bg-white/10" />}
              <h4 className="mt-3 line-clamp-2 text-sm font-semibold">{video.title}</h4>
              <p className="mt-1 text-xs text-blue-100">{video.channelTitle}</p>
              <p className="mt-2 line-clamp-3 text-xs text-blue-100">{video.description}</p>
              <div className="mt-3 flex flex-wrap gap-2 text-[11px]">
                <span className="rounded-lg bg-white/15 px-2 py-1">{video.subject}</span>
                <span className="rounded-lg bg-white/15 px-2 py-1">{video.grade}</span>
                <span className="rounded-lg bg-white/15 px-2 py-1">{video.phase}</span>
                <span className="rounded-lg bg-white/15 px-2 py-1">{video.term}</span>
                <span className="rounded-lg bg-white/15 px-2 py-1">{video.topic}</span>
              </div>
              <a
                className="mt-3 inline-flex w-full items-center justify-center rounded-xl bg-white/20 px-3 py-2 text-sm font-semibold text-white hover:bg-white/30"
                href={`https://www.youtube.com/watch?v=${video.id}`}
                target="_blank"
                rel="noopener noreferrer"
              >
                Watch Lesson
              </a>
            </article>
          ))}
        </div>
      ) : null}
    </div>
    <div className="space-y-3">
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-slate-900">Learning Category</h2>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">{selectedCategoryCount} courses</span>
          </div>
          <p className="text-sm text-slate-500">Select a category to narrow the recommended free courses.</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <label className="flex min-w-[240px] flex-col gap-1 text-sm font-medium text-slate-700">
            <span className="sr-only">Learning Category</span>
            <select
              value={categoryFilter}
              onChange={(event) => applyQuickFilter(undefined, event.target.value)}
              className="h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100 sm:min-w-[260px]"
            >
              <option value="All Categories">All Categories ({fromApi.length})</option>
              {categoryOptions.map((category) => (
                <option key={category.label} value={category.label}>
                  {category.label} ({category.count})
                </option>
              ))}
            </select>
          </label>
          <Button className="cursor-pointer rounded-xl bg-slate-700 hover:bg-slate-600" onClick={clearFilters}>Clear Filters</Button>
        </div>
      </div>
    </div>

    <div className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Recommended Courses</h2>
      {!featuredCourses.length ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-6 text-center">
          <h3 className="text-base font-semibold text-slate-900">No courses found yet</h3>
          <p className="mt-2 text-sm text-slate-500">Try another category or refresh courses.</p>
          {activeFilters.length ? <p className="mt-2 text-xs text-slate-600">Active filters: {activeFilters.join(' | ')}</p> : null}
          <div className="mt-4 flex justify-center gap-2">
            <Button className="cursor-pointer rounded-xl" onClick={clearFilters}>Clear Filters</Button>
            {isAdmin ? <Button className="cursor-pointer rounded-xl bg-slate-700 hover:bg-slate-600" disabled={refreshCourses.isPending} onClick={() => refreshCourses.mutate()}>{refreshCourses.isPending ? 'Refreshing...' : 'Refresh Courses'}</Button> : null}
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {featuredCourses.map((course) => <article key={course.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <Badge color="blue">{course.category}</Badge>
              <Badge color="emerald">{course.level}</Badge>
            </div>
            <h3 className="mt-3 line-clamp-2 text-sm font-semibold text-slate-900">{course.title}</h3>
            <p className="mt-1 text-xs font-medium uppercase tracking-wide text-slate-500">{course.provider}</p>
            <p className="mt-2 line-clamp-4 text-sm text-slate-600">{course.description}</p>
            <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-slate-500">
              <span className="rounded-lg bg-slate-100 px-2 py-1">{course.subject}</span>
              <span className="rounded-lg bg-slate-100 px-2 py-1">{course.duration}</span>
              <span className="rounded-lg bg-slate-100 px-2 py-1">Free</span>
            </div>
            <a className="mt-4 inline-flex w-full items-center justify-center rounded-xl bg-primary-600 px-3 py-2 text-sm font-semibold text-white hover:bg-primary-500" href={course.sourceUrl} target="_blank" rel="noopener noreferrer">Open Course</a>
          </article>)}
        </div>
      )}
    </div>

    <div className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Continue Learning</h2>
      <div className="flex gap-3 overflow-x-auto pb-1">
        {continueLearning.map((item) => (
          <article key={item.id} className="min-w-[250px] rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="line-clamp-2 text-sm font-semibold">{item.title}</h3>
            <p className="mt-2 text-xs text-slate-500">Provider: {item.provider}</p>
            <div className="mt-3 h-2 rounded-full bg-slate-200"><div className="h-full rounded-full bg-primary-600" style={{ width: `${Math.max(0, Math.min(100, item.progress))}%` }} /></div>
            <a className="mt-3 inline-flex w-full items-center justify-center rounded-xl bg-primary-600 px-3 py-2 text-sm font-semibold text-white hover:bg-primary-500" href={item.sourceUrl} target="_blank" rel="noopener noreferrer">Open Course</a>
          </article>
        ))}
      </div>
    </div>

    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div className="space-y-3">
        <h2 className="text-lg font-semibold text-slate-900">AI Learning Tools</h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {aiTools.map((tool) => (
            <Link key={tool.title} to={tool.to} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
              <div className="mb-2 inline-flex rounded-xl bg-slate-100 p-2 text-slate-700"><tool.icon size={16} /></div>
              <h3 className="text-sm font-semibold text-slate-900">{tool.title}</h3>
              <p className="mt-1 text-xs text-slate-500">{tool.description}</p>
            </Link>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        <h2 className="text-lg font-semibold text-slate-900">Progress Tracking</h2>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {progressRings.map((ring) => (
              <div key={ring.label} className="flex flex-col items-center">
                <div
                  className="relative h-20 w-20 rounded-full"
                  style={{ background: `conic-gradient(${ring.color} ${ring.value * 3.6}deg, #e2e8f0 0deg)` }}
                >
                  <div className="absolute inset-[7px] flex items-center justify-center rounded-full bg-white text-xs font-semibold text-slate-700">{ring.value}%</div>
                </div>
                <p className="mt-2 text-xs text-slate-600">{ring.label}</p>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-xl bg-slate-50 p-3">
            <p className="text-sm font-medium text-slate-800">Learning streak</p>
            <p className="text-xl font-bold text-slate-900">{streakDays} days</p>
            <p className="text-xs text-slate-500">Keep your momentum to unlock stronger mastery insights.</p>
          </div>
        </div>
      </div>
    </div>

    <div className="space-y-3">
      <h2 className="text-lg font-semibold text-slate-900">Past Papers & Resources</h2>
      {showGoogleBooks502 ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          Books search is temporarily unavailable. You can still use videos, quizzes, coding tutorials, and past papers.
        </div>
      ) : null}
      {!featuredResources.length ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center">
          <PlayCircle size={28} className="mx-auto text-primary-600" />
          <h3 className="mt-3 text-base font-semibold text-slate-900">No matching resources yet</h3>
          <p className="mt-2 text-sm text-slate-500">Try adjusting search terms or filters to discover relevant learning resources.</p>
          {activeFilters.length ? <p className="mt-2 text-xs text-slate-600">Active filters: {activeFilters.join(' | ')}</p> : null}
          <div className="mt-4 flex justify-center">
            <Button className="rounded-xl" onClick={clearFilters}>Clear Filters</Button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {featuredResources.map((resource) => (
            <article key={resource.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md">
              <div className="mb-2 inline-flex rounded-xl bg-slate-100 p-2 text-slate-700">
                {resource.resourceType?.toLowerCase().includes('video') ? <Video size={16} /> : resource.resourceType?.toLowerCase().includes('paper') ? <FileText size={16} /> : <BookOpen size={16} />}
              </div>
              <h3 className="line-clamp-2 text-sm font-semibold">{resource.title}</h3>
              <p className="mt-1 line-clamp-2 text-xs text-slate-500">{resource.description ?? 'Learning resource'}</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {resource.resourceType ? <Badge color="slate">{resource.resourceType}</Badge> : null}
                {resource.category ? <Badge color="blue">{resource.category}</Badge> : null}
              </div>
              <button type="button" className="mt-3 inline-flex cursor-pointer text-sm font-semibold text-primary-600 hover:text-primary-500" onClick={(event) => openDetails(resource, event.currentTarget)}>View Resource</button>
            </article>
          ))}
        </div>
      )}
    </div>
    {isResourceModalOpen && selectedResource ? (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
            closeResourceModal();
          }
        }}
      >
        <div
          className="responsive-modal-panel w-full max-w-3xl rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl sm:p-6"
          role="dialog"
          aria-modal="true"
          aria-labelledby="learning-centre-resource-modal-title"
          onMouseDown={(event) => event.stopPropagation()}
        >
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 id="learning-centre-resource-modal-title" className="text-xl font-bold text-slate-900">{selectedResource.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{selectedResource.description}</p>
            </div>
            <button
              type="button"
              onClick={closeResourceModal}
              className="rounded-xl border border-slate-200 p-2 text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
              aria-label="Close resource modal"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <p className="text-sm"><span className="font-semibold">Instructor:</span> {selectedResource.instructor}</p>
            <p className="text-sm"><span className="font-semibold">Duration:</span> {selectedResource.duration}</p>
            <p className="text-sm"><span className="font-semibold">Level:</span> {selectedResource.level}</p>
            <p className="text-sm"><span className="font-semibold">Category:</span> {selectedResource.category}</p>
            <p className="text-sm"><span className="font-semibold">Resource Type:</span> {selectedResource.resourceType}</p>
            <p className="text-sm"><span className="font-semibold">Progress:</span> {selectedResource.progress}%</p>
          </div>
          <div className="mt-4">
            <p className="text-sm font-semibold text-slate-900">Lesson List</p>
            <ul className="mt-2 space-y-1 text-sm text-slate-700">
              {selectedResource.lessons.map((lesson) => <li key={lesson}>- {lesson}</li>)}
            </ul>
          </div>
          <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:flex-wrap">
            <Button className="w-full cursor-pointer rounded-xl sm:w-auto" onClick={() => setSelectedResource((prev) => prev ? { ...prev, progress: Math.min(100, prev.progress + 10) } : prev)}>
              {selectedResource.progress > 0 ? 'Resume' : 'Start'}
            </Button>
            {selectedResource.sourceUrl ? <a className="inline-flex items-center justify-center rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-primary-700 hover:bg-slate-50" href={selectedResource.sourceUrl} target="_blank" rel="noopener noreferrer">Open External Resource</a> : null}
            <button type="button" onClick={closeResourceModal} className="inline-flex items-center justify-center rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100">Close</button>
          </div>
        </div>
      </div>
    ) : null}
  </section>;
};

export const StudentRewardsPage = () => {
  const qc = useQueryClient();
  const summary = useAppQuery({ queryKey: ['gamification', 'summary'], queryFn: gamificationService.summary });
  const rewardTitle = 'End of Term Reward';
  const rewardDescription = 'Reward claim for consistent engagement this term.';
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    if (!feedback) {
      return undefined;
    }
    const timeout = window.setTimeout(() => setFeedback(null), 2400);
    return () => window.clearTimeout(timeout);
  }, [feedback]);

  const claim = useMutation({
    mutationFn: () => gamificationService.claimReward(rewardTitle, rewardDescription),
    onSuccess: async () => {
      setFeedback({ type: 'success', message: 'Reward claim submitted for review.' });
      await qc.invalidateQueries({ queryKey: ['gamification', 'summary'] });
    },
    onError: (error) => {
      setFeedback({ type: 'error', message: error instanceof Error ? error.message : 'Unable to submit reward claim right now.' });
    },
  });

  if (summary.isLoading) return <LoadingState />;
  if (summary.isError || !summary.data) return <ErrorState message="Could not load points and rewards right now." />;

  const claimState = getRewardClaimState(summary.data.availablePoints, REWARD_CLAIM_COST);

  return <Section title="Points & Rewards">
    {feedback ? (
      <div className={`fixed right-4 top-4 z-50 rounded-2xl border px-4 py-3 text-sm font-medium shadow-lg ${feedback.type === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-800 shadow-emerald-100' : 'border-red-200 bg-red-50 text-red-700 shadow-red-100'}`}>
        {feedback.message}
      </div>
    ) : null}
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      <Card label="Total points" value={summary.data.totalPoints} />
      <Card label="Reserved points" value={summary.data.reservedPoints} />
      <Card label="Available points" value={summary.data.availablePoints} />
    </div>
    <p className="text-sm text-slate-600">Current term: {summary.data.currentTermCode}</p>
    <div className="rounded border p-4 space-y-3">
      <h3 className="font-semibold">Claim end-of-term reward</h3>
      <div className="space-y-1 text-sm">
        <p className="font-medium text-slate-900">{rewardTitle}</p>
        <p className="text-slate-600">{rewardDescription}</p>
      </div>
      <Button
        onClick={() => claim.mutate()}
        disabled={claim.isPending || !claimState.canClaim}
        className="disabled:cursor-not-allowed"
        aria-describedby="reward-claim-help"
        title={!claimState.canClaim ? claimState.helperMessage : undefined}
      >
        {claim.isPending ? 'Claiming...' : claimState.buttonLabel}
      </Button>
      <p id="reward-claim-help" className={`text-sm ${claimState.canClaim ? 'text-slate-600' : 'text-slate-600'}`}>
        {claimState.helperMessage}
      </p>
    </div>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-2">
      <div className="rounded border p-4">
        <h3 className="font-semibold">Recent activity</h3>
        {(summary.data.recentEvents ?? []).map((event, index) => <p key={`${event.awardedAt}-${index}`} className="text-sm">{formatRewardEventSummary(event)}</p>)}
        {!summary.data.recentEvents?.length ? <p className="text-sm text-slate-500">No activity recorded yet.</p> : null}
      </div>
      <div className="rounded border p-4">
        <h3 className="font-semibold">Recent claims</h3>
        {(summary.data.recentClaims ?? []).map((item, index) => <p key={`${item.claimedAt}-${index}`} className="text-sm">{formatRewardClaimSummary(item)}</p>)}
        {!summary.data.recentClaims?.length ? <p className="text-sm text-slate-500">No claims submitted yet.</p> : null}
      </div>
    </div>
  </Section>;
};

type NotificationType = 'INFO' | 'WARNING' | 'SUCCESS' | 'PAYMENT' | 'SYSTEM' | 'ANNOUNCEMENT' | 'BURSARY' | 'DEADLINE' | 'CAREER' | 'SUBSCRIPTION';

type NotificationTypeMeta = {
  label: string;
  icon: typeof Bell;
  unreadAccent: string;
  iconClassName: string;
  pill: 'slate' | 'emerald' | 'amber' | 'blue';
};

const notificationTypeMeta: Record<NotificationType, NotificationTypeMeta> = {
  INFO: {
    label: 'Info',
    icon: Bell,
    unreadAccent: 'border-l-blue-400 bg-blue-50/80',
    iconClassName: 'bg-blue-100 text-blue-700',
    pill: 'blue',
  },
  WARNING: {
    label: 'Warning',
    icon: AlertTriangle,
    unreadAccent: 'border-l-amber-400 bg-amber-50/80',
    iconClassName: 'bg-amber-100 text-amber-700',
    pill: 'amber',
  },
  SUCCESS: {
    label: 'Success',
    icon: CheckCircle2,
    unreadAccent: 'border-l-emerald-400 bg-emerald-50/80',
    iconClassName: 'bg-emerald-100 text-emerald-700',
    pill: 'emerald',
  },
  PAYMENT: {
    label: 'Payment',
    icon: CircleDollarSign,
    unreadAccent: 'border-l-emerald-400 bg-emerald-50/80',
    iconClassName: 'bg-emerald-100 text-emerald-700',
    pill: 'emerald',
  },
  ANNOUNCEMENT: {
    label: 'Announcement',
    icon: Bell,
    unreadAccent: 'border-l-blue-400 bg-blue-50/80',
    iconClassName: 'bg-blue-100 text-blue-700',
    pill: 'blue',
  },
  SYSTEM: {
    label: 'System',
    icon: Settings,
    unreadAccent: 'border-l-slate-400 bg-slate-50/80',
    iconClassName: 'bg-slate-100 text-slate-700',
    pill: 'slate',
  },
  BURSARY: {
    label: 'Bursary',
    icon: CircleDollarSign,
    unreadAccent: 'border-l-emerald-400 bg-emerald-50/80',
    iconClassName: 'bg-emerald-100 text-emerald-700',
    pill: 'emerald',
  },
  DEADLINE: {
    label: 'Deadline',
    icon: Clock3,
    unreadAccent: 'border-l-amber-400 bg-amber-50/80',
    iconClassName: 'bg-amber-100 text-amber-700',
    pill: 'amber',
  },
  CAREER: {
    label: 'Career',
    icon: BriefcaseBusiness,
    unreadAccent: 'border-l-blue-400 bg-blue-50/80',
    iconClassName: 'bg-blue-100 text-blue-700',
    pill: 'blue',
  },
  SUBSCRIPTION: {
    label: 'Subscription',
    icon: Bell,
    unreadAccent: 'border-l-violet-400 bg-violet-50/80',
    iconClassName: 'bg-violet-100 text-violet-700',
    pill: 'blue',
  },
};

const inferNotificationType = (note: Notification): NotificationType => {
  const backendType = (note.type ?? '').toUpperCase();
  if (backendType in notificationTypeMeta) return backendType as NotificationType;

  const haystack = `${note.title} ${note.message}`.toLowerCase();
  if (haystack.includes('subscription') || haystack.includes('plan') || haystack.includes('premium')) return 'SUBSCRIPTION';
  if (haystack.includes('deadline') || haystack.includes('closes') || haystack.includes('due')) return 'DEADLINE';
  if (haystack.includes('bursary') || haystack.includes('scholarship') || haystack.includes('funding')) return 'BURSARY';
  if (haystack.includes('career') || haystack.includes('job') || haystack.includes('match')) return 'CAREER';
  return 'SYSTEM';
};

const formatNotificationTimestamp = (value?: string) => {
  if (!value) return 'Recently';

  const timestamp = new Date(value);
  if (Number.isNaN(timestamp.getTime())) return 'Recently';

  const diffMs = Date.now() - timestamp.getTime();
  if (diffMs < 0) {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(timestamp);
  }

  const diffMinutes = Math.floor(diffMs / 60000);
  if (diffMinutes < 1) return 'Just now';
  if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;

  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(timestamp);
};

export const StudentNotificationsPage = () => {
  const qc = useQueryClient();
  const notes = useAppQuery({ queryKey: ['notes'], queryFn: () => notificationService.mine({ page: 0, size: 100 }) });
  const markRead = useMutation({ mutationFn: (id: string) => notificationService.markRead(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notes'] }); qc.invalidateQueries({ queryKey: ['notes-unread'] }); } });
  const markAllRead = useMutation({ mutationFn: () => notificationService.markAllRead(), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notes'] }); qc.invalidateQueries({ queryKey: ['notes-unread'] }); } });
  const deleteNotification = useMutation({ mutationFn: (id: string) => notificationService.deleteMine(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notes'] }); qc.invalidateQueries({ queryKey: ['notes-unread'] }); } });

  const rawItems = useMemo(() => {
    if (Array.isArray(notes.data)) return notes.data;
    const content = (notes.data as { content?: unknown } | undefined)?.content;
    return Array.isArray(content) ? content : [];
  }, [notes.data]);

  const notifications = useMemo(() => rawItems.flatMap((item, index) => {
    if (!item || typeof item !== 'object') return [];
    const note = item as Partial<Notification> & Record<string, unknown>;
    const mapped: Notification = {
      id: String(note.id ?? `malformed-${index}`),
      title: String(note.title ?? 'Untitled notification'),
      message: String(note.message ?? 'No message available.'),
      type: typeof note.type === 'string' ? note.type : 'SYSTEM',
      priority: typeof note.priority === 'string' ? note.priority : 'NORMAL',
      createdAt: typeof note.createdAt === 'string'
        ? note.createdAt
        : typeof note.notificationCreatedAt === 'string'
          ? note.notificationCreatedAt
          : typeof note.deliveredAt === 'string'
            ? note.deliveredAt
            : undefined,
      isRead: Boolean(note.isRead ?? note.read),
    };
    const type = inferNotificationType(mapped);
    return [{
      ...mapped,
      read: mapped.isRead ?? false,
      type,
      meta: notificationTypeMeta[type],
      timestampLabel: formatNotificationTimestamp(mapped.createdAt),
    }];
  }), [rawItems]);

  if (notes.isLoading) return <LoadingState />;
  if (notes.isError) {
    const status = (notes.error as ApiError | undefined)?.status;
    const message = status === 401 || status === 403
      ? 'Failed to load notifications. Please sign in again.'
      : 'Failed to load notifications.';
    return <ErrorState message={message} />;
  }
  if (!notifications.length) return <EmptyState title="No notifications available." message="No notifications available." />;

  return <Section title="Notifications">
    <div className="flex justify-end">
      <Button onClick={() => markAllRead.mutate()} disabled={markAllRead.isPending || notifications.every((item) => item.read)}>
        Mark all as read
      </Button>
    </div>
    <div className="grid gap-3">
      {notifications.map((n) => {
        const Icon = n.meta.icon;
        return <article
          key={n.id}
          className={`group rounded-2xl border border-slate-200 p-4 transition hover:-translate-y-0.5 hover:shadow-md md:p-5 ${n.read ? 'bg-slate-50/70 opacity-90' : `border-l-4 ${n.meta.unreadAccent} shadow-sm`}`}
        >
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="flex gap-3">
              <div className={`mt-0.5 flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${n.read ? 'bg-slate-100 text-slate-500' : `${n.meta.iconClassName} shadow-sm`}`}>
                <Icon size={20} />
              </div>
              <div className="min-w-0 space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <p className={`text-base ${n.read ? 'font-medium text-slate-700' : 'font-semibold text-slate-900'}`}>{n.title}</p>
                  <Badge color={n.meta.pill}>{n.meta.label}</Badge>
                  {!n.read ? <span className="inline-flex items-center gap-1 rounded-full bg-primary-50 px-2 py-1 text-xs font-medium text-primary-700"><span className="h-2 w-2 rounded-full bg-primary-600" />Unread</span> : null}
                </div>
                <p className={`text-sm leading-6 ${n.read ? 'text-slate-500' : 'text-slate-600'}`}>{n.message}</p>
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                  <span>{n.timestampLabel}</span>
                </div>
              </div>
            </div>
            <div className="flex shrink-0 items-center md:pl-4">
              <div className="flex gap-2">
                {n.read ? (
                  <span className="inline-flex cursor-default items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-500">
                    <CheckCheck size={16} />
                    Read
                  </span>
                ) : (
                  <Button
                    onClick={() => markRead.mutate(n.id)}
                    disabled={markRead.isPending}
                    className="inline-flex items-center gap-2 bg-primary-600 shadow-sm hover:bg-primary-500"
                  >
                    <CheckCheck size={16} />
                    Mark as Read
                  </Button>
                )}
                <Button onClick={() => deleteNotification.mutate(n.id)} className="bg-slate-700 hover:bg-slate-600">Delete</Button>
              </div>
            </div>
          </div>
        </article>;
      })}
    </div>
  </Section>;
};

export const StudentSubscriptionPage = () => {
  const qc = useQueryClient();
  const location = useLocation();
  const redirectHandledRef = useRef<string>('');
  const [checkoutMessage, setCheckoutMessage] = useState('');
  const [messageTone, setMessageTone] = useState<'emerald' | 'amber' | 'red' | 'slate'>('slate');
  const [pendingPaymentReference, setPendingPaymentReference] = useState<string | null>(null);

  const plans = useAppQuery({ queryKey: ['subscription-plans'], queryFn: subscriptionService.plans });
  const current = useAppQuery({ queryKey: ['sub'], queryFn: subscriptionService.current });

  const checkout = useMutation({
    mutationFn: subscriptionService.checkout,
    onSuccess: (response) => {
      setMessageTone(response.paymentStatus === 'FAILED' ? 'red' : response.paymentStatus === 'PENDING' ? 'amber' : 'emerald');
      setCheckoutMessage(response.message);
      qc.invalidateQueries({ queryKey: ['sub'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
      if (response.checkoutUrl) {
        window.location.assign(response.checkoutUrl);
      }
    },
  });

  const confirm = useMutation({
    mutationFn: subscriptionService.confirm,
    onSuccess: (response) => {
      setMessageTone(response.paymentStatus === 'COMPLETED' ? 'emerald' : response.paymentStatus === 'PENDING' ? 'amber' : 'red');
      setCheckoutMessage(response.message);
      qc.invalidateQueries({ queryKey: ['sub'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
    },
  });

  const cancel = useMutation({
    mutationFn: subscriptionService.cancel,
    onSuccess: (response) => {
      setMessageTone(response.paymentStatus === 'CANCELLED' ? 'amber' : 'red');
      setCheckoutMessage(response.message);
      setPendingPaymentReference(null);
      qc.invalidateQueries({ queryKey: ['sub'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
    },
  });

  const payFastInitiate = useMutation({
    mutationFn: subscriptionService.payFastInitiate,
    onSuccess: (response) => {
      setMessageTone(response.paymentStatus === 'FAILED' ? 'red' : 'amber');
      setCheckoutMessage(response.message);
      setPendingPaymentReference(response.paymentReference);
      qc.invalidateQueries({ queryKey: ['sub'] });
      qc.invalidateQueries({ queryKey: ['dash'] });
      qc.invalidateQueries({ queryKey: ['recs'] });
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = response.paymentUrl;
      Object.entries(response.formFields ?? {}).forEach(([key, value]) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        form.appendChild(input);
      });
      document.body.appendChild(form);
      form.submit();
      form.remove();
    },
  });

  const paymentStatus = useAppQuery({
    queryKey: ['payfast-payment-status', pendingPaymentReference ?? 'none'],
    enabled: Boolean(pendingPaymentReference),
    queryFn: () => subscriptionService.payFastStatus(String(pendingPaymentReference)),
    refetchInterval: (query) => {
      const status = query.state.data?.paymentStatus;
      return status === 'PENDING' ? 3000 : false;
    },
  });

  useEffect(() => {
    const query = new URLSearchParams(location.search);
    const checkoutResult = (query.get('checkoutResult') ?? '').toLowerCase();
    const paymentReference = (query.get('paymentReference') ?? '').trim();
    if (!checkoutResult || !paymentReference) return;

    const provider = (query.get('provider') ?? '').toLowerCase();
    const key = `${checkoutResult}:${paymentReference}:${provider}`;
    if (redirectHandledRef.current === key) return;
    redirectHandledRef.current = key;

    if ((checkoutResult === 'processing' || checkoutResult === 'success') && provider === 'payfast') {
      setMessageTone('amber');
      setCheckoutMessage('Payment received by PayFast. Verification is in progress.');
      setPendingPaymentReference(paymentReference);
      return;
    }

    if (checkoutResult === 'success') {
      confirm.mutate({
        paymentReference,
        provider: provider ? provider as PaymentProviderCode : undefined,
        sessionId: query.get('session_id') ?? query.get('sessionId') ?? undefined,
        orderId: query.get('orderId') ?? query.get('token') ?? undefined,
        token: query.get('token') ?? undefined,
        payerId: query.get('PayerID') ?? query.get('payerId') ?? undefined,
      });
      return;
    }

    if (checkoutResult === 'cancel' && provider === 'payfast') {
      setMessageTone('amber');
      setCheckoutMessage(query.get('message') ?? 'Checkout cancelled by user.');
      setPendingPaymentReference(null);
      qc.invalidateQueries({ queryKey: ['sub'] });
      return;
    }

    if (checkoutResult === 'cancel') {
      cancel.mutate({
        paymentReference,
        provider: provider ? provider as PaymentProviderCode : undefined,
        reason: query.get('message') ?? 'Checkout cancelled by user.',
      });
      return;
    }

    setMessageTone('red');
    setCheckoutMessage('Checkout returned with an unknown state. Please retry.');
    setPendingPaymentReference(null);
  }, [cancel, confirm, location.search, qc]);

  useEffect(() => {
    if (!paymentStatus.data) return;
    const response = paymentStatus.data;
    if (response.paymentStatus === 'COMPLETED') {
      setMessageTone('emerald');
      setCheckoutMessage('Payment verified. Premium access is active.');
      setPendingPaymentReference(null);
    } else if (response.paymentStatus === 'CANCELLED') {
      setMessageTone('amber');
      setCheckoutMessage(response.message || 'Payment was cancelled.');
      setPendingPaymentReference(null);
    } else if (response.paymentStatus === 'FAILED') {
      setMessageTone('red');
      setCheckoutMessage(response.message || 'Payment failed. Please try again.');
      setPendingPaymentReference(null);
    } else {
      setMessageTone('amber');
      setCheckoutMessage(response.message || 'Payment is pending confirmation.');
    }
    qc.invalidateQueries({ queryKey: ['sub'] });
    qc.invalidateQueries({ queryKey: ['dash'] });
    qc.invalidateQueries({ queryKey: ['recs'] });
  }, [paymentStatus.data, qc]);

  const chooseBasic = (planCode: string) => {
    checkout.mutate({ planCode, provider: 'internal' });
  };

  if (current.isLoading) return <LoadingState />;
  if (current.isError) return <ErrorState message="Could not load your subscription." />;

  const viewPlans = plans.data ?? [];
  const messageClassName = messageTone === 'emerald'
    ? 'text-emerald-700'
    : messageTone === 'amber'
      ? 'text-amber-700'
      : messageTone === 'red'
        ? 'text-red-600'
        : 'text-slate-600';
  const checkoutErrorMessage = (checkout.error as ApiError | null)?.message ?? 'Could not update plan. Please retry.';
  const confirmErrorMessage = (confirm.error as ApiError | null)?.message ?? 'We could not confirm your payment yet. Retry from this page.';
  const cancelErrorMessage = (cancel.error as ApiError | null)?.message ?? 'We could not record checkout cancellation. Refresh and try again.';
  const payFastErrorMessage = (payFastInitiate.error as ApiError | null)?.message ?? 'Could not initialize PayFast checkout. Verify PayFast configuration and retry.';
  const statusErrorMessage = (paymentStatus.error as ApiError | null)?.message ?? 'Could not refresh payment status. Please reload this page.';
  const actionInProgress = checkout.isPending || confirm.isPending || cancel.isPending || payFastInitiate.isPending || paymentStatus.isFetching;
  const currentPlanCode = current.data?.planCode ?? 'PLAN_BASIC';
  const currentStatus = current.data?.status ?? 'ACTIVE';
  const trialEndLabel = current.data?.trialEndDate
    ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(current.data.trialEndDate))
    : null;

  return <Section title="Subscription & Payment" description="Manage your Basic or Premium access with secure PayFast checkout.">
    <div className="student-hero-card">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">Current subscription</p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <h2 className="text-2xl font-bold text-slate-950">{currentPlanCode}</h2>
            <Badge color={currentStatus === 'ACTIVE' ? 'emerald' : currentStatus === 'PAST_DUE' ? 'amber' : 'slate'}>{currentStatus}</Badge>
          </div>
          <p className="mt-2 text-sm text-slate-600">Premium upgrades activate after PayFast verification.</p>
        </div>
        <div className="rounded-2xl border border-primary-100 bg-primary-50 px-4 py-3 text-sm font-medium text-primary-800">
          {current.data?.renewalDate ? `Renewal: ${new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(current.data.renewalDate))}` : 'Subscription access is active'}
        </div>
      </div>
    </div>
    {current.data?.trialActive ? (
      <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
        You are on a free Premium trial. Trial ends on {trialEndLabel ?? 'your trial end date'}.
      </p>
    ) : null}
    {!current.data?.trialActive && current.data?.accessMessage ? (
      <p className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        {current.data.accessMessage}
      </p>
    ) : null}
    {(currentStatus === 'PAYMENT_FAILED' || currentStatus === 'PAST_DUE') && <p className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">Your last payment did not complete. Please retry checkout to reactivate premium access.</p>}
    {checkoutMessage && <p className={`rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm ${messageClassName}`}>{checkoutMessage}</p>}
    {checkout.isError && <p className="text-sm text-red-600">{checkoutErrorMessage}</p>}
    {confirm.isError && <p className="text-sm text-red-600">{confirmErrorMessage}</p>}
    {cancel.isError && <p className="text-sm text-red-600">{cancelErrorMessage}</p>}
    {payFastInitiate.isError && <p className="text-sm text-red-600">{payFastErrorMessage}</p>}
    {paymentStatus.isError && <p className="text-sm text-red-600">{statusErrorMessage}</p>}
    {plans.isError && <p className="text-sm text-red-600">Could not load live subscription plans. Please refresh and try again.</p>}
    {!plans.isError && viewPlans.length === 0 ? <EmptyState title="No subscription plans available" message="Live plans are unavailable right now. Please refresh in a moment." /> : null}
    <div className="grid gap-4 lg:grid-cols-2">
      {viewPlans.map((plan) => {
        const isPaid = Number(plan.amount) > 0;
        const isCurrent = plan.code === currentPlanCode;
        return <article
          key={plan.code}
          className={`relative flex h-full flex-col gap-5 overflow-hidden rounded-[20px] border border-slate-200 bg-white p-6 shadow-[0_10px_30px_rgba(15,23,42,0.08)] transition-all duration-200 hover:-translate-y-1 hover:shadow-[0_16px_40px_rgba(37,99,235,0.12)] ${plan.premium ? 'lg:scale-[1.02] ring-1 ring-blue-100' : ''}`}
        >
          {plan.premium ? <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-[#2563EB] via-[#60A5FA] to-[#93C5FD]" /> : null}
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="text-xl font-bold text-[#1E293B]">{plan.name}</h3>
                {plan.premium ? <Badge color="blue">Most Popular</Badge> : plan.recommended ? <Badge color="blue">Recommended</Badge> : null}
                {isCurrent ? <Badge color="emerald">Current</Badge> : null}
              </div>
              {plan.description ? <p className="mt-2 text-sm leading-6 text-[#64748B]">{plan.description}</p> : null}
            </div>
            <div className="rounded-2xl bg-[#F8FAFF] px-4 py-3 text-right">
              <p className="text-2xl font-bold text-[#2563EB]">{plan.premium ? 'R49.99 / month' : formatPlanPrice(Number(plan.amount), plan.currency, plan.billingPeriod ?? plan.billingInterval)}</p>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{plan.billingPeriod ?? plan.billingInterval}</p>
            </div>
          </div>
          {!!plan.features?.length && (
            <ul className="grid gap-3 text-sm text-[#334155] sm:grid-cols-2">
              {plan.features.map((feature) => <li key={feature} className="flex items-start gap-3 rounded-xl bg-[#F8FAFF] px-3 py-2">
                <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-500" />
                <span>{feature}</span>
              </li>)}
            </ul>
          )}
          <div className="mt-auto">
            {isPaid ? <div className="space-y-2">
              <Button onClick={() => payFastInitiate.mutate({ planCode: plan.code })} disabled={actionInProgress} className="inline-flex w-full items-center justify-center gap-2 rounded-[12px] bg-[#2563EB] text-white hover:bg-[#1D4ED8]">
                <CircleDollarSign size={16} />
                Pay with PayFast
              </Button>
              <p className="text-xs text-slate-500">Secure payment powered by PayFast.</p>
            </div> : <Button onClick={() => chooseBasic(plan.code)} disabled={actionInProgress} className="rounded-[12px] bg-slate-900 text-white hover:bg-slate-800">Choose Basic</Button>}
          </div>
        </article>;
      })}
    </div>
  </Section>;
};
export const StudentSettingsPage = () => {
  const qc = useQueryClient();
  const { logout } = useAuth();
  const settings = useAppQuery({ queryKey: ['student-settings'], queryFn: settingsService.get });
  const [deleteConfirmation, setDeleteConfirmation] = useState('');
  const [deleteReason, setDeleteReason] = useState('');
  const save = useMutation({
    mutationFn: settingsService.update,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['student-settings'] }),
  });
  const deleteAccount = useMutation({
    mutationFn: () => accountService.deleteMine(deleteConfirmation, deleteReason),
    onSuccess: async () => {
      await logout();
    },
  });

  if (settings.isLoading) return <LoadingState />;
  if (settings.isError || !settings.data) return <ErrorState message="Could not load settings." />;

  const data = settings.data;
  const toggle = (key: 'inAppNotificationsEnabled' | 'emailNotificationsEnabled' | 'smsNotificationsEnabled') => {
    save.mutate({ ...data, [key]: !data[key] });
  };

  return <Section title="Settings">
    <p className="text-sm text-slate-600">Manage how EduRite sends student notifications.</p>
    <div className="space-y-3">
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">In-app notifications</p><p className="text-sm text-slate-500">Receive alerts in your dashboard and notifications page.</p></div>
        <Button onClick={() => toggle('inAppNotificationsEnabled')} disabled={save.isPending}>{data.inAppNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">Email notifications</p><p className="text-sm text-slate-500">Receive bursary and subscription updates via email.</p></div>
        <Button onClick={() => toggle('emailNotificationsEnabled')} disabled={save.isPending}>{data.emailNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
      <div className="flex items-center justify-between rounded border p-3">
        <div><p className="font-medium">SMS notifications</p><p className="text-sm text-slate-500">Receive urgent reminders by SMS.</p></div>
        <Button onClick={() => toggle('smsNotificationsEnabled')} disabled={save.isPending}>{data.smsNotificationsEnabled ? 'Enabled' : 'Disabled'}</Button>
      </div>
    </div>
    {save.isSuccess && <p className="text-sm text-emerald-700">Settings saved.</p>}
    {save.isError && <p className="text-sm text-red-600">Unable to save settings right now.</p>}
    <div className="rounded border border-red-200 bg-red-50 p-4 space-y-3">
      <h3 className="font-semibold text-red-800">Delete account</h3>
      <p className="text-sm text-red-700">This will deactivate your account and revoke access. Type <span className="font-semibold">DELETE</span> to confirm.</p>
      <Input placeholder="Type DELETE to confirm" value={deleteConfirmation} onChange={(event) => setDeleteConfirmation(event.target.value)} />
      <Input placeholder="Reason (optional)" value={deleteReason} onChange={(event) => setDeleteReason(event.target.value)} />
      <Button onClick={() => deleteAccount.mutate()} disabled={deleteAccount.isPending || deleteConfirmation !== 'DELETE'} className="bg-red-700 hover:bg-red-600">Delete My Account</Button>
      {deleteAccount.isError ? <p className="text-sm text-red-700">Unable to delete account right now. Check the confirmation text and retry.</p> : null}
    </div>
  </Section>;
};
export { StudentUniversitiesPage } from './StudentUniversitiesPage';
export { StudentCollegesTvetsPage } from './StudentCollegesTvetsPage';




























