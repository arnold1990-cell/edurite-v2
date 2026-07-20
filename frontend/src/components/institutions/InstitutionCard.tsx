import { useNavigate } from 'react-router-dom';
import { ArrowUpRight, CalendarDays, ExternalLink, GraduationCap, MapPin } from 'lucide-react';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { Button } from '@/components/ui/Button';
import { createInstitutionSlug } from '@/lib/institutionSlug';
import { isOnlineEntry, isUniversityEntry, resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import type { Institution } from '@/types';

const statusStyles = {
  OPEN: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  OPENING_SOON: 'border-primary-200 bg-primary-50 text-primary-800',
  CLOSED: 'border-red-200 bg-red-50 text-red-800',
} as const;

const statusLabel = {
  OPEN: 'Applications Open',
  OPENING_SOON: 'Opening Soon',
  CLOSED: 'Applications Closed',
} as const;

const infoLabel = (value?: string | null) => value?.trim() || 'Not available';

interface InstitutionCardProps {
  institution: Institution;
  compact?: boolean;
}

export const InstitutionCard = ({ institution, compact = false }: InstitutionCardProps) => {
  const navigate = useNavigate();
  const resolved = resolveInstitutionDisplay(institution);
  const hasFaculties = Boolean(resolved.faculties?.length);
  const hasStatus = resolved.applicationStatus && resolved.applicationStatus in statusStyles;
  const isUniversity = isUniversityEntry(resolved);
  const isOnline = isOnlineEntry(resolved);
  const slug = createInstitutionSlug(resolved.displayName);
  const metadata = [
    resolved.qsRanking ? { label: 'QS ranking', value: resolved.qsRanking } : null,
    resolved.theRanking ? { label: 'THE ranking', value: resolved.theRanking } : null,
    resolved.acceptanceIndicator ? { label: 'Acceptance', value: resolved.acceptanceIndicator } : null,
    resolved.facultyCount ? { label: 'Faculties', value: String(resolved.facultyCount) } : null,
    resolved.programmeCount ? { label: 'Programmes', value: String(resolved.programmeCount) } : null,
    resolved.applicationClosingDate ? { label: 'Closing date', value: resolved.applicationClosingDate } : null,
  ].filter(Boolean) as Array<{ label: string; value: string }>;

  return (
    <article className="flex h-full flex-col rounded-2xl border border-[#E2E8F0] bg-white p-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)] transition-all duration-200 hover:-translate-y-[3px] hover:shadow-[0_18px_38px_rgba(15,23,42,0.12)]">
      <div className="flex items-start gap-4">
        <InstitutionLogo src={resolved.logoUrl} institutionName={resolved.displayName} abbreviation={resolved.abbreviation} size={72} className="rounded-2xl" />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-500">{resolved.institutionType || resolved.category || 'Institution'}</p>
            {hasStatus ? <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${statusStyles[resolved.applicationStatus as keyof typeof statusStyles]}`}>{statusLabel[resolved.applicationStatus as keyof typeof statusLabel]}</span> : null}
          </div>
          <h3 className="mt-2 text-lg font-bold text-slate-900">{resolved.displayName}</h3>
          <p className="mt-1 text-sm font-medium text-slate-600">{resolved.abbreviation || 'Institution'}</p>
          <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-500">
            <span className="inline-flex items-center gap-1"><MapPin size={14} />{[resolved.city, resolved.province].filter(Boolean).join(', ') || infoLabel(resolved.country)}</span>
            <span>{infoLabel(resolved.country)}</span>
          </div>
        </div>
      </div>

      {!compact && resolved.description ? <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-600">{resolved.description}</p> : null}

      {metadata.length ? (
        <div className={`mt-4 grid gap-3 text-sm text-slate-600 ${compact ? 'sm:grid-cols-2' : 'sm:grid-cols-2 xl:grid-cols-3'}`}>
          {metadata.map((item) => (
            <div key={item.label}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">{item.label}</p>
              <p className="mt-1">{item.value}</p>
            </div>
          ))}
        </div>
      ) : null}

      {hasFaculties ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {resolved.faculties?.slice(0, compact ? 4 : 6).map((faculty) => (
            <span key={faculty} className="inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700">
              <GraduationCap size={12} className="mr-1" />
              {faculty}
            </span>
          ))}
        </div>
      ) : null}

      <div className="mt-auto flex flex-wrap gap-2 pt-5">
        {resolved.website ? (
          <a href={resolved.website} target="_blank" rel="noreferrer">
            <Button type="button" className="bg-primary-600 hover:bg-primary-700">
              <ExternalLink size={15} className="mr-2" />
              {isOnline ? 'Explore Platform' : 'Visit Website'}
            </Button>
          </a>
        ) : null}
        {isUniversity ? (
          <>
            <Button type="button" className="bg-blue-600 hover:bg-primary-500" onClick={() => navigate(`/student/universities/${slug}/programmes`)}>View Programmes</Button>
            <Button type="button" className="bg-primary-600 hover:bg-primary-700" onClick={() => navigate(`/student/universities/${slug}/admission-requirements`)}>Admission Requirements</Button>
          </>
        ) : null}
        {resolved.applicationUrl && resolved.applicationStatus === 'OPEN' ? (
          <a href={resolved.applicationUrl} target="_blank" rel="noreferrer">
            <Button type="button"><ArrowUpRight size={15} className="mr-2" />Apply Now</Button>
          </a>
        ) : null}
        {!resolved.applicationUrl && resolved.applicationClosingDate ? <span className="inline-flex items-center text-xs font-medium text-slate-500"><CalendarDays size={14} className="mr-1" />Closing {resolved.applicationClosingDate}</span> : null}
      </div>
    </article>
  );
};



