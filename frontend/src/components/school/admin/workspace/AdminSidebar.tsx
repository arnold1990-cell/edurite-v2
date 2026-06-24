import {
  Bell,
  BookOpen,
  ClipboardCheck,
  FileBarChart2,
  FileText,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Settings,
  ShieldCheck,
  Users,
  UserSquare2,
  X,
  type LucideIcon,
} from 'lucide-react';
import { DashboardLogo } from '@/components/common/DashboardLogo';

type NavItem = { label: string; icon: LucideIcon };

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'Teachers', icon: UserSquare2 },
  { label: 'Learners', icon: Users },
  { label: 'Classes', icon: GraduationCap },
  { label: 'Subjects', icon: BookOpen },
  { label: 'Assignments', icon: ClipboardCheck },
  { label: 'Assessments', icon: FileText },
  { label: 'Results', icon: FileBarChart2 },
  { label: 'Reports', icon: FileBarChart2 },
  { label: 'Notifications', icon: Bell },
  { label: 'Settings', icon: Settings },
  { label: 'Change Password', icon: ShieldCheck },
  { label: 'Logout', icon: LogOut },
];

export const AdminSidebar = ({
  activeSection,
  setActiveSection,
  onLogout,
  mobileOpen = false,
  onCloseMobile,
}: {
  activeSection: string;
  setActiveSection: (section: string) => void;
  onLogout: () => void;
  mobileOpen?: boolean;
  onCloseMobile?: () => void;
}) => {
  const renderItems = () => navItems.map((item) => {
    const Icon = item.icon;
    const active = activeSection === item.label;
    return (
      <button
        key={item.label}
        type="button"
        onClick={() => {
          if (item.label === 'Logout') {
            onLogout();
            onCloseMobile?.();
            return;
          }
          setActiveSection(item.label);
          onCloseMobile?.();
        }}
        className={`mb-1.5 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm ${active ? 'bg-primary-600 text-white shadow-lg shadow-primary-600/25' : 'text-slate-700 hover:bg-slate-100'}`}
      >
        <Icon className="h-4 w-4" />
        {item.label}
      </button>
    );
  });

  return (
    <>
      <aside className="hidden w-[250px] shrink-0 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm lg:block">
        <div className="mb-4 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
          <DashboardLogo className="h-12 shrink-0" />
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-slate-500">EduRite</p>
            <p className="text-sm font-bold text-slate-900">School Admin Portal</p>
          </div>
        </div>
        {renderItems()}
      </aside>

      {mobileOpen ? (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 bg-slate-950/40 lg:hidden"
            aria-label="Close admin menu"
            onClick={onCloseMobile}
          />
          <aside className="fixed inset-y-0 left-0 z-50 w-[86vw] max-w-[320px] overflow-y-auto border-r border-slate-200 bg-white p-4 shadow-2xl lg:hidden">
            <div className="mb-4 flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
              <div className="flex items-center gap-3">
                <DashboardLogo className="h-12 shrink-0" />
                <div>
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-500">EduRite</p>
                  <p className="text-sm font-bold text-slate-900">School Admin Portal</p>
                </div>
              </div>
              <button type="button" onClick={onCloseMobile} className="rounded-lg border border-slate-200 p-1.5 text-slate-600" aria-label="Close menu">
                <X className="h-4 w-4" />
              </button>
            </div>
            {renderItems()}
          </aside>
        </>
      ) : null}
    </>
  );
};
