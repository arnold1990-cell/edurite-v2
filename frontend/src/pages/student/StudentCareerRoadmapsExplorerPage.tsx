import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { Input } from '@/components/ui/Input';
import { useAppQuery } from '@/hooks/useAppQuery';
import { featureModulesService } from '@/services/featureModulesService';
import { studentService } from '@/services/studentService';
import type {
  ApsSubjectInput,
  CareerRoadmapGenerateResponse,
  CareerRoadmapPathwayStep,
  CareerRoadmapSubjectRequirement,
  SavedCareerRoadmap,
  StudentProfile,
} from '@/types';

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <section className="student-page-section">
    <header>
      <h1 className="student-page-title">{title}</h1>
    </header>
    <div className="space-y-5">{children}</div>
  </section>
);

const grades = ['Grade 9', 'Grade 10', 'Grade 11', 'Grade 12'];
const provinces = ['Eastern Cape', 'Free State', 'Gauteng', 'KwaZulu-Natal', 'Limpopo', 'Mpumalanga', 'Northern Cape', 'North West', 'Western Cape'];
const subjectOptions = [
  'Mathematics',
  'Mathematical Literacy',
  'English HL',
  'English FAL',
  'Afrikaans FAL',
  'Accounting',
  'Business Studies',
  'Economics',
  'Information Technology',
  'Computer Applications Technology',
  'Physical Sciences',
  'Life Sciences',
  'Geography',
  'History',
  'Life Orientation',
];
const tabs = ['Roadmap', 'University Requirements', 'APS Readiness', 'Subject Requirements', 'Alternative Pathways', 'AI Study Plan'] as const;
const careerSuggestions = ['Chartered Accountant', 'Doctor', 'Software Engineer', 'Lawyer', 'Nurse', 'Engineer'];

type SubjectRow = {
  id: string;
  subjectName: string;
  markPercentage: string;
  level: string;
};

const roadmapStatusColor = (status?: string): 'emerald' | 'amber' | 'slate' => {
  const normalized = (status ?? '').toLowerCase();
  if (normalized.includes('eligible') && !normalized.includes('almost') && !normalized.includes('not')) return 'emerald';
  if (normalized.includes('almost')) return 'amber';
  return 'slate';
};

const levelFromMark = (mark?: number | null) => {
  if (mark == null || Number.isNaN(mark)) return null;
  if (mark >= 80) return 7;
  if (mark >= 70) return 6;
  if (mark >= 60) return 5;
  if (mark >= 50) return 4;
  if (mark >= 40) return 3;
  if (mark >= 30) return 2;
  return 1;
};

const apsFromLevel = (level?: number | null) => {
  if (level == null || Number.isNaN(level)) return 0;
  return Math.max(1, Math.min(7, level));
};

const normalizeRow = (row: SubjectRow): ApsSubjectInput | null => {
  if (!row.subjectName.trim()) return null;
  const mark = row.markPercentage === '' ? null : Number(row.markPercentage);
  const level = row.level === '' ? levelFromMark(mark) : Number(row.level);
  return {
    subjectName: row.subjectName.trim(),
    markPercentage: mark == null || Number.isNaN(mark) ? null : mark,
    level: level == null || Number.isNaN(level) ? null : level,
    apsPoints: level == null || Number.isNaN(level) ? null : apsFromLevel(level),
  };
};

const localAps = (rows: SubjectRow[]) => {
  const subjects = rows.map(normalizeRow).filter(Boolean) as ApsSubjectInput[];
  const resolved = subjects.map((subject) => {
    const level = subject.level ?? levelFromMark(subject.markPercentage);
    return { ...subject, level, apsPoints: apsFromLevel(level) };
  });
  return {
    subjects: resolved,
    totalAps: resolved.reduce((sum, item) => sum + (item.apsPoints ?? 0), 0),
  };
};

const toSubjectRows = (profile?: StudentProfile, apsProfile?: { subjectName: string; markPercentage?: number | null; level?: number | null }[]) => {
  if (apsProfile?.length) {
    return apsProfile.map((item, index) => ({
      id: `${item.subjectName}-${index}`,
      subjectName: item.subjectName,
      markPercentage: item.markPercentage == null ? '' : String(item.markPercentage),
      level: item.level == null ? '' : String(item.level),
    }));
  }
  if (profile?.subjectAchievements?.length) {
    return profile.subjectAchievements.map((item, index) => ({
      id: `${item.subjectName}-${index}`,
      subjectName: item.subjectName,
      markPercentage: '',
      level: item.achievementLevel == null ? '' : String(item.achievementLevel),
    }));
  }
  return [
    { id: '1', subjectName: 'Mathematics', markPercentage: '', level: '' },
    { id: '2', subjectName: 'English HL', markPercentage: '', level: '' },
    { id: '3', subjectName: 'Life Orientation', markPercentage: '', level: '' },
  ];
};

const pathwayBlock = (title: string, steps: CareerRoadmapPathwayStep[]) => (
  <div className="rounded-2xl border border-slate-200 bg-white p-4">
    <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
    <div className="mt-3 space-y-3">
      {steps.map((step) => (
        <div key={`${title}-${step.title}`}>
          <p className="text-sm font-medium text-slate-800">{step.title}</p>
          <p className="text-sm text-slate-600">{step.description}</p>
        </div>
      ))}
    </div>
  </div>
);

const subjectRequirementCard = (item: CareerRoadmapSubjectRequirement) => (
  <article key={`${item.subject}-${item.required}`} className="rounded-2xl border border-slate-200 bg-white p-4">
    <div className="flex items-start justify-between gap-3">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">{item.subject}</h3>
        <p className="text-sm text-slate-600">{item.notes || (item.required ? 'Required subject' : 'Recommended subject')}</p>
      </div>
      <Badge color={item.required ? 'emerald' : 'amber'}>{item.required ? 'Required' : 'Recommended'}</Badge>
    </div>
    <div className="mt-3 grid gap-2 text-sm text-slate-700 sm:grid-cols-3">
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Minimum</p><p>{item.minimumPass || 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Level</p><p>{item.minimumLevel ?? 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Suggested mark</p><p>{item.suggestedMark || 'Not set'}</p></div>
    </div>
  </article>
);

export const StudentCareerRoadmapsExplorerPage = () => {
  const queryClient = useQueryClient();
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const apsProfile = useAppQuery({ queryKey: ['student-aps-profile'], queryFn: featureModulesService.apsProfile });
  const savedRoadmaps = useAppQuery({ queryKey: ['student-career-roadmaps-saved'], queryFn: featureModulesService.savedCareerRoadmaps });

  const [careerName, setCareerName] = useState('Chartered Accountant');
  const [grade, setGrade] = useState('Grade 12');
  const [province, setProvince] = useState('Gauteng');
  const [subjects, setSubjects] = useState<SubjectRow[]>([]);
  const [activeTab, setActiveTab] = useState<(typeof tabs)[number]>('Roadmap');
  const [generated, setGenerated] = useState<CareerRoadmapGenerateResponse | null>(null);
  const [history, setHistory] = useState<string[]>([]);

  useEffect(() => {
    if (!profile.data && !apsProfile.data) return;
    setGrade((current) => current || profile.data?.selectedGrade || apsProfile.data?.grade || 'Grade 12');
    setProvince((current) => current || apsProfile.data?.province || 'Gauteng');
    setSubjects((current) => current.length ? current : toSubjectRows(profile.data, apsProfile.data?.subjects));
  }, [profile.data, apsProfile.data]);

  const apsPreview = useMemo(() => localAps(subjects), [subjects]);

  const generate = useMutation({
    mutationFn: () => featureModulesService.generateCareerRoadmap({ careerName, grade, province, subjects: apsPreview.subjects }),
    onSuccess: (data) => {
      setGenerated(data);
      setHistory((current) => [data.careerName, ...current.filter((item) => item !== data.careerName)].slice(0, 8));
    },
  });

  const saveRoadmap = useMutation({
    mutationFn: () => {
      if (!generated) throw new Error('No roadmap to save');
      return featureModulesService.saveCareerRoadmap({
        careerName: generated.careerName,
        roadmap: generated,
        learnerAps: generated.apsReadiness.learnerAps,
        requiredAps: generated.apsReadiness.requiredAps,
        apsGap: generated.apsReadiness.apsGap,
        readinessScore: generated.apsReadiness.readinessScore,
      });
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['student-career-roadmaps-saved'] });
      setHistory((current) => [saved.careerName, ...current.filter((item) => item !== saved.careerName)].slice(0, 8));
    },
  });

  const selectSaved = (item: SavedCareerRoadmap) => {
    setCareerName(item.careerName);
    setGenerated(item.roadmap);
    setActiveTab('Roadmap');
  };

  if (profile.isLoading || apsProfile.isLoading) return <LoadingState message="Loading AI career roadmap explorer..." />;
  if (profile.isError || apsProfile.isError) return <ErrorState message="Could not load your career roadmap planner." />;

  const current = generated;
  const topStats = current ? [
    { label: 'Your APS', value: current.apsReadiness.learnerAps },
    { label: 'Required APS', value: current.apsReadiness.requiredAps },
    { label: 'APS Gap', value: current.apsReadiness.apsGap },
    { label: 'Best-fit Universities', value: current.apsReadiness.bestFitUniversities },
    { label: 'Readiness Score', value: `${current.apsReadiness.readinessScore}%` },
  ] : [
    { label: 'Your APS', value: apsPreview.totalAps },
    { label: 'Required APS', value: '--' },
    { label: 'APS Gap', value: '--' },
    { label: 'Best-fit Universities', value: '--' },
    { label: 'Readiness Score', value: '--' },
  ];

  return <Section title="AI Career Roadmap Explorer">
    <div className="grid gap-4 xl:grid-cols-[260px_minmax(0,1fr)_320px]">
      <aside className="space-y-4">
        <div className="rounded-3xl border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 p-5 text-white">
          <p className="text-xs uppercase tracking-[0.22em] text-sky-200">Search History</p>
          <div className="mt-4 space-y-2">
            {[...history, ...(savedRoadmaps.data ?? []).map((item) => item.careerName)].filter((item, index, list) => list.indexOf(item) === index).slice(0, 8).map((item) => (
              <button key={item} type="button" className="block w-full rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-left text-sm hover:bg-white/10" onClick={() => setCareerName(item)}>
                {item}
              </button>
            ))}
            {!history.length && !(savedRoadmaps.data ?? []).length ? <p className="text-sm text-sky-100/80">Saved and generated careers will appear here.</p> : null}
          </div>
        </div>
        <div className="rounded-3xl border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-slate-900">Saved Roadmaps</h2>
          <div className="mt-3 space-y-2">
            {(savedRoadmaps.data ?? []).map((item) => (
              <button key={item.id} type="button" className="block w-full rounded-2xl border border-slate-200 px-3 py-3 text-left hover:border-primary-300 hover:bg-primary-50" onClick={() => selectSaved(item)}>
                <p className="text-sm font-medium text-slate-900">{item.careerName}</p>
                <p className="text-xs text-slate-500">APS {item.learnerAps} • Score {item.readinessScore}%</p>
              </button>
            ))}
            {!(savedRoadmaps.data ?? []).length ? <p className="text-sm text-slate-500">No saved roadmaps yet.</p> : null}
          </div>
        </div>
      </aside>

      <main className="space-y-4">
        <div className="rounded-3xl border border-slate-200 bg-[radial-gradient(circle_at_top_left,_rgba(14,165,233,0.14),_transparent_40%),linear-gradient(135deg,_#ffffff,_#f8fafc)] p-5">
          <div className="grid gap-3 lg:grid-cols-[minmax(0,1.3fr)_140px_160px]">
            <label className="space-y-1 text-sm">
              <span>Search career e.g. Chartered Accountant, Doctor, Engineer</span>
              <Input value={careerName} onChange={(event) => setCareerName(event.target.value)} placeholder="Search career e.g. Chartered Accountant" />
            </label>
            <label className="space-y-1 text-sm">
              <span>Grade</span>
              <select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" value={grade} onChange={(event) => setGrade(event.target.value)}>
                {grades.map((item) => <option key={item}>{item}</option>)}
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span>Province</span>
              <select className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" value={province} onChange={(event) => setProvince(event.target.value)}>
                {provinces.map((item) => <option key={item}>{item}</option>)}
              </select>
            </label>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {careerSuggestions.map((item) => (
              <button key={item} type="button" className="rounded-full border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:border-primary-300 hover:bg-primary-50" onClick={() => setCareerName(item)}>
                {item}
              </button>
            ))}
          </div>
          <div className="mt-5 overflow-hidden rounded-3xl border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">Current subjects and marks</h2>
                <p className="text-sm text-slate-500">APS is calculated automatically from NSC levels.</p>
              </div>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => setSubjects((currentRows) => [...currentRows, { id: `${Date.now()}`, subjectName: '', markPercentage: '', level: '' }])}>Add Subject</Button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">Subject</th>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">Mark %</th>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">Level</th>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">APS</th>
                    <th className="px-4 py-3 text-left font-medium text-slate-500">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {subjects.map((row) => {
                    const normalized = normalizeRow(row);
                    const level = normalized?.level ?? levelFromMark(normalized?.markPercentage);
                    const aps = apsFromLevel(level);
                    return <tr key={row.id}>
                      <td className="px-4 py-3">
                        <input list="career-roadmap-subjects" className="w-full rounded-lg border border-slate-300 px-3 py-2" value={row.subjectName} onChange={(event) => setSubjects((currentRows) => currentRows.map((item) => item.id === row.id ? { ...item, subjectName: event.target.value } : item))} placeholder="Subject name" />
                      </td>
                      <td className="px-4 py-3">
                        <Input type="number" min={0} max={100} value={row.markPercentage} onChange={(event) => setSubjects((currentRows) => currentRows.map((item) => item.id === row.id ? { ...item, markPercentage: event.target.value, level: event.target.value ? String(levelFromMark(Number(event.target.value)) ?? '') : item.level } : item))} placeholder="75" />
                      </td>
                      <td className="px-4 py-3">
                        <Input type="number" min={1} max={7} value={row.level} onChange={(event) => setSubjects((currentRows) => currentRows.map((item) => item.id === row.id ? { ...item, level: event.target.value } : item))} placeholder="6" />
                      </td>
                      <td className="px-4 py-3 text-slate-700">{normalized?.subjectName ? aps : '--'}</td>
                      <td className="px-4 py-3">
                        <button type="button" className="text-sm font-medium text-red-600 hover:text-red-500" onClick={() => setSubjects((currentRows) => currentRows.filter((item) => item.id !== row.id))}>Remove</button>
                      </td>
                    </tr>;
                  })}
                </tbody>
              </table>
            </div>
            <datalist id="career-roadmap-subjects">
              {subjectOptions.map((item) => <option key={item} value={item} />)}
            </datalist>
          </div>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            <div className="rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.22em] text-sky-600">Your APS</p>
              <p className="text-2xl font-semibold text-slate-900">{apsPreview.totalAps}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button type="button" onClick={() => generate.mutate()} disabled={generate.isPending || !careerName.trim()}>{generate.isPending ? 'Generating...' : 'Generate Roadmap'}</Button>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" disabled={!generated || saveRoadmap.isPending} onClick={() => saveRoadmap.mutate()}>{saveRoadmap.isPending ? 'Saving...' : 'Save Roadmap'}</Button>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" disabled={!generated} onClick={() => window.print()}>Export PDF</Button>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" disabled={!generated || saveRoadmap.isPending} onClick={() => saveRoadmap.mutate()}>Add to My Career Plan</Button>
            </div>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          {topStats.map((card) => (
            <div key={card.label} className="rounded-2xl border border-slate-200 bg-white p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">{card.label}</p>
              <p className="mt-2 text-2xl font-semibold text-slate-900">{card.value}</p>
            </div>
          ))}
        </div>

        <div className="rounded-3xl border border-slate-200 bg-white">
          <div className="flex flex-wrap gap-2 border-b border-slate-200 px-4 py-3">
            {tabs.map((tab) => (
              <button key={tab} type="button" className={`rounded-full px-4 py-2 text-sm font-medium ${activeTab === tab ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`} onClick={() => setActiveTab(tab)}>
                {tab}
              </button>
            ))}
          </div>
          <div className="p-5">
            {!current ? <EmptyState title="Generate a roadmap" message="Search a career, enter your subjects and marks, then generate your AI career and university readiness roadmap." /> : null}

            {current && activeTab === 'Roadmap' ? <div className="space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-semibold text-slate-900">{current.careerName}</h2>
                    <p className="mt-2 text-sm text-slate-600">{current.overview.description || 'Career overview is being refined.'}</p>
                  </div>
                  <Badge color={roadmapStatusColor(current.apsReadiness.status)}>{current.apsReadiness.status}</Badge>
                </div>
                <div className="mt-4 grid gap-4 lg:grid-cols-2">
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Daily responsibilities</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.dailyResponsibilities.map((item) => <li key={item}>• {item}</li>)}
                    </ul>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Skills needed</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.skillsNeeded.map((item) => <li key={item}>• {item}</li>)}
                    </ul>
                  </div>
                </div>
                <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-3">
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Career demand</p><p>{current.overview.careerDemand || 'Verify current demand by region.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Salary range</p><p>{current.overview.salaryRange || 'Varies by experience.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Professional body</p><p>{current.overview.professionalBody || 'Check sector-specific registration.'}</p></div>
                </div>
              </div>
              <div className="grid gap-4 lg:grid-cols-2">
                {pathwayBlock('University pathway', current.universityPathway)}
                {pathwayBlock('Professional pathway', current.professionalPathway)}
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <h3 className="text-sm font-semibold text-slate-900">Roadmap timeline</h3>
                <div className="mt-4 space-y-3">
                  {current.roadmapTimeline.map((item) => (
                    <div key={`${item.stage}-${item.title}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-medium text-slate-900">{item.stage ? `${item.stage}. ` : ''}{item.title}</p>
                      <p className="mt-1 text-sm text-slate-600">{item.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div> : null}

            {current && activeTab === 'University Requirements' ? <div className="space-y-4">
              <div className="grid gap-3 lg:grid-cols-2">
                {current.universityRequirements.map((item) => (
                  <article key={`${item.institutionName}-${item.qualificationName}`} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h3 className="text-sm font-semibold text-slate-900">{item.institutionName}</h3>
                        <p className="text-sm text-slate-600">{item.qualificationName}</p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Badge color={item.verified ? 'emerald' : 'amber'}>{item.verificationBadge}</Badge>
                        <Badge color={roadmapStatusColor(item.requirementStatus)}>{item.requirementStatus}</Badge>
                      </div>
                    </div>
                    <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS</p><p>{item.apsRequired ?? 'Verify'}{item.apsGap ? ` • Gap ${item.apsGap}` : ''}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Province</p><p>{item.province || 'South Africa'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Mathematics</p><p>{item.mathematicsRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">English</p><p>{item.englishRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Accounting</p><p>{item.accountingRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Duration</p><p>{item.duration || 'Verify with institution'}</p></div>
                    </div>
                    <p className="mt-3 text-sm text-slate-600">{item.notes || item.source || 'Verify the full admission requirement directly with the institution.'}</p>
                    {item.applicationUrl ? <a className="mt-3 inline-block text-sm font-medium text-primary-600 hover:text-primary-500" href={item.applicationUrl} target="_blank" rel="noreferrer">Application link</a> : null}
                  </article>
                ))}
              </div>
            </div> : null}

            {current && activeTab === 'APS Readiness' ? <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Readiness summary</h3>
                  <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Current APS</p><p>{current.gapAnalysis.currentAps}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Required APS</p><p>{current.gapAnalysis.requiredAps}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS gap</p><p>{current.gapAnalysis.apsGap}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Risk level</p><p>{current.gapAnalysis.riskLevel}</p></div>
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Improvement suggestions</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {current.gapAnalysis.improvementSuggestions.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                </div>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Missing subjects</h3>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {current.gapAnalysis.missingSubjects.length ? current.gapAnalysis.missingSubjects.map((item) => <Badge key={item} color="amber">{item}</Badge>) : <p className="text-sm text-slate-500">No missing required subjects detected.</p>}
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Subjects needing improvement</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {current.gapAnalysis.subjectsNeedingImprovement.length ? current.gapAnalysis.subjectsNeedingImprovement.map((item) => <li key={item}>• {item}</li>) : <li className="text-slate-500">No urgent subject improvements flagged.</li>}
                  </ul>
                </div>
              </div>
            </div> : null}

            {current && activeTab === 'Subject Requirements' ? <div className="space-y-4">
              <div className="grid gap-4 lg:grid-cols-2">
                {current.requiredSubjects.map(subjectRequirementCard)}
                {current.recommendedSubjects.map(subjectRequirementCard)}
              </div>
            </div> : null}

            {current && activeTab === 'Alternative Pathways' ? <div className="space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <h3 className="text-sm font-semibold text-slate-900">Alternative pathways</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-700">
                  {current.alternativePathways.map((item) => <li key={item}>• {item}</li>)}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-900">Best-fit universities</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-700">
                  {current.gapAnalysis.bestFitUniversities.map((item) => <li key={item}>• {item}</li>)}
                </ul>
              </div>
            </div> : null}

            {current && activeTab === 'AI Study Plan' ? <div className="space-y-4">
              {current.studyPlan.map((item) => (
                <article key={item.title} className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">{item.title}</h3>
                  <p className="mt-1 text-sm text-slate-600">{item.focus}</p>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {item.actions.map((action) => <li key={action}>• {action}</li>)}
                  </ul>
                </article>
              ))}
            </div> : null}
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          Admission requirements may change yearly. Always verify final requirements directly with the university or college before applying.
        </div>
      </main>

      <aside className="space-y-4">
        <div className="rounded-3xl border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-slate-900">APS readiness</h2>
          {current ? <div className="mt-4 space-y-3">
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Status</p>
              <div className="mt-2 flex items-center justify-between gap-3">
                <p className="text-lg font-semibold text-slate-900">{current.apsReadiness.status}</p>
                <Badge color={roadmapStatusColor(current.apsReadiness.status)}>{current.apsReadiness.readinessScore}%</Badge>
              </div>
            </div>
            <div className="grid gap-3 text-sm text-slate-700">
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Learner APS</p><p className="mt-1 font-semibold text-slate-900">{current.apsReadiness.learnerAps}</p></div>
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Required APS</p><p className="mt-1 font-semibold text-slate-900">{current.apsReadiness.requiredAps}</p></div>
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS Gap</p><p className="mt-1 font-semibold text-slate-900">{current.apsReadiness.apsGap}</p></div>
            </div>
          </div> : <p className="mt-3 text-sm text-slate-500">Generate a roadmap to see eligibility, APS gap, and matched institutions.</p>}
        </div>
        <div className="rounded-3xl border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-slate-900">University match snapshot</h2>
          <div className="mt-3 space-y-2">
            {(current?.universityRequirements ?? []).slice(0, 5).map((item) => (
              <div key={`${item.institutionName}-${item.qualificationName}`} className="rounded-2xl border border-slate-200 p-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium text-slate-900">{item.institutionName}</p>
                    <p className="text-xs text-slate-500">{item.qualificationName}</p>
                  </div>
                  <Badge color={roadmapStatusColor(item.requirementStatus)}>{item.requirementStatus}</Badge>
                </div>
              </div>
            ))}
            {!current?.universityRequirements?.length ? <p className="text-sm text-slate-500">No institution matches yet.</p> : null}
          </div>
        </div>
      </aside>
    </div>
  </Section>;
};
