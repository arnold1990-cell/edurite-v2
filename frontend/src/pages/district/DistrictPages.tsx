import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { AlertTriangle, ArrowDownToLine, BarChart3, BellRing, BookOpen, Building2, CalendarDays, ClipboardCheck, FileSpreadsheet, FileText, Filter, GraduationCap, LifeBuoy, Search, ShieldCheck, Sparkles, Trash2, Users2, Wrench } from 'lucide-react';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { CompactDataTable, DashboardKpiCard, DashboardSectionCard, DashboardShell, EmptyStateCompact, ProgressBar, QuickActionButton, RiskBadge } from '@/components/dashboard/DashboardPrimitives';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useAppQuery } from '@/hooks/useAppQuery';
import {
  districtService,
  type DistrictCurriculumAsset,
  type DistrictCurriculumAssetUpsert,
  type DistrictCurriculumComplianceResponse,
  type DistrictCurriculumCalendarItem,
  type DistrictDistributionItem,
  type DistrictInsightItem,
  type DistrictMetricCard,
  type DistrictTrendPoint,
} from '@/services/districtService';

const toneClass = (tone?: string) => tone === 'positive'
  ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
  : tone === 'warning' || tone === 'critical'
    ? 'border-amber-200 bg-amber-50 text-amber-700'
    : 'border-slate-200 bg-white text-slate-700';

const StatCard = ({ item }: { item: DistrictMetricCard }) => (
  <div className={`rounded-[24px] border p-5 shadow-sm ${toneClass(item.tone)}`}>
    <p className="text-xs font-semibold uppercase tracking-[0.2em]">{item.label}</p>
    <p className="mt-3 text-3xl font-semibold">{item.value}</p>
    <p className="mt-2 text-sm opacity-80">{item.helperText}</p>
  </div>
);

const ChartCard = ({ title, points }: { title: string; points: Array<DistrictTrendPoint | DistrictDistributionItem> }) => (
  <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex items-center gap-2">
      <BarChart3 className="h-4 w-4 text-blue-600" />
      <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
    </div>
    {points.length ? (
      <div className="mt-5 space-y-3">
        {points.map((point) => {
          const value = Number(point.value ?? 0);
          const width = Math.max(8, Math.min(100, value * 4));
          return (
            <div key={point.label}>
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate text-slate-600">{point.label}</span>
                <span className="font-medium text-slate-900">{value}</span>
              </div>
              <div className="mt-2 h-2 rounded-full bg-slate-100">
                <div
                  className={`h-2 rounded-full ${point.tone === 'positive' ? 'bg-emerald-500' : point.tone === 'warning' || point.tone === 'critical' ? 'bg-amber-500' : 'bg-blue-500'}`}
                  style={{ width: `${width}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    ) : (
      <div className="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
        No chart data available yet.
      </div>
    )}
  </div>
);

const InsightList = ({ title, items, icon: Icon }: { title: string; items: DistrictInsightItem[]; icon: React.ComponentType<{ className?: string }>; }) => (
  <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex items-center gap-2">
      <Icon className="h-4 w-4 text-blue-600" />
      <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
    </div>
    {items.length ? (
      <div className="mt-4 space-y-3">
        {items.map((item, index) => (
          <div key={`${item.title}-${index}`} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
            <p className="text-sm font-semibold text-slate-900">{item.title}</p>
            <p className="mt-1 text-sm text-slate-600">{item.detail}</p>
          </div>
        ))}
      </div>
    ) : (
      <div className="mt-4">
        <EmptyState title={title} message="No insights available yet." />
      </div>
    )}
  </div>
);

const DataShell = ({ isLoading, error, children }: { isLoading: boolean; error: unknown; children: React.ReactNode }) => {
  if (isLoading) return <LoadingState message="Loading district data..." detail="Preparing live district analytics and school intelligence." />;
  if (error) return <ErrorState message={error instanceof Error ? error.message : 'Unable to load district data.'} />;
  return <>{children}</>;
};

const saveBase64File = (fileName: string, contentType: string, base64Content: string) => {
  const bytes = Uint8Array.from(atob(base64Content), (char) => char.charCodeAt(0));
  const blob = new Blob([bytes], { type: contentType });
  saveBlobFile(fileName, blob);
};

const saveBlobFile = (fileName: string, blob: Blob) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

const curriculumViewMeta: Record<string, { title: string; subtitle: string; repositoryType?: string; icon: React.ComponentType<{ className?: string }> }> = {
  'atp-repository': { title: 'ATP Repository', subtitle: 'Upload, replace, archive, and distribute official district ATPs by grade, phase, year, and province.', repositoryType: 'ATP', icon: BookOpen },
  'syllabus-repository': { title: 'Syllabus Repository', subtitle: 'Publish official CAPS-aligned syllabuses that schools and teachers can view or download.', repositoryType: 'SYLLABUS', icon: FileText },
  'lesson-plan-repository': { title: 'Lesson Plan Repository', subtitle: 'Manage annual, term, and weekly lesson plans with district-first visibility for teachers.', repositoryType: 'LESSON_PLAN', icon: ClipboardCheck },
  'curriculum-calendar': { title: 'Curriculum Calendar', subtitle: 'See ATP milestones across the academic year and monitor district-level curriculum rollout dates.', icon: CalendarDays },
  'weekly-coverage-tracker': { title: 'Weekly Coverage Tracker', subtitle: 'Track school coverage, teacher pacing, and ATP execution from a district perspective.', icon: BarChart3 },
  'teacher-reminders': { title: 'Teacher Reminders', subtitle: 'Monitor automated Monday, Wednesday, and Friday curriculum reminders and the teachers who need follow-up.', icon: BellRing },
  'curriculum-compliance': { title: 'Curriculum Compliance', subtitle: 'Review compliance percentages, subject and teacher backlog, heat-map signals, and curriculum risk alerts.', icon: ShieldCheck },
};

const fileToPayload = async (file: File | null | undefined) => {
  if (!file) return undefined;
  if (file.size <= 0) {
    throw new Error(`${file.name} is empty.`);
  }
  const extension = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() ?? '' : '';
  const allowedExtensions = new Set(['pdf', 'doc', 'docx', 'xls', 'xlsx', 'csv']);
  if (!allowedExtensions.has(extension)) {
    throw new Error(`Unsupported file format for ${file.name}.`);
  }
  const base64Content = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const raw = typeof reader.result === 'string' ? reader.result : '';
      resolve(raw.includes(',') ? raw.split(',')[1] : raw);
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
  return { fileName: file.name, contentType: file.type || 'application/octet-stream', base64Content };
};

const deriveTitleFromFileName = (fileName: string) => fileName.replace(/\.[^/.]+$/, '').trim();

export const DistrictDashboardPage = () => {
  const query = useAppQuery({ queryKey: ['district-dashboard'], queryFn: districtService.dashboard });
  const curriculumComplianceQuery = useAppQuery({ queryKey: ['district-curriculum-dashboard-preview'], queryFn: districtService.curriculumCompliance });

  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[32px] bg-gradient-to-r from-[#0F172A] via-[#1E3A8A] to-[#2563EB] px-6 py-7 text-white shadow-xl">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-100">District Overview</p>
            <h1 className="mt-2 text-3xl font-semibold">{query.data.districtName}</h1>
            <p className="mt-3 max-w-4xl text-sm text-blue-50">{query.data.summaryHeadline}</p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {query.data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="District-wide Performance Trends" points={query.data.performanceTrends} />
            <ChartCard title="Learner Risk Distribution" points={query.data.learnerRiskDistribution} />
            <ChartCard title="Report Upload Completion" points={query.data.reportUploadProgress} />
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <InsightList title="School Ranking" items={query.data.schoolRanking} icon={Building2} />
            <InsightList title="Urgent Intervention Queue" items={query.data.schoolsNeedingIntervention} icon={AlertTriangle} />
            <InsightList title="AI Highlights" items={query.data.aiHighlights} icon={Sparkles} />
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <LifeBuoy className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Recent Support Requests</h3>
              </div>
              <div className="mt-4 space-y-3">
                {query.data.recentSupportRequests.length ? query.data.recentSupportRequests.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.schoolName} • {item.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                  </div>
                )) : <EmptyState title="Support requests" message="No school support requests are open." />}
              </div>
            </div>
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <BellRing className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Recent Announcements</h3>
              </div>
              <div className="mt-4 space-y-3">
                {query.data.recentAnnouncements.length ? query.data.recentAnnouncements.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                  </div>
                )) : <EmptyState title="Announcements" message="No district announcements have been sent yet." />}
              </div>
            </div>
          </div>

          {curriculumComplianceQuery.data ? (
            <div className="grid gap-5 xl:grid-cols-[1fr_1fr]">
              <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-blue-600" />
                  <h3 className="text-sm font-semibold text-slate-900">Curriculum Compliance</h3>
                </div>
                <div className="mt-4 grid gap-3 md:grid-cols-2">
                  {curriculumComplianceQuery.data.metrics.map((item) => <StatCard key={`curriculum-${item.label}`} item={item} />)}
                </div>
              </div>
              <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-600" />
                  <h3 className="text-sm font-semibold text-slate-900">Curriculum Risk Alerts</h3>
                </div>
                <div className="mt-4 space-y-3">
                  {curriculumComplianceQuery.data.riskAlerts.length ? curriculumComplianceQuery.data.riskAlerts.slice(0, 4).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3">
                      <p className="text-sm font-semibold text-slate-900">{item.schoolName} - {item.subject}</p>
                      <p className="mt-1 text-sm text-slate-700">{item.detail}</p>
                      <p className="mt-1 text-xs uppercase tracking-[0.12em] text-amber-700">{item.teacherName}</p>
                    </div>
                  )) : <EmptyState title="Curriculum alerts" message="No curriculum risk alerts are currently open." />}
                </div>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictSchoolsPage = () => {
  const [search, setSearch] = useState('');
  const [riskLevel, setRiskLevel] = useState('');
  const [complianceStatus, setComplianceStatus] = useState('');
  const query = useAppQuery({
    queryKey: ['district-schools', search, riskLevel, complianceStatus],
    queryFn: () => districtService.schools({ search: search || undefined, riskLevel: riskLevel || undefined, complianceStatus: complianceStatus || undefined }),
  });

  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="flex flex-col gap-4 rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm xl:flex-row xl:items-center xl:justify-between">
            <div>
              <h1 className="text-2xl font-semibold text-slate-900">District Schools</h1>
              <p className="mt-2 text-sm text-slate-600">Search, filter, and drill into school performance, reporting, readiness, and compliance.</p>
            </div>
            <div className="grid gap-3 md:grid-cols-3 xl:w-[55rem]">
              <label className="relative">
                <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
                <Input value={search} onChange={(event) => setSearch(event.target.value)} className="rounded-2xl border-slate-200 bg-slate-50 pl-10 py-3" placeholder="Search schools" />
              </label>
              <label className="relative">
                <Filter className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
                <select value={riskLevel} onChange={(event) => setRiskLevel(event.target.value)} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-10 py-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">All risk levels</option>
                  <option value="Low">Low</option>
                  <option value="Medium">Medium</option>
                  <option value="High">High</option>
                </select>
              </label>
              <select value={complianceStatus} onChange={(event) => setComplianceStatus(event.target.value)} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500">
                <option value="">All compliance states</option>
                <option value="Compliant">Compliant</option>
                <option value="Review Needed">Review Needed</option>
              </select>
            </div>
          </div>

          <div className="grid gap-3 lg:grid-cols-3">
            <Link to="/district/school-registration-requests?status=PENDING" className="rounded-[24px] border border-amber-200 bg-amber-50 px-5 py-4 transition hover:border-amber-300">
              <p className="text-sm font-semibold text-slate-900">Pending Requests</p>
              <p className="mt-1 text-sm text-slate-600">Review new self-registered schools waiting for district approval.</p>
            </Link>
            <Link to="/district/school-registration-requests?status=APPROVED" className="rounded-[24px] border border-emerald-200 bg-emerald-50 px-5 py-4 transition hover:border-emerald-300">
              <p className="text-sm font-semibold text-slate-900">Approved Schools</p>
              <p className="mt-1 text-sm text-slate-600">See which registration requests are already active in the district.</p>
            </Link>
            <Link to="/district/school-registration-requests?status=REJECTED" className="rounded-[24px] border border-rose-200 bg-rose-50 px-5 py-4 transition hover:border-rose-300">
              <p className="text-sm font-semibold text-slate-900">Rejected Requests</p>
              <p className="mt-1 text-sm text-slate-600">Review rejected requests and the reasons shared back to schools.</p>
            </Link>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {query.data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
          </div>

          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">School</th>
                    <th className="px-4 py-3 font-medium">Learners</th>
                    <th className="px-4 py-3 font-medium">Teachers</th>
                    <th className="px-4 py-3 font-medium">Reports</th>
                    <th className="px-4 py-3 font-medium">APS</th>
                    <th className="px-4 py-3 font-medium">Risk</th>
                    <th className="px-4 py-3 font-medium">Compliance</th>
                    <th className="px-4 py-3 font-medium">Drill-down</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.items.length ? query.data.items.map((item) => (
                    <tr key={item.schoolId} className="border-t border-slate-100">
                      <td className="px-4 py-3">
                        <div>
                          <p className="font-semibold text-slate-900">{item.schoolName}</p>
                          <p className="text-xs text-slate-500">{item.province || 'Province not set'}</p>
                        </div>
                      </td>
                      <td className="px-4 py-3">{item.learnerCount}</td>
                      <td className="px-4 py-3">{item.teacherCount}</td>
                      <td className="px-4 py-3">{item.reportUploadStatus}</td>
                      <td className="px-4 py-3">{item.averageApsScore}</td>
                      <td className="px-4 py-3">{item.riskLevel}</td>
                      <td className="px-4 py-3">{item.complianceStatus}</td>
                      <td className="px-4 py-3">
                        <Link to={`/district/schools/${item.schoolId}`} className="font-semibold text-blue-600 hover:text-blue-700">Open school dashboard</Link>
                      </td>
                    </tr>
                  )) : (
                    <tr>
                      <td colSpan={8} className="px-4 py-8">
                        <EmptyState title="No schools found" message="Adjust your filters or onboard more schools into the district." />
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

/* const LegacyDistrictSchoolDetailPage = () => {
  const { schoolId = '' } = useParams();
  const [exporting, setExporting] = useState<'pdf' | 'xlsx' | null>(null);
  const detailQuery = useAppQuery({ queryKey: ['district-school-detail', schoolId], queryFn: () => districtService.schoolDetail(schoolId), enabled: Boolean(schoolId) });
  const analyticsQuery = useAppQuery({ queryKey: ['district-school-analytics', schoolId], queryFn: () => districtService.schoolAnalytics(schoolId), enabled: Boolean(schoolId) });

  const handleExport = async (format: 'pdf' | 'xlsx') => {
    if (!detailQuery.data) return;
    setExporting(format);
    try {
      const response = await districtService.exportReport(detailQuery.data.exportReportType, format);
      saveBase64File(response.fileName, response.contentType, response.base64Content);
    } finally {
      setExporting(null);
    }
  };

  return (
    <DataShell isLoading={detailQuery.isLoading || analyticsQuery.isLoading} error={detailQuery.error ?? analyticsQuery.error}>
      {detailQuery.data && analyticsQuery.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-700">School Drill-down Dashboard</p>
                <h1 className="mt-2 text-3xl font-semibold text-slate-900">{detailQuery.data.schoolName}</h1>
                <div className="mt-4 flex flex-wrap gap-3 xl:hidden">
                  <Button onClick={() => handleExport('pdf')} disabled={exporting !== null} className="rounded-2xl px-4 py-2.5">
                    <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                    {exporting === 'pdf' ? 'Exporting PDF...' : 'Export school report'}
                  </Button>
                  <Button onClick={() => handleExport('xlsx')} disabled={exporting !== null} className="rounded-2xl bg-emerald-600 px-4 py-2.5 hover:bg-emerald-500">
                    <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                    {exporting === 'xlsx' ? 'Exporting Excel...' : 'Export Excel'}
                  </Button>
                </div>
            <p className="mt-3 text-sm text-slate-600">{detailQuery.data.district || 'District not set'} • {detailQuery.data.province || 'Province not set'} • {detailQuery.data.contactEmail || 'No contact email'}</p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {detailQuery.data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="Subject Performance" points={detailQuery.data.subjectPerformance} />
            <ChartCard title="APS Readiness" points={detailQuery.data.apsBandDistribution} />
            <ChartCard title="Report Uploads" points={detailQuery.data.reportUploads} />
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <InsightList title="Teacher Activity" items={detailQuery.data.teacherActivity} icon={Users2} />
            <InsightList title="AI Recommendations" items={detailQuery.data.aiRecommendations} icon={Sparkles} />
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="Performance Trends" points={analyticsQuery.data.performanceTrends} />
            <ChartCard title="Grade / Class Comparison" points={analyticsQuery.data.gradePerformanceComparison} />
            <ChartCard title="Career Pathways" points={analyticsQuery.data.careerPathwayDistribution} />
          </div>
        </section>
      ) : null}
    </DataShell>
  );
}; */

export const DistrictSchoolDetailPage = () => {
  const { schoolId = '' } = useParams();
  const [exporting, setExporting] = useState<'pdf' | 'xlsx' | null>(null);
  const detailQuery = useAppQuery({ queryKey: ['district-school-detail', schoolId], queryFn: () => districtService.schoolDetail(schoolId), enabled: Boolean(schoolId) });
  const analyticsQuery = useAppQuery({ queryKey: ['district-school-analytics', schoolId], queryFn: () => districtService.schoolAnalytics(schoolId), enabled: Boolean(schoolId) });

  const handleExport = async (format: 'pdf' | 'xlsx') => {
    if (!detailQuery.data) return;
    setExporting(format);
    try {
      const response = await districtService.exportReport(detailQuery.data.exportReportType, format);
      saveBase64File(response.fileName, response.contentType, response.base64Content);
    } finally {
      setExporting(null);
    }
  };

  return (
    <DataShell isLoading={detailQuery.isLoading || analyticsQuery.isLoading} error={detailQuery.error ?? analyticsQuery.error}>
      {detailQuery.data && analyticsQuery.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-700">School Drill-down Dashboard</p>
                <h1 className="mt-2 text-3xl font-semibold text-slate-900">{detailQuery.data.schoolName}</h1>
                <p className="mt-3 text-sm text-slate-600">{detailQuery.data.district || 'District not set'} | {detailQuery.data.province || 'Province not set'} | {detailQuery.data.contactEmail || 'No contact email'}</p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Button onClick={() => handleExport('pdf')} disabled={exporting !== null} className="rounded-2xl px-4 py-2.5">
                  <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                  {exporting === 'pdf' ? 'Exporting PDF...' : 'Export school report'}
                </Button>
                <Button onClick={() => handleExport('xlsx')} disabled={exporting !== null} className="rounded-2xl bg-emerald-600 px-4 py-2.5 hover:bg-emerald-500">
                  <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                  {exporting === 'xlsx' ? 'Exporting Excel...' : 'Export Excel'}
                </Button>
              </div>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {detailQuery.data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="Subject Performance" points={detailQuery.data.subjectPerformance} />
            <ChartCard title="APS Readiness" points={detailQuery.data.apsBandDistribution} />
            <ChartCard title="Report Uploads" points={detailQuery.data.reportUploads} />
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <InsightList title="Teacher Activity" items={detailQuery.data.teacherActivity} icon={Users2} />
            <InsightList title="AI Recommendations" items={detailQuery.data.aiRecommendations} icon={Sparkles} />
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="Performance Trends" points={analyticsQuery.data.performanceTrends} />
            <ChartCard title="Grade / Class Comparison" points={analyticsQuery.data.gradePerformanceComparison} />
            <ChartCard title="Career Pathways" points={analyticsQuery.data.careerPathwayDistribution} />
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictAnalyticsPage = () => {
  const [searchParams] = useSearchParams();
  const query = useAppQuery({ queryKey: ['district-analytics'], queryFn: districtService.analytics });
  const activeView = searchParams.get('view');

  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">District Analytics</h1>
            <p className="mt-2 text-sm text-slate-600">
              {activeView === 'subject-gaps' ? 'Weak subject patterns across schools and grades.' :
                activeView === 'aps-readiness' ? 'APS readiness posture and school comparison.' :
                  activeView === 'career-pathways' ? 'Career pathway uptake across the district.' :
                    'District-wide performance trends, school ranking, reporting completion, and intervention demand.'}
            </p>
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="District Performance Trends" points={query.data.districtPerformanceTrends} />
            <ChartCard title="APS Band Distribution" points={query.data.apsBandDistribution} />
            <ChartCard title="Report Upload Completion" points={query.data.reportUploadCompletionProgress} />
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <ChartCard title="Subject Performance by School" points={query.data.subjectPerformanceBySchool} />
            <ChartCard title="Career Pathway Distribution" points={query.data.careerPathwayDistribution} />
            <ChartCard title="Grade / Class Comparison" points={query.data.gradeClassPerformanceComparison} />
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <InsightList title="School Ranking / Comparison" items={query.data.schoolRankingComparison} icon={Building2} />
            <ChartCard title="Learner Risk Distribution" points={query.data.learnerRiskDistribution} />
            <InsightList title="Urgent Interventions" items={query.data.urgentInterventions} icon={AlertTriangle} />
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictAIInsightsPage = () => {
  const query = useAppQuery({ queryKey: ['district-ai-insights'], queryFn: districtService.aiInsights });

  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">District AI Insights</h1>
            <p className="mt-2 text-sm text-slate-600">AI signals from live school data surface risk, readiness, gaps, and district-wide intervention priorities.</p>
          </div>

          {!query.data.dataAvailable ? (
            <EmptyState title="District AI Insights" message={query.data.emptyStateMessage || 'District AI insights are not available yet.'} />
          ) : (
            <div className="grid gap-5 xl:grid-cols-2">
              <InsightList title="Schools at Risk" items={query.data.schoolsAtRisk} icon={AlertTriangle} />
              <InsightList title="Learners at Risk by School" items={query.data.learnersAtRiskBySchool} icon={Users2} />
              <InsightList title="Weak Subjects Across District" items={query.data.weakSubjectsAcrossDistrict} icon={GraduationCap} />
              <InsightList title="APS Readiness Warnings" items={query.data.apsReadinessWarnings} icon={FileText} />
              <InsightList title="Career Pathway Gaps" items={query.data.careerPathwayGaps} icon={Sparkles} />
              <InsightList title="Bursary / Funding Indicators" items={query.data.bursaryFundingIndicators} icon={FileSpreadsheet} />
              <InsightList title="Teacher Activity Alerts" items={query.data.teacherActivityAlerts} icon={BellRing} />
              <InsightList title="Recommended District Interventions" items={query.data.recommendedDistrictInterventions} icon={Wrench} />
            </div>
          )}
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictReportsPage = () => {
  const [searchParams] = useSearchParams();
  const [exporting, setExporting] = useState<string | null>(null);
  const reportsQuery = useAppQuery({ queryKey: ['district-reports'], queryFn: districtService.reports });
  const settingsQuery = useAppQuery({ queryKey: ['district-settings-report-page'], queryFn: districtService.settings });
  const activeView = searchParams.get('view');

  const handleExport = async (type: string, format: 'pdf' | 'xlsx') => {
    setExporting(`${type}-${format}`);
    try {
      const response = await districtService.exportReport(type, format);
      saveBase64File(response.fileName, response.contentType, response.base64Content);
    } finally {
      setExporting(null);
    }
  };

  return (
    <DataShell isLoading={reportsQuery.isLoading || settingsQuery.isLoading} error={reportsQuery.error ?? settingsQuery.error}>
      {reportsQuery.data && settingsQuery.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">District Reports</h1>
            <p className="mt-2 text-sm text-slate-600">
              {activeView === 'announcements' ? 'District communication history and outbound announcements.' :
                activeView === 'school-reports' ? 'School-facing reporting catalogue and export actions.' :
                  'Export-ready district performance, readiness, subject gap, APS, career pathway, and intervention reports.'}
            </p>
          </div>

          {activeView === 'announcements' ? (
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <BellRing className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Message History</h3>
              </div>
              <div className="mt-4 space-y-3">
                {settingsQuery.data.announcements.length ? settingsQuery.data.announcements.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                  </div>
                )) : <EmptyState title="Announcements" message="No district announcements have been sent yet." />}
              </div>
            </div>
          ) : (
            <div className="grid gap-5 xl:grid-cols-2">
              {reportsQuery.data.map((report) => (
                <div key={report.key} className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h3 className="text-lg font-semibold text-slate-900">{report.title}</h3>
                      <p className="mt-2 text-sm text-slate-600">{report.description}</p>
                    </div>
                    <FileText className="h-5 w-5 text-blue-600" />
                  </div>
                  <div className="mt-5 flex flex-wrap gap-3">
                    <Button onClick={() => handleExport(report.key, 'pdf')} disabled={exporting !== null} className="rounded-2xl px-4 py-2.5">
                      <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                      {exporting === `${report.key}-pdf` ? 'Exporting PDF...' : 'Export PDF'}
                    </Button>
                    <Button onClick={() => handleExport(report.key, 'xlsx')} disabled={exporting !== null} className="rounded-2xl bg-emerald-600 px-4 py-2.5 hover:bg-emerald-500">
                      <ArrowDownToLine className="mr-2 inline h-4 w-4" />
                      {exporting === `${report.key}-xlsx` ? 'Exporting Excel...' : 'Export Excel'}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictInterventionsPage = () => {
  const [form, setForm] = useState({ title: '', category: 'ACADEMIC_SUPPORT', priority: 'MEDIUM', notes: '', targetScope: 'DISTRICT', schoolId: '', followUpDate: '' });
  const interventionsQuery = useAppQuery({ queryKey: ['district-interventions'], queryFn: districtService.interventions });
  const schoolsQuery = useAppQuery({ queryKey: ['district-schools-interventions'], queryFn: () => districtService.schools() });
  const createMutation = useMutation({
    mutationFn: districtService.createIntervention,
    onSuccess: () => {
      setForm({ title: '', category: 'ACADEMIC_SUPPORT', priority: 'MEDIUM', notes: '', targetScope: 'DISTRICT', schoolId: '', followUpDate: '' });
      interventionsQuery.refetch();
    },
  });

  return (
    <DataShell isLoading={interventionsQuery.isLoading || schoolsQuery.isLoading} error={interventionsQuery.error ?? schoolsQuery.error}>
      {interventionsQuery.data && schoolsQuery.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">District Interventions</h1>
            <p className="mt-2 text-sm text-slate-600">Track intervention load across schools and open district-level support actions.</p>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            {interventionsQuery.data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
          </div>

          <div className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <Wrench className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Open District Cases</h3>
              </div>
              <div className="mt-4 space-y-3">
                {interventionsQuery.data.items.length ? interventionsQuery.data.items.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.schoolName || 'District-wide'} • {item.category} • {item.priority}</p>
                    <p className="mt-2 text-sm text-slate-600">{item.notes}</p>
                  </div>
                )) : <EmptyState title="Interventions" message="No district interventions have been opened yet." />}
              </div>
            </div>

            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Create Intervention</h3>
              </div>
              <form
                className="mt-4 space-y-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  createMutation.mutate({
                    title: form.title,
                    category: form.category,
                    priority: form.priority,
                    notes: form.notes,
                    targetScope: form.targetScope,
                    schoolId: form.schoolId || undefined,
                    followUpDate: form.followUpDate || undefined,
                  });
                }}
              >
                <Input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} className="rounded-2xl border-slate-200 bg-slate-50 py-3" placeholder="Intervention title" required />
                <div className="grid gap-3 md:grid-cols-2">
                  <select value={form.category} onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="ACADEMIC_SUPPORT">Academic support</option>
                    <option value="REPORTING_ESCALATION">Reporting escalation</option>
                    <option value="TEACHER_SUPPORT">Teacher support</option>
                    <option value="READINESS_PROGRAMME">Readiness programme</option>
                  </select>
                  <select value={form.priority} onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <select value={form.targetScope} onChange={(event) => setForm((current) => ({ ...current, targetScope: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="DISTRICT">District-wide</option>
                    <option value="SELECTED_SCHOOL">Selected school</option>
                  </select>
                  <select value={form.schoolId} onChange={(event) => setForm((current) => ({ ...current, schoolId: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="">No school selected</option>
                    {schoolsQuery.data.items.map((school) => <option key={school.schoolId} value={school.schoolId}>{school.schoolName}</option>)}
                  </select>
                </div>
                <Input type="date" value={form.followUpDate} onChange={(event) => setForm((current) => ({ ...current, followUpDate: event.target.value }))} className="rounded-2xl border-slate-200 bg-slate-50 py-3" />
                <textarea value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} className="min-h-32 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="Intervention notes" required />
                {createMutation.error ? <ErrorState message={createMutation.error instanceof Error ? createMutation.error.message : 'Unable to create intervention.'} /> : null}
                <Button type="submit" disabled={createMutation.isPending} className="rounded-2xl px-5 py-3">
                  {createMutation.isPending ? 'Creating intervention...' : 'Create intervention'}
                </Button>
              </form>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictSettingsPage = () => {
  const [searchParams] = useSearchParams();
  const settingsQuery = useAppQuery({ queryKey: ['district-settings'], queryFn: districtService.settings });
  const schoolsQuery = useAppQuery({ queryKey: ['district-schools-settings'], queryFn: () => districtService.schools() });
  const [form, setForm] = useState({ audience: 'ALL_SCHOOLS', title: '', message: '', deliveryScope: 'ALL_SCHOOLS', schoolId: '' });
  const createMutation = useMutation({
    mutationFn: districtService.createAnnouncement,
    onSuccess: () => {
      setForm({ audience: 'ALL_SCHOOLS', title: '', message: '', deliveryScope: 'ALL_SCHOOLS', schoolId: '' });
      settingsQuery.refetch();
    },
  });
  const activeView = searchParams.get('view');

  return (
    <DataShell isLoading={settingsQuery.isLoading || schoolsQuery.isLoading} error={settingsQuery.error ?? schoolsQuery.error}>
      {settingsQuery.data && schoolsQuery.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">District Settings</h1>
            <p className="mt-2 text-sm text-slate-600">
              {activeView === 'support'
                ? 'Review support requests from schools and operational health signals.'
                : 'District profile, licensing posture, security access, linked school settings, and communication controls.'}
            </p>
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm xl:col-span-1">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-700">District Profile</p>
              <div className="mt-4 space-y-3 text-sm text-slate-700">
                <div><span className="font-semibold text-slate-900">Name:</span> {settingsQuery.data.districtName}</div>
                <div><span className="font-semibold text-slate-900">Code:</span> {settingsQuery.data.districtCode || 'Not set'}</div>
                <div><span className="font-semibold text-slate-900">Province:</span> {settingsQuery.data.province || 'Not set'}</div>
                <div><span className="font-semibold text-slate-900">Contact email:</span> {settingsQuery.data.contactEmail || 'Not set'}</div>
                <div><span className="font-semibold text-slate-900">Contact phone:</span> {settingsQuery.data.contactPhone || 'Not set'}</div>
                <div><span className="font-semibold text-slate-900">Licensing:</span> {settingsQuery.data.licensingStatus}</div>
              </div>
            </div>

            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm xl:col-span-2">
              <div className="flex items-center gap-2">
                <BellRing className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Send Announcement</h3>
              </div>
              <form
                className="mt-4 space-y-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  createMutation.mutate({
                    audience: form.audience,
                    title: form.title,
                    message: form.message,
                    deliveryScope: form.deliveryScope,
                    schoolId: form.schoolId || undefined,
                  });
                }}
              >
                <div className="grid gap-3 md:grid-cols-2">
                  <select value={form.audience} onChange={(event) => setForm((current) => ({ ...current, audience: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="ALL_SCHOOLS">All schools</option>
                    <option value="SCHOOL_ADMINS">School admins</option>
                    <option value="TEACHERS">Teachers</option>
                  </select>
                  <select value={form.deliveryScope} onChange={(event) => setForm((current) => ({ ...current, deliveryScope: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="ALL_SCHOOLS">All schools</option>
                    <option value="SELECTED_SCHOOL">Selected school</option>
                  </select>
                </div>
                <select value={form.schoolId} onChange={(event) => setForm((current) => ({ ...current, schoolId: event.target.value }))} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">No school selected</option>
                  {schoolsQuery.data.items.map((school) => <option key={school.schoolId} value={school.schoolId}>{school.schoolName}</option>)}
                </select>
                <Input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} className="rounded-2xl border-slate-200 bg-slate-50 py-3" placeholder="Announcement title" required />
                <textarea value={form.message} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} className="min-h-28 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="Message to schools" required />
                {createMutation.error ? <ErrorState message={createMutation.error instanceof Error ? createMutation.error.message : 'Unable to send announcement.'} /> : null}
                <Button type="submit" disabled={createMutation.isPending} className="rounded-2xl px-5 py-3">
                  {createMutation.isPending ? 'Sending announcement...' : 'Send announcement'}
                </Button>
              </form>
            </div>
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <LifeBuoy className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Support Requests</h3>
              </div>
              <div className="mt-4 space-y-3">
                {settingsQuery.data.supportRequests.length ? settingsQuery.data.supportRequests.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.schoolName} • {item.title}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                  </div>
                )) : <EmptyState title="Support requests" message="No support requests are waiting." />}
              </div>
            </div>
            <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-semibold text-slate-900">Security / Audit</h3>
              </div>
              <div className="mt-4 space-y-3">
                {settingsQuery.data.recentAuditLogs.length ? settingsQuery.data.recentAuditLogs.map((item, index) => (
                  <div key={`${item.action}-${index}`} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{item.action}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.createdAt}</p>
                  </div>
                )) : <EmptyState title="Audit log" message="District audit activity will appear here." />}
              </div>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictCurriculumManagementPage = () => {
  const { view = 'atp-repository' } = useParams();
  const meta = curriculumViewMeta[view] ?? curriculumViewMeta['atp-repository'];
  const PageIcon = meta.icon;
  const repositoryType = meta.repositoryType;
  const [editingAssetId, setEditingAssetId] = useState<string | null>(null);
  const [form, setForm] = useState({
    repositoryType: repositoryType ?? 'ATP',
    title: '',
    subject: 'Mathematics',
    grade: 'Grade 4',
    curriculumPhase: 'Intermediate Phase',
    academicYear: new Date().getFullYear(),
    province: 'Gauteng',
    versionNumber: 'v1.0',
    description: '',
    term: 'Term 1',
    weekNumber: 1,
  });
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [docxFile, setDocxFile] = useState<File | null>(null);
  const [excelFile, setExcelFile] = useState<File | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const assetsQuery = useAppQuery({
    queryKey: ['district-curriculum-assets', repositoryType],
    queryFn: () => districtService.curriculumAssets(repositoryType),
    enabled: Boolean(repositoryType),
  });
  const calendarQuery = useAppQuery({
    queryKey: ['district-curriculum-calendar'],
    queryFn: districtService.curriculumCalendar,
    enabled: view === 'curriculum-calendar',
  });
  const complianceQuery = useAppQuery({
    queryKey: ['district-curriculum-compliance', view],
    queryFn: districtService.curriculumCompliance,
    enabled: ['weekly-coverage-tracker', 'teacher-reminders', 'curriculum-compliance'].includes(view),
  });

  const resetForm = () => {
    setEditingAssetId(null);
    setFormError(null);
    setForm({
      repositoryType: repositoryType ?? 'ATP',
      title: '',
      subject: 'Mathematics',
      grade: 'Grade 4',
      curriculumPhase: 'Intermediate Phase',
      academicYear: new Date().getFullYear(),
      province: 'Gauteng',
      versionNumber: 'v1.0',
      description: '',
      term: 'Term 1',
      weekNumber: 1,
    });
    setPdfFile(null);
    setDocxFile(null);
    setExcelFile(null);
  };

  const upsertMutation = useMutation({
    mutationFn: async () => {
      if (!form.title.trim()) {
        throw new Error('Document title is required.');
      }
      if (!pdfFile && !docxFile && !excelFile && !editingAssetId) {
        throw new Error('Select at least one file to upload.');
      }
      const payload: DistrictCurriculumAssetUpsert = {
        repositoryType: repositoryType ?? form.repositoryType,
        title: form.title,
        subject: form.subject,
        grade: form.grade,
        curriculumPhase: form.curriculumPhase,
        academicYear: Number(form.academicYear),
        province: form.province,
        versionNumber: form.versionNumber,
        description: form.description,
        term: ['ATP', 'LESSON_PLAN'].includes(repositoryType ?? '') ? form.term : undefined,
        weekNumber: repositoryType === 'LESSON_PLAN' ? Number(form.weekNumber) : undefined,
        pdf: await fileToPayload(pdfFile),
        docx: await fileToPayload(docxFile),
        excel: await fileToPayload(excelFile),
      };
      return editingAssetId
        ? districtService.updateCurriculumAsset(editingAssetId, payload)
        : districtService.createCurriculumAsset(payload);
    },
    onSuccess: async () => {
      resetForm();
      await assetsQuery.refetch();
      await calendarQuery.refetch();
      await complianceQuery.refetch();
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'Unable to save curriculum asset.');
    },
  });
  const archiveMutation = useMutation({
    mutationFn: (assetId: string) => districtService.archiveCurriculumAsset(assetId),
    onSuccess: () => {
      assetsQuery.refetch();
      complianceQuery.refetch();
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (assetId: string) => districtService.deleteCurriculumAsset(assetId),
    onSuccess: () => {
      assetsQuery.refetch();
      complianceQuery.refetch();
    },
  });
  const downloadMutation = useMutation({
    mutationFn: ({ assetId, format }: { assetId: string; format: 'PDF' | 'DOCX' | 'EXCEL' }) => districtService.downloadCurriculumAsset(assetId, format),
    onSuccess: (data) => saveBlobFile(data.fileName, data.blob),
  });
  const extractMutation = useMutation({
    mutationFn: (assetId: string) => districtService.extractCurriculumAtp(assetId),
    onSuccess: async () => {
      await assetsQuery.refetch();
      await calendarQuery.refetch();
    },
  });
  const publishCalendarMutation = useMutation({
    mutationFn: (itemId: string) => districtService.publishCurriculumCalendarItem(itemId),
    onSuccess: async () => {
      await calendarQuery.refetch();
      await complianceQuery.refetch();
    },
  });
  const syncPublishedCalendarMutation = useMutation({
    mutationFn: () => districtService.syncPublishedCurriculumCalendar(),
    onSuccess: async () => {
      await calendarQuery.refetch();
      await complianceQuery.refetch();
    },
  });

  useEffect(() => {
    resetForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view]);

  const startEdit = (asset: DistrictCurriculumAsset) => {
    setEditingAssetId(asset.id);
    setFormError(null);
    setForm({
      repositoryType: asset.repositoryType,
      title: asset.title,
      subject: asset.subject,
      grade: asset.grade,
      curriculumPhase: asset.curriculumPhase ?? '',
      academicYear: asset.academicYear ?? new Date().getFullYear(),
      province: asset.province ?? '',
      versionNumber: asset.versionNumber ?? 'v1.0',
      description: asset.description ?? '',
      term: asset.term ?? 'Term 1',
      weekNumber: asset.weekNumber ?? 1,
    });
    setPdfFile(null);
    setDocxFile(null);
    setExcelFile(null);
  };

  const syncTitleFromFile = (file: File | null) => {
    if (!file) {
      return;
    }
    setFormError(null);
    setForm((current) => current.title.trim() ? current : { ...current, title: deriveTitleFromFileName(file.name) });
  };

  const renderAssetTable = (items: DistrictCurriculumAsset[]) => (
    <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Title</th>
              <th className="px-4 py-3 font-medium">Subject / Grade</th>
              <th className="px-4 py-3 font-medium">Version</th>
              <th className="px-4 py-3 font-medium">Source</th>
              <th className="px-4 py-3 font-medium">Files</th>
              <th className="px-4 py-3 font-medium">AI Extraction</th>
              <th className="px-4 py-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.length ? items.map((item) => (
              <tr key={item.id} className="border-t border-slate-100 align-top">
                <td className="px-4 py-3">
                  <p className="font-semibold text-slate-900">{item.title}</p>
                  <p className="text-xs text-slate-500">{item.uploadedBy} · {new Date(item.uploadDate).toLocaleDateString()}</p>
                </td>
                <td className="px-4 py-3">
                  <p className="text-slate-900">{item.subject}</p>
                  <p className="text-xs text-slate-500">{item.grade} · {item.curriculumPhase || 'Phase not set'}</p>
                </td>
                <td className="px-4 py-3">{item.versionNumber || 'v1.0'}</td>
                <td className="px-4 py-3"><span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700">{item.badge}</span></td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    {item.pdfAvailable ? <Button type="button" onClick={() => downloadMutation.mutate({ assetId: item.id, format: 'PDF' })} className="rounded-xl px-3 py-2 text-xs"><FileText className="mr-2 h-3.5 w-3.5" />PDF</Button> : null}
                    {item.docxAvailable ? <Button type="button" onClick={() => downloadMutation.mutate({ assetId: item.id, format: 'DOCX' })} className="rounded-xl bg-slate-900 px-3 py-2 text-xs hover:bg-slate-800"><FileText className="mr-2 h-3.5 w-3.5" />DOCX</Button> : null}
                    {item.excelAvailable ? <Button type="button" onClick={() => downloadMutation.mutate({ assetId: item.id, format: 'EXCEL' })} className="rounded-xl bg-emerald-600 px-3 py-2 text-xs hover:bg-emerald-500"><FileSpreadsheet className="mr-2 h-3.5 w-3.5" />Excel</Button> : null}
                  </div>
                </td>
                <td className="px-4 py-3">
                  {item.repositoryType === 'ATP' ? (
                    <div className="space-y-2">
                      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${item.extractionStatus === 'EXTRACTION_FAILED' ? 'bg-rose-50 text-rose-700' : item.extractionStatus === 'PUBLISHED' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
                        {item.extractionStatus ?? 'PENDING'}
                      </span>
                      <Button type="button" onClick={() => extractMutation.mutate(item.id)} className="rounded-xl px-3 py-2 text-xs" disabled={extractMutation.isPending || !item.pdfAvailable}>
                        {extractMutation.isPending ? 'Extracting...' : 'Run AI Extract'}
                      </Button>
                    </div>
                  ) : <span className="text-xs text-slate-400">Not applicable</span>}
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" onClick={() => startEdit(item)} className="rounded-xl px-3 py-2 text-xs">Edit</Button>
                    <Button type="button" onClick={() => archiveMutation.mutate(item.id)} className="rounded-xl bg-amber-500 px-3 py-2 text-xs hover:bg-amber-400">Archive</Button>
                    <Button type="button" onClick={() => deleteMutation.mutate(item.id)} className="rounded-xl bg-rose-600 px-3 py-2 text-xs hover:bg-rose-500"><Trash2 className="mr-2 h-3.5 w-3.5" />Delete</Button>
                  </div>
                </td>
              </tr>
            )) : (
              <tr>
                <td colSpan={7} className="px-4 py-8"><EmptyState title={meta.title} message="No curriculum assets uploaded for this repository yet." /></td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderUploadPanel = () => (
    <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">{editingAssetId ? 'Edit / Replace upload' : 'Upload official district content'}</h3>
          <p className="mt-1 text-sm text-slate-600">District content remains the official reference schools and teachers see first.</p>
        </div>
        {editingAssetId ? <Button type="button" onClick={resetForm} className="rounded-2xl bg-slate-200 px-4 py-2 text-slate-900 hover:bg-slate-300">Cancel edit</Button> : null}
      </div>
      <form
        className="mt-4 grid gap-3 md:grid-cols-2"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          setFormError(null);
          upsertMutation.mutate();
        }}
      >
        <div className="md:col-span-1">
          <Input value={form.title} onChange={(event) => {
            setFormError(null);
            setForm((current) => ({ ...current, title: event.target.value }));
          }} placeholder="Document title" className="rounded-2xl" required />
          <p className="mt-1 text-xs text-slate-500">Required. If left blank, selecting a file now fills it automatically.</p>
        </div>
        <Input value={form.subject} onChange={(event) => setForm((current) => ({ ...current, subject: event.target.value }))} placeholder="Subject" className="rounded-2xl" required />
        <Input value={form.grade} onChange={(event) => setForm((current) => ({ ...current, grade: event.target.value }))} placeholder="Grade" className="rounded-2xl" required />
        <Input value={form.curriculumPhase} onChange={(event) => setForm((current) => ({ ...current, curriculumPhase: event.target.value }))} placeholder="Curriculum phase" className="rounded-2xl" />
        <Input type="number" value={String(form.academicYear)} onChange={(event) => setForm((current) => ({ ...current, academicYear: Number(event.target.value) }))} placeholder="Academic year" className="rounded-2xl" />
        <Input value={form.province} onChange={(event) => setForm((current) => ({ ...current, province: event.target.value }))} placeholder="Province" className="rounded-2xl" />
        <Input value={form.versionNumber} onChange={(event) => setForm((current) => ({ ...current, versionNumber: event.target.value }))} placeholder="Version number" className="rounded-2xl" />
        <Input value={form.term} onChange={(event) => setForm((current) => ({ ...current, term: event.target.value }))} placeholder="Term" className="rounded-2xl" />
        {repositoryType === 'LESSON_PLAN' ? <Input type="number" value={String(form.weekNumber)} onChange={(event) => setForm((current) => ({ ...current, weekNumber: Number(event.target.value) }))} placeholder="Week number" className="rounded-2xl" /> : null}
        <label className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">PDF<input type="file" accept=".pdf" className="mt-2 block w-full" onChange={(event) => {
          const file = event.target.files?.[0] ?? null;
          setPdfFile(file);
          syncTitleFromFile(file);
        }} /></label>
        <label className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">DOCX<input type="file" accept=".doc,.docx" className="mt-2 block w-full" onChange={(event) => {
          const file = event.target.files?.[0] ?? null;
          setDocxFile(file);
          syncTitleFromFile(file);
        }} /></label>
        <label className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">Excel<input type="file" accept=".xls,.xlsx,.csv" className="mt-2 block w-full" onChange={(event) => {
          const file = event.target.files?.[0] ?? null;
          setExcelFile(file);
          syncTitleFromFile(file);
        }} /></label>
        <textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} className="min-h-28 rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500 md:col-span-2" placeholder="Description or notes" />
        {formError || upsertMutation.error ? <div className="md:col-span-2"><ErrorState message={formError ?? (upsertMutation.error instanceof Error ? upsertMutation.error.message : 'Unable to save curriculum asset.')} /></div> : null}
        <div className="md:col-span-2">
          <Button type="submit" disabled={upsertMutation.isPending} className="rounded-2xl px-5 py-3">
            {upsertMutation.isPending ? 'Saving curriculum content...' : editingAssetId ? 'Save changes' : 'Upload official content'}
          </Button>
        </div>
      </form>
    </div>
  );

  const renderComplianceView = (data: DistrictCurriculumComplianceResponse) => (
    <div className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {data.metrics.map((item) => <StatCard key={item.label} item={item} />)}
      </div>
      <div className="grid gap-5 xl:grid-cols-[1fr_1fr]">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-blue-600" />
            <h3 className="text-sm font-semibold text-slate-900">School Compliance</h3>
          </div>
          <div className="mt-4 space-y-3">
            {data.schools.length ? data.schools.map((item) => (
              <div key={item.schoolId} className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-slate-900">{item.schoolName}</p>
                  <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${item.compliancePercent >= 85 ? 'bg-emerald-50 text-emerald-700' : item.compliancePercent >= 60 ? 'bg-amber-50 text-amber-700' : 'bg-rose-50 text-rose-700'}`}>{item.compliancePercent}%</span>
                </div>
                <p className="mt-1 text-sm text-slate-600">{item.status} · {item.teachersBehind} teachers behind · {item.subjectsBehind} subjects behind</p>
              </div>
            )) : <EmptyState title="School compliance" message="No teacher assignment coverage is available yet." />}
          </div>
        </div>
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-600" />
            <h3 className="text-sm font-semibold text-slate-900">Curriculum Risk Alerts</h3>
          </div>
          <div className="mt-4 space-y-3">
            {data.riskAlerts.length ? data.riskAlerts.map((item) => (
              <div key={item.id} className="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3">
                <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                <p className="mt-1 text-sm text-slate-700">{item.schoolName} · {item.teacherName} · {item.subject}</p>
                <p className="mt-1 text-sm text-slate-700">{item.detail}</p>
              </div>
            )) : <EmptyState title="Curriculum alerts" message="No curriculum risk alerts are open." />}
          </div>
        </div>
      </div>
      <div className="grid gap-5 xl:grid-cols-[1fr_1fr]">
        <InsightList title="Subjects Behind Schedule" items={data.subjectsBehindSchedule} icon={BookOpen} />
        <InsightList title="Teachers Behind Schedule" items={data.teachersBehindSchedule} icon={Users2} />
      </div>
      <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2">
          <BarChart3 className="h-4 w-4 text-blue-600" />
          <h3 className="text-sm font-semibold text-slate-900">Curriculum Heat Map</h3>
        </div>
        <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {data.heatMap.length ? data.heatMap.map((item) => (
            <div key={`${item.schoolName}-${item.subject}`} className={`rounded-2xl border px-4 py-3 ${item.tone === 'positive' ? 'border-emerald-200 bg-emerald-50' : item.tone === 'warning' ? 'border-amber-200 bg-amber-50' : 'border-rose-200 bg-rose-50'}`}>
              <p className="text-sm font-semibold text-slate-900">{item.schoolName}</p>
              <p className="mt-1 text-sm text-slate-700">{item.subject}</p>
              <p className="mt-1 text-xs uppercase tracking-[0.12em] text-slate-500">{item.status} · {item.compliancePercent}%</p>
            </div>
          )) : <EmptyState title="Heat map" message="No subject coverage heat map is available yet." />}
        </div>
      </div>
    </div>
  );

  return (
    <section className="space-y-6">
      <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <PageIcon className="h-5 w-5 text-blue-600" />
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">{meta.title}</h1>
            <p className="mt-2 text-sm text-slate-600">{meta.subtitle}</p>
          </div>
        </div>
      </div>

      {repositoryType ? (
        <>
          {renderUploadPanel()}
          <DataShell isLoading={assetsQuery.isLoading} error={assetsQuery.error}>
            {assetsQuery.data ? renderAssetTable(assetsQuery.data) : null}
          </DataShell>
        </>
      ) : null}

      {view === 'curriculum-calendar' ? (
        <DataShell isLoading={calendarQuery.isLoading} error={calendarQuery.error}>
          {calendarQuery.data ? (
            <div className="space-y-5">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
                <StatCard item={{ label: 'ATPs Processed', value: String(calendarQuery.data.stats.atpsProcessed), helperText: 'ATP PDFs extracted into draft calendars.', tone: 'positive' }} />
                <StatCard item={{ label: 'Calendar Items Generated', value: String(calendarQuery.data.stats.calendarItemsGenerated), helperText: 'Weekly items currently mapped.', tone: 'neutral' }} />
                <StatCard item={{ label: 'Published Topics', value: String(calendarQuery.data.stats.publishedTopics), helperText: 'Visible to schools and teachers.', tone: 'positive' }} />
                <StatCard item={{ label: 'Draft Topics', value: String(calendarQuery.data.stats.draftTopics), helperText: 'Awaiting district review.', tone: 'warning' }} />
                <StatCard item={{ label: 'Teacher Reminders Created', value: String(calendarQuery.data.stats.teacherRemindersCreated), helperText: 'Automation records created from published ATPs.', tone: 'neutral' }} />
              </div>
              <div className="flex justify-end">
                <Button type="button" className="rounded-xl px-4 py-2 text-sm" onClick={() => syncPublishedCalendarMutation.mutate()} disabled={syncPublishedCalendarMutation.isPending}>
                  {syncPublishedCalendarMutation.isPending ? 'Syncing...' : 'Sync Published Calendar to Schools'}
                </Button>
              </div>
              {calendarQuery.data.extractionErrors.length ? (
                <div className="rounded-[28px] border border-rose-200 bg-rose-50 p-5 shadow-sm">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="h-4 w-4 text-rose-600" />
                    <h3 className="text-sm font-semibold text-slate-900">Extraction Errors</h3>
                  </div>
                  <div className="mt-4 space-y-3">
                    {calendarQuery.data.extractionErrors.map((asset) => (
                      <div key={asset.id} className="rounded-2xl border border-rose-100 bg-white px-4 py-3">
                        <p className="text-sm font-semibold text-slate-900">{asset.title}</p>
                        <p className="mt-1 text-sm text-slate-700">{asset.extractionError || 'ATP uploaded but no weekly topics were extracted.'}</p>
                        <div className="mt-3">
                          <Button type="button" className="rounded-xl px-3 py-2 text-xs" onClick={() => extractMutation.mutate(asset.id)} disabled={extractMutation.isPending}>
                            Generate AI Calendar
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {calendarQuery.data.items.length ? calendarQuery.data.items.map((item: DistrictCurriculumCalendarItem) => (
                  <div key={item.id} className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">{item.subject} · {item.grade}</p>
                        <p className="mt-1 text-xs uppercase tracking-[0.14em] text-slate-500">{item.term} · Week {item.weekNumber} · {item.status}</p>
                      </div>
                      <CalendarDays className="h-5 w-5 text-blue-600" />
                    </div>
                    <p className="mt-4 text-lg font-semibold text-slate-900">{item.topic}</p>
                    <p className="mt-1 text-sm text-slate-600">{item.subtopic || 'Manual subtopic review required before publish.'}</p>
                    <div className="mt-4 grid gap-3 text-sm text-slate-700">
                      <div className="grid gap-2 sm:grid-cols-2">
                        <p><span className="font-semibold text-slate-900">Phase:</span> {item.phase || 'Not set'}</p>
                        <p><span className="font-semibold text-slate-900">Academic Year:</span> {item.academicYear ?? 'Not set'}</p>
                        <p><span className="font-semibold text-slate-900">Province:</span> {item.province || 'Not set'}</p>
                        <p><span className="font-semibold text-slate-900">Source ATP:</span> {item.sourceTitle}</p>
                        <p className="sm:col-span-2"><span className="font-semibold text-slate-900">Dates:</span> {item.startDate ? new Date(item.startDate).toLocaleDateString() : 'Date pending'} {item.endDate ? `- ${new Date(item.endDate).toLocaleDateString()}` : ''}</p>
                      </div>
                      <p><span className="font-semibold text-slate-900">Objectives:</span> {item.learningObjectives || 'Missing structured objectives. Regenerate or edit before publish.'}</p>
                      <p><span className="font-semibold text-slate-900">Activities:</span> {item.lessonFocus || 'Missing suggested activities. Regenerate or edit before publish.'}</p>
                      <p><span className="font-semibold text-slate-900">Assessment:</span> {item.assessmentTask || 'Missing assessment task. Regenerate or edit before publish.'}</p>
                      <p><span className="font-semibold text-slate-900">Resources / Media:</span> {item.resources || 'Missing resources or media guidance.'}</p>
                      {!item.publishable ? (
                        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-3 py-2 text-amber-900">
                          This ATP row is too weak to publish. Regenerate the AI calendar or edit the item so it includes structured objectives, activities, assessment, or resources.
                        </div>
                      ) : null}
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                      <Button type="button" className="rounded-xl px-3 py-2 text-xs" onClick={() => publishCalendarMutation.mutate(item.id)} disabled={publishCalendarMutation.isPending || item.status === 'PUBLISHED' || !item.publishable}>
                        {item.status === 'PUBLISHED' ? 'Published' : 'Publish Calendar'}
                      </Button>
                    </div>
                  </div>
                )) : <EmptyState title="Curriculum calendar" message="No ATP planner milestones are available yet." />}
              </div>
            </div>
          ) : null}
        </DataShell>
      ) : null}

      {['weekly-coverage-tracker', 'teacher-reminders', 'curriculum-compliance'].includes(view) ? (
        <DataShell isLoading={complianceQuery.isLoading} error={complianceQuery.error}>
          {complianceQuery.data ? (
            <>
              {view === 'teacher-reminders' ? (
                <div className="grid gap-5 xl:grid-cols-[0.8fr_1.2fr]">
                  <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
                    <h3 className="text-sm font-semibold text-slate-900">Reminder Cadence</h3>
                    <div className="mt-4 space-y-3 text-sm text-slate-700">
                      <div className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"><p className="font-semibold text-slate-900">Monday 06:00</p><p className="mt-1">Week-specific ATP start reminder for each teacher subject.</p></div>
                      <div className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"><p className="font-semibold text-slate-900">Wednesday</p><p className="mt-1">Mid-week curriculum progress check reminder.</p></div>
                      <div className="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"><p className="font-semibold text-slate-900">Friday</p><p className="mt-1">Prompt teachers to mark the week topic as completed.</p></div>
                    </div>
                  </div>
                  <div>{renderComplianceView(complianceQuery.data)}</div>
                </div>
              ) : renderComplianceView(complianceQuery.data)}
            </>
          ) : null}
        </DataShell>
      ) : null}
    </section>
  );
};

const SimpleMetricSummary = ({ metrics }: { metrics: DistrictMetricCard[] }) => (
  <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
    {metrics.map((item) => (
      <DashboardKpiCard
        key={item.label}
        label={item.label}
        value={item.value}
        helperText={item.helperText}
        tone={item.tone}
        actionLabel="Open"
      />
    ))}
  </div>
);

export const DistrictCircuitDashboardPage = () => {
  const dashboardQuery = useAppQuery({ queryKey: ['district-circuit-dashboard'], queryFn: districtService.circuitDashboard });
  const schoolsQuery = useAppQuery({ queryKey: ['district-circuit-schools'], queryFn: districtService.circuitSchools });
  const curriculumQuery = useAppQuery({ queryKey: ['district-circuit-curriculum'], queryFn: () => districtService.circuitCurriculum() });
  const visitsQuery = useAppQuery({ queryKey: ['district-circuit-visits'], queryFn: districtService.circuitVisits });
  const supportQuery = useAppQuery({ queryKey: ['district-circuit-support-requests'], queryFn: districtService.circuitSupportRequests });
  const interventionsQuery = useAppQuery({ queryKey: ['district-circuit-interventions'], queryFn: districtService.circuitInterventions });
  return (
    <DataShell
      isLoading={dashboardQuery.isLoading || schoolsQuery.isLoading || curriculumQuery.isLoading || visitsQuery.isLoading || supportQuery.isLoading || interventionsQuery.isLoading}
      error={dashboardQuery.error ?? schoolsQuery.error ?? curriculumQuery.error ?? visitsQuery.error ?? supportQuery.error ?? interventionsQuery.error}
    >
      {dashboardQuery.data && schoolsQuery.data ? (
        <DashboardShell>
          <DashboardSectionCard title={dashboardQuery.data.title} subtitle={dashboardQuery.data.subtitle}>
            <SimpleMetricSummary metrics={dashboardQuery.data.metrics} />
          </DashboardSectionCard>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr_1fr]">
            <div className="space-y-4 xl:col-span-2">
              <div className="grid gap-4 xl:grid-cols-3">
                <ChartCard title="ATP Compliance Overview" points={schoolsQuery.data.items.map((item) => ({ label: item.schoolName, value: item.atpCompliance, tone: item.atpCompliance >= 85 ? 'positive' : item.atpCompliance >= 60 ? 'warning' : 'critical' }))} />
                <ChartCard title="Schools At Risk" points={schoolsQuery.data.items.map((item) => ({ label: item.schoolName, value: item.riskStatus.toLowerCase().includes('high') ? 100 : item.riskStatus.toLowerCase().includes('medium') ? 60 : 30, tone: item.riskStatus }))} />
                <ChartCard title="Upcoming School Visits" points={(visitsQuery.data ?? []).slice(0, 6).map((item) => ({ label: item.schoolName, value: 100, tone: item.status }))} />
              </div>
              <CompactDataTable
                columns={['School', 'Subject', 'Grade', 'Expected', 'Actual', 'Weeks Behind', 'Status']}
                emptyState={!(curriculumQuery.data?.items ?? []).length ? <EmptyStateCompact title="Curriculum Monitoring" message="No ATP progress has been submitted yet." /> : undefined}
              >
                {(curriculumQuery.data?.items ?? []).slice(0, 8).map((item) => (
                  <tr key={`${item.schoolId}-${item.subject}-${item.grade}-${item.expectedWeek}`} className="h-11">
                    <td className="px-4 py-3 font-semibold text-slate-900">{item.schoolName}</td>
                    <td className="px-4 py-3">{item.subject}</td>
                    <td className="px-4 py-3">{item.grade}</td>
                    <td className="px-4 py-3">{item.expectedWeek}</td>
                    <td className="px-4 py-3">{item.actualWeek}</td>
                    <td className="px-4 py-3">{item.weeksBehind}</td>
                    <td className="px-4 py-3"><RiskBadge label={item.weeksBehind >= 3 ? 'Curriculum Risk' : item.status} /></td>
                  </tr>
                ))}
              </CompactDataTable>
            </div>
            <div className="space-y-4">
              <DashboardSectionCard title="Recent Support Requests" subtitle="Newest school support requests in this circuit.">
                <div className="space-y-3">
                  {(supportQuery.data ?? []).slice(0, 5).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="text-[13px] font-semibold text-slate-900">{item.schoolName}</p>
                          <p className="mt-1 text-[12px] text-slate-500">{item.requestType}</p>
                        </div>
                        <RiskBadge label={item.status} />
                      </div>
                    </div>
                  ))}
                  {!(supportQuery.data ?? []).length ? <EmptyStateCompact title="Support Requests" message="No support requests are waiting." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Quick Actions" subtitle="Most-used circuit manager actions.">
                <div className="grid gap-3">
                  <QuickActionButton label="Schedule School Visit" icon={CalendarDays} />
                  <QuickActionButton label="Create Intervention" icon={Wrench} />
                  <QuickActionButton label="View Reports" icon={FileText} />
                  <QuickActionButton label="Send Message" icon={BellRing} />
                  <QuickActionButton label="Create Announcement" icon={BellRing} />
                  <QuickActionButton label="Upload Resource" icon={BookOpen} />
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Intervention Overview" subtitle="Open intervention load across the circuit.">
                <div className="space-y-3">
                  {(interventionsQuery.data ?? []).slice(0, 4).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-[12px] text-slate-500">{item.schoolName || 'District-wide'} · {item.priority}</p>
                      <ProgressBar value={item.priority === 'CRITICAL' ? 100 : item.priority === 'HIGH' ? 75 : item.priority === 'MEDIUM' ? 50 : 25} tone={item.priority} />
                    </div>
                  ))}
                  {!(interventionsQuery.data ?? []).length ? <EmptyStateCompact title="Interventions" message="No interventions are open." /> : null}
                </div>
              </DashboardSectionCard>
            </div>
          </div>
        </DashboardShell>
      ) : null}
    </DataShell>
  );
};

export const DistrictDirectorDashboardPage = () => {
  const dashboardQuery = useAppQuery({ queryKey: ['district-director-dashboard'], queryFn: districtService.districtDirectorDashboard });
  const schoolsQuery = useAppQuery({ queryKey: ['district-schools-director'], queryFn: () => districtService.schools() });
  const complianceQuery = useAppQuery({ queryKey: ['district-director-compliance'], queryFn: districtService.curriculumCompliance });
  const settingsQuery = useAppQuery({ queryKey: ['district-settings-director'], queryFn: districtService.settings });
  return (
    <DataShell
      isLoading={dashboardQuery.isLoading || schoolsQuery.isLoading || complianceQuery.isLoading || settingsQuery.isLoading}
      error={dashboardQuery.error ?? schoolsQuery.error ?? complianceQuery.error ?? settingsQuery.error}
    >
      {dashboardQuery.data && schoolsQuery.data && complianceQuery.data && settingsQuery.data ? (
        <DashboardShell>
          <DashboardSectionCard title={dashboardQuery.data.title} subtitle={dashboardQuery.data.subtitle}>
            <SimpleMetricSummary metrics={dashboardQuery.data.metrics} />
          </DashboardSectionCard>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr_1fr]">
            <div className="space-y-4 xl:col-span-2">
              <div className="grid gap-4 xl:grid-cols-3">
                <ChartCard title="District Pass Rate" points={schoolsQuery.data.items.slice(0, 8).map((item) => ({ label: item.schoolName, value: item.averageApsScore, tone: item.riskLevel }))} />
                <ChartCard title="ATP Compliance %" points={complianceQuery.data.schools.slice(0, 8).map((item) => ({ label: item.schoolName, value: item.compliancePercent, tone: item.status }))} />
                <ChartCard title="Curriculum Risk Alerts" points={complianceQuery.data.riskAlerts.slice(0, 8).map((item) => ({ label: item.schoolName, value: 100, tone: 'critical' }))} />
              </div>
              <CompactDataTable
                columns={['School', 'Teachers', 'Learners', 'Pass Rate', 'ATP', 'Risk']}
                emptyState={!schoolsQuery.data.items.length ? <EmptyStateCompact title="Schools" message="District dashboard data is not available yet." /> : undefined}
              >
                {schoolsQuery.data.items.slice(0, 8).map((item) => (
                  <tr key={item.schoolId} className="h-11">
                    <td className="px-4 py-3 font-semibold text-slate-900">{item.schoolName}</td>
                    <td className="px-4 py-3">{item.teacherCount}</td>
                    <td className="px-4 py-3">{item.learnerCount}</td>
                    <td className="px-4 py-3">{item.averageApsScore}</td>
                    <td className="px-4 py-3">{item.complianceStatus}</td>
                    <td className="px-4 py-3"><RiskBadge label={item.riskLevel} /></td>
                  </tr>
                ))}
              </CompactDataTable>
            </div>
            <div className="space-y-4">
              <DashboardSectionCard title="Open Interventions" subtitle="District-wide intervention and support load.">
                <div className="space-y-3">
                  {complianceQuery.data.riskAlerts.slice(0, 5).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-amber-100 bg-amber-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.schoolName}</p>
                      <p className="mt-1 text-[12px] text-slate-600">{item.subject} · {item.teacherName}</p>
                    </div>
                  ))}
                  {!complianceQuery.data.riskAlerts.length ? <EmptyStateCompact title="Open Interventions" message="No interventions are open." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Messages And Requests" subtitle="Recent support and announcement activity.">
                <div className="space-y-3">
                  {settingsQuery.data.supportRequests.slice(0, 4).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.schoolName}</p>
                      <p className="mt-1 text-[12px] text-slate-500">{item.title}</p>
                    </div>
                  ))}
                  {!settingsQuery.data.supportRequests.length ? <EmptyStateCompact title="Messages" message="No support requests are waiting." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Quick Actions" subtitle="District executive shortcuts.">
                <div className="grid gap-3">
                  <QuickActionButton label="View All Circuits" icon={Building2} />
                  <QuickActionButton label="View All Schools" icon={Building2} />
                  <QuickActionButton label="View Curriculum Resources" icon={BookOpen} />
                  <QuickActionButton label="Send Announcement" icon={BellRing} />
                </div>
              </DashboardSectionCard>
            </div>
          </div>
        </DashboardShell>
      ) : null}
    </DataShell>
  );
};

export const DistrictSubjectAdvisorDashboardPage = () => {
  const dashboardQuery = useAppQuery({ queryKey: ['district-advisor-dashboard'], queryFn: districtService.advisorDashboard });
  const teachersQuery = useAppQuery({ queryKey: ['district-advisor-teachers'], queryFn: () => districtService.advisorTeachers() });
  const monitoringQuery = useAppQuery({ queryKey: ['district-advisor-atp-monitoring'], queryFn: () => districtService.advisorAtpMonitoring() });
  const assessmentsQuery = useAppQuery({ queryKey: ['district-advisor-assessments'], queryFn: districtService.advisorAssessments });
  return (
    <DataShell
      isLoading={dashboardQuery.isLoading || teachersQuery.isLoading || monitoringQuery.isLoading || assessmentsQuery.isLoading}
      error={dashboardQuery.error ?? teachersQuery.error ?? monitoringQuery.error ?? assessmentsQuery.error}
    >
      {dashboardQuery.data && teachersQuery.data ? (
        <DashboardShell>
          <DashboardSectionCard title={dashboardQuery.data.title} subtitle={dashboardQuery.data.subtitle}>
            <SimpleMetricSummary metrics={dashboardQuery.data.metrics} />
          </DashboardSectionCard>
          <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr_1fr]">
            <div className="space-y-4 xl:col-span-2">
              <div className="grid gap-4 xl:grid-cols-3">
                <ChartCard title="Subject Performance" points={teachersQuery.data.items.slice(0, 8).map((item) => ({ label: item.teacherName, value: item.averageMark, tone: item.status }))} />
                <ChartCard title="ATP Compliance Overview" points={(monitoringQuery.data?.items ?? []).slice(0, 8).map((item) => ({ label: item.teacherName, value: Math.max(0, Math.min(100, item.atpWeek * 10)), tone: item.status }))} />
                <ChartCard title="Schools Behind Schedule" points={(monitoringQuery.data?.items ?? []).slice(0, 8).map((item) => ({ label: item.schoolName, value: Math.max(0, (item.expectedWeek - item.atpWeek) * 20), tone: item.status }))} />
              </div>
              <CompactDataTable
                columns={['Teacher', 'School', 'Subject', 'ATP Week', 'Expected', 'Status', 'Average Mark']}
                emptyState={!teachersQuery.data.items.length ? <EmptyStateCompact title="Teachers" message="No subject advisor assignment found." /> : undefined}
              >
                {teachersQuery.data.items.slice(0, 8).map((item) => (
                  <tr key={`${item.teacherUserId}-${item.subject}-${item.grade}`} className="h-11">
                    <td className="px-4 py-3 font-semibold text-slate-900">{item.teacherName}</td>
                    <td className="px-4 py-3">{item.schoolName}</td>
                    <td className="px-4 py-3">{item.subject}</td>
                    <td className="px-4 py-3">{item.atpWeek}</td>
                    <td className="px-4 py-3">{item.expectedWeek}</td>
                    <td className="px-4 py-3"><RiskBadge label={item.status} /></td>
                    <td className="px-4 py-3">{item.averageMark}%</td>
                  </tr>
                ))}
              </CompactDataTable>
            </div>
            <div className="space-y-4">
              <DashboardSectionCard title="Recent Interventions" subtitle="Teachers and schools needing subject support.">
                <div className="space-y-3">
                  {(monitoringQuery.data?.items ?? []).slice(0, 5).map((item) => (
                    <div key={`${item.teacherUserId}-${item.subject}-${item.grade}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.teacherName}</p>
                      <p className="mt-1 text-[12px] text-slate-500">{item.schoolName} · {item.subject}</p>
                      <ProgressBar value={Math.max(0, Math.min(100, item.atpWeek * 10))} tone={item.status} />
                    </div>
                  ))}
                  {!(monitoringQuery.data?.items ?? []).length ? <EmptyStateCompact title="Recent Interventions" message="No interventions are open." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Assessment Schedule" subtitle="Latest common assessments and workshops.">
                <div className="space-y-3">
                  {(assessmentsQuery.data ?? []).slice(0, 5).map((item) => (
                    <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[13px] font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-[12px] text-slate-500">{item.subject} · {item.grade} · {item.term}</p>
                    </div>
                  ))}
                  {!(assessmentsQuery.data ?? []).length ? <EmptyStateCompact title="Assessments" message="No common assessments have been created yet." /> : null}
                </div>
              </DashboardSectionCard>
              <DashboardSectionCard title="Quick Actions" subtitle="Advisor shortcuts for support and curriculum.">
                <div className="grid gap-3">
                  <QuickActionButton label="Upload Support Resource" icon={BookOpen} />
                  <QuickActionButton label="Create Intervention" icon={Wrench} />
                  <QuickActionButton label="Generate AI Support Plan" icon={Sparkles} />
                  <QuickActionButton label="Open Assessments" icon={ClipboardCheck} />
                </div>
              </DashboardSectionCard>
            </div>
          </div>
        </DashboardShell>
      ) : null}
    </DataShell>
  );
};

export const DistrictCircuitSchoolsPage = () => {
  const query = useAppQuery({ queryKey: ['district-circuit-schools'], queryFn: districtService.circuitSchools });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">My Schools</h1>
            <p className="mt-2 text-sm text-slate-600">Only schools assigned to the logged-in circuit manager are visible here.</p>
          </div>
          <SimpleMetricSummary metrics={query.data.metrics} />
          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">School</th>
                    <th className="px-4 py-3 font-medium">Principal</th>
                    <th className="px-4 py-3 font-medium">Learners</th>
                    <th className="px-4 py-3 font-medium">Teachers</th>
                    <th className="px-4 py-3 font-medium">Pass Rate</th>
                    <th className="px-4 py-3 font-medium">ATP Compliance</th>
                    <th className="px-4 py-3 font-medium">Attendance</th>
                    <th className="px-4 py-3 font-medium">Risk</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.items.length ? query.data.items.map((item) => (
                    <tr key={item.schoolId} className="border-t border-slate-100">
                      <td className="px-4 py-3 font-semibold text-slate-900">{item.schoolName}</td>
                      <td className="px-4 py-3">{item.principal}</td>
                      <td className="px-4 py-3">{item.learners}</td>
                      <td className="px-4 py-3">{item.teachers}</td>
                      <td className="px-4 py-3">{item.passRate}%</td>
                      <td className="px-4 py-3">{item.atpCompliance}%</td>
                      <td className="px-4 py-3">{item.attendance}%</td>
                      <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${item.riskStatus === 'Green' ? 'bg-emerald-50 text-emerald-700' : item.riskStatus === 'Amber' ? 'bg-amber-50 text-amber-700' : 'bg-rose-50 text-rose-700'}`}>{item.riskStatus}</span></td>
                    </tr>
                  )) : (
                    <tr><td colSpan={8} className="px-4 py-10"><EmptyState title="My Schools" message="No schools are assigned to this circuit yet." /></td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictCircuitCurriculumPage = () => {
  const [filters, setFilters] = useState({ school: '', subject: '', grade: '', term: '', week: '' });
  const query = useAppQuery({
    queryKey: ['district-circuit-curriculum', filters],
    queryFn: () => districtService.circuitCurriculum({
      school: filters.school || undefined,
      subject: filters.subject || undefined,
      grade: filters.grade || undefined,
      term: filters.term || undefined,
      week: filters.week ? Number(filters.week) : undefined,
    }),
  });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">Curriculum Monitoring</h1>
            <p className="mt-2 text-sm text-slate-600">Track expected ATP coverage against teacher progress across assigned schools.</p>
            <div className="mt-4 grid gap-3 md:grid-cols-5">
              <Input value={filters.school} onChange={(event) => setFilters((current) => ({ ...current, school: event.target.value }))} placeholder="School ID" className="rounded-2xl" />
              <Input value={filters.subject} onChange={(event) => setFilters((current) => ({ ...current, subject: event.target.value }))} placeholder="Subject" className="rounded-2xl" />
              <Input value={filters.grade} onChange={(event) => setFilters((current) => ({ ...current, grade: event.target.value }))} placeholder="Grade" className="rounded-2xl" />
              <Input value={filters.term} onChange={(event) => setFilters((current) => ({ ...current, term: event.target.value }))} placeholder="Term" className="rounded-2xl" />
              <Input value={filters.week} onChange={(event) => setFilters((current) => ({ ...current, week: event.target.value }))} placeholder="Week" className="rounded-2xl" />
            </div>
          </div>
          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">School</th>
                    <th className="px-4 py-3 font-medium">Subject</th>
                    <th className="px-4 py-3 font-medium">Grade</th>
                    <th className="px-4 py-3 font-medium">Expected ATP Week</th>
                    <th className="px-4 py-3 font-medium">Actual Progress</th>
                    <th className="px-4 py-3 font-medium">Topic Expected</th>
                    <th className="px-4 py-3 font-medium">Topic Completed</th>
                    <th className="px-4 py-3 font-medium">Weeks Behind</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.items.length ? query.data.items.map((item) => (
                    <tr key={`${item.schoolId}-${item.subject}-${item.grade}-${item.expectedWeek}`} className="border-t border-slate-100">
                      <td className="px-4 py-3 font-semibold text-slate-900">{item.schoolName}</td>
                      <td className="px-4 py-3">{item.subject}</td>
                      <td className="px-4 py-3">{item.grade}</td>
                      <td className="px-4 py-3">{item.expectedWeek}</td>
                      <td className="px-4 py-3">{item.actualWeek}</td>
                      <td className="px-4 py-3">{item.topicExpected || '-'}</td>
                      <td className="px-4 py-3">{item.topicCompleted || '-'}</td>
                      <td className="px-4 py-3">{item.weeksBehind}</td>
                      <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${item.riskTone === 'Green' ? 'bg-emerald-50 text-emerald-700' : item.riskTone === 'Amber' ? 'bg-amber-50 text-amber-700' : 'bg-rose-50 text-rose-700'}`}>{item.status}</span></td>
                    </tr>
                  )) : (
                    <tr><td colSpan={9} className="px-4 py-10"><EmptyState title="Curriculum Monitoring" message="No ATP progress has been submitted yet." /></td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictCircuitVisitsPage = () => {
  const [form, setForm] = useState({ schoolId: '', visitDate: '', purpose: '', notes: '', status: 'SCHEDULED', outcome: '' });
  const query = useAppQuery({ queryKey: ['district-circuit-visits'], queryFn: districtService.circuitVisits });
  const createVisit = useMutation({
    mutationFn: () => districtService.createCircuitVisit(form),
    onSuccess: () => {
      setForm({ schoolId: '', visitDate: '', purpose: '', notes: '', status: 'SCHEDULED', outcome: '' });
      query.refetch();
    },
  });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      <section className="space-y-6">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <h1 className="text-2xl font-semibold text-slate-900">School Visits</h1>
          <p className="mt-2 text-sm text-slate-600">Schedule visits, capture outcomes, and maintain a circuit visit calendar.</p>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <Input value={form.schoolId} onChange={(event) => setForm((current) => ({ ...current, schoolId: event.target.value }))} placeholder="School ID" className="rounded-2xl" />
            <Input type="date" value={form.visitDate} onChange={(event) => setForm((current) => ({ ...current, visitDate: event.target.value }))} className="rounded-2xl" />
            <Input value={form.purpose} onChange={(event) => setForm((current) => ({ ...current, purpose: event.target.value }))} placeholder="Purpose" className="rounded-2xl" />
            <Input value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} placeholder="Notes" className="rounded-2xl md:col-span-2" />
            <Input value={form.outcome} onChange={(event) => setForm((current) => ({ ...current, outcome: event.target.value }))} placeholder="Outcome report" className="rounded-2xl" />
          </div>
          <Button type="button" className="mt-4 rounded-2xl" onClick={() => createVisit.mutate()} disabled={createVisit.isPending || !form.schoolId || !form.visitDate || !form.purpose}>
            {createVisit.isPending ? 'Saving visit...' : 'Schedule school visit'}
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {query.data?.length ? query.data.map((item) => (
            <div key={item.id} className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">{item.schoolName}</p>
              <p className="mt-1 text-sm text-slate-600">{new Date(item.visitDate).toLocaleDateString()} | {item.purpose}</p>
              <p className="mt-2 text-sm text-slate-600">{item.notes || 'No notes added.'}</p>
              <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-500">{item.status}</p>
            </div>
          )) : <EmptyState title="School Visits" message="No school visits have been scheduled yet." />}
        </div>
      </section>
    </DataShell>
  );
};

export const DistrictCircuitSupportRequestsPage = () => {
  const query = useAppQuery({ queryKey: ['district-circuit-support-requests'], queryFn: districtService.circuitSupportRequests });
  const updateRequest = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => districtService.updateCircuitSupportRequest(id, { status }),
    onSuccess: () => query.refetch(),
  });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      <section className="space-y-6">
        <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-semibold text-slate-900">Support Requests</h1>
          <p className="mt-2 text-sm text-slate-600">Review school requests, assign support, mark progress, and close the loop.</p>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {query.data?.length ? query.data.map((item) => (
            <div key={item.id} className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">{item.schoolName}</p>
                  <p className="mt-1 text-sm text-slate-600">{item.requestType}</p>
                </div>
                <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700">{item.status}</span>
              </div>
              <p className="mt-3 text-sm text-slate-600">{item.description}</p>
              <div className="mt-4 flex gap-2">
                <Button type="button" className="rounded-xl px-3 py-2 text-xs" onClick={() => updateRequest.mutate({ id: item.id, status: 'IN_PROGRESS' })}>Mark In Progress</Button>
                <Button type="button" className="rounded-xl bg-slate-900 px-3 py-2 text-xs hover:bg-slate-800" onClick={() => updateRequest.mutate({ id: item.id, status: 'CLOSED' })}>Close Request</Button>
              </div>
            </div>
          )) : <EmptyState title="Support Requests" message="No support requests are waiting." />}
        </div>
      </section>
    </DataShell>
  );
};

export const DistrictCircuitInterventionsPage = () => {
  const [form, setForm] = useState({ title: '', description: '', interventionType: 'ATP_SUPPORT', priority: 'HIGH', schoolId: '', subject: '', grade: '', dueDate: '' });
  const query = useAppQuery({ queryKey: ['district-circuit-interventions'], queryFn: districtService.circuitInterventions });
  const createIntervention = useMutation({
    mutationFn: () => districtService.createDistrictWorkflowIntervention(form),
    onSuccess: () => {
      setForm({ title: '', description: '', interventionType: 'ATP_SUPPORT', priority: 'HIGH', schoolId: '', subject: '', grade: '', dueDate: '' });
      query.refetch();
    },
  });
  const generatePlan = useMutation({
    mutationFn: (id: string) => districtService.generateAiSupportPlan(id),
    onSuccess: () => query.refetch(),
  });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      <section className="space-y-6">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <h1 className="text-2xl font-semibold text-slate-900">Interventions</h1>
          <p className="mt-2 text-sm text-slate-600">Open support actions for schools, principals, teachers, or subject advisors.</p>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <Input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="Title" className="rounded-2xl" />
            <Input value={form.schoolId} onChange={(event) => setForm((current) => ({ ...current, schoolId: event.target.value }))} placeholder="School ID" className="rounded-2xl" />
            <Input value={form.interventionType} onChange={(event) => setForm((current) => ({ ...current, interventionType: event.target.value }))} placeholder="Type" className="rounded-2xl" />
            <Input value={form.subject} onChange={(event) => setForm((current) => ({ ...current, subject: event.target.value }))} placeholder="Subject" className="rounded-2xl" />
            <Input value={form.grade} onChange={(event) => setForm((current) => ({ ...current, grade: event.target.value }))} placeholder="Grade" className="rounded-2xl" />
            <Input type="date" value={form.dueDate} onChange={(event) => setForm((current) => ({ ...current, dueDate: event.target.value }))} className="rounded-2xl" />
            <textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} className="min-h-28 rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500 md:col-span-3" placeholder="Description" />
          </div>
          <Button type="button" className="mt-4 rounded-2xl" onClick={() => createIntervention.mutate()} disabled={createIntervention.isPending || !form.title.trim()}>
            {createIntervention.isPending ? 'Creating intervention...' : 'Create intervention'}
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {query.data?.length ? query.data.map((item) => (
            <div key={item.id} className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                  <p className="mt-1 text-sm text-slate-600">{item.schoolName || 'District-wide'} | {item.interventionType}</p>
                </div>
                <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700">{item.status}</span>
              </div>
              <p className="mt-3 text-sm text-slate-600">{item.description || 'No description added.'}</p>
              {item.supportPlan ? <pre className="mt-4 whitespace-pre-wrap rounded-2xl border border-slate-100 bg-slate-50 p-4 text-xs text-slate-700">{item.supportPlan}</pre> : null}
              <Button type="button" className="mt-4 rounded-xl px-3 py-2 text-xs" onClick={() => generatePlan.mutate(item.id)} disabled={generatePlan.isPending}>
                Generate AI Support Plan
              </Button>
            </div>
          )) : <EmptyState title="Interventions" message="No interventions are open." />}
        </div>
      </section>
    </DataShell>
  );
};

export const DistrictSubjectAdvisorTeachersPage = () => {
  const query = useAppQuery({ queryKey: ['district-advisor-teachers'], queryFn: districtService.advisorTeachers });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      {query.data ? (
        <section className="space-y-6">
          <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-sm">
            <h1 className="text-2xl font-semibold text-slate-900">Teachers</h1>
            <p className="mt-2 text-sm text-slate-600">Teachers teaching the advisor&apos;s assigned subjects and grades.</p>
          </div>
          <SimpleMetricSummary metrics={query.data.metrics} />
          <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">Teacher</th>
                    <th className="px-4 py-3 font-medium">School</th>
                    <th className="px-4 py-3 font-medium">Subject</th>
                    <th className="px-4 py-3 font-medium">Grade</th>
                    <th className="px-4 py-3 font-medium">Classes</th>
                    <th className="px-4 py-3 font-medium">ATP Week</th>
                    <th className="px-4 py-3 font-medium">Expected Week</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Average Mark</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.items.length ? query.data.items.map((item) => (
                    <tr key={`${item.teacherUserId}-${item.subject}-${item.grade}`} className="border-t border-slate-100">
                      <td className="px-4 py-3 font-semibold text-slate-900">{item.teacherName}</td>
                      <td className="px-4 py-3">{item.schoolName}</td>
                      <td className="px-4 py-3">{item.subject}</td>
                      <td className="px-4 py-3">{item.grade}</td>
                      <td className="px-4 py-3">{item.classes}</td>
                      <td className="px-4 py-3">{item.atpWeek}</td>
                      <td className="px-4 py-3">{item.expectedWeek}</td>
                      <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${item.status === 'On Track' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>{item.status}</span></td>
                      <td className="px-4 py-3">{item.averageMark}%</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={9} className="px-4 py-10"><EmptyState title="Teachers" message="No subject advisor assignment found." /></td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      ) : null}
    </DataShell>
  );
};

export const DistrictSubjectAdvisorAssessmentsPage = () => {
  const [form, setForm] = useState({ title: '', subject: '', grade: '', term: '', date: '', totalMarks: '', dueDate: '' });
  const query = useAppQuery({ queryKey: ['district-advisor-assessments'], queryFn: districtService.advisorAssessments });
  const createAssessment = useMutation({
    mutationFn: () => districtService.createAdvisorAssessment({
      title: form.title,
      subject: form.subject,
      grade: form.grade,
      term: form.term,
      date: form.date || undefined,
      totalMarks: form.totalMarks ? Number(form.totalMarks) : undefined,
      dueDate: form.dueDate || undefined,
      asset: {
        repositoryType: 'COMMON_ASSESSMENT',
        title: form.title,
        subject: form.subject,
        grade: form.grade,
        term: form.term,
      },
    }),
    onSuccess: () => {
      setForm({ title: '', subject: '', grade: '', term: '', date: '', totalMarks: '', dueDate: '' });
      query.refetch();
    },
  });
  return (
    <DataShell isLoading={query.isLoading} error={query.error}>
      <section className="space-y-6">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
          <h1 className="text-2xl font-semibold text-slate-900">Assessments</h1>
          <p className="mt-2 text-sm text-slate-600">Create common tests, exams, preparatory tasks, and revision activities for assigned subjects.</p>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <Input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="Title" className="rounded-2xl" />
            <Input value={form.subject} onChange={(event) => setForm((current) => ({ ...current, subject: event.target.value }))} placeholder="Subject" className="rounded-2xl" />
            <Input value={form.grade} onChange={(event) => setForm((current) => ({ ...current, grade: event.target.value }))} placeholder="Grade" className="rounded-2xl" />
            <Input value={form.term} onChange={(event) => setForm((current) => ({ ...current, term: event.target.value }))} placeholder="Term" className="rounded-2xl" />
            <Input type="date" value={form.date} onChange={(event) => setForm((current) => ({ ...current, date: event.target.value }))} className="rounded-2xl" />
            <Input value={form.totalMarks} onChange={(event) => setForm((current) => ({ ...current, totalMarks: event.target.value }))} placeholder="Total marks" className="rounded-2xl" />
          </div>
          <Button type="button" className="mt-4 rounded-2xl" onClick={() => createAssessment.mutate()} disabled={createAssessment.isPending || !form.title || !form.subject || !form.grade || !form.term}>
            {createAssessment.isPending ? 'Creating assessment...' : 'Create common assessment'}
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {query.data?.length ? query.data.map((item) => (
            <div key={item.id} className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">{item.title}</p>
              <p className="mt-1 text-sm text-slate-600">{item.subject} | {item.grade} | {item.term}</p>
              <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-500">{item.badge}</p>
            </div>
          )) : <EmptyState title="Assessments" message="No common assessments have been created yet." />}
        </div>
      </section>
    </DataShell>
  );
};
