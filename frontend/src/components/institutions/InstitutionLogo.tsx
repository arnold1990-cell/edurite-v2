import { useEffect, useMemo, useState } from 'react';
import { cn } from '@/lib/cn';

interface InstitutionLogoProps {
  src?: string | null;
  institutionName: string;
  abbreviation?: string | null;
  size?: number;
  className?: string;
}

const getInitials = (value: string) => value
  .split(/\s+/)
  .filter(Boolean)
  .slice(0, 2)
  .map((part) => part[0]?.toUpperCase() ?? '')
  .join('') || value.slice(0, 2).toUpperCase();

export const InstitutionLogo = ({ src, institutionName, abbreviation, size = 72, className }: InstitutionLogoProps) => {
  const [failed, setFailed] = useState(false);
  const fallback = useMemo(() => (abbreviation?.trim() || getInitials(institutionName.trim() || 'ED')), [abbreviation, institutionName]);
  const resolvedSrc = src?.trim();
  const showImage = Boolean(resolvedSrc) && !failed;

  useEffect(() => {
    setFailed(false);
  }, [resolvedSrc]);

  return (
    <div
      className={cn(
        'relative flex shrink-0 items-center justify-center overflow-hidden border border-slate-200 bg-white text-slate-600 shadow-sm',
        className,
      )}
      style={{ width: size, height: size, minWidth: size, minHeight: size, borderRadius: 16 }}
    >
      {showImage ? (
        <img
          src={resolvedSrc}
          alt={`${institutionName} logo`}
          className="h-full w-full object-contain p-3"
          loading="lazy"
          decoding="async"
          onError={() => setFailed(true)}
        />
      ) : (
        <span className="px-2 text-center text-[11px] font-bold uppercase tracking-[0.18em] text-slate-500">
          {fallback}
        </span>
      )}
    </div>
  );
};
