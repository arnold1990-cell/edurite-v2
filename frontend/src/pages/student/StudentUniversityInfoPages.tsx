import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ExternalLink, RefreshCcw } from 'lucide-react';
import { EmptyState, ErrorState } from '@/components/feedback/States';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useAppQuery } from '@/hooks/useAppQuery';
import { useAuth } from '@/hooks/useAuth';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { universityInfoService, type UniversityAdmissionRequirementsView, type UniversityProgrammesView } from '@/services/universityInfoService';

const UniversityInfoSkeleton = () => (
  <section className="student-page-section">
    <div className="animate-pulse space-y-4">
      <div className="h-8 w-52 rounded-xl bg-slate-200" />
      <div className="rounded-3xl border border-slate-200 bg-white p-5">
        <div className="flex gap-4">
          <div className="h-[72px] w-[72px] rounded-2xl bg-slate-200" />
          <div className="flex-1 space-y-3">
            <div className="h-6 w-64 rounded bg-slate-200" />
            <div className="h-4 w-40 rounded bg-slate-100" />
            <div className="h-4 w-full rounded bg-slate-100" />
          </div>
        </div>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="rounded-2xl border border-slate-200 bg-white p-4">
            <div className="h-5 w-52 rounded bg-slate-200" />
            <div className="mt-3 h-4 w-36 rounded bg-slate-100" />
            <div className="mt-2 h-4 w-24 rounded bg-slate-100" />
            <div className="mt-4 h-10 w-40 rounded-xl bg-slate-200" />
          </div>
        ))}
      </div>
    </div>
  </section>
);

const formatDate = (value?: string | null) => {
  if (!value) return 'Not available';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString();
};

const statusLabel = (view: { cachedData: boolean; fallbackOnly: boolean; retrievalStatus: string }) => {
  if (view.fallbackOnly) return 'Official website fallback';
  if (view.cachedData) return 'Cached verified data';
  return view.retrievalStatus.replace(/_/g, ' ');
};

const SourceAudit = ({ logs }: { logs: UniversityProgrammesView['retrievalLogs'] | UniversityAdmissionRequirementsView['retrievalLogs'] }) => {
  if (!logs.length) return null;
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <h3 className="text-sm font-semibold text-slate-900">Latest retrieval activity</h3>
      <div className="mt-3 space-y-2 text-sm text-slate-600">
        {logs.slice(0, 5).map((log) => (
          <div key={log.id} className="rounded-xl border border-slate-200 bg-white p-3">
            <p className="font-medium text-slate-900">{log.status}</p>
            <p className="mt-1">{log.message || 'No additional retrieval detail was recorded.'}</p>
            {log.sourceUrl ? <a href={log.sourceUrl} target="_blank" rel="noreferrer" className="mt-2 inline-flex text-xs font-medium text-blue-700 hover:text-blue-600">{log.sourceUrl}</a> : null}
          </div>
        ))}
      </div>
    </div>
  );
};

const HeaderCard = ({
  institution,
  description,
  status,
  lastUpdatedAt,
  onRefresh,
  refreshPending,
  canRefresh,
}: {
  institution: UniversityProgrammesView['institution'] | UniversityAdmissionRequirementsView['institution'];
  description: string;
  status: string;
  lastUpdatedAt?: string | null;
  onRefresh: () => void;
  refreshPending: boolean;
  canRefresh: boolean;
}) => {
  const visual = resolveInstitutionDisplay({
    name: institution.name,
    website: institution.officialWebsite ?? undefined,
  });

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)]">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <InstitutionLogo
          src={visual.logoUrl}
          institutionName={visual.displayName}
          abbreviation={visual.abbreviation}
          size={72}
          className="rounded-2xl"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-600">{status}</span>
            {lastUpdatedAt ? <span className="text-xs text-slate-500">Last updated {formatDate(lastUpdatedAt)}</span> : null}
          </div>
          <h1 className="mt-3 text-2xl font-bold text-slate-900">{institution.name}</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">{description}</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {institution.officialWebsite ? (
              <a href={institution.officialWebsite} target="_blank" rel="noreferrer">
                <Button type="button" className="bg-slate-900 hover:bg-slate-800">
                  <ExternalLink size={15} className="mr-2" />
                  Visit Official Website
                </Button>
              </a>
            ) : null}
            {institution.officialProgrammesUrl ? (
              <a href={institution.officialProgrammesUrl} target="_blank" rel="noreferrer">
                <Button type="button" className="bg-blue-600 hover:bg-blue-500">Open Programmes Page</Button>
              </a>
            ) : null}
            {institution.officialAdmissionsUrl ? (
              <a href={institution.officialAdmissionsUrl} target="_blank" rel="noreferrer">
                <Button type="button" className="bg-slate-700 hover:bg-slate-600">Open Admissions Page</Button>
              </a>
            ) : null}
            {canRefresh ? (
              <Button type="button" onClick={onRefresh} disabled={refreshPending} className="bg-emerald-600 hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-70">
                <RefreshCcw size={15} className="mr-2" />
                {refreshPending ? 'Refreshing...' : 'Refresh University Data'}
              </Button>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
};

export const StudentUniversityProgrammesPage = () => {
  const { slug = '' } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [facultyFilter, setFacultyFilter] = useState('ALL');
  const [qualificationFilter, setQualificationFilter] = useState('ALL');
  const [studyModeFilter, setStudyModeFilter] = useState('ALL');

  const query = useAppQuery<UniversityProgrammesView>({
    queryKey: ['student', 'universities', slug, 'programmes'],
    queryFn: () => universityInfoService.programmes(slug),
    enabled: Boolean(slug),
  });

  const refreshMutation = useMutation({
    mutationFn: () => universityInfoService.refresh(slug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student', 'universities', slug, 'programmes'] });
      queryClient.invalidateQueries({ queryKey: ['student', 'universities', slug, 'admission-requirements'] });
    },
  });

  const filteredProgrammes = useMemo(() => {
    const programmes = query.data?.programmes ?? [];
    const normalizedSearch = search.trim().toLowerCase();
    return programmes.filter((programme) => {
      const matchesSearch = !normalizedSearch
        || programme.name.toLowerCase().includes(normalizedSearch)
        || (programme.faculty ?? '').toLowerCase().includes(normalizedSearch)
        || (programme.qualificationType ?? '').toLowerCase().includes(normalizedSearch);
      const matchesFaculty = facultyFilter === 'ALL' || programme.faculty === facultyFilter;
      const matchesQualification = qualificationFilter === 'ALL' || programme.qualificationType === qualificationFilter;
      const matchesStudyMode = studyModeFilter === 'ALL' || programme.studyMode === studyModeFilter;
      return matchesSearch && matchesFaculty && matchesQualification && matchesStudyMode;
    });
  }, [facultyFilter, qualificationFilter, query.data?.programmes, search, studyModeFilter]);

  const canRefresh = Boolean(user?.roles?.includes('ROLE_ADMIN'));

  if (query.isLoading) return <UniversityInfoSkeleton />;
  if (query.isError || !query.data) return <ErrorState message="Could not load university programmes right now." />;

  return (
    <section className="student-page-section">
      <div className="mb-4">
        <Link to="/student/universities" className="inline-flex items-center text-sm font-medium text-slate-600 hover:text-slate-900">
          <ArrowLeft size={16} className="mr-2" />
          Back to Universities
        </Link>
      </div>

      <HeaderCard
        institution={query.data.institution}
        description={query.data.message}
        status={statusLabel(query.data)}
        lastUpdatedAt={query.data.lastUpdatedAt}
        onRefresh={() => refreshMutation.mutate()}
        refreshPending={refreshMutation.isPending}
        canRefresh={canRefresh}
      />

      {query.data.programmes.length > 0 ? (
        <>
          <div className="rounded-2xl border border-slate-200 bg-white p-4">
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              <Input placeholder="Search programmes or faculty" value={search} onChange={(event) => setSearch(event.target.value)} />
              <select value={facultyFilter} onChange={(event) => setFacultyFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100">
                <option value="ALL">All faculties</option>
                {query.data.availableFaculties.map((faculty) => <option key={faculty} value={faculty}>{faculty}</option>)}
              </select>
              <select value={qualificationFilter} onChange={(event) => setQualificationFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100">
                <option value="ALL">All qualifications</option>
                {query.data.availableQualificationTypes.map((qualification) => <option key={qualification} value={qualification}>{qualification}</option>)}
              </select>
              <select value={studyModeFilter} onChange={(event) => setStudyModeFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100">
                <option value="ALL">All study modes</option>
                {query.data.availableStudyModes.map((mode) => <option key={mode} value={mode}>{mode}</option>)}
              </select>
            </div>
          </div>

          {filteredProgrammes.length === 0 ? (
            <EmptyState title="No programmes match these filters" message="Try clearing one of the filters or searching with a broader term." />
          ) : (
            <div className="grid gap-4 lg:grid-cols-2">
              {filteredProgrammes.map((programme) => (
                <article key={programme.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)]">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h2 className="text-lg font-semibold text-slate-900">{programme.name}</h2>
                      <p className="mt-1 text-sm text-slate-500">{programme.qualificationType || 'Qualification type not available'}</p>
                    </div>
                    <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">{programme.faculty || 'Faculty not available'}</span>
                  </div>
                  <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
                    <p><span className="font-medium text-slate-900">Duration:</span> {programme.duration || 'Not available from the official source.'}</p>
                    <p><span className="font-medium text-slate-900">Study mode:</span> {programme.studyMode || 'Not available from the official source.'}</p>
                    <p><span className="font-medium text-slate-900">Campus:</span> {programme.campus || 'Not available from the official source.'}</p>
                    <p><span className="font-medium text-slate-900">Last verified:</span> {formatDate(programme.lastVerifiedAt)}</p>
                  </div>
                  <p className="mt-3 text-xs font-medium uppercase tracking-[0.16em] text-slate-400">{programme.sourceLabel || 'Official source'}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <a href={programme.programmeUrl || programme.sourceUrl} target="_blank" rel="noreferrer">
                      <Button type="button" className="bg-slate-900 hover:bg-slate-800">
                        <ExternalLink size={15} className="mr-2" />
                        View Official Programme
                      </Button>
                    </a>
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="space-y-4">
          <EmptyState title="Verified programmes are not available right now" message={query.data.message} />
          <div className="flex flex-wrap gap-2">
            <Button type="button" className="bg-slate-900 hover:bg-slate-800" onClick={() => query.refetch()}>Retry</Button>
            {query.data.institution.officialWebsite ? (
              <a href={query.data.institution.officialWebsite} target="_blank" rel="noreferrer">
                <Button type="button" className="bg-blue-600 hover:bg-blue-500">Visit Official Website</Button>
              </a>
            ) : null}
          </div>
        </div>
      )}

      <SourceAudit logs={query.data.retrievalLogs} />
    </section>
  );
};

export const StudentUniversityAdmissionRequirementsPage = () => {
  const { slug = '' } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const query = useAppQuery<UniversityAdmissionRequirementsView>({
    queryKey: ['student', 'universities', slug, 'admission-requirements'],
    queryFn: () => universityInfoService.admissionRequirements(slug),
    enabled: Boolean(slug),
  });

  const refreshMutation = useMutation({
    mutationFn: () => universityInfoService.refresh(slug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student', 'universities', slug, 'programmes'] });
      queryClient.invalidateQueries({ queryKey: ['student', 'universities', slug, 'admission-requirements'] });
    },
  });

  const canRefresh = Boolean(user?.roles?.includes('ROLE_ADMIN'));

  if (query.isLoading) return <UniversityInfoSkeleton />;
  if (query.isError || !query.data) return <ErrorState message="Could not load university admission requirements right now." />;

  return (
    <section className="student-page-section">
      <div className="mb-4">
        <Link to="/student/universities" className="inline-flex items-center text-sm font-medium text-slate-600 hover:text-slate-900">
          <ArrowLeft size={16} className="mr-2" />
          Back to Universities
        </Link>
      </div>

      <HeaderCard
        institution={query.data.institution}
        description={query.data.message}
        status={statusLabel(query.data)}
        lastUpdatedAt={query.data.lastUpdatedAt}
        onRefresh={() => refreshMutation.mutate()}
        refreshPending={refreshMutation.isPending}
        canRefresh={canRefresh}
      />

      {query.data.requirements.length > 0 ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {query.data.requirements.map((requirement) => (
            <article key={requirement.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)]">
              <h2 className="text-lg font-semibold text-slate-900">{requirement.requirementTitle || 'Admission requirement'}</h2>
              <p className="mt-1 text-sm text-slate-500">{requirement.programmeName && requirement.programmeName !== 'General' ? requirement.programmeName : 'General admission requirement'}</p>
              <div className="mt-4 space-y-3 text-sm text-slate-600">
                <p><span className="font-medium text-slate-900">Minimum APS:</span> {requirement.apsMinimum ?? 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">Required subjects:</span> {requirement.requiredSubjects.length ? requirement.requiredSubjects.join(', ') : 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">NSC requirement:</span> {requirement.nscRequirement || 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">Language requirement:</span> {requirement.languageRequirement || 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">Faculty-specific requirement:</span> {requirement.facultySpecificRequirement || 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">International applicants:</span> {requirement.internationalRequirement || 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">Additional tests:</span> {requirement.additionalTests || 'Not available from the official source.'}</p>
                <p><span className="font-medium text-slate-900">Last verified:</span> {formatDate(requirement.lastVerifiedAt)}</p>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <a href={requirement.sourceUrl} target="_blank" rel="noreferrer">
                  <Button type="button" className="bg-slate-900 hover:bg-slate-800">
                    <ExternalLink size={15} className="mr-2" />
                    Open Official Source
                  </Button>
                </a>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          <EmptyState title="Verified admission requirements are not available right now" message={query.data.message} />
          <div className="flex flex-wrap gap-2">
            <Button type="button" className="bg-slate-900 hover:bg-slate-800" onClick={() => query.refetch()}>Retry</Button>
            {query.data.institution.officialWebsite ? (
              <a href={query.data.institution.officialWebsite} target="_blank" rel="noreferrer">
                <Button type="button" className="bg-blue-600 hover:bg-blue-500">Visit Official Website</Button>
              </a>
            ) : null}
          </div>
        </div>
      )}

      <SourceAudit logs={query.data.retrievalLogs} />
    </section>
  );
};
