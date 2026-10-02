import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { BarChart3, BookOpen, BriefcaseBusiness, CalendarDays, Check, ChevronRight, Compass, GraduationCap, MapPin, Pencil, School, ShieldCheck, Sparkles, Target, UserRound, Wallet, Zap, type LucideIcon } from 'lucide-react';
import type { StudentProfile } from '@/types';
import { ProgressRing } from '@/components/student/dashboard/StudentDashboard';
import { academicMark } from '@/components/student/dashboard/dashboardData';
import banner from '@/assets/images/careers.jpeg';
import './student-profile.css';

const tabs = ['Overview', 'Academic Details', 'Interests & Skills', 'Career Aspirations', 'Goals', 'Settings'] as const;
type ProfileTab = typeof tabs[number];
const tabKeys = ['overview', 'academic', 'interests', 'career', 'goals', 'settings'];

type Props = {
  profile: StudentProfile;
  authenticatedName?: string;
  schoolName?: string;
  form: Record<string, string>;
  onChange: (field: string, value: string) => void;
  onSave: () => Promise<unknown>;
  saving: boolean;
  saveError?: string;
  personalContent: ReactNode;
  academicContent: ReactNode;
  readinessContent: ReactNode;
  guidanceContent: ReactNode;
  documentsContent: ReactNode;
  savedProfilesContent: ReactNode;
};

function ProfileCard({ title, icon: Icon, edit, className = '', children }: { title: string; icon: LucideIcon; edit?: () => void; className?: string; children: ReactNode }) {
  return <section className={`ed-panel ep-card ${className}`}><header className="ed-panel-heading"><h2><Icon size={22} />{title}</h2>{edit && <button className="ep-edit" type="button" onClick={edit}><Pencil size={12} />Edit<span className="sr-only"> {title}</span></button>}</header>{children}</section>;
}

function TextField({ label, field, form, onChange, multiline = false, hint }: { label: string; field: string; form: Props['form']; onChange: Props['onChange']; multiline?: boolean; hint?: string }) {
  return <label className="ep-field"><span>{label}</span>{multiline ? <textarea aria-label={label} rows={4} value={form[field] ?? ''} onChange={(event) => onChange(field, event.target.value)} /> : <input aria-label={label} value={form[field] ?? ''} onChange={(event) => onChange(field, event.target.value)} />}{hint && <small>{hint}</small>}</label>;
}

const Empty = ({ children }: { children: ReactNode }) => <p className="ep-empty">{children}</p>;

export function StudentProfileView(props: Props) {
  const { profile, schoolName, form, onChange } = props;
  const location = useLocation();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const sectionTab = ({ overview: 'overview', goals: 'goals', academic: 'academic', interests: 'interests', career: 'career', documents: 'settings' } as Record<string, string>)[params.get('section') || ''];
  const requestedTab = sectionTab || params.get('tab') || (location.pathname === '/student/goals' ? 'goals' : null);
  const aliasTab = location.pathname.endsWith('/academic-profile') ? 'academic' : location.pathname.endsWith('/documents') ? 'settings' : /\/(qualifications|experience)$/.test(location.pathname) ? 'career' : 'overview';
  const activeIndex = Math.max(0, tabKeys.indexOf(requestedTab ?? aliasTab));
  const activeTab = tabs[activeIndex];
  const [editingPersonal, setEditingPersonal] = useState(false);
  const [savedMessage, setSavedMessage] = useState('');
  const personalEditor = useRef<HTMLDivElement>(null);
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || props.authenticatedName || 'Your profile';
  const initials = name.split(/\s+/).slice(0, 2).map((word) => word[0]).join('').toUpperCase();
  const subjects = profile.subjectAchievements ?? [];
  const interests = profile.interests ?? [];
  const skills = profile.skills ?? [];
  const aspirations = [profile.preferredCareer, profile.careerGoals, ...(profile.qualifications ?? []), ...(profile.experience ?? [])].filter((item): item is string => Boolean(item?.trim()));
  const completion = [
    ['Personal Details', Boolean(profile.firstName && profile.lastName && profile.phone)],
    ['Academic Information', Boolean(profile.selectedGrade && subjects.some((subject) => subject.subjectName && subject.achievementLevel))],
    ['Interests & Skills', Boolean(interests.length && skills.length)],
    ['Career Aspirations', Boolean(profile.preferredCareer || profile.qualifications?.length || profile.experience?.length)],
    ['Goals / Action Plan', Boolean(profile.careerGoals?.trim())],
    ['Documents', Boolean(profile.cvFileUrl && profile.transcriptFileUrl)],
  ] as const;
  const showTab = (tab: ProfileTab) => {
    setEditingPersonal(false);
    setSavedMessage('');
    const next = new URLSearchParams(params);
    next.delete('tab');
    const key = tabKeys[tabs.indexOf(tab)];
    next.set('section', key === 'settings' ? 'documents' : key);
    navigate(tab === 'Goals' ? '/student/goals' : `/student/profile?${next}`, { preventScrollReset: true });
  };
  const editPersonal = () => { showTab('Overview'); setEditingPersonal(true); };
  useEffect(() => {
    if (editingPersonal) personalEditor.current?.querySelector<HTMLInputElement>('input')?.focus();
  }, [editingPersonal]);
  const save = async () => {
    setSavedMessage('');
    try { await props.onSave(); setSavedMessage('Profile saved successfully.'); setEditingPersonal(false); }
    catch { /* The existing mutation supplies the visible error. */ }
  };
  const field = (label: string, key: string, multiline = false, hint?: string) => <TextField label={label} field={key} form={form} onChange={onChange} multiline={multiline} hint={hint} />;

  return <div className="ep-profile">
    <section className="ep-profile-header">
      <div className="ep-hero"><img className="ep-banner" src={banner} alt="" /><div className="ep-identity"><div className="ep-profile-avatar" aria-label={`${name} initials`}>{initials}</div><div><h1>{name}</h1><p>{profile.selectedGrade || 'Grade not added yet'}{profile.location ? `  |  ${profile.location}` : ''}</p></div></div><button type="button" className="ep-hero-edit" onClick={editPersonal}><Pencil size={14} />Edit Profile</button></div>

    </section>
    {savedMessage && <p className="ep-save-success" role="status"><Check size={16} />{savedMessage}</p>}
    <div className="ep-main-grid">
      <div id="profile-tab-content" role="region" aria-label={activeTab} className="ep-tab-content">
        {activeTab === 'Overview' && !editingPersonal && <div className="ep-overview">
          <ProfileCard title="About Me" icon={UserRound} edit={editPersonal} className="ep-about"><div className="ep-about-body"><div>{profile.bio ? <p className="ep-bio">{profile.bio}</p> : <Empty>Tell us a little about yourself. Add a bio to complete this section.</Empty>}</div><aside className="ep-statement"><span aria-hidden="true">“</span><p>{profile.careerGoals || 'Add your career goal and make this space your own.'}</p></aside></div><dl className="ep-facts">{[
            { label: 'Date of Birth', value: profile.dateOfBirth ? new Date(`${profile.dateOfBirth.slice(0, 10)}T12:00:00`).toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' }) : undefined, icon: CalendarDays },
            { label: 'Location', value: profile.location, icon: MapPin },
            { label: 'School', value: schoolName, icon: School },
            { label: 'Grade', value: profile.selectedGrade, icon: GraduationCap },
          ].map(({ label, value, icon: Icon }) => <div key={label}><Icon size={22} /><div><dt>{label}</dt><dd>{value || 'Not added yet'}</dd></div></div>)}</dl></ProfileCard>
          <div className="ep-interest-row"><ProfileCard title="My Interests" icon={Target} edit={() => showTab('Interests & Skills')} className="ep-interests">{interests.length ? <div className="ep-interest-list">{interests.slice(0, 5).map((interest, index) => <div className="ep-interest" key={`${interest}-${index}`}><div className={`ep-interest-art ep-soft-${index % 4}`}><Compass size={29} /></div><h3>{interest}</h3></div>)}</div> : <Empty>Add your interests to discover study and career opportunities that fit you.</Empty>}{interests.length > 5 && <button type="button" className="ep-text-action" onClick={() => showTab('Interests & Skills')}>View all {interests.length} interests</button>}</ProfileCard>
          <ProfileCard title="My Strengths" icon={BarChart3} edit={() => showTab('Interests & Skills')} className="ep-strengths">{skills.length ? <div className="ep-skill-list">{skills.slice(0, 8).map((skill, index) => <span className={`ep-soft-${index % 4}`} key={`${skill}-${index}`}><ShieldCheck size={13} />{skill}</span>)}</div> : <Empty>Your skills will appear here. Complete this section to show your strengths.</Empty>}{skills.length > 8 && <button type="button" className="ep-text-action" onClick={() => showTab('Interests & Skills')}>View all skills</button>}</ProfileCard></div>
          <div className="ep-bottom-row"><ProfileCard title="Academic Snapshot" icon={BookOpen} edit={() => showTab('Academic Details')} className="ep-academic-snapshot">{subjects.length ? <div className="ep-subject-snapshot">{subjects.slice(0, 5).map((subject, index) => { const mark = academicMark(subject); return <div className={`ep-soft-${index % 4}`} key={`${subject.subjectName}-${index}`}><BookOpen size={21} /><strong>{mark !== null ? `${mark}%` : subject.achievementLevel ? `Level ${subject.achievementLevel}` : '—'}</strong><span>{subject.subjectName}</span></div>; })}</div> : <Empty>Add your report-card subjects and NSC achievement levels in Academic Details.</Empty>}</ProfileCard>
          <ProfileCard title="Career Aspirations" icon={Compass} edit={() => showTab('Career Aspirations')} className="ep-aspirations">{aspirations.length ? <ol>{aspirations.slice(0, 4).map((aspiration, index) => <li key={`${aspiration}-${index}`}><span>{index + 1}</span>{aspiration}</li>)}</ol> : <Empty>Add your career goals, qualifications and experience to shape your next steps.</Empty>}</ProfileCard></div>
        </div>}
        {activeTab === 'Overview' && editingPersonal && <div className="ep-editor" ref={personalEditor}><div className="ep-editor-title"><h2>Edit personal information</h2><button type="button" onClick={() => setEditingPersonal(false)}>Back to overview</button></div>{props.personalContent}</div>}
        {activeTab === 'Academic Details' && <div className="ep-editor">{props.academicContent}{props.readinessContent}<div className="ep-guidance-link"><Compass size={20} /><span>Review programme requirements and qualification guidance in Career Aspirations.</span><button type="button" onClick={() => showTab('Career Aspirations')}>Open guidance <ChevronRight size={14} /></button></div></div>}
        {activeTab === 'Interests & Skills' && <ProfileCard title="Interests & Skills" icon={Sparkles}><div className="ep-editor-fields">{field('Interests', 'interests', true, 'Separate your interests with commas.')}{field('Skills', 'skills', true, 'Separate your skills with commas.')}</div></ProfileCard>}
        {activeTab === 'Career Aspirations' && <div className="ep-editor"><ProfileCard title="Career Aspirations" icon={Compass}><div className="ep-editor-fields">{field('Career goals', 'careerGoals', true)}{field('Qualifications', 'qualifications', true, 'Separate qualifications with commas.')}{field('Experience', 'experience', true, 'Separate experience entries with commas.')}</div></ProfileCard>{props.guidanceContent}</div>}
        {activeTab === 'Goals' && <ProfileCard title="My Goals" icon={Target}><p className="ep-section-note">Your career goal is shared with Career Roadmaps and your personalised guidance.</p>{field('Career goals', 'careerGoals', true)}<div className="ep-goal-links"><Link to="/student/career-roadmaps"><Compass size={18} />Open Career Roadmaps<ChevronRight size={15} /></Link><Link to="/student/recommendations/careers"><Sparkles size={18} />Review AI Guidance<ChevronRight size={15} /></Link></div></ProfileCard>}
        {activeTab === 'Settings' && <div className="ep-editor">{props.documentsContent}{props.savedProfilesContent}<ProfileCard title="Account Settings" icon={ShieldCheck}><p className="ep-section-note">Your profile uses your initials. Profile photo uploads are not currently supported.</p><div className="ep-goal-links"><Link to="/student/settings">Notification &amp; account preferences<ChevronRight size={15} /></Link><Link to="/account/change-password">Change password<ChevronRight size={15} /></Link></div></ProfileCard></div>}
        {(activeTab !== 'Overview' || editingPersonal) && <div className="ep-save-bar"><p role={props.saveError ? 'alert' : undefined}>{props.saveError || 'Save to update your EduRite profile and linked guidance.'}</p><button type="button" className="ep-primary" onClick={save} disabled={props.saving}>{props.saving ? 'Saving profile…' : 'Save Profile'}</button></div>}
      </div>
      <aside className="ep-right-column">
        <ProfileCard title="My Profile Completion" icon={UserRound} className="ep-completion"><div className="ep-completion-body"><ProgressRing value={profile.profileCompleteness} /><ul>{completion.map(([label, complete]) => <li key={label}><span className={`ed-check ${complete ? 'is-complete' : 'ep-incomplete'}`} aria-label={complete ? 'Complete' : 'Incomplete'}>{complete && <Check size={10} />}</span>{label}</li>)}</ul></div><button className="ep-completion-action" type="button" onClick={editPersonal}>Complete your profile for more accurate career matches and study recommendations.<ChevronRight size={19} /></button></ProfileCard>
        <ProfileCard title="Quick Actions" icon={Zap} className="ep-quick-actions"><Link to="/student/psychometric"><Compass /><span><strong>Take Psychometric Test</strong><small>Discover your interests and strengths</small></span><ChevronRight /></Link><button type="button" onClick={() => showTab('Academic Details')}><ShieldCheck /><span><strong>Update My Results</strong><small>Keep your academic profile up to date</small></span><ChevronRight /></button><button type="button" onClick={() => showTab('Goals')}><Target /><span><strong>Update Career Goals</strong><small>Set your direction and next steps</small></span><ChevronRight /></button><Link to="/student/applications"><Wallet /><span><strong>Explore Bursaries</strong><small>Find funding opportunities</small></span><ChevronRight /></Link><Link to="/student/universities"><GraduationCap /><span><strong>Explore Universities</strong><small>Discover institutions and study options</small></span><ChevronRight /></Link><Link to="/student/recommendations/careers"><BriefcaseBusiness /><span><strong>AI Guidance</strong><small>Review personalised recommendations</small></span><ChevronRight /></Link></ProfileCard>
      </aside>
    </div>
  </div>;
}
