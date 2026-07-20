import { useMemo, useState } from 'react';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { InstitutionCard } from '@/components/institutions/InstitutionCard';
import { Input } from '@/components/ui/Input';
import { useAppQuery } from '@/hooks/useAppQuery';
import { buildMergedInstitutionList, isOnlineEntry, isPrivateEntry, isTvetEntry, resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { institutionService } from '@/services/institutionService';
import type { Institution } from '@/types';

type InstitutionTab = 'PRIVATE' | 'TVET' | 'ONLINE';

const SA_COLLEGE_PROVINCES = ['Eastern Cape', 'Free State', 'Gauteng', 'KwaZulu-Natal', 'Limpopo', 'Mpumalanga', 'Northern Cape', 'North West', 'Western Cape'];

const matchesTab = (tab: InstitutionTab, institution: Institution) => (tab === 'TVET' ? isTvetEntry(institution) : tab === 'PRIVATE' ? isPrivateEntry(institution) : isOnlineEntry(institution));

export const StudentCollegesTvetsPage = () => {
  const [activeTab, setActiveTab] = useState<InstitutionTab>('PRIVATE');
  const [search, setSearch] = useState('');
  const [provinceFilter, setProvinceFilter] = useState<'ALL' | string>('ALL');

  const institutions = useAppQuery<Institution[]>({ queryKey: ['student', 'institutions', 'colleges-tvets'], queryFn: () => institutionService.list() });
  const merged = useMemo(() => buildMergedInstitutionList(institutions.data ?? [], (item) => isTvetEntry(item) || isPrivateEntry(item) || isOnlineEntry(item)).map((item) => resolveInstitutionDisplay(item)).filter((item) => item.isActive !== false), [institutions.data]);
  const activeInstitutions = useMemo(() => merged.filter((item) => matchesTab(activeTab, item as Institution)), [activeTab, merged]);

  const provinceOptions = useMemo(() => {
    const values = Array.from(new Set(activeInstitutions.map((item) => item.province).filter(Boolean)));
    return values.sort((a, b) => {
      const aIndex = SA_COLLEGE_PROVINCES.indexOf(a);
      const bIndex = SA_COLLEGE_PROVINCES.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [activeInstitutions]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return activeInstitutions.filter((institution) => {
      const matchesQuery = !query || institution.displayName.toLowerCase().includes(query) || (institution.abbreviation ?? '').toLowerCase().includes(query) || (institution.province ?? '').toLowerCase().includes(query) || (institution.city ?? '').toLowerCase().includes(query);
      const matchesProvince = provinceFilter === 'ALL' || institution.province === provinceFilter;
      return matchesQuery && matchesProvince;
    });
  }, [activeInstitutions, provinceFilter, search]);

  const groupedByProvince = useMemo(() => {
    const groupMap = new Map<string, Institution[]>();
    filtered.forEach((institution) => {
      const key = institution.province || (activeTab === 'ONLINE' ? 'Global' : 'Unspecified');
      if (!groupMap.has(key)) groupMap.set(key, []);
      groupMap.get(key)?.push(institution as Institution);
    });
    return Array.from(groupMap.entries()).sort(([a], [b]) => {
      const aIndex = SA_COLLEGE_PROVINCES.indexOf(a);
      const bIndex = SA_COLLEGE_PROVINCES.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [activeTab, filtered]);

  if (institutions.isLoading) return <LoadingState message="Loading colleges, TVET institutions, and online providers..." />;
  if (institutions.isError) return <ErrorState message="Could not load institution listings right now." />;

  return (
    <section className="student-page-section">
      <header>
        <h1 className="student-page-title">Institutions</h1>
        <p className="student-page-subtitle">Browse private institutions, public TVET colleges, and online learning providers with the same official logo system used across the university directory.</p>
      </header>
      <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-white p-2">
        {([{ key: 'PRIVATE', label: 'Private Institutions' }, { key: 'TVET', label: 'TVET Colleges' }, { key: 'ONLINE', label: 'Online Providers' }] as const).map((tab) => <button key={tab.key} type="button" onClick={() => setActiveTab(tab.key)} className={['rounded-xl px-4 py-2 text-sm font-medium transition', activeTab === tab.key ? 'bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] text-white shadow-sm' : 'text-slate-700 hover:bg-slate-100'].join(' ')}>{tab.label}</button>)}
      </div>
      <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-2">
        <Input placeholder="Search by institution, abbreviation, or province" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select value={provinceFilter} onChange={(event) => setProvinceFilter(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"><option value="ALL">All provinces</option>{provinceOptions.map((province) => <option key={province} value={province}>{province}</option>)}</select>
      </div>
      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">Institution names and logos are used for identification purposes. EduRite is not affiliated with or endorsed by these institutions unless stated otherwise.</div>
      {groupedByProvince.length === 0 ? <EmptyState title="No institutions found" message="Try adjusting your search or filters to find matching institutions." /> : <div className="space-y-6">{groupedByProvince.map(([province, items]) => <div key={province} className="space-y-3"><div className="flex items-center justify-between"><h2 className="text-base font-semibold text-slate-900">{province}</h2><span className="rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-600">{items.length} institution{items.length === 1 ? '' : 's'}</span></div><div className="grid gap-4 lg:grid-cols-2">{items.map((institution) => <InstitutionCard key={institution.id ?? institution.name} institution={institution} compact />)}</div></div>)}</div>}
    </section>
  );
};




