import { type RefObject, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { ArrowRight, BookOpen, BriefcaseBusiness, Building2, Check, ChevronDown, Eye, EyeOff, GraduationCap, Lock, Mail, School, ShieldCheck, Sparkles, User as UserIcon, UserCheck, Users } from 'lucide-react';
import { EduRiteLogo } from '@/components/common/EduRiteLogo';
import { PopiaConsentCheckbox } from '@/components/forms/PopiaConsentCheckbox';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ErrorState, LoadingState } from '@/components/feedback/States';
import { getCompanyPathForApprovalStatus, getDashboardPathForRole, getDashboardPathForUser, isAuthorizedPathForRole, resolvePrimaryRole } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { authService } from '@/services/authService';
import { locationService } from '@/services/locationService';
import type { CompanyRegisterPayload, LocationOption, Role, SchoolRegisterPayload, User } from '@/types';

type AuthRole = Role;
type AuthMode = 'login' | 'register';

type LoginFormValues = {
  email: string;
  schoolName: string;
  emisNumber: string;
  password: string;
  rememberMe: boolean;
};

type DemoCredential = {
  username: string;
  password: string;
};

type SelectedPortal = 'STUDENT' | 'SCHOOL' | 'COMPANY' | 'ADMIN';
type SelectedRoleOption =
  | 'STUDENT'
  | 'SCHOOL_STUDENT'
  | 'TEACHER'
  | 'SCHOOL_ADMIN'
  | 'COMPANY'
  | 'DISTRICT_ADMIN'
  | 'PLATFORM_ADMIN';

const roleContent: Record<AuthRole, {
  badge: string;
  loginTitle: string;
  loginSubtitle: string;
  registerTitle: string;
  registerSubtitle: string;
  registerPath?: string;
  ctaLabel: string;
}> = {
  STUDENT: {
    badge: 'Student access',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Sign in with your existing EduRite student account to continue your journey.',
    registerTitle: 'Create student account',
    registerSubtitle: 'Set up your student profile to access careers, courses, bursaries, and guidance.',
    registerPath: '/auth/register/student',
    ctaLabel: 'Sign in to Student Portal',
  },
  COMPANY: {
    badge: 'Company portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Access your company workspace to manage bursaries, talent pipelines, and approvals.',
    registerTitle: 'Register your company',
    registerSubtitle: 'Create a company account, submit verification details, and start engaging with talent.',
    registerPath: '/auth/register/company',
    ctaLabel: 'Sign in to Company Portal',
  },
  ADMIN: {
    badge: 'Admin console',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Use your secure admin credentials to access EduRite administration tools.',
    registerTitle: 'Admin access is restricted',
    registerSubtitle: 'Public admin registration is intentionally disabled. Admin accounts must be provisioned through secure internal setup.',
    ctaLabel: 'Sign in to Admin Portal',
  },
  DISTRICT_ADMIN: {
    badge: 'District portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'District oversight for schools, reports, interventions, and readiness.',
    registerTitle: 'District access is restricted',
    registerSubtitle: 'District admin accounts are provisioned by platform administrators.',
    ctaLabel: 'Sign in to District Portal',
  },
  DISTRICT_DIRECTOR: {
    badge: 'District portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'District-wide education oversight, reports, curriculum, and interventions.',
    registerTitle: 'District access is restricted',
    registerSubtitle: 'District director accounts are provisioned by platform administrators.',
    ctaLabel: 'Sign in to District Portal',
  },
  CIRCUIT_MANAGER: {
    badge: 'District portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Manage assigned circuit schools, visits, curriculum monitoring, and support requests.',
    registerTitle: 'District access is restricted',
    registerSubtitle: 'Circuit manager accounts are provisioned by platform administrators.',
    ctaLabel: 'Sign in to District Portal',
  },
  SUBJECT_ADVISOR: {
    badge: 'District portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Support teachers, curriculum delivery, assessments, and interventions by subject.',
    registerTitle: 'District access is restricted',
    registerSubtitle: 'Subject advisor accounts are provisioned by platform administrators.',
    ctaLabel: 'Sign in to District Portal',
  },
  SCHOOL_ADMIN: {
    badge: 'School administration',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Access your school operations dashboard.',
    registerTitle: 'Register your school',
    registerSubtitle: 'Create a school account, send your district join request, and track approval from the school portal.',
    registerPath: '/auth/register/school',
    ctaLabel: 'Sign in to School Admin Portal',
  },
  TEACHER: {
    badge: 'Teacher portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'Access your classes, SBA tasks, marking and learner progress.',
    registerTitle: 'Teacher access is provisioned',
    registerSubtitle: 'Teacher accounts are provisioned by school administrators.',
    ctaLabel: 'Sign in to Teacher Portal',
  },
  SCHOOL_STUDENT: {
    badge: 'School learner portal',
    loginTitle: 'Welcome back',
    loginSubtitle: 'View notes, assignments, SBA tasks, exams and feedback.',
    registerTitle: 'Learner access is provisioned',
    registerSubtitle: 'School learner accounts are provisioned by school administrators.',
    ctaLabel: 'Sign in to Learner Portal',
  },
};

const getRoleDashboard = (user: User): string => getDashboardPathForUser(user) ?? '/auth/login';

const resolveRoleFromPath = (pathname: string): AuthRole => {
  if (pathname.includes('/school-student/')) return 'SCHOOL_STUDENT';
  if (pathname.includes('/teacher/')) return 'TEACHER';
  if (pathname.includes('/school/')) return 'SCHOOL_ADMIN';
  if (pathname.includes('/company/')) return 'COMPANY';
  if (pathname.includes('/district/')) return 'DISTRICT_ADMIN';
  if (pathname.includes('/admin/')) return 'ADMIN';
  return 'STUDENT';
};

const resolveModeFromPath = (pathname: string): AuthMode => (pathname.includes('/register') ? 'register' : 'login');

const buildAuthPath = (role: AuthRole, mode: AuthMode) => {
  if (mode === 'login') {
    if (role === 'SCHOOL_ADMIN') return '/school/login';
    if (role === 'TEACHER') return '/teacher/login';
    if (role === 'SCHOOL_STUDENT') return '/school-student/login';
    if (role === 'COMPANY') return '/company/login';
    if (role === 'DISTRICT_ADMIN') return '/district/login';
    if (role === 'ADMIN') return '/admin/login';
    return '/auth/login';
  }

  if (role === 'SCHOOL_ADMIN') return '/auth/register/school';
  if (role === 'COMPANY') return '/auth/register/company';
  return '/auth/register/student';
};

const getForgotPasswordPath = (role: AuthRole) => role === 'COMPANY'
  ? '/company/forgot-password'
  : role === 'DISTRICT_ADMIN'
    ? '/district/forgot-password'
    : role === 'ADMIN'
      ? '/admin/forgot-password'
      : '/auth/forgot-password';
const getResetPasswordPath = (role: AuthRole) => role === 'COMPANY'
  ? '/company/reset-password'
  : role === 'DISTRICT_ADMIN'
    ? '/district/reset-password'
    : role === 'ADMIN'
      ? '/admin/reset-password'
      : '/auth/reset-password';
const getResetPasswordLoginPath = (role: AuthRole) => role === 'COMPANY'
  ? '/company/login'
  : role === 'DISTRICT_ADMIN'
    ? '/district/login'
    : role === 'ADMIN'
      ? '/admin/login'
      : role === 'SCHOOL_ADMIN'
        ? '/school/login'
        : role === 'TEACHER'
          ? '/teacher/login'
          : role === 'SCHOOL_STUDENT'
            ? '/school-student/login'
            : '/auth/login';

const passwordRequirements = [
  'At least 8 characters',
  'One uppercase letter (Aâ€“Z)',
  'One lowercase letter (aâ€“z)',
  'One number (0â€“9)',
  'One special character (!@#$%^&*)',
];

const parseEnvBoolean = (value: string | undefined): boolean => {
  if (!value) return false;
  return ['true', '1', 'yes', 'on'].includes(value.trim().toLowerCase());
};

const sanitizeClientId = (value: string | undefined): string => {
  if (!value) return '';
  return value.trim().replace(/^['"]|['"]$/g, '');
};

const GOOGLE_CLIENT_ID_PATTERN = /^[a-zA-Z0-9-]+\.apps\.googleusercontent\.com$/;
const POPIA_CONSENT_VERSION = (import.meta.env.VITE_POPI_CONSENT_VERSION?.trim() || 'v1.0');
const GOOGLE_UNAVAILABLE_MESSAGE = 'Google sign-in is not available right now. Please use email and password.';
const GOOGLE_CLIENT_ID = sanitizeClientId(import.meta.env.VITE_GOOGLE_CLIENT_ID ?? import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID);
const GOOGLE_SIGNIN_ENABLED = parseEnvBoolean(import.meta.env.VITE_GOOGLE_SIGNIN_ENABLED ?? import.meta.env.VITE_GOOGLE_OAUTH_ENABLED)
  || Boolean(GOOGLE_CLIENT_ID);
const GOOGLE_SCRIPT_ID = 'google-identity-services-script';

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (options: { client_id: string; callback: (response: { credential?: string }) => void }) => void;
          renderButton: (element: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

const loadGoogleIdentityScript = (): Promise<void> => new Promise((resolve, reject) => {
  if (window.google?.accounts?.id) {
    resolve();
    return;
  }

  const existing = document.getElementById(GOOGLE_SCRIPT_ID) as HTMLScriptElement | null;
  if (existing) {
    existing.addEventListener('load', () => resolve(), { once: true });
    existing.addEventListener('error', () => reject(new Error('Failed to load Google sign-in script.')), { once: true });
    return;
  }

  const script = document.createElement('script');
  script.id = GOOGLE_SCRIPT_ID;
  script.src = 'https://accounts.google.com/gsi/client';
  script.async = true;
  script.defer = true;
  script.onload = () => resolve();
  script.onerror = () => reject(new Error('Failed to load Google sign-in script.'));
  document.head.appendChild(script);
});

const PasswordRequirementsPanel = () => (
  <div className="mt-2 rounded-xl border border-sky-100 bg-sky-50/80 px-3 py-2.5 text-sm text-slate-700">
    <p className="text-[13px] font-semibold text-slate-900">Password must contain:</p>
    <ul className="mt-2 grid grid-cols-1 gap-1.5 md:grid-cols-2">
      {passwordRequirements.map((rule) => (
        <li key={rule} className="flex items-center gap-1.5 rounded-lg border border-sky-100/70 bg-white/70 px-2 py-1">
          <Check className="h-4 w-4 text-emerald-600" aria-hidden="true" />
          <span className="text-sm leading-5">{rule}</span>
        </li>
      ))}
    </ul>
  </div>
);

const SelectError = ({ message }: { message?: string }) => message ? (
  <p className="mt-1.5 text-xs font-medium text-rose-600">{message}</p>
) : null;

const roleLabel: Record<AuthRole, string> = {
  STUDENT: 'Student',
  COMPANY: 'Company',
  ADMIN: 'Admin',
  DISTRICT_ADMIN: 'District Admin',
  DISTRICT_DIRECTOR: 'District Director',
  CIRCUIT_MANAGER: 'Circuit Manager',
  SUBJECT_ADVISOR: 'Subject Advisor',
  SCHOOL_ADMIN: 'School Admin',
  TEACHER: 'Teacher',
  SCHOOL_STUDENT: 'Learner',
};

const loginPathForSelection = (selection: SelectedRoleOption): string => {
  if (selection === 'SCHOOL_ADMIN') return '/school/login';
  if (selection === 'TEACHER') return '/teacher/login';
  if (selection === 'SCHOOL_STUDENT') return '/school-student/login';
  if (selection === 'COMPANY') return '/company/login';
  if (selection === 'DISTRICT_ADMIN') return '/district/login';
  if (selection === 'PLATFORM_ADMIN') return '/admin/login';
  return '/auth/login';
};

const demoCredentialsByRole: Record<'STUDENT' | 'TEACHER' | 'SCHOOL_ADMIN' | 'DISTRICT_ADMIN' | 'PLATFORM_ADMIN', DemoCredential> = {
  STUDENT: {
    username: 'arnoldmadaz@gmail.com',
    password: 'Student@123',
  },
  TEACHER: {
    username: 'teacher@edurite.com',
    password: 'Teacher@123',
  },
  SCHOOL_ADMIN: {
    username: 'EduRite / 99999999',
    password: 'Admin@123',
  },
  DISTRICT_ADMIN: {
    username: 'districtadmin@edurite.com',
    password: 'DistrictAdmin@123',
  },
  PLATFORM_ADMIN: {
    username: 'admin@edurite.com',
    password: 'Admin@123',
  },
};

const GoogleAuthSection = ({
  enabled,
  available,
  isSubmitting,
  buttonRef,
}: {
  enabled: boolean;
  available: boolean;
  isSubmitting: boolean;
  buttonRef: RefObject<HTMLDivElement>;
}) => {
  if (!enabled) return null;

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-slate-500">
        <span className="h-px flex-1 bg-slate-200" />
        Or continue with
        <span className="h-px flex-1 bg-slate-200" />
      </div>
      <div className="mt-4 mb-4 flex w-full justify-center">
        <div className="w-full max-w-[380px]">
          {available ? <div ref={buttonRef} className="w-full" /> : (
            <button
              type="button"
              disabled
              className="flex w-full cursor-not-allowed items-center justify-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-500"
              title="Google sign-in is enabled but not configured for this build."
            >
              <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-slate-200 text-[11px] font-bold text-slate-700">G</span>
              Continue with Google
            </button>
          )}
        </div>
      </div>
      {available && isSubmitting ? <p className="text-xs text-slate-600">Completing Google sign-in...</p> : null}
      <p className="text-[11px] text-slate-500">
        By continuing with Google, you agree to EduRite&apos;s{' '}
        <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/privacy-policy">Privacy Policy</Link>{' '}
        and{' '}
        <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/terms-and-conditions">Terms &amp; Conditions</Link>.
      </p>
    </div>
  );
};

const RegisterPortalSelector = ({ role }: { role: AuthRole }) => {
  const isSchoolPortal = role === 'SCHOOL_ADMIN' || role === 'TEACHER' || role === 'SCHOOL_STUDENT';
  const isStudentSelected = role === 'STUDENT';
  const isCompanySelected = role === 'COMPANY';
  const cards: Array<{ key: 'STUDENT' | 'SCHOOL' | 'COMPANY' | 'ADMIN'; title: string; description: string; icon: React.ReactNode; to: string }> = [
    { key: 'STUDENT', title: 'Student', description: 'Learner profile and guidance', icon: <GraduationCap className="h-5 w-5" />, to: '/auth/register/student' },
    { key: 'SCHOOL', title: 'School', description: 'School admin, teacher, learner', icon: <School className="h-5 w-5" />, to: '/auth/register/school' },
    { key: 'COMPANY', title: 'Company', description: 'Bursaries and opportunities', icon: <BriefcaseBusiness className="h-5 w-5" />, to: '/auth/register/company' },
    { key: 'ADMIN', title: 'Admin', description: 'Provisioned platform access', icon: <ShieldCheck className="h-5 w-5" />, to: '/admin/login' },
  ];

  const schoolRoles: Array<{ label: string; to: string }> = [
    { label: 'School Admin', to: '/school/login' },
    { label: 'Teacher', to: '/teacher/login' },
    { label: 'Learner', to: '/school-student/login' },
  ];

  return (
    <div className="space-y-3.5">
      <div>
        <h2 className="text-[clamp(1.5rem,1.8vw,1.75rem)] font-semibold tracking-tight text-slate-900">Get started with EduRite</h2>
        <p className="mt-1 text-[13px] text-slate-600">Choose your account type to continue</p>
      </div>
      <div className="grid grid-cols-2 gap-2 lg:grid-cols-4">
        {cards.map((card) => {
          const isActive = (card.key === 'SCHOOL' && isSchoolPortal)
            || (card.key === 'STUDENT' && isStudentSelected)
            || (card.key === 'COMPANY' && isCompanySelected)
            || (card.key === 'ADMIN' && role === 'ADMIN');
          return (
            <Link
              key={card.key}
              to={card.to}
              className={`rounded-xl border p-2.5 transition ${isActive ? 'border-primary-500 bg-primary-50/70 shadow-[0_0_0_3px_rgba(59,130,246,0.15)]' : 'border-slate-200 bg-white hover:border-primary-300 hover:shadow-md'}`}
            >
              <div className="flex items-start gap-2.5">
                <span className={`mt-0.5 inline-flex h-7 w-7 items-center justify-center rounded-lg ${isActive ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-700'}`}>
                  {card.icon}
                </span>
                <div>
                  <p className="text-sm font-semibold text-slate-900">{card.title}</p>
                  <p className="mt-0.5 text-[10px] text-slate-600">{card.description}</p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>
      {isSchoolPortal ? (
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-2.5">
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">School role</p>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
            {schoolRoles.map((schoolRole) => (
              <Link key={schoolRole.label} to={schoolRole.to} className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-center text-[13px] font-semibold text-slate-700 transition hover:border-primary-300">
                {schoolRole.label}
              </Link>
            ))}
          </div>
          <p className="mt-2 text-xs text-slate-600">School admins can self-register, while teacher and learner accounts are still provisioned by approved schools.</p>
        </div>
      ) : null}
    </div>
  );
};

const AuthShell = ({ children, role, mode }: { children: React.ReactNode; role: AuthRole; mode: AuthMode }) => (
  <section className="bg-slate-50 px-4 py-5 md:px-6 md:py-6 lg:px-8">
    <div className="mx-auto grid max-w-[1680px] gap-0 overflow-hidden rounded-[30px] border border-slate-200 bg-white shadow-[0_40px_100px_-56px_rgba(15,23,42,0.35)] lg:grid-cols-[520px_minmax(0,1fr)]">
      <div className="relative overflow-hidden bg-gradient-to-br from-[#173d96] via-[#2244cd] to-[#3b36c6] p-8 text-white md:p-10">
        <div className="pointer-events-none absolute left-8 top-10 h-24 w-24 rounded-full bg-white/10 blur-2xl" aria-hidden="true" />
        <div className="pointer-events-none absolute bottom-12 right-10 h-20 w-20 rounded-full bg-sky-200/20 blur-2xl" aria-hidden="true" />
        <div className="flex items-start justify-between gap-4">
          <div className="flex min-h-[80px] items-center rounded-[24px] bg-white px-4 py-3 shadow-lg shadow-slate-950/15 md:min-h-[96px] md:px-5">
            <EduRiteLogo className="h-auto w-[160px] sm:w-[180px] md:w-[200px] lg:w-[220px]" />
          </div>
          <span className="rounded-full border border-white/40 bg-white/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.2em] text-white/90">
            {roleLabel[role]} Portal
          </span>
        </div>
        <h1 className="mt-14 max-w-[320px] text-[44px] font-semibold leading-[1.05] tracking-tight text-white sm:max-w-[380px] sm:text-[56px]">
          {mode === 'register' ? 'Start your EduRite journey.' : 'One Platform. Limitless Opportunities.'}
        </h1>
        <p className="mt-6 max-w-md text-[16px] leading-8 text-blue-50/95">
          {mode === 'register'
            ? 'Create your account and access learning, school tools, career guidance, bursaries, and opportunities in one platform.'
            : 'EduRite connects students, schools, teachers, districts and companies on a single AI-powered education ecosystem.'}
        </p>
        <div className="mt-10 grid gap-4 sm:grid-cols-2">
          {(mode === 'register'
            ? [
              { label: 'Create your learner profile', icon: <UserCheck className="h-4 w-4" /> },
              { label: 'Join your school portal', icon: <School className="h-4 w-4" /> },
              { label: 'Access notes and assignments', icon: <BookOpen className="h-4 w-4" /> },
              { label: 'Discover bursaries and careers', icon: <BriefcaseBusiness className="h-4 w-4" /> },
            ]
            : [
              { label: 'AI Career Guidance', icon: <Sparkles className="h-4 w-4" /> },
              { label: 'Smart School Management', icon: <School className="h-4 w-4" /> },
              { label: 'Data-Driven Insights', icon: <Users className="h-4 w-4" /> },
              { label: 'Opportunities for All', icon: <BriefcaseBusiness className="h-4 w-4" /> },
            ]).map((feature) => (
            <div key={feature.label} className="flex items-start gap-3 rounded-2xl border border-white/20 bg-white/10 px-4 py-3 text-sm text-white backdrop-blur-sm">
              <span className="mt-0.5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/10">
                {feature.icon}
              </span>
              <span className="leading-6">{feature.label}</span>
            </div>
          ))}
        </div>
        <div className="mt-12 rounded-[24px] border border-white/15 bg-white/10 p-5 backdrop-blur-sm">
          <p className="text-sm font-semibold text-white">Platform Status</p>
          <div className="mt-4 flex items-center gap-3">
            <span className="inline-flex h-4 w-4 rounded-full bg-emerald-400 shadow-[0_0_0_6px_rgba(16,185,129,0.18)]" />
            <div>
              <p className="text-sm font-medium text-white">All systems operational</p>
              <p className="mt-1 text-xs text-blue-100">Last updated: May 25, 2025 10:30 AM</p>
            </div>
          </div>
        </div>
        <div className="mt-16 flex items-center gap-3 text-sm text-blue-100">
          <ShieldCheck className="h-5 w-5 text-emerald-300" />
          <span>Secure</span>
          <span>•</span>
          <span>Reliable</span>
          <span>•</span>
          <span>Always Learning</span>
        </div>
      </div>

      <div className="bg-white p-6 sm:p-8 md:p-10 lg:p-12">
        {children}
      </div>
    </div>
  </section>
);

const LoginAuthShell = ({ children, role }: { children: React.ReactNode; role: AuthRole }) => (
  <section className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(37,99,235,0.12),_transparent_38%),linear-gradient(180deg,#f8fafc_0%,#eff6ff_100%)] px-4 py-6 md:px-6 lg:px-8">
    <div className="mx-auto grid min-h-[calc(100vh-3rem)] max-w-[1500px] overflow-hidden rounded-[32px] border border-white/60 bg-white/70 shadow-[0_32px_120px_-56px_rgba(15,23,42,0.4)] backdrop-blur-sm lg:grid-cols-[minmax(420px,1fr)_minmax(0,1fr)]">
      <div className="relative hidden overflow-hidden bg-[linear-gradient(145deg,#0f3ca9_0%,#2563eb_55%,#60a5fa_100%)] p-8 text-white lg:flex lg:flex-col lg:justify-between xl:p-12">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(255,255,255,0.22),_transparent_28%),radial-gradient(circle_at_bottom_right,_rgba(191,219,254,0.28),_transparent_24%)]" aria-hidden="true" />
        <div className="relative">
          <div className="flex justify-end">
            <span className="rounded-full border border-white/25 bg-white/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.22em] text-white/90">
              {roleLabel[role]} Portal
            </span>
          </div>

          <div className="mt-16 max-w-xl">
            <p className="text-sm font-semibold uppercase tracking-[0.24em] text-blue-100/90">EduRite Platform</p>
            <h1 className="mt-4 text-[40px] font-semibold leading-tight tracking-tight xl:text-[52px]">
              Everything your education ecosystem needs in one secure place.
            </h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-blue-50/92">
              A unified platform for student growth, school operations, district visibility, and company engagement.
            </p>
          </div>

          <div className="mt-10 grid gap-3">
            {[
              { label: 'AI career guidance and student planning', icon: <Sparkles className="h-4 w-4" /> },
              { label: 'School, teacher, and learner workflows', icon: <School className="h-4 w-4" /> },
              { label: 'District reporting and intervention visibility', icon: <Users className="h-4 w-4" /> },
              { label: 'Company bursaries and talent pipelines', icon: <BriefcaseBusiness className="h-4 w-4" /> },
            ].map((feature) => (
              <div key={feature.label} className="flex items-center gap-3 rounded-2xl border border-white/16 bg-white/10 px-4 py-3 text-sm text-white/95 backdrop-blur-md">
                <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/14">
                  {feature.icon}
                </span>
                <span>{feature.label}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative rounded-[28px] border border-white/16 bg-white/12 p-5 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/14">
              <ShieldCheck className="h-5 w-5 text-emerald-200" />
            </span>
            <div>
              <p className="text-sm font-semibold text-white">Secure access for every portal</p>
              <p className="mt-1 text-sm text-blue-100/90">JWT-based sessions, role-aware routing, and protected workspaces.</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-center bg-transparent p-4 sm:p-8 lg:p-10 xl:p-14">
        {children}
      </div>
    </div>
  </section>
);

const AuthHeader = ({ role, mode }: { role: AuthRole; mode: AuthMode }) => {
  const config = roleContent[role];
  return (
    <div>
      <span className="inline-flex rounded-full bg-primary-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-primary-700">
        {config.badge}
      </span>
      <h2 className="mt-4 text-3xl font-semibold tracking-tight text-slate-900">{mode === 'login' ? config.loginTitle : config.registerTitle}</h2>
      <p className="mt-3 text-sm leading-6 text-slate-600">{mode === 'login' ? config.loginSubtitle : config.registerSubtitle}</p>
    </div>
  );
};

const LoginAccessSelector = ({
  role,
  selectedRole,
  roleOptions,
  onSelectRole,
  onNavigateToRole,
}: {
  role: AuthRole;
  selectedRole: SelectedRoleOption;
  roleOptions: Array<{ value: SelectedRoleOption; label: string; helper: string }>;
  onSelectRole: (roleOption: SelectedRoleOption) => void;
  onNavigateToRole: (roleOption: SelectedRoleOption) => void;
}) => {
  const selectedPortal: SelectedPortal = role === 'COMPANY'
    ? 'COMPANY'
    : (role === 'ADMIN' || role === 'DISTRICT_ADMIN')
      ? 'ADMIN'
      : (role === 'SCHOOL_ADMIN' || role === 'TEACHER' || role === 'SCHOOL_STUDENT')
        ? 'SCHOOL'
        : 'STUDENT';

  const portalCards: Array<{ key: SelectedPortal | 'ADMIN'; title: string; description: string; icon: React.ReactNode; to: string; accent: string }> = [
    { key: 'STUDENT', title: 'Student Portal', description: 'Study, submit and track progress', icon: <GraduationCap className="h-5 w-5" />, to: '/auth/login', accent: 'bg-blue-600 text-white' },
    { key: 'ADMIN', title: 'District Portal', description: 'Manage schools and district data', icon: <Building2 className="h-5 w-5" />, to: '/district/login', accent: 'bg-violet-600 text-white' },
    { key: 'SCHOOL', title: 'School Portal', description: 'Manage classes, tasks and workflows', icon: <School className="h-5 w-5" />, to: '/school/login', accent: 'bg-emerald-600 text-white' },
    { key: 'COMPANY', title: 'Company Portal', description: 'Manage bursaries and talent pipelines', icon: <BriefcaseBusiness className="h-5 w-5" />, to: '/company/login', accent: 'bg-amber-500 text-white' },
    { key: 'ADMIN', title: 'Admin Portal', description: 'Control platform and approvals', icon: <ShieldCheck className="h-5 w-5" />, to: '/admin/login', accent: 'bg-orange-500 text-white' },
  ];

  return (
    <div className="space-y-5">
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_64px_minmax(0,1fr)] xl:items-start">
        <div className="space-y-3">
          <p className="text-[15px] font-semibold text-slate-900">I want to access the platform as</p>
          <div className="flex h-12 items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 text-sm font-medium text-slate-800 shadow-[0_10px_30px_-24px_rgba(15,23,42,0.28)]">
            <span>Select Portal</span>
            <ChevronDown className="h-4 w-4 text-slate-400" />
          </div>
          <div className="rounded-[22px] border border-slate-200 bg-white p-3 shadow-[0_22px_50px_-36px_rgba(15,23,42,0.35)]">
            <div className="space-y-1.5">
              {portalCards.map((portal) => {
                const isActive = portal.to === '/district/login'
                  ? role === 'DISTRICT_ADMIN'
                  : portal.to === '/admin/login'
                    ? role === 'ADMIN'
                    : portal.key === selectedPortal;
                return (
                  <Link
                    key={portal.title}
                    to={portal.to}
                    className={`flex items-start gap-3 rounded-2xl px-3 py-3 transition ${isActive ? 'bg-slate-50' : 'hover:bg-slate-50'}`}
                  >
                    <span className={`inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${portal.accent}`}>
                      {portal.icon}
                    </span>
                    <span className="min-w-0">
                      <span className="block text-[15px] font-semibold text-slate-900">{portal.title}</span>
                      <span className="mt-0.5 block text-[13px] leading-5 text-slate-600">{portal.description}</span>
                    </span>
                  </Link>
                );
              })}
            </div>
          </div>
        </div>

        <div className="hidden h-12 items-center justify-center xl:flex">
          <ArrowRight className="h-8 w-8 text-slate-500" />
        </div>

        <div className="space-y-3">
          <p className="text-[15px] font-semibold text-slate-900">I want to sign in as</p>
          <div className="flex h-12 items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 text-sm font-medium text-slate-800 shadow-[0_10px_30px_-24px_rgba(15,23,42,0.28)]">
            <span>Select Login Type</span>
            <ChevronDown className="h-4 w-4 text-slate-400" />
          </div>
          <div className="rounded-[22px] border border-slate-200 bg-white p-3 shadow-[0_22px_50px_-36px_rgba(15,23,42,0.35)]">
            <div className="space-y-1.5">
              {roleOptions.map((option) => {
                const isActive = option.value === selectedRole;
                const iconClasses = option.value === 'DISTRICT_ADMIN'
                  ? 'bg-violet-50 text-violet-600'
                  : option.value === 'PLATFORM_ADMIN'
                    ? 'bg-orange-50 text-orange-600'
                    : option.value === 'SCHOOL_ADMIN'
                      ? 'bg-blue-50 text-blue-600'
                      : option.value === 'TEACHER'
                        ? 'bg-emerald-50 text-emerald-600'
                        : option.value === 'SCHOOL_STUDENT'
                          ? 'bg-sky-50 text-sky-600'
                          : option.value === 'COMPANY'
                            ? 'bg-amber-50 text-amber-600'
                            : 'bg-blue-50 text-blue-600';

                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => {
                      onSelectRole(option.value);
                      onNavigateToRole(option.value);
                    }}
                    className={`flex w-full items-start gap-3 rounded-2xl border px-3 py-3 text-left transition ${isActive ? 'border-primary-300 bg-primary-50/70 shadow-[0_0_0_1px_rgba(37,99,235,0.08)]' : 'border-transparent hover:border-slate-200 hover:bg-slate-50'}`}
                  >
                    <span className={`inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${iconClasses}`}>
                      <UserCheck className="h-5 w-5" />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-[15px] font-semibold text-slate-900">{option.label} Login</span>
                      <span className="mt-0.5 block text-[13px] leading-5 text-slate-600">{option.helper}</span>
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const SignInForm = ({ role }: { role: AuthRole }) => {
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | undefined)?.from?.pathname;
  const [serverError, setServerError] = useState<string | null>(null);
  const [form, setForm] = useState<LoginFormValues>({ email: '', schoolName: '', emisNumber: '', password: '', rememberMe: true });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGoogleSubmitting, setIsGoogleSubmitting] = useState(false);
  const googleButtonRef = useRef<HTMLDivElement | null>(null);
  const googleSectionEnabled = role !== 'ADMIN' && GOOGLE_SIGNIN_ENABLED;
  const googleClientConfigured = Boolean(GOOGLE_CLIENT_ID);
  const googleClientLooksValid = GOOGLE_CLIENT_ID_PATTERN.test(GOOGLE_CLIENT_ID);
  const googleSignInAvailable = googleSectionEnabled && googleClientConfigured && googleClientLooksValid;
  const googleLoginRole: 'STUDENT' | 'COMPANY' = role === 'COMPANY' ? 'COMPANY' : 'STUDENT';

  const routeAuthenticatedUser = useCallback(async (loggedInUser: User) => {
    const primaryRole = resolvePrimaryRole(loggedInUser);
    if (!primaryRole) {
      throw new Error('Signed in successfully, but no supported role was returned for this account.');
    }

    const roleMismatch = primaryRole !== role
      ? `This account is signed in as ${primaryRole.toLowerCase()}, so EduRite redirected you to the ${primaryRole.toLowerCase()} workspace.`
      : undefined;
    if (roleMismatch && import.meta.env.DEV) {
      console.warn('[auth] role mismatch detected during sign-in', { selectedRole: role, authenticatedRole: primaryRole, fromPath: from });
    }

    if (primaryRole === 'ADMIN') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/admin/dashboard';
      navigate(finalPath, {
        replace: true,
        state: roleMismatch ? { roleMismatch } : undefined,
      });
      return;
    }

    if (primaryRole === 'DISTRICT_ADMIN') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/district/dashboard';
      navigate(finalPath, {
        replace: true,
        state: roleMismatch ? { roleMismatch } : undefined,
      });
      return;
    }

    if (primaryRole === 'STUDENT') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/student/dashboard';
      navigate(finalPath, {
        replace: true,
        state: roleMismatch ? { roleMismatch } : undefined,
      });
      return;
    }
    if (primaryRole === 'SCHOOL_ADMIN') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : getDashboardPathForUser(loggedInUser) ?? '/school/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'TEACHER') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : '/teacher/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'SCHOOL_STUDENT') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : '/school-student/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }

    const dashboardPath = primaryRole === 'COMPANY'
      ? getCompanyPathForApprovalStatus(loggedInUser.approvalStatus)
      : getDashboardPathForRole(primaryRole);
    const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : dashboardPath ?? '/auth/login';
    navigate(finalPath, {
      replace: true,
      state: roleMismatch ? { roleMismatch } : undefined,
    });
  }, [from, navigate, role]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setServerError(null);
    setIsSubmitting(true);

    try {
      const loggedInUser = await login({ email: form.email, password: form.password }, { rememberMe: form.rememberMe });
      const expectedRole = expectedRoleForSelection(selectedRole);
      const actualRole = resolvePrimaryRole(loggedInUser);
      if (expectedRole && actualRole && expectedRole !== actualRole) {
        setServerError(
          selectedPortal === 'SCHOOL'
            ? 'This account does not match the selected role. Please choose the correct school role.'
            : 'This account does not match the selected portal. Please choose the correct portal.',
        );
        return;
      }
      await routeAuthenticatedUser(loggedInUser);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to sign you in.';
      setServerError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!import.meta.env.DEV || !googleSectionEnabled) {
      return;
    }

    if (!googleClientConfigured) {
      console.warn('[auth] Google sign-in is enabled but VITE_GOOGLE_CLIENT_ID is missing.');
      return;
    }

    if (!googleClientLooksValid) {
      console.warn('[auth] Google sign-in client ID looks invalid. Expected *.apps.googleusercontent.com.', { clientId: GOOGLE_CLIENT_ID });
    }
  }, [googleClientConfigured, googleClientLooksValid, googleSectionEnabled]);

  useEffect(() => {
    if (!googleSignInAvailable || !googleButtonRef.current) {
      return;
    }

    let active = true;
    loadGoogleIdentityScript()
      .then(() => {
        if (!active) return;
        const googleIdentity = window.google?.accounts?.id;
        if (!googleIdentity) {
          setServerError(GOOGLE_UNAVAILABLE_MESSAGE);
          return;
        }

        googleIdentity.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: async (response) => {
            const credential = response?.credential;
            if (!credential) {
              setServerError('Unable to sign in with Google. Please try again.');
              return;
            }

            setServerError(null);
            setIsGoogleSubmitting(true);
            try {
              const loggedInUser = await loginWithGoogle(credential, googleLoginRole, { rememberMe: form.rememberMe });
              await routeAuthenticatedUser(loggedInUser);
            } catch (error) {
              const message = error instanceof Error ? error.message : '';
              const looksTechnicalGoogleFailure = message.toLowerCase().includes('invalid_client')
                || message.toLowerCase().includes('oauth')
                || message.toLowerCase().includes('origin')
                || message.toLowerCase().includes('google');
              setServerError(looksTechnicalGoogleFailure ? GOOGLE_UNAVAILABLE_MESSAGE : (message || 'Unable to sign in with Google.'));
            } finally {
              if (active) {
                setIsGoogleSubmitting(false);
              }
            }
          },
        });

        googleButtonRef.current!.innerHTML = '';
        googleIdentity.renderButton(googleButtonRef.current!, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          width: Math.max(googleButtonRef.current!.offsetWidth, 280),
        });
      })
      .catch(() => {
        if (!active) return;
        setServerError(GOOGLE_UNAVAILABLE_MESSAGE);
      });

    return () => {
      active = false;
    };
  }, [form.rememberMe, googleLoginRole, googleSignInAvailable, googleClientLooksValid, loginWithGoogle, routeAuthenticatedUser]);

  const locationState = location.state as { roleMismatch?: string; registrationMessage?: string; sessionExpiredMessage?: string } | undefined;
  const mismatchMessage = locationState?.roleMismatch;
  const registrationMessage = locationState?.registrationMessage;
  const sessionExpiredMessage = locationState?.sessionExpiredMessage;

  const selectedPortal: SelectedPortal = role === 'COMPANY'
    ? 'COMPANY'
    : (role === 'ADMIN' || role === 'DISTRICT_ADMIN')
      ? 'ADMIN'
      : (role === 'SCHOOL_ADMIN' || role === 'TEACHER' || role === 'SCHOOL_STUDENT')
        ? 'SCHOOL'
        : 'STUDENT';

  const defaultRoleForPortal = useMemo<SelectedRoleOption>(() => {
    if (selectedPortal === 'SCHOOL') {
      if (role === 'TEACHER') return 'TEACHER';
      if (role === 'SCHOOL_STUDENT') return 'SCHOOL_STUDENT';
      return 'SCHOOL_ADMIN';
    }
    if (selectedPortal === 'COMPANY') return 'COMPANY';
    if (selectedPortal === 'ADMIN') return role === 'DISTRICT_ADMIN' ? 'DISTRICT_ADMIN' : 'PLATFORM_ADMIN';
    return 'STUDENT';
  }, [role, selectedPortal]);

  const [selectedRole, setSelectedRole] = useState<SelectedRoleOption>(defaultRoleForPortal);

  useEffect(() => {
    setSelectedRole(defaultRoleForPortal);
  }, [defaultRoleForPortal, selectedPortal]);

  const roleOptions: Array<{ value: SelectedRoleOption; label: string; helper: string }> = selectedPortal === 'SCHOOL'
    ? [
      { value: 'SCHOOL_ADMIN', label: 'School Admin', helper: 'Manage learners, teachers, classes, subjects, assessments, and reports.' },
      { value: 'TEACHER', label: 'Teacher', helper: 'Upload notes, create SBA tasks, mark submissions, and manage classes.' },
      { value: 'SCHOOL_STUDENT', label: 'Learner', helper: 'View notes, submit SBA tasks, write assessments, and track progress.' },
    ]
    : selectedPortal === 'COMPANY'
      ? [
        { value: 'COMPANY', label: 'Company', helper: 'Sign in to manage company opportunities, bursaries, talent pipelines, and applications.' },
      ]
    : selectedPortal === 'ADMIN'
      ? [
        role === 'DISTRICT_ADMIN'
          ? { value: 'DISTRICT_ADMIN', label: 'District Admin', helper: 'District oversight for schools, reports, interventions, and readiness.' }
          : { value: 'PLATFORM_ADMIN', label: 'Platform Admin', helper: 'Manage users, approvals, content, subscriptions, companies, schools, and platform settings.' },
        ]
        : [
          { value: 'STUDENT', label: 'Student', helper: 'Access AI career guidance, bursaries, learning resources, applications, and your personal dashboard.' },
        ];

  const selectedRoleMeta = roleOptions.find((option) => option.value === selectedRole) ?? roleOptions[0];
  const currentPortalLabel = selectedRole === 'DISTRICT_ADMIN'
    ? 'EduRite District Portal'
    : selectedRole === 'PLATFORM_ADMIN'
      ? 'EduRite Admin Portal'
      : selectedRole === 'SCHOOL_ADMIN'
        ? 'EduRite School Portal'
        : selectedRole === 'TEACHER'
          ? 'EduRite Teacher Portal'
          : selectedRole === 'SCHOOL_STUDENT'
            ? 'EduRite Learner Portal'
            : selectedRole === 'COMPANY'
              ? 'EduRite Company Portal'
              : 'EduRite Student Portal';

  const submitLabel = (() => {
    if (selectedRole === 'SCHOOL_STUDENT') return 'Sign in as Learner';
    if (selectedRole === 'TEACHER') return 'Sign in as Teacher';
    if (selectedRole === 'SCHOOL_ADMIN') return 'Sign in as School Admin';
    if (selectedRole === 'COMPANY') return 'Sign in to Company Portal';
    if (selectedRole === 'DISTRICT_ADMIN') return 'Sign in as District Admin';
    if (selectedRole === 'PLATFORM_ADMIN') return 'Sign in as Platform Admin';
    return 'Sign in to AI Career Guidance';
  })();

  const expectedRoleForSelection = (selection: SelectedRoleOption): Role | null => {
    if (selection === 'TEACHER') return 'TEACHER';
    if (selection === 'SCHOOL_ADMIN') return 'SCHOOL_ADMIN';
    if (selection === 'SCHOOL_STUDENT') return 'SCHOOL_STUDENT';
    if (selection === 'COMPANY') return 'COMPANY';
    if (selection === 'DISTRICT_ADMIN') return 'DISTRICT_ADMIN';
    if (selection === 'PLATFORM_ADMIN') return 'ADMIN';
    if (selection === 'STUDENT') return 'STUDENT';
    return null;
  };

  return (
    <form className="space-y-6" onSubmit={handleSubmit}>
      <LoginAccessSelector
        role={role}
        selectedRole={selectedRole}
        roleOptions={roleOptions}
        onSelectRole={setSelectedRole}
        onNavigateToRole={(selection) => {
          const targetPath = loginPathForSelection(selection);
          if (targetPath !== location.pathname) {
            navigate(targetPath);
          }
        }}
      />

      <div className="space-y-2">
        <h2 className="text-[20px] font-bold tracking-tight text-slate-950 sm:text-[22px]">Welcome back!</h2>
        <p className="text-[14px] leading-6 text-slate-600">
          Sign in to continue to the {currentPortalLabel}.
        </p>
        <p className="text-[13px] text-slate-500">{selectedRoleMeta.helper}</p>
      </div>
      {mismatchMessage ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{mismatchMessage}</div> : null}
      {registrationMessage ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{registrationMessage}</div> : null}
      {sessionExpiredMessage ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{sessionExpiredMessage}</div> : null}
      {googleSectionEnabled && !googleClientConfigured ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Google sign-in is enabled, but this build is missing a valid Google client ID.
        </div>
      ) : null}
      <div className="space-y-4">
        <label className="block text-sm font-medium text-slate-700">
          Email or username
          <div className="relative mt-2">
            <Mail className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              type="text"
              autoComplete="username"
              value={form.email}
              onChange={(event) => {
                setForm((current) => ({ ...current, email: event.target.value }));
              }}
              className="h-12 rounded-2xl border-slate-200 bg-white pl-11 pr-4 text-sm shadow-[0_10px_30px_-24px_rgba(15,23,42,0.28)]"
              placeholder="Enter your email or username"
              required
            />
          </div>
        </label>

        <label className="block text-sm font-medium text-slate-700">
          Password
          <div className="relative mt-2">
            <Lock className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(event) => {
                setForm((current) => ({ ...current, password: event.target.value }));
              }}
              className="h-12 rounded-2xl border-slate-200 bg-white pl-11 pr-4 text-sm shadow-[0_10px_30px_-24px_rgba(15,23,42,0.28)]"
              placeholder="Enter your password"
              required
            />
          </div>
        </label>
      </div>

      <div className="flex flex-col gap-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
        <label className="inline-flex items-center gap-3 font-medium text-slate-700">
          <input
            type="checkbox"
            checked={form.rememberMe}
            onChange={(event) => setForm((current) => ({ ...current, rememberMe: event.target.checked }))}
            className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
          />
          Remember me
        </label>
        <Link className="font-semibold text-primary-600 hover:text-primary-500" to={getForgotPasswordPath(role)}>
          Forgot password?
        </Link>
      </div>

      {serverError ? <p className="text-sm text-red-600">{serverError}</p> : null}

      <Button disabled={isSubmitting} type="submit" className="h-11 w-full rounded-2xl px-5 text-[15px] shadow-lg shadow-primary-600/20">
        {isSubmitting ? 'Signing in...' : submitLabel}
      </Button>
      <GoogleAuthSection
        enabled={googleSectionEnabled}
        available={googleSignInAvailable}
        isSubmitting={isGoogleSubmitting}
        buttonRef={googleButtonRef}
      />

      <p className="text-center text-xs text-slate-600">
        {role === 'ADMIN' ? (
          <>
            Need admin access? <span className="font-medium text-slate-800">Use your provisioned internal credentials.</span>
          </>
        ) : (
          <>
            Don&apos;t have an account?{' '}
            <Link className="font-semibold text-primary-600 hover:text-primary-500" to={roleContent[role].registerPath ?? '/auth/register/student'}>
              Sign up
            </Link>
          </>
        )}
      </p>
      <div className="border-t border-slate-200 pt-2 text-center text-[11px] text-slate-500">
        © 2026 EduRite · <Link to="/privacy-policy" className="hover:text-slate-700">Privacy Policy</Link> · <Link to="/terms-and-conditions" className="hover:text-slate-700">Terms of Service</Link> · <Link to="/about" className="hover:text-slate-700">Contact Us</Link>
      </div>
    </form>
  );
};

void SignInForm;

const PremiumSignInForm = ({ role }: { role: AuthRole }) => {
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | undefined)?.from?.pathname;
  const [serverError, setServerError] = useState<string | null>(null);
  const [form, setForm] = useState<LoginFormValues>({ email: '', schoolName: '', emisNumber: '', password: '', rememberMe: true });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGoogleSubmitting, setIsGoogleSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const googleButtonRef = useRef<HTMLDivElement | null>(null);
  const googleSectionEnabled = role !== 'ADMIN' && GOOGLE_SIGNIN_ENABLED;
  const googleClientConfigured = Boolean(GOOGLE_CLIENT_ID);
  const googleClientLooksValid = GOOGLE_CLIENT_ID_PATTERN.test(GOOGLE_CLIENT_ID);
  const googleSignInAvailable = googleSectionEnabled && googleClientConfigured && googleClientLooksValid;
  const googleLoginRole: 'STUDENT' | 'COMPANY' = role === 'COMPANY' ? 'COMPANY' : 'STUDENT';
  const selectedRole: SelectedRoleOption = role === 'ADMIN'
    ? 'PLATFORM_ADMIN'
    : role === 'DISTRICT_DIRECTOR' || role === 'CIRCUIT_MANAGER' || role === 'SUBJECT_ADVISOR'
      ? 'DISTRICT_ADMIN'
      : role === 'SCHOOL_STUDENT'
        ? 'STUDENT'
        : role;

  const routeAuthenticatedUser = useCallback(async (loggedInUser: User) => {
    const primaryRole = resolvePrimaryRole(loggedInUser);
    if (!primaryRole) {
      throw new Error('Signed in successfully, but no supported role was returned for this account.');
    }

    const roleMismatch = primaryRole !== role
      ? `This account is signed in as ${primaryRole.toLowerCase()}, so EduRite redirected you to the ${primaryRole.toLowerCase()} workspace.`
      : undefined;

    if (primaryRole === 'ADMIN') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/admin/dashboard';
      navigate(finalPath, { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'DISTRICT_ADMIN') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/district/dashboard';
      navigate(finalPath, { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'STUDENT') {
      const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : '/student/dashboard';
      navigate(finalPath, { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'SCHOOL_ADMIN') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : getDashboardPathForUser(loggedInUser) ?? '/school/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'TEACHER') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : '/teacher/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }
    if (primaryRole === 'SCHOOL_STUDENT') {
      navigate(isAuthorizedPathForRole(from, primaryRole) ? from! : '/school-student/dashboard', { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
      return;
    }

    const dashboardPath = primaryRole === 'COMPANY'
      ? getCompanyPathForApprovalStatus(loggedInUser.approvalStatus)
      : getDashboardPathForRole(primaryRole);
    const finalPath = isAuthorizedPathForRole(from, primaryRole) ? from! : dashboardPath ?? '/auth/login';
    navigate(finalPath, { replace: true, state: roleMismatch ? { roleMismatch } : undefined });
  }, [from, navigate, role]);

  const expectedRoleForSelection = (selection: SelectedRoleOption): Role | null => {
    if (selection === 'TEACHER') return 'TEACHER';
    if (selection === 'SCHOOL_ADMIN') return 'SCHOOL_ADMIN';
    if (selection === 'SCHOOL_STUDENT') return 'SCHOOL_STUDENT';
    if (selection === 'COMPANY') return 'COMPANY';
    if (selection === 'DISTRICT_ADMIN') return 'DISTRICT_ADMIN';
    if (selection === 'PLATFORM_ADMIN') return 'ADMIN';
    if (selection === 'STUDENT') return 'STUDENT';
    return null;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setServerError(null);
    setIsSubmitting(true);

    try {
      const loginPayload = selectedRole === 'SCHOOL_ADMIN'
        ? { schoolName: form.schoolName, emisNumber: form.emisNumber, password: form.password }
        : { email: form.email, password: form.password };
      const loggedInUser = await login(loginPayload, { rememberMe: form.rememberMe });
      const expectedRole = expectedRoleForSelection(selectedRole);
      const actualRole = resolvePrimaryRole(loggedInUser);
      if (expectedRole && actualRole && expectedRole !== actualRole) {
        setServerError(
          selectedRole === 'SCHOOL_ADMIN' || selectedRole === 'TEACHER'
            ? 'This account does not match the selected role. Please choose the correct school role.'
            : 'This account does not match the selected portal. Please choose the correct portal.',
        );
        return;
      }
      await routeAuthenticatedUser(loggedInUser);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to sign you in.';
      setServerError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!import.meta.env.DEV || !googleSectionEnabled) return;
    if (!googleClientConfigured) {
      console.warn('[auth] Google sign-in is enabled but VITE_GOOGLE_CLIENT_ID is missing.');
      return;
    }
    if (!googleClientLooksValid) {
      console.warn('[auth] Google sign-in client ID looks invalid. Expected *.apps.googleusercontent.com.', { clientId: GOOGLE_CLIENT_ID });
    }
  }, [googleClientConfigured, googleClientLooksValid, googleSectionEnabled]);

  useEffect(() => {
    if (selectedRole === 'SCHOOL_ADMIN') {
      setForm((current) => ({
        ...current,
        email: '',
        schoolName: '',
        emisNumber: '',
        password: '',
      }));
      return;
    }

    const demoCredentials = demoCredentialsByRole[
      selectedRole === 'PLATFORM_ADMIN'
        ? 'PLATFORM_ADMIN'
        : selectedRole === 'DISTRICT_ADMIN'
          ? 'DISTRICT_ADMIN'
          : selectedRole === 'TEACHER'
            ? 'TEACHER'
            : 'STUDENT'
    ];

    setForm((current) => ({
      ...current,
      email: demoCredentials.username,
      schoolName: '',
      emisNumber: '',
      password: demoCredentials.password,
    }));
  }, [selectedRole]);

  useEffect(() => {
    if (!googleSignInAvailable || !googleButtonRef.current) {
      return;
    }

    let active = true;
    loadGoogleIdentityScript()
      .then(() => {
        if (!active) return;
        const googleIdentity = window.google?.accounts?.id;
        if (!googleIdentity) {
          setServerError(GOOGLE_UNAVAILABLE_MESSAGE);
          return;
        }

        googleIdentity.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: async (response) => {
            const credential = response?.credential;
            if (!credential) {
              setServerError('Unable to sign in with Google. Please try again.');
              return;
            }

            setServerError(null);
            setIsGoogleSubmitting(true);
            try {
              const loggedInUser = await loginWithGoogle(credential, googleLoginRole, { rememberMe: form.rememberMe });
              await routeAuthenticatedUser(loggedInUser);
            } catch (error) {
              const message = error instanceof Error ? error.message : '';
              const looksTechnicalGoogleFailure = message.toLowerCase().includes('invalid_client')
                || message.toLowerCase().includes('oauth')
                || message.toLowerCase().includes('origin')
                || message.toLowerCase().includes('google');
              setServerError(looksTechnicalGoogleFailure ? GOOGLE_UNAVAILABLE_MESSAGE : (message || 'Unable to sign in with Google.'));
            } finally {
              if (active) {
                setIsGoogleSubmitting(false);
              }
            }
          },
        });

        googleButtonRef.current!.innerHTML = '';
        googleIdentity.renderButton(googleButtonRef.current!, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          width: Math.max(googleButtonRef.current!.offsetWidth, 280),
        });
      })
      .catch(() => {
        if (!active) return;
        setServerError(GOOGLE_UNAVAILABLE_MESSAGE);
      });

    return () => {
      active = false;
    };
  }, [form.rememberMe, googleLoginRole, googleSignInAvailable, loginWithGoogle, routeAuthenticatedUser]);

  const locationState = location.state as { roleMismatch?: string; registrationMessage?: string; sessionExpiredMessage?: string } | undefined;
  const mismatchMessage = locationState?.roleMismatch;
  const registrationMessage = locationState?.registrationMessage;
  const sessionExpiredMessage = locationState?.sessionExpiredMessage;
  const roleOptions: Array<{ value: SelectedRoleOption; label: string }> = [
    { value: 'STUDENT', label: 'Student' },
    { value: 'TEACHER', label: 'Teacher' },
    { value: 'SCHOOL_ADMIN', label: 'School Admin' },
    { value: 'DISTRICT_ADMIN', label: 'District' },
    { value: 'PLATFORM_ADMIN', label: 'Admin' },
  ];

  const portalTitle = selectedRole === 'DISTRICT_ADMIN'
    ? 'EduRite District Portal'
    : selectedRole === 'PLATFORM_ADMIN'
      ? 'EduRite Admin Portal'
      : selectedRole === 'SCHOOL_ADMIN'
        ? 'EduRite School Portal'
        : selectedRole === 'TEACHER'
          ? 'EduRite Teacher Portal'
          : selectedRole === 'COMPANY'
            ? 'EduRite Company Portal'
            : 'EduRite Student Portal';

  const portalHelperText = selectedRole === 'DISTRICT_ADMIN'
    ? 'Need district access? Contact your system administrator.'
    : selectedRole === 'PLATFORM_ADMIN'
      ? 'Use your provisioned internal credentials for secure platform administration.'
      : selectedRole === 'SCHOOL_ADMIN'
        ? 'Manage learners, teachers, classes, subjects, assessments, and reports.'
        : selectedRole === 'TEACHER'
          ? 'Upload notes, create SBA tasks, mark submissions, and manage your classes.'
          : selectedRole === 'COMPANY'
            ? 'Manage bursaries, applications, opportunities, and talent pipelines.'
            : 'Access AI career guidance, bursaries, learning resources, and your student dashboard.';

  return (
    <form className="w-full max-w-[420px] rounded-[24px] border border-[#e2e8f0] bg-white p-6 shadow-[0_30px_80px_-42px_rgba(15,23,42,0.35)] sm:p-8" onSubmit={handleSubmit}>
      <div className="space-y-6">
        <div className="space-y-4 text-center">
          <div className="mx-auto inline-flex min-h-14 items-center justify-center rounded-2xl bg-gradient-to-br from-[#dbeafe] to-[#bfdbfe] px-4 py-3 shadow-inner shadow-white">
            <EduRiteLogo className="h-auto w-[110px]" />
          </div>
          <div className="space-y-2">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#2563eb]">{portalTitle}</p>
            <h2 className="text-[30px] font-semibold tracking-tight text-[#0f172a]">Welcome back</h2>
            <p className="text-sm text-[#64748b]">Sign in to continue</p>
            <p className="text-sm leading-6 text-[#64748b]">{portalHelperText}</p>
          </div>
        </div>

        <div className="space-y-4">
          <label className="block text-sm font-medium text-[#0f172a]">
            Portal
            <select
              value={selectedRole}
              onChange={(event) => {
                const nextPath = loginPathForSelection(event.target.value as SelectedRoleOption);
                if (nextPath !== location.pathname) {
                  navigate(nextPath);
                }
              }}
              className="mt-2 h-[52px] w-full rounded-2xl border border-[#dbe3ef] bg-white px-4 text-sm text-[#0f172a] outline-none transition focus:border-[#2563eb] focus:ring-2 focus:ring-[#bfdbfe]"
            >
              {roleOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          {mismatchMessage ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{mismatchMessage}</div> : null}
          {registrationMessage ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{registrationMessage}</div> : null}
          {sessionExpiredMessage ? <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{sessionExpiredMessage}</div> : null}
          {googleSectionEnabled && !googleClientConfigured ? (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
              Google sign-in is enabled, but this build is missing a valid Google client ID.
            </div>
          ) : null}

          {selectedRole === 'SCHOOL_ADMIN' ? (
            <>
              <label className="block text-sm font-medium text-[#0f172a]">
                School Name
                <div className="relative mt-2">
                  <School className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#64748b]" />
                  <Input
                    type="text"
                    autoComplete="organization"
                    value={form.schoolName}
                    onChange={(event) => {
                      setForm((current) => ({ ...current, schoolName: event.target.value }));
                    }}
                    className="h-[52px] rounded-2xl border-[#dbe3ef] bg-white pl-11 pr-4 text-sm text-[#0f172a] shadow-none focus:border-[#2563eb] focus:ring-[#bfdbfe]"
                    placeholder="Enter school name"
                    required
                  />
                </div>
              </label>
              <label className="block text-sm font-medium text-[#0f172a]">
                EMIS Number
                <div className="relative mt-2">
                  <BookOpen className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#64748b]" />
                  <Input
                    type="text"
                    value={form.emisNumber}
                    onChange={(event) => {
                      setForm((current) => ({ ...current, emisNumber: event.target.value }));
                    }}
                    className="h-[52px] rounded-2xl border-[#dbe3ef] bg-white pl-11 pr-4 text-sm text-[#0f172a] shadow-none focus:border-[#2563eb] focus:ring-[#bfdbfe]"
                    placeholder="Enter EMIS number"
                    required
                  />
                </div>
              </label>
            </>
          ) : (
            <label className="block text-sm font-medium text-[#0f172a]">
              Username
              <div className="relative mt-2">
                <UserIcon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#64748b]" />
                <Input
                  type="text"
                  autoComplete="username"
                  value={form.email}
                  onChange={(event) => {
                    setForm((current) => ({ ...current, email: event.target.value }));
                  }}
                  className="h-[52px] rounded-2xl border-[#dbe3ef] bg-white pl-11 pr-4 text-sm text-[#0f172a] shadow-none focus:border-[#2563eb] focus:ring-[#bfdbfe]"
                  placeholder="Enter your email or username"
                  required
                />
              </div>
            </label>
          )}

          <label className="block text-sm font-medium text-[#0f172a]">
            Password
            <div className="relative mt-2">
              <Lock className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#64748b]" />
              <Input
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={form.password}
                onChange={(event) => {
                  setForm((current) => ({ ...current, password: event.target.value }));
                }}
                className="h-[52px] rounded-2xl border-[#dbe3ef] bg-white pl-11 pr-12 text-sm text-[#0f172a] shadow-none focus:border-[#2563eb] focus:ring-[#bfdbfe]"
                placeholder="Enter your password"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#64748b] transition hover:text-[#0f172a]"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </label>
        </div>

        <div className="flex items-center justify-between gap-3 text-sm text-[#64748b]">
          <label className="inline-flex items-center gap-3 font-medium text-[#334155]">
            <input
              type="checkbox"
              checked={form.rememberMe}
              onChange={(event) => setForm((current) => ({ ...current, rememberMe: event.target.checked }))}
              className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
            />
            Remember me
          </label>
          <Link className="font-semibold text-[#2563eb] hover:text-[#1d4ed8]" to={getForgotPasswordPath(role)}>
            Forgot password?
          </Link>
        </div>

        {serverError ? <p className="text-sm text-red-600">{serverError}</p> : null}

        <Button
          disabled={isSubmitting}
          type="submit"
          className="h-14 w-full rounded-2xl bg-[linear-gradient(135deg,#2563eb_0%,#1d4ed8_100%)] px-5 text-[15px] font-semibold text-white shadow-[0_18px_36px_-18px_rgba(37,99,235,0.65)] hover:bg-[linear-gradient(135deg,#2563eb_0%,#1d4ed8_100%)] sm:w-full"
        >
          {isSubmitting ? 'Signing in...' : 'Sign In'}
        </Button>

        <GoogleAuthSection
          enabled={googleSectionEnabled}
          available={googleSignInAvailable}
          isSubmitting={isGoogleSubmitting}
          buttonRef={googleButtonRef}
        />

        <p className="text-center text-xs text-slate-600">
          {role === 'ADMIN' ? (
            <>
              Need admin access? <span className="font-medium text-slate-800">Use your provisioned internal credentials.</span>
            </>
          ) : (
            <>
              Don&apos;t have an account?{' '}
              <Link className="font-semibold text-primary-600 hover:text-primary-500" to={roleContent[role].registerPath ?? '/auth/register/student'}>
                Sign up
              </Link>
            </>
          )}
        </p>
        <p className="text-center text-[11px] text-slate-500">
          © 2026 EduRite · <Link to="/privacy-policy" className="hover:text-slate-700">Privacy Policy</Link> · <Link to="/terms-and-conditions" className="hover:text-slate-700">Terms of Service</Link> · <Link to="/about" className="hover:text-slate-700">Contact Us</Link>
        </p>
      </div>
    </form>
  );
};

const VerificationNoticeCard = ({
  phoneNumber,
  role,
  title,
  message,
}: {
  phoneNumber: string;
  role: AuthRole;
  title?: string;
  message?: string;
}) => {
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [phone, setPhone] = useState(phoneNumber);
  const otpDeliveryDelayed = (message ?? '').toLowerCase().includes('could not send');

  const resend = async () => {
    setStatus(null);
    setError(null);
    const safePhone = phone.trim();
    if (!safePhone) {
      setError('Enter the phone number used during registration.');
      return;
    }
    setIsSubmitting(true);
    try {
      const response = await authService.resendVerificationOtp({ phoneNumber: safePhone });
      setStatus(response.message);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to resend verification OTP.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mt-8 space-y-5">
      <div className="rounded-2xl border border-sky-100 bg-sky-50 px-5 py-4 text-sm text-slate-700">
        <p className="font-semibold text-slate-900">{title ?? 'Verify your phone number'}</p>
        <p className="mt-2 leading-6">{message ?? 'Your account has been created, but it stays inactive until you confirm your phone number with OTP.'}</p>
        {otpDeliveryDelayed
          ? <p className="mt-2 text-amber-800">Registration succeeded. If no OTP arrives yet, use the resend button below.</p>
          : <p className="mt-2">If you do not receive the OTP, use <span className="font-semibold">Resend verification OTP</span> below.</p>}
        <label className="mt-3 block font-medium text-slate-900">
          Verification phone number
          <Input
            type="tel"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="+26770000000"
            className="mt-2 rounded-2xl border-slate-200 bg-white px-4 py-3"
          />
        </label>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row">
        <Button type="button" disabled={isSubmitting || !phone.trim()} onClick={resend} className="rounded-2xl px-6 py-3 text-sm">
          {isSubmitting ? 'Resending...' : 'Resend verification OTP'}
        </Button>
        <Link to={`/auth/verify-otp?phone=${encodeURIComponent(phone.trim())}&role=${encodeURIComponent(role)}`} className="inline-flex items-center justify-center rounded-2xl border border-primary-200 px-6 py-3 text-sm font-semibold text-primary-700 transition hover:bg-primary-50">
          Enter OTP code
        </Link>
        <Link to={buildAuthPath(role, 'login')} className="inline-flex items-center justify-center rounded-2xl border border-slate-200 px-6 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50">
          Back to sign in
        </Link>
      </div>
      {status ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{status}</div> : null}
      {error ? <ErrorState message={error} /> : null}
    </div>
  );
};

export const LoginPage = () => {
  const { isAuthenticated, user, isHydrated } = useAuth();
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);

  if (isHydrated && isAuthenticated && user) {
    return <Navigate to={getRoleDashboard(user)} replace />;
  }

  return (
    <LoginAuthShell role={role}>
      <PremiumSignInForm role={role} />
    </LoginAuthShell>
  );
};

export const RegisterStudentPage = () => {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  return (
    <AuthShell role="STUDENT" mode="register">
      <RegisterPortalSelector role="STUDENT" />
      <div className="mt-5 flex justify-center">
        <div className="w-full max-w-[700px]">
        <form
          className="mt-6 grid gap-3.5 sm:grid-cols-2"
          onSubmit={async (event) => {
            event.preventDefault();
            setServerError(null);
            setIsSubmitting(true);
            const formData = new FormData(event.currentTarget);
            const firstName = String(formData.get('firstName') ?? '').trim();
            const lastName = String(formData.get('lastName') ?? '').trim();
            const email = String(formData.get('email') ?? '').trim();
            const password = String(formData.get('password') ?? '');
            const interests = String(formData.get('interests') ?? '').trim();
            const location = String(formData.get('location') ?? '').trim();
            const phone = String(formData.get('phone') ?? '').trim();
            const dateOfBirth = String(formData.get('dateOfBirth') ?? '').trim();
            const gender = String(formData.get('gender') ?? '').trim();
            const qualificationLevel = String(formData.get('qualificationLevel') ?? '').trim();
            const popiaConsentAccepted = String(formData.get('popiaConsentAccepted') ?? '') === 'on';
            const consentVersion = POPIA_CONSENT_VERSION;

            if (!popiaConsentAccepted) {
              setServerError('You must agree to the Privacy Policy and Terms & Conditions to create an account.');
              setIsSubmitting(false);
              return;
            }
            if (!phone) {
              setServerError('Phone number is required.');
              setIsSubmitting(false);
              return;
            }

            try {
              const response = await authService.registerStudent({
                fullName: `${firstName} ${lastName}`.trim(),
                firstName,
                lastName,
                email,
                password,
                interests,
                location,
                phone,
                dateOfBirth,
                gender,
                qualificationLevel,
                popiaConsentAccepted,
                consentVersion,
              });
              if (response.verificationRequired) {
                navigate(`/auth/verify-otp/notice?phone=${encodeURIComponent(phone)}&role=STUDENT`, {
                  replace: true,
                  state: { message: response.message },
                });
                return;
              }
              navigate('/auth/login', {
                replace: true,
                state: { registrationMessage: response.message },
              });
            } catch (error) {
              setServerError(error instanceof Error ? error.message : 'Unable to create your account.');
            } finally {
              setIsSubmitting(false);
            }
          }}
        >
          <label className="block text-[14px] font-medium text-slate-700">
            First name
            <Input name="firstName" autoComplete="given-name" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" required />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Last name
            <Input name="lastName" autoComplete="family-name" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" required />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Email address
            <Input name="email" type="email" autoComplete="email" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" required />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Password
            <Input name="password" type="password" autoComplete="new-password" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" minLength={8} required />
          </label>
          <div className="sm:col-span-2">
            <PasswordRequirementsPanel />
          </div>
          <label className="block text-[14px] font-medium text-slate-700">
            Interests
            <Input name="interests" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" placeholder="Engineering, coding" />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Location
            <Input name="location" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Phone
            <Input name="phone" autoComplete="tel" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" required />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Date of birth
            <Input name="dateOfBirth" type="date" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Gender
            <Input name="gender" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" />
          </label>
          <label className="block text-[14px] font-medium text-slate-700">
            Qualification level
            <Input name="qualificationLevel" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" placeholder="High School" />
          </label>
          <PopiaConsentCheckbox
            version={POPIA_CONSENT_VERSION}
            label={
              <>
                I agree to the{' '}
                <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/privacy-policy">Privacy Policy</Link>{' '}
                and{' '}
                <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/terms-and-conditions">Terms &amp; Conditions</Link>.
              </>
            }
            inputProps={{ name: 'popiaConsentAccepted' }}
          />
          <div className="sm:col-span-2 space-y-3 pt-1">
            {serverError ? <ErrorState message={serverError} /> : null}
            <Button disabled={isSubmitting} type="submit" className="h-11 w-full rounded-xl px-5 text-sm shadow-lg shadow-primary-600/20">{isSubmitting ? 'Creating account...' : 'Create Student Account'}</Button>
            <p className="text-center text-sm text-slate-600">Already have an account? <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/auth/login">Sign in</Link></p>
          </div>
        </form>
        </div>
      </div>
    </AuthShell>
  );
};

export const RegisterSchoolPage = () => {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [districts, setDistricts] = useState<LocationOption[]>([]);
  const [circuits, setCircuits] = useState<LocationOption[]>([]);
  const [locationsLoading, setLocationsLoading] = useState(true);
  const [circuitsLoading, setCircuitsLoading] = useState(false);
  const { register, handleSubmit, watch, setValue, formState: { isSubmitting, errors } } = useForm<SchoolRegisterPayload>({
    defaultValues: {
      schoolName: '',
      emisNumber: '',
      districtId: '',
      circuitId: '',
      schoolType: '',
      principalName: '',
      principalEmail: '',
      schoolEmail: '',
      phoneNumber: '',
      physicalAddress: '',
      password: '',
      confirmPassword: '',
    },
  });
  const selectedDistrictId = watch('districtId');

  useEffect(() => {
    let cancelled = false;

    const loadDistricts = async () => {
      try {
        setLocationsLoading(true);
        const response = await locationService.getDistricts();
        if (!cancelled) {
          setDistricts(response);
        }
      } catch (error) {
        if (!cancelled) {
          setServerError(error instanceof Error ? error.message : 'Unable to load districts.');
        }
      } finally {
        if (!cancelled) {
          setLocationsLoading(false);
        }
      }
    };

    void loadDistricts();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    setValue('circuitId', '');
    setCircuits([]);

    if (!selectedDistrictId) {
      return;
    }

    let cancelled = false;

    const loadCircuits = async () => {
      try {
        setCircuitsLoading(true);
        const response = await locationService.getCircuits(selectedDistrictId);
        if (!cancelled) {
          setCircuits(response);
        }
      } catch (error) {
        if (!cancelled) {
          setServerError(error instanceof Error ? error.message : 'Unable to load circuits.');
        }
      } finally {
        if (!cancelled) {
          setCircuitsLoading(false);
        }
      }
    };

    void loadCircuits();
    return () => {
      cancelled = true;
    };
  }, [selectedDistrictId, setValue]);

  return (
    <AuthShell role="SCHOOL_ADMIN" mode="register">
      <RegisterPortalSelector role="SCHOOL_ADMIN" />
      <div className="mt-5 flex justify-center">
        <div className="w-full max-w-[860px]">
          <div className="mt-5 rounded-xl border border-sky-200 bg-sky-50 px-4 py-2.5 text-sm text-sky-900">
            New schools are created with <span className="font-semibold">pending district approval</span>. You can sign in immediately after registration to track the request status.
          </div>
          <form className="mt-6 grid gap-3.5 sm:grid-cols-2" onSubmit={handleSubmit(async (data) => {
            try {
              setServerError(null);
              if (data.password !== data.confirmPassword) {
                setServerError('Password and confirm password must match.');
                return;
              }
              const response = await authService.registerSchool(data);
              navigate('/school/login', {
                replace: true,
                state: { registrationMessage: response.message },
              });
            } catch (error) {
              setServerError(error instanceof Error ? error.message : 'Unable to create school registration request.');
            }
          })}>
            <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">School Name<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('schoolName', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">EMIS Number<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('emisNumber', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">
              District
              <select
                className="mt-1.5 h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-700 disabled:cursor-not-allowed disabled:bg-slate-100"
                disabled={locationsLoading}
                {...register('districtId', { required: 'Please select a district' })}
              >
                <option value="">
                  {locationsLoading ? 'Loading districts...' : 'Select district'}
                </option>
                {districts.map((district) => <option key={district.id} value={district.id}>{district.name}</option>)}
              </select>
              <SelectError message={errors.districtId?.message} />
            </label>
            <label className="block text-[14px] font-medium text-slate-700">
              Circuit
              <select
                className="mt-1.5 h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-700 disabled:cursor-not-allowed disabled:bg-slate-100"
                disabled={!selectedDistrictId || circuitsLoading}
                {...register('circuitId', { required: 'Please select a circuit' })}
              >
                <option value="">
                  {!selectedDistrictId ? 'Select district first' : circuitsLoading ? 'Loading circuits...' : 'Select circuit'}
                </option>
                {circuits.map((circuit) => <option key={circuit.id} value={circuit.id}>{circuit.name}</option>)}
              </select>
              <SelectError message={errors.circuitId?.message} />
            </label>
            <label className="block text-[14px] font-medium text-slate-700">School Type<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('schoolType', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">Principal Name<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('principalName', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">Principal Email<Input type="email" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('principalEmail', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">School Email<Input type="email" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('schoolEmail', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">Phone Number<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('phoneNumber', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Physical Address<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('physicalAddress', { required: true })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">Password<Input type="password" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('password', { required: true, minLength: 8 })} /></label>
            <label className="block text-[14px] font-medium text-slate-700">Confirm Password<Input type="password" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('confirmPassword', { required: true, minLength: 8 })} /></label>
            <div className="sm:col-span-2">
              <PasswordRequirementsPanel />
            </div>
            <div className="sm:col-span-2 space-y-3 pt-1">
              {serverError ? <ErrorState message={serverError} /> : null}
              <Button disabled={isSubmitting} type="submit" className="h-11 w-full rounded-xl px-5 text-sm shadow-lg shadow-primary-600/20">{isSubmitting ? 'Submitting request...' : 'Register School'}</Button>
              <p className="text-center text-sm text-slate-600">Already registered? <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/school/login">Sign in</Link></p>
            </div>
          </form>
        </div>
      </div>
    </AuthShell>
  );
};

export const RegisterCompanyPage = () => {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<CompanyRegisterPayload>({
    defaultValues: {
      companyName: '',
      registrationNumber: '',
      industry: '',
      officialEmail: '',
      mobileNumber: '',
      contactPersonName: '',
      address: '',
      website: '',
      description: '',
      popiaConsentAccepted: false,
      consentVersion: POPIA_CONSENT_VERSION,
      password: '',
    },
  });

  return (
    <AuthShell role="COMPANY" mode="register">
      <RegisterPortalSelector role="COMPANY" />
      <div className="mt-5 flex justify-center">
        <div className="w-full max-w-[700px]">
        <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm text-amber-900">
          New company accounts are created with <span className="font-semibold">pending</span> access. Admin approval is required before bursary posting and talent search are unlocked.
        </div>
        <form className="mt-6 grid gap-3.5 sm:grid-cols-2" onSubmit={handleSubmit(async (data) => {
          try {
            setServerError(null);
            const response = await authService.registerCompany({ ...data, consentVersion: data.consentVersion || POPIA_CONSENT_VERSION });
            if (response.verificationRequired) {
              navigate(`/auth/verify-otp/notice?phone=${encodeURIComponent(data.mobileNumber)}&role=COMPANY`, {
                replace: true,
                state: { message: response.message },
              });
              return;
            }
            navigate('/company/login', {
              replace: true,
              state: { registrationMessage: response.message },
            });
          } catch (error) {
            setServerError(error instanceof Error ? error.message : 'Unable to create company account.');
          }
        })}>
          <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Company name<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('companyName', { required: true })} /></label>
          <label className="block text-[14px] font-medium text-slate-700">Registration number<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('registrationNumber', { required: true })} /></label>
          <label className="block text-[14px] font-medium text-slate-700">Industry<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('industry')} /></label>
          <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Official email<Input type="email" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('officialEmail', { required: true })} /></label>
          <label className="block text-[14px] font-medium text-slate-700">Mobile number<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('mobileNumber', { required: true })} /></label>
          <label className="block text-[14px] font-medium text-slate-700">Contact person<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('contactPersonName', { required: true })} /></label>
          <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Address<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('address')} /></label>
          <label className="block text-[14px] font-medium text-slate-700">Website<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('website')} /></label>
          <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Password<Input type="password" className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('password', { required: true, minLength: 8 })} /><PasswordRequirementsPanel /></label>
          <label className="block text-[14px] font-medium text-slate-700 sm:col-span-2">Description<Input className="mt-1.5 h-11 rounded-xl border-slate-200 bg-slate-50 px-3.5 text-sm" {...register('description')} /></label>
          <PopiaConsentCheckbox
            version={POPIA_CONSENT_VERSION}
            label={
              <>
                I agree to the{' '}
                <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/privacy-policy">Privacy Policy</Link>{' '}
                and{' '}
                <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/terms-and-conditions">Terms &amp; Conditions</Link>.
              </>
            }
            inputProps={{ ...register('popiaConsentAccepted', { required: true }) }}
          />
          <div className="sm:col-span-2 space-y-3 pt-1">
            {serverError ? <ErrorState message={serverError} /> : null}
            <Button disabled={isSubmitting} type="submit" className="h-11 w-full rounded-xl px-5 text-sm shadow-lg shadow-primary-600/20">{isSubmitting ? 'Creating account...' : 'Create Company Account'}</Button>
            <p className="text-center text-sm text-slate-600">Already have a company account? <Link className="font-semibold text-primary-600 hover:text-primary-500" to="/company/login">Sign in</Link></p>
          </div>
        </form>
        </div>
      </div>
    </AuthShell>
  );
};

export const VerifyEmailNoticePage = () => {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const phoneNumber = searchParams.get('phone') ?? '';
  const role = (searchParams.get('role') === 'COMPANY' ? 'COMPANY' : 'STUDENT') as AuthRole;
  const message = (location.state as { message?: string } | undefined)?.message;

  return (
    <AuthShell role={role} mode="login">
      <AuthHeader role={role} mode="login" />
      <VerificationNoticeCard phoneNumber={phoneNumber} role={role} message={message} />
    </AuthShell>
  );
};

export const VerifyEmailPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const role = (searchParams.get('role') === 'COMPANY' ? 'COMPANY' : 'STUDENT') as AuthRole;
  const initialPhone = searchParams.get('phone') ?? '';
  const [phoneNumber, setPhoneNumber] = useState(initialPhone);
  const [code, setCode] = useState('');
  const [status, setStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);

  useEffect(() => {
    setPhoneNumber(initialPhone);
  }, [initialPhone]);

  useEffect(() => {
    if (status !== 'success') return;
    const timeoutId = window.setTimeout(() => {
      navigate(buildAuthPath(role, 'login'), { replace: true });
    }, 1800);
    return () => window.clearTimeout(timeoutId);
  }, [navigate, role, status]);

  const handleVerify = async () => {
    const safePhone = phoneNumber.trim();
    const safeCode = code.trim();
    setMessage('');
    setErrorMessage(null);
    if (!safePhone || !safeCode) {
      setStatus('error');
      setErrorMessage('Enter both phone number and OTP code.');
      return;
    }

    setStatus('submitting');
    try {
      const response = await authService.verifyOtp({ phoneNumber: safePhone, code: safeCode });
      setStatus('success');
      setMessage(response.message || 'Your account has been verified.');
    } catch (error) {
      setStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'Unable to verify OTP.');
    }
  };

  const handleResend = async () => {
    const safePhone = phoneNumber.trim();
    setMessage('');
    setErrorMessage(null);
    if (!safePhone) {
      setStatus('error');
      setErrorMessage('Enter the phone number to resend OTP.');
      return;
    }

    setIsResending(true);
    try {
      const response = await authService.resendVerificationOtp({ phoneNumber: safePhone });
      setStatus('success');
      setMessage(response.message || 'Verification OTP sent.');
    } catch (error) {
      setStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'Unable to resend verification OTP right now.');
    } finally {
      setIsResending(false);
    }
  };

  return (
    <AuthShell role={role} mode="login">
      <AuthHeader role={role} mode="login" />
      <div className="mt-8 space-y-5">
        {status === 'submitting' ? <LoadingState message="Verifying OTP" detail="This can take a few seconds." /> : null}
        {status === 'success' ? (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm text-emerald-800">
            <p className="font-semibold text-emerald-900">Phone verified</p>
            <p className="mt-2">{message}</p>
            <p className="mt-2 text-xs text-emerald-700">Redirecting to sign in...</p>
          </div>
        ) : null}
        {status === 'error' && errorMessage ? <ErrorState message={errorMessage} /> : null}

        {status !== 'submitting' ? (
          <div className="rounded-2xl border border-slate-200 bg-white px-5 py-5">
            <p className="text-sm text-slate-600">Enter the OTP sent to your phone number.</p>
            <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_220px]">
              <Input
                type="tel"
                value={phoneNumber}
                onChange={(event) => setPhoneNumber(event.target.value)}
                placeholder="+26770000000"
                className="rounded-2xl border-slate-200 bg-slate-50 px-4 py-3"
              />
              <Input
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="OTP code"
                className="rounded-2xl border-slate-200 bg-slate-50 px-4 py-3"
              />
            </div>
            <div className="mt-4 flex flex-col gap-3 sm:flex-row">
              <Button type="button" disabled={!phoneNumber.trim() || !code.trim()} onClick={handleVerify} className="rounded-2xl px-6 py-3 text-sm">
                Verify OTP
              </Button>
              <Button type="button" disabled={isResending} onClick={handleResend} className="rounded-2xl px-6 py-3 text-sm">
                {isResending ? 'Resending...' : 'Resend OTP'}
              </Button>
            </div>
            <div className="mt-4 flex flex-col gap-2 text-sm sm:flex-row sm:items-center sm:justify-between">
              <Link to={buildAuthPath(role, 'login')} className="font-semibold text-primary-600 hover:text-primary-500">Go to sign in</Link>
              <Link to={getForgotPasswordPath(role)} className="font-semibold text-primary-600 hover:text-primary-500">Forgot password?</Link>
            </div>
          </div>
        ) : null}
      </div>
    </AuthShell>
  );
};

export const ForgotPasswordPage = () => {
  const navigate = useNavigate();
  const { register, handleSubmit } = useForm<{ accountIdentifier: string }>();
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [requestedPhoneNumber, setRequestedPhoneNumber] = useState('');
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);
  const looksLikePhoneNumber = (value: string) => /^[+\d()\s-]{8,}$/.test(value.trim());

  return (
    <AuthShell role={role} mode={resolveModeFromPath(location.pathname)}>
      <AuthHeader role={role} mode="login" />
      <form className="mt-8 space-y-4" onSubmit={handleSubmit(async ({ accountIdentifier }) => {
        const safeIdentifier = accountIdentifier.trim();
        setMessage('');
        setErrorMessage('');
        if (!safeIdentifier) {
          setErrorMessage('Enter your email, username, or registered phone number.');
          return;
        }

        try {
          const response = await authService.requestForgotPasswordOtp({
            accountIdentifier: safeIdentifier,
            phoneNumber: looksLikePhoneNumber(safeIdentifier) ? safeIdentifier : undefined,
          });
          const responseMessage = response.message || 'If the account exists, a reset OTP has been sent.';
          const dispatchFailed = responseMessage.toLowerCase().includes('could not send');
          if (dispatchFailed) {
            setErrorMessage(responseMessage);
            return;
          }
          setRequestedPhoneNumber(looksLikePhoneNumber(safeIdentifier) ? safeIdentifier : '');
          setMessage(responseMessage);
        } catch (error) {
          setErrorMessage(error instanceof Error ? error.message : 'Unable to send reset OTP right now.');
        }
      })}>
        <label className="text-sm font-medium text-slate-700">
          Email, username, or phone number
          <Input
            className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5"
            placeholder="Enter your email, username, or +26770000000"
            {...register('accountIdentifier', { required: true })}
          />
        </label>
        <Button type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">Send reset OTP</Button>
        {errorMessage ? <ErrorState message={errorMessage} /> : null}
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
        <p className="text-xs text-slate-500">
          If the OTP is sent successfully, use your registered phone number on the next step to complete the reset.
        </p>
        {requestedPhoneNumber ? (
          <Button
            type="button"
            onClick={() => navigate(`${getResetPasswordPath(role)}?phone=${encodeURIComponent(requestedPhoneNumber)}`, { replace: true })}
            className="w-full rounded-2xl px-6 py-3.5 text-sm"
          >
            I received OTP, continue
          </Button>
        ) : message ? (
          <Button
            type="button"
            onClick={() => navigate(getResetPasswordPath(role), { replace: true })}
            className="w-full rounded-2xl px-6 py-3.5 text-sm"
          >
            I received OTP, continue
          </Button>
        ) : null}
      </form>
    </AuthShell>
  );
};

export const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const role = useMemo(() => resolveRoleFromPath(location.pathname), [location.pathname]);
  const phoneFromQuery = searchParams.get('phone') ?? '';
  const { register, handleSubmit } = useForm<{ phoneNumber: string; code: string; newPassword: string; confirmPassword: string }>({
    defaultValues: {
      phoneNumber: phoneFromQuery,
      code: '',
      newPassword: '',
      confirmPassword: '',
    },
  });
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  return (
    <AuthShell role={role} mode="login">
      <AuthHeader role={role} mode="login" />
      <form className="mt-8 space-y-4" onSubmit={handleSubmit(async ({ phoneNumber, code, newPassword, confirmPassword }) => {
        setMessage('');
        setErrorMessage('');
        const safePhoneNumber = phoneNumber.trim();
        const safeCode = code.trim();

        if (!safePhoneNumber) {
          setErrorMessage('Phone number is required.');
          return;
        }
        if (!safeCode) {
          setErrorMessage('OTP code is required.');
          return;
        }
        if (newPassword !== confirmPassword) {
          setErrorMessage('Passwords do not match.');
          return;
        }

        try {
          const response = await authService.resetPasswordWithOtp({ phoneNumber: safePhoneNumber, code: safeCode, newPassword });
          setMessage(response.message || 'Password reset complete. Redirecting to sign in...');
          setTimeout(() => navigate(getResetPasswordLoginPath(role), { replace: true }), 1500);
        } catch (error) {
          setErrorMessage(error instanceof Error ? error.message : 'Unable to reset password.');
        }
      })}>
        <label className="text-sm font-medium text-slate-700">
          Phone number
          <Input type="tel" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('phoneNumber', { required: true })} />
        </label>
        <label className="text-sm font-medium text-slate-700">
          OTP code
          <Input className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('code', { required: true })} />
        </label>
        <label className="text-sm font-medium text-slate-700">New password<Input type="password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('newPassword')} /></label>
        <label className="text-sm font-medium text-slate-700">Confirm password<Input type="password" className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5" {...register('confirmPassword')} /></label>
        <Button type="submit" className="w-full rounded-2xl px-6 py-3.5 text-sm shadow-lg shadow-primary-600/20">Reset password</Button>
        {errorMessage ? <ErrorState message={errorMessage} /> : null}
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </form>
    </AuthShell>
  );
};





























