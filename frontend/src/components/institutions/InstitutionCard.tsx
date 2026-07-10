import { useNavigate } from 'react-router-dom';
import { ArrowUpRight, CalendarDays, ExternalLink, GraduationCap, MapPin } from 'lucide-react';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { Button } from '@/components/ui/Button';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import { createInstitutionSlug } from '@/lib/institutionSlug';
import type { Institution } from '@/types';

const statusStyles = {
  OPEN: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  OPENING_SOON: 'border-blue-200 bg-blue-50 text-blue-800',
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

const UNIVERSITY_TYPES = new Set(['traditional', 'comprehensive', 'university of technology', 'health sciences', 'university']);

export const InstitutionCard = ({ institution, compact = false }: InstitutionCardProps) => {
  const navigate = useNavigate();
  const resolved = resolveInstitutionDisplay(institution);
  const hasFaculties = Boolean(resolved.faculties?.length);
  const hasStatus = resolved.applicationStatus && resolved.applicationStatus in statusStyles;
  const normalizedType = (resolved.institutionType || resolved.category || '').trim().toLowerCase();
  const isUniversity = UNIVERSITY_TYPES.has(normalizedType) || normalizedType.includes('university');
  const slug = createInstitutionSlug(resolved.displayName);

  return (
    <article className="flex h-full flex-col rounded-2xl border border-[#E2E8F0] bg-white p-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)] transition-all duration-200 hover:-translate-y-[3px] hover:shadow-[0_18px_38px_rgba(15,23,42,0.12)]">
      <div className="flex items-start gap-4">
        <InstitutionLogo
          src={resolved.logoUrl}
          institutionName={resolved.displayName}
          abbreviation={resolved.abbreviation}
          size={72}
          className="rounded-2xl"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-500">{resolved.institutionType || resolved.category || 'Institution'}</p>
            {hasStatus ? (
              <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${statusStyles[resolved.applicationStatus as keyof typeof statusStyles]}`}>
                {statusLabel[resolved.applicationStatus as keyof typeof statusLabel]}
              </span>
            ) : null}
          </div>
          <h3 className="mt-2 text-lg font-bold text-slate-900">{resolved.displayName}</h3>
          <p className="mt-1 text-sm font-medium text-slate-600">{resolved.abbreviation || 'Institution'}</p>
          <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-500">
            <span className="inline-flex items-center gap-1"><MapPin size={14} />{[resolved.city, resolved.province].filter(Boolean).join(', ') || infoLabel(resolved.country)}</span>
            <span>{infoLabel(resolved.country)}</span>
          </div>
        </div>
      </div>

      {!compact && resolved.description ? (
        <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-600">{resolved.description}</p>
      ) : null}

      <div className={`mt-4 grid gap-3 text-sm text-slate-600 ${compact ? 'sm:grid-cols-2' : 'sm:grid-cols-2 xl:grid-cols-3'}`}>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">QS ranking</p>
          <p className="mt-1">{infoLabel(resolved.qsRanking)}</p>
        </div>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">THE ranking</p>
          <p className="mt-1">{infoLabel(resolved.theRanking)}</p>
        </div>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">Acceptance</p>
          <p className="mt-1">{infoLabel(resolved.acceptanceIndicator)}</p>
        </div>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">Faculties</p>
          <p className="mt-1">{resolved.facultyCount ?? 'Not available'}</p>
        </div>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">Programmes</p>
          <p className="mt-1">{resolved.programmeCount ?? 'Not available'}</p>
        </div>
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">Closing date</p>
          <p className="mt-1">{resolved.applicationClosingDate || 'Not available'}</p>
        </div>
      </div>

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
            <Button type="button" className="bg-slate-900 hover:bg-slate-800">
              <ExternalLink size={15} className="mr-2" />
              Visit Website
            </Button>
          </a>
        ) : null}
        {isUniversity ? (
          <>
            <Button type="button" className="bg-blue-600 hover:bg-blue-500" onClick={() => navigate(`/student/universities/${slug}/programmes`)}>View Programmes</Button>
            <Button type="button" className="bg-slate-700 hover:bg-slate-600" onClick={() => navigate(`/student/universities/${slug}/admission-requirements`)}>Admission Requirements</Button>
          </>
        ) : null}
        {resolved.applicationUrl && resolved.applicationStatus === 'OPEN' ? (
          <a href={resolved.applicationUrl} target="_blank" rel="noreferrer">
            <Button type="button">
              <ArrowUpRight size={15} className="mr-2" />
              Apply Now
            </Button>
          </a>
        ) : null}
        {!resolved.applicationUrl && resolved.applicationClosingDate ? (
          <span className="inline-flex items-center text-xs font-medium text-slate-500">
            <CalendarDays size={14} className="mr-1" />
            Closing {resolved.applicationClosingDate}
          </span>
        ) : null}
      </div>
    </article>
  );
};
