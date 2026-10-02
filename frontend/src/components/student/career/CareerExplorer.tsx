import { RecommendedCareerCard } from './RecommendedCareerCard';
import { useRef, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, ArrowRight, BookOpen, Compass, GraduationCap, Target, Wallet, Pencil, Check, Route, ChevronRight } from 'lucide-react';
import type { StudentProfile, UniversitySourcesAnalysisResponse, UniversityRecommendedCareer } from '@/types';
import { useAppQuery } from '@/hooks/useAppQuery';
import { featureModulesService } from '@/services/featureModulesService';
import { bursaryService } from '@/services/bursaryService';
import { ProgressRing } from '@/components/student/dashboard/StudentDashboard';
import hero from '@/assets/edurite-classroom-login-bg.png';
import './career-explorer.css';

type Props = { profile?: StudentProfile; advice?: UniversitySourcesAnalysisResponse; careers: UniversityRecommendedCareer[]; premium: boolean; controls: ReactNode; details: ReactNode; loading: boolean; error?: string | null };
const Empty = ({ children }: { children: ReactNode }) => <p className="ec-empty">{children}</p>;
export function CareerExplorer({ profile, advice, careers, premium, controls, details, loading, error }: Props) {
  const saved = useAppQuery({ queryKey: ['student-career-roadmaps-saved'], queryFn: featureModulesService.savedCareerRoadmaps });
  const funding = useAppQuery({ queryKey: ['career-explorer-bursaries'], queryFn: bursaryService.recommended });
  const [selected, setSelected] = useState<string>('');
  const [expanded, setExpanded] = useState(false);
  const cards = useRef<HTMLDivElement>(null);
  const detail = useRef<HTMLDetailsElement>(null);
  const career = careers.find(item => item.name === selected) ?? careers[0];
  const roadmap = saved.data?.find(item => item.careerName.toLowerCase() === career?.name.toLowerCase())?.roadmap;
  const steps = roadmap?.roadmapTimeline ?? [];
  const subjects = [...(roadmap?.requiredSubjects ?? []), ...(roadmap?.recommendedSubjects ?? [])].filter((item, index, all) => all.findIndex(other => other.subject === item.subject) === index);
  const programmes = premium ? advice?.recommendedProgrammes ?? [] : [];
  const score = advice?.careerSuitabilityScore ?? advice?.suitabilityScore;
  const showDetails = () => { if (detail.current) { detail.current.open = true; detail.current.scrollIntoView({ behavior: 'smooth', block: 'start' }); } };
  return <div className="ec-explorer">
    <div className="ec-grid">
      <div className="ec-left">
        <section className="ec-hero"><img src={hero} alt="" /><div className="ec-hero-copy"><span>CAREER GUIDANCE</span><h1>Explore. Discover.<br />Find <em>Your Path.</em></h1><p>Discover careers that match your interests, strengths and subjects. See what you can study, where you can study and what opportunities are available.</p></div><nav aria-label="Career guidance categories">{[['Careers','#recommended-careers'],['Courses','/student/universities'],['Subjects','/student/profile?tab=academic'],['Institutions','/student/universities'],['Bursaries','/student/applications'],['Career Pathways','/student/career-roadmaps']].map(([label,to], index) => to.startsWith('#') ? <a className="is-active" key={label} href={to}>{label}</a> : <Link key={label} to={to} className={index === 0 ? 'is-active' : ''}>{label}</Link>)}</nav></section>
        <section className="ed-panel ec-interests"><header><Target /><div><h2>Your Interests</h2><p>From your saved EduRite profile</p></div><Link to="/student/profile?tab=interests"><Pencil size={12} />Edit Interests</Link></header><div className="ec-chips">{profile?.interests?.length ? profile.interests.map((item,index) => <span className={`ec-tone-${index%4}`} key={item}><Compass size={19} />{item}</span>) : <Empty>Add your interests to personalise your career guidance.</Empty>}</div></section>
        <section className="ed-panel ec-recommendations" id="recommended-careers"><header><Compass /><div><h2>Recommended Careers</h2><p>Explore recommendations based on your profile and academic information.</p></div><button onClick={() => setExpanded(!expanded)}>{expanded ? 'Show carousel' : 'View All Careers'}<ArrowRight size={12} /></button></header>
          <div className="ec-controls">{controls}</div>
          {error && <p className="ec-error" role="alert">{error}</p>}
          {loading && <p className="ec-empty" role="status">Finding your career recommendations...</p>}
          {!careers.length && !loading && <Empty>Generate your guidance to explore suitable careers. Complete your profile if more information is needed.</Empty>}
          <div className={`ec-career-cards ${expanded ? 'is-expanded' : ''}`} ref={cards}>{careers.map((item,index) => <RecommendedCareerCard key={item.name} item={item} index={index} selected={career?.name === item.name} onSelect={() => setSelected(item.name)} onDetails={() => { setSelected(item.name); showDetails(); }} />)}</div>
          {careers.length > 3 && !expanded && <div className="ec-carousel-controls"><button aria-label="Previous careers" onClick={() => cards.current?.scrollBy({left:-350,behavior:'smooth'})}><ArrowLeft size={15}/></button><button aria-label="Next careers" onClick={() => cards.current?.scrollBy({left:350,behavior:'smooth'})}><ArrowRight size={15}/></button></div>}
        </section>
        <div className="ec-bottom"><section className="ed-panel"><header><BookOpen /><div><h2>Related Subjects</h2><p>Requirements for {career?.name || 'your selected career'}</p></div><Link to="/student/profile?tab=academic">View All</Link></header><div className="ec-subjects">{subjects.length ? subjects.map((item,index) => { const result = profile?.subjectAchievements?.find(subject => subject.subjectName.toLowerCase() === item.subject.toLowerCase()); return <div className={`ec-tone-${index%4}`} key={item.subject}><BookOpen size={23}/><strong>{item.subject}</strong><small>{item.required ? 'Required' : 'Recommended'}{item.minimumLevel ? ` · Level ${item.minimumLevel}+` : ''}</small>{result && <small>Your level: {result.achievementLevel ?? 'Not added'}</small>}{result?.achievementLevel && item.minimumLevel && result.achievementLevel < item.minimumLevel ? <small>Needs improvement</small> : null}</div>; }) : <Empty>Open Career Roadmaps to check subject requirements for this career. No requirements have been assumed.</Empty>}</div></section>
        <section className="ed-panel"><header><GraduationCap /><div><h2>Top Study Options</h2><p>Qualifications from your guidance</p></div><Link to="/student/universities">View All</Link></header>{programmes.slice(0,3).map(item => <button className="ec-study" key={`${item.name}-${item.university}`} onClick={showDetails}><GraduationCap size={27}/><span><strong>{item.name}</strong><small>{item.university}</small><small>{item.studentEligibilityStatus || item.matchLevel || 'Review admission requirements'}</small></span><ChevronRight size={16}/></button>)}{!programmes.length && <Empty>{premium ? 'Generate guidance to discover relevant study options.' : 'Explore universities for study options. Detailed programme guidance is available on Premium.'}</Empty>}</section></div>
      </div>
      <aside className="ec-right">
        <section className="ed-panel ec-match"><header><Target/><h2>Career Match</h2></header><div className="ec-match-body"><div><ProgressRing value={typeof score === 'number' && Number.isFinite(score) ? score : null}/><strong>Guidance suitability</strong></div><ul>{advice?.suitabilitySignalsUsed?.map(signal => <li key={signal}><Check size={14}/>{signal}</li>)}</ul></div><p className="ec-note">{advice?.suitabilityScoreReason || 'Generate guidance to see a supported suitability score. This is not an admission guarantee.'}</p></section>
        <section className="ed-panel ec-pathway"><header><Route/><div><h2>Career Pathway</h2><p>{career?.name || 'See the steps to reach your goal.'}</p></div></header>{steps.length ? <ol>{steps.map((step,index) => <li key={`${step.title}-${index}`}><span className={`ec-step ec-tone-${index%4}`}>{index+1}</span><div><h3>{step.title}</h3><p>{step.description}</p></div></li>)}</ol> : <Empty>{saved.isError ? 'Saved pathways are currently unavailable.' : 'No saved pathway for this career yet. Create one using your academic results in Career Roadmaps.'}</Empty>}<Link className="ec-pathway-link" to="/student/career-roadmaps">Open Career Roadmaps <ArrowRight size={14}/></Link><Link className="ec-pathway-link" to="/student/psychometric">Explore your strengths <ArrowRight size={14}/></Link></section>
        <section className="ed-panel ec-funding"><header><Wallet/><div><h2>Bursaries &amp; Funding</h2><p>Opportunities recommended for you</p></div><Link to="/student/applications">View All</Link></header>{funding.data?.slice(0,3).map(item => <Link className="ec-study" key={item.externalId} to="/student/applications"><Wallet size={23}/><span><strong>{item.title}</strong><small>{item.provider}</small>{item.deadline && <small>Deadline: {item.deadline}</small>}</span><ChevronRight size={15}/></Link>)}{!funding.data?.length && <Empty>{funding.isError ? 'Funding recommendations are temporarily unavailable.' : funding.isLoading ? 'Loading funding opportunities...' : 'No recommended bursaries yet. Explore the funding directory.'}</Empty>}</section>
      </aside>
    </div>
    <details className="ed-panel ec-details" ref={detail}><summary>Guidance details, requirements &amp; sources{career ? ` · ${career.name}` : ''}</summary>{details}</details>
  </div>;
}
