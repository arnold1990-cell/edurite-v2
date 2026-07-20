import { useMemo, useState } from 'react';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { InstitutionCard } from '@/components/institutions/InstitutionCard';
import { Input } from '@/components/ui/Input';
import { useAppQuery } from '@/hooks/useAppQuery';
import { buildMergedInstitutionList, isUniversityEntry, resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { institutionService } from '@/services/institutionService';
import type { Institution } from '@/types';

const featuredUniversities = new Set(['University of KwaZulu-Natal', 'Tshwane University of Technology', 'University of Pretoria', 'University of Johannesburg', 'University of the Free State', 'University of Cape Town', 'University of the Witwatersrand', 'Stellenbosch University']);

export const StudentUniversitiesPage = () => {
  const [search, setSearch] = useState('');
  const [provinceFilter, setProvinceFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [facultyFilter, setFacultyFilter] = useState('ALL');

  const institutions = useAppQuery<Institution[]>({ queryKey: ['student', 'institutions'], queryFn: () => institutionService.list() });
  const allUniversities = useMemo(() => buildMergedInstitutionList(institutions.data ?? [], isUniversityEntry).map((item) => resolveInstitutionDisplay(item)).filter((item) => item.isActive !== false), [institutions.data]);
  const provinceOptions = useMemo(() => Array.from(new Set(allUniversities.map((item) => item.province).filter(Boolean))).sort(), [allUniversities]);
  const typeOptions = useMemo(() => Array.from(new Set(allUniversities.map((item) => item.institutionType).filter(Boolean))).sort(), [allUniversities]);
  const facultyOptions = useMemo(() => Array.from(new Set(allUniversities.flatMap((item) => item.faculties ?? []))).sort(), [allUniversities]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return allUniversities.filter((item) => {
      const matchesQuery = !query || item.displayName.toLowerCase().includes(query) || (item.abbreviation ?? '').toLowerCase().includes(query) || (item.city ?? '').toLowerCase().includes(query) || (item.province ?? '').toLowerCase().includes(query);
      const matchesProvince = provinceFilter === 'ALL' || item.province === provinceFilter;
      const matchesType = typeFilter === 'ALL' || item.institutionType === typeFilter;
      const matchesStatus = statusFilter === 'ALL' || item.applicationStatus === statusFilter;
      const matchesFaculty = facultyFilter === 'ALL' || (item.faculties ?? []).includes(facultyFilter);
      return matchesQuery && matchesProvince && matchesType && matchesStatus && matchesFaculty;
    });
  }, [allUniversities, facultyFilter, provinceFilter, search, statusFilter, typeFilter]);

  const featured = useMemo(() => filtered.filter((item) => item.isFeatured || featuredUniversities.has(item.displayName)).slice(0, 8), [filtered]);

  if (institutions.isLoading) return <LoadingState />;
  if (institutions.isError) return <ErrorState message="Could not load universities right now." />;
  if (allUniversities.length === 0) return <EmptyState title="No universities yet" message="No universities available at the moment." />;

  return (
    <section className="student-page-section">
      <header>
        <h1 className="student-page-title">Universities</h1>
        <p className="student-page-subtitle">Compare South African universities with official logos, location details, verified institution facts, and direct official links.</p>
      </header>
      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <Input placeholder="Search by university or abbreviation" value={search} onChange={(event) => setSearch(event.target.value)} />
          <select value={provinceFilter} onChange={(event) => setProvinceFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"><option value="ALL">All provinces</option>{provinceOptions.map((province) => <option key={province} value={province}>{province}</option>)}</select>
          <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"><option value="ALL">All institution types</option>{typeOptions.map((type) => <option key={type} value={type}>{type}</option>)}</select>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"><option value="ALL">All application statuses</option><option value="OPEN">Applications Open</option><option value="OPENING_SOON">Opening Soon</option><option value="CLOSED">Applications Closed</option></select>
          <select value={facultyFilter} onChange={(event) => setFacultyFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"><option value="ALL">All faculties</option>{facultyOptions.map((faculty) => <option key={faculty} value={faculty}>{faculty}</option>)}</select>
        </div>
      </div>
      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">Institution names and logos are used for identification purposes. EduRite is not affiliated with or endorsed by these institutions unless stated otherwise.</div>
      {featured.length > 0 ? <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:p-5"><div className="mb-4"><h2 className="text-base font-semibold">Featured Institutions</h2><p className="mt-1 text-sm text-slate-600">A curated set of major public universities with richer metadata and official branding.</p></div><div className="grid gap-4 lg:grid-cols-2">{featured.map((university) => <InstitutionCard key={`featured-${university.id ?? university.displayName}`} institution={university as Institution} compact />)}</div></div> : null}
      {filtered.length === 0 ? <EmptyState title="No universities found" message="Try broadening your search or clearing one of the filters." /> : <div><h2 className="mb-3 text-base font-semibold">All universities ({filtered.length})</h2><div className="grid gap-4 lg:grid-cols-2">{filtered.map((university) => <InstitutionCard key={university.id ?? university.displayName} institution={university as Institution} />)}</div></div>}
    </section>
  );
};
