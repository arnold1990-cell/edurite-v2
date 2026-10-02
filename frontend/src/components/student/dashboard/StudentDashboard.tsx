import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, BarChart3, BookOpen, CalendarDays, Check, ChevronRight, Circle, Compass, GraduationCap, Layers3, PlayCircle, Target, UserRound, Wallet, type LucideIcon } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useAppQuery } from '@/hooks/useAppQuery';
import { studentService } from '@/services/studentService';
import { recommendationService } from '@/services/recommendationService';
import { featureModulesService } from '@/services/featureModulesService';
import { learningService } from '@/services/learningService';
import { bursaryService } from '@/services/bursaryService';
import { schoolService } from '@/services/schoolService';
import type { Recommendation, StudentDashboard as DashboardData } from '@/types';
import { academicMark, careerMatch, percentage, upcomingBursaries } from './dashboardData';
import classroomPhoto from '@/assets/edurite-classroom-login-bg.png';
import careersPhoto from '@/assets/images/careers.jpeg';
import coursesPhoto from '@/assets/images/courses.jpeg';
import bursariesPhoto from '@/assets/images/bursaries.jpeg';
import './student-dashboard.css';

export function ProgressRing({ value, small = false }: { value: number | null; small?: boolean }) {
  const score = percentage(value);
  return <div className={`ed-ring ${small ? 'ed-ring-small' : ''}`} role="img" aria-label={score === null ? 'Progress unavailable' : `${Math.round(score)}%`}>
    <svg viewBox="0 0 100 100" aria-hidden="true"><circle cx="50" cy="50" r="43" className="ed-ring-track" /><circle cx="50" cy="50" r="43" className="ed-ring-value" strokeDasharray={`${(score ?? 0) * 2.702} 270.2`} /></svg>
    <strong>{score === null ? '—' : `${Math.round(score)}%`}</strong>
  </div>;
}

function Panel({ title, icon: Icon, href, action = 'View All', tone = 'orange', children, className = '', id }: { title: string; icon?: LucideIcon; href?: string; action?: string; tone?: string; children: ReactNode; className?: string; id?: string }) {
  return <section id={id} className={`ed-panel ${className}`}><header className="ed-panel-heading"><h2>{Icon && <Icon className={`ed-tone-${tone}`} size={22} />}{title}</h2>{href && (href.startsWith('#') ? <a href={href} onClick={() => { const target = document.getElementById(href.slice(1)); if (target instanceof HTMLDetailsElement) target.open = true; }}>{action}</a> : <Link to={href}>{action}</Link>)}</header>{children}</section>;
}
function DataState({ loading, error, empty }: { loading?: boolean; error?: boolean; empty: string }) {
  return <p className="ed-empty" role={error ? 'alert' : 'status'}>{loading ? 'Loading your data…' : error ? 'This section is temporarily unavailable. Please try again later.' : empty}</p>;
}
const shortcuts = [
  { title: 'Career Explorer', description: 'Discover careers based on your interests and strengths.', href: '/student/recommendations/careers', icon: Compass, tone: 'orange', image: careersPhoto },
  { title: 'Study Options', description: 'Find courses and explore your next steps.', href: '/student/learning-centre', icon: GraduationCap, tone: 'blue', image: coursesPhoto },
  { title: 'Institutions', description: 'Explore universities, TVET colleges and more.', href: '/student/universities', icon: Layers3, tone: 'purple', image: careersPhoto },
  { title: 'Bursaries & Funding', description: 'Find bursaries and other funding opportunities.', href: '/student/applications', icon: Wallet, tone: 'green', image: bursariesPhoto },
];

export function StudentDashboard({ renderDetails }: { renderDetails?: (data: DashboardData, recommendations?: Recommendation) => ReactNode }) {
  const { user } = useAuth();
  const dashboard = useAppQuery({ queryKey: ['dash'], queryFn: studentService.getDashboard });
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const recs = useAppQuery({ queryKey: ['recs'], queryFn: recommendationService.mine });
  const progress = useAppQuery({ queryKey: ['progress-score'], queryFn: featureModulesService.progressScore });
  const resources = useAppQuery({ queryKey: ['dashboard-learning-recommended'], queryFn: () => learningService.recommended(), staleTime: 300_000 });
  const bursaries = useAppQuery({ queryKey: ['dashboard-bursary-deadlines'], queryFn: () => bursaryService.list({ size: 30 }), staleTime: 300_000 });
  const school = useAppQuery({ queryKey: ['student', 'my-school', 'status'], queryFn: schoolService.getMySchoolStatus, staleTime: 60_000 });
  const [academicTab, setAcademicTab] = useState<'results' | 'subjects'>('results');
  const d = dashboard.data;
  const firstName = profile.data?.firstName || user?.fullName?.trim().split(/\s+/)[0] || 'there';
  const subjects = profile.data?.subjectAchievements ?? [];
  const careers = recs.data?.suggestedCareers?.slice(0, 4) ?? [];
  const studyOptions = recs.data?.suggestedCoursesOrImprovements?.filter((item) => item.id.startsWith('course-')).slice(0, 3) ?? [];
  const opportunities = upcomingBursaries(Array.isArray(bursaries.data) ? bursaries.data : bursaries.data?.content ?? []);
  const progressValue = percentage(progress.data?.overallPercentage);
  const checklist = [
    { label: 'Profile Completed', complete: profile.data ? profile.data.profileCompleted : null },
    { label: 'Interests Added', complete: profile.data ? Boolean(profile.data.interests?.length) : null },
    { label: 'Academic Results', complete: profile.data ? subjects.length > 0 : null },
    { label: 'Career Matches', complete: recs.data ? careers.length > 0 : null },
    { label: 'Goals Set', complete: d ? d.idealCareerObjectiveSet || d.trackProgress?.goalSet : null },
    { label: 'Bursary Applications', complete: d ? d.activeApplications > 0 : null },
  ];
  const goal = d?.idealCareerObjective || profile.data?.careerGoals || d?.trackProgress?.goalTitle;
  const milestones = d?.trackProgress?.nextMilestones ?? [];

  return <div className="ed-dashboard-page">
    <div className="ed-hero-row">
      <section className="ed-hero" aria-labelledby="student-welcome">
        <div className="ed-hero-copy"><h1 id="student-welcome">Hi {firstName},<span>Your future has possibilities.</span></h1><p>Explore careers, find the right courses,<br className="ed-desktop-break" /> discover institutions and apply for bursaries<br className="ed-desktop-break" /> — all in one place.</p><Link to="/student/career-roadmaps" className="ed-hero-button">Explore Your Path <ArrowRight size={15} /></Link></div>
        <img className="ed-hero-photo" src={classroomPhoto} alt="Learners studying in a classroom" />
      </section>
      <Panel title="My Progress" href="#dashboard-progress-details" id="dashboard-progress" className="ed-progress-panel"><div className="ed-progress-body"><div><ProgressRing value={progressValue} /><p className="ed-ring-caption">Overall Progress</p></div><ul className="ed-checklist">{checklist.map((item) => <li key={item.label}><span className={item.complete ? 'ed-check is-complete' : 'ed-check'} aria-label={item.complete === null ? 'Unavailable' : item.complete ? 'Complete' : 'Not complete'}>{item.complete ? <Check size={10} /> : null}</span>{item.label}</li>)}</ul></div>{progress.isError && <p className="ed-inline-message">Progress is currently unavailable.</p>}</Panel>
    </div>
    <div className="ed-shortcuts">{shortcuts.map(({ title, description, href, icon: Icon, tone, image }) => <Link key={title} to={href} className={`ed-shortcut ed-shortcut-${tone}`}><Icon className="ed-shortcut-icon" size={36} /><div><h2>{title}</h2><p>{description}</p></div><img src={image} alt="" /><span className="ed-arrow"><ArrowRight size={16} /></span></Link>)}</div>
    <div className="ed-lower-grid">
      <div className="ed-column ed-left-column">
        <Panel title="My Academic Performance" icon={BarChart3} tone="blue" href="/student/profile" className="ed-academics">
          <div className="ed-tabs" aria-label="Academic view"><button aria-pressed={academicTab === 'results'} className={academicTab === 'results' ? 'is-active' : ''} onClick={() => setAcademicTab('results')}>Latest Results</button><button aria-pressed={academicTab === 'subjects'} className={academicTab === 'subjects' ? 'is-active' : ''} onClick={() => setAcademicTab('subjects')}>Subject Performance</button><Link to="/student/career-roadmaps">APS Calculator</Link></div>
          {subjects.length ? <div className={`ed-marks ${academicTab === 'subjects' ? 'ed-marks-expanded' : ''}`}>{subjects.slice(0, academicTab === 'results' ? 6 : subjects.length).map((subject, index) => { const mark = academicMark(subject); return <div key={`${subject.subjectName}-${index}`} className={`ed-mark ${mark !== null && mark >= 75 ? 'ed-mark-green' : 'ed-mark-orange'}`}><strong>{mark !== null ? `${mark}%` : subject.achievementLevel ? `L${subject.achievementLevel}` : '—'}</strong><span>{subject.subjectName}</span>{mark !== null ? <progress max={100} value={mark} aria-label={`${subject.subjectName} mark`} /> : <small>{subject.achievementLevel ? 'NSC level' : 'No mark'}</small>}</div>; })}</div> : <DataState loading={profile.isLoading} error={profile.isError} empty="Add your academic results to your profile to see your subjects here." />}
        </Panel>
        <Panel title="Recommended Study Options" icon={GraduationCap} href="/student/learning-centre" className="ed-study">
          {studyOptions.length ? <div className="ed-study-cards">{studyOptions.map((item) => <Link to="/student/learning-centre" key={item.id} className="ed-study-card"><img src={coursesPhoto} alt="" /><div><h3>{item.title}</h3><p>{item.rationale}</p><span>Explore course <ArrowRight size={13} /></span></div></Link>)}</div> : <DataState loading={recs.isLoading} error={recs.isError} empty="Your recommended courses will appear here when available. Explore the Learning Centre to find study options." />}
        </Panel>
      </div>
      <div className="ed-column ed-middle-column">
        <Panel title="My Career Matches" icon={UserRound} href="/student/recommendations/careers" className="ed-careers">
          {careers.length ? <div className="ed-career-list">{careers.map((item) => <Link to="/student/recommendations/careers" key={item.id} className="ed-career"><div className="ed-career-art"><Compass size={28} /></div><div className="ed-career-copy"><h3>{item.title}</h3><p>{item.rationale}</p></div><div className="ed-match"><ProgressRing value={careerMatch(item)} small /><small>Match</small></div><ChevronRight size={15} /></Link>)}</div> : <DataState loading={recs.isLoading} error={recs.isError} empty="Complete your profile to discover career matches tailored to your interests." />}
        </Panel>
        <Panel title="Learning Resources" icon={BookOpen} href="/student/learning-centre" className="ed-learning">
          {resources.data?.length ? <div className="ed-resource-cards">{resources.data.slice(0, 3).map((item) => <Link key={item.id} to="/student/learning-centre" className="ed-resource"><div className="ed-resource-picture">{item.thumbnailUrl ? <img src={item.thumbnailUrl} alt="" loading="lazy" onError={(event) => { event.currentTarget.style.display = 'none'; }} /> : <BookOpen size={24} />}</div><div><PlayCircle size={17} /><h3>{item.title}</h3></div><small>{item.resourceType || item.provider || 'Learning resource'}</small></Link>)}</div> : <DataState loading={resources.isLoading} error={resources.isError} empty="No recommended learning resources yet. Browse the Learning Centre." />}
        </Panel>
      </div>
      <div className="ed-column ed-right-column">
        <Panel title="My Goals" icon={Target} href="/student/profile" action="Edit" id="dashboard-goals" className="ed-goals">
          {goal || milestones.length ? <ul className="ed-goal-list">{goal && <li><Target size={17} /><span>{goal}</span></li>}{milestones.slice(0, 4).map((item, index) => <li key={`${item}-${index}`}><Circle size={17} /><span>{item}</span></li>)}</ul> : <DataState loading={dashboard.isLoading} error={dashboard.isError} empty="Set your career goal in My Profile. Your next milestones will appear here." />}
        </Panel>
        <Panel title="Upcoming Opportunities" icon={CalendarDays} href="/student/applications" className="ed-opportunities">
          {opportunities.length ? opportunities.map((item, index) => <div key={item.id} className="ed-opportunity"><CalendarDays size={17} /><div><h3>{item.title}</h3><p>Closes {new Date(item.deadline!).toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' })}</p></div><Link className={index === 0 ? 'ed-action-solid' : 'ed-action-outline'} to={`/bursaries/${encodeURIComponent(item.id)}`}>View</Link></div>) : <DataState loading={bursaries.isLoading} error={bursaries.isError} empty="No upcoming bursary deadlines are available. Browse funding opportunities to find more." />}
        </Panel>
      </div>
    </div>
    <div className="ed-dashboard-extras">
      <details id="dashboard-progress-details"><summary>My progress, readiness &amp; recommendations <ChevronDownIcon /></summary><div className="ed-extra-body">{progress.data?.cards.map((card) => <div key={card.key} className="ed-progress-detail"><strong>{card.label}</strong><progress value={percentage(card.percentage) ?? 0} max={100} aria-label={card.label} /><span>{card.percentage}%</span><p>{card.recommendation}</p></div>)}{dashboard.isError ? <DataState error empty="" /> : d && renderDetails?.(d, recs.data)}</div></details>
      <details><summary>My School &amp; account <ChevronDownIcon /></summary><div className="ed-extra-body"><p>{school.data?.school?.name || school.data?.message || 'View or link your school account.'}</p><Link to="/student/my-school">Open My School</Link>{school.data?.status === 'APPROVED' && <Link to="/school-student/dashboard">School Portal</Link>}<div className="ed-account-links"><Link to="/student/rewards">Points &amp; Rewards{d ? ` · ${d.points} points` : ''}</Link><Link to="/student/saved">Saved Opportunities</Link><Link to="/student/psychometric">Psychometric Test</Link><Link to="/student/cv-builder">CV Builder</Link><Link to="/student/scholarships">Scholarship Assistant</Link><Link to="/student/university-applications">University Applications</Link><Link to="/student/subscription">Subscription</Link><Link to="/account/change-password">Change Password</Link></div></div></details>
    </div>
  </div>;
}

const ChevronDownIcon = () => <ChevronRight size={15} />;
