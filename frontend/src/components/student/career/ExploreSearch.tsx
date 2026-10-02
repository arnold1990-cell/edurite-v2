import { studentTools } from '@/components/student/StudentSectionNavigation';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { careerService } from '@/services/careerService';
import { courseService } from '@/services/courseService';
import { institutionService } from '@/services/institutionService';
import { bursaryService } from '@/services/bursaryService';
import { studentService } from '@/services/studentService';
import { createInstitutionSlug } from '@/lib/institutionSlug';
import type { Institution } from '@/types';

export function ExploreSearch() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') || '');
  const [term, setTerm] = useState('');
  const [open, setOpen] = useState(false);
  useEffect(() => setQuery(params.get('q') || ''), [params]);
  useEffect(() => { const timer = window.setTimeout(() => setTerm(query.trim()), 300); return () => clearTimeout(timer); }, [query]);
  const toolMatches = query.trim().length >= 2 ? studentTools.filter(tool => tool.label.toLowerCase().includes(query.trim().toLowerCase())) : [];
  const openTool = (to: string) => { navigate(to); setOpen(false); setQuery(''); };
  const enabled = open && term.length >= 2;
  const careers = useAppQuery({ queryKey: ['explore-search-careers', term], queryFn: () => careerService.list({ q: term, size: 4 }), enabled });
  const courses = useAppQuery({ queryKey: ['explore-search-courses', term], queryFn: () => courseService.list({ q: term, size: 4 }), enabled });
  const institutions = useAppQuery<Institution[]>({ queryKey: ['student', 'institutions'], queryFn: () => institutionService.list(), enabled });
  const funding = useAppQuery({ queryKey: ['explore-search-funding', term], queryFn: () => bursaryService.search({ q: term, size: 4 }), enabled });
  const opportunities = useAppQuery({ queryKey: ['explore-search-opportunities', term], queryFn: () => studentService.searchOpportunities({ q: term, opportunityType: 'ALL' }), enabled });
  const go = (category: string, extra: Record<string, string> = {}) => { navigate(`/student/explore?${new URLSearchParams({ q: query.trim(), category, ...extra })}`); setOpen(false); };
  const careerItems = Array.isArray(careers.data) ? careers.data : careers.data?.content ?? [];
  const courseItems = Array.isArray(courses.data) ? courses.data : courses.data?.content ?? [];
  const groups = [
    { title: 'Careers', state: careers, items: careerItems.map(item => ({ id: item.id, title: item.title, select: () => go('Careers', { career: item.title }) })) },
    { title: 'Courses', state: courses, items: courseItems.map(item => ({ id: item.id, title: `${item.name} - ${item.institutionName}`, select: () => go('Courses', { q: item.name }) })) },
    { title: 'Institutions', state: institutions, items: (institutions.data ?? []).filter(item => `${item.name} ${item.abbreviation || ''}`.toLowerCase().includes(term.toLowerCase())).slice(0,4).map(item => ({ id: item.id, title: item.name, select: () => go('Institutions', { institution: createInstitutionSlug(item.name), q: '' }) })) },
    { title: 'Bursaries', state: funding, items: (funding.data?.items ?? []).map(item => ({ id: item.externalId, title: item.title, select: () => go('Bursaries', { q: item.title }) })) },
    { title: 'Opportunities', state: opportunities, items: (opportunities.data ?? []).slice(0,4).map(item => ({ id: `${item.type}-${item.id}`, title: item.title, select: () => go('Opportunities', { q: item.title }) })) },
  ];
  return <form className="ex-search" role="search" onSubmit={e => { e.preventDefault(); if (toolMatches.length) openTool(toolMatches[0].to); else go('Careers'); }} onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) setOpen(false); }}>
    <Search size={18}/><input aria-label="Search tools, careers, courses, bursaries, institutions" aria-expanded={open && query.trim().length >= 2} aria-controls="explore-search-results" placeholder="Search tools, careers, courses, bursaries, institutions..." value={query} onChange={e => { setQuery(e.target.value); setOpen(true); }} onFocus={() => setOpen(true)} onKeyDown={e => { if (e.key === 'Escape') setOpen(false); }}/><button aria-label="Search Explore" type="submit">Search</button>
    {open && query.trim().length >= 2 && <div id="explore-search-results" className="ex-search-results">
      {toolMatches.length > 0 && <section aria-label="Student tools"><header><strong>Student tools</strong></header>{toolMatches.map(tool => <button type="button" key={`${tool.to}-${tool.label}`} onClick={() => openTool(tool.to)}>{tool.label}</button>)}</section>}
      {term !== query.trim() ? <p role="status">Searching...</p> : groups.map(group => <section key={group.title} aria-label={group.title}><header><strong>{group.title}</strong><button type="button" onClick={() => go(group.title)}>View all</button></header>{group.state.isFetching && <p role="status">Loading...</p>}{group.state.isError ? <p>Results unavailable. <button type="button" onClick={() => group.state.refetch()}>Retry</button></p> : group.items.map(item => <button type="button" key={item.id} onClick={item.select}>{item.title}</button>)}{!group.state.isFetching && !group.state.isError && !group.items.length && <p>No matches</p>}</section>)}
      <button type="button" onClick={() => go('Career Pathways', { career: query.trim() })}>Create a roadmap for {query}</button>
    </div>}
  </form>;
}
