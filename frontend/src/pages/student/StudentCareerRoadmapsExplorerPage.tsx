import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { Input } from '@/components/ui/Input';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { careerService } from '@/services/careerService';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { useAppQuery } from '@/hooks/useAppQuery';
import { authoritativeAps, calculateApsGap, calculateNscLevel, countValidManualSubjects, createSubjectRow, fingerprintCalculation, normalizeManualRow, type ResultSource, type SubjectRow, toSubjectRowsFromAps } from '@/pages/student/roadmapAps.utils';
import { featureModulesService } from '@/services/featureModulesService';
import { studentService } from '@/services/studentService';
import type {
  ApsCalculationResponse,
  ApsSubjectInput,
  Career,
  CareerRoadmapGenerateResponse,
  CareerRoadmapPathwayStep,
  CareerRoadmapSubjectRequirement,
  PaginatedResponse,
  SavedCareerRoadmap,
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

type FeedbackState = {
  type: 'success' | 'error';
  message: string;
};

type AcademicResultSet = {
  source: ResultSource;
  resultSetId?: string;
  verificationStatus: 'VERIFIED' | 'UNVERIFIED' | 'INCOMPLETE';
  subjects: SubjectRow[];
};

const TEXT_REPLACEMENTS: Array<[string, string]> = [
  ['â€¢', '•'],
  ['â€“', '–'],
  ['â€”', '—'],
  ['â€™', '’'],
  ['ï¿½', '�'],
];

const roadmapStatusColor = (status?: string): 'emerald' | 'amber' | 'slate' => {
  const normalized = (status ?? '').toLowerCase();
  if (normalized.includes('eligible') && !normalized.includes('almost') && !normalized.includes('not')) return 'emerald';
  if (normalized.includes('almost')) return 'amber';
  return 'slate';
};

const normalizeText = (value?: string | null) => {
  if (!value) return '';
  return TEXT_REPLACEMENTS.reduce((result, [from, to]) => result.split(from).join(to), value).replace(/\uFFFD/g, '•');
};

const slugifyFilename = (value: string) => value
  .trim()
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-+|-+$/g, '') || 'career-roadmap';

const pathwayBlock = (title: string, steps: CareerRoadmapPathwayStep[]) => (
  <div className="rounded-2xl border border-slate-200 bg-white p-4">
    <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
    <div className="mt-3 space-y-3">
      {steps.map((step) => (
        <div key={`${title}-${step.title}`}>
          <p className="text-sm font-medium text-slate-800">{normalizeText(step.title)}</p>
          <p className="text-sm text-slate-600">{normalizeText(step.description)}</p>
        </div>
      ))}
    </div>
  </div>
);

const subjectRequirementCard = (item: CareerRoadmapSubjectRequirement) => (
  <article key={`${item.subject}-${item.required}`} className="rounded-2xl border border-slate-200 bg-white p-4">
    <div className="flex items-start justify-between gap-3">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">{normalizeText(item.subject)}</h3>
        <p className="text-sm text-slate-600">{normalizeText(item.notes) || (item.required ? 'Required subject' : 'Recommended subject')}</p>
      </div>
      <Badge color={item.required ? 'emerald' : 'amber'}>{item.required ? 'Required' : 'Recommended'}</Badge>
    </div>
    <div className="mt-3 grid gap-2 text-sm text-slate-700 sm:grid-cols-3">
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Minimum</p><p>{normalizeText(item.minimumPass) || 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Level</p><p>{item.minimumLevel ?? 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Suggested mark</p><p>{normalizeText(item.suggestedMark) || 'Not set'}</p></div>
    </div>
  </article>
);

export const StudentCareerRoadmapsExplorerPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const apsProfile = useAppQuery({ queryKey: ['student-aps-profile'], queryFn: featureModulesService.apsProfile });
  const savedRoadmaps = useAppQuery({ queryKey: ['student-career-roadmaps-saved'], queryFn: featureModulesService.savedCareerRoadmaps });
  const [actionFeedback, setActionFeedback] = useState<FeedbackState | null>(null);

  const [careerName, setCareerName] = useState('Chartered Accountant');
  const [grade, setGrade] = useState('Grade 12');
  const [province, setProvince] = useState('Gauteng');
  const [activeSource, setActiveSource] = useState<ResultSource>('MANUAL');
  const [manualSubjects, setManualSubjects] = useState<SubjectRow[]>([
    createSubjectRow(),
    createSubjectRow(),
    createSubjectRow(),
  ]);
  const [profileSubjects, setProfileSubjects] = useState<SubjectRow[]>([]);
  const [activeTab, setActiveTab] = useState<(typeof tabs)[number]>('Roadmap');
  const [generated, setGenerated] = useState<CareerRoadmapGenerateResponse | null>(null);
  const [generatedFingerprint, setGeneratedFingerprint] = useState('');
  const [history, setHistory] = useState<string[]>([]);

  useEffect(() => {
    if (!profile.data && !apsProfile.data) return;
    setGrade((current) => current || profile.data?.selectedGrade || apsProfile.data?.grade || 'Grade 12');
    setProvince((current) => current || apsProfile.data?.province || 'Gauteng');
    setProfileSubjects((current) => current.length ? current : toSubjectRowsFromAps(apsProfile.data?.subjects));
  }, [profile.data, apsProfile.data]);

  const subjects = activeSource === 'PROFILE' ? profileSubjects : manualSubjects;
  const activeInputs = useMemo(() => subjects.map(normalizeManualRow).filter(Boolean) as ApsSubjectInput[], [subjects]);
  const manualFingerprint = useMemo(() => fingerprintCalculation('MANUAL', careerName, grade, province, manualSubjects.map(normalizeManualRow).filter(Boolean) as ApsSubjectInput[]), [careerName, grade, province, manualSubjects]);
  const profileFingerprint = useMemo(() => fingerprintCalculation('PROFILE', careerName, grade, province, apsProfile.data?.subjects?.map((item) => ({
    subjectName: item.subjectName,
    markPercentage: item.markPercentage ?? null,
    level: item.level ?? null,
    apsPoints: item.apsPoints ?? item.level ?? null,
  })) ?? [], apsProfile.data?.resultSetId), [careerName, grade, province, apsProfile.data]);
  const activeFingerprint = activeSource === 'PROFILE' ? profileFingerprint : manualFingerprint;
  const manualAps = useAppQuery<ApsCalculationResponse>({
    queryKey: ['student-roadmap-aps-manual', grade, province, manualFingerprint],
    enabled: activeSource === 'MANUAL' && activeInputs.length > 0,
    queryFn: () => featureModulesService.calculateAps({ grade, province, subjects: activeInputs }),
  });
  const activeAps = activeSource === 'PROFILE' ? apsProfile : manualAps;
  const current = generated;
  const manualResultSet = useMemo<AcademicResultSet>(() => ({
    source: 'MANUAL',
    verificationStatus: activeInputs.length >= 6 ? 'VERIFIED' : activeInputs.length > 0 ? 'INCOMPLETE' : 'UNVERIFIED',
    subjects: manualSubjects,
  }), [activeInputs.length, manualSubjects]);
  const profileResultSet = useMemo<AcademicResultSet>(() => ({
    source: 'PROFILE',
    resultSetId: apsProfile.data?.resultSetId ?? undefined,
    verificationStatus: !profileSubjects.length
      ? 'INCOMPLETE'
      : apsProfile.data?.status === 'UNAVAILABLE'
        ? 'INCOMPLETE'
        : 'VERIFIED',
    subjects: profileSubjects,
  }), [apsProfile.data?.resultSetId, apsProfile.data?.status, profileSubjects]);
  const activeResultSet = activeSource === 'PROFILE' ? profileResultSet : manualResultSet;
  const careerLookup = useAppQuery<PaginatedResponse<Career> | Career[]>({
    queryKey: ['career-roadmap-career-match', current?.careerName ?? ''],
    enabled: Boolean(current?.careerName?.trim()),
    queryFn: () => careerService.list({ q: current?.careerName ?? '', size: 25 }),
  });
  const careerOptions = useMemo(() => Array.isArray(careerLookup.data) ? careerLookup.data : careerLookup.data?.content ?? [], [careerLookup.data]);
  const matchedCareer = useMemo(() => {
    if (!current?.careerName) return null;
    const normalizedCareerName = current.careerName.trim().toLowerCase();
    return careerOptions.find((item) => item.title?.trim().toLowerCase() === normalizedCareerName)
      ?? careerOptions.find((item) => item.title?.trim().toLowerCase().includes(normalizedCareerName))
      ?? null;
  }, [careerOptions, current?.careerName]);

  const generate = useMutation({
    mutationFn: () => featureModulesService.generateCareerRoadmap({ careerName, grade, province, subjects: activeInputs }),
    onSuccess: (data) => {
      setGenerated(data);
      setGeneratedFingerprint(activeFingerprint);
      setActionFeedback(null);
      setHistory((current) => [data.careerName, ...current.filter((item) => item !== data.careerName)].slice(0, 8));
    },
  });

  const saveRoadmap = useMutation({
    mutationFn: () => {
      if (!generated) throw new Error('No roadmap to save');
      return featureModulesService.saveCareerRoadmap({
        careerName: generated.careerName,
        roadmap: generated,
        learnerAps: authoritativeAps(activeAps.data) ?? generated.apsReadiness.learnerAps,
        requiredAps: generated.apsReadiness.requiredAps,
        apsGap: generated.apsReadiness.apsGap,
        readinessScore: generated.apsReadiness.readinessScore,
      });
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['student-career-roadmaps-saved'] });
      setActionFeedback({ type: 'success', message: 'Roadmap saved successfully.' });
      setHistory((current) => [saved.careerName, ...current.filter((item) => item !== saved.careerName)].slice(0, 8));
    },
    onError: (error) => {
      setActionFeedback({ type: 'error', message: (error as Error).message || 'Could not save this roadmap right now.' });
    },
  });

  const addToCareerPlan = useMutation({
    mutationFn: async () => {
      if (!matchedCareer?.id) {
        throw new Error('This roadmap career is not available in your saved careers catalog yet.');
      }
      await studentService.saveCareer(matchedCareer.id);
    },
    onSuccess: async () => {
      setActionFeedback({ type: 'success', message: 'Career added to My Career Plan.' });
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (error) => {
      setActionFeedback({ type: 'error', message: (error as Error).message || 'Could not add this career to your plan right now.' });
    },
  });

  const selectSaved = (item: SavedCareerRoadmap) => {
    setCareerName(item.careerName);
    setGenerated(item.roadmap);
    setGeneratedFingerprint(`saved:${item.id}`);
    setActiveTab('Roadmap');
    setActionFeedback(null);
  };

  const selectHistoryItem = (item: string) => {
    const saved = (savedRoadmaps.data ?? []).find((roadmap) => roadmap.careerName === item);
    if (saved) {
      selectSaved(saved);
      return;
    }
    setCareerName(item);
    setGenerated(null);
    setActiveTab('Roadmap');
    setActionFeedback(null);
  };

  const exportRoadmap = () => {
    if (!current) return;
    const previousTitle = document.title;
    document.title = `${slugifyFilename(current.careerName)}-career-roadmap`;
    window.print();
    window.setTimeout(() => {
      document.title = previousTitle;
    }, 1000);
  };

  if (profile.isLoading || apsProfile.isLoading) return <LoadingState message="Loading AI career roadmap explorer..." />;
  if (profile.isError || apsProfile.isError || (activeSource === 'MANUAL' && manualAps.isError)) return <ErrorState message="Could not load your career roadmap planner." />;

  const switchToManual = () => {
    setActiveSource('MANUAL');
    setGenerated(null);
    setGeneratedFingerprint('');
    setActionFeedback(null);
  };

  const switchToProfile = () => {
    if (!(apsProfile.data?.subjects?.length)) {
      setActionFeedback({ type: 'error', message: 'No verified academic results were found in your profile. Upload a report or enter your subjects manually.' });
      return;
    }
    setProfileSubjects(toSubjectRowsFromAps(apsProfile.data.subjects));
    setActiveSource('PROFILE');
    setGenerated(null);
    setGeneratedFingerprint('');
    setActionFeedback(null);
  };

  const addToCareerPlanDisabledReason = !generated
    ? 'Generate a roadmap before adding it to your career plan.'
    : !matchedCareer?.id
      ? 'This roadmap is generated, but it is not linked to a saved EduRite career record for My Career Plan yet.'
      : '';
  const activeApsValue = authoritativeAps(activeAps.data);
  const activeApsStatus = activeAps.data?.status ?? 'UNAVAILABLE';
  const analysisOutdated = Boolean(generated && generatedFingerprint && generatedFingerprint !== activeFingerprint);
  const canUseActiveAnalysis = activeApsStatus !== 'UNAVAILABLE' && activeResultSet.subjects.length > 0;
  const displayRequiredAps = current?.apsReadiness.requiredAps ?? current?.gapAnalysis.requiredAps ?? null;
  const displayCurrentAps = canUseActiveAnalysis ? activeApsValue : null;
  const displayApsGap = calculateApsGap(displayCurrentAps, displayRequiredAps);
  const profileRowsUnavailable = activeSource === 'PROFILE' && activeApsStatus === 'UNAVAILABLE';
  const topStats = current ? [
    { label: 'Your APS', value: displayCurrentAps ?? '--' },
    { label: 'Required APS', value: displayRequiredAps ?? '--' },
    { label: 'APS Gap', value: displayApsGap ?? '--' },
    { label: 'Best-fit Universities', value: current.apsReadiness.bestFitUniversities },
    { label: 'Readiness Score', value: current.apsReadiness.status },
  ] : [
    { label: 'Your APS', value: activeApsValue ?? '--' },
    { label: 'Required APS', value: '--' },
    { label: 'APS Gap', value: '--' },
    { label: 'Best-fit Universities', value: '--' },
    { label: 'Readiness Score', value: activeAps.data?.status ?? '--' },
  ];

  return <Section title="AI Career Roadmap Explorer">
    <div className="grid gap-4 xl:grid-cols-[260px_minmax(0,1fr)_320px]">
      <aside className="space-y-4">
        <div className="rounded-3xl border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 p-5 text-white">
          <p className="text-xs uppercase tracking-[0.22em] text-sky-200">Search History</p>
          <div className="mt-4 space-y-2">
            {[...history, ...(savedRoadmaps.data ?? []).map((item) => item.careerName)].filter((item, index, list) => list.indexOf(item) === index).slice(0, 8).map((item) => (
              <button key={item} type="button" className="block w-full rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-left text-sm hover:bg-white/10" onClick={() => selectHistoryItem(item)}>
                {normalizeText(item)}
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
                <p className="text-sm font-medium text-slate-900">{normalizeText(item.careerName)}</p>
                <p className="text-xs text-slate-500">Saved snapshot APS {item.learnerAps} • Score {item.readinessScore}%</p>
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
                <p className="mt-1 text-xs text-slate-500">
                  Analysis source: {activeSource === 'PROFILE' ? 'My Profile' : 'Manually entered subjects'}
                  {activeSource === 'PROFILE' && activeAps.data?.resultSetLabel ? ` — ${activeAps.data.resultSetLabel}` : ''}
                  {activeSource === 'MANUAL' ? ` — ${countValidManualSubjects(manualSubjects)} valid subjects` : ''}
                </p>
                <p className="mt-1 text-xs text-slate-500">Result status: {activeResultSet.verificationStatus}</p>
              </div>
              <div className="flex flex-wrap items-center justify-end gap-2">
                <Button
                  type="button"
                  className="bg-slate-700 hover:bg-slate-600"
                  aria-label="Use verified profile results"
                  onClick={switchToProfile}
                >
                  Link to My Profile
                </Button>
                <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => {
                  switchToManual();
                  setManualSubjects((currentRows) => [...currentRows, createSubjectRow()]);
                }}>Add Subject</Button>
              </div>
            </div>
            {activeSource === 'PROFILE' && !(apsProfile.data?.subjects?.length) ? (
              <div className="border-b border-slate-200 px-4 py-3 text-sm text-amber-800">
                No verified academic results were found in your profile. Upload a report or enter your subjects manually. <button type="button" className="font-medium text-primary-600 hover:text-primary-500" onClick={() => navigate('/student/profile')}>Open My Profile</button>
              </div>
            ) : null}
            {profileRowsUnavailable ? (
              <div className="border-b border-slate-200 px-4 py-3 text-sm text-amber-800">
                Your profile subject results are incomplete for APS analysis. Add the missing marks in My Profile or switch to manual entry before generating a roadmap.
              </div>
            ) : null}
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
                    const normalized = normalizeManualRow(row);
                    const level = normalized?.level ?? calculateNscLevel(normalized?.markPercentage);
                    const aps = normalized?.apsPoints ?? level ?? '--';
                    return <tr key={row.id}>
                      <td className="px-4 py-3">
                        <input list="career-roadmap-subjects" className="w-full rounded-lg border border-slate-300 px-3 py-2" value={row.subjectName} onChange={(event) => {
                          switchToManual();
                          setManualSubjects((currentRows) => currentRows.map((item) => item.id === row.id ? { ...item, subjectName: event.target.value } : item));
                        }} placeholder="Subject name" disabled={activeSource === 'PROFILE'} />
                      </td>
                      <td className="px-4 py-3">
                        <Input type="number" min={0} max={100} value={row.markPercentage} onChange={(event) => {
                          switchToManual();
                          setManualSubjects((currentRows) => currentRows.map((item) => item.id === row.id ? { ...item, markPercentage: event.target.value } : item));
                        }} placeholder="" disabled={activeSource === 'PROFILE'} />
                      </td>
                      <td className="px-4 py-3">
                        <Input type="number" min={1} max={7} value={level == null ? '' : String(level)} readOnly placeholder="" />
                      </td>
                      <td className="px-4 py-3 text-slate-700">{normalized?.subjectName ? aps : '--'}</td>
                      <td className="px-4 py-3">
                        <button type="button" className="text-sm font-medium text-red-600 hover:text-red-500 disabled:cursor-not-allowed disabled:text-slate-400" onClick={() => {
                          switchToManual();
                          setManualSubjects((currentRows) => currentRows.filter((item) => item.id !== row.id));
                        }} disabled={activeSource === 'PROFILE'}>Remove</button>
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
              <p className="text-2xl font-semibold text-slate-900">{activeApsValue ?? '--'}</p>
              <p className="mt-1 text-xs text-slate-500">{activeApsStatus} • {activeAps.data?.calculationRule ?? 'General NSC APS preview'}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button type="button" onClick={() => generate.mutate()} disabled={generate.isPending || !careerName.trim() || !activeInputs.length || activeAps.data?.status === 'UNAVAILABLE'}>{generate.isPending ? 'Generating...' : 'Generate Roadmap'}</Button>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" disabled={!generated || analysisOutdated || saveRoadmap.isPending} onClick={() => saveRoadmap.mutate()}>{saveRoadmap.isPending ? 'Saving...' : 'Save Roadmap'}</Button>
              <Button type="button" className="bg-slate-700 hover:bg-slate-600" disabled={!generated || analysisOutdated} onClick={exportRoadmap}>Export PDF</Button>
              <Button
                type="button"
                className="bg-slate-700 hover:bg-slate-600"
                disabled={!generated || analysisOutdated || addToCareerPlan.isPending || !matchedCareer?.id}
                onClick={() => addToCareerPlan.mutate()}
                title={addToCareerPlanDisabledReason}
              >
                {addToCareerPlan.isPending ? 'Adding...' : 'Add to My Career Plan'}
              </Button>
            </div>
          </div>
          {analysisOutdated ? <p className="mt-3 text-sm text-amber-700">Your subject results changed. Regenerate the roadmap to refresh APS, eligibility, and study-plan analysis.</p> : null}
          {actionFeedback ? <p className={`mt-3 text-sm ${actionFeedback.type === 'success' ? 'text-emerald-700' : 'text-red-600'}`}>{actionFeedback.message}</p> : null}
          {!actionFeedback && addToCareerPlanDisabledReason && generated ? <p className="mt-3 text-sm text-slate-500">{addToCareerPlanDisabledReason}</p> : null}
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
                    <h2 className="text-lg font-semibold text-slate-900">{normalizeText(current.careerName)}</h2>
                    <p className="mt-2 text-sm text-slate-600">{normalizeText(current.overview.description) || 'Career overview is being refined.'}</p>
                  </div>
                  <Badge color={roadmapStatusColor(current.apsReadiness.status)}>{current.apsReadiness.status}</Badge>
                </div>
                <div className="mt-4 grid gap-4 lg:grid-cols-2">
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Daily responsibilities</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.dailyResponsibilities.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                    </ul>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Skills needed</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.skillsNeeded.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                    </ul>
                  </div>
                </div>
                <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-3">
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Career demand</p><p>{normalizeText(current.overview.careerDemand) || 'Verify current demand by region.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Salary range</p><p>{normalizeText(current.overview.salaryRange) || 'Varies by experience.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Professional body</p><p>{normalizeText(current.overview.professionalBody) || 'Check sector-specific registration.'}</p></div>
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
                      <p className="text-sm font-medium text-slate-900">{item.stage ? `${item.stage}. ` : ''}{normalizeText(item.title)}</p>
                      <p className="mt-1 text-sm text-slate-600">{normalizeText(item.description)}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div> : null}

            {current && activeTab === 'University Requirements' ? <div className="space-y-4">
              <div className="grid gap-3 lg:grid-cols-2">
                {current.universityRequirements.map((item) => {
                  const institution = resolveInstitutionDisplay({ name: item.institutionName, province: item.province, applicationUrl: item.applicationUrl, institutionType: item.institutionType });
                  return (
                  <article key={`${item.institutionName}-${item.qualificationName}`} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="flex items-start gap-3">
                        <InstitutionLogo src={institution.logoUrl} institutionName={institution.displayName} abbreviation={institution.abbreviation} size={56} className="rounded-2xl" />
                        <div>
                          <h3 className="text-sm font-semibold text-slate-900">{item.institutionName}</h3>
                          <p className="text-sm text-slate-600">{normalizeText(item.qualificationName)}</p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Badge color={item.verified ? 'emerald' : 'amber'}>{item.verificationBadge}</Badge>
                        <Badge color={roadmapStatusColor(item.requirementStatus)}>{item.requirementStatus}</Badge>
                      </div>
                    </div>
                    <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">{item.verified ? 'APS' : 'Estimated APS'}</p><p>{item.apsRequired ?? 'Verify'}{item.apsGap != null ? ` · Gap ${item.apsGap}` : ''}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Province</p><p>{item.province || 'South Africa'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Mathematics</p><p>{item.mathematicsRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">English</p><p>{item.englishRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Accounting</p><p>{item.accountingRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Duration</p><p>{item.duration || 'Verify with institution'}</p></div>
                    </div>
                    <p className="mt-3 text-sm text-slate-600">{normalizeText(item.notes || item.source) || 'Verify the full admission requirement directly with the institution.'}</p>
                    {item.applicationUrl ? <a className="mt-3 inline-block text-sm font-medium text-primary-600 hover:text-primary-500" href={item.applicationUrl} target="_blank" rel="noreferrer">Application link</a> : null}
                  </article>
                );
                })}
              </div>
            </div> : null}

            {current && activeTab === 'APS Readiness' ? <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Readiness summary</h3>
                  <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Current APS</p><p>{displayCurrentAps ?? 'Unavailable'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Required APS</p><p>{displayRequiredAps ?? 'APS requirement not verified'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS gap</p><p>{displayApsGap ?? 'Unavailable'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Risk level</p><p>{current.gapAnalysis.riskLevel}</p></div>
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Improvement suggestions</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {current.gapAnalysis.improvementSuggestions.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
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
                    {current.gapAnalysis.subjectsNeedingImprovement.length ? current.gapAnalysis.subjectsNeedingImprovement.map((item) => <li key={item}>• {normalizeText(item)}</li>) : <li className="text-slate-500">No urgent subject improvements flagged.</li>}
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
                  {current.alternativePathways.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-900">Best-fit universities</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-700">
                  {current.gapAnalysis.bestFitUniversities.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                </ul>
              </div>
            </div> : null}

            {current && activeTab === 'AI Study Plan' ? <div className="space-y-4">
              {current.studyPlan.map((item) => (
                <article key={item.title} className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">{normalizeText(item.title)}</h3>
                  <p className="mt-1 text-sm text-slate-600">{normalizeText(item.focus)}</p>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {item.actions.map((action) => <li key={action}>• {normalizeText(action)}</li>)}
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
                <Badge color={roadmapStatusColor(current.apsReadiness.status)}>{analysisOutdated ? 'Outdated' : activeApsStatus}</Badge>
              </div>
            </div>
            <div className="grid gap-3 text-sm text-slate-700">
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Learner APS</p><p className="mt-1 font-semibold text-slate-900">{displayCurrentAps ?? 'Unavailable'}</p></div>
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Required APS</p><p className="mt-1 font-semibold text-slate-900">{displayRequiredAps ?? 'APS requirement not verified'}</p></div>
              <div className="rounded-2xl border border-slate-200 p-3"><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS Gap</p><p className="mt-1 font-semibold text-slate-900">{displayApsGap ?? 'Unavailable'}</p></div>
            </div>
          </div> : <p className="mt-3 text-sm text-slate-500">Generate a roadmap to see eligibility, APS gap, and matched institutions.</p>}
        </div>
        <div className="rounded-3xl border border-slate-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-slate-900">University match snapshot</h2>
          <div className="mt-3 space-y-2">
            {(current?.universityRequirements ?? []).slice(0, 5).map((item) => {
              const institution = resolveInstitutionDisplay({ name: item.institutionName, province: item.province, institutionType: item.institutionType });
              return (
              <div key={`${item.institutionName}-${item.qualificationName}`} className="rounded-2xl border border-slate-200 p-3">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-start gap-3">
                    <InstitutionLogo src={institution.logoUrl} institutionName={institution.displayName} abbreviation={institution.abbreviation} size={44} className="rounded-xl" />
                    <div>
                      <p className="text-sm font-medium text-slate-900">{item.institutionName}</p>
                      <p className="text-xs text-slate-500">{item.qualificationName}</p>
                    </div>
                  </div>
                  <Badge color={roadmapStatusColor(item.requirementStatus)}>{item.requirementStatus}</Badge>
                </div>
              </div>
            );
            })}
            {!current?.universityRequirements?.length ? <p className="text-sm text-slate-500">No institution matches yet.</p> : null}
          </div>
        </div>
      </aside>
    </div>
  </Section>;
};

