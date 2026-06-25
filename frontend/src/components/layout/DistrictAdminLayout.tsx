import { BarChart3, BellRing, BookOpen, Bot, Building2, CalendarDays, ClipboardCheck, FileText, GraduationCap, Home, KeyRound, LifeBuoy, LogOut, Menu, Settings, ShieldCheck, Sparkles, Users2, Wrench, X } from 'lucide-react';
import { NavLink, Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { DashboardLogo } from '@/components/common/DashboardLogo';
import { resolvePrimaryRole } from '@/features/auth/roleUtils';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { districtService } from '@/services/districtService';

type NavItem = { to: string; label: string; icon: React.ComponentType<{ className?: string }>; exact?: boolean; showPendingBadge?: boolean };
type NavGroup = { title: string; items: NavItem[] };

const districtOverviewNav: NavGroup[] = [
  {
    title: 'Overview',
    items: [
      { to: '/district/dashboard', label: 'District Dashboard', icon: Home, exact: true },
      { to: '/district/analytics', label: 'Analytics', icon: BarChart3 },
      { to: '/district/ai-insights', label: 'AI Insights', icon: Bot },
    ],
  },
  {
    title: 'Schools',
    items: [
      { to: '/district/schools', label: 'Schools', icon: Building2, exact: true },
      { to: '/district/school-registration-requests', label: 'School Registration Requests', icon: Users2, showPendingBadge: true },
      { to: '/district/schools?view=performance', label: 'School Performance', icon: Sparkles },
      { to: '/district/reports?view=school-reports', label: 'School Reports', icon: FileText },
      { to: '/district/schools?view=compliance', label: 'School Compliance', icon: ShieldCheck },
    ],
  },
  {
    title: 'Academics',
    items: [
      { to: '/district/curriculum/atp-repository', label: 'Curriculum Management', icon: BookOpen },
      { to: '/district/curriculum/atp-repository', label: 'ATP Repository', icon: BookOpen },
      { to: '/district/curriculum/syllabus-repository', label: 'Syllabus Repository', icon: FileText },
      { to: '/district/curriculum/lesson-plan-repository', label: 'Lesson Plan Repository', icon: ClipboardCheck },
      { to: '/district/curriculum/curriculum-calendar', label: 'Curriculum Calendar', icon: CalendarDays },
      { to: '/district/curriculum/weekly-coverage-tracker', label: 'Weekly Coverage Tracker', icon: BarChart3 },
      { to: '/district/curriculum/teacher-reminders', label: 'Teacher Reminders', icon: BellRing },
      { to: '/district/curriculum/curriculum-compliance', label: 'Curriculum Compliance', icon: ShieldCheck },
      { to: '/district/analytics?view=learner-readiness', label: 'Learner Readiness', icon: Users2 },
      { to: '/district/analytics?view=aps-readiness', label: 'APS Readiness', icon: GraduationCap },
      { to: '/district/analytics?view=subject-gaps', label: 'Subject Gaps', icon: Sparkles },
      { to: '/district/analytics?view=career-pathways', label: 'Career Pathways', icon: Bot },
    ],
  },
  {
    title: 'Administration',
    items: [
      { to: '/district/interventions', label: 'Interventions', icon: Wrench },
      { to: '/district/reports?view=announcements', label: 'Announcements', icon: BellRing },
      { to: '/district/settings?view=support', label: 'Support Requests', icon: LifeBuoy },
      { to: '/district/reports', label: 'District Reports', icon: FileText, exact: true },
    ],
  },
  {
    title: 'Settings',
    items: [
      { to: '/district/settings', label: 'District Settings', icon: Settings, exact: true },
      { to: '/account/change-password', label: 'Security / Change Password', icon: KeyRound },
    ],
  },
];

const circuitManagerNav: NavGroup[] = [
  {
    title: 'Circuit',
    items: [
      { to: '/district/circuit/dashboard', label: 'Dashboard', icon: Home, exact: true },
      { to: '/district/circuit/schools', label: 'My Schools', icon: Building2 },
      { to: '/district/school-registration-requests', label: 'School Registration Requests', icon: Users2, showPendingBadge: true },
      { to: '/district/circuit/curriculum', label: 'Curriculum Monitoring', icon: BookOpen },
      { to: '/district/circuit/visits', label: 'School Visits', icon: CalendarDays },
      { to: '/district/circuit/support-requests', label: 'Support Requests', icon: LifeBuoy },
      { to: '/district/circuit/interventions', label: 'Interventions', icon: Wrench },
    ],
  },
  {
    title: 'Account',
    items: [
      { to: '/district/reports', label: 'Reports', icon: FileText },
      { to: '/account/change-password', label: 'Security / Change Password', icon: KeyRound },
    ],
  },
];

const subjectAdvisorNav: NavGroup[] = [
  {
    title: 'Advisor',
    items: [
      { to: '/district/advisor/dashboard', label: 'Dashboard', icon: Home, exact: true },
      { to: '/district/advisor/teachers', label: 'Teachers', icon: Users2 },
      { to: '/district/curriculum/atp-repository', label: 'Curriculum Resources', icon: BookOpen },
      { to: '/district/advisor/atp-monitoring', label: 'ATP Monitoring', icon: BarChart3 },
      { to: '/district/advisor/assessments', label: 'Assessments', icon: ClipboardCheck },
      { to: '/district/circuit/interventions', label: 'Interventions', icon: Wrench },
    ],
  },
  {
    title: 'Account',
    items: [
      { to: '/district/reports', label: 'Reports', icon: FileText },
      { to: '/account/change-password', label: 'Security / Change Password', icon: KeyRound },
    ],
  },
];

export const DistrictAdminLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [open, setOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const activeView = searchParams.get('view');
  const primaryRole = resolvePrimaryRole(user);

  const displayName = useMemo(() => user?.fullName || user?.email || 'District Admin', [user]);
  const roleLabel = primaryRole === 'DISTRICT_DIRECTOR'
    ? 'District Director'
    : primaryRole === 'CIRCUIT_MANAGER'
      ? 'Circuit Manager'
      : primaryRole === 'SUBJECT_ADVISOR'
        ? 'Subject Advisor'
        : 'District Admin';
  const navGroups = primaryRole === 'CIRCUIT_MANAGER'
    ? circuitManagerNav
    : primaryRole === 'SUBJECT_ADVISOR'
      ? subjectAdvisorNav
      : districtOverviewNav;
  const pendingRequestsQuery = useAppQuery({
    queryKey: ['district', 'school-registration-requests', 'pending-count'],
    queryFn: () => districtService.schoolRegistrationRequests({ status: 'PENDING' }),
    enabled: primaryRole === 'DISTRICT_ADMIN' || primaryRole === 'DISTRICT_DIRECTOR' || primaryRole === 'CIRCUIT_MANAGER' || primaryRole === 'ADMIN',
    staleTime: 30_000,
    refetchInterval: 60_000,
  });
  const pendingRequestsCount = pendingRequestsQuery.data?.total ?? 0;
  const headerTitle = primaryRole === 'CIRCUIT_MANAGER'
    ? 'Circuit performance and support'
    : primaryRole === 'SUBJECT_ADVISOR'
      ? 'Subject delivery and teacher support'
      : 'District-wide school intelligence';
  const headerBadge = primaryRole === 'CIRCUIT_MANAGER'
    ? 'Circuit command center'
    : primaryRole === 'SUBJECT_ADVISOR'
      ? 'Subject advisor workspace'
      : 'Premium command center';

  const isNavActive = (to: string, exact?: boolean) => {
    const [path, query] = to.split('?');
    if (window.location.pathname !== path) return false;
    if (!query) return exact ? window.location.pathname === path : true;
    const expectedView = new URLSearchParams(query).get('view');
    return activeView === expectedView;
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-slate-900">
      {open ? <button type="button" className="fixed inset-0 z-30 bg-slate-950/45 lg:hidden" onClick={() => setOpen(false)} aria-label="Close navigation" /> : null}
      <div className="flex min-h-screen">
        <aside className={`fixed inset-y-0 left-0 z-40 flex ${collapsed ? 'w-[72px]' : 'w-[260px]'} flex-col bg-[#0F172A] text-white shadow-2xl transition-[width,transform] duration-200 lg:static lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
          <div className={`flex items-center justify-between border-b border-white/10 ${collapsed ? 'px-3 py-4' : 'px-4 py-4'}`}>
            <button type="button" className="flex min-w-0 items-center gap-3 text-left" onClick={() => { navigate('/district/dashboard'); setOpen(false); }}>
              <DashboardLogo className="block h-20 w-auto shrink-0 object-contain border-4 border-green-500 bg-yellow-100" />
              <div className={collapsed ? 'hidden' : 'block'}>
                <h1 className="mt-1 text-xl font-semibold">District Portal</h1>
              </div>
            </button>
            <div className="flex items-center gap-2">
              <button type="button" className="hidden rounded-xl border border-white/15 p-2 lg:block" onClick={() => setCollapsed((value) => !value)} aria-label="Toggle navigation width">
                <Menu className="h-4 w-4" />
              </button>
              <button type="button" className="rounded-xl border border-white/15 p-2 lg:hidden" onClick={() => setOpen(false)} aria-label="Close navigation">
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>
          <div className={`border-b border-white/10 ${collapsed ? 'px-3 py-3' : 'px-4 py-3.5'}`}>
            <p className={`text-sm font-medium text-white ${collapsed ? 'hidden' : 'block'}`}>{displayName}</p>
            <p className={`mt-1 text-xs uppercase tracking-[0.2em] text-blue-200 ${collapsed ? 'hidden' : 'block'}`}>{roleLabel}</p>
            {collapsed ? <p className="text-center text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-200">Role</p> : null}
          </div>
          <nav className={`flex-1 space-y-5 overflow-y-auto ${collapsed ? 'px-2 py-4' : 'px-3 py-4'}`}>
            {navGroups.map((group) => (
              <div key={group.title}>
                <p className={`px-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400 ${collapsed ? 'hidden' : 'block'}`}>{group.title}</p>
                <div className="mt-2 space-y-1">
                  {group.items.map((item) => {
                    const active = isNavActive(item.to, item.exact);
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.label + item.to}
                        to={item.to}
                        onClick={() => setOpen(false)}
                        className={`relative flex items-center gap-3 rounded-2xl ${collapsed ? 'justify-center px-2 py-3' : 'px-3 py-2.5'} text-sm transition ${active ? 'bg-gradient-to-r from-[#2563EB] to-[#10B981] text-white shadow-lg shadow-blue-950/30' : 'text-slate-200 hover:bg-white/8 hover:text-white'}`}
                        title={collapsed ? item.label : undefined}
                      >
                        <Icon className="h-4 w-4" />
                        <span className={collapsed ? 'hidden' : 'block'}>{item.label}</span>
                        {item.showPendingBadge && pendingRequestsCount > 0 && !collapsed ? (
                          <span className="ml-auto rounded-full bg-amber-400 px-2 py-0.5 text-[11px] font-semibold text-slate-950">
                            Pending Requests: {pendingRequestsCount}
                          </span>
                        ) : null}
                        {item.showPendingBadge && pendingRequestsCount > 0 && collapsed ? (
                          <span className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full bg-amber-400" />
                        ) : null}
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            ))}
          </nav>
          <div className={`border-t border-white/10 ${collapsed ? 'p-2' : 'p-3'}`}>
            <button type="button" onClick={() => logout()} className={`flex w-full items-center gap-3 rounded-2xl ${collapsed ? 'justify-center px-2 py-3' : 'px-3 py-2.5'} text-sm font-medium text-slate-200 transition hover:bg-white/8 hover:text-white`}>
              <LogOut className="h-4 w-4" />
              <span className={collapsed ? 'hidden' : 'block'}>Logout</span>
            </button>
          </div>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/90 px-4 py-3 backdrop-blur md:px-5">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <button type="button" className="rounded-2xl border border-slate-200 p-2.5 text-slate-700 lg:hidden" onClick={() => setOpen(true)} aria-label="Open navigation">
                  <Menu className="h-5 w-5" />
                </button>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-700">{headerBadge}</p>
                  <h2 className="text-lg font-semibold text-slate-900 md:text-xl">{headerTitle}</h2>
                </div>
              </div>
              <div className="hidden rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700 md:block">
                Live district view
              </div>
            </div>
          </header>
          <main className="min-w-0 flex-1 overflow-y-auto px-4 py-4 md:px-5 md:py-5">
            <div className="mx-auto w-full max-w-none">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </div>
  );
};
