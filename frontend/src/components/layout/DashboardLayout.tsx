import {
  Bell,
  BookOpen,
  Bot,
  Brain,
  BriefcaseBusiness,
  Building2,
  CreditCard,
  FileText,
  GraduationCap,
  Home,
  KeyRound,
  LogOut,
  Map,
  Menu,
  MessageSquare,
  Search,
  Settings,
  Sparkles,
  Trophy,
  UserCircle2,
  X,
  type LucideIcon,
} from 'lucide-react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useMemo, useRef, useState } from 'react';
import { resolvePrimaryRole } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { notificationService } from '@/services/notificationService';
import { schoolService } from '@/services/schoolService';
import type { Role } from '@/types';
import { DashboardLogo } from '@/components/common/DashboardLogo';

type NavItem = { to: string; label: string; icon?: LucideIcon };

const navByRole: Record<Role, NavItem[]> = {
  STUDENT: [
    { to: '/student/dashboard', label: 'Dashboard', icon: Home },
    { to: '/student/profile', label: 'My Profile', icon: UserCircle2 },
    { to: '/student/psychometric', label: 'Psychometric Test', icon: Brain },
    { to: '/student/recommendations/careers', label: 'AI Guidance', icon: Sparkles },
    { to: '/student/ai-tutor', label: 'AI Tutor', icon: Bot },
    { to: '/student/cv-builder', label: 'CV Builder', icon: FileText },
    { to: '/student/rewards', label: 'Points & Rewards', icon: Trophy },
    { to: '/student/career-roadmaps', label: 'Career Roadmaps', icon: Map },
    { to: '/student/saved', label: 'Opportunities', icon: BriefcaseBusiness },
    { to: '/student/applications', label: 'Bursary Finder', icon: GraduationCap },
    { to: '/student/scholarships', label: 'Scholarship Assistant', icon: FileText },
    { to: '/student/universities', label: 'Universities', icon: Building2 },
    { to: '/student/university-applications', label: 'University Applications', icon: GraduationCap },
    { to: '/student/subscription', label: 'Subscription', icon: CreditCard },
    { to: '/student/notifications', label: 'Notifications', icon: Bell },
    { to: '/student/settings', label: 'Settings', icon: Settings },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  COMPANY: [
    { to: '/company/dashboard', label: 'Dashboard' }, { to: '/company/profile', label: 'Company Profile' }, { to: '/company/bursaries/new', label: 'Post Bursary' }, { to: '/company/bursaries', label: 'Manage Bursaries' }, { to: '/company/applicants', label: 'Applications' }, { to: '/company/verification-docs', label: 'Documents' }, { to: '/company/notifications', label: 'Notifications' }, { to: '/company/settings', label: 'Settings' }, { to: '/account/change-password', label: 'Change Password' },
  ],
  ADMIN: [
    { to: '/admin/dashboard', label: 'Dashboard' }, { to: '/admin/users', label: 'User Management' }, { to: '/admin/district-management', label: 'District Management' }, { to: '/admin/pending-approvals', label: 'Company Approvals' }, { to: '/admin/bursaries', label: 'Bursary Management' }, { to: '/admin/notifications', label: 'Notifications' }, { to: '/admin/analytics', label: 'Analytics' }, { to: '/admin/schools', label: 'School Portal' }, { to: '/admin/settings', label: 'System Settings' }, { to: '/account/change-password', label: 'Change Password' },
  ],
  DISTRICT_ADMIN: [
    { to: '/district/dashboard', label: 'District Dashboard', icon: Home },
    { to: '/district/schools', label: 'Schools', icon: Building2 },
    { to: '/district/analytics', label: 'Analytics', icon: BookOpen },
    { to: '/district/ai-insights', label: 'AI Insights', icon: Sparkles },
    { to: '/district/interventions', label: 'Interventions', icon: Bell },
    { to: '/district/reports', label: 'Reports', icon: FileText },
    { to: '/district/settings', label: 'Settings', icon: Settings },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  DISTRICT_DIRECTOR: [
    { to: '/district/dashboard', label: 'District Dashboard', icon: Home },
    { to: '/district/schools', label: 'Schools', icon: Building2 },
    { to: '/district/analytics', label: 'Analytics', icon: BookOpen },
    { to: '/district/reports', label: 'Reports', icon: FileText },
    { to: '/district/curriculum/atp-repository', label: 'Curriculum', icon: BookOpen },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  CIRCUIT_MANAGER: [
    { to: '/district/circuit/dashboard', label: 'Dashboard', icon: Home },
    { to: '/district/circuit/schools', label: 'My Schools', icon: Building2 },
    { to: '/district/circuit/curriculum', label: 'Curriculum Monitoring', icon: BookOpen },
    { to: '/district/circuit/visits', label: 'School Visits', icon: Bell },
    { to: '/district/circuit/support-requests', label: 'Support Requests', icon: FileText },
    { to: '/district/circuit/interventions', label: 'Interventions', icon: Bell },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  SUBJECT_ADVISOR: [
    { to: '/district/advisor/dashboard', label: 'Dashboard', icon: Home },
    { to: '/district/advisor/teachers', label: 'Teachers', icon: UserCircle2 },
    { to: '/district/advisor/atp-monitoring', label: 'ATP Monitoring', icon: BookOpen },
    { to: '/district/advisor/assessments', label: 'Assessments', icon: FileText },
    { to: '/district/curriculum/atp-repository', label: 'Curriculum', icon: BookOpen },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  SCHOOL_ADMIN: [
    { to: '/school/dashboard', label: 'Dashboard', icon: Home },
    { to: '/school/learners', label: 'Learners', icon: UserCircle2 },
    { to: '/school/curriculum', label: 'Curriculum', icon: BookOpen },
    { to: '/school/academic-insights', label: 'Academic Insights', icon: BookOpen },
    { to: '/school/career-readiness', label: 'Career Readiness', icon: Sparkles },
    { to: '/school/courses', label: 'Courses', icon: GraduationCap },
    { to: '/school/bursaries', label: 'Bursaries', icon: BriefcaseBusiness },
    { to: '/school/interventions', label: 'Interventions', icon: Bell },
    { to: '/school/reports', label: 'Reports', icon: FileText },
    { to: '/school/settings', label: 'Settings', icon: Settings },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  TEACHER: [
    { to: '/teacher/dashboard', label: 'Dashboard', icon: Home },
    { to: '/teacher/learners', label: 'Learners', icon: UserCircle2 },
    { to: '/teacher/curriculum', label: 'Curriculum', icon: BookOpen },
    { to: '/teacher/academic-insights', label: 'Academic Insights', icon: BookOpen },
    { to: '/teacher/career-readiness', label: 'Career Readiness', icon: Sparkles },
    { to: '/teacher/courses', label: 'Courses', icon: GraduationCap },
    { to: '/teacher/bursaries', label: 'Bursaries', icon: BriefcaseBusiness },
    { to: '/teacher/interventions', label: 'Interventions', icon: Bell },
    { to: '/teacher/reports', label: 'Reports', icon: FileText },
    { to: '/teacher/settings', label: 'Settings', icon: Settings },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
  SCHOOL_STUDENT: [
    { to: '/school-student/dashboard', label: 'Learner Dashboard', icon: Home },
    { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
  ],
};

const pendingCompanyNav: NavItem[] = [
  { to: '/company/pending-approval', label: 'Review Status' },
  { to: '/company/profile', label: 'Company Profile' },
  { to: '/company/verification-docs', label: 'Documents' },
  { to: '/company/settings', label: 'Settings' },
  { to: '/account/change-password', label: 'Change Password' },
];

const getInitials = (name?: string, email?: string) => {
  const parts = (name?.trim() || email?.split('@')[0] || 'Student')
    .split(/\s+/)
    .filter(Boolean);
  return parts.slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'ST';
};

const studentTopNav: NavItem[] = [
  { to: '/student/dashboard', label: 'Dashboard', icon: Home },
  { to: '/student/rewards', label: 'Points & Rewards', icon: Trophy },
  { to: '/student/career-roadmaps', label: 'Career Roadmaps', icon: Map },
  { to: '/student/saved', label: 'Opportunities', icon: BriefcaseBusiness },
  { to: '/student/applications', label: 'Bursary Finder', icon: GraduationCap },
  { to: '/student/scholarships', label: 'Scholarship Assistant', icon: FileText },
  { to: '/student/universities', label: 'Universities', icon: Building2 },
  { to: '/student/university-applications', label: 'University Applications', icon: GraduationCap },
];

const studentPersonalNav: NavItem[] = [
  { to: '/student/profile', label: 'My Profile', icon: UserCircle2 },
  { to: '/student/psychometric', label: 'Psychometric Test', icon: Brain },
  { to: '/student/recommendations/careers', label: 'AI Guidance', icon: Sparkles },
  { to: '/student/ai-tutor', label: 'AI Tutor', icon: Bot },
  { to: '/student/learning-centre', label: 'Learning Centre', icon: BookOpen },
  { to: '/student/cv-builder', label: 'CV Builder', icon: FileText },
];

const studentAccountNav: NavItem[] = [
  { to: '/student/subscription', label: 'Subscription', icon: CreditCard },
  { to: '/student/notifications', label: 'Notifications', icon: Bell },
  { to: '/student/settings', label: 'Settings', icon: Settings },
  { to: '/account/change-password', label: 'Change Password', icon: KeyRound },
];

export const DashboardLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showSearchResults, setShowSearchResults] = useState(false);
  const [searchMessage, setSearchMessage] = useState('');
  const searchBoxRef = useRef<HTMLDivElement | null>(null);
  if (!user) return null;

  const primaryRole = resolvePrimaryRole(user);
  if (!primaryRole) return null;
  const effectiveRole: Role = primaryRole === 'STUDENT' && location.pathname.startsWith('/school-student/')
    ? 'SCHOOL_STUDENT'
    : primaryRole;
  const navItems = effectiveRole === 'COMPANY' && user.approvalStatus !== 'APPROVED'
    ? pendingCompanyNav
    : navByRole[effectiveRole];
  const isStudent = effectiveRole === 'STUDENT';
  const isSchoolStudent = effectiveRole === 'SCHOOL_STUDENT';
  const displayName = user.fullName || user.companyName || user.email;
  const subscriptionLabel = user.planType ? `${user.planType.toLowerCase()} plan` : '';
  const premiumLabel = user.planType?.toUpperCase() === 'PREMIUM' ? 'Premium' : 'Basic';
  const notificationsPath = effectiveRole === 'STUDENT'
    ? '/student/notifications'
    : effectiveRole === 'COMPANY'
      ? '/company/notifications'
      : effectiveRole === 'DISTRICT_ADMIN' || effectiveRole === 'DISTRICT_DIRECTOR'
        ? '/district/dashboard'
        : effectiveRole === 'CIRCUIT_MANAGER'
          ? '/district/circuit/dashboard'
          : effectiveRole === 'SUBJECT_ADVISOR'
            ? '/district/advisor/dashboard'
      : effectiveRole === 'SCHOOL_ADMIN'
        ? '/school/dashboard'
        : effectiveRole === 'TEACHER'
          ? '/teacher/dashboard'
          : effectiveRole === 'SCHOOL_STUDENT'
            ? '/school-student/dashboard'
            : '/admin/dashboard';
  const messagesPath = effectiveRole === 'STUDENT'
    ? '/student/ai-tutor'
    : notificationsPath;
  const unreadNotifications = useAppQuery({
    queryKey: ['notes-unread'],
    queryFn: () => notificationService.unreadCount(),
    enabled: ['STUDENT', 'SCHOOL_ADMIN', 'TEACHER', 'SCHOOL_STUDENT', 'COMPANY', 'ADMIN', 'DISTRICT_ADMIN', 'DISTRICT_DIRECTOR', 'CIRCUIT_MANAGER', 'SUBJECT_ADVISOR'].includes(effectiveRole),
    refetchInterval: 30000,
  });
  const mySchoolStatus = useAppQuery({
    queryKey: ['student', 'my-school', 'status', 'layout'],
    queryFn: schoolService.getMySchoolStatus,
    enabled: isStudent,
    staleTime: 60_000,
  });
  const unreadCount = unreadNotifications.data?.unreadCount ?? 0;
  const mySchoolData = mySchoolStatus.data;
  const schoolLinked = mySchoolData?.status === 'APPROVED';
  const showMySchoolDot = isStudent && !schoolLinked;
  const mySchoolTooltip = schoolLinked ? 'Open your school connection' : 'Link your school account to access school resources';
  const homePath = effectiveRole === 'STUDENT'
    ? '/student/dashboard'
    : effectiveRole === 'COMPANY'
      ? '/company/dashboard'
      : effectiveRole === 'DISTRICT_ADMIN' || effectiveRole === 'DISTRICT_DIRECTOR'
        ? '/district/dashboard'
        : effectiveRole === 'CIRCUIT_MANAGER'
          ? '/district/circuit/dashboard'
          : effectiveRole === 'SUBJECT_ADVISOR'
            ? '/district/advisor/dashboard'
      : effectiveRole === 'SCHOOL_ADMIN'
        ? '/school/dashboard'
        : effectiveRole === 'TEACHER'
          ? '/teacher/dashboard'
          : effectiveRole === 'SCHOOL_STUDENT'
            ? '/school-student/dashboard'
            : '/admin/dashboard';

  const searchableNavItems = useMemo(() => {
    const baseItems = effectiveRole === 'STUDENT'
      ? [...studentTopNav, ...studentPersonalNav, ...studentAccountNav]
      : navItems;

    const deduped = baseItems.reduce<NavItem[]>((acc, item) => {
      if (!acc.some((entry) => entry.to === item.to)) {
        acc.push(item);
      }
      return acc;
    }, []);

    const aliases: Record<string, string[]> = {
      'AI Guidance': ['ai', 'guidance', 'recommendations'],
      'AI Tutor': ['ai', 'tutor'],
      'My Profile': ['profile'],
      'Psychometric Test': ['psychometric', 'test'],
      'CV Builder': ['cv', 'resume'],
      'Points & Rewards': ['points', 'rewards'],
      'Career Roadmaps': ['career', 'roadmap', 'roadmaps'],
      Opportunities: ['opportunities', 'jobs'],
      'Bursary Finder': ['bursary', 'bursaries', 'finder'],
      'Scholarship Assistant': ['scholarship', 'assistant'],
      Universities: ['university', 'universities'],
      'Learning Centre': ['learning', 'centre', 'center', 'courses', 'study materials', 'past papers', 'revision notes', 'video lessons', 'ai study assistant', 'learning resources', 'exam preparation', 'skills development', 'coding tutorials'],
      'University Applications': ['university', 'applications', 'admissions'],
      Subscription: ['subscription', 'plan', 'premium'],
      Notifications: ['notifications', 'alerts'],
      Settings: ['settings'],
      'Change Password': ['password', 'change password'],
      Dashboard: ['home', 'dashboard'],
    };

    return deduped.map((item) => ({
      ...item,
      searchText: `${item.label} ${(aliases[item.label] ?? []).join(' ')}`.toLowerCase(),
    }));
  }, [effectiveRole, navItems]);

  const filteredSearchResults = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) return searchableNavItems.slice(0, 8);

    const startsWith = searchableNavItems.filter((item) => item.label.toLowerCase().startsWith(query));
    const includes = searchableNavItems.filter((item) => !item.label.toLowerCase().startsWith(query) && item.searchText.includes(query));
    return [...startsWith, ...includes].slice(0, 8);
  }, [searchTerm, searchableNavItems]);

  const runSearch = () => {
    if (!searchTerm.trim()) {
      setShowSearchResults(true);
      setSearchMessage('');
      return;
    }
    const best = filteredSearchResults[0];
    if (best) {
      setShowSearchResults(false);
      setSearchMessage('');
      setSearchTerm('');
      navigate(best.to);
      setOpen(false);
      return;
    }
    setShowSearchResults(true);
    setSearchMessage('No matching page found.');
  };

  useEffect(() => {
    const onPointerDown = (event: MouseEvent) => {
      if (!searchBoxRef.current?.contains(event.target as Node)) {
        setShowSearchResults(false);
      }
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, []);

  const mySchoolButton = isStudent ? (
    <Link
      to="/student/my-school"
      title={mySchoolTooltip}
      className="relative inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:brightness-110"
    >
      <GraduationCap size={16} className="shrink-0" />
      <span className="hidden sm:inline">My School</span>
      {showMySchoolDot ? <span className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full bg-orange-400 ring-2 ring-white/40" /> : null}
    </Link>
  ) : null;

  const renderDashboardSearch = (darkTheme: boolean) => (
    <div ref={searchBoxRef} className="relative hidden w-full max-w-[min(52vw,42rem)] sm:block">
      <Search size={18} className={`pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 ${darkTheme ? 'text-slate-300' : 'text-slate-400'}`} />
      <input
        type="search"
        aria-label="Search dashboard"
        placeholder="Search dashboard"
        value={searchTerm}
        onChange={(event) => {
          setSearchTerm(event.target.value);
          setShowSearchResults(true);
          setSearchMessage('');
        }}
        onFocus={() => setShowSearchResults(true)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault();
            runSearch();
          }
        }}
        className={darkTheme
          ? 'w-full rounded-2xl border border-white/20 bg-white/10 py-3 pl-11 pr-12 text-sm text-white outline-none transition placeholder:text-slate-300 focus:border-[#1E8BFF] focus:bg-white/15 focus:ring-4 focus:ring-blue-500/20'
          : 'w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-12 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-primary-300 focus:bg-white focus:ring-4 focus:ring-primary-100'
        }
      />
      <button
        type="button"
        aria-label="Submit search"
        onClick={runSearch}
        className={`absolute right-2 top-1/2 -translate-y-1/2 rounded-xl p-2 transition ${darkTheme ? 'text-slate-200 hover:bg-white/10' : 'text-slate-500 hover:bg-slate-100'}`}
      >
        <Search size={16} />
      </button>
      {showSearchResults ? (
        <div className={`absolute left-0 right-0 top-[calc(100%+8px)] z-50 rounded-2xl border p-2 shadow-xl ${darkTheme ? 'border-white/15 bg-[#0d1f49]' : 'border-slate-200 bg-white'}`}>
          {filteredSearchResults.length ? (
            filteredSearchResults.map((item) => (
              <button
                key={item.to}
                type="button"
                onClick={() => {
                  setShowSearchResults(false);
                  setSearchMessage('');
                  setSearchTerm('');
                  navigate(item.to);
                  setOpen(false);
                }}
                className={`flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm transition ${darkTheme ? 'text-white hover:bg-white/10' : 'text-slate-700 hover:bg-slate-100'}`}
              >
                {item.icon ? <item.icon size={16} className="shrink-0" /> : null}
                <span>{item.label}</span>
              </button>
            ))
          ) : (
            <p className={`px-3 py-2 text-sm ${darkTheme ? 'text-slate-200' : 'text-slate-600'}`}>
              {searchMessage || 'No matching page found.'}
            </p>
          )}
        </div>
      ) : null}
    </div>
  );

  if (isSchoolStudent) {
    return (
      <div className="min-h-screen bg-slate-100">
        <main className="min-w-0 overflow-x-hidden p-3 md:p-6">
          <div className="w-full">
            <Outlet />
          </div>
        </main>
      </div>
    );
  }

  if (primaryRole === 'SCHOOL_ADMIN') {
    return (
      <div className="min-h-screen bg-slate-100">
        <main className="min-w-0 overflow-x-hidden">
          <Outlet />
        </main>
      </div>
    );
  }

  if (isStudent) {
    return (
      <div className="relative flex h-[100dvh] min-h-screen flex-col overflow-hidden bg-slate-100">
        {open ? (
          <button
            type="button"
            className="fixed inset-0 z-30 bg-slate-950/45 md:hidden"
            aria-label="Close menu"
            onClick={() => setOpen(false)}
          />
        ) : null}
        <header className="shrink-0 bg-[#081739] px-4 py-3 text-white md:px-6 lg:px-8">
          <div className="flex min-w-0 items-center justify-between gap-3">
            <div className="flex min-w-0 flex-1 items-center gap-3">
              <button type="button" className="rounded-xl border border-white/25 p-2 text-white md:hidden" onClick={() => setOpen((v) => !v)} aria-label="Toggle menu"><Menu size={20} /></button>
              <Link to={homePath} className="block rounded-xl pr-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 md:pr-4 lg:pr-6" aria-label="EduRite home">
                <DashboardLogo className="block h-10 w-auto object-contain md:h-11 lg:h-12" />
              </Link>
              {renderDashboardSearch(true)}
            </div>
            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              <div className="hidden md:block">{mySchoolButton}</div>
              <Link to={notificationsPath} className="relative rounded-2xl border border-white/20 bg-white/10 p-2.5 text-white transition hover:bg-white/15" aria-label="Notifications">
                <Bell size={18} />
                {unreadCount > 0 ? <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
              </Link>
              <Link to={messagesPath} className="rounded-2xl border border-white/20 bg-white/10 p-2.5 text-white transition hover:bg-white/15" aria-label="Messages">
                <MessageSquare size={18} />
              </Link>
              <div className="md:hidden">{mySchoolButton}</div>
              <div className="flex min-w-0 items-center gap-2 rounded-2xl border border-white/20 bg-white/10 px-2 py-1.5 sm:px-3">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#1E8BFF] text-sm font-semibold text-white">
                  {getInitials(displayName, user.email)}
                </div>
                <div className="hidden min-w-0 sm:block">
                  <p className="truncate text-sm font-semibold text-white">{displayName}</p>
                  <div className="flex items-center gap-2">
                    <p className="text-xs text-slate-200">{primaryRole.toLowerCase()}</p>
                    <span className="rounded-full bg-gradient-to-r from-[#FF8A00] to-[#FF6B00] px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-white">{premiumLabel}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </header>
        <div className="top-module-nav shrink-0 overflow-x-auto border-t border-white/10 bg-[#081739] px-4 pb-3 md:px-6 lg:px-8">
          <nav className="student-top-tabs-row flex w-max min-w-max items-center gap-2">
            {studentTopNav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to.endsWith('/dashboard')}
                className={({ isActive }) => [
                  'inline-flex items-center gap-2 whitespace-nowrap rounded-2xl px-3 py-2 text-sm font-medium transition',
                  isActive ? 'bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] text-white shadow-lg shadow-blue-950/30' : 'border border-white/15 bg-white/5 text-white/90 hover:bg-white/10',
                ].join(' ')}
              >
                {item.icon ? <item.icon size={16} className="shrink-0" /> : null}
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="flex min-h-0 flex-1 overflow-hidden">
          <aside className={`fixed inset-y-0 left-0 z-40 flex w-72 shrink-0 flex-col border-r border-[#E2E8F0] bg-white px-4 py-5 text-slate-700 shadow-[2px_0_10px_rgba(15,23,42,0.04)] transition-transform duration-200 md:static md:z-auto md:w-[84px] md:min-w-[84px] md:px-2 lg:w-[230px] lg:min-w-[230px] lg:px-4 md:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}>
            <div className="mb-4 flex items-center justify-between md:hidden">
              <p className="text-sm font-semibold text-slate-700">Menu</p>
              <button type="button" aria-label="Close menu" className="rounded-lg border border-slate-200 p-1.5 text-slate-600 transition hover:bg-slate-50 hover:text-slate-900" onClick={() => setOpen(false)}><X size={16} /></button>
            </div>
            <div className="sidebar-nav min-h-0 flex-1 space-y-5 overflow-y-auto pr-1">
              <div className="space-y-2">
                <p className="hidden px-2 text-xs font-semibold uppercase tracking-[0.12em] text-[#94A3B8] lg:block">Personal</p>
                {studentPersonalNav.map((item) => (
                  <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)} className={({ isActive }) => [
                    'flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-medium transition md:justify-center md:px-2 lg:justify-start lg:px-3',
                    isActive ? 'bg-[#EFF6FF] text-[#2563EB]' : 'text-[#334155] hover:bg-[#F8FAFC] hover:text-[#1E293B]',
                  ].join(' ')}>
                    {item.icon ? <item.icon size={18} className="shrink-0" /> : null}
                    <span className="truncate md:hidden lg:inline">{item.label}</span>
                  </NavLink>
                ))}
              </div>
              <div className="space-y-2">
                <p className="hidden px-2 text-xs font-semibold uppercase tracking-[0.12em] text-[#94A3B8] lg:block">Account / More</p>
                {studentAccountNav.map((item) => (
                  <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)} className={({ isActive }) => [
                    'flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-medium transition md:justify-center md:px-2 lg:justify-start lg:px-3',
                    isActive ? 'bg-[#EFF6FF] text-[#2563EB]' : 'text-[#334155] hover:bg-[#F8FAFC] hover:text-[#1E293B]',
                  ].join(' ')}>
                    {item.icon ? <item.icon size={18} className="shrink-0" /> : null}
                    <span className="truncate md:hidden lg:inline">{item.label}</span>
                  </NavLink>
                ))}
              </div>
            </div>
            <div className="mt-4 flex-shrink-0 border-t border-[#E2E8F0] pt-4">
              <button type="button" onClick={() => logout()} className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left text-sm font-semibold text-[#334155] transition hover:bg-[#FEF2F2] hover:text-[#DC2626] md:justify-center md:px-2 lg:justify-start lg:px-3">
                <LogOut size={18} />
                <span className="md:hidden lg:inline">Logout</span>
              </button>
            </div>
          </aside>
          <main className="min-h-0 min-w-0 flex-1 overflow-y-auto overflow-x-hidden p-3 md:p-6">
            <div className="student-dashboard-shell mx-auto w-full max-w-[1900px] p-3 sm:p-4 md:p-6 2xl:px-8">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
      <div className="relative flex h-screen overflow-hidden bg-slate-100">
      {open ? (
        <button
          type="button"
          className="fixed inset-0 z-10 bg-slate-950/40 md:hidden"
          aria-label="Close menu"
          onClick={() => setOpen(false)}
        />
      ) : null}
      <aside className={`fixed inset-y-0 left-0 z-20 flex h-screen w-[260px] shrink-0 flex-col bg-[#081739] px-4 py-4 text-white shadow-2xl shadow-slate-950/30 transition-transform duration-200 md:sticky md:top-0 md:w-[72px] md:px-2 lg:w-[260px] lg:px-4 md:translate-x-0 ${open ? 'visible translate-x-0' : 'invisible -translate-x-full md:visible'}`}>
        <div className="flex-shrink-0">
          <Link to={homePath} className="mb-4 block rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70" aria-label="EduRite home" onClick={() => setOpen(false)}>
            <DashboardLogo className="hidden h-12 w-auto object-contain lg:block" />
            <div className="hidden h-10 w-10 items-center justify-center overflow-hidden rounded-xl bg-white/10 px-1 md:flex lg:hidden">
              <DashboardLogo className="block h-8 w-auto object-contain" />
            </div>
          </Link>
        </div>
        <nav className="sidebar-nav min-h-0 flex-1 space-y-1 overflow-y-auto pr-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              className={({ isActive }) => [
                'flex min-w-0 items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-medium transition md:justify-center md:px-2 lg:justify-start lg:px-3',
                isActive
                  ? 'bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] text-white shadow-lg shadow-blue-900/30'
                  : 'text-white/78 hover:bg-white/10 hover:text-white',
              ].join(' ')}
              to={item.to}
              end={item.to.endsWith('/dashboard')}
              onClick={() => setOpen(false)}
            >
              {item.icon ? <item.icon size={18} className="shrink-0" /> : null}
              <span className="min-w-0 truncate md:hidden lg:block">{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="mt-4 flex-shrink-0 border-t border-[#E2E8F0] pt-4">
          <button
            type="button"
            onClick={() => logout()}
            className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left text-sm font-semibold text-white/85 transition hover:bg-white/10 hover:text-white md:justify-center md:px-2 lg:justify-start lg:px-3"
          >
            <LogOut size={18} className="shrink-0" />
            <span className="md:hidden lg:block">Logout</span>
          </button>
        </div>
      </aside>
      <div className="flex h-screen min-w-0 flex-1 flex-col">
        <header className="shrink-0 border-b border-slate-200/80 bg-white/95 px-4 py-3 backdrop-blur md:px-5">
          <div className="flex min-w-0 items-center justify-between gap-3">
            <div className="flex min-w-0 flex-1 items-center gap-3">
              <button type="button" className="rounded-xl border border-slate-200 p-2 text-slate-700 md:hidden" onClick={() => setOpen((v) => !v)} aria-label="Toggle menu"><Menu size={20} /></button>
              {renderDashboardSearch(false)}
            </div>
            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              {isStudent ? <div className="hidden md:block">{mySchoolButton}</div> : null}
              {subscriptionLabel ? (
                <span className="hidden rounded-full border border-primary-100 bg-primary-50 px-3 py-1 text-xs font-semibold capitalize text-primary-700 sm:inline-flex">
                  {subscriptionLabel}
                </span>
              ) : null}
              <Link to={notificationsPath} className="relative rounded-2xl border border-slate-200 bg-white p-2.5 text-slate-700 shadow-sm transition hover:border-primary-200 hover:text-primary-700" aria-label="Notifications">
                <Bell size={18} />
                {unreadCount > 0 ? <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1.5 text-[10px] font-bold text-white">{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
              </Link>
              <Link to={messagesPath} className="relative rounded-2xl border border-slate-200 bg-white p-2.5 text-slate-700 shadow-sm transition hover:border-primary-200 hover:text-primary-700" aria-label="Messages">
                <MessageSquare size={18} />
              </Link>
              {isStudent ? <div className="md:hidden">{mySchoolButton}</div> : null}
              <div className="flex min-w-0 items-center gap-2 rounded-2xl border border-slate-200 bg-white px-2 py-1.5 shadow-sm sm:px-3">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-600 text-sm font-semibold text-white">
                  {getInitials(displayName, user.email)}
                </div>
                <div className="hidden min-w-0 sm:block">
                  <p className="truncate text-sm font-semibold text-slate-900">{displayName}</p>
                  <div className="flex items-center gap-2">
                    <p className="text-xs text-slate-500">{primaryRole.toLowerCase()}</p>
                    <span className="rounded-full bg-gradient-to-r from-[#FF8A00] to-[#FF6B00] px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-white">{premiumLabel}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </header>
        <main className="min-h-0 min-w-0 flex-1 overflow-y-auto overflow-x-hidden p-4 scroll-smooth md:p-5">
          <div className={isStudent ? 'student-dashboard-shell mx-auto w-full max-w-none p-0' : 'mx-auto w-full max-w-none'}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

