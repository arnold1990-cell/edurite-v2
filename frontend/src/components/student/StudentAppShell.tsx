import { StudentRouteAccess, AiUsageDisplay } from '@/features/subscriptions/access';
import { ExploreSearch } from '@/components/student/career/ExploreSearch';
import '@/components/student/career/explore-workspace.css';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { Bell, BookOpen, Bot, Building2, ChevronDown, ChevronRight, Compass, GraduationCap, Home, LogOut, Mail, Menu, Settings, Target, Trophy, UserRound, Wallet, X } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { EduRiteLogo } from '@/components/common/EduRiteLogo';
import './dashboard/student-dashboard.css';
import './student-shell.css';
import { notificationService } from '@/services/notificationService';
import { StudentSectionNavigation, studentSection } from './StudentSectionNavigation';

const navigation = [
  { label: 'Dashboard', to: '/student/dashboard', icon: Home },
  { label: 'My Profile', to: '/student/profile', icon: UserRound },
  { label: 'Career Explorer', to: '/student/career-explorer', icon: Compass },
  { label: 'Study Options', to: '/student/study-options', icon: GraduationCap },
  { label: 'Bursaries & Funding', to: '/student/funding', icon: Wallet },
  { label: 'Learning Resources', to: '/student/learning', icon: BookOpen },
  { label: 'AI Guidance', to: '/student/career-explorer?section=guidance', icon: Bot },
  { label: 'Messages', to: '/student/messages', icon: Mail },
];

export function StudentAppShell() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const unread = useAppQuery({ queryKey: ['notes-unread'], queryFn: notificationService.unreadCount, refetchInterval: 30000 });
  const unreadCount = unread.data?.unreadCount ?? 0;
  const section = studentSection(location.pathname, location.search);
  const isCurrent = (to: string) => section === to;
  useEffect(() => {
    if (!location.hash) return;
    const frame = requestAnimationFrame(() => document.getElementById(location.hash.slice(1))?.scrollIntoView());
    return () => cancelAnimationFrame(frame);
  }, [location.pathname, location.hash]);
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const [open, setOpen] = useState(false);
  const menuButton = useRef<HTMLButtonElement>(null);
  const drawer = useRef<HTMLElement>(null);
  const workspace = useRef<HTMLDivElement>(null);
  const account = useRef<HTMLDetailsElement>(null);
  useEffect(() => {
    setOpen(false);
    if (account.current) account.current.open = false;
  }, [location.pathname, location.search]);
  const name = [profile.data?.firstName, profile.data?.lastName].filter(Boolean).join(' ') || user?.fullName || user?.email || 'Student';
  const initials = name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
  useLayoutEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    const previousPadding = document.body.style.paddingRight;
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.paddingRight = `${parseFloat(getComputedStyle(document.body).paddingRight) + scrollbarWidth}px`;
    const background = workspace.current;
    background?.setAttribute('inert', '');
    document.body.style.overflow = 'hidden';
    drawer.current?.querySelector<HTMLButtonElement>('button')?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); setOpen(false); }
      if (event.key === 'Tab') {
        const items = Array.from(drawer.current?.querySelectorAll<HTMLElement>('a, button') ?? []).filter((item) => item.getClientRects().length);
        const first = items[0], last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.body.style.paddingRight = previousPadding;
      background?.removeAttribute('inert');
      document.removeEventListener('keydown', onKey);
      menuButton.current?.focus({ preventScroll: true });
    };
  }, [open]);
  return <div className="ed-dashboard">
    <a className="ed-skip" href="#student-dashboard-content">Skip to content</a>
    <div className={`ed-scrim ${open ? 'is-open' : ''}`} aria-hidden="true" onClick={() => setOpen(false)} />
    <aside ref={drawer} id="student-dashboard-navigation" className={`ed-sidebar ${open ? 'is-open' : ''}`} role="dialog" aria-modal={open || undefined} aria-hidden={!open} aria-label="Student navigation" {...(!open ? { inert: '' } : {})} onClick={(event) => { if ((event.target as HTMLElement).closest('a')) setOpen(false); }}>
      <button className="ed-drawer-close" onClick={() => setOpen(false)} aria-label="Close navigation"><X size={22} /></button>
      <Link to="/student/dashboard" className="ed-brand"><EduRiteLogo size="medium" /></Link>
      <nav>{navigation.map(({ label, to, icon: Icon }) => to.startsWith('#')
        ? <a key={to} href={to} onClick={() => setOpen(false)}><Icon size={19} /><span>{label}</span></a>
        : <Link key={to} to={to} className={isCurrent(to) ? 'is-active' : ''} aria-current={isCurrent(to) ? 'page' : undefined} onClick={() => setOpen(false)}><Icon size={19} /><span>{label}</span></Link>)}
      </nav>
      <Link to="/student/learning/tutor" className="ed-tutor"><span className="ed-tutor-icon"><Bot size={23} /></span><span><strong>Ask EduRite AI</strong><small>Get personalised guidance anytime.</small></span><ChevronRight size={16} /></Link>
    </aside>
    <div ref={workspace} className="ed-workspace">
      <header className="ed-header">
        <button ref={menuButton} className="ed-menu-button" aria-label="Open navigation" aria-expanded={open} aria-controls="student-dashboard-navigation" onClick={() => setOpen(true)}><Menu size={22} /></button>
        <div className="ed-search"><ExploreSearch /></div>
        <Link to="/student/notifications" className="ed-notifications" aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ''}`}><Bell size={22} />{unreadCount > 0 && <span />}</Link>
        <details ref={account} className="ed-account"><summary><span className="ed-avatar" aria-hidden="true">{initials}</span><span className="ed-account-name"><strong>{name}</strong><small>{profile.data?.selectedGrade || 'Student'}</small></span><ChevronDown size={15} /></summary><div className="ed-account-menu"><Link to="/student/profile"><UserRound size={15} />My Profile</Link><Link to="/student/subscription"><Wallet size={15} />Subscription</Link><Link to="/student/notifications"><Bell size={15} />Notifications</Link><Link to="/student/settings"><Settings size={15} />Settings</Link><button onClick={() => logout()}><LogOut size={15} />Log out</button></div></details>
      </header>
      <main id="student-dashboard-content" className="ed-content"><StudentSectionNavigation /><div className="ed-page-content"><StudentRouteAccess>{['/student/learning','/student/career-explorer'].includes(location.pathname) && <AiUsageDisplay />}<Outlet /></StudentRouteAccess></div></main>
    </div>
  </div>;
}
