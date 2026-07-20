import { useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { BookOpen, ClipboardCheck, Eye, Download, FileText, Search } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { useAppQuery } from '@/hooks/useAppQuery';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { schoolService, type CurriculumAsset, type CurriculumResourceFilters, type TeacherCoverageItem, type TeacherLessonPlanResponse } from '@/services/schoolService';
import { AdminCard, AdminMetricCard, AdminPageHeader, AdminPageLayout } from '@/components/school/admin/AdminUi';

type SchoolTab = 'district-atps' | 'syllabuses' | 'lesson-plans' | 'school-uploads';
type TeacherTab = 'district-atps' | 'syllabuses' | 'lesson-plans' | 'weekly-atp';

const FILTER_OPTIONS = {
  types: ['ATP', 'SYLLABUS', 'LESSON_PLAN'],
  terms: ['Term 1', 'Term 2', 'Term 3', 'Term 4'],
};

const emptyMessage = 'No curriculum resources are available yet. Once the District uploads ATPs, syllabuses, or lesson plans, they will appear here.';

const normalizeType = (value?: string | null) => (value ?? '').trim().toUpperCase();

const formatTypeLabel = (value?: string | null) => {
  const normalized = normalizeType(value);
  if (normalized === 'LESSON_PLAN') return 'Lesson Plan';
  if (normalized === 'SYLLABUS') return 'Syllabus';
  return normalized || 'Resource';
};

const formatDate = (value?: string | null) => {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
};

const openBlob = (blob: Blob) => {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener,noreferrer');
  window.setTimeout(() => URL.revokeObjectURL(url), 60000);
};

const saveBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
};

const typeMatchesTab = (asset: CurriculumAsset, tab: SchoolTab | TeacherTab) => {
  if (tab === 'school-uploads') return (asset.source ?? asset.badge).toUpperCase().includes('SCHOOL');
  if (tab === 'weekly-atp') return normalizeType(asset.repositoryType) === 'ATP';
  if (tab === 'district-atps') return normalizeType(asset.repositoryType) === 'ATP';
  if (tab === 'syllabuses') return normalizeType(asset.repositoryType) === 'SYLLABUS';
  return normalizeType(asset.repositoryType) === 'LESSON_PLAN';
};

const applyClientFilters = (asset: CurriculumAsset, search: string, filters: CurriculumResourceFilters) => {
  const keyword = search.trim().toLowerCase();
  if (keyword) {
    const haystack = [
      asset.title,
      asset.subject,
      asset.grade,
      asset.curriculumPhase,
      asset.term,
      asset.province,
      asset.uploadedBy,
      asset.versionNumber,
    ].join(' ').toLowerCase();
    if (!haystack.includes(keyword)) {
      return false;
    }
  }
  if (filters.subject && asset.subject !== filters.subject) return false;
  if (filters.grade && asset.grade !== filters.grade) return false;
  if (filters.phase && (asset.curriculumPhase ?? '') !== filters.phase) return false;
  if (filters.term && (asset.term ?? '') !== filters.term) return false;
  if (filters.week && asset.weekNumber !== filters.week) return false;
  if (filters.academicYear && asset.academicYear !== filters.academicYear) return false;
  if (filters.type && normalizeType(asset.repositoryType) !== filters.type) return false;
  return true;
};

const ResourceActions = ({
  asset,
  onView,
  onDownload,
}: {
  asset: CurriculumAsset;
  onView: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') => void;
  onDownload: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') => void;
}) => (
  <div className="flex flex-wrap gap-2">
    <Button type="button" className="h-9 rounded-xl bg-slate-100 px-3 text-xs text-slate-900 hover:bg-slate-200" onClick={() => onView(asset.id)}>
      <Eye className="mr-2 h-4 w-4" />
      View
    </Button>
    {asset.pdfAvailable ? (
      <Button type="button" className="h-9 rounded-xl bg-blue-600 px-3 text-xs hover:bg-blue-700" onClick={() => onDownload(asset.id, 'PDF')}>
        <Download className="mr-2 h-4 w-4" />
        Download PDF
      </Button>
    ) : null}
    {asset.docxAvailable ? (
      <Button type="button" className="h-9 rounded-xl bg-primary-700 px-3 text-xs hover:bg-primary-700" onClick={() => onDownload(asset.id, 'DOCX')}>
        Download DOCX
      </Button>
    ) : null}
    {asset.excelAvailable ? (
      <Button type="button" className="h-9 rounded-xl bg-emerald-600 px-3 text-xs hover:bg-emerald-700" onClick={() => onDownload(asset.id, 'EXCEL')}>
        Download Excel
      </Button>
    ) : null}
  </div>
);

const ResourceTable = ({
  items,
  onView,
  onDownload,
}: {
  items: CurriculumAsset[];
  onView: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') => void;
  onDownload: (assetId: string, format?: 'PDF' | 'DOCX' | 'EXCEL') => void;
}) => {
  if (!items.length) {
    return <EmptyState title="Curriculum Resources" message={emptyMessage} />;
  }

  return (
    <div className="overflow-x-auto rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <table className="min-w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase tracking-[0.14em] text-slate-500">
          <tr>
            {['Title', 'Type', 'Subject', 'Grade', 'Phase', 'Term', 'Week', 'Academic Year', 'Province', 'Version', 'Uploaded By', 'Upload Date', 'Status', 'Badge', 'Actions'].map((label) => (
              <th key={label} className="px-4 py-3 font-semibold">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {items.map((asset) => (
            <tr key={asset.id} className="align-top">
              <td className="px-4 py-4">
                <p className="font-semibold text-slate-900">{asset.title}</p>
              </td>
              <td className="px-4 py-4 text-slate-700">{formatTypeLabel(asset.repositoryType)}</td>
              <td className="px-4 py-4 text-slate-700">{asset.subject}</td>
              <td className="px-4 py-4 text-slate-700">{asset.grade}</td>
              <td className="px-4 py-4 text-slate-700">{asset.curriculumPhase || 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.term || 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.weekNumber ?? 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.academicYear ?? 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.province || 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.versionNumber || 'Not set'}</td>
              <td className="px-4 py-4 text-slate-700">{asset.uploadedBy}</td>
              <td className="px-4 py-4 text-slate-700">{formatDate(asset.uploadDate)}</td>
              <td className="px-4 py-4">
                <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${(asset.status ?? 'ACTIVE') === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'}`}>
                  {asset.status ?? 'ACTIVE'}
                </span>
              </td>
              <td className="px-4 py-4">
                <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${asset.badge === 'District Approved' ? 'bg-primary-50 text-primary-700' : 'bg-amber-50 text-amber-700'}`}>
                  {asset.badge}
                </span>
              </td>
              <td className="px-4 py-4">
                <ResourceActions asset={asset} onView={onView} onDownload={onDownload} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const FilterBar = ({
  filters,
  setFilters,
  options,
  search,
  setSearch,
}: {
  filters: CurriculumResourceFilters;
  setFilters: Dispatch<SetStateAction<CurriculumResourceFilters>>;
  options: {
    subjects: string[];
    grades: string[];
    phases: string[];
    academicYears: number[];
    weeks: number[];
  };
  search: string;
  setSearch: (value: string) => void;
}) => (
  <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
    <div className="grid gap-3 lg:grid-cols-4 xl:grid-cols-5">
      <div className="relative lg:col-span-2">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search title, subject, grade, uploader..." className="h-11 rounded-2xl pl-10" />
      </div>
      <select value={filters.subject ?? ''} onChange={(event) => setFilters((current) => ({ ...current, subject: event.target.value || undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All subjects</option>
        {options.subjects.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.grade ?? ''} onChange={(event) => setFilters((current) => ({ ...current, grade: event.target.value || undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All grades</option>
        {options.grades.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.phase ?? ''} onChange={(event) => setFilters((current) => ({ ...current, phase: event.target.value || undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All phases</option>
        {options.phases.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.term ?? ''} onChange={(event) => setFilters((current) => ({ ...current, term: event.target.value || undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All terms</option>
        {FILTER_OPTIONS.terms.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.week?.toString() ?? ''} onChange={(event) => setFilters((current) => ({ ...current, week: event.target.value ? Number(event.target.value) : undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All weeks</option>
        {options.weeks.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.academicYear?.toString() ?? ''} onChange={(event) => setFilters((current) => ({ ...current, academicYear: event.target.value ? Number(event.target.value) : undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All academic years</option>
        {options.academicYears.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <select value={filters.type ?? ''} onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value || undefined }))} className="h-11 rounded-2xl border border-slate-200 px-3 text-sm">
        <option value="">All resource types</option>
        {FILTER_OPTIONS.types.map((item) => <option key={item} value={item}>{formatTypeLabel(item)}</option>)}
      </select>
    </div>
  </div>
);

export const SchoolCurriculumPage = () => {
  const [activeTab, setActiveTab] = useState<SchoolTab>('district-atps');
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<CurriculumResourceFilters>({});

  const districtResourcesQuery = useAppQuery({
    queryKey: ['school-curriculum-resources', filters],
    queryFn: () => schoolService.schoolCurriculumResources(filters),
  });
  const schoolAssetsQuery = useAppQuery({
    queryKey: ['school-admin-curriculum-assets'],
    queryFn: () => schoolService.schoolAdminCurriculumAssets(),
  });
  const schoolCalendarQuery = useAppQuery({
    queryKey: ['school-curriculum-calendar'],
    queryFn: schoolService.schoolCurriculumCalendar,
  });

  const viewFile = useMutation({
    mutationFn: ({ assetId, format }: { assetId: string; format?: 'PDF' | 'DOCX' | 'EXCEL' }) => schoolService.viewCurriculumResourceFile(assetId, format),
    onSuccess: (file) => openBlob(file.blob),
  });
  const downloadFile = useMutation({
    mutationFn: ({ assetId, format }: { assetId: string; format?: 'PDF' | 'DOCX' | 'EXCEL' }) => schoolService.downloadCurriculumResourceFile(assetId, format),
    onSuccess: (file) => saveBlob(file.blob, file.fileName),
  });

  const districtResources = districtResourcesQuery.data ?? [];
  const allSchoolAssets = schoolAssetsQuery.data ?? [];
  const schoolUploads = useMemo(
    () => allSchoolAssets.filter((asset) => (asset.source ?? asset.badge).toUpperCase().includes('SCHOOL') && !asset.archived && asset.active !== false && asset.deleted !== true),
    [allSchoolAssets],
  );

  const options = useMemo(() => {
    const union = [...districtResources, ...schoolUploads];
    return {
      subjects: Array.from(new Set(union.map((item) => item.subject).filter(Boolean))).sort(),
      grades: Array.from(new Set(union.map((item) => item.grade).filter(Boolean))).sort(),
      phases: Array.from(new Set(union.map((item) => item.curriculumPhase).filter(Boolean) as string[])).sort(),
      academicYears: Array.from(new Set(union.map((item) => item.academicYear).filter((item): item is number => Boolean(item)))).sort((a, b) => b - a),
      weeks: Array.from(new Set(union.map((item) => item.weekNumber).filter((item): item is number => Boolean(item)))).sort((a, b) => a - b),
    };
  }, [districtResources, schoolUploads]);

  const filteredDistrict = useMemo(
    () => districtResources.filter((asset) => applyClientFilters(asset, search, filters)),
    [districtResources, search, filters],
  );
  const filteredSchoolUploads = useMemo(
    () => schoolUploads.filter((asset) => applyClientFilters(asset, search, filters)),
    [schoolUploads, search, filters],
  );

  const currentItems = useMemo(() => {
    if (activeTab === 'school-uploads') {
      return filteredSchoolUploads;
    }
    return filteredDistrict.filter((asset) => typeMatchesTab(asset, activeTab));
  }, [activeTab, filteredDistrict, filteredSchoolUploads]);

  const summary = useMemo(() => ({
    total: districtResources.length,
    atps: districtResources.filter((item) => normalizeType(item.repositoryType) === 'ATP').length,
    syllabuses: districtResources.filter((item) => normalizeType(item.repositoryType) === 'SYLLABUS').length,
    lessonPlans: districtResources.filter((item) => normalizeType(item.repositoryType) === 'LESSON_PLAN').length,
  }), [districtResources]);

  if (districtResourcesQuery.isLoading || schoolAssetsQuery.isLoading || schoolCalendarQuery.isLoading) {
    return <LoadingState message="Loading curriculum resources..." />;
  }

  if (districtResourcesQuery.isError || schoolAssetsQuery.isError || schoolCalendarQuery.isError) {
    return <ErrorState message="Unable to load curriculum resources." />;
  }

  return (
    <AdminPageLayout>
      <AdminPageHeader title="Curriculum Resources" subtitle="View and download official district ATPs, syllabuses, lesson plans, and school-uploaded curriculum resources." />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <AdminMetricCard label="Total District Resources" value={String(summary.total)} helperText="All active district-approved resources visible to this school." tone="info" trendLabel="Live" />
        <AdminMetricCard label="ATPs Available" value={String(summary.atps)} helperText="District ATPs currently available." tone="positive" trendLabel="District" />
        <AdminMetricCard label="Syllabuses Available" value={String(summary.syllabuses)} helperText="Official syllabuses currently available." tone="positive" trendLabel="District" />
        <AdminMetricCard label="Lesson Plans Available" value={String(summary.lessonPlans)} helperText="District lesson plans currently available." tone="positive" trendLabel="District" />
      </div>
      <AdminCard className="p-5">
        <h3 className="text-base font-semibold text-slate-900">School ATP Calendar</h3>
        <div className="mt-4 grid gap-4 xl:grid-cols-[1fr_1fr]">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">This Week&apos;s ATP Topics</p>
            {schoolCalendarQuery.data?.thisWeeksCoverage ? (
              <div className="mt-2">
                <p className="font-semibold text-slate-900">{schoolCalendarQuery.data.thisWeeksCoverage.subject} · {schoolCalendarQuery.data.thisWeeksCoverage.grade}</p>
                <p className="mt-1 text-sm text-slate-700">{schoolCalendarQuery.data.thisWeeksCoverage.topic}</p>
                <p className="mt-1 text-sm text-slate-600">{schoolCalendarQuery.data.thisWeeksCoverage.subtopic || 'District ATP guidance available for this week.'}</p>
                <p className="mt-2 text-xs text-slate-500">{schoolCalendarQuery.data.thisWeeksCoverage.learningObjectives || schoolCalendarQuery.data.thisWeeksCoverage.lessonFocus || 'AI-generated lesson guidance is available once the district ATP has structured objectives and activities.'}</p>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button type="button" className="h-9 rounded-xl bg-slate-100 px-3 text-xs text-slate-900 hover:bg-slate-200" onClick={() => viewFile.mutate({ assetId: schoolCalendarQuery.data!.thisWeeksCoverage!.curriculumResourceId })}>
                    <Eye className="mr-2 h-4 w-4" />
                    View Source ATP
                  </Button>
                  <Button type="button" className="h-9 rounded-xl bg-blue-600 px-3 text-xs hover:bg-blue-700" onClick={() => downloadFile.mutate({ assetId: schoolCalendarQuery.data!.thisWeeksCoverage!.curriculumResourceId, format: 'PDF' })}>
                    <Download className="mr-2 h-4 w-4" />
                    Download Source ATP
                  </Button>
                </div>
              </div>
            ) : <p className="mt-2 text-sm text-slate-500">No published ATP topic has been mapped for this week.</p>}
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">District Reminders</p>
            <div className="mt-2 space-y-2">
              {(schoolCalendarQuery.data?.reminders ?? []).slice(0, 4).map((item) => (
                <div key={`${item.reminderType}-${item.reminderDate}`} className="rounded-xl border border-slate-200 bg-white px-3 py-2">
                  <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                  <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                </div>
              ))}
              {!(schoolCalendarQuery.data?.reminders ?? []).length ? <p className="text-sm text-slate-500">No district reminders are queued for this school.</p> : null}
            </div>
          </div>
        </div>
        <div className="mt-4 grid gap-4 xl:grid-cols-[1fr_1fr]">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">Upcoming Topics</p>
            <div className="mt-2 space-y-2">
              {(schoolCalendarQuery.data?.upcomingTopics ?? []).slice(0, 4).map((item) => (
                <div key={`${item.weekPlanId}-upcoming`} className="rounded-xl border border-slate-200 bg-white px-3 py-2">
                  <p className="text-sm font-semibold text-slate-900">{item.term} Week {item.weekNumber} • {item.subject}</p>
                  <p className="mt-1 text-sm text-slate-700">{item.topic}</p>
                  <p className="mt-1 text-xs text-slate-500">{item.subtopic || item.learningObjectives || 'District ATP guidance available.'}</p>
                </div>
              ))}
              {!(schoolCalendarQuery.data?.upcomingTopics ?? []).length ? <p className="text-sm text-slate-500">No upcoming ATP topics are available yet.</p> : null}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">ATP Debug</p>
            <div className="mt-2 grid gap-2 sm:grid-cols-2">
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Total Published ATP Calendar Items</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.totalPublishedAtpCalendarItems ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Items Visible To This School</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.itemsVisibleToSchool ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Current School ATP Matches</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.currentSchoolAtpMatches ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Items Matching Current Week</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.itemsMatchingCurrentWeek ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Items With Null Dates</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.itemsWithNullDates ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Null school_id Treated As District-Wide</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.districtWideItems ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2"><p className="text-xs text-slate-500">Reminders Queued For This School</p><p className="text-sm font-semibold text-slate-900">{schoolCalendarQuery.data?.remindersQueuedForSchool ?? 0}</p></div>
              <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 sm:col-span-2">
                <p className="text-xs text-slate-500">Current Week ATP Item</p>
                <p className="text-sm font-semibold text-slate-900">
                  {schoolCalendarQuery.data?.currentWeekAtpItem ? `${schoolCalendarQuery.data.currentWeekAtpItem.term} Week ${schoolCalendarQuery.data.currentWeekAtpItem.weekNumber}: ${schoolCalendarQuery.data.currentWeekAtpItem.topic}` : 'No current week ATP item matched.'}
                </p>
              </div>
            </div>
          </div>
        </div>
        {!schoolCalendarQuery.data?.thisWeeksCoverage && (schoolCalendarQuery.data?.itemsVisibleToSchool ?? 0) > 0 ? (
          <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4">
            <p className="text-sm font-medium text-amber-900">No ATP item is mapped to this week yet, but published ATP calendar items are available below.</p>
            <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {(schoolCalendarQuery.data?.visibleTopics ?? []).slice(0, 9).map((item) => (
                <div key={`visible-${item.weekPlanId}`} className="rounded-xl border border-amber-100 bg-white px-3 py-3">
                  <p className="text-sm font-semibold text-slate-900">{item.subject} • {item.grade}</p>
                  <p className="mt-1 text-xs uppercase tracking-[0.12em] text-slate-500">{item.term} • Week {item.weekNumber}</p>
                  <p className="mt-2 text-sm text-slate-700">{item.topic}</p>
                  <p className="mt-1 text-xs text-slate-500">{item.subtopic || item.lessonFocus || item.learningObjectives || 'Topic available without a mapped date window.'}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <Button type="button" className="h-8 rounded-xl bg-slate-100 px-3 text-[11px] text-slate-900 hover:bg-slate-200" onClick={() => viewFile.mutate({ assetId: item.curriculumResourceId })}>
                      View Source ATP
                    </Button>
                    <Button type="button" className="h-8 rounded-xl bg-blue-600 px-3 text-[11px] hover:bg-blue-700" onClick={() => downloadFile.mutate({ assetId: item.curriculumResourceId, format: 'PDF' })}>
                      Download Source ATP
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </AdminCard>
      <FilterBar filters={filters} setFilters={setFilters} options={options} search={search} setSearch={setSearch} />
      <AdminCard className="p-4">
        <div className="flex flex-wrap gap-2">
          {[
            { key: 'district-atps', label: 'District ATPs', icon: BookOpen },
            { key: 'syllabuses', label: 'Syllabuses', icon: FileText },
            { key: 'lesson-plans', label: 'Lesson Plans', icon: ClipboardCheck },
            { key: 'school-uploads', label: 'School Uploads', icon: FileText },
          ].map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key as SchoolTab)}
                className={`inline-flex items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-medium ${active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </AdminCard>
      <ResourceTable items={currentItems} onView={(assetId, format) => viewFile.mutate({ assetId, format })} onDownload={(assetId, format) => downloadFile.mutate({ assetId, format })} />
    </AdminPageLayout>
  );
};

export const TeacherCurriculumPage = ({ latestResources }: { latestResources?: CurriculumAsset[] }) => {
  const [activeTab, setActiveTab] = useState<TeacherTab>('district-atps');
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<CurriculumResourceFilters>({});
  const [generatedLessonPlan, setGeneratedLessonPlan] = useState<TeacherLessonPlanResponse | null>(null);
  const [generatedLessonPlanMatchKey, setGeneratedLessonPlanMatchKey] = useState<string | null>(null);
  const [lessonPlanMessage, setLessonPlanMessage] = useState<string | null>(null);
  const [lessonPlanError, setLessonPlanError] = useState<string | null>(null);

  const resourcesQuery = useAppQuery({
    queryKey: ['teacher-curriculum-resources', filters],
    queryFn: () => schoolService.teacherCurriculumResources(filters),
  });
  const widgetsQuery = useAppQuery({
    queryKey: ['teacher-curriculum-page-widgets'],
    queryFn: schoolService.teacherCurriculumWidgets,
  });
  const progressMutation = useMutation({
    mutationFn: ({ weekPlanId, status, completionPercent }: { weekPlanId: string; status: string; completionPercent: number }) =>
      schoolService.updateTeacherCurriculumProgress(weekPlanId, { status, completionPercent }),
    onSuccess: () => {
      widgetsQuery.refetch();
    },
  });
  const generateLessonPlanMutation = useMutation({
    mutationFn: ({ weekPlanId, calendarItemId, regenerate }: { weekPlanId: string; calendarItemId?: string | null; regenerate?: boolean }) =>
      calendarItemId
        ? schoolService.createTeacherLessonPlanFromCalendarItem(calendarItemId, regenerate ? { regenerate: true } : undefined)
        : schoolService.generateTeacherLessonPlan(weekPlanId),
    onSuccess: async (data, variables) => {
      setGeneratedLessonPlan(data);
      setGeneratedLessonPlanMatchKey(variables.calendarItemId ?? variables.weekPlanId);
      setLessonPlanError(null);
      setLessonPlanMessage(data.alreadyExisted ? 'An existing lesson plan was loaded from the repository.' : 'Lesson plan created and saved to the lesson plan repositories.');
      await Promise.all([resourcesQuery.refetch(), widgetsQuery.refetch()]);
    },
    onError: (error) => {
      setLessonPlanError(error instanceof Error ? error.message : 'Unable to create the lesson plan from this ATP topic.');
    },
  });

  const viewFile = useMutation({
    mutationFn: ({ assetId, format }: { assetId: string; format?: 'PDF' | 'DOCX' | 'EXCEL' }) => schoolService.viewCurriculumResourceFile(assetId, format),
    onSuccess: (file) => openBlob(file.blob),
  });
  const downloadFile = useMutation({
    mutationFn: ({ assetId, format }: { assetId: string; format?: 'PDF' | 'DOCX' | 'EXCEL' }) => schoolService.downloadCurriculumResourceFile(assetId, format),
    onSuccess: (file) => saveBlob(file.blob, file.fileName),
  });

  const widgetResources = useMemo(() => {
    const merged = [
      ...(widgetsQuery.data?.districtResources ?? []),
      ...(widgetsQuery.data?.officialSyllabuses ?? []),
      ...(widgetsQuery.data?.lessonPlans ?? []),
    ];
    const seen = new Set<string>();
    return merged.filter((asset) => {
      if (seen.has(asset.id)) {
        return false;
      }
      seen.add(asset.id);
      return true;
    });
  }, [widgetsQuery.data?.districtResources, widgetsQuery.data?.officialSyllabuses, widgetsQuery.data?.lessonPlans]);
  const resources = (resourcesQuery.data?.length ? resourcesQuery.data : widgetResources) ?? [];
  const repositoryLessonPlans = useMemo(() => {
    const merged = [
      ...resources.filter((asset) => normalizeType(asset.repositoryType) === 'LESSON_PLAN'),
      ...(widgetsQuery.data?.lessonPlans ?? []),
    ];
    const seen = new Set<string>();
    return merged.filter((asset) => {
      if (seen.has(asset.id)) {
        return false;
      }
      seen.add(asset.id);
      return true;
    });
  }, [resources, widgetsQuery.data?.lessonPlans]);
  const coverage = widgetsQuery.data?.thisWeeksCoverage ?? null;
  const weeklyTopics = useMemo(() => {
    if (widgetsQuery.data?.visibleTopics?.length) {
      return widgetsQuery.data.visibleTopics;
    }
    const items: TeacherCoverageItem[] = [];
    if (coverage) items.push(coverage);
    return items;
  }, [coverage, widgetsQuery.data?.visibleTopics]);

  const options = useMemo(() => ({
    subjects: Array.from(new Set([...resources, ...repositoryLessonPlans].map((item) => item.subject).filter(Boolean))).sort(),
    grades: Array.from(new Set([...resources, ...repositoryLessonPlans].map((item) => item.grade).filter(Boolean))).sort(),
    phases: Array.from(new Set([...resources, ...repositoryLessonPlans].map((item) => item.curriculumPhase).filter(Boolean) as string[])).sort(),
    academicYears: Array.from(new Set([...resources, ...repositoryLessonPlans].map((item) => item.academicYear).filter((item): item is number => Boolean(item)))).sort((a, b) => b - a),
    weeks: Array.from(new Set([...resources, ...repositoryLessonPlans].map((item) => item.weekNumber).filter((item): item is number => Boolean(item)))).sort((a, b) => a - b),
  }), [resources, repositoryLessonPlans]);

  const filteredResources = useMemo(
    () => resources.filter((asset) => applyClientFilters(asset, search, filters)),
    [resources, search, filters],
  );
  const filteredLessonPlans = useMemo(
    () => repositoryLessonPlans.filter((asset) => applyClientFilters(asset, search, filters)),
    [repositoryLessonPlans, search, filters],
  );

  const currentItems = useMemo(() => {
    if (activeTab === 'weekly-atp') {
      return [];
    }
    if (activeTab === 'lesson-plans') {
      return filteredLessonPlans;
    }
    return filteredResources.filter((asset) => typeMatchesTab(asset, activeTab));
  }, [activeTab, filteredLessonPlans, filteredResources]);

  if (resourcesQuery.isLoading || widgetsQuery.isLoading) {
    return <LoadingState message="Loading teacher curriculum resources..." />;
  }

  if (resourcesQuery.isError || widgetsQuery.isError) {
    return <ErrorState message="Unable to load teacher curriculum resources." />;
  }

  return (
    <div className="space-y-5">
      <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-semibold text-slate-900">Curriculum Resources</h2>
            <p className="mt-2 text-sm text-slate-600">District ATPs, syllabuses, lesson plans, and this week&apos;s ATP coverage for your assigned teaching scope.</p>
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            {(latestResources ?? resources.slice(0, 5)).slice(0, 5).map((item) => (
              <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                <p className="mt-1 text-xs text-slate-500">{item.subject} · {item.grade}</p>
              </div>
            ))}
          </div>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">Total Published ATP Calendar Items</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">{widgetsQuery.data?.totalPublishedAtpCalendarItems ?? 0}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">Current Teacher ATP Matches</p>
            <p className="mt-1 text-lg font-semibold text-slate-900">{widgetsQuery.data?.currentTeacherAtpMatches ?? 0}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.12em] text-slate-500">Current Week ATP Item</p>
            <p className="mt-1 text-sm font-semibold text-slate-900">
              {widgetsQuery.data?.currentWeekAtpItem ? `${widgetsQuery.data.currentWeekAtpItem.term} Week ${widgetsQuery.data.currentWeekAtpItem.weekNumber}: ${widgetsQuery.data.currentWeekAtpItem.topic}` : 'No match found'}
            </p>
          </div>
        </div>
      </section>
      <FilterBar filters={filters} setFilters={setFilters} options={options} search={search} setSearch={setSearch} />
      <section className="rounded-[28px] border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap gap-2">
          {[
            { key: 'district-atps', label: 'District ATPs', icon: BookOpen },
            { key: 'syllabuses', label: 'Syllabuses', icon: FileText },
            { key: 'lesson-plans', label: 'Lesson Plans', icon: ClipboardCheck },
            { key: 'weekly-atp', label: 'Weekly ATP Topics', icon: ClipboardCheck },
          ].map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key as TeacherTab)}
                className={`inline-flex items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-medium ${active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </section>
      {activeTab === 'weekly-atp' ? (
        <section className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="text-base font-semibold text-slate-900">This Week&apos;s ATP Coverage</h3>
          {weeklyTopics.length ? (
            <div className="mt-4 space-y-3">
              {weeklyTopics.map((item) => (
                <div key={item.weekPlanId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="grid gap-3 md:grid-cols-3">
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Subject</p><p className="mt-1 font-semibold text-slate-900">{item.subject}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Grade</p><p className="mt-1 font-semibold text-slate-900">{item.grade}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Status</p><p className="mt-1 font-semibold text-slate-900">{item.status.split('_').join(' ')}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Phase</p><p className="mt-1 font-semibold text-slate-900">{item.phase || 'Not set'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Academic Year</p><p className="mt-1 font-semibold text-slate-900">{item.academicYear ?? 'Not set'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Term</p><p className="mt-1 font-semibold text-slate-900">{item.term}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Week</p><p className="mt-1 font-semibold text-slate-900">{item.weekNumber}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Topic</p><p className="mt-1 font-semibold text-slate-900">{item.topic}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Sub-topic</p><p className="mt-1 font-semibold text-slate-900">{item.subtopic || 'Not set'}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Learning Objectives</p><p className="mt-1 text-slate-700">{item.learningObjectives || 'Not set'}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Suggested Activities</p><p className="mt-1 text-slate-700">{item.lessonFocus || 'Not set'}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Assessment Task</p><p className="mt-1 text-slate-700">{item.assessmentTask || 'Not set'}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">Media / Resources</p><p className="mt-1 text-slate-700">{item.resources || 'Not set'}</p></div>
                    <div className="md:col-span-3"><p className="text-xs uppercase tracking-[0.12em] text-slate-500">ATP Source Document</p><p className="mt-1 text-slate-700">{item.sourceTitle || 'District ATP source available in resources.'}</p></div>
                    <div className="md:col-span-3 flex flex-wrap gap-2">
                      <Button type="button" className="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-900 hover:bg-slate-200" onClick={() => viewFile.mutate({ assetId: item.curriculumResourceId })}>
                        <Eye className="mr-2 h-4 w-4" />
                        View Source ATP
                      </Button>
                      <Button
                        type="button"
                        className="rounded-xl bg-primary-700 px-3 py-2 text-xs hover:bg-primary-700"
                        disabled={generateLessonPlanMutation.isPending}
                        onClick={() => {
                          setLessonPlanError(null);
                          setLessonPlanMessage(null);
                          generateLessonPlanMutation.mutate({ weekPlanId: item.weekPlanId, calendarItemId: item.atpCalendarItemId });
                        }}
                      >
                        Create Lesson Plan from ATP Topic
                      </Button>
                      <Button type="button" className="rounded-xl px-3 py-2 text-xs" disabled={progressMutation.isPending} onClick={() => progressMutation.mutate({ weekPlanId: item.weekPlanId, status: 'NOT_STARTED', completionPercent: 0 })}>Mark Not Started</Button>
                      <Button type="button" className="rounded-xl bg-amber-600 px-3 py-2 text-xs hover:bg-amber-700" disabled={progressMutation.isPending} onClick={() => progressMutation.mutate({ weekPlanId: item.weekPlanId, status: 'IN_PROGRESS', completionPercent: 50 })}>Mark In Progress</Button>
                      <Button type="button" className="rounded-xl bg-emerald-600 px-3 py-2 text-xs hover:bg-emerald-700" disabled={progressMutation.isPending} onClick={() => progressMutation.mutate({ weekPlanId: item.weekPlanId, status: 'COMPLETED', completionPercent: 100 })}>Mark Completed</Button>
                    </div>
                    {lessonPlanError && generatedLessonPlanMatchKey === (item.atpCalendarItemId ?? item.weekPlanId) ? (
                      <div className="md:col-span-3">
                        <ErrorState message={lessonPlanError} />
                      </div>
                    ) : null}
                    {generatedLessonPlan && generatedLessonPlanMatchKey === (item.atpCalendarItemId ?? item.weekPlanId) ? (
                      <div className="md:col-span-3 rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-700">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="font-semibold text-slate-900">{generatedLessonPlan.title}</p>
                            <p className="mt-1 text-sm text-slate-600">Week Ending: {generatedLessonPlan.weekEnding}</p>
                            <p className="mt-1 text-sm text-slate-600">Sub-topic: {generatedLessonPlan.subtopic}</p>
                            {lessonPlanMessage ? <p className="mt-2 text-xs font-medium text-emerald-700">{lessonPlanMessage}</p> : null}
                          </div>
                          <div className="flex flex-wrap gap-2">
                            {generatedLessonPlan.lessonPlanAssetId ? (
                              <Button type="button" className="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-900 hover:bg-slate-200" onClick={() => viewFile.mutate({ assetId: generatedLessonPlan.lessonPlanAssetId!, format: generatedLessonPlan.pdfAvailable ? 'PDF' : undefined })}>
                                View Lesson Plan
                              </Button>
                            ) : null}
                            <Button type="button" className="rounded-xl bg-blue-600 px-3 py-2 text-xs hover:bg-blue-700" disabled={!generatedLessonPlan.lessonPlanAssetId || !generatedLessonPlan.docxAvailable} onClick={() => downloadFile.mutate({ assetId: generatedLessonPlan.lessonPlanAssetId!, format: 'DOCX' })}>
                              Download Word
                            </Button>
                            <Button type="button" className="rounded-xl bg-emerald-600 px-3 py-2 text-xs hover:bg-emerald-700" disabled={!generatedLessonPlan.lessonPlanAssetId || !generatedLessonPlan.pdfAvailable} onClick={() => downloadFile.mutate({ assetId: generatedLessonPlan.lessonPlanAssetId!, format: 'PDF' })}>
                              Download PDF
                            </Button>
                            <Button
                              type="button"
                              className="rounded-xl bg-primary-700 px-3 py-2 text-xs hover:bg-primary-700"
                              disabled={generateLessonPlanMutation.isPending}
                              onClick={() => {
                                if (window.confirm('Regenerate and replace the existing lesson plan for this ATP topic?')) {
                                  setLessonPlanError(null);
                                  generateLessonPlanMutation.mutate({ weekPlanId: item.weekPlanId, calendarItemId: item.atpCalendarItemId, regenerate: true });
                                }
                              }}
                            >
                              Edit Lesson Plan
                            </Button>
                          </div>
                        </div>
                        <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200">
                          <table className="min-w-full text-left text-xs sm:text-sm">
                            <thead className="bg-slate-50 text-slate-700">
                              <tr>
                                {['Day', 'Topic/Content', 'Objectives', 'Source of Matter', 'Media', 'Lesson Activities', 'Evaluation'].map((label) => (
                                  <th key={label} className="px-3 py-2 font-semibold">{label}</th>
                                ))}
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200 bg-white align-top">
                              {generatedLessonPlan.days.map((day) => (
                                <tr key={day.day}>
                                  <td className="px-3 py-3 font-semibold text-slate-900">{day.day}</td>
                                  <td className="px-3 py-3 text-slate-700">{day.topicContent}</td>
                                  <td className="px-3 py-3 text-slate-700">{day.objectives}</td>
                                  <td className="px-3 py-3 text-slate-700">{day.sourceOfMatter}</td>
                                  <td className="px-3 py-3 text-slate-700">{day.media}</td>
                                  <td className="px-3 py-3 text-slate-700 whitespace-pre-line">{day.lessonActivities}</td>
                                  <td className="px-3 py-3 text-slate-700">{day.evaluation}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-500">No weekly ATP topic has been mapped yet.</p>
          )}
        </section>
      ) : (
        <ResourceTable items={currentItems} onView={(assetId, format) => viewFile.mutate({ assetId, format })} onDownload={(assetId, format) => downloadFile.mutate({ assetId, format })} />
      )}
    </div>
  );
};




