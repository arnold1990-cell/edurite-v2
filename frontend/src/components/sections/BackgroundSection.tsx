import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

interface BackgroundSectionProps {
  eyebrow?: ReactNode;
  title: string;
  description: string;
  imageSrc: string;
  imageAlt?: string;
  className?: string;
  contentClassName?: string;
  overlayClassName?: string;
  imagePositionClassName?: string;
  children?: ReactNode;
}

export const BackgroundSection = ({
  eyebrow,
  title,
  description,
  imageSrc,
  imageAlt = '',
  className,
  contentClassName,
  overlayClassName,
  imagePositionClassName,
  children,
}: BackgroundSectionProps) => (
  <section
    className={cn(
      'relative overflow-hidden rounded-[28px] border border-slate-200 bg-slate-900 shadow-sm',
      className,
    )}
  >
    <img
      src={imageSrc}
      alt={imageAlt}
      loading="lazy"
      className={cn(
        'absolute inset-0 h-full w-full object-cover',
        imagePositionClassName,
      )}
    />
    <div
      className={cn(
        'absolute inset-0 bg-gradient-to-br from-slate-950/85 via-slate-900/70 to-primary-900/45',
        overlayClassName,
      )}
      aria-hidden="true"
    />
    <div className={cn('relative flex h-full flex-col space-y-4 p-6 text-white sm:p-8', contentClassName)}>
      {eyebrow ? <div>{eyebrow}</div> : null}
      <div className="max-w-2xl space-y-3">
        <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h2>
        <p className="text-sm leading-6 text-slate-100/90 sm:text-base">{description}</p>
      </div>
      {children}
    </div>
  </section>
);
