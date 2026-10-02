import { useSearchParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { GraduationCap, Wallet, BriefcaseBusiness } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { courseService } from '@/services/courseService';
import { jobsService } from '@/services/jobsService';
import { institutionService } from '@/services/institutionService';
import { universityInfoService } from '@/services/universityInfoService';
import { studentService } from '@/services/studentService';
import { bursaryService } from '@/services/bursaryService';
import { createInstitutionSlug } from '@/lib/institutionSlug';
import type { Institution } from '@/types';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import type { CareerRoadmapGenerateResponse, UnifiedOpportunity } from '@/types';
import { ExploreDialog, ExplorePanel } from './ExploreUi';

export function ExploreStudyOptions({ roadmap, query, expanded, onExpand }: { roadmap: CareerRoadmapGenerateResponse | null; query: string; expanded: boolean; onExpand: () => void }) {
  const institutions = useAppQuery<Institution[]>({ queryKey: ['student','institutions'], queryFn: () => institutionService.list() });
  const [coursePage,setCoursePage] = useState(0);
  useEffect(() => setCoursePage(0),[query]);
  const courses = useAppQuery({ queryKey: ['explore-courses',query,coursePage], queryFn: () => courseService.list({q:query,size:50,page:coursePage}), enabled: expanded });
  const courseItems = Array.isArray(courses.data) ? courses.data : courses.data?.content ?? [];
  const [province,setProvince] = useState('');
  const [type,setType] = useState('');
  const [qualification,setQualification] = useState('');
  const [institution,setInstitution] = useState('');
  const [maxAps,setMaxAps] = useState('');
  const [studyMode,setStudyMode] = useState('');
  const [params,setParams] = useSearchParams();
  const [selected,setSelected] = useState(params.get('institution') || '');
  useEffect(() => { setSelected(params.get('institution') || ''); }, [params.get('institution')]);
  const programmes = useAppQuery({ queryKey: ['explore-programmes',selected], queryFn: () => universityInfoService.programmes(selected), enabled: !!selected });
  const requirements = useAppQuery({ queryKey: ['explore-admissions',selected], queryFn: () => universityInfoService.admissionRequirements(selected), enabled: !!selected });
  const items = (institutions.data ?? []).map(item => resolveInstitutionDisplay(item));
  const matches = (roadmap?.universityRequirements ?? []).filter(item => (!province || item.province === province) && (!type || item.institutionType === type) && (!institution || item.institutionName === institution) && (!maxAps || (item.apsRequired != null && item.apsRequired <= Number(maxAps))) && `${item.institutionName} ${item.qualificationName}`.toLowerCase().includes(query.toLowerCase()));
  const filtered = items.filter(item => (!province || item.province === province) && (!type || item.institutionType === type) && (!institution || item.displayName === institution) && `${item.displayName} ${item.faculties}`.toLowerCase().includes(query.toLowerCase()));
  return <ExplorePanel title="Top Study Options" icon={<GraduationCap/>} action={<button onClick={onExpand}>View All</button>}>
    {expanded && <div className="ex-filter"><select aria-label="Province" value={province} onChange={e => setProvince(e.target.value)}><option value="">All provinces</option>{[...new Set(items.map(i => i.province).filter(Boolean))].map(p => <option key={p}>{p}</option>)}</select><select aria-label="Institution type" value={type} onChange={e => setType(e.target.value)}><option value="">All institution types</option>{[...new Set(items.map(i => i.institutionType).filter(Boolean))].map(p => <option key={p}>{p}</option>)}</select><select aria-label="Institution" value={institution} onChange={e => setInstitution(e.target.value)}><option value="">All institutions</option>{items.map(item => <option key={item.id || item.displayName}>{item.displayName}</option>)}</select><select aria-label="Qualification level" value={qualification} onChange={e => setQualification(e.target.value)}><option value="">All qualifications</option>{[...new Set(courseItems.map(item => item.level).filter(Boolean))].map(value => <option key={value}>{value}</option>)}</select>{roadmap && <label>Maximum required APS<input type="number" min="0" max="49" value={maxAps} onChange={e => setMaxAps(e.target.value)} placeholder="Any"/></label>}</div>}
    {expanded && courses.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading courses"/>}
    {expanded && courses.isError && <p className="ec-error">Courses could not load. <button onClick={() => courses.refetch()}>Retry</button></p>}
    {expanded && courseItems.filter(course => (!qualification || course.level === qualification) && (!institution || course.institutionName === institution) && (!province || items.some(item => item.displayName === course.institutionName && item.province === province)) && (!type || items.some(item => item.displayName === course.institutionName && item.institutionType === type))).map(course => <details className="ec-study" key={course.id}><summary><strong>{course.name}</strong><small>{course.institutionName}</small></summary><p className="ec-note">{course.duration} {course.level}</p><p className="ec-note">Confirm entry requirements directly with the institution.</p></details>)}
    {expanded && courses.data && !Array.isArray(courses.data) && courses.data.totalPages > 1 && <div className="ec-controls"><button disabled={coursePage === 0} onClick={() => setCoursePage(coursePage-1)}>Previous courses</button><span>Page {coursePage+1} of {courses.data.totalPages}</span><button disabled={coursePage+1 >= courses.data.totalPages} onClick={() => setCoursePage(coursePage+1)}>Next courses</button></div>}
    {matches.slice(0,expanded ? undefined : 3).map((item,i) => <div className="ec-study" key={`${item.institutionName}-${i}`}><GraduationCap/><span><strong>{item.qualificationName}</strong><small>{item.institutionName}</small><small>{item.apsRequired != null ? `APS ${item.apsRequired} · ` : ''}{item.duration}</small><small>{item.requirementStatus === 'Eligible' ? 'Likely meets requirements' : item.requirementStatus === 'Almost Eligible' ? 'Close to requirements' : item.requirementStatus === 'Not Yet Eligible' ? 'Requirements not yet met' : item.requirementStatus} · {item.verificationBadge}</small>{item.applicationUrl && <a href={item.applicationUrl} target="_blank" rel="noreferrer">View official requirements ↗</a>}</span></div>)}
    {(expanded || !matches.length) && filtered.slice(0,expanded ? undefined : 3).map(item => <button className="ec-study" key={item.id || item.displayName} onClick={() => setSelected(createInstitutionSlug(item.displayName))}><InstitutionLogo institutionName={item.displayName} src={item.logoUrl} abbreviation={item.abbreviation} size={36}/><span><strong>{item.displayName}</strong><small>{item.province} · {item.institutionType}</small><small>Browse programmes and admission requirements →</small></span></button>)}
    {institutions.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading institutions"/>}{institutions.isError && <p className="ec-error">Institutions could not load. <button onClick={() => institutions.refetch()}>Retry</button></p>}{!institutions.isLoading && !institutions.isError && !filtered.length && !matches.length && !courseItems.length && <p className="ec-empty">No study options match your filters.</p>}
    {selected && <ExploreDialog title="Programmes & admission requirements" onClose={() => { setSelected(''); setParams(previous => { const next = new URLSearchParams(previous); next.delete('institution'); return next; }, {replace:true}); }}>
      {programmes.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading programmes"/>}{programmes.isError && <p className="ec-error">Programmes could not load. <button onClick={() => programmes.refetch()}>Retry</button></p>}
      <p className="ec-note">{programmes.data?.message}</p><div className="ex-filter"><select aria-label="Study mode" value={studyMode} onChange={e => setStudyMode(e.target.value)}><option value="">All study modes</option>{programmes.data?.availableStudyModes.map(mode => <option key={mode}>{mode}</option>)}</select></div>{!programmes.isLoading && !programmes.isError && !programmes.data?.programmes.length && <p className="ec-empty">No programme records are available. Check the institution's official website.</p>}{programmes.data?.programmes.filter(item => !studyMode || item.studyMode === studyMode).map(item => <article className="ec-study" key={item.id}><GraduationCap/><span><strong>{item.name}</strong><small>{item.qualificationType} · {item.duration} · {item.studyMode}</small><a href={item.programmeUrl || item.sourceUrl} target="_blank" rel="noreferrer">Official programme ↗</a></span></article>)}
      {requirements.isError && <p className="ec-error">Admission requirements could not load. <button onClick={() => requirements.refetch()}>Retry</button></p>}{requirements.data?.requirements.map(item => <article className="ec-study" key={item.id}><span><strong>{item.programmeName || item.requirementTitle}</strong><small>{item.apsMinimum != null ? `Minimum APS ${item.apsMinimum}` : 'APS not published'}</small><small>{item.requiredSubjects.join(', ')}</small><small>{item.minimumMarks.join(', ')}</small><a href={item.sourceUrl} target="_blank" rel="noreferrer">Verify requirements ↗</a></span></article>)}
    </ExploreDialog>}
  </ExplorePanel>;
}

export function ExploreOpportunities({ query, fundingOnly, expanded = false, onExpand, savedOnly: initiallySaved = false }: { query: string; fundingOnly: boolean; expanded?: boolean; onExpand?: () => void; savedOnly?: boolean }) {
  const qc = useQueryClient();
  const [all,setAll] = useState(false);
  const [fundingPage,setFundingPage] = useState(0);
  useEffect(() => setFundingPage(0),[query,expanded]);
  const [opportunityType,setOpportunityType] = useState('ALL');
  const [savedOnly,setSavedOnly] = useState(initiallySaved);
  useEffect(() => setSavedOnly(initiallySaved), [initiallySaved]);
  const [detail,setDetail] = useState<UnifiedOpportunity | null>(null);
  const opportunities = useAppQuery({ queryKey: ['student-opportunities','explore',query,opportunityType], queryFn: () => studentService.searchOpportunities({ q:query, opportunityType }), enabled: !fundingOnly });
  const funding = useAppQuery({ queryKey: ['explore-funding',query,expanded,fundingPage], queryFn: async () => expanded || query ? bursaryService.search({ q:query, size:20, page:fundingPage }) : { items: await bursaryService.recommended(), total:0, page:0, size:20 }, enabled: fundingOnly });
  const toggle = useMutation({ mutationFn: (item: UnifiedOpportunity) => item.saved ? studentService.unsaveOpportunity(item.type,item.id) : studentService.saveOpportunity(item.type,item.id,item.title), onSuccess: () => qc.invalidateQueries({ queryKey: ['student-opportunities'] }) });
  const jobs = useAppQuery({ queryKey: ['explore-jobs',query], queryFn: () => jobsService.search({query,location:'South Africa'}), enabled: !fundingOnly && !!query, retry:false });
  const state = fundingOnly ? funding : opportunities;
  return <ExplorePanel title={fundingOnly ? 'Bursaries & Funding' : 'Opportunities For You'} icon={fundingOnly ? <Wallet/> : <BriefcaseBusiness/>} action={<button onClick={() => onExpand ? onExpand() : setAll(!all)}>{all ? 'Show less' : 'View All'}</button>}>
    {!fundingOnly && <div className="ex-filter"><select aria-label="Opportunity type" value={opportunityType} onChange={e => setOpportunityType(e.target.value)}><option value="ALL">All opportunities</option><option value="CAREER">Careers</option><option value="JOB">Jobs</option></select><label className="ex-check"><input type="checkbox" checked={savedOnly} onChange={e => setSavedOnly(e.target.checked)}/> Saved only</label></div>}
    {state.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading opportunities"/>}{state.isError && <p className="ec-error">Unable to load opportunities. <button onClick={() => state.refetch()}>Retry</button></p>}
    {fundingOnly ? funding.data?.items.slice(0,expanded ? undefined : 3).map(item => <article className="ec-study" key={item.externalId}><Wallet size={23}/><span><strong>{item.title}</strong><small>{item.provider}</small>{item.deadline && <small>Deadline: {item.deadline}{Number.isFinite(Date.parse(item.deadline)) && Date.parse(item.deadline) + 86400000 < Date.now() ? ' · Deadline passed' : ''}</small>}<details><summary>View Details</summary><p className="ec-note">{item.eligibility || 'Check eligibility with the provider.'}</p>{item.applicationLink && <a href={item.applicationLink} target="_blank" rel="noreferrer">Visit provider ↗</a>}</details></span></article>) : <div className="ex-opportunities">{opportunities.data?.filter(item => !savedOnly || item.saved).slice(0,all ? undefined : 4).map(item => <article key={`${item.type}-${item.id}`}><span className="ec-tag ec-tone-1">{item.type}</span><h3>{item.title}</h3><p>{[item.industry,item.location,item.qualification].filter(Boolean).join(' · ')}</p>{item.recommended && <small>Recommended for you</small>}<div className="ec-controls"><button onClick={() => setDetail(item)}>View Details</button><button disabled={toggle.isPending} onClick={() => toggle.mutate(item)}>{item.saved ? 'Unsave' : 'Save'}</button></div></article>)}</div>}
    {fundingOnly && expanded && funding.data && funding.data.total > funding.data.size && <div className="ec-controls"><button disabled={fundingPage === 0} onClick={() => setFundingPage(fundingPage-1)}>Previous funding</button><span>Page {fundingPage+1}</span><button disabled={(fundingPage+1)*funding.data.size >= funding.data.total} onClick={() => setFundingPage(fundingPage+1)}>Next funding</button></div>}
    {!fundingOnly && !savedOnly && !!query && jobs.isLoading && <div className="ex-skeleton" role="status" aria-label="Loading live jobs"/>}
    {!fundingOnly && !savedOnly && jobs.data?.slice(0,all ? undefined : 3).map(job => <article className="ec-study" key={job.id}><BriefcaseBusiness/><span><strong>{job.title}</strong><small>{[job.company,job.location,job.contractType].filter(Boolean).join(' / ')}</small><details><summary>View Details</summary><p className="ec-note">{job.description}</p><a href={job.redirectUrl} target="_blank" rel="noreferrer">View opportunity</a></details></span></article>)}
    {!fundingOnly && jobs.isError && <p className="ec-error">Live jobs are temporarily unavailable.</p>}
    {toggle.isError && <p role="alert" className="ec-error">Could not update your saved opportunity. Please retry.</p>}{!state.isLoading && !state.isError && !(fundingOnly ? funding.data?.items.length : opportunities.data?.filter(item => !savedOnly || item.saved).length) && <p className="ec-empty">No opportunities found. Try another search or complete your profile.</p>}
    {detail && <ExploreDialog title={detail.title} onClose={() => setDetail(null)}><p>{detail.type} · {detail.field || detail.industry}</p><p className="ec-note">{detail.location}</p><p className="ec-note">Qualification: {detail.qualification || 'Not provided'}</p><p className="ec-note">Demand: {detail.demand || 'Not provided'}</p><button className="ex-saved" disabled={toggle.isPending} onClick={() => toggle.mutate(detail,{ onSuccess: () => setDetail(null) })}>{detail.saved ? 'Unsave opportunity' : 'Save opportunity'}</button></ExploreDialog>}
  </ExplorePanel>;
}
