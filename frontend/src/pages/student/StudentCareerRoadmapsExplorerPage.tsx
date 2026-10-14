import { StudyPlans } from '@/components/student/career/StudyPlans';
import { ProfileAssociationButton } from '@/components/student/profile/ProfileAssociations';
import { mergeManualResults, validateAcademicResults } from './academicResults';
import { useSubjectCatalogue } from '@/hooks/useSubjectCatalogue';
import { SubjectEditor } from '@/components/student/career/SubjectEditor';
import { useSubscriptionAccess, RequireEntitlement, AiUsageDisplay } from '@/features/subscriptions/access';
import { CareerRoadmapDetails } from '@/components/student/career/CareerRoadmapDetails';
import { ExploreWorkspace } from '@/components/student/career/ExploreWorkspace';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorState, LoadingState } from '@/components/feedback/States';
import { careerService } from '@/services/careerService';
import { useAppQuery } from '@/hooks/useAppQuery';
import { authoritativeAps, calculateApsGap, createSubjectRow, fingerprintCalculation, normalizeManualRow, type ResultSource, type SubjectRow, toSubjectRowsFromAps } from '@/pages/student/roadmapAps.utils';
import { featureModulesService } from '@/services/featureModulesService';
import { studentService } from '@/services/studentService';
import type {
  ApsCalculationResponse,
  ApsSubjectInput,
  Career,
  CareerRoadmapGenerateResponse,
  PaginatedResponse,
  SavedCareerRoadmap,
  StudentSubjectAchievement,
} from '@/types';

const grades = ['Grade 9', 'Grade 10', 'Grade 11', 'Grade 12'];
const provinces = ['Eastern Cape', 'Free State', 'Gauteng', 'KwaZulu-Natal', 'Limpopo', 'Mpumalanga', 'Northern Cape', 'North West', 'Western Cape'];

const tabs = ['Roadmap', 'University Requirements', 'APS Readiness', 'Subject Requirements', 'Alternative Pathways', 'AI Study Plan'] as const;

type FeedbackState = {
  type: 'success' | 'error';
  message: string;
};

const slugifyFilename = (value: string) => value
  .trim()
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-+|-+$/g, '') || 'career-roadmap';

export const StudentCareerRoadmapsExplorerPage = () => {
  const catalogue = useSubjectCatalogue();
  const subjectOptions = [...new Set(catalogue.data?.map(s => s.name) ?? [])];
  const [params, setParams] = useSearchParams();
  const queryClient = useQueryClient();
  const access = useSubscriptionAccess();
  const profile = useAppQuery({ queryKey: ['me'], queryFn: studentService.getMe });
  const apsProfile = useAppQuery({ queryKey: ['student-aps-profile'], queryFn: featureModulesService.apsProfile });
  const savedRoadmaps = useAppQuery({ queryKey: ['student-career-roadmaps-saved', access.data?.plan], enabled: Boolean(access.data?.entitlements.includes('CAREER_ROADMAP_PERSONALISED')), queryFn: featureModulesService.savedCareerRoadmaps });
  const [actionFeedback, setActionFeedback] = useState<FeedbackState | null>(null);

  const selection = useAppQuery({ queryKey: ['selected-career'], queryFn: featureModulesService.selectedCareer });
  useEffect(() => { if (!params.get('career') && selection.data?.careerName) setParams(previous => { const next = new URLSearchParams(previous); next.set('career', selection.data.careerName); return next; }, { replace: true }); }, [selection.data]);
  const [careerName, setCareerName] = useState(params.get('career') || '');
  const requestedCareer = useRef(careerName);
  useEffect(() => { const name = params.get('career') || ''; setCareerName(name); setGenerated(null); setSavedSnapshot(false); }, [params.get('career')]);
  const [grade, setGrade] = useState('');
  const [province, setProvince] = useState('');
  const [activeSource, setActiveSource] = useState<ResultSource>('PROFILE');
  const [manualSubjects, setManualSubjects] = useState<SubjectRow[]>([
    createSubjectRow(),
    createSubjectRow(),
    createSubjectRow(),
  ]);
  const [profileSubjects, setProfileSubjects] = useState<SubjectRow[]>([]);
  const [activeTab, setActiveTab] = useState<(typeof tabs)[number]>('Roadmap');
  const section = params.get('section');
  useEffect(() => { setActiveTab(section === 'learning-path' || section === 'study-plan' ? 'AI Study Plan' : section === 'readiness' ? 'APS Readiness' : 'Roadmap'); }, [section]);
  const [generated, setGenerated] = useState<CareerRoadmapGenerateResponse | null>(null);
  useEffect(() => { setGenerated(null); }, [access.data?.plan]);
  const [generatedFingerprint, setGeneratedFingerprint] = useState('');
  const [savedSnapshot, setSavedSnapshot] = useState(false);
  const [programmeId, setProgrammeId] = useState(params.get('programme') || '');
  useEffect(() => {
    if (selection.data?.careerName === careerName) setProgrammeId(params.get('programme') || selection.data.programmeId || '');
  }, [selection.data, careerName, params.get('programme')]);
  const chooseProgramme = useMutation({
    mutationFn: featureModulesService.selectProgramme,
    onSuccess: data => { queryClient.setQueryData(['selected-career'], data); setProgrammeId(data.programmeId || ''); setActionFeedback({ type: 'success', message: 'Programme target saved.' }); },
    onError: error => setActionFeedback({ type: 'error', message: error.message }),
  });
  const [history, setHistory] = useState<string[]>([]);

  useEffect(() => {
    if (!profile.data && !apsProfile.data) return;
    setGrade(profile.data?.selectedGrade || apsProfile.data?.grade || '');
    setProvince((current) => current || apsProfile.data?.province || '');
    setProfileSubjects(toSubjectRowsFromAps(apsProfile.data?.subjects));
      if (activeSource === 'PROFILE') setGenerated(null);
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
    queryFn: () => featureModulesService.calculateAps({ grade: grade || undefined, province: province || undefined, subjects: activeInputs }),
  });
  const [academicReview, setAcademicReview] = useState<{ baseline: StudentSubjectAchievement[]; subjects: StudentSubjectAchievement[] } | null>(null);
  useEffect(() => { setAcademicReview(null); }, [manualFingerprint]);
  const reviewAcademicSave = useMutation({
    mutationFn: async () => {
      const rows = manualSubjects.map(row => ({ subjectName: row.subjectName.trim(), markPercentage: row.markPercentage === '' ? null : Number(row.markPercentage), achievementLevel: normalizeManualRow(row)?.level ?? null }));
      validateAcademicResults(rows);
      const incoming = rows.filter(row => row.subjectName);
      if (!incoming.length) throw new Error('Enter at least one subject and mark.');
      const latest = await studentService.getMe();
      const baseline = latest.subjectAchievements ?? [];
      return { baseline, subjects: mergeManualResults(baseline, incoming) };
    },
    onSuccess: setAcademicReview,
    onError: (error) => setActionFeedback({ type: 'error', message: error.message }),
  });
  const saveAcademic = useMutation({
    mutationFn: () => {
      if (!academicReview) throw new Error('Review your marks before saving.');
      return studentService.updateMe({ selectedGrade: grade || undefined, subjectAchievements: academicReview.subjects, expectedSubjectAchievements: academicReview.baseline });
    },
    onSuccess: (saved) => {
      queryClient.setQueryData(['me'], saved);
      for (const key of ['student-aps-profile', 'dash', 'recs', 'profile-ai-readiness', 'ai-guidance-university-sources', 'profile-ai-recommendations-fallback']) queryClient.invalidateQueries({ queryKey: [key] });
      setAcademicReview(null);
      setActiveSource('PROFILE');
      setActionFeedback({ type: 'success', message: 'Academic results saved to your profile.' });
    },
    onError: (error) => setActionFeedback({ type: 'error', message: error.message }),
  });
  const activeAps = activeSource === 'PROFILE' ? apsProfile : manualAps;
  const current = generated;
  const careerLookup = useAppQuery<PaginatedResponse<Career> | Career[]>({
    queryKey: ['career-roadmap-career-match', careerName],
    enabled: Boolean(careerName.trim()),
    queryFn: () => careerService.list({ q: careerName, size: 25 }),
  });
  const careerOptions = useMemo(() => Array.isArray(careerLookup.data) ? careerLookup.data : careerLookup.data?.content ?? [], [careerLookup.data]);
  const matchedCareer = useMemo(() => {
    if (!careerName) return null;
    const normalizedCareerName = careerName.trim().toLowerCase();
    return careerOptions.find((item) => item.title?.trim().toLowerCase() === normalizedCareerName)
           ?? null;
  }, [careerOptions, careerName]);

  const generate = useMutation({
    mutationFn: (request: { payload: Parameters<typeof featureModulesService.generateCareerRoadmap>[0]; fingerprint: string }) => featureModulesService.generateCareerRoadmap(request.payload),
    onSettled: () => { queryClient.invalidateQueries({ queryKey: ['subscription-access'] }); },
    onSuccess: (data, request) => {
      if (requestedCareer.current !== request.payload.careerName) return;
      setSavedSnapshot(false);
      setCareerName(data.careerName);
      setGenerated(data);
      const resultFingerprint = request.fingerprint.split('|');
      resultFingerprint[2] = data.careerName.trim().toLowerCase();
      setGeneratedFingerprint(resultFingerprint.join('|'));
      setActionFeedback(null);
      setHistory((current) => [data.careerName, ...current.filter((item) => item !== data.careerName)].slice(0, 8));
    },
    onError: (error) => setActionFeedback({ type: 'error', message: (error as Error).message || 'Could not generate this roadmap right now.' }),
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
      await queryClient.invalidateQueries({ queryKey: ['explore-saved-careers'] });
    },
    onError: (error) => {
      setActionFeedback({ type: 'error', message: (error as Error).message || 'Could not add this career to your plan right now.' });
    },
  });

  const selectSaved = (item: SavedCareerRoadmap) => {
    requestedCareer.current = item.careerName;
    setProgrammeId(params.get('programme') || '');
    setSavedSnapshot(true);
    setCareerName(item.careerName);
    setGenerated(item.roadmap);
    const savedFingerprint = activeFingerprint.split('|');
    savedFingerprint[2] = item.careerName.trim().toLowerCase();
    setGeneratedFingerprint(savedFingerprint.join('|'));
    setActiveTab(section === 'learning-path' || section === 'study-plan' ? 'AI Study Plan' : section === 'readiness' ? 'APS Readiness' : 'Roadmap');
    setActionFeedback({ type: 'success', message: 'Viewing a saved snapshot. Regenerate to check current results.' });
  };

  const selectCareer = useMutation({
    mutationFn: featureModulesService.selectCareer,
    onSuccess: data => {
      requestedCareer.current = data.careerName;
      setProgrammeId('');
      queryClient.setQueryData(['selected-career'], data); selectHistoryItem(data.careerName);
      if (!(savedRoadmaps.data ?? []).some(item => item.careerName === data.careerName)) generate.mutate({ payload: { careerName: data.careerName, grade: grade || undefined, province: province || undefined, subjects: activeInputs }, fingerprint: activeFingerprint });
    },
    onError: error => setActionFeedback({ type: 'error', message: error.message }),
  });
  const clearSelection = useMutation({
    mutationFn: featureModulesService.clearSelectedCareer,
    onSuccess: data => {
      requestedCareer.current = '';
      queryClient.setQueryData(['selected-career'], data);
      setCareerName(''); setGenerated(null); setProgrammeId(''); setSavedSnapshot(false);
      setParams(previous => { const next = new URLSearchParams(previous); next.delete('career'); return next; }, { replace: true });
    },
    onError: error => setActionFeedback({ type: 'error', message: error.message }),
  });
  const selectHistoryItem = (item: string) => {
    const saved = (savedRoadmaps.data ?? []).find((roadmap) => roadmap.careerName === item);
    if (saved) {
      selectSaved(saved);
      return;
    }
    setSavedSnapshot(false);
    setHistory(previous => [item,...previous.filter(name => name !== item)].slice(0,8));
    setParams(previous => { const next = new URLSearchParams(previous); next.set('career', item); return next; });
    setCareerName(item);
    setGenerated(null);
    setActiveTab(section === 'learning-path' || section === 'study-plan' ? 'AI Study Plan' : section === 'readiness' ? 'APS Readiness' : 'Roadmap');
    setActionFeedback(null);
  };

  useEffect(() => {
    if (!generated && !generate.isPending && savedRoadmaps.data && careerName) {
      const saved = savedRoadmaps.data.find(item => item.careerName === careerName);
      if (saved) selectSaved(saved);
    }
  }, [savedRoadmaps.data, careerName, profile.data, apsProfile.data]);

  const exportRoadmap = () => {
    if (!current) return;
    const previousTitle = document.title;
    document.title = `${slugifyFilename(current.careerName)}-career-roadmap`;
    window.print();
    window.setTimeout(() => {
      document.title = previousTitle;
    }, 1000);
  };

  if (profile.isLoading || apsProfile.isLoading) return <LoadingState message="Loading Career Explorer..." />;
  if (profile.isError) return <><ErrorState message="Could not load your Career Explorer profile." /><Button onClick={() => profile.refetch()}>Retry</Button></>;

  const switchToManual = () => {
    if (activeSource === 'PROFILE') setManualSubjects(profileSubjects.length ? profileSubjects : [createSubjectRow()]);
    setActiveSource('MANUAL');
    setActionFeedback(null);
  };

  const switchToProfile = () => {
    if (!(apsProfile.data?.subjects?.length)) {
      setActionFeedback({ type: 'error', message: 'No saved academic results were found in your profile. Upload a report or enter your subjects manually.' });
      return;
    }
    setProfileSubjects(toSubjectRowsFromAps(apsProfile.data.subjects));
    setActiveSource('PROFILE');
    setActionFeedback(null);
  };

  const activeApsValue = activeAps.data?.status === 'UNAVAILABLE' ? null : authoritativeAps(activeAps.data);
  const activeApsStatus = activeAps.data?.status ?? 'UNAVAILABLE';
  const analysisOutdated = Boolean(generated && generatedFingerprint && generatedFingerprint !== activeFingerprint && !generatedFingerprint.startsWith('saved:'));
  const canUseActiveAnalysis = activeApsStatus !== 'UNAVAILABLE' && subjects.length > 0;
  const programme = current?.universityRequirements.find(row => row.id === programmeId);
  const displayRequiredAps = programme?.verified ? programme.apsRequired ?? null : null;
  const displayCurrentAps = programme ? programme.learnerAps ?? null : canUseActiveAnalysis ? activeApsValue : null;
  const displayApsGap = programme?.apsGap ?? null;
  return <ExploreWorkspace academicSubjects={activeInputs} profile={profile.data} careerName={careerName} onSelect={name => { requestedCareer.current = name; selectCareer.mutate(name); }} onClear={() => clearSelection.mutate()} selectionPending={selectCareer.isPending || clearSelection.isPending || generate.isPending}
    savedSnapshot={savedSnapshot} roadmap={current} aps={programme ? programme.learnerAps ?? null : activeApsValue} requiredAps={displayRequiredAps} gap={displayApsGap}
    saved={savedRoadmaps.data ?? []} savedLoading={savedRoadmaps.isLoading} retrySaved={() => savedRoadmaps.refetch()} savedError={savedRoadmaps.isError} onSaved={selectSaved}
    history={history} clearHistory={() => setHistory([])} outdated={analysisOutdated}
    actions={<><label>Programme admission target<select className="rounded-lg border p-2" value={programmeId} disabled={chooseProgramme.isPending} onChange={event => { const id = event.target.value; chooseProgramme.mutate(id); setParams(previous => { const next = new URLSearchParams(previous); next.delete('programme'); return next; }, { replace: true }); }}><option value="">Choose a programme</option>{current?.universityRequirements.filter(row => row.id).map(row => <option key={row.id} value={row.id!}>{row.institutionName} - {row.qualificationName}</option>)}</select></label>{programme && <p>Programme learner APS: {programme.learnerAps ?? 'Unavailable'}; required / target APS: {displayRequiredAps ?? 'Unavailable'}. {programme.apsRule ? `Rule: ${programme.apsRule}` : 'Institution scoring rule is not available; your general APS cannot establish eligibility.'}</p>}{matchedCareer?.id && <ProfileAssociationButton type="CAREER" targetId={matchedCareer.id} />}<RequireEntitlement feature="CAREER_ROADMAP_PERSONALISED"><AiUsageDisplay /><Button onClick={() => generate.mutate({ payload: { careerName, grade: grade || undefined, province: province || undefined, subjects: activeInputs }, fingerprint: activeFingerprint })} disabled={generate.isPending || !matchedCareer || !activeInputs.length || activeAps.isFetching || activeAps.isError || activeAps.data?.status === 'UNAVAILABLE' || activeInputs.some(subject => subject.markPercentage != null && (subject.markPercentage < 0 || subject.markPercentage > 100))}>{generate.isPending ? 'Generating...' : 'Generate Roadmap'}</Button><Button onClick={() => { setActiveTab('AI Study Plan'); setParams(previous => { const next = new URLSearchParams(previous); next.set('section', 'study-plan'); return next; }); }}>Generate / View Study Plan</Button><Button disabled={!generated || analysisOutdated || saveRoadmap.isPending} onClick={() => saveRoadmap.mutate()}>Save Roadmap</Button><Button disabled={!matchedCareer || addToCareerPlan.isPending} onClick={() => addToCareerPlan.mutate()}>Save Career</Button>{current && <Button disabled={analysisOutdated} onClick={exportRoadmap}>Export PDF</Button>}</RequireEntitlement></>}
    feedback={actionFeedback?.message || (activeAps.isError ? 'Academic results could not load. Open your academic profile to retry or enter subjects manually.' : undefined)}
    editor={<SubjectEditor disabled={reviewAcademicSave.isPending || saveAcademic.isPending} saveControls={activeSource === 'MANUAL' && <section aria-label="Save academic results">
      <Button disabled={reviewAcademicSave.isPending || saveAcademic.isPending} onClick={() => reviewAcademicSave.mutate()}>{reviewAcademicSave.isPending ? 'Loading saved results...' : 'Review and save marks'}</Button>
      {academicReview && <div><p>Confirm these results for your academic profile{grade ? ` (${grade})` : ''}. Changed marks replace the saved values shown below.</p>
        <ul>{academicReview.subjects.map(row => { const before = academicReview.baseline.find(item => item.subjectName === row.subjectName); return <li key={row.subjectName}>{row.subjectName}: {before ? `${before.markPercentage != null ? `${before.markPercentage}%` : `Level ${before.achievementLevel}`} -> ` : 'New: '}{row.markPercentage != null ? `${row.markPercentage}%` : `Level ${row.achievementLevel}`}</li>; })}</ul>
        <Button disabled={saveAcademic.isPending} onClick={() => saveAcademic.mutate()}>{saveAcademic.isPending ? 'Saving academic results...' : 'Confirm and save to profile'}</Button>
        <Button disabled={saveAcademic.isPending} onClick={() => setAcademicReview(null)}>Cancel</Button>
      </div>}
    </section>} subjects={subjects} source={activeSource} grade={grade} province={province} grades={grades} provinces={provinces} subjectOptions={subjectOptions}
      onGrade={setGrade} onProvince={setProvince} onLink={switchToProfile} onManual={switchToManual}
      onChange={(id,field,value) => setManualSubjects(rows => rows.map(row => row.id === id ? {...row,[field]:value,level:undefined,apsPoints:undefined,included:undefined,exclusionReason:undefined} : row))}
      onRemove={id => setManualSubjects(rows => rows.filter(row => row.id !== id))}
      onAdd={() => { setManualSubjects([...subjects,createSubjectRow()]); setActiveSource('MANUAL'); }}
      aps={activeApsValue} loading={activeAps.isFetching} error={activeAps.isError} onRetry={() => activeAps.refetch()} feedback={actionFeedback?.message}/>}
    printContent={current && <div className="ex-print-roadmap"><h1>{current.careerName}</h1>{savedSnapshot && <p>Saved roadmap snapshot</p>}{tabs.map(tab => <section key={tab}><h2>{tab}</h2><CareerRoadmapDetails current={current} activeTab={tab} setActiveTab={() => undefined} displayCurrentAps={current.apsReadiness.learnerAps} displayRequiredAps={current.apsReadiness.requiredAps} displayApsGap={current.apsReadiness.apsGap}/></section>)}</div>}
    details={<RequireEntitlement feature={activeTab === 'AI Study Plan' ? 'CAREER_ROADMAP_ADVANCED' : 'CAREER_ROADMAP_PERSONALISED'}>{activeTab === 'AI Study Plan' ? <><Button onClick={() => setActiveTab('Roadmap')}>View roadmap</Button><StudyPlans careerName={careerName} programmeId={programmeId || undefined}/></> : <CareerRoadmapDetails current={current} activeTab={activeTab} setActiveTab={setActiveTab} displayCurrentAps={displayCurrentAps} displayRequiredAps={displayRequiredAps} displayApsGap={displayApsGap} />}</RequireEntitlement>}
  />;
};
