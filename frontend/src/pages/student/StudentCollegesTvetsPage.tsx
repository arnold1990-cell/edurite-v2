import { useMemo, useState } from 'react';
import { Building2, Globe, MapPin, School } from 'lucide-react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { institutionService } from '@/services/institutionService';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { Input } from '@/components/ui/Input';
import type { Institution } from '@/types';

type InstitutionTab = 'COLLEGE' | 'TVET';

const SA_COLLEGE_PROVINCES = [
  'Eastern Cape',
  'Free State',
  'Gauteng',
  'KwaZulu-Natal',
  'Limpopo',
  'Mpumalanga',
  'Northern Cape',
  'North West',
  'Western Cape',
];

const getInstitutionType = (institution: Institution): InstitutionTab =>
  institution.category?.toUpperCase() === 'TVET' ? 'TVET' : 'COLLEGE';

const cleanText = (value?: string | null) => value?.trim() ?? '';

const provinceName = (institution: Institution) =>
  cleanText(institution.province) || cleanText(institution.location) || cleanText(institution.city) || 'Unspecified';

const InstitutionCard = ({ institution }: { institution: Institution }) => {
  const website = cleanText(institution.website);
  const province = provinceName(institution);
  const type = getInstitutionType(institution);
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="text-base font-semibold text-slate-900">{institution.name}</h3>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
            <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 font-medium text-slate-700">
              <MapPin size={12} />
              {province}
            </span>
            <span className="inline-flex items-center gap-1 rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 font-medium text-blue-700">
              {type === 'TVET' ? <School size={12} /> : <Building2 size={12} />}
              {type === 'TVET' ? 'TVET College' : 'College'}
            </span>
          </div>
        </div>
      </div>
      <div className="mt-4">
        {website ? (
          <a
            href={website}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 text-sm font-medium text-blue-700 hover:text-blue-600"
          >
            <Globe size={14} />
            Visit Website
          </a>
        ) : (
          <p className="text-sm text-slate-500">Website unavailable</p>
        )}
      </div>
    </article>
  );
};

export const StudentCollegesTvetsPage = () => {
  const [activeTab, setActiveTab] = useState<InstitutionTab>('COLLEGE');
  const [search, setSearch] = useState('');
  const [provinceFilter, setProvinceFilter] = useState<'ALL' | string>('ALL');
  const [typeFilter, setTypeFilter] = useState<'ALL' | InstitutionTab>('ALL');

  const institutions = useAppQuery<Institution[]>({
    queryKey: ['student', 'institutions', 'colleges-tvets'],
    queryFn: () => institutionService.list(),
  });

  const activeInstitutions = useMemo(
    () => (institutions.data ?? []).filter((item) => item.active !== false),
    [institutions.data],
  );

  const tabInstitutions = useMemo(
    () => activeInstitutions.filter((item) => getInstitutionType(item) === activeTab),
    [activeInstitutions, activeTab],
  );

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return tabInstitutions.filter((institution) => {
      const institutionType = getInstitutionType(institution);
      const p = provinceName(institution);
      const matchesQuery = !query
        || institution.name.toLowerCase().includes(query)
        || p.toLowerCase().includes(query)
        || institutionType.toLowerCase().includes(query)
        || (institution.category ?? '').toLowerCase().includes(query);
      const matchesProvince = provinceFilter === 'ALL' || p === provinceFilter;
      const matchesType = typeFilter === 'ALL' || institutionType === typeFilter;
      return matchesQuery && matchesProvince && matchesType;
    });
  }, [tabInstitutions, search, provinceFilter, typeFilter]);

  const groupedByProvince = useMemo(() => {
    const groupMap = new Map<string, Institution[]>();
    filtered.forEach((institution) => {
      const key = provinceName(institution);
      if (!groupMap.has(key)) groupMap.set(key, []);
      groupMap.get(key)?.push(institution);
    });
    const ordered = Array.from(groupMap.entries()).sort(([a], [b]) => {
      const aIndex = SA_COLLEGE_PROVINCES.indexOf(a);
      const bIndex = SA_COLLEGE_PROVINCES.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
    return ordered;
  }, [filtered]);

  const provinceOptions = useMemo(() => {
    const values = new Set<string>();
    tabInstitutions.forEach((institution) => values.add(provinceName(institution)));
    return Array.from(values).sort((a, b) => {
      const aIndex = SA_COLLEGE_PROVINCES.indexOf(a);
      const bIndex = SA_COLLEGE_PROVINCES.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a.localeCompare(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [tabInstitutions]);

  if (institutions.isLoading) return <LoadingState message="Loading colleges and TVET institutions..." />;
  if (institutions.isError) return <ErrorState message="Could not load colleges and TVET institutions right now." />;

  return (
    <section className="student-page-section">
      <header>
        <h1 className="student-page-title">Colleges & TVETs</h1>
        <p className="student-page-subtitle">Browse South African institutions by type and province, with official TVET coverage aligned to DHET public college listings.</p>
      </header>

      <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-white p-2">
        <button
          type="button"
          onClick={() => setActiveTab('COLLEGE')}
          className={[
            'rounded-xl px-4 py-2 text-sm font-medium transition',
            activeTab === 'COLLEGE' ? 'bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] text-white shadow-sm' : 'text-slate-700 hover:bg-slate-100',
          ].join(' ')}
        >
          Colleges
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('TVET')}
          className={[
            'rounded-xl px-4 py-2 text-sm font-medium transition',
            activeTab === 'TVET' ? 'bg-gradient-to-r from-[#0B5BFF] to-[#1E8BFF] text-white shadow-sm' : 'text-slate-700 hover:bg-slate-100',
          ].join(' ')}
        >
          TVET Colleges
        </button>
      </div>

      <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-3">
        <Input
          placeholder="Search by institution, province, or type"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select
          value={provinceFilter}
          onChange={(event) => setProvinceFilter(event.target.value)}
          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"
        >
          <option value="ALL">All provinces</option>
          {provinceOptions.map((province) => (
            <option key={province} value={province}>{province}</option>
          ))}
        </select>
        <select
          value={typeFilter}
          onChange={(event) => setTypeFilter(event.target.value as 'ALL' | InstitutionTab)}
          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none transition focus:border-primary-300 focus:ring-4 focus:ring-primary-100"
        >
          <option value="ALL">All types</option>
          <option value="COLLEGE">Colleges</option>
          <option value="TVET">TVET Colleges</option>
        </select>
      </div>

      {groupedByProvince.length === 0 ? (
        <EmptyState
          title="No institutions found"
          message="Try adjusting your search or filters to find matching colleges or TVET institutions."
        />
      ) : (
        <div className="space-y-6">
          {groupedByProvince.map(([province, items]) => (
            <div key={province} className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-slate-900">{province}</h2>
                <span className="rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-600">
                  {items.length} institution{items.length === 1 ? '' : 's'}
                </span>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {items.map((institution) => (
                  <InstitutionCard key={institution.id} institution={institution} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
};
