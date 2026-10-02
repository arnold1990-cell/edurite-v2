import { SavedCareers } from './SavedCareers';
import { ExplorePanel, ExploreDialog } from './ExploreUi';
import { useEffect, useState, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { BookOpen, Compass, GraduationCap, Target, Route, Pencil, Bookmark } from 'lucide-react';
import type { CareerRoadmapGenerateResponse, SavedCareerRoadmap, StudentProfile, ApsSubjectInput } from '@/types';
import { useAppQuery } from '@/hooks/useAppQuery';
import { recommendationService } from '@/services/recommendationService';
import { psychometricService } from '@/services/psychometricService';
import { careerService } from '@/services/careerService';
import { RecommendedCareerCard } from './RecommendedCareerCard';
import { ExploreOpportunities, ExploreStudyOptions } from './ExploreResources';
import hero from '@/assets/edurite-classroom-login-bg.png';
import './career-explorer.css';
import './explore-workspace.css';

type Props = {
  savedSnapshot: boolean; academicSubjects: ApsSubjectInput[]; profile?: StudentProfile; careerName: string; onSelect: (name: string) => void;
  roadmap: CareerRoadmapGenerateResponse | null; aps: number | null; requiredAps: number | null; gap: number | null;
  saved: SavedCareerRoadmap[]; savedLoading: boolean; retrySaved: () => void; savedError: boolean; onSaved: (item: SavedCareerRoadmap) => void;
  history: string[]; clearHistory: () => void; outdated: boolean; actions: ReactNode; feedback?: string;
  editor: ReactNode; details: ReactNode; printContent: ReactNode;
};
const categories = ['Careers', 'Courses', 'Subjects', 'Institutions', 'Bursaries', 'Career Pathways'];
export function ExploreWorkspace(p: Props) {
  const [params, setParams] = useSearchParams();
  const category = params.get('category') || 'Careers';
  const query = params.get('q') || '';
  const section = params.get('section');
  const focused = ['career-path', 'learning-path', 'study-plan', 'readiness'].includes(section || '');
  const [dialog, setDialog] = useState<'academic' | 'details' | 'saved' | null>(null);
  useEffect(() => { if (params.get('view') === 'saved') setDialog('saved'); }, [params]);
  const closeDialog = () => { setDialog(null); setParams(previous => { const next = new URLSearchParams(previous); next.delete('view'); return next; }, { replace: true }); };
  const [all, setAll] = useState(false);
  const [page,setPage] = useState(0);
  useEffect(() => setPage(0),[query]);
  const recommendations = useAppQuery({ queryKey: ['recommendations'], queryFn: recommendationService.mine });
  const psychometric = useAppQuery({ queryKey: ['psychometric-latest'], queryFn: psychometricService.latestStudent });
  const catalog = useAppQuery({ queryKey: ['explore-careers', query, page], queryFn: () => careerService.list({ q: query, size: 24, page }) });
  const catalogue = Array.isArray(catalog.data) ? catalog.data : catalog.data?.content ?? [];
  const careers = !query && !all && recommendations.data?.suggestedCareers?.length ? recommendations.data.suggestedCareers.map(item => ({ id: item.id, title: item.title, description: item.rationale, industry: 'Recommended for you' })) : catalogue;
  const roadmap = p.roadmap;
  const subjects = [...(roadmap?.requiredSubjects ?? []), ...(roadmap?.recommendedSubjects ?? [])].filter((s,i,a) => a.findIndex(x => x.subject === s.subject) === i);
  const changeCategory = (value: string) => { setParams(previous => { const next = new URLSearchParams(previous); next.set('category', value); return next; }); };
  return <div className="ec-explorer ex-workspace">
    <div className="ec-grid"><div className="ec-left">
      <section className="ec-hero"><img src={hero} alt="Students exploring their future"/><div className="ec-hero-copy"><span>CAREER GUIDANCE</span><h1>Explore. Discover.<br/>Find <em>Your Path.</em></h1><p>Discover careers that match your interests, strengths and subjects. See what you can study, where you can study and what opportunities are available.</p></div><nav aria-label="Explore categories">{categories.map(item => <button key={item} className={category === item ? 'is-active' : ''} onClick={() => changeCategory(item)}>{item}</button>)}</nav></section>
      <ExplorePanel title="Your Interests" icon={<Target/>} action={<Link to="/student/profile?tab=interests"><Pencil size={12}/> Edit Interests</Link>}><div className="ec-chips">{p.profile?.interests?.length ? p.profile.interests.map((interest,i) => <span key={interest} className={`ec-tone-${i%4}`}><Compass size={18}/>{interest}</span>) : <p className="ec-empty">Discover what inspires you. <Link to="/student/psychometric">Explore your strengths</Link> or add interests to your profile.</p>}</div></ExplorePanel>
      {p.history.length > 0 && <div className="ex-recent"><span>Recent searches</span>{p.history.map(name => <button key={name} onClick={() => p.onSelect(name)}>{name}</button>)}<button onClick={p.clearHistory}>Clear</button></div>}
      {['Careers','Career Pathways'].includes(category) && <ExplorePanel title={all ? 'Career Catalogue' : query ? 'Career search results' : recommendations.data?.suggestedCareers?.length ? 'Recommended Careers' : 'Discover Careers'} icon={<Compass/>} action={<button onClick={() => setAll(!all)}>{all ? 'Show less' : 'View All Careers'}</button>}>{recommendations.isError && !query && <p className="ec-note">Personalised recommendations are unavailable. You can still browse the career catalogue.</p>}<p className="ec-note">Explore careers. Select a career to check your personal academic readiness.</p>{catalog.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading careers"/>}{catalog.isError && <p role="alert" className="ec-error">Careers could not load. <button onClick={() => catalog.refetch()}>Retry</button></p>}<div className={`ec-career-cards ${all ? 'is-expanded' : ''}`}>{(all ? careers : careers.slice(0,8)).map((career,i) => <RecommendedCareerCard key={career.id} item={{ name: career.title, reason: career.description || 'Explore requirements and career pathways.', rankingCategory: career.industry, requirements: [], relatedProgrammes: [] }} index={i} selected={p.careerName === career.title} onDetails={() => p.onSelect(career.title)}/>)}</div>{all && catalog.data && !Array.isArray(catalog.data) && catalog.data.totalPages > 1 && <div className="ec-controls"><button disabled={page === 0} onClick={() => setPage(page-1)}>Previous careers</button><span>Page {page+1} of {catalog.data.totalPages}</span><button disabled={page+1 >= catalog.data.totalPages} onClick={() => setPage(page+1)}>Next careers</button></div>}{!catalog.isLoading && !catalog.isError && !careers.length && <p className="ec-empty">No careers found. Search for a different career above.</p>}</ExplorePanel>}
      {p.careerName && <ExplorePanel title={p.careerName} icon={<Compass/>} action={<button onClick={() => setDialog('saved')}><Bookmark size={13}/> Saved Careers / Roadmaps</button>}><p className="ec-note">{roadmap?.overview.description || 'Use your subjects and marks to discover the steps towards this career.'}</p><div className="ec-controls ex-actions">{p.actions}<button onClick={() => setDialog('academic')}>Edit academic profile</button>{roadmap && <button onClick={() => setDialog('details')}>View full guidance</button>}</div>{p.outdated && <p className="ec-error">Your academic information has changed. Regenerate this roadmap to refresh its analysis.</p>}{p.feedback && <p role="status" className="ec-note">{p.feedback}</p>}</ExplorePanel>}
      {!p.careerName && <button className="ex-saved" onClick={() => setDialog('saved')}><Bookmark size={15}/> Saved Careers / Roadmaps</button>}
    {focused && <section className="ed-panel p-4"><h2 className="text-xl font-semibold">{section === 'readiness' ? 'Academic Readiness' : section === 'career-path' ? 'Career Path' : 'Learning Path / Study Plan'}</h2><p>Choose a career above, then generate a roadmap or open a saved roadmap to review your plan.</p>{section === 'readiness' && p.editor}{p.details}</section>}
    {section === 'saved' && <SavedCareers loading={p.savedLoading} retry={p.retrySaved} roadmaps={p.saved} error={p.savedError} onRoadmap={p.onSaved} onCareer={p.onSelect} />}
      <div className={['Subjects','Courses','Institutions'].includes(category) ? 'ex-category-content' : 'ec-bottom'}>
        <ExplorePanel title="Related Subjects" icon={<BookOpen/>} action={<button onClick={() => setDialog('academic')}>View Academic Profile</button>}><div className="ec-subjects">{subjects.map((item,i) => { const mark = p.academicSubjects.find(s => s.subjectName.toLowerCase() === item.subject.toLowerCase()); return <div key={item.subject} className={`ec-tone-${i%4}`}><BookOpen size={23}/><strong>{item.subject}</strong><small>{item.minimumLevel ? `Required level: ${item.minimumLevel}` : item.minimumPass || 'See requirements'}</small>{mark?.level != null && <small>{mark.markPercentage != null ? `${mark.markPercentage}% / ` : ''}Your level: {mark.level} {item.minimumLevel ? mark.level >= item.minimumLevel ? '✓ Meets level' : '• Improvement needed' : ''}</small>}</div>; })}</div>{!subjects.length && <p className="ec-empty">Choose a career and generate its roadmap to see subject requirements.</p>}</ExplorePanel>
        <ExploreStudyOptions roadmap={p.outdated ? null : roadmap} query={query} expanded={['Institutions','Courses'].includes(category)} onExpand={() => changeCategory('Institutions')}/>
      </div>
      <ExploreOpportunities query={query} fundingOnly={false}/>
    </div><aside className="ec-right">
      <ExplorePanel title={p.savedSnapshot ? "Career Match / Saved snapshot" : "Career Match"} icon={<Target/>}><div className="ex-readiness"><Target size={42}/><strong>{p.outdated ? 'Refresh your roadmap' : roadmap?.apsReadiness.status || 'Discover your fit'}</strong></div><p className="ec-note">{roadmap ? 'Academic readiness based on the subjects and results used for this roadmap. Admission is not guaranteed.' : 'Choose a career and add your academic results for an evidence-based readiness assessment.'}</p><Link className="ec-pathway-link" to="/student/ai-guidance">Personalised AI guidance</Link>{psychometric.data?.strengthAreas?.length ? <p className="ec-note">Your strengths: {psychometric.data.strengthAreas.join(', ')}</p> : null}<Link className="ec-pathway-link" to="/student/psychometric">Discover your interests and strengths →</Link></ExplorePanel>
      <ExplorePanel title="Career Pathway" icon={<Route/>}><div className="ec-pathway">{roadmap && !p.outdated ? <ol>{roadmap.roadmapTimeline.map((step,i) => <li key={`${step.title}-${i}`}><span className={`ec-step ec-tone-${i%4}`}>{i+1}</span><div><h3>{step.title}</h3><p>{step.description}</p></div></li>)}</ol> : <p className="ec-empty">Your next chapter starts here. Select a career and generate your roadmap to see each step towards your goal.</p>}</div></ExplorePanel>
      <ExplorePanel title="Academic Readiness" icon={<GraduationCap/>}><div className="ex-aps">{[['Your APS',p.aps],['Target APS',p.outdated ? null : p.requiredAps],['APS Gap',p.outdated ? null : p.gap]].map(([label,value]) => <div key={label}><small>{label}</small><strong>{value ?? '—'}</strong></div>)}</div>{p.aps == null && <p className="ec-empty">Add your subjects and marks to check whether you meet requirements.</p>}<div className="ec-controls"><button onClick={() => setDialog('academic')}>Link My Profile / Add Subjects</button><Link to="/student/profile?tab=academic">View Academic Profile</Link></div></ExplorePanel>
      <ExploreOpportunities query={query} fundingOnly expanded={category === 'Bursaries'} onExpand={() => changeCategory('Bursaries')}/>
    </aside></div>
    {p.printContent}
    {dialog && <ExploreDialog title={dialog === 'academic' ? 'Subjects, marks & APS' : dialog === 'saved' ? 'Saved Careers / Roadmaps' : p.careerName} onClose={closeDialog}>{dialog === 'academic' ? p.editor : dialog === 'details' ? p.details : <SavedCareers loading={p.savedLoading} retry={p.retrySaved} roadmaps={p.saved} error={p.savedError} onRoadmap={item => { p.onSaved(item); closeDialog(); }} onCareer={name => { p.onSelect(name); closeDialog(); }}/>}</ExploreDialog>}
  </div>;
}
