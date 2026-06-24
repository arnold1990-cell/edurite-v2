import { useMemo } from 'react';
import { useAppQuery } from '@/hooks/useAppQuery';
import { institutionService } from '@/services/institutionService';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import type { Institution } from '@/types';

const featuredUniversities = new Set([
  'University of KwaZulu-Natal',
  'Tshwane University of Technology',
  'University of Pretoria',
  'University of Johannesburg',
  'University of the Free State',
  'University of Cape Town',
  'University of the Witwatersrand',
  'Stellenbosch University',
]);

const getInstitutionLocation = (university: Institution) => university.country ?? university.city ?? university.location ?? 'South Africa';

const getInstitutionInitials = (name: string) => name
  .split(/\s+/)
  .filter(Boolean)
  .slice(0, 2)
  .map((part) => part[0]?.toUpperCase() ?? '')
  .join('');

const UniversityCard = ({ university, compact = false }: { university: Institution; compact?: boolean }) => {
  const website = university.website?.trim();
  const location = getInstitutionLocation(university);
  const logoUrl = university.logoUrl?.trim();
  const cardBody = (
    <article
      className={[
        'group flex h-full flex-col rounded-xl border border-slate-200 bg-white transition duration-200',
        compact
          ? 'gap-3 p-3 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-sm'
          : 'gap-4 p-4 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md',
      ].join(' ')}
    >
      <div className={['flex items-center', compact ? 'gap-3' : 'gap-4'].join(' ')}>
        <div
          className={[
            'group/logo flex shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50 text-slate-500',
            compact ? 'h-14 w-14' : 'h-16 w-16',
          ].join(' ')}
        >
          {logoUrl ? (
            <img
              src={logoUrl}
              alt={`${university.name} logo`}
              className="h-full w-full object-contain p-2 group-[.has-logo-error]/logo:hidden"
              loading="lazy"
              onError={(event) => {
                event.currentTarget.parentElement?.classList.add('has-logo-error');
              }}
            />
          ) : null}
          <span
            className={[
              'font-semibold tracking-wide text-slate-600',
              compact ? 'text-sm' : 'text-base',
              logoUrl ? 'hidden group-[.has-logo-error]/logo:block' : 'block',
            ].join(' ')}
          >
            {getInstitutionInitials(university.name)}
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-slate-500">{university.category ?? 'University'}</p>
          <h3 className={['mt-1 font-semibold text-slate-900', compact ? 'line-clamp-2 text-sm sm:text-base' : 'text-base'].join(' ')}>{university.name}</h3>
          <p className="mt-1 text-xs text-slate-600">{location}</p>
        </div>
      </div>

      <div className="mt-auto flex items-center justify-between gap-2 pt-1">
        <span className="text-xs font-medium text-slate-500">{website ? 'Official website' : 'Website unavailable'}</span>
        <span
          className={[
            'inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold',
            website
              ? 'border-blue-200 bg-blue-50 text-blue-700'
              : 'border-slate-200 bg-slate-100 text-slate-500',
          ].join(' ')}
        >
          {website ? 'Visit Website' : 'Unavailable'}
        </span>
      </div>
    </article>
  );

  if (!website) {
    return <div className="h-full cursor-not-allowed opacity-80">{cardBody}</div>;
  }

  return (
    <a
      href={website}
      target="_blank"
      rel="noopener noreferrer"
      className="block h-full cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
      aria-label={`Visit ${university.name} official website`}
    >
      {cardBody}
    </a>
  );
};

export const StudentUniversitiesPage = () => {
  const institutions = useAppQuery<Institution[]>({ queryKey: ['student', 'institutions'], queryFn: () => institutionService.list() });

  const allUniversities = useMemo(() => (institutions.data ?? []).filter((item) => item.active !== false), [institutions.data]);
  const featured = useMemo(
    () => allUniversities.filter((item) => item.featured || featuredUniversities.has(item.name)).slice(0, 10),
    [allUniversities],
  );

  if (institutions.isLoading) return <LoadingState />;
  if (institutions.isError) return <ErrorState message="Could not load universities right now." />;
  if (allUniversities.length === 0) return <EmptyState title="No universities yet" message="No universities available at the moment." />;

  return (
    <section className="student-page-section">
      <header>
        <h1 className="student-page-title">Universities</h1>
        <p className="student-page-subtitle">Browse South African public universities and open their official sites for programmes, admissions, and fees.</p>
      </header>

      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 sm:p-5">
        <div className="mb-4">
          <h2 className="text-base font-semibold">Featured Institutions</h2>
          <p className="mt-1 text-sm text-slate-600">Explore highlighted universities in a compact, easy-to-scan layout.</p>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          {featured.map((university) => (
            <UniversityCard key={`featured-${university.id}`} university={university} compact />
          ))}
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-base font-semibold">All universities ({allUniversities.length})</h2>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {allUniversities.map((university) => (
            <UniversityCard key={university.id} university={university} />
          ))}
        </div>
      </div>
    </section>
  );
};
